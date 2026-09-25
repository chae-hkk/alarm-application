package com.movealarm.app.alarm

import android.media.AudioManager

data class RingFeedback(val sound: Boolean, val vibrate: Boolean)

object AlarmPolicy {

    const val RING_DURATION_MS = 15_000L

    /** 휴대폰의 소리 모드를 따른다: 소리 → 소리+진동, 진동 → 진동만, 무음 → 둘 다 없음(화면·알림만). */
    fun feedbackFor(ringerMode: Int): RingFeedback = when (ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> RingFeedback(sound = true, vibrate = true)
        AudioManager.RINGER_MODE_VIBRATE -> RingFeedback(sound = false, vibrate = true)
        else -> RingFeedback(sound = false, vibrate = false)
    }

    /** 폰을 쓰고 있으면(화면 켜짐 + 잠금 해제) 작은 알림창만, 잠겨 있거나 화면이 꺼져 있으면 전체 화면. */
    fun useFullScreen(screenOn: Boolean, keyguardLocked: Boolean): Boolean = !screenOn || keyguardLocked
}
