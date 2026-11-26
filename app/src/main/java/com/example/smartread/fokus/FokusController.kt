package com.example.smartread.fokus

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class FokusController private constructor(context: Context) {
    private val store = FokusStore(context)
    private val system = TimerSystem(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    private val model = MutableStateFlow<FokusState?>(null)
    val state = model.asStateFlow()
    private val loadFailed = MutableStateFlow(false)
    val initialLoadFailed = loadFailed.asStateFlow()
    private val errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = errors.asSharedFlow()
    private var wakeup: Job? = null

    fun now() = TimeMark(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot)

    fun sync() = action { current, time -> applySystem(current, time) }

    fun toggleTimer(phase: Phase, expected: TimerRun?) = action { current, time ->
        val run = current.run
        if (current.phase != phase || run != expected) return@action
        val next = when {
            run == null -> newRun(current.phase, current.selectedActivity, current.preferences, time)
            run.deadline == null -> run.resume(time)
            else -> run.pause(time)
        }
        changeTimer(current, next, next.phase, time)
    }

    fun changePhase(phase: Phase) = action { current, time ->
        if (current.run == null && phase != current.phase) changeTimer(current, null, phase, time)
    }

    fun finish(save: Boolean, expectedId: String?) = action { current, time ->
        val run = current.run ?: return@action
        if (run.id != expectedId) return@action
        val worked = run.durationMillis - run.remaining(time)
        val session = if (save && run.phase == Phase.FOCUS && worked >= 1000) run.session(time.wall, worked)
        else null
        changeTimer(current, null, Phase.FOCUS, time, session = session)
    }

    fun updatePreferences(update: (Preferences) -> Preferences) = action { current, time ->
        val preferences = update(current.preferences)
        store.savePreferences(preferences)
        val next = current.copy(preferences = preferences)
        model.value = next
        applySystem(next, time)
    }

    fun selectActivity(activity: Activity) = updatePreferences { it.copy(selectedActivityId = activity.id) }

    fun saveActivity(name: String, category: Category, existing: Activity?, select: Boolean) =
        action { current, _ ->
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@action
            val activity = Activity(existing?.id ?: id(), trimmed, category, existing?.archived ?: false)
            store.saveActivity(activity)
            val preferences = if (select) current.preferences.copy(selectedActivityId = activity.id)
                .also(store::savePreferences) else current.preferences
            model.value = current.copy(activities = store.activities(), preferences = preferences)
        }

    fun archiveActivity(activity: Activity) = action { current, _ ->
        store.saveActivity(activity.copy(archived = !activity.archived))
        val preferences = if (!activity.archived && current.preferences.selectedActivityId == activity.id) {
            current.preferences.copy(selectedActivityId = generalActivity.id).also(store::savePreferences)
        } else current.preferences
        model.value = current.copy(activities = store.activities(), preferences = preferences)
    }

    fun logSession(activity: Activity, start: Long, duration: Long, note: String) = action { current, time ->
        if (duration <= 0 || start + duration > time.wall) return@action
        val session = Session(id(), activity, start, start + duration, duration, note.trim(), manual = true)
        store.addSession(session)
        model.value = current.withSession(session)
    }

    fun deleteSession(session: Session) = action { current, _ ->
        store.removeSession(session.id)
        model.value = current.copy(
            sessions = current.sessions.filterNot { it.id == session.id },
            lastSavedSession = current.lastSavedSession?.takeUnless { it.id == session.id }
        )
    }

    fun updateSessionNote(session: Session, note: String) = action { current, _ ->
        val cleaned = note.trim()
        store.updateSessionNote(session.id, cleaned)
        model.value = current.copy(sessions = current.sessions.map {
            if (it.id == session.id) it.copy(note = cleaned) else it
        })
    }

    private fun action(block: (FokusState, TimeMark) -> Unit): Job = scope.launch {
        try {
            val current = model.value ?: store.load()
            val time = now()
            val reconciled = completeIfDue(current, time)
            if (model.value == null) model.value = reconciled
            loadFailed.value = false
            block(reconciled, time)
        } catch (failure: android.database.sqlite.SQLiteException) {
            Log.e("Fokus", "Could not update local data", failure)
            if (model.value == null) loadFailed.value = true
            else errors.tryEmit("Fokus couldn’t access its local data. Try again.")
        }
    }

    private fun completeIfDue(current: FokusState, time: TimeMark): FokusState {
        val run = current.run ?: return current
        if (run.deadline == null || run.remaining(time) > 0L) return current
        val ended = run.completedAt(time)
        val session = if (run.phase == Phase.FOCUS) run.session(ended, run.durationMillis) else null
        val autoContinue = current.preferences.autoBreak || current.preferences.aodMode
        val late = (time.wall - ended).coerceAtLeast(0)
        val nextRun = if (run.phase == Phase.FOCUS && autoContinue) {
            val duration = current.preferences.breakMinutes * 60_000L
            val vibrationLead = if (late < 3_000L) 3_000L - late else 0L
            if (late < duration) newRun(
                Phase.BREAK, run.activity, current.preferences,
                time.copy(wall = ended + vibrationLead, elapsed = time.elapsed - late + vibrationLead)
            ) else null
        } else null
        val nextPhase = nextRun?.phase ?: if (run.phase == Phase.FOCUS && !autoContinue) {
            Phase.BREAK
        } else Phase.FOCUS
        val next = changeTimer(current, nextRun, nextPhase, time, session, ready = nextRun == null)
        if (late < 60_000L) {
            system.vibrateCompletion(3_000L)
            system.notifyComplete(run.phase, nextRun != null)
        }
        return next
    }

    private fun changeTimer(
        current: FokusState,
        run: TimerRun?,
        phase: Phase,
        time: TimeMark,
        session: Session? = null,
        ready: Boolean = false
    ): FokusState {
        store.saveTimer(run, phase, ready, session)
        val next = current.copy(run = run, phase = phase, ready = ready).withSession(session)
        model.value = next
        applySystem(next, time)
        return next
    }

    private fun applySystem(current: FokusState, time: TimeMark) {
        val run = current.run?.takeIf { it.deadline != null }
        val remaining = run?.remaining(time)
        val wakeupDelay = run?.untilDeadline(time)
        system.sync(current, time, remaining, wakeupDelay)
        wakeup?.cancel()
        wakeup = wakeupDelay?.let {
            scope.launch {
                delay(it)
                sync()
            }
        }
    }

    private fun newRun(phase: Phase, activity: Activity, preferences: Preferences, time: TimeMark) =
        TimerRun(id(), phase, activity, time.wall, preferences.minutes(phase) * 60_000L).resume(time)

    private fun id() = UUID.randomUUID().toString()

    private fun FokusState.withSession(session: Session?): FokusState =
        if (session == null) this else copy(
            sessions = (sessions + session).sortedByDescending { it.endMillis },
            lastSavedSession = session
        )

    companion object {
        @Volatile private var instance: FokusController? = null

        fun get(context: Context): FokusController = instance ?: synchronized(this) {
            instance ?: FokusController(context.applicationContext).also { instance = it }
        }
    }
}
