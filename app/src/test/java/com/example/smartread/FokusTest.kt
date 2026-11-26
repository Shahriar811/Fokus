package com.example.smartread

import com.example.smartread.fokus.Phase
import com.example.smartread.fokus.Session
import com.example.smartread.fokus.TimeMark
import com.example.smartread.fokus.TimerRun
import com.example.smartread.fokus.countdown
import com.example.smartread.fokus.generalActivity
import com.example.smartread.fokus.summarize
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FokusTest {
    @Test
    fun runningCountdownIgnoresWallClockChangesAndPauseExcludesIdleTime() {
        val started = TimeMark(1_000_000, 20_000, 1)
        val run = TimerRun("one", Phase.FOCUS, generalActivity, started.wall, 60_000).resume(started)
        val paused = run.pause(TimeMark(2_010_000, 30_000, 1))
        assertEquals(50_000L, paused.remaining(TimeMark(2_030_000, 50_000, 1)))

        val resumed = paused.resume(TimeMark(2_030_000, 50_000, 1))
        assertEquals("00:01", countdown(resumed.remaining(TimeMark(2_079_250, 99_250, 1))))
        val returned = TimeMark(2_090_000, 110_000, 1)
        assertEquals(0L, resumed.remaining(returned))
        assertEquals(2_080_000L, resumed.completedAt(returned))
        assertEquals(60_000L, resumed.session(resumed.completedAt(returned), 60_000).durationMillis)
    }

    @Test
    fun restoredCountdownUsesWallDeadlineAfterRestart() {
        val started = TimeMark(1_000_000, 300_000, 8)
        val run = TimerRun("one", Phase.BREAK, generalActivity, started.wall, 300_000).resume(started)
        val restarted = TimeMark(1_100_000, 10_000, 9)
        assertEquals(200_000L, run.remaining(restarted))
        assertEquals(1_300_000L, run.completedAt(restarted))
    }

    @Test
    fun comparisonUsesDisjointPeriodsAndLocalCompletionDates() {
        val zone = ZoneId.of("Asia/Dhaka")
        val date = LocalDate.of(2026, 11, 25)
        fun session(id: String, dayOffset: Long, minutes: Long): Session {
            val end = date.plusDays(dayOffset).atTime(0, 1).atZone(zone).toInstant().toEpochMilli()
            val duration = minutes * 60_000
            return Session(id, generalActivity, end - duration, end, duration)
        }

        val stats = summarize(listOf(
            session("today", 0, 25), session("current-start", -6, 15),
            session("previous-end", -7, 30), session("previous-start", -13, 10),
            session("earlier", -14, 20), session("future", 1, 50)
        ), date, 7, zone)
        assertEquals(listOf("today", "current-start"), stats.current.map { it.id })
        assertEquals(40 * 60_000L, stats.duration)
        assertEquals(40 * 60_000L, stats.previousDuration)
        assertEquals(2, stats.activeDays)
    }

    @Test
    fun breakTransitionWithThreeSecondVibrationLeadKeepsFullBreakDuration() {
        val breakStart = TimeMark(1_003_000, 23_000, 1)
        val breakRun = TimerRun("break-1", Phase.BREAK, generalActivity, breakStart.wall, 300_000).resume(breakStart)

        val duringVibration = TimeMark(1_001_500, 21_500, 1)
        assertEquals(300_000L, breakRun.remaining(duringVibration))
        assertEquals(301_500L, breakRun.untilDeadline(duringVibration))
        assertEquals("05:00", countdown(breakRun.remaining(duringVibration)))

        val tenSecondsAfterVibration = TimeMark(1_013_000, 33_000, 1)
        assertEquals(290_000L, breakRun.remaining(tenSecondsAfterVibration))
        assertEquals(290_000L, breakRun.untilDeadline(tenSecondsAfterVibration))
        assertEquals("04:50", countdown(breakRun.remaining(tenSecondsAfterVibration)))
    }
}
