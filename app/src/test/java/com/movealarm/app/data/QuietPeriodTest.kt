package com.movealarm.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class QuietPeriodTest {

    private val friday = LocalDate.of(2026, 9, 25)
    private val saturday = friday.plusDays(1)

    @Test fun `저장 형식으로 바꿨다가 되돌려도 같다`() {
        val periods = listOf(
            QuietPeriod(22, 8),
            QuietPeriod(14, 15, QuietPeriod.WEEKDAYS),
            QuietPeriod(0, 0, setOf(DayOfWeek.SUNDAY)),
        )
        assertEquals(periods, QuietPeriod.decode(QuietPeriod.encode(periods)))
    }

    @Test fun `비어 있거나 망가진 값은 무시`() {
        assertEquals(emptyList<QuietPeriod>(), QuietPeriod.decode(""))
        assertEquals(listOf(QuietPeriod(1, 2)), QuietPeriod.decode("abc;1-2-127;25-3-1;4-5-0"))
    }

    @Test fun `요일 표시`() {
        assertEquals("매일", QuietPeriod.daysLabel(QuietPeriod.EVERY_DAY))
        assertEquals("평일 (월~금)", QuietPeriod.daysLabel(QuietPeriod.WEEKDAYS))
        assertEquals("주말 (토·일)", QuietPeriod.daysLabel(QuietPeriod.WEEKEND))
        assertEquals("월·수·금", QuietPeriod.daysLabel(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
    }

    @Test fun `자정을 넘기는 구간의 새벽은 전날 요일을 따른다`() {
        val fridayNight = QuietPeriod(22, 8, setOf(DayOfWeek.FRIDAY))
        assertTrue(fridayNight.covers(friday, 23))
        assertTrue(fridayNight.covers(saturday, 3))
        assertFalse(fridayNight.covers(friday, 3))
        assertFalse(fridayNight.covers(saturday, 23))
    }

    @Test fun `하루 종일`() {
        val sundayOff = QuietPeriod(0, 0, setOf(DayOfWeek.SUNDAY))
        val sunday = friday.plusDays(2)
        assertTrue(sundayOff.isAllDay)
        assertTrue((0..23).all { sundayOff.covers(sunday, it) })
        assertFalse(sundayOff.covers(saturday, 12))
    }
}
