package com.bestmusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestmusic.app.MainViewModel
import com.bestmusic.app.data.Track
import com.bestmusic.app.data.rememberPlayerColors

private val tabs = listOf("โฟลเดอร์", "MV", "รายการโปรด", "เพลย์ลิสต์")

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenFolder: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSearch: () -> Unit,
    onMore: (Track) -> Unit,
) {
    val folders by vm.folders.collectAsState()
    val videos by vm.videos.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val loading by vm.loading.collectAsState()
    val state by vm.playerState.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var newPlaylist by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBg).statusBarsPadding()) {
        // ---- Header ----
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppLogo(38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Best music", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = AppAccent, lineHeight = 28.sp)
                AppCredit()
            }
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = AppAccent)
            IconButton(onClick = onOpenSearch) { Icon(Icons.Rounded.Search, "ค้นหา", tint = Color.White) }
            IconButton(onClick = { vm.scan() }) { Icon(Icons.Rounded.Refresh, "สแกนใหม่", tint = TextSecondary) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "เมนู", tint = TextSecondary) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("แสดงเพลงที่ซ่อนทั้งหมด") }, onClick = { vm.unhideAll(); menu = false })
                    DropdownMenuItem(text = { Text("เกี่ยวกับแอป ($APP_VERSION)") }, onClick = { about = true; menu = false })
                }
            }
        }

        // ---- Tabs (chips) ----
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { i, label ->
                val sel = i == tab
                Text(
                    label,
                    color = if (sel) OnAccent else Color(0xFFDDDDDD),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) AppAccent else AppSurface)
                        .clickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> if (folders.isEmpty()) EmptyState(loading, "ไม่พบไฟล์เพลงหรือวิดีโอในเครื่อง") else
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(folders, key = { it.path }) { f ->
                            FolderRow(f.name, buildString {
                                append("${f.audioCount} เพลง")
                                if (f.videoCount > 0) append(" · ${f.videoCount} MV")
                            }, f.tracks.firstOrNull()) { onOpenFolder(f.path) }
                        }
                    }

                1 -> if (videos.isEmpty()) EmptyState(loading, "ไม่พบวิดีโอ / MV") else
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(videos, key = { it.key }) { v ->
                            VideoCard(v, isCurrent = v.key == state.currentKey, onClick = {
                                vm.play(videos, videos.indexOf(v)); onOpenPlayer()
                            }, onMore = { onMore(v) })
                        }
                    }

                2 -> if (favorites.isEmpty()) EmptyState(false, "กดหัวใจในหน้าเล่นเพลงเพื่อเพิ่มรายการโปรด") else
                    TrackList(favorites, vm, onOpenPlayer, onMore)

                else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().clickable { newPlaylist = true }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(AppSurface), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, null, tint = AppAccent)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text("สร้างเพลย์ลิสต์ใหม่", color = AppAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    items(playlists, key = { it.name }) { p ->
                        val first = p.keys.firstNotNullOfOrNull { vm.trackByKey(it) }
                        FolderRow(p.name, "${p.keys.size} รายการ", first, icon = Icons.AutoMirrored.Rounded.QueueMusic) {
                            onOpenPlaylist(p.name)
                        }
                    }
                }
            }
        }
    }

    if (about) AboutDialog { about = false }
    if (newPlaylist) TextInputDialog(
        title = "สร้างเพลย์ลิสต์", label = "ชื่อเพลย์ลิสต์", confirm = "สร้าง",
        onConfirm = { vm.createPlaylist(it, null); newPlaylist = false },
        onDismiss = { newPlaylist = false },
    )
}

@Composable
private fun EmptyState(loading: Boolean, text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator(color = AppAccent)
        else Text(text, color = TextSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun FolderRow(
    name: String,
    subtitle: String,
    cover: Track?,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Folder,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp)) {
            Artwork(cover, Modifier.fillMaxSize(), RoundedCornerShape(12.dp), sizePx = 160)
            Box(
                Modifier.align(Alignment.BottomEnd).padding(3.dp).size(22.dp).clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC1B1B1D)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = AppAccent, modifier = Modifier.size(15.dp)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun VideoCard(v: Track, isCurrent: Boolean, onClick: () -> Unit, onMore: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            Artwork(v, Modifier.fillMaxSize(), RoundedCornerShape(14.dp), sizePx = 384, iconSize = 36.dp)
            Text(
                formatTime(v.durationMs), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(4.dp))
                    .background(Color(0xAA000000)).padding(horizontal = 5.dp, vertical = 1.dp),
            )
            if (isCurrent) MvBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(start = 2.dp)) {
                Text(
                    v.title, color = if (isCurrent) AppAccent else Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp,
                )
                Text(v.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            Icon(
                Icons.Rounded.MoreVert, "ตัวเลือก", tint = TextSecondary,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(50)).clickable(onClick = onMore).padding(4.dp),
            )
        }
    }
}

@Composable
private fun TrackList(
    list: List<Track>,
    vm: MainViewModel,
    onOpenPlayer: () -> Unit,
    onMore: (Track) -> Unit,
    onLongPress: ((Track) -> Unit)? = null,
) {
    val state by vm.playerState.collectAsState()
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        itemsIndexed(list, key = { _, t -> t.key }) { i, t ->
            TrackRow(
                t, isCurrent = t.key == state.currentKey, isPlaying = state.isPlaying,
                onClick = { vm.play(list, i); onOpenPlayer() },
                onMore = { onMore(t) },
                onLongClick = onLongPress?.let { f -> { f(t) } },
            )
        }
    }
}

/** หน้ารายการเพลงในโฟลเดอร์ / เพลย์ลิสต์ (มีปุ่มเรียงลำดับและเลือกหลายรายการ) */
@Composable
fun TrackListScreen(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    vm: MainViewModel,
    actions: ActivityActions,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    onMore: (Track) -> Unit,
    extraMenu: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    val order by vm.sortOrder.collectAsState()
    val sorted = remember(tracks, order) { order.apply(tracks) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var initialSelection by rememberSaveable { mutableStateOf(listOf<String>()) }

    DisposableEffect(selecting) {
        vm.selectionActive.value = selecting
        onDispose { vm.selectionActive.value = false }
    }

    if (selecting) {
        SelectionScreen(
            tracks = sorted,
            initialSelected = initialSelection.toSet(),
            vm = vm,
            actions = actions,
            onExit = { selecting = false },
        )
        return
    }
    val startSelect = { first: Track? ->
        initialSelection = listOfNotNull(first?.key)
        selecting = true
    }

    Column(Modifier.fillMaxSize().background(AppBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "กลับ", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            SortButton(vm)
            IconButton(onClick = { startSelect(null) }, enabled = tracks.isNotEmpty()) {
                Icon(Icons.Rounded.Checklist, "เลือก", tint = TextSecondary)
            }
            if (extraMenu != null) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "เมนู", tint = TextSecondary) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) { extraMenu { menu = false } }
            }
        }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(tracks.firstOrNull(), Modifier.size(84.dp), RoundedCornerShape(18.dp), sizePx = 256, iconSize = 36.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextSecondary, fontSize = 14.sp)
            }
        }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vm.play(sorted, 0); onOpenPlayer() },
                enabled = sorted.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent, contentColor = OnAccent),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("เล่นทั้งหมด", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { if (sorted.isNotEmpty()) { vm.play(sorted.shuffled(), 0); onOpenPlayer() } },
                enabled = sorted.isNotEmpty(),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Rounded.Shuffle, null, tint = AppAccent)
                Spacer(Modifier.width(6.dp))
                Text("สุ่มเล่น", color = AppAccent, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.weight(1f)) { TrackList(sorted, vm, onOpenPlayer, onMore, onLongPress = { startSelect(it) }) }
    }
}

/** แถบเล่นเพลงเล็กด้านล่าง */
@Composable
fun MiniPlayer(vm: MainViewModel, onOpen: () -> Unit) {
    val track by vm.currentTrack.collectAsState()
    val state by vm.playerState.collectAsState()
    val batterySaver by vm.prefs.batterySaver.collectAsState()
    val colors by rememberPlayerColors(track, batterySaver)
    val t = track ?: return
    val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    Column(
        Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(colors.sheet, colors.background)))
            .clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(t, Modifier.size(48.dp), RoundedCornerShape(12.dp), sizePx = 160)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = colors.secondary, fontSize = 13.sp, maxLines = 1)
            }
            IconButton(onClick = { vm.playOrPause() }) {
                Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = colors.accent, modifier = Modifier.size(34.dp))
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Rounded.SkipNext, null, tint = colors.accent, modifier = Modifier.size(30.dp))
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(2.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxWidth(progress).height(2.dp).background(colors.accent))
        }
        Spacer(Modifier.height(6.dp))
    }
}
