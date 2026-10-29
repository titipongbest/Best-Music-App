package com.bestmusic.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bestmusic.app.R

/** แสดงภาพ MV ภายในหน้าเล่นเพลง (ไม่มีปุ่มควบคุมของ PlayerView) */
@OptIn(UnstableApi::class)
@Composable
fun InlineVideo(player: Player?, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.player_texture_view, null) as PlayerView).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        update = { if (it.player !== player) it.player = player },
        onRelease = { it.player = null },
    )
}

/** MV เต็มจอ แนวนอน พร้อมปุ่มควบคุม */
@OptIn(UnstableApi::class)
@Composable
fun FullscreenVideoScreen(player: Player?, onClose: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(Unit) {
        val window = activity?.window
        val oldOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val insets = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            insets?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = oldOrientation
        }
    }
    BackHandler(onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.player_texture_view, null) as PlayerView).apply {
                    useController = true
                    controllerShowTimeoutMs = 2500
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowNextButton(true)
                    setShowPreviousButton(true)
                    this.player = player
                }
            },
            update = { if (it.player !== player) it.player = player },
            onRelease = { it.player = null },
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp),
        ) {
            Icon(Icons.Rounded.FullscreenExit, "ออกจากเต็มจอ", tint = Color.White)
        }
    }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
