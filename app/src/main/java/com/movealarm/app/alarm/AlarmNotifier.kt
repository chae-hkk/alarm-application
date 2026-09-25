package com.movealarm.app.alarm

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.movealarm.app.AlarmActivity
import com.movealarm.app.R
import com.movealarm.app.data.SettingsStore
import com.movealarm.app.photo.PhotoStore

enum class AlarmKind { HOURLY, TEST, SNOOZE }

object AlarmNotifier {

    // 채널 설정(소리·중요도)은 한 번 만들어지면 앱에서 바꿀 수 없으므로, 바꿔야 할 때는 ID를 새로 만든다.
    private const val CHANNEL_ID = "move_alarm_v1"
    private const val NOTIFICATION_ID = 1001

    // 알림 비트맵은 프로세스 간 전달 크기 제한이 있어 작게 만든다.
    private const val NOTIFICATION_PICTURE_PX = 600
    private const val LARGE_ICON_PX = 160

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(CHANNEL_ID, "움직임 알람", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "매시간 움직이라고 알려주는 알람"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, kind: AlarmKind) {
        val store = SettingsStore(context)
        try {
            ensureChannel(context)
            if (!canPostNotifications(context)) {
                store.recordError("알림 권한이 꺼져 있어 알람을 표시하지 못했어요")
                return
            }
            val photo = PhotoStore(context).pickForAlarm(store, store.load().photoOrder)
            val title = when (kind) {
                AlarmKind.TEST -> "테스트 알람이에요!"
                else -> "움직일 시간이에요!"
            }
            val text = when (kind) {
                AlarmKind.TEST -> "이 알림이 보이면 알람이 잘 작동하는 거예요."
                else -> "잠깐 일어나서 기지개를 켜볼까요?"
            }

            val screenIntent = AlarmActivity.intent(context, photo?.absolutePath, kind)
            val screenPi = PendingIntent.getActivity(
                context, 10, screenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val picture = photo?.let { PhotoStore.decodeFile(it, NOTIFICATION_PICTURE_PX, Bitmap.Config.RGB_565) }
                ?: defaultPicture(context, NOTIFICATION_PICTURE_PX)
            val largeIcon = photo?.let { PhotoStore.decodeFile(it, LARGE_ICON_PX) }
                ?: defaultPicture(context, LARGE_ICON_PX)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setLargeIcon(largeIcon)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(picture)
                        .bigLargeIcon(null as Bitmap?)
                        .setSummaryText(text),
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(screenPi)
                .setFullScreenIntent(screenPi, true)
                .addAction(0, "확인", receiverPi(context, AlarmReceiver.ACTION_DISMISS, 20))
                .addAction(0, "5분 뒤 다시", receiverPi(context, AlarmReceiver.ACTION_SNOOZE_REQUEST, 21))
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            store.recordError("알림 표시 실패: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun defaultPicture(context: Context, px: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.drawable.illust_move)!!
        val w = px
        val h = px * drawable.intrinsicHeight / drawable.intrinsicWidth
        return drawable.toBitmap(w, h)
    }

    private fun receiverPi(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
