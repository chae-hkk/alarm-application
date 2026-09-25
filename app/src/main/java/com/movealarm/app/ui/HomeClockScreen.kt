package com.movealarm.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.R
import com.movealarm.app.alarm.AlarmTimeCalculator
import com.movealarm.app.data.AlarmSettings
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DATE = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

@Composable
fun HomeClockScreen(
    settings: AlarmSettings,
    permissions: PermissionState,
    onOpenSettings: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixExactAlarm: () -> Unit,
) {
    val now = rememberNow()
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(MoveColors.ClockCenter, MoveColors.ClockEdge),
                        center = Offset(size.width / 2f, size.height * 0.28f),
                        radius = max(size.width, size.height) * 0.78f,
                    ),
                )
            }
            .systemBarsPadding(),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(now.format(TIME), fontFamily = Jua, fontSize = 96.sp, color = MoveColors.Ink)
                Text(now.format(DATE), fontSize = 26.sp, color = MoveColors.Sub)
                VSpace(24.dp)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MoveColors.Card,
                    border = BorderStroke(1.dp, MoveColors.CardBorder),
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = MoveColors.Ink, modifier = Modifier.size(26.dp))
                        val label = if (settings.enabled) {
                            val next = AlarmTimeCalculator.nextAlarm(now, settings.minute, settings.quietStartHour, settings.quietEndHour)
                            "다음 알람 · ${formatNextAlarm(next, now)}"
                        } else {
                            "알람이 꺼져 있어요"
                        }
                        Text(label, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink)
                    }
                }
            }
            StatusFooter(settings, permissions, onFixNotifications, onFixExactAlarm)
            VSpace(12.dp)
            BigButton("알람 설정하기", onClick = onOpenSettings, icon = R.drawable.ic_bell)
        }
    }
}
