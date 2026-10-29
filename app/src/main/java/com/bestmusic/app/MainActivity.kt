package com.bestmusic.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bestmusic.app.data.PlayerColors
import com.bestmusic.app.data.Track
import com.bestmusic.app.ui.ActivityActions
import com.bestmusic.app.ui.AppAccent
import com.bestmusic.app.ui.AppBg
import com.bestmusic.app.ui.BestMusicTheme
import com.bestmusic.app.ui.FullscreenVideoScreen
import com.bestmusic.app.ui.HomeScreen
import com.bestmusic.app.ui.LyricsScreen
import com.bestmusic.app.ui.MiniPlayer
import com.bestmusic.app.ui.PlayerScreen
import com.bestmusic.app.ui.SearchScreen
import com.bestmusic.app.ui.TextSecondary
import com.bestmusic.app.ui.TrackListScreen
import com.bestmusic.app.ui.TrackMoreSheet

class MainActivity : ComponentActivity(), ActivityActions {

    private val vm: MainViewModel by viewModels()

    private var hasPermission by mutableStateOf(false)
    private var openPlayerRequest by mutableStateOf(false)
    private var pendingDelete: List<Track> = emptyList()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            hasPermission = checkMediaPermission()
            if (hasPermission) vm.scan()
        }

    private val deleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val list = pendingDelete
            pendingDelete = emptyList()
            if (result.resultCode == Activity.RESULT_OK && list.isNotEmpty()) {
                vm.confirmDeleted(list)
                toast(if (list.size == 1) "ลบแล้ว" else "ลบแล้ว ${list.size} ไฟล์")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        hasPermission = checkMediaPermission()
        if (hasPermission) vm.scan() else requestMediaPermission()
        openPlayerRequest = intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true

        setContent {
            BestMusicTheme {
                if (hasPermission) BestMusicApp(vm, this, openPlayerRequest) { openPlayerRequest = false }
                else PermissionScreen(
                    onRequest = { requestMediaPermission() },
                    onOpenSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) openPlayerRequest = true
    }

    override fun onResume() {
        super.onResume()
        // ผู้ใช้อาจไปอนุญาตจากหน้าตั้งค่ามาแล้ว
        if (!hasPermission && checkMediaPermission()) {
            hasPermission = true
            vm.scan()
        }
    }

    private fun mediaPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS,
        ) else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun checkMediaPermission(): Boolean {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            listOf(Manifest.permission.READ_MEDIA_AUDIO) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        return needed.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestMediaPermission() = permissionLauncher.launch(mediaPermissions())

    // ---------------- ActivityActions ----------------

    override fun share(track: Track) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (track.isVideo) "video/*" else "audio/*"
            putExtra(Intent.EXTRA_STREAM, track.uri)
            putExtra(Intent.EXTRA_TITLE, track.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "แบ่งปัน \"${track.title}\""))
    }

    override fun delete(track: Track) = deleteMany(listOf(track))

    override fun deleteMany(tracks: List<Track>) {
        when (val r = vm.requestDelete(tracks)) {
            is MainViewModel.DeleteResult.NeedsConfirm -> {
                pendingDelete = tracks
                deleteLauncher.launch(IntentSenderRequest.Builder(r.sender).build())
            }
            is MainViewModel.DeleteResult.Done ->
                if (r.count > 0) toast(if (r.count == 1) "ลบแล้ว" else "ลบแล้ว ${r.count} ไฟล์")
            MainViewModel.DeleteResult.Failed -> toast("ลบไฟล์ไม่สำเร็จ")
        }
    }

    override fun shareMany(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        if (tracks.size == 1) return share(tracks.first())
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = when {
                tracks.all { it.isVideo } -> "video/*"
                tracks.none { it.isVideo } -> "audio/*"
                else -> "*/*"
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(tracks.map { it.uri }))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "แบ่งปัน ${tracks.size} รายการ"))
    }

    override fun requestWriteSettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
    }
}

private object Routes {
    const val HOME = "home"
    const val FOLDER = "folder/{path}"
    const val PLAYLIST = "playlist/{name}"
    const val PLAYER = "player"
    const val LYRICS = "lyrics"
    const val VIDEO = "video"
    const val SEARCH = "search"
    fun folder(path: String) = "folder/${Uri.encode(path)}"
    fun playlist(name: String) = "playlist/${Uri.encode(name)}"
}

private data class SheetRequest(val track: Track, val colors: PlayerColors)

@Composable
private fun BestMusicApp(
    vm: MainViewModel,
    actions: ActivityActions,
    openPlayerRequest: Boolean,
    onPlayerOpened: () -> Unit,
) {
    val nav = rememberNavController()
    var sheet by androidx.compose.runtime.remember { mutableStateOf<SheetRequest?>(null) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val current by vm.currentTrack.collectAsState()
    val selecting by vm.selectionActive.collectAsState()

    LaunchedEffect(openPlayerRequest) {
        if (openPlayerRequest) {
            if (route != Routes.PLAYER) nav.navigate(Routes.PLAYER) { launchSingleTop = true }
            onPlayerOpened()
        }
    }

    val openPlayer = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } }
    val openMore = { t: Track -> sheet = SheetRequest(t, PlayerColors.Default) }

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Box(Modifier.weight(1f)) {
            AppNavHost(nav, vm, actions, openPlayer, openMore) { t, c -> sheet = SheetRequest(t, c) }
        }
        val showMini = current != null && !selecting && route in setOf(Routes.HOME, Routes.FOLDER, Routes.PLAYLIST, Routes.SEARCH)
        if (showMini) Box(Modifier.navigationBarsPadding()) { MiniPlayer(vm, onOpen = openPlayer) }
        else if (!selecting && route != Routes.PLAYER && route != Routes.LYRICS && route != Routes.VIDEO) Spacer(Modifier.navigationBarsPadding())
    }

    sheet?.let { req ->
        TrackMoreSheet(req.track, req.colors, vm, actions) { sheet = null }
    }
}

@Composable
private fun AppNavHost(
    nav: NavHostController,
    vm: MainViewModel,
    actions: ActivityActions,
    openPlayer: () -> Unit,
    openMore: (Track) -> Unit,
    openMoreColored: (Track, PlayerColors) -> Unit,
) {
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onOpenFolder = { nav.navigate(Routes.folder(it)) },
                onOpenPlaylist = { nav.navigate(Routes.playlist(it)) },
                onOpenPlayer = openPlayer,
                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                onMore = openMore,
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpenFolder = { nav.navigate(Routes.folder(it)) },
                onOpenPlayer = openPlayer,
                onMore = openMore,
            )
        }
        composable(Routes.FOLDER, arguments = listOf(navArgument("path") { type = NavType.StringType })) { entry ->
            val path = entry.arguments?.getString("path")?.let(Uri::decode) ?: ""
            val folders by vm.folders.collectAsState()
            val folder = folders.firstOrNull { it.path == path }
            TrackListScreen(
                title = folder?.name ?: path.substringAfterLast('/'),
                subtitle = folder?.let {
                    "${it.audioCount} เพลง" + if (it.videoCount > 0) " · ${it.videoCount} MV" else ""
                } ?: "",
                tracks = folder?.tracks ?: emptyList(),
                vm = vm,
                actions = actions,
                onBack = { nav.popBackStack() },
                onOpenPlayer = openPlayer,
                onMore = openMore,
            )
        }
        composable(Routes.PLAYLIST, arguments = listOf(navArgument("name") { type = NavType.StringType })) { entry ->
            val name = entry.arguments?.getString("name")?.let(Uri::decode) ?: ""
            val playlists by vm.playlists.collectAsState()
            val visible by vm.tracks.collectAsState()
            val playlist = playlists.firstOrNull { it.name == name }
            val tracks = androidx.compose.runtime.remember(playlist, visible) {
                playlist?.let { vm.playlistTracks(it) } ?: emptyList()
            }
            TrackListScreen(
                title = name,
                subtitle = "${tracks.size} รายการ",
                tracks = tracks,
                vm = vm,
                actions = actions,
                onBack = { nav.popBackStack() },
                onOpenPlayer = openPlayer,
                onMore = openMore,
                extraMenu = { dismiss ->
                    DropdownMenuItem(
                        text = { Text("ลบเพลย์ลิสต์") },
                        onClick = {
                            dismiss()
                            vm.deletePlaylist(name)
                            nav.popBackStack()
                        },
                    )
                },
            )
        }
        composable(
            Routes.PLAYER,
            enterTransition = { slideInVertically(tween(320)) { it } + fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { slideOutVertically(tween(300)) { it } + fadeOut(tween(250)) },
        ) {
            PlayerScreen(
                vm = vm,
                actions = actions,
                onClose = { nav.popBackStack() },
                onFullscreenVideo = { nav.navigate(Routes.VIDEO) },
                onMore = openMoreColored,
            )
        }
        composable(
            Routes.LYRICS,
            enterTransition = { slideInVertically(tween(300)) { it } },
            popExitTransition = { slideOutVertically(tween(280)) { it } },
        ) {
            LyricsScreen(vm, onClose = { nav.popBackStack() })
        }
        composable(Routes.VIDEO) {
            FullscreenVideoScreen(vm.player.controller, onClose = { nav.popBackStack() })
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(AppBg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("Best music", color = AppAccent, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        Text(
            "แอปต้องได้รับอนุญาตให้เข้าถึงเพลงและวิดีโอในเครื่อง\nเพื่อแสดงเพลงตามโฟลเดอร์และเล่น MV",
            color = TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = AppAccent, contentColor = Color(0xFF2A1520)),
        ) { Text("อนุญาตการเข้าถึง", fontWeight = FontWeight.Bold) }
        androidx.compose.material3.TextButton(onClick = onOpenSettings) {
            Text("เปิดการตั้งค่าแอป", color = TextSecondary)
        }
    }
}
