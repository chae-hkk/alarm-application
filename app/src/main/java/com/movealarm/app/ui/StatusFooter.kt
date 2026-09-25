package com.movealarm.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.alarm.AlarmTimeCalculator
import com.movealarm.app.data.AlarmSettings

data class PermissionState(val notificationsAllowed: Boolean, val exactAlarmAllowed: Boolean)

@Composable
fun StatusFooter(
    settings: AlarmSettings,
    permissions: PermissionState,
    onFixNotifications: () -> Unit,
    onFixExactAlarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow()
    val (dot, text, onClick) = when {
        !permissions.notificationsAllowed ->
            Triple(MoveColors.Accent, "알림이 꺼져 있어요 · 여기를 눌러 켜주세요", onFixNotifications)
        !settings.enabled ->
            Triple(MoveColors.OffDot, "알람이 꺼져 있어요", null)
        !permissions.exactAlarmAllowed ->
            Triple(MoveColors.Accent, "정확한 알람 허용이 필요해요 · 여기를 눌러주세요", onFixExactAlarm)
        else -> {
            val next = AlarmTimeCalculator.nextAlarm(now, settings.minute, settings.quietStartHour, settings.quietEndHour)
            Triple(MoveColors.Ok, "정상 작동 중 · 다음 알람 ${formatNextAlarm(next, now)}", null)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(dot)
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = if (onClick != null) FontWeight.Bold else FontWeight.Normal,
            color = if (onClick != null) MoveColors.Accent else MoveColors.Sub,
            textAlign = TextAlign.Center,
        )
    }
}
