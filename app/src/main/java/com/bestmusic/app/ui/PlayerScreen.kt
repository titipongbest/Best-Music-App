package com.bestmusic.app.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestmusic.app.MainViewModel
import com.bestmusic.app.data.PlayerColors
import com.bestmusic.app.data.Track
import com.bestmusic.app.data.rememberPlayerColors
import com.bestmusic.app.playback.PlayMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** หน้าเล่นเพลงแบบในภาพตัวอย่าง: ปกใหญ่มุมโค้ง, ชื่อเพลงตัวหนาสีตามปก, แถบเวลาบาง, ปุ่มควบคุมใหญ่ */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: MainViewModel,
    actions: ActivityActions,
    onClose: () -> Unit,
    onFullscreenVideo: () -> Unit,
    onMore: (Track, PlayerColors) -> Unit,
) {
    val track by vm.currentTrack.collectAsState()
    val state by vm.playerState.collectAsState()
    val favorites by vm.prefs.favorites.collectAsState()
    val batterySaver by vm.prefs.batterySaver.collectAsState()
    val palette by rememberPlayerColors(track, batterySaver)
    val context = LocalContext.current

    val bg by animateColorAsState(palette.background, tween(600), label = "bg")
    val accent by animateColorAsState(palette.accent, tween(600), label = "accent")
    val colors = palette.copy(background = bg, accent = accent)

    var showVideo by rememberSaveable(track?.key) { mutableStateOf(true) }
    var showQueue by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > with(density) { 140.dp.toPx() }) onClose()
                        dragY = 0f
                    },
                    onDragCancel = { dragY = 0f },
                ) { _, delta -> dragY = (dragY + delta).coerceAtLeast(0f) }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (dragY * 0.5f).roundToInt()) }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            Spacer(Modifier.height(36.dp))

            // ---------------- Artwork / MV ----------------
            val isVideo = track?.isVideo == true
            val videoOn = isVideo && showVideo && !batterySaver
            val maxArt = (LocalConfiguration.current.screenHeightDp * 0.46f).dp
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .heightIn(max = maxArt)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = track?.key to videoOn,
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                    label = "art",
                ) { (_, video) ->
                    if (video) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            InlineVideo(
                                vm.player.controller,
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(state.videoAspect.coerceIn(0.5f, 2.4f))
                                    .clip(RoundedCornerShape(28.dp)),
                            )
                        }
                    } else {
                        Artwork(
                            track,
                            Modifier
                                .fillMaxSize()
                                .pointerInput(track?.key) {
                                    var total = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { total = 0f },
                                        onDragEnd = {
                                            if (total < -120) vm.next() else if (total > 120) vm.previous()
                                        },
                                    ) { _, d -> total += d }
                                },
                            shape = RoundedCornerShape(28.dp),
                            sizePx = 1024,
                            iconSize = 96.dp,
                        )
                    }
                }
                if (isVideo && !batterySaver) {
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PillButton(
                            icon = if (showVideo) Icons.Rounded.Image else null,
                            text = if (showVideo) "ปก" else "MV",
                        ) { showVideo = !showVideo }
                        if (showVideo) PillButton(icon = Icons.Rounded.Fullscreen, text = null, onClick = onFullscreenVideo)
                    }
                }
            }
            }

            Spacer(Modifier.height(30.dp))

            // ---------------- Title / artist ----------------
            Text(
                track?.title ?: "ยังไม่ได้เลือกเพลง",
                color = accent,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    track?.artist ?: "",
                    color = colors.secondary,
                    fontSize = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val fav = track?.key?.let { it in favorites } == true
                IconButton(onClick = { track?.let { vm.toggleFavorite(it.key) } }) {
                    Icon(
                        if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "รายการโปรด",
                        tint = if (fav) accent else colors.secondary,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { track?.let { onMore(it, colors) } }) {
                    Icon(Icons.Rounded.MoreVert, "ตัวเลือก", tint = colors.secondary, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // ---------------- Seek bar ----------------
            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                accent = accent,
                onSeek = vm::seekTo,
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(formatTime(state.positionMs), color = colors.secondary, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text(formatTime(state.durationMs), color = colors.secondary, fontSize = 15.sp)
            }

            Spacer(Modifier.weight(0.6f))

            // ---------------- Transport ----------------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BigIcon(Icons.Rounded.FastRewind, "ก่อนหน้า", accent, 76.dp) { vm.previous() }
                BigIcon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (state.isPlaying) "หยุดชั่วคราว" else "เล่น",
                    accent, 104.dp,
                ) { vm.playOrPause() }
                BigIcon(Icons.Rounded.FastForward, "ถัดไป", accent, 76.dp) { vm.next() }
            }

            Spacer(Modifier.weight(0.7f))

            // ---------------- Bottom row ----------------
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    val i = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, state.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    runCatching { context.startActivity(i) }
                        .onFailure { actions.toast("ไม่พบอีควอไลเซอร์ในเครื่องนี้") }
                }) {
                    Icon(Icons.Rounded.Tune, "อีควอไลเซอร์", tint = colors.secondary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                val m = vm.cycleMode()
                                actions.toast(
                                    when (m) {
                                        PlayMode.REPEAT_ALL -> "เล่นซ้ำทั้งหมด"
                                        PlayMode.REPEAT_ONE -> "เล่นซ้ำเพลงเดียว"
                                        PlayMode.SHUFFLE -> "เล่นแบบสุ่ม"
                                    },
                                )
                            },
                            onLongClick = { showQueue = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (state.mode) {
                            PlayMode.REPEAT_ALL -> Icons.Rounded.Repeat
                            PlayMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
                            PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                        },
                        "โหมดการเล่น (กดค้างเพื่อดูคิว)",
                        tint = colors.secondary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }

    if (showQueue) QueueSheet(vm, colors) { showQueue = false }
}

@Composable
private fun BigIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, color = tint),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    text: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        if (icon != null && text != null) Spacer(Modifier.width(6.dp))
        if (text != null) Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** แถบเวลาแบบบาง มีปุ่มเลื่อนเล็ก ๆ (ลาก/แตะเพื่อเลื่อนตำแหน่ง) */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val fraction = when {
        dragging -> dragFraction
        durationMs > 0 -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(durationMs) {
                detectTapGestures { o ->
                    if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragging = true
                        dragFraction = (o.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (durationMs > 0) onSeek((dragFraction * durationMs).toLong())
                        dragging = false
                    },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val w = maxWidth
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (dragging) 10.dp else 8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.07f)),
        )
        Box(
            Modifier
                .width(w * fraction)
                .height(if (dragging) 10.dp else 8.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.55f)),
        )
        Box(
            Modifier
                .offset(x = (w * fraction - 3.dp).coerceIn(0.dp, w - 6.dp))
                .size(width = 6.dp, height = if (dragging) 22.dp else 16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFEDEDED)),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(vm: MainViewModel, colors: PlayerColors, onDismiss: () -> Unit) {
    val state by vm.playerState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch { listState.scrollToItem((state.queueIndex - 2).coerceAtLeast(0)) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = colors.sheet,
    ) {
        Text(
            "คิวเพลง (${state.queueKeys.size})",
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxHeight(0.75f).navigationBarsPadding()) {
            itemsIndexed(state.queueKeys) { i, key ->
                val t = vm.trackByKey(key) ?: return@itemsIndexed
                val current = i == state.queueIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.skipToQueueIndex(i) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(t, Modifier.size(44.dp), RoundedCornerShape(8.dp), sizePx = 128)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.title, color = if (current) colors.accent else Color.White, fontSize = 15.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(t.artist, color = colors.secondary, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
