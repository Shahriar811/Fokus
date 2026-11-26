package com.example.smartread

import com.example.smartread.fokus.Session
import com.example.smartread.fokus.TreeStage
import com.example.smartread.fokus.generalActivity
import com.example.smartread.fokus.treeGrowth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TreeGrowthTest {
    private val zone = ZoneId.of("Asia/Dhaka")
    private val today = LocalDate.of(2026, 11, 25)

    @Test
    fun theTreeStartsAsASeedlingAndLongerSessionsWaterItMore() {
        val seedling = treeGrowth(emptyList(), today, zone)
        val short = treeGrowth(listOf(session(0, 5)), today, zone)
        val long = treeGrowth(listOf(session(0, 20, manual = true)), today, zone)

        assertEquals(TreeStage.SEEDLING, seedling.stage)
        assertEquals(0f, seedling.growth, 0f)
        assertEquals(1, long.sessionCount)
        assertEquals(20 * 60_000L, long.focusedMillis)
        assertTrue(long.growth > short.growth)
    }

    @Test
    fun milestonesRequireTimeAndSeparateLocalDays() {
        val oneDay = treeGrowth(listOf(session(0, 50), session(0, 80)), today, zone)
        val threeDays = treeGrowth(listOf(session(0, 50), session(-1, 30), session(-2, 20)), today, zone)

        assertEquals(1, oneDay.focusedDays)
        assertEquals(TreeStage.SPROUT, oneDay.stage)
        assertEquals(3, threeDays.focusedDays)
        assertEquals(TreeStage.SAPLING, threeDays.stage)
        assertEquals(3, threeDays.streak)
    }

    @Test
    fun aMissedDayBreaksTheStreakWithoutRemovingGrowth() {
        val sessions = listOf(session(-3, 50), session(-2, 30), session(-1, 20))
        val yesterday = treeGrowth(sessions, today.minusDays(1), zone)
        val todayWithFutureLog = treeGrowth(sessions + session(1, 120), today, zone)
        val tomorrow = treeGrowth(sessions, today.plusDays(1), zone)

        assertEquals(yesterday.growth, tomorrow.growth, 0f)
        assertEquals(yesterday.stage, tomorrow.stage)
        assertEquals(3, todayWithFutureLog.streak)
        assertEquals(3, todayWithFutureLog.sessionCount)
        assertEquals(0, tomorrow.streak)
        assertEquals(listOf(0L, 0L, 0L, 50 * 60_000L, 30 * 60_000L, 20 * 60_000L, 0L),
            todayWithFutureLog.week.map { it.duration })
    }

    @Test
    fun theTreeContinuesToGrowAfterReachingBloom() {
        val sessions = (0..29).map { session(-it.toLong(), 40) }
        val flowering = treeGrowth(sessions, today, zone)
        val afterAnotherSession = treeGrowth(sessions + session(0, 90, manual = true), today, zone)

        assertEquals(TreeStage.IN_BLOOM, flowering.stage)
        assertNull(flowering.next)
        assertTrue(afterAnotherSession.growth > flowering.growth)
    }

    private fun session(day: Long, minutes: Int, manual: Boolean = false): Session {
        val end = today.plusDays(day).atTime(0, 10).atZone(zone).toInstant().toEpochMilli()
        val duration = minutes * 60_000L
        return Session("$day-$minutes", generalActivity, end - duration, end, duration, manual = manual)
    }
}
