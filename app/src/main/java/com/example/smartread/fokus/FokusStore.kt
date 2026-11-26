package com.example.smartread.fokus

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import androidx.core.database.sqlite.transaction
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal class FokusStore(private val context: Context) :
    SQLiteOpenHelper(context, "fokus.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE activities (id TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, archived INTEGER NOT NULL)")
        db.execSQL("""
            CREATE TABLE sessions (
                id TEXT PRIMARY KEY, activity_id TEXT NOT NULL, activity_name TEXT NOT NULL,
                category TEXT NOT NULL, started INTEGER NOT NULL, ended INTEGER NOT NULL,
                duration INTEGER NOT NULL, note TEXT NOT NULL, manual INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX sessions_ended ON sessions (ended DESC)")
        db.execSQL("CREATE TABLE app_state (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        importSmartReadHistory(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun load(): FokusState {
        val timer = readValue("timer")
        return FokusState(
            preferences = readValue("preferences")?.toPreferences() ?: Preferences(),
            activities = activities(),
            sessions = sessions(),
            run = timer?.optJSONObject("run")?.toRun(),
            phase = timer?.optString("phase")?.let { enumOr(it, Phase.FOCUS) } ?: Phase.FOCUS,
            ready = timer?.optBoolean("ready") ?: false
        )
    }

    fun activities(): List<Activity> = readableDatabase.rawQuery(
        "SELECT id, name, category, archived FROM activities ORDER BY name COLLATE NOCASE, id",
        null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(Activity(cursor.getString(0), cursor.getString(1),
                    enumOr(cursor.getString(2), Category.OTHER), cursor.getInt(3) != 0))
            }
        }
    }

    private fun sessions(): List<Session> = readableDatabase.rawQuery(
        "SELECT * FROM sessions ORDER BY ended DESC, id", null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val activity = Activity(cursor.getString(1), cursor.getString(2),
                    enumOr(cursor.getString(3), Category.OTHER))
                add(Session(cursor.getString(0), activity, cursor.getLong(4),
                    cursor.getLong(5), cursor.getLong(6), cursor.getString(7), cursor.getInt(8) != 0))
            }
        }
    }

    fun saveActivity(activity: Activity) = insertActivity(writableDatabase, activity)

    fun addSession(session: Session) = insertSession(writableDatabase, session)

    fun updateSessionNote(id: String, note: String) {
        writableDatabase.update("sessions", ContentValues().apply { put("note", note) }, "id = ?", arrayOf(id))
    }

    fun removeSession(id: String) = writableDatabase.delete("sessions", "id = ?", arrayOf(id))

    fun savePreferences(preferences: Preferences) {
        writeValue(writableDatabase, "preferences", JSONObject().apply {
            put("focusMinutes", preferences.focusMinutes)
            put("breakMinutes", preferences.breakMinutes)
            put("autoBreak", preferences.autoBreak)
            put("aodMode", preferences.aodMode)
            put("quietMode", preferences.quietMode)
            put("lockScreen", preferences.lockScreen)
            put("keepScreenOn", preferences.keepScreenOn)
            put("appearance", preferences.appearance.name)
            put("selectedActivityId", preferences.selectedActivityId)
        })
    }

    fun saveTimer(run: TimerRun?, phase: Phase, ready: Boolean, session: Session?) {
        writableDatabase.transaction {
            if (session != null) insertSession(this, session)
            writeValue(this, "timer", JSONObject().apply {
                put("run", run?.toJson())
                put("phase", phase.name)
                put("ready", ready)
            })
        }
    }

    private fun readValue(key: String): JSONObject? = readableDatabase.rawQuery(
        "SELECT value FROM app_state WHERE key = ?", arrayOf(key)
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getStringOrNull(0)?.let(::JSONObject) else null
    }

    private fun writeValue(db: SQLiteDatabase, key: String, value: JSONObject) {
        db.replaceOrThrow("app_state", null, ContentValues().apply {
            put("key", key)
            put("value", value.toString())
        })
    }

    private fun importSmartReadHistory(db: SQLiteDatabase) {
        val json = context.getSharedPreferences("smartread_history", Context.MODE_PRIVATE)
            .getString("sessions", null) ?: return
        val oldSessions = runCatching { JSONArray(json) }.getOrNull() ?: return
        val subjects = mutableMapOf<String, Activity>()
        for (index in 0 until oldSessions.length()) {
            val data = oldSessions.optJSONObject(index) ?: continue
            val end = data.optLong("completedAt")
            val duration = data.optLong("totalStudySeconds") * 1000
            if (end <= 0 || duration <= 0 || duration > end) continue
            val name = data.optString("subject").trim().ifEmpty { generalActivity.name }
            val activity = if (name == generalActivity.name) generalActivity else subjects.getOrPut(name) {
                Activity(UUID.randomUUID().toString(), name, Category.STUDY).also {
                    insertActivity(db, it)
                }
            }
            insertSession(db, Session("legacy-$index", activity, end - duration, end, duration))
        }
    }

    private fun insertActivity(db: SQLiteDatabase, activity: Activity) {
        db.replaceOrThrow("activities", null, ContentValues().apply {
            put("id", activity.id)
            put("name", activity.name)
            put("category", activity.category.name)
            put("archived", if (activity.archived) 1 else 0)
        })
    }

    private fun insertSession(db: SQLiteDatabase, session: Session) {
        db.insertWithOnConflict("sessions", null, ContentValues().apply {
            put("id", session.id)
            put("activity_id", session.activity.id)
            put("activity_name", session.activity.name)
            put("category", session.activity.category.name)
            put("started", session.startMillis)
            put("ended", session.endMillis)
            put("duration", session.durationMillis)
            put("note", session.note)
            put("manual", if (session.manual) 1 else 0)
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }
}

private fun JSONObject.toPreferences() = Preferences(
    focusMinutes = optInt("focusMinutes", 25),
    breakMinutes = optInt("breakMinutes", 5),
    autoBreak = optBoolean("autoBreak", true),
    aodMode = optBoolean("aodMode"),
    quietMode = optBoolean("quietMode"),
    lockScreen = optBoolean("lockScreen"),
    keepScreenOn = optBoolean("keepScreenOn"),
    appearance = enumOr(optString("appearance"), Appearance.SYSTEM),
    selectedActivityId = optString("selectedActivityId", generalActivity.id)
)

private fun TimerRun.toJson() = JSONObject().apply {
    put("id", id)
    put("phase", phase.name)
    put("activityId", activity.id)
    put("activityName", activity.name)
    put("category", activity.category.name)
    put("started", startedAt)
    put("duration", durationMillis)
    put("remaining", pausedRemaining)
    deadline?.let {
        put("deadline", JSONObject().apply {
            put("wall", it.wall)
            put("elapsed", it.elapsed)
            put("boot", it.boot)
        })
    }
}

private fun JSONObject.toRun() = TimerRun(
    id = getString("id"),
    phase = enumOr(getString("phase"), Phase.FOCUS),
    activity = Activity(getString("activityId"), getString("activityName"),
        enumOr(getString("category"), Category.OTHER)),
    startedAt = getLong("started"),
    durationMillis = getLong("duration"),
    pausedRemaining = getLong("remaining"),
    deadline = optJSONObject("deadline")?.let {
        TimeMark(it.getLong("wall"), it.getLong("elapsed"), it.getInt("boot"))
    }
)

private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback
