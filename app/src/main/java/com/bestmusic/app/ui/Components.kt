package com.bestmusic.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestmusic.app.data.Track
import com.bestmusic.app.data.rememberArtwork

/** ภาพปก ถ้าไม่มีจะแสดงไอคอนโน้ตเพลงบนพื้นไล่สี */
@Composable
fun Artwork(
    track: Track?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    sizePx: Int = 256,
    iconSize: Dp = 24.dp,
) {
    val art by rememberArtwork(track, sizePx)
    Box(modifier.clip(shape), contentAlignment = Alignment.Center) {
        Crossfade(targetState = art, label = "art") { bmp ->
            if (bmp != null) {
                Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF3B2A35), Color(0xFF25252B)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (track?.isVideo == true) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                        null,
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
        }
    }
}

@Composable
fun MvBadge(modifier: Modifier = Modifier) {
    Text(
        "MV",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AppAccent.copy(alpha = 0.85f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Artwork(track, Modifier.size(52.dp), RoundedCornerShape(10.dp), sizePx = 160)
            if (isCurrent) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Equalizer, null,
                        tint = if (isPlaying) AppAccent else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (isCurrent) AppAccent else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (track.isVideo) MvBadge()
                Text(
                    "${track.artist} · ${formatTime(track.durationMs)}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Rounded.MoreVert, "ตัวเลือก", tint = TextSecondary)
        }
    }
}
