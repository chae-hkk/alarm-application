package com.movealarm.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.movealarm.app.data.SettingsStore

/**
 * 알람이 울리는 동안(최대 15초)만 도는 포그라운드 서비스.
 * 휴대폰 소리 모드에 맞춰 소리/진동을 직접 반복하고, 15초가 지나면 멈춘 뒤 알림만 남긴다.
 */
class AlarmRingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish(keepNotification = true) }
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var content: AlarmContent? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        current = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val kind = AlarmKind.from(intent?.getStringExtra(EXTRA_KIND))
        stopFeedback()
        AlarmNotifier.ensureChannels(this)
        val prepared = AlarmNotifier.prepareContent(this, kind)
        content = prepared
        val notification = AlarmNotifier.buildRinging(this, prepared, AlarmNotifier.shouldUseFullScreen(this))
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(AlarmNotifier.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            } else {
                startForeground(AlarmNotifier.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            SettingsStore(this).recordError("알람 소리 서비스 시작 실패 (알림만 표시): ${e.javaClass.simpleName} ${e.message}")
            AlarmNotifier.showFallback(this, prepared)
            stopSelf()
            return START_NOT_STICKY
        }
        startFeedback()
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, AlarmPolicy.RING_DURATION_MS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        stopFeedback()
        if (current === this) current = null
        super.onDestroy()
    }

    private fun finish(keepNotification: Boolean) {
        handler.removeCallbacks(timeout)
        stopFeedback()
        if (keepNotification) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            content?.let { AlarmNotifier.notifyQuiet(this, it) }
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            AlarmNotifier.cancel(this)
        }
        stopSelf()
    }

    private fun startFeedback() {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "movealarm:ring")
            .apply { acquire(AlarmPolicy.RING_DURATION_MS + 5_000L) }

        val ringerMode = getSystemService(AudioManager::class.java).ringerMode
        val feedback = AlarmPolicy.feedbackFor(ringerMode)
        if (feedback.sound) playSound()
        if (feedback.vibrate) vibrate()
    }

    private fun playSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(ALARM_AUDIO)
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            player?.release()
            player = null
            SettingsStore(this).recordError("알람 소리 재생 실패: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (v == null || !v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, ALARM_AUDIO)
        }
        vibrator = v
    }

    private fun stopFeedback() {
        player?.runCatching { stop(); release() }
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.runCatching { if (isHeld) release() }
        wakeLock = null
    }

    companion object {
        private const val EXTRA_KIND = "kind"
        private val VIBRATION_PATTERN = longArrayOf(0, 800, 400)
        private val ALARM_AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 수신기와 알람 화면이 같은 프로세스의 메인 스레드에서 울림을 멈출 수 있도록 현재 인스턴스를 들고 있는다.
        @Volatile
        private var current: AlarmRingService? = null

        fun ringIntent(context: Context, kind: AlarmKind): Intent =
            Intent(context, AlarmRingService::class.java).putExtra(EXTRA_KIND, kind.name)

        /** 울림을 멈추고 알림도 지운다 (확인 / 5분 뒤 / 알림 밀어서 지우기). */
        fun stop(context: Context) {
            current?.finish(keepNotification = false) ?: AlarmNotifier.cancel(context)
        }
    }
}
