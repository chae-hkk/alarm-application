package com.movealarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
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
                if (store.load().enabled) ring(context, AlarmKind.HOURLY)
            }
            ACTION_TEST -> ring(context, AlarmKind.TEST)
            ACTION_SNOOZE -> ring(context, AlarmKind.SNOOZE)
            ACTION_DISMISS -> AlarmRingService.stop(context)
            ACTION_SNOOZE_REQUEST -> {
                AlarmRingService.stop(context)
                AlarmScheduler.scheduleSnooze(context)
            }
        }
    }

    private fun ring(context: Context, kind: AlarmKind) {
        if (!AlarmNotifier.canPostNotifications(context)) {
            SettingsStore(context).recordError("알림 권한이 꺼져 있어 알람을 표시하지 못했어요")
            return
        }
        try {
            // 정확한 알람으로 깨어난 직후라 백그라운드에서도 포그라운드 서비스를 시작할 수 있다.
            ContextCompat.startForegroundService(context, AlarmRingService.ringIntent(context, kind))
        } catch (e: IllegalStateException) {
            SettingsStore(context).recordError("알람 소리 서비스를 시작하지 못해 알림만 표시: ${e.javaClass.simpleName}")
            AlarmNotifier.showFallback(context, AlarmNotifier.prepareContent(context, kind))
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
