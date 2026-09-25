package com.movealarm.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.R
import com.movealarm.app.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MoveCard(
    modifier: Modifier = Modifier,
    color: Color = MoveColors.Card,
    borderColor: Color = MoveColors.CardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val border = BorderStroke(1.dp, borderColor)
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border, content = inner)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border, content = inner)
    }
}

@Composable
fun SectionLabel(text: String, @DrawableRes icon: Int? = null, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(painterResource(icon), contentDescription = null, tint = MoveColors.Ink, modifier = Modifier.size(24.dp))
        Text(text, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = 17.sp, color = MoveColors.Sub)
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, @DrawableRes icon: Int? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = MoveColors.Accent, contentColor = Color.White),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
            Box(Modifier.size(8.dp))
        }
        Text(text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
            Icon(painterResource(R.drawable.ic_back), contentDescription = "뒤로 가기", tint = MoveColors.Ink, modifier = Modifier.size(30.dp))
        }
        Text(title, fontFamily = Jua, fontSize = 28.sp, color = MoveColors.Ink, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun StatusDot(color: Color) {
    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
}

/** 1초마다 갱신되는 현재 시각 */
@Composable
fun rememberNow(): ZonedDateTime {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000L - now.nano / 1_000_000 % 1000)
        }
    }
    return now
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

fun formatNextAlarm(next: ZonedDateTime, now: ZonedDateTime): String {
    val days = next.toLocalDate().toEpochDay() - now.toLocalDate().toEpochDay()
    val time = next.format(HHMM)
    return when (days) {
        0L -> time
        1L -> "내일 $time"
        else -> next.format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"))
    }
}

/** 저장된 사진을 백그라운드에서 읽어 보여준다. 읽는 동안/실패 시 기본 그림. */
@Composable
fun PhotoImage(file: File?, maxPx: Int, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file) {
        value = file?.let { f -> withContext(Dispatchers.IO) { PhotoStore.decodeFile(f, maxPx)?.asImageBitmap() } }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bmp, contentDescription = "알람 사진", modifier = modifier, contentScale = contentScale)
    } else {
        Image(painterResource(R.drawable.illust_move), contentDescription = if (file == null) "기본 그림" else null, modifier = modifier, contentScale = contentScale)
    }
}

@Composable
fun Thumb(file: File?, size: Dp) {
    PhotoImage(file, maxPx = 256, modifier = Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(MoveColors.PhotoBg))
}

@Composable
fun VSpace(height: Dp) = Box(Modifier.height(height))
