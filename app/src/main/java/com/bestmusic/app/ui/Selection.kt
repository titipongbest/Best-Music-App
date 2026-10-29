package com.bestmusic.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestmusic.app.MainViewModel
import com.bestmusic.app.data.Folder
import com.bestmusic.app.data.SortOrder
import com.bestmusic.app.data.Track

// =====================================================================
//  Sort
// =====================================================================

/** ปุ่มเรียงลำดับ: เปิดเมนูให้เลือกวิธีเรียง (จำค่าไว้ทุกโฟลเดอร์) */
@Composable
fun SortButton(vm: MainViewModel) {
    val order by vm.sortOrder.collectAsState()
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, "เรียงลำดับ", tint = if (order == SortOrder.DEFAULT) TextSecondary else AppAccent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                "เรียงตาม", color = TextSecondary, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            SortOrder.entries.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o.label, color = if (o == order) AppAccent else Color.White) },
                    trailingIcon = { if (o == order) Icon(Icons.Rounded.Done, null, tint = AppAccent) },
                    onClick = { vm.setSortOrder(o); open = false },
                )
            }
        }
    }
}

// =====================================================================
//  Multi-select (แบบในภาพตัวอย่าง: "0 เลือกแล้ว" + แถบเครื่องมือด้านล่าง)
// =====================================================================

private enum class SelectDialog { NONE, PLAYLIST, HIDE, DELETE, REMOVE }

@Composable
fun SelectionScreen(
    tracks: List<Track>,
    initialSelected: Set<String>,
    vm: MainViewModel,
    actions: ActivityActions,
    onExit: () -> Unit,
    playlistName: String? = null,
) {
    var selected by rememberSaveable { mutableStateOf(initialSelected.toList()) }
    val selectedSet = remember(selected) { selected.toSet() }
    val chosen = remember(selectedSet, tracks) { tracks.filter { it.key in selectedSet } }
    val allSelected = tracks.isNotEmpty() && selectedSet.size == tracks.size
    var dialog by remember { mutableStateOf(SelectDialog.NONE) }

    BackHandler(onBack = onExit)

    fun done(msg: String) {
        actions.toast(msg)
        onExit()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0C)).statusBarsPadding()) {
        // ---- Top bar ----
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "ออก", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${selectedSet.size} เลือกแล้ว",
                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { selected = if (allSelected) emptyList() else tracks.map { it.key } }) {
                Icon(
                    Icons.Rounded.SelectAll, if (allSelected) "ยกเลิกเลือกทั้งหมด" else "เลือกทั้งหมด",
                    tint = if (allSelected) AppAccent else TextSecondary,
                )
            }
        }

        // ---- List ----
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(tracks, key = { it.key }) { t ->
                val isSel = t.key in selectedSet
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selected = if (isSel) selected - t.key else selected + t.key }
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (isSel) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        null,
                        tint = if (isSel) AppAccent else Color(0xFF6B6B70),
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(18.dp))
                    Artwork(t, Modifier.size(64.dp), RoundedCornerShape(14.dp), sizePx = 192)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${t.artist} - ${t.folderName}", color = TextSecondary, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // ---- Bottom action bar ----
        val enabled = chosen.isNotEmpty()
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF2A2A2C))
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BarAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "เพิ่มไปที่", enabled) { dialog = SelectDialog.PLAYLIST }
            BarAction(Icons.AutoMirrored.Rounded.PlaylistPlay, "เล่นต่อไป", enabled) {
                vm.playNext(chosen)
                done("เพิ่ม ${chosen.size} รายการให้เล่นต่อไปแล้ว")
            }
            BarAction(Icons.Outlined.Share, "แบ่งปัน", enabled) { actions.shareMany(chosen) }
            if (playlistName != null) {
                BarAction(Icons.Outlined.RemoveCircleOutline, "นำออก", enabled) { dialog = SelectDialog.REMOVE }
            } else {
                BarAction(Icons.Outlined.VisibilityOff, "ซ่อน", enabled) { dialog = SelectDialog.HIDE }
            }
            BarAction(Icons.Outlined.Delete, "ลบ", enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    actions.deleteMany(chosen) // ระบบจะถามยืนยันเอง
                    onExit()
                } else dialog = SelectDialog.DELETE
            }
        }
    }

    when (dialog) {
        SelectDialog.PLAYLIST -> PlaylistPickerDialog(
            vm = vm,
            onPick = { name -> vm.addAllToPlaylist(name, chosen); dialog = SelectDialog.NONE; done("เพิ่ม ${chosen.size} รายการลงใน \"$name\" แล้ว") },
            onCreate = { name -> vm.addAllToPlaylist(name, chosen); dialog = SelectDialog.NONE; done("สร้าง \"$name\" พร้อม ${chosen.size} รายการแล้ว") },
            onDismiss = { dialog = SelectDialog.NONE },
        )
        SelectDialog.HIDE -> ConfirmDialog(
            title = "ซ่อน ${chosen.size} รายการ?",
            text = "รายการที่เลือกจะไม่แสดงในคลังเพลง (แสดงกลับได้จากเมนูในหน้าหลัก)",
            confirm = "ซ่อน",
            onConfirm = { vm.hideAll(chosen); dialog = SelectDialog.NONE; done("ซ่อนแล้ว ${chosen.size} รายการ") },
            onDismiss = { dialog = SelectDialog.NONE },
        )
        SelectDialog.DELETE -> ConfirmDialog(
            title = "ลบ ${chosen.size} ไฟล์?",
            text = "ไฟล์ที่เลือกจะถูกลบออกจากเครื่องอย่างถาวร",
            confirm = "ลบ",
            onConfirm = { dialog = SelectDialog.NONE; actions.deleteMany(chosen); onExit() },
            onDismiss = { dialog = SelectDialog.NONE },
        )
        SelectDialog.REMOVE -> ConfirmDialog(
            title = "นำ ${chosen.size} รายการออกจากเพลย์ลิสต์?",
            text = "นำออกจาก \"$playlistName\" เท่านั้น ไฟล์ในเครื่องจะไม่ถูกลบ",
            confirm = "นำออก",
            onConfirm = {
                playlistName?.let { vm.removeAllFromPlaylist(it, chosen) }
                dialog = SelectDialog.NONE
                done("นำออกแล้ว ${chosen.size} รายการ")
            },
            onDismiss = { dialog = SelectDialog.NONE },
        )
        SelectDialog.NONE -> Unit
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) Color(0xFFE6E6E6) else Color(0xFF77777B)
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = tint, fontSize = 15.sp, maxLines = 1)
    }
}

// =====================================================================
//  Search
// =====================================================================

@Composable
fun SearchScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onMore: (Track) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val library by vm.tracks.collectAsState()
    val folders by vm.folders.collectAsState()
    val state by vm.playerState.collectAsState()
    val results by remember(library) { derivedStateOf { vm.search(query) } }
    val folderResults: List<Folder> = remember(query, folders) {
        val q = query.trim()
        if (q.isEmpty()) emptyList() else folders.filter { it.name.contains(q, ignoreCase = true) }
    }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(AppBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "กลับ", tint = Color.White) }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("ค้นหาเพลง ศิลปิน โฟลเดอร์", color = TextSecondary) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextSecondary) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, "ล้าง", tint = TextSecondary)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppAccent,
                ),
                modifier = Modifier.weight(1f).padding(end = 12.dp).focusRequester(focus),
            )
        }

        when {
            query.isBlank() -> Hint("พิมพ์เพื่อค้นหาจาก ${library.size} รายการในเครื่อง")
            results.isEmpty() && folderResults.isEmpty() -> Hint("ไม่พบ \"${query.trim()}\"")
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                if (folderResults.isNotEmpty()) {
                    item { SectionTitle("โฟลเดอร์") }
                    items(folderResults, key = { "f:" + it.path }) { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenFolder(f.path) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(AppSurface),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Folder, null, tint = AppAccent) }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(f.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("${f.tracks.size} รายการ", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) {
                    item { SectionTitle("เพลงและ MV (${results.size})") }
                    itemsIndexed(results, key = { _, t -> t.key }) { i, t ->
                        TrackRow(
                            t, isCurrent = t.key == state.currentKey, isPlaying = state.isPlaying,
                            onClick = { keyboard?.hide(); vm.play(results, i); onOpenPlayer() },
                            onMore = { onMore(t) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 40.dp))
    }
}
