package com.movealarm.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.alarm.AlarmKind
import java.io.File

@Composable
fun AlarmScreen(photo: File?, kind: AlarmKind, onConfirm: () -> Unit, onSnooze: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MoveColors.RingTop, MoveColors.RingBottom)))
            .systemBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(32.dp), color = MoveColors.Popup, shadowElevation = 12.dp, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MoveColors.PhotoBg),
                    contentAlignment = Alignment.Center,
                ) {
                    PhotoImage(
                        photo,
                        maxPx = 1440,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (photo == null) ContentScale.Crop else ContentScale.Fit,
                    )
                }
                Text(
                    if (kind == AlarmKind.TEST) "테스트 알람이에요!" else "움직일 시간이에요!",
                    fontFamily = Jua,
                    fontSize = 38.sp,
                    color = MoveColors.Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (kind == AlarmKind.TEST) "이 화면이 보이면 알람이 잘 작동하는 거예요."
                    else "잠깐 일어나서 기지개를 켜볼까요?",
                    fontSize = 21.sp,
                    color = MoveColors.Sub,
                    textAlign = TextAlign.Center,
                )
                BigButton("확인", onClick = onConfirm)
                TextButton(onClick = onSnooze, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text(
                        "5분 뒤 다시 알림",
                        fontSize = 20.sp,
                        color = MoveColors.Sub,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}
