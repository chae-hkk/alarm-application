package com.movealarm.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.R

@Composable
fun QuietHoursDialog(
    initialStart: Int,
    initialEnd: Int,
    onDismiss: () -> Unit,
    onSave: (start: Int, end: Int) -> Unit,
) {
    var start by remember { mutableIntStateOf(initialStart) }
    var end by remember { mutableIntStateOf(initialEnd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MoveColors.Popup,
        title = { Text("무음 시간대", fontFamily = Jua, fontSize = 30.sp, color = MoveColors.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HourStepper("시작", start) { start = it }
                HourStepper("끝", end) { end = it }
                Text(
                    if (start == end) "무음 시간이 없어요.\n하루 종일 알람이 울려요."
                    else "%d시부터 %d시까지는\n알람이 울리지 않아요.".format(start, end),
                    fontSize = 20.sp,
                    color = MoveColors.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (start != end) QuietTrack(start, end)
                TextButton(onClick = { end = start }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("무음 시간 없애기", fontSize = 19.sp, color = MoveColors.Sub)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(start, end) }, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("저장", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MoveColors.Accent)
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
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink, modifier = Modifier.width(56.dp))
        StepButton(R.drawable.ic_remove, "$label 시각 1시간 빼기") { onChange(Math.floorMod(hour - 1, 24)) }
        Text(
            "%02d시".format(hour),
            fontFamily = Jua,
            fontSize = 34.sp,
            color = MoveColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepButton(R.drawable.ic_add, "$label 시각 1시간 더하기") { onChange(Math.floorMod(hour + 1, 24)) }
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
