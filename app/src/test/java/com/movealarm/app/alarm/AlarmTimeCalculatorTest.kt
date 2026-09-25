package com.movealarm.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeCalculatorTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 9, day, hour, minute, second, 0, seoul)

    private fun next(now: ZonedDateTime, minute: Int = 30, start: Int = 22, end: Int = 8) =
        AlarmTimeCalculator.nextAlarm(now, minute, start, end)

    @Test fun `같은 시간대의 설정 분이 아직 안 지났으면 그 시각`() {
        assertEquals(at(25, 9, 30), next(at(25, 9, 10)))
    }

    @Test fun `설정 분과 정확히 같은 순간이면 다음 시간`() {
        assertEquals(at(25, 10, 30), next(at(25, 9, 30)))
    }

    @Test fun `연속으로 매시간 울린다`() {
        assertEquals(at(25, 11, 30), next(at(25, 10, 30, 1)))
    }

    @Test fun `무음 시작 직전 마지막 알람 뒤에는 다음날 무음 끝나는 시간대로 건너뛴다`() {
        // 21:30 울린 뒤 → 22~07시는 무음 → 08:30
        assertEquals(at(26, 8, 30), next(at(25, 21, 30)))
    }

    @Test fun `무음 끝 시각 이후 첫 알람`() {
        assertEquals(at(26, 8, 30), next(at(26, 7, 59)))
        assertEquals(at(26, 8, 30), next(at(25, 23, 0)))
    }

    @Test fun `자정을 넘기지 않는 무음 구간 (점심 12~13시)`() {
        assertEquals(at(25, 13, 0), next(at(25, 11, 30), minute = 0, start = 12, end = 13))
    }

    @Test fun `시작과 끝이 같으면 무음 없음`() {
        assertEquals(at(26, 0, 0), next(at(25, 23, 50), minute = 0, start = 0, end = 0))
    }

    @Test fun `정각(0분) 설정`() {
        assertEquals(at(25, 11, 0), next(at(25, 10, 0), minute = 0))
    }

    @Test fun `무음 시간 판정`() {
        assertTrue(AlarmTimeCalculator.isQuietHour(22, 22, 8))
        assertTrue(AlarmTimeCalculator.isQuietHour(0, 22, 8))
        assertTrue(AlarmTimeCalculator.isQuietHour(7, 22, 8))
        assertFalse(AlarmTimeCalculator.isQuietHour(8, 22, 8))
        assertFalse(AlarmTimeCalculator.isQuietHour(21, 22, 8))
        assertTrue(AlarmTimeCalculator.isQuietHour(12, 12, 13))
        assertFalse(AlarmTimeCalculator.isQuietHour(13, 12, 13))
        assertFalse(AlarmTimeCalculator.isQuietHour(5, 5, 5))
    }

    @Test fun `거의 하루 종일 무음이어도 비무음 시간을 찾는다`() {
        // 9시~8시 무음 = 8시대만 울림
        assertEquals(at(26, 8, 15), next(at(25, 9, 0), minute = 15, start = 9, end = 8))
    }
}
