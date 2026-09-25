package com.movealarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.movealarm.app.data.SettingsStore
import java.time.ZonedDateTime

object AlarmScheduler {

    private const val REQ_HOURLY = 1
    private const val REQ_TEST = 2
    private const val REQ_SNOOZE = 3

    const val TEST_DELAY_MS = 60_000L
    const val SNOOZE_DELAY_MS = 5 * 60_000L

    /** 설정을 읽어 다음 정기 알람을 (재)등록한다. 꺼져 있으면 취소. 등록된 시각(ms)을 반환. */
    fun rescheduleHourly(context: Context, from: ZonedDateTime = ZonedDateTime.now()): Long? {
        val store = SettingsStore(context)
        val settings = store.load()
        val pi = pendingIntent(context, AlarmReceiver.ACTION_HOURLY, REQ_HOURLY)
        if (!settings.enabled) {
            alarmManager(context).cancel(pi)
            store.clearNextAlarm()
            return null
        }
        val next = AlarmTimeCalculator.nextAlarm(from, settings.minute, settings.quietStartHour, settings.quietEndHour)
        val at = next.toInstant().toEpochMilli()
        setExact(context, store, at, pi)
        store.recordScheduled(at)
        return at
    }

    fun scheduleTest(context: Context) {
        val pi = pendingIntent(context, AlarmReceiver.ACTION_TEST, REQ_TEST)
        setExact(context, SettingsStore(context), System.currentTimeMillis() + TEST_DELAY_MS, pi)
    }

    fun scheduleSnooze(context: Context) {
        val pi = pendingIntent(context, AlarmReceiver.ACTION_SNOOZE, REQ_SNOOZE)
        setExact(context, SettingsStore(context), System.currentTimeMillis() + SNOOZE_DELAY_MS, pi)
    }

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    private fun setExact(context: Context, store: SettingsStore, at: Long, pi: PendingIntent) {
        val am = alarmManager(context)
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                return
            }
            store.recordError("정확한 알람 권한이 없어 알람이 몇 분 늦게 울릴 수 있어요")
        } catch (e: SecurityException) {
            store.recordError("정확한 알람 등록 실패: ${e.message}")
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun pendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
