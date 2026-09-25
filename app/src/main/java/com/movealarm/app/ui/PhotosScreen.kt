package com.movealarm.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movealarm.app.R
import com.movealarm.app.data.PhotoOrder
import java.io.File

@Composable
fun PhotosScreen(
    photos: List<File>,
    order: PhotoOrder,
    nextSequentialIndex: Int,
    importing: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (File) -> Unit,
    onOrderChange: (PhotoOrder) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Pair<Int, File>?>(null) }
    val nextIndex = if (photos.isEmpty()) -1 else Math.floorMod(nextSequentialIndex, photos.size)

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TopBar(title = "알람 사진", onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MoveCard {
                    SectionLabel("사진 보여주는 순서")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OrderOption("순서대로", order == PhotoOrder.SEQUENTIAL, Modifier.weight(1f)) { onOrderChange(PhotoOrder.SEQUENTIAL) }
                        OrderOption("랜덤으로", order == PhotoOrder.RANDOM, Modifier.weight(1f)) { onOrderChange(PhotoOrder.RANDOM) }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (importing) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = MoveColors.Accent, modifier = Modifier.size(32.dp))
                        Text("사진을 저장하는 중이에요…", fontSize = 21.sp, color = MoveColors.Ink)
                    }
                } else {
                    BigButton("갤러리에서 사진 추가", onClick = onAdd, icon = R.drawable.ic_add)
                }
            }
            if (photos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MoveCard {
                        Box(Modifier.fillMaxWidth().aspectRatio(200f / 180f).clip(RoundedCornerShape(16.dp))) {
                            PhotoImage(null, 0, Modifier.fillMaxSize())
                        }
                        Text(
                            "아직 등록된 사진이 없어요.\n사진이 없으면 이 기본 그림이 알람과 함께 나와요.",
                            fontSize = 19.sp,
                            color = MoveColors.Sub,
                        )
                    }
                }
            }
            itemsIndexed(photos, key = { _, f -> f.name }) { index, file ->
                PhotoTile(
                    number = index + 1,
                    file = file,
                    isNext = order == PhotoOrder.SEQUENTIAL && index == nextIndex,
                    onDelete = { pendingDelete = (index + 1) to file },
                )
            }
        }
    }

    pendingDelete?.let { (number, file) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = MoveColors.Popup,
            title = { Text("${number}번 사진을 지울까요?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MoveColors.Ink) },
            text = { Text("앱에서만 지워지고, 갤러리의 원본 사진은 그대로 있어요.", fontSize = 19.sp, color = MoveColors.Sub) },
            confirmButton = {
                TextButton(onClick = { onDelete(file); pendingDelete = null }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("지우기", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MoveColors.Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("취소", fontSize = 22.sp, color = MoveColors.Sub)
                }
            },
        )
    }
}

@Composable
private fun OrderOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MoveColors.Ink else MoveColors.PillBg,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MoveColors.Background, modifier = Modifier.size(24.dp))
                Box(Modifier.size(6.dp))
            }
            Text(label, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = if (selected) MoveColors.Background else MoveColors.Ink)
        }
    }
}

@Composable
private fun PhotoTile(number: Int, file: File, isNext: Boolean, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(MoveColors.PhotoBg)) {
        PhotoImage(file, maxPx = 512, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.padding(8.dp).size(36.dp).clip(CircleShape).background(MoveColors.Ink).align(Alignment.TopStart),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MoveColors.Background)
        }
        Surface(
            onClick = onDelete,
            shape = CircleShape,
            color = MoveColors.Card,
            shadowElevation = 2.dp,
            modifier = Modifier.padding(6.dp).size(48.dp).align(Alignment.TopEnd),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "${number}번 사진 지우기", tint = MoveColors.Ink, modifier = Modifier.size(26.dp))
            }
        }
        if (isNext) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(MoveColors.Ink.copy(alpha = 0.82f)).padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("다음 알람 사진", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MoveColors.Background)
            }
        }
    }
}
