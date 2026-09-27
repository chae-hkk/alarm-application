package com.movealarm.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.R
import com.movealarm.app.data.QuietPeriod
import java.time.DayOfWeek

fun quietTimeText(period: QuietPeriod): String =
    if (period.isAllDay) "하루 종일" else "%02d:00 ~ %02d:00".format(period.startHour, period.endHour)

/** 설정 화면 카드 안의 한 줄 요약: "매일  22:00 ~ 08:00" */
@Composable
fun QuietPeriodSummary(period: QuietPeriod) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = MoveColors.PillBg) {
            Text(
                QuietPeriod.daysLabel(period.days),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MoveColors.Ink,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(quietTimeText(period), fontFamily = Jua, fontSize = 26.sp, color = MoveColors.Ink)
    }
}

@Composable
fun QuietScreen(
    periods: List<QuietPeriod>,
    onBack: () -> Unit,
    onChange: (List<QuietPeriod>) -> Unit,
) {
    // null: 닫힘, -1: 새로 추가, 0 이상: 해당 항목 수정
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(title = "무음 시간대", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "아래 시간에는 알람이 울리지 않아요.\n예) 잠자는 시간, 평일 회의 시간, 주말",
                fontSize = 19.sp,
                color = MoveColors.Sub,
            )
            if (periods.isEmpty()) {
                MoveCard {
                    Text("무음 시간이 없어요", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink)
                    Text("매일 매시간 알람이 울려요.", fontSize = 18.sp, color = MoveColors.Sub)
                }
            }
            periods.forEachIndexed { index, period ->
                MoveCard(onClick = { editingIndex = index }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(QuietPeriod.daysLabel(period.days), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink)
                            Text(quietTimeText(period), fontFamily = Jua, fontSize = 32.sp, color = MoveColors.Ink)
                        }
                        Text("바꾸기 ›", fontSize = 17.sp, color = MoveColors.Sub)
                    }
                    QuietTrack(period.startHour, period.endHour)
                }
            }
            if (periods.size < QuietPeriod.MAX_COUNT) {
                BigButton("무음 시간 추가", onClick = { editingIndex = -1 }, icon = R.drawable.ic_add)
            }
        }
    }

    editingIndex?.let { index ->
        val isNew = index < 0
        QuietPeriodDialog(
            initial = if (isNew) QuietPeriod(12, 13, QuietPeriod.WEEKDAYS) else periods[index],
            isNew = isNew,
            onDismiss = { editingIndex = null },
            onSave = { edited ->
                editingIndex = null
                onChange(if (isNew) periods + edited else periods.toMutableList().also { it[index] = edited })
            },
            onDelete = {
                editingIndex = null
                onChange(periods.filterIndexed { i, _ -> i != index })
            },
        )
    }
}

@Composable
private fun QuietPeriodDialog(
    initial: QuietPeriod,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (QuietPeriod) -> Unit,
    onDelete: () -> Unit,
) {
    var start by remember { mutableIntStateOf(initial.startHour) }
    var end by remember { mutableIntStateOf(initial.endHour) }
    var days by remember { mutableStateOf(initial.days) }
    val allDay = start == end

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoveColors.Popup,
        title = { Text(if (isNew) "무음 시간 추가" else "무음 시간 바꾸기", fontFamily = Jua, fontSize = 28.sp, color = MoveColors.Ink) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("요일", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val on = day in days
                        val name = QuietPeriod.shortName(day)
                        Surface(
                            onClick = { days = if (on) days - day else days + day },
                            shape = CircleShape,
                            color = if (on) MoveColors.Ink else MoveColors.PillBg,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .semantics {
                                    selected = on
                                    contentDescription = "${name}요일 ${if (on) "선택됨" else "선택 안 됨"}"
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    name,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (on) MoveColors.Background else MoveColors.Ink,
                                )
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresetChip("매일", days == QuietPeriod.EVERY_DAY, Modifier.weight(1f)) { days = QuietPeriod.EVERY_DAY }
                    PresetChip("평일", days == QuietPeriod.WEEKDAYS, Modifier.weight(1f)) { days = QuietPeriod.WEEKDAYS }
                    PresetChip("주말", days == QuietPeriod.WEEKEND, Modifier.weight(1f)) { days = QuietPeriod.WEEKEND }
                }

                Text("시간", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink)
                PresetChip("하루 종일", allDay, Modifier.fillMaxWidth()) {
                    if (allDay) { start = 22; end = 8 } else { start = 0; end = 0 }
                }
                if (!allDay) {
                    HourStepper("시작", start, avoid = end) { start = it }
                    HourStepper("끝", end, avoid = start) { end = it }
                }
                QuietTrack(start, end)

                Text(
                    when {
                        days.isEmpty() -> "요일을 하나 이상 골라주세요."
                        allDay -> "${QuietPeriod.daysLabel(days)}에는\n하루 종일 알람이 울리지 않아요."
                        start > end -> "${QuietPeriod.daysLabel(days)} ${start}시부터\n다음날 ${end}시까지 울리지 않아요."
                        else -> "${QuietPeriod.daysLabel(days)} ${start}시부터\n${end}시까지 울리지 않아요."
                    },
                    fontSize = 19.sp,
                    color = if (days.isEmpty()) MoveColors.Accent else MoveColors.Ink,
                    fontWeight = if (days.isEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!isNew) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text("이 무음 시간 지우기", fontSize = 19.sp, color = MoveColors.Accent)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(QuietPeriod(start, end, days)) },
                enabled = days.isNotEmpty(),
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text(
                    "저장",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (days.isNotEmpty()) MoveColors.Accent else MoveColors.OffDot,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("취소", fontSize = 22.sp, color = MoveColors.Sub)
            }
        },
    )
}

@Composable
private fun PresetChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MoveColors.Ink else MoveColors.Card,
        border = BorderStroke(1.dp, if (selected) MoveColors.Ink else MoveColors.CardBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MoveColors.Background else MoveColors.Ink,
            )
        }
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, avoid: Int, onChange: (Int) -> Unit) {
    // 시작과 끝이 같아지면 "하루 종일"이 되므로, 같은 시각은 건너뛴다.
    fun step(delta: Int): Int {
        val next = Math.floorMod(hour + delta, 24)
        return if (next == avoid) Math.floorMod(next + delta, 24) else next
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink, modifier = Modifier.width(56.dp))
        StepButton(R.drawable.ic_remove, "$label 시각 1시간 빼기") { onChange(step(-1)) }
        Text(
            "%02d시".format(hour),
            fontFamily = Jua,
            fontSize = 34.sp,
            color = MoveColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepButton(R.drawable.ic_add, "$label 시각 1시간 더하기") { onChange(step(1)) }
    }
}

@Composable
private fun StepButton(icon: Int, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MoveColors.PillBg, modifier = Modifier.size(56.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = description, tint = MoveColors.Ink, modifier = Modifier.size(30.dp))
        }
    }
}
