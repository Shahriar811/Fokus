package com.example.smartread.fokus

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.exp

enum class TreeStage(val label: String, val minutes: Int, val focusedDays: Int) {
    SEEDLING("Seedling", 0, 0),
    SPROUT("Sprout", 25, 1),
    SAPLING("Sapling", 100, 3),
    YOUNG_TREE("Young tree", 240, 7),
    MATURE("Mature tree", 600, 14),
    IN_BLOOM("In bloom", 1200, 30);

    val durationMillis: Long get() = minutes * 60_000L
}

data class TreeGrowth(
    val stage: TreeStage,
    val next: TreeStage?,
    val focusedMillis: Long,
    val focusedDays: Int,
    val sessionCount: Int,
    val streak: Int,
    val week: List<DailyTotal>,
    val growth: Float
)

fun treeGrowth(sessions: List<Session>, today: LocalDate, zone: ZoneId): TreeGrowth {
    val days = mutableMapOf<LocalDate, Long>()
    var focusedMillis = 0L
    var sessionCount = 0
    sessions.forEach { session ->
        val date = session.endMillis.localDate(zone)
        if (session.durationMillis > 0 && date <= today) {
            days[date] = days.getOrDefault(date, 0L) + session.durationMillis
            focusedMillis += session.durationMillis
            sessionCount++
        }
    }

    val stage = TreeStage.entries.last { focusedMillis >= it.durationMillis && days.size >= it.focusedDays }
    val next = TreeStage.entries.getOrNull(stage.ordinal + 1)
    val progress = if (next != null) {
        val time = (focusedMillis - stage.durationMillis).toFloat() / (next.durationMillis - stage.durationMillis)
        val dates = (days.size - stage.focusedDays).toFloat() / (next.focusedDays - stage.focusedDays)
        0.74f * time.coerceIn(0f, 1f) + 0.26f * dates.coerceIn(0f, 1f)
    } else {
        val extraTime = (focusedMillis - stage.durationMillis) / stage.durationMillis.toDouble()
        val extraDays = (days.size - stage.focusedDays) / stage.focusedDays.toDouble()
        (0.85 * (1 - exp(-0.65 * extraTime - 0.35 * extraDays))).toFloat()
    }

    var current = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (current in days) {
        streak++
        current = current.minusDays(1)
    }

    return TreeGrowth(stage, next, focusedMillis, days.size, sessionCount, streak,
        List(7) { day ->
            val date = today.minusDays(6L - day)
            DailyTotal(date, days.getOrDefault(date, 0L))
        }, stage.ordinal + progress)
}
