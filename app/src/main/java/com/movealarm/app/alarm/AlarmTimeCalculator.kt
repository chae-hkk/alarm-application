package com.movealarm.app.alarm

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object AlarmTimeCalculator {

    /** 무음 구간은 [startHour:00, endHour:00) 이며 자정을 넘길 수 있다. start == end 이면 무음 없음. */
    fun isQuietHour(hour: Int, startHour: Int, endHour: Int): Boolean = when {
        startHour == endHour -> false
        startHour < endHour -> hour in startHour until endHour
        else -> hour >= startHour || hour < endHour
    }

    /** now 보다 엄격하게 뒤에 있는, 무음 구간이 아닌 첫 번째 "매시 minute분" 시각. */
    fun nextAlarm(now: ZonedDateTime, minute: Int, quietStartHour: Int, quietEndHour: Int): ZonedDateTime {
        require(minute in 0..59) { "minute must be 0..59: $minute" }
        var candidate = now.truncatedTo(ChronoUnit.HOURS).withMinute(minute)
        if (!candidate.isAfter(now)) candidate = candidate.plusHours(1)
        repeat(24) {
            if (!isQuietHour(candidate.hour, quietStartHour, quietEndHour)) return candidate
            candidate = candidate.plusHours(1)
        }
        return candidate
    }
}
