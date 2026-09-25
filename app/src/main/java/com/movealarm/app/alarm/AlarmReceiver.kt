package com.movealarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.movealarm.app.data.SettingsStore
import java.time.ZonedDateTime

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HOURLY -> {
                val store = SettingsStore(context)
                store.recordFired()
                // 다음 알람을 먼저 등록해 두어야 알림 표시 중 문제가 생겨도 반복이 끊기지 않는다.
                // +60초: 혹시 알람이 조금 일찍 전달돼도 같은 시각을 다시 잡지 않도록.
                AlarmScheduler.rescheduleHourly(context, ZonedDateTime.now().plusSeconds(60))
                if (store.load().enabled) AlarmNotifier.show(context, AlarmKind.HOURLY)
            }
            ACTION_TEST -> AlarmNotifier.show(context, AlarmKind.TEST)
            ACTION_SNOOZE -> AlarmNotifier.show(context, AlarmKind.SNOOZE)
            ACTION_DISMISS -> AlarmNotifier.cancel(context)
            ACTION_SNOOZE_REQUEST -> {
                AlarmNotifier.cancel(context)
                AlarmScheduler.scheduleSnooze(context)
            }
        }
    }

    companion object {
        const val ACTION_HOURLY = "com.movealarm.app.action.HOURLY"
        const val ACTION_TEST = "com.movealarm.app.action.TEST"
        const val ACTION_SNOOZE = "com.movealarm.app.action.SNOOZE"
        const val ACTION_DISMISS = "com.movealarm.app.action.DISMISS"
        const val ACTION_SNOOZE_REQUEST = "com.movealarm.app.action.SNOOZE_REQUEST"
    }
}
