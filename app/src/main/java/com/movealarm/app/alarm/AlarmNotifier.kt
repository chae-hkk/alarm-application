package com.movealarm.app.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.movealarm.app.AlarmActivity
import com.movealarm.app.R
import com.movealarm.app.data.SettingsStore
import com.movealarm.app.photo.PhotoStore
import java.io.File

enum class AlarmKind {
    HOURLY, TEST, SNOOZE;

    companion object {
        fun from(name: String?): AlarmKind = entries.firstOrNull { it.name == name } ?: HOURLY
    }
}

/** 알람 한 번에 보여줄 내용 (소리 서비스가 15초 뒤 조용한 알림으로 바꿀 때 다시 쓴다) */
class AlarmContent(
    val kind: AlarmKind,
    val title: String,
    val text: String,
    val photo: File?,
    val picture: Bitmap,
    val largeIcon: Bitmap,
)

object AlarmNotifier {

    // 채널 설정은 한 번 만들어지면 앱에서 바꿀 수 없어서, 바꿀 때는 새 ID를 쓰고 옛 채널을 지운다.
    private const val OLD_CHANNEL_ID = "move_alarm_v1"
    // 소리·진동은 AlarmRingService가 직접 15초 동안 내므로 이 채널은 조용하다.
    private const val CHANNEL_ALARM = "move_alarm_v2"
    // 소리 서비스를 시작하지 못했을 때만 쓰는 예비 채널 (시스템이 소리/진동 모드를 따름)
    private const val CHANNEL_FALLBACK = "move_alarm_fallback_v1"

    const val NOTIFICATION_ID = 1001

    // 알림 비트맵은 프로세스 간 전달 크기 제한이 있어 작게 만든다.
    private const val NOTIFICATION_PICTURE_PX = 600
    private const val LARGE_ICON_PX = 160

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(OLD_CHANNEL_ID)
        if (nm.getNotificationChannel(CHANNEL_ALARM) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALARM, "움직임 알람", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "매시간 움직이라고 알려주는 알람"
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }
        if (nm.getNotificationChannel(CHANNEL_FALLBACK) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_FALLBACK, "움직임 알람 (예비)", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "알람 소리를 직접 낼 수 없을 때 쓰는 예비 알림"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun shouldUseFullScreen(context: Context): Boolean {
        val screenOn = context.getSystemService(PowerManager::class.java).isInteractive
        val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        return AlarmPolicy.useFullScreen(screenOn, locked)
    }

    /** 보여줄 사진을 고르고(다음 순서 기록) 알림 내용을 만든다. */
    fun prepareContent(context: Context, kind: AlarmKind): AlarmContent {
        val store = SettingsStore(context)
        val photo = PhotoStore(context).pickForAlarm(store, store.load().photoOrder)
        return AlarmContent(
            kind = kind,
            title = if (kind == AlarmKind.TEST) "테스트 알람이에요!" else "움직일 시간이에요!",
            text = if (kind == AlarmKind.TEST) "이 알림이 보이면 알람이 잘 작동하는 거예요." else "잠깐 일어나서 기지개를 켜볼까요?",
            photo = photo,
            picture = photo?.let { PhotoStore.decodeFile(it, NOTIFICATION_PICTURE_PX, Bitmap.Config.RGB_565) }
                ?: defaultPicture(context, NOTIFICATION_PICTURE_PX),
            largeIcon = photo?.let { PhotoStore.decodeFile(it, LARGE_ICON_PX) } ?: defaultPicture(context, LARGE_ICON_PX),
        )
    }

    /** 울리는 중 알림. fullScreen=false 이면 위쪽 작은 알림창(헤드업)으로만 뜬다. */
    fun buildRinging(context: Context, content: AlarmContent, fullScreen: Boolean): Notification =
        builder(context, content, CHANNEL_ALARM, fullScreen)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    /** 15초가 지난 뒤: 소리 없이 알림창에 남겨 두는 알림. */
    @SuppressLint("MissingPermission")
    fun notifyQuiet(context: Context, content: AlarmContent) {
        runCatching {
            val n = builder(context, content, CHANNEL_ALARM, fullScreen = false).setOnlyAlertOnce(true).build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        }
    }

    /** 소리 서비스를 시작할 수 없을 때: 시스템 알림 소리/진동(휴대폰 소리 모드를 따름)으로만 알린다. */
    @SuppressLint("MissingPermission")
    fun showFallback(context: Context, content: AlarmContent) {
        try {
            ensureChannels(context)
            val n = builder(context, content, CHANNEL_FALLBACK, shouldUseFullScreen(context)).build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        } catch (e: Exception) {
            SettingsStore(context).recordError("알림 표시 실패: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun builder(context: Context, content: AlarmContent, channel: String, fullScreen: Boolean): NotificationCompat.Builder {
        val screenPi = PendingIntent.getActivity(
            context, 10, AlarmActivity.intent(context, content.photo?.absolutePath, content.kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissPi = receiverPi(context, AlarmReceiver.ACTION_DISMISS, 20)
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setLargeIcon(content.largeIcon)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(content.picture)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(content.text),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(screenPi)
            .setDeleteIntent(dismissPi)
            .addAction(0, "확인", dismissPi)
            .addAction(0, "5분 뒤 다시", receiverPi(context, AlarmReceiver.ACTION_SNOOZE_REQUEST, 21))
            .apply { if (fullScreen) setFullScreenIntent(screenPi, true) }
    }

    private fun defaultPicture(context: Context, px: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.drawable.illust_move)!!
        return drawable.toBitmap(px, px * drawable.intrinsicHeight / drawable.intrinsicWidth)
    }

    private fun receiverPi(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
