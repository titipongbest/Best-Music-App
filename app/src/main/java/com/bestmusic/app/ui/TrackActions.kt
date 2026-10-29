package com.bestmusic.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestmusic.app.MainViewModel
import com.bestmusic.app.data.PlayerColors
import com.bestmusic.app.data.Track
import com.bestmusic.app.playback.PlayerState

/** การกระทำที่ต้องใช้ Activity (แชร์, ลบ, ขอสิทธิ์) */
interface ActivityActions {
    fun share(track: Track)
    fun delete(track: Track)
    fun shareMany(tracks: List<Track>)
    fun deleteMany(tracks: List<Track>)
    fun requestWriteSettings()
    fun toast(msg: String)
}

private enum class SheetDialog { NONE, PLAYLIST, SPEED, SLEEP, EDIT, CONFIRM_DELETE, CONFIRM_HIDE }

/**
 * เมนู "⋮" แบบในภาพตัวอย่าง: ปก+ชื่อ+ปุ่มแก้ไข แล้วตามด้วยรายการ
 * เพิ่มลงในเพลย์ลิสต์ / แบ่งปัน / ความเร็ว / ทำเป็นเสียงริงโทน / ตั้งเวลาปิดเครื่อง / โหมดประหยัดพลังงาน / ซ่อน / ลบ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMoreSheet(
    track: Track,
    colors: PlayerColors,
    vm: MainViewModel,
    actions: ActivityActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playerState by vm.playerState.collectAsState()
    val batterySaver by vm.prefs.batterySaver.collectAsState()
    var dialog by remember { mutableStateOf(SheetDialog.NONE) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheet,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Icon(
                Icons.Rounded.KeyboardArrowDown, null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(width = 48.dp, height = 28.dp),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            // ---- Header ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(track, Modifier.size(76.dp), RoundedCornerShape(14.dp), sizePx = 256)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title, color = colors.onBackground, fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        lineHeight = 25.sp,
                    )
                    Text(track.artist, color = colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                IconButton(onClick = { dialog = SheetDialog.EDIT }) {
                    Icon(Icons.Outlined.Edit, "แก้ไข", tint = colors.secondary, modifier = Modifier.size(26.dp))
                }
            }

            SheetItem(Icons.AutoMirrored.Rounded.PlaylistAdd, "เพิ่มลงในเพลย์ลิสต์") { dialog = SheetDialog.PLAYLIST }
            SheetItem(Icons.Outlined.Share, "แบ่งปัน") { actions.share(track) }
            SheetDivider()
            SheetItem(
                Icons.Outlined.Speed, "ความเร็ว",
                trailing = { if (playerState.speed != 1f) TrailingText("${playerState.speed}x", colors.accent) },
            ) { dialog = SheetDialog.SPEED }
            SheetItem(Icons.Outlined.Notifications, "ทำเป็นเสียงริงโทน") {
                when {
                    track.isVideo -> actions.toast("ตั้งเป็นเสียงเรียกเข้าได้เฉพาะไฟล์เพลง")
                    !vm.canWriteSettings() -> {
                        actions.toast("กรุณาอนุญาต \"แก้ไขการตั้งค่าระบบ\" แล้วลองอีกครั้ง")
                        actions.requestWriteSettings()
                    }
                    vm.setAsRingtone(track) -> actions.toast("ตั้งเป็นเสียงเรียกเข้าแล้ว")
                    else -> actions.toast("ตั้งเสียงเรียกเข้าไม่สำเร็จ")
                }
            }
            SheetItem(
                Icons.Outlined.Alarm, "ตั้งเวลาปิดเครื่อง",
                trailing = { sleepLabel(playerState)?.let { TrailingText(it, colors.accent) } },
            ) { dialog = SheetDialog.SLEEP }
            SheetItem(
                Icons.Outlined.BatterySaver, "โหมดประหยัดพลังงาน",
                trailing = {
                    Switch(
                        checked = batterySaver,
                        onCheckedChange = { vm.setBatterySaver(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.accent,
                            checkedThumbColor = colors.background,
                        ),
                    )
                },
            ) { vm.setBatterySaver(!batterySaver) }
            SheetDivider()
            SheetItem(Icons.Outlined.VisibilityOff, "ซ่อน") { dialog = SheetDialog.CONFIRM_HIDE }
            SheetItem(Icons.Outlined.Delete, "ลบ") {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    actions.delete(track) // ระบบจะแสดงหน้ายืนยันเอง
                    onDismiss()
                } else dialog = SheetDialog.CONFIRM_DELETE
            }
        }
    }

    when (dialog) {
        SheetDialog.PLAYLIST -> PlaylistPickerDialog(
            vm = vm,
            onPick = { name ->
                vm.addToPlaylist(name, track.key)
                actions.toast("เพิ่มลงใน \"$name\" แล้ว")
                dialog = SheetDialog.NONE
            },
            onCreate = { name ->
                vm.createPlaylist(name, track.key)
                actions.toast("สร้างเพลย์ลิสต์ \"$name\" แล้ว")
                dialog = SheetDialog.NONE
            },
            onDismiss = { dialog = SheetDialog.NONE },
        )
        SheetDialog.SPEED -> SpeedDialog(
            current = playerState.speed,
            accent = colors.accent,
            onPick = { vm.setSpeed(it); dialog = SheetDialog.NONE },
            onDismiss = { dialog = SheetDialog.NONE },
        )
        SheetDialog.SLEEP -> SleepTimerDialog(
            state = playerState,
            onPick = { min ->
                vm.setSleepTimer(min)
                actions.toast(
                    when {
                        min > 0 -> "จะหยุดเล่นในอีก $min นาที"
                        min < 0 -> "จะหยุดเล่นเมื่อจบเพลงนี้"
                        else -> "ยกเลิกตั้งเวลาแล้ว"
                    },
                )
                dialog = SheetDialog.NONE
            },
            onDismiss = { dialog = SheetDialog.NONE },
        )
        SheetDialog.EDIT -> EditInfoDialog(
            track = track,
            onSave = { t, a -> vm.editInfo(track, t, a); dialog = SheetDialog.NONE },
            onDismiss = { dialog = SheetDialog.NONE },
        )
        SheetDialog.CONFIRM_HIDE -> ConfirmDialog(
            title = "ซ่อนเพลงนี้?",
            text = "\"${track.title}\" จะไม่แสดงในคลังเพลง (แสดงกลับได้จากเมนูในหน้าหลัก)",
            confirm = "ซ่อน",
            onConfirm = {
                vm.hide(track)
                dialog = SheetDialog.NONE
                onDismiss()
            },
            onDismiss = { dialog = SheetDialog.NONE },
        )
        SheetDialog.CONFIRM_DELETE -> ConfirmDialog(
            title = "ลบไฟล์นี้?",
            text = "\"${track.title}\" จะถูกลบออกจากเครื่องอย่างถาวร",
            confirm = "ลบ",
            onConfirm = {
                actions.delete(track)
                dialog = SheetDialog.NONE
                onDismiss()
            },
            onDismiss = { dialog = SheetDialog.NONE },
        )
        SheetDialog.NONE -> Unit
    }
}

private fun sleepLabel(s: PlayerState): String? = when {
    s.sleepEndOfTrack -> "จบเพลงนี้"
    s.sleepEndAt > 0 -> {
        val left = ((s.sleepEndAt - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0) + 1
        "$left นาที"
    }
    else -> null
}

@Composable
private fun TrailingText(text: String, color: Color) {
    Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun SheetItem(
    icon: ImageVector,
    label: String,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(22.dp))
        Text(label, color = Color(0xFFEDEDED), fontSize = 19.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        thickness = 1.dp,
        color = Color.White.copy(alpha = 0.06f),
    )
}

// ------------------------------- Dialogs -------------------------------

@Composable
fun PlaylistPickerDialog(
    vm: MainViewModel,
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val playlists by vm.playlists.collectAsState()
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("เพิ่มลงในเพลย์ลิสต์") },
        text = {
            Column {
                if (playlists.isNotEmpty()) {
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(playlists, key = { it.name }) { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onPick(p.name) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = AppAccent)
                                Spacer(Modifier.width(12.dp))
                                Text(p.name, Modifier.weight(1f), fontSize = 16.sp)
                                Text("${p.keys.size}", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        label = { Text("สร้างเพลย์ลิสต์ใหม่") },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (newName.isNotBlank()) onCreate(newName.trim()) }, enabled = newName.isNotBlank()) {
                        Icon(Icons.Rounded.Add, "สร้าง")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ปิด") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedDialog(current: Float, accent: Color, onPick: (Float) -> Unit, onDismiss: () -> Unit) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ความเร็วในการเล่น") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                speeds.forEach { s ->
                    FilterChip(
                        selected = s == current,
                        onClick = { onPick(s) },
                        label = { Text(if (s == 1f) "ปกติ" else "${s}x") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Color.Black,
                        ),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ปิด") } },
    )
}

@Composable
private fun SleepTimerDialog(state: PlayerState, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(15, 30, 45, 60, 90)
    val active = state.sleepEndAt > 0 || state.sleepEndOfTrack
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ตั้งเวลาปิด") },
        text = {
            Column {
                options.forEach { m -> DialogOption("$m นาที") { onPick(m) } }
                DialogOption("เมื่อจบเพลงนี้") { onPick(-1) }
                if (active) DialogOption("ยกเลิกตั้งเวลา", color = AppAccent) { onPick(0) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ปิด") } },
    )
}

@Composable
private fun DialogOption(text: String, color: Color = Color.White, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 6.dp),
    ) { Text(text, color = color, fontSize = 16.sp) }
}

@Composable
private fun EditInfoDialog(track: Track, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("แก้ไขข้อมูลเพลง") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("ชื่อเพลง") }, singleLine = true)
                OutlinedTextField(artist, { artist = it }, label = { Text("ศิลปิน") }, singleLine = true)
                Text("แก้ไขเฉพาะที่แสดงในแอป ไฟล์ต้นฉบับไม่เปลี่ยน", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, artist) }) { Text("บันทึก") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = AppAccent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

@Composable
fun TextInputDialog(title: String, label: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }, enabled = value.isNotBlank()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}
