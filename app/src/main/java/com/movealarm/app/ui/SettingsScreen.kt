package com.movealarm.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.R
import com.movealarm.app.data.AlarmSettings
import com.movealarm.app.data.PhotoOrder
import java.io.File

@Composable
fun SettingsScreen(
    settings: AlarmSettings,
    photos: List<File>,
    permissions: PermissionState,
    onBack: () -> Unit,
    onChange: (AlarmSettings) -> Unit,
    onOpenPhotos: () -> Unit,
    onTestAlarm: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixExactAlarm: () -> Unit,
) {
    var editingQuiet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(title = "무브알람", onBack = onBack) {
            Text(
                if (settings.enabled) "켜짐" else "꺼짐",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MoveColors.Ink,
                modifier = Modifier.padding(end = 10.dp),
            )
            Switch(
                checked = settings.enabled,
                onCheckedChange = { onChange(settings.copy(enabled = it)) },
                modifier = Modifier.scale(1.15f).semantics { contentDescription = "알람 켜기 끄기" },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MoveColors.Accent,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = MoveColors.Off,
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = MoveColors.OffDot,
                ),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeroCard(settings)
            MinuteCard(settings.minute) { onChange(settings.copy(minute = it)) }
            PhotosCard(photos, settings.photoOrder, onOpenPhotos)
            QuietCard(settings.quietStartHour, settings.quietEndHour) { editingQuiet = true }

            OutlinedButton(
                onClick = onTestAlarm,
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(2.dp, MoveColors.Accent),
            ) {
                Text("1분 뒤 테스트 알람 보내기", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MoveColors.Accent)
            }

            StatusFooter(settings, permissions, onFixNotifications, onFixExactAlarm)
        }
    }

    if (editingQuiet) {
        QuietHoursDialog(
            initialStart = settings.quietStartHour,
            initialEnd = settings.quietEndHour,
            onDismiss = { editingQuiet = false },
            onSave = { start, end ->
                editingQuiet = false
                onChange(settings.copy(quietStartHour = start, quietEndHour = end))
            },
        )
    }
}

@Composable
private fun HeroCard(settings: AlarmSettings) {
    MoveCard(color = MoveColors.Hero, borderColor = MoveColors.HeroBorder) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = MoveColors.HeroSub, modifier = Modifier.size(20.dp))
                Text("정각 기준 · 매시", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MoveColors.HeroSub)
            }
            Text("%02d분".format(settings.minute), fontFamily = Jua, fontSize = 64.sp, color = MoveColors.Ink)
            Text(
                if (settings.enabled) "에 알람이 울려요" else "알람이 꺼져 있어요",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = MoveColors.Ink,
            )
        }
    }
}

@Composable
private fun MinuteCard(minute: Int, onSelect: (Int) -> Unit) {
    MoveCard {
        SectionLabel("울리는 분 설정", trailing = "매시 반복")
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onSelect(Math.floorMod(minute - 1, 60)) }, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "1분 빼기", tint = MoveColors.Ink, modifier = Modifier.size(40.dp))
            }
            MinutePill(Math.floorMod(minute - 1, 60), active = false, onSelect)
            MinutePill(minute, active = true, onSelect)
            MinutePill(Math.floorMod(minute + 1, 60), active = false, onSelect)
            IconButton(onClick = { onSelect(Math.floorMod(minute + 1, 60)) }, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "1분 더하기", tint = MoveColors.Ink, modifier = Modifier.size(40.dp))
            }
        }
        Text("자주 쓰는 시간", fontSize = 17.sp, color = MoveColors.Sub)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 15, 30, 45).forEach { m ->
                val selected = m == minute
                Surface(
                    onClick = { onSelect(m) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MoveColors.Ink else MoveColors.PillBg,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "%02d분".format(m),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MoveColors.Background else MoveColors.Ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinutePill(minute: Int, active: Boolean, onSelect: (Int) -> Unit) {
    Surface(
        onClick = { onSelect(minute) },
        modifier = Modifier.size(if (active) 76.dp else 58.dp),
        shape = CircleShape,
        color = if (active) MoveColors.Ink else MoveColors.PillBg,
        shadowElevation = if (active) 3.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "%02d".format(minute),
                fontFamily = Jua,
                fontSize = if (active) 32.sp else 24.sp,
                color = if (active) MoveColors.Background else MoveColors.Sub,
            )
        }
    }
}

@Composable
private fun PhotosCard(photos: List<File>, order: PhotoOrder, onOpen: () -> Unit) {
    MoveCard(onClick = onOpen) {
        SectionLabel("알람 사진", icon = R.drawable.ic_photo, trailing = "바꾸기 ›")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (photos.isEmpty()) {
                Thumb(null, 72.dp)
                Text("등록된 사진이 없어요\n지금은 기본 그림이 나와요", fontSize = 18.sp, color = MoveColors.Sub)
            } else {
                photos.take(3).forEach { Thumb(it, 72.dp) }
                if (photos.size > 3) {
                    Text("+${photos.size - 3}", fontFamily = Jua, fontSize = 26.sp, color = MoveColors.Sub)
                }
            }
        }
        if (photos.isNotEmpty()) {
            val orderText = if (order == PhotoOrder.SEQUENTIAL) "순서대로" else "랜덤으로"
            Text("${photos.size}장 등록됨 · $orderText 보여줘요", fontSize = 18.sp, color = MoveColors.Sub)
        }
    }
}

@Composable
private fun QuietCard(start: Int, end: Int, onEdit: () -> Unit) {
    MoveCard(onClick = onEdit) {
        SectionLabel("무음 시간대", icon = R.drawable.ic_moon, trailing = "바꾸기 ›")
        if (start == end) {
            Text("없음", fontFamily = Jua, fontSize = 32.sp, color = MoveColors.Ink)
            Text("하루 종일 매시간 알람이 울려요", fontSize = 18.sp, color = MoveColors.Sub)
        } else {
            Text("%02d:00 ~ %02d:00".format(start, end), fontFamily = Jua, fontSize = 32.sp, color = MoveColors.Ink)
            QuietTrack(start, end)
            Text("이 시간엔 알람이 울리지 않아요", fontSize = 18.sp, color = MoveColors.Sub)
        }
    }
}

/** 하루 24시간 막대 위에 무음 구간을 칠한다. */
@Composable
fun QuietTrack(start: Int, end: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            val radius = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(MoveColors.TrackBg, cornerRadius = radius)
            fun segment(from: Int, to: Int) {
                val x0 = size.width * from / 24f
                val x1 = size.width * to / 24f
                drawRoundRect(MoveColors.Accent, topLeft = Offset(x0, 0f), size = Size(x1 - x0, size.height), cornerRadius = radius)
            }
            if (start < end) segment(start, end) else if (start > end) { segment(start, 24); segment(0, end) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("0시", "6시", "12시", "18시", "24시").forEachIndexed { i, label ->
                Text(
                    label,
                    fontSize = 14.sp,
                    color = MoveColors.Sub,
                    modifier = Modifier.weight(1f),
                    textAlign = when (i) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center },
                )
            }
        }
    }
}
