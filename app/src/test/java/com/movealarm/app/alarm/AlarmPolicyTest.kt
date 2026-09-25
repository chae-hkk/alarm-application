package com.movealarm.app.alarm

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPolicyTest {

    @Test fun `소리 모드면 소리와 진동`() {
        assertEquals(RingFeedback(sound = true, vibrate = true), AlarmPolicy.feedbackFor(AudioManager.RINGER_MODE_NORMAL))
    }

    @Test fun `진동 모드면 진동만`() {
        assertEquals(RingFeedback(sound = false, vibrate = true), AlarmPolicy.feedbackFor(AudioManager.RINGER_MODE_VIBRATE))
    }

    @Test fun `무음 모드면 소리도 진동도 없음`() {
        assertEquals(RingFeedback(sound = false, vibrate = false), AlarmPolicy.feedbackFor(AudioManager.RINGER_MODE_SILENT))
    }

    @Test fun `폰을 사용 중이면 작은 알림창`() {
        assertFalse(AlarmPolicy.useFullScreen(screenOn = true, keyguardLocked = false))
    }

    @Test fun `화면이 꺼져 있으면 전체 화면`() {
        assertTrue(AlarmPolicy.useFullScreen(screenOn = false, keyguardLocked = true))
        assertTrue(AlarmPolicy.useFullScreen(screenOn = false, keyguardLocked = false))
    }

    @Test fun `화면은 켜졌지만 잠금 화면이면 전체 화면`() {
        assertTrue(AlarmPolicy.useFullScreen(screenOn = true, keyguardLocked = true))
    }

    @Test fun `15초 동안 울린다`() {
        assertEquals(15_000L, AlarmPolicy.RING_DURATION_MS)
    }
}
