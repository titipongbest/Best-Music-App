package com.bestmusic.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestmusic.app.MainViewModel
import com.bestmusic.app.data.Lyrics
import com.bestmusic.app.data.rememberPlayerColors

/**
 * เนื้อเพลง: อ่านจากไฟล์ .lrc/.txt ชื่อเดียวกับเพลง หรือที่ผู้ใช้วางเอง
 * ถ้าเป็น LRC (มีเวลากำกับ) จะไฮไลต์บรรทัดตามเพลง และแตะบรรทัดเพื่อข้ามไปได้
 */
@Composable
fun LyricsScreen(vm: MainViewModel, onClose: () -> Unit) {
    val track by vm.currentTrack.collectAsState()
    val state by vm.playerState.collectAsState()
    val batterySaver by vm.prefs.batterySaver.collectAsState()
    val palette by rememberPlayerColors(track, batterySaver)
    val bg by animateColorAsState(palette.background, label = "bg")

    var raw by remember(track?.key) { mutableStateOf<String?>(null) }
    var loaded by remember(track?.key) { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(track?.key, reloadTick) {
        val t = track ?: return@LaunchedEffect
        raw = vm.loadLyrics(t)
        loaded = true
    }
    val lyrics = remember(raw) { raw?.let { Lyrics.parse(it) } }
    val active = lyrics?.activeIndex(state.positionMs) ?: -1
    val listState = rememberLazyListState()

    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem((active - 3).coerceAtLeast(0))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, "ปิด", tint = palette.secondary, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    track?.title ?: "", color = palette.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(track?.artist ?: "", color = palette.secondary, fontSize = 14.sp, maxLines = 1)
            }
            IconButton(onClick = { editing = true }) {
                Icon(Icons.Outlined.Edit, "แก้ไขเนื้อเพลง", tint = palette.secondary)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                !loaded -> Unit
                lyrics == null || lyrics.lines.isEmpty() -> Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("ไม่พบเนื้อเพลง", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "วางไฟล์ .lrc ชื่อเดียวกับเพลงไว้ในโฟลเดอร์เดียวกัน\nหรือกดปุ่มด้านล่างเพื่อวางเนื้อเพลงเอง",
                        color = palette.secondary, fontSize = 14.sp, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { editing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
                    ) { Text("เพิ่มเนื้อเพลง") }
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(lyrics.lines) { i, line ->
                        val isActive = i == active
                        val alpha by animateFloatAsState(
                            when {
                                !lyrics.synced -> 0.9f
                                isActive -> 1f
                                i < active -> 0.35f
                                else -> 0.5f
                            },
                            label = "a",
                        )
                        Text(
                            line.text.ifBlank { "♪" },
                            color = if (isActive) palette.accent else Color.White,
                            fontSize = if (lyrics.synced) 26.sp else 20.sp,
                            lineHeight = if (lyrics.synced) 34.sp else 30.sp,
                            fontWeight = if (lyrics.synced) FontWeight.ExtraBold else FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { this.alpha = alpha }
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = lyrics.synced) { vm.seekTo(line.timeMs) },
                        )
                    }
                }
            }
        }

        // ---- mini controls ----
        Column(Modifier.padding(horizontal = 28.dp, vertical = 12.dp)) {
            SeekBar(state.positionMs, state.durationMs, palette.accent, vm::seekTo)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(state.positionMs), color = palette.secondary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                        .clickable { vm.playOrPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null, tint = bg, modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(formatTime(state.durationMs), color = palette.secondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.width(1.dp))
    }

    if (editing) {
        var text by remember { mutableStateOf(raw ?: "") }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("เนื้อเพลง") },
            text = {
                Column {
                    Text("วางเนื้อเพลงธรรมดา หรือรูปแบบ LRC เช่น [00:12.30] ...", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        text, { text = it },
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    track?.let { vm.saveLyrics(it, text) }
                    editing = false
                    reloadTick++
                }) { Text("บันทึก") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("ยกเลิก") } },
        )
    }
}
