package com.movealarm.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 알람이 울리지 않는 시간대 하나.
 * - startHour < endHour: 선택한 요일의 [start:00, end:00)
 * - startHour > endHour: 자정을 넘김. 선택한 요일 start:00 부터 다음날 end:00 까지 (시작한 요일 기준)
 * - startHour == endHour: 선택한 요일 하루 종일
 */
data class QuietPeriod(
    val startHour: Int,
    val endHour: Int,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
) {
    val isAllDay: Boolean get() = startHour == endHour

    fun covers(date: LocalDate, hour: Int): Boolean {
        val today = date.dayOfWeek in days
        return when {
            isAllDay -> today
            startHour < endHour -> today && hour in startHour until endHour
            else -> (today && hour >= startHour) || (hour < endHour && date.minusDays(1).dayOfWeek in days)
        }
    }

    companion object {
        const val MAX_COUNT = 6
        val WEEKDAYS: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val WEEKEND: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val EVERY_DAY: Set<DayOfWeek> = DayOfWeek.entries.toSet()

        private val SHORT_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")
        fun shortName(day: DayOfWeek): String = SHORT_NAMES[day.value - 1]

        fun daysLabel(days: Set<DayOfWeek>): String = when (days) {
            EVERY_DAY -> "매일"
            WEEKDAYS -> "평일 (월~금)"
            WEEKEND -> "주말 (토·일)"
            else -> DayOfWeek.entries.filter { it in days }.joinToString("·") { shortName(it) }
        }

        /** "22-8-127;14-15-31" (요일 비트: 월=1, 화=2 … 일=64) */
        fun encode(periods: List<QuietPeriod>): String = periods.joinToString(";") { p ->
            val mask = p.days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
            "${p.startHour}-${p.endHour}-$mask"
        }

        fun decode(text: String): List<QuietPeriod> = text.split(";").mapNotNull { part ->
            val nums = part.split("-").mapNotNull { it.trim().toIntOrNull() }
            if (nums.size != 3) return@mapNotNull null
            val (start, end, mask) = nums
            if (start !in 0..23 || end !in 0..23) return@mapNotNull null
            val days = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
            if (days.isEmpty()) null else QuietPeriod(start, end, days)
        }.take(MAX_COUNT)
    }
}
