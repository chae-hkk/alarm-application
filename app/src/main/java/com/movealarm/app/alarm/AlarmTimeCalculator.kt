package com.movealarm.app.alarm

import com.movealarm.app.data.QuietPeriod
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object AlarmTimeCalculator {

    // 요일 설정이 있으므로 일주일을 넘겨(8일) 찾아본다.
    private const val SEARCH_HOURS = 24 * 8

    fun isQuiet(time: ZonedDateTime, periods: List<QuietPeriod>): Boolean =
        periods.any { it.covers(time.toLocalDate(), time.hour) }

    /** now 보다 엄격하게 뒤에 있는, 무음이 아닌 첫 번째 "매시 minute분". 일주일 내내 무음이면 null. */
    fun nextAlarm(now: ZonedDateTime, minute: Int, periods: List<QuietPeriod>): ZonedDateTime? {
        require(minute in 0..59) { "minute must be 0..59: $minute" }
        var candidate = now.truncatedTo(ChronoUnit.HOURS).withMinute(minute)
        if (!candidate.isAfter(now)) candidate = candidate.plusHours(1)
        repeat(SEARCH_HOURS) {
            if (!isQuiet(candidate, periods)) return candidate
            candidate = candidate.plusHours(1)
        }
        return null
    }
}
