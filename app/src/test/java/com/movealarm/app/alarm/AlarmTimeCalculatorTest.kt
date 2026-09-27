package com.movealarm.app.alarm

import com.movealarm.app.data.QuietPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

/** 2026-09-25 = 금요일, 26 = 토, 27 = 일, 28 = 월, 30 = 수 */
class AlarmTimeCalculatorTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 9, day, hour, minute, second, 0, seoul)

    private val sleep = QuietPeriod(22, 8)

    private fun next(now: ZonedDateTime, minute: Int = 30, periods: List<QuietPeriod> = listOf(sleep)) =
        AlarmTimeCalculator.nextAlarm(now, minute, periods)

    // ---- 매일 무음 (이전 버전과 같은 동작) ----

    @Test fun `같은 시간대의 설정 분이 아직 안 지났으면 그 시각`() {
        assertEquals(at(25, 9, 30), next(at(25, 9, 10)))
    }

    @Test fun `설정 분과 정확히 같은 순간이면 다음 시간`() {
        assertEquals(at(25, 10, 30), next(at(25, 9, 30)))
    }

    @Test fun `무음 시작 직전 마지막 알람 뒤에는 다음날 무음 끝나는 시간대로 건너뛴다`() {
        assertEquals(at(26, 8, 30), next(at(25, 21, 30)))
    }

    @Test fun `무음 끝 시각 이후 첫 알람`() {
        assertEquals(at(26, 8, 30), next(at(26, 7, 59)))
        assertEquals(at(26, 8, 30), next(at(25, 23, 0)))
    }

    @Test fun `무음 시간이 없으면 매시간`() {
        assertEquals(at(26, 0, 0), next(at(25, 23, 50), minute = 0, periods = emptyList()))
    }

    // ---- 요일 ----

    @Test fun `평일 회의 시간은 평일에만 무음`() {
        val meeting = QuietPeriod(14, 15, QuietPeriod.WEEKDAYS)
        // 수요일 14시는 무음 → 15시
        assertEquals(at(30, 15, 0), next(at(30, 13, 30), minute = 0, periods = listOf(meeting)))
        // 토요일 14시는 울림
        assertEquals(at(26, 14, 0), next(at(26, 13, 30), minute = 0, periods = listOf(meeting)))
    }

    @Test fun `자정을 넘기는 무음은 시작한 요일 기준 - 금요일 밤만`() {
        val fridayNight = QuietPeriod(22, 8, setOf(DayOfWeek.FRIDAY))
        // 금 22:30 ~ 토 07:30 무음 → 토 08:30
        assertEquals(at(26, 8, 30), next(at(25, 21, 30), periods = listOf(fridayNight)))
        // 토 22:30 은 토요일이 선택되지 않았으므로 울림
        assertEquals(at(26, 22, 30), next(at(26, 21, 30), periods = listOf(fridayNight)))
    }

    @Test fun `주말 하루 종일 무음`() {
        val weekendOff = QuietPeriod(0, 0, QuietPeriod.WEEKEND)
        // 금 23:10 → 금 23:30 은 울림
        assertEquals(at(25, 23, 30), next(at(25, 23, 10), periods = listOf(weekendOff)))
        // 금 23:30 뒤 → 토·일 전부 무음 → 월 00:30
        assertEquals(at(28, 0, 30), next(at(25, 23, 30), periods = listOf(weekendOff)))
    }

    @Test fun `여러 무음 시간을 함께 적용`() {
        val weekendOff = QuietPeriod(0, 0, QuietPeriod.WEEKEND)
        // 금 21:30 뒤: 금 밤 22~ 무음, 토·일 종일 무음, 일요일 밤 22시~월 08시 무음 → 월 08:30
        assertEquals(at(28, 8, 30), next(at(25, 21, 30), periods = listOf(sleep, weekendOff)))
    }

    @Test fun `일주일 내내 하루 종일 무음이면 울릴 시간이 없다`() {
        assertNull(next(at(25, 9, 0), periods = listOf(QuietPeriod(0, 0))))
    }

    @Test fun `무음 판정`() {
        val periods = listOf(sleep)
        assertEquals(true, AlarmTimeCalculator.isQuiet(at(25, 22, 0), periods))
        assertEquals(true, AlarmTimeCalculator.isQuiet(at(26, 7, 30), periods))
        assertEquals(false, AlarmTimeCalculator.isQuiet(at(26, 8, 0), periods))
        assertEquals(false, AlarmTimeCalculator.isQuiet(at(25, 21, 59), periods))
    }
}
