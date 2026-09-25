package com.movealarm.app.data

import android.content.Context
import androidx.core.content.edit

enum class PhotoOrder { SEQUENTIAL, RANDOM }

data class AlarmSettings(
    val enabled: Boolean = true,
    val minute: Int = 0,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 8,
    val photoOrder: PhotoOrder = PhotoOrder.SEQUENTIAL,
)

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("move_alarm", Context.MODE_PRIVATE)

    fun load(): AlarmSettings {
        val defaults = AlarmSettings()
        return AlarmSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, defaults.enabled),
            minute = prefs.getInt(KEY_MINUTE, defaults.minute).coerceIn(0, 59),
            quietStartHour = prefs.getInt(KEY_QUIET_START, defaults.quietStartHour).coerceIn(0, 23),
            quietEndHour = prefs.getInt(KEY_QUIET_END, defaults.quietEndHour).coerceIn(0, 23),
            photoOrder = runCatching {
                PhotoOrder.valueOf(prefs.getString(KEY_PHOTO_ORDER, null) ?: defaults.photoOrder.name)
            }.getOrDefault(defaults.photoOrder),
        )
    }

    fun save(settings: AlarmSettings) = prefs.edit {
        putBoolean(KEY_ENABLED, settings.enabled)
        putInt(KEY_MINUTE, settings.minute)
        putInt(KEY_QUIET_START, settings.quietStartHour)
        putInt(KEY_QUIET_END, settings.quietEndHour)
        putString(KEY_PHOTO_ORDER, settings.photoOrder.name)
    }

    var sequentialNextIndex: Int
        get() = prefs.getInt(KEY_SEQ_NEXT, 0)
        set(value) = prefs.edit { putInt(KEY_SEQ_NEXT, value) }

    var lastShownIndex: Int
        get() = prefs.getInt(KEY_LAST_SHOWN, -1)
        set(value) = prefs.edit { putInt(KEY_LAST_SHOWN, value) }

    var askedNotificationPermission: Boolean
        get() = prefs.getBoolean(KEY_ASKED_NOTIF, false)
        set(value) = prefs.edit { putBoolean(KEY_ASKED_NOTIF, value) }

    // 상태 기록 (원격 진단용 상태 화면에서 사용)
    val lastScheduledAt: Long get() = prefs.getLong(KEY_LAST_SCHEDULED, 0L)
    val nextAlarmAt: Long get() = prefs.getLong(KEY_NEXT_AT, 0L)
    val lastFiredAt: Long get() = prefs.getLong(KEY_LAST_FIRED, 0L)
    val lastError: String? get() = prefs.getString(KEY_LAST_ERROR, null)
    val lastErrorAt: Long get() = prefs.getLong(KEY_LAST_ERROR_AT, 0L)

    fun recordScheduled(nextAt: Long) = prefs.edit {
        putLong(KEY_LAST_SCHEDULED, System.currentTimeMillis())
        putLong(KEY_NEXT_AT, nextAt)
    }

    fun clearNextAlarm() = prefs.edit { putLong(KEY_NEXT_AT, 0L) }

    fun recordFired() = prefs.edit { putLong(KEY_LAST_FIRED, System.currentTimeMillis()) }

    fun recordError(message: String) = prefs.edit {
        putString(KEY_LAST_ERROR, message)
        putLong(KEY_LAST_ERROR_AT, System.currentTimeMillis())
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_MINUTE = "minute"
        const val KEY_QUIET_START = "quiet_start"
        const val KEY_QUIET_END = "quiet_end"
        const val KEY_PHOTO_ORDER = "photo_order"
        const val KEY_SEQ_NEXT = "seq_next"
        const val KEY_LAST_SHOWN = "last_shown"
        const val KEY_ASKED_NOTIF = "asked_notif"
        const val KEY_LAST_SCHEDULED = "last_scheduled"
        const val KEY_NEXT_AT = "next_at"
        const val KEY_LAST_FIRED = "last_fired"
        const val KEY_LAST_ERROR = "last_error"
        const val KEY_LAST_ERROR_AT = "last_error_at"
    }
}
