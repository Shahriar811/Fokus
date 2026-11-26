package com.example.smartread.fokus

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

enum class Category(val label: String) {
    TASK("Task"), COURSE("Course"), BOOK("Book"), STUDY("Study"), WORK("Work"),
    GYM("Gym"), WALKING("Walking"), RUNNING("Running"), OTHER("Other")
}

data class Activity(
    val id: String,
    val name: String,
    val category: Category,
    val archived: Boolean = false
)

val generalActivity = Activity("general", "General", Category.OTHER)

data class Session(
    val id: String,
    val activity: Activity,
    val startMillis: Long,
    val endMillis: Long,
    val durationMillis: Long,
    val note: String = "",
    val manual: Boolean = false
)

enum class Phase(val label: String) { FOCUS("Focus"), BREAK("Break") }
enum class Appearance(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class Preferences(
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val autoBreak: Boolean = true,
    val aodMode: Boolean = false,
    val quietMode: Boolean = false,
    val lockScreen: Boolean = false,
    val keepScreenOn: Boolean = false,
    val appearance: Appearance = Appearance.SYSTEM,
    val selectedActivityId: String = generalActivity.id
) {
    fun minutes(phase: Phase) = if (phase == Phase.FOCUS) focusMinutes else breakMinutes
}

data class TimeMark(val wall: Long, val elapsed: Long, val boot: Int)

data class TimerRun(
    val id: String,
    val phase: Phase,
    val activity: Activity,
    val startedAt: Long,
    val durationMillis: Long,
    val pausedRemaining: Long = durationMillis,
    val deadline: TimeMark? = null
) {
    fun untilDeadline(now: TimeMark): Long {
        val end = deadline ?: return pausedRemaining
        val untilEnd = if (end.boot >= 0 && now.boot == end.boot) end.elapsed - now.elapsed
        else end.wall - now.wall
        return untilEnd.coerceAtLeast(0L)
    }

    fun remaining(now: TimeMark): Long = untilDeadline(now).coerceIn(0, pausedRemaining)

    fun resume(now: TimeMark) = copy(
        deadline = TimeMark(now.wall + pausedRemaining, now.elapsed + pausedRemaining, now.boot)
    )

    fun pause(now: TimeMark) = copy(pausedRemaining = remaining(now), deadline = null)

    fun completedAt(now: TimeMark): Long {
        val end = requireNotNull(deadline)
        return if (now.boot == end.boot && end.boot >= 0) now.wall + end.elapsed - now.elapsed
        else end.wall
    }

    fun session(endedAt: Long, worked: Long) = Session(
        id, activity, startedAt.coerceAtMost(endedAt - worked), endedAt, worked
    )
}

data class FokusState(
    val preferences: Preferences = Preferences(),
    val activities: List<Activity> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val lastSavedSession: Session? = null,
    val run: TimerRun? = null,
    val phase: Phase = Phase.FOCUS,
    val ready: Boolean = false
) {
    val selectedActivity: Activity
        get() = activities.firstOrNull { it.id == preferences.selectedActivityId && !it.archived }
            ?: generalActivity
}

data class DailyTotal(val date: LocalDate, val duration: Long)

data class PeriodStats(
    val daily: List<DailyTotal>,
    val current: List<Session>,
    val previousDuration: Long
) {
    val duration = daily.sumOf { it.duration }
    val activeDays = daily.count { it.duration > 0 }
}

fun summarize(
    sessions: List<Session>,
    today: LocalDate,
    days: Int,
    zone: ZoneId
): PeriodStats {
    val first = today.minusDays(days - 1L)
    val previous = first.minusDays(days.toLong())
    val byDate = sessions.groupBy { it.endMillis.localDate(zone) }
    val daily = List(days) { index ->
        val date = first.plusDays(index.toLong())
        DailyTotal(date, byDate[date].orEmpty().sumOf { it.durationMillis })
    }
    return PeriodStats(
        daily,
        byDate.filterKeys { it in first..today }.values.flatten().sortedByDescending { it.endMillis },
        byDate.filterKeys { it >= previous && it < first }.values.sumOf { records ->
            records.sumOf { it.durationMillis }
        }
    )
}

fun Long.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun countdown(millis: Long): String {
    val seconds = ceil(millis.coerceAtLeast(0) / 1000.0).toLong()
    return (seconds / 60).toString().padStart(2, '0') + ":" +
        (seconds % 60).toString().padStart(2, '0')
}

fun durationLabel(millis: Long, includeSeconds: Boolean = false): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val label = when {
        minutes >= 60 -> if (minutes % 60 == 0L) "${minutes / 60}h" else "${minutes / 60}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0m"
    }
    return if (includeSeconds && minutes > 0 && seconds % 60 != 0L) "$label ${seconds % 60}s" else label
}

fun sessionCountLabel(count: Int) = "$count ${if (count == 1) "session" else "sessions"}"
