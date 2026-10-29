package com.bestmusic.app.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.bestmusic.app.data.MediaScanner
import com.bestmusic.app.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayMode { REPEAT_ALL, REPEAT_ONE, SHUFFLE }

data class PlayerState(
    val connected: Boolean = false,
    val currentKey: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val mode: PlayMode = PlayMode.REPEAT_ALL,
    val speed: Float = 1f,
    val audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET,
    val sleepEndAt: Long = 0,
    val sleepEndOfTrack: Boolean = false,
    val videoAspect: Float = 16f / 9f,
    val hasVideo: Boolean = false,
    val queueKeys: List<String> = emptyList(),
    val queueIndex: Int = 0,
)

/** ตัวเชื่อม UI กับ MusicService ผ่าน MediaController */
class PlayerConnection(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    var controller: MediaController? = null
        private set

    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                _state.value = _state.value.copy(
                    videoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height,
                )
            }
        }
    }

    fun connect() {
        if (controller != null) return
        scope.launch {
            val token = SessionToken(context, ComponentName(context, MusicService::class.java))
            val c = MediaController.Builder(context, token)
                .setListener(object : MediaController.Listener {
                    override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                        applyExtras(extras)
                    }
                })
                .buildAsync()
                .await()
            controller = c
            c.addListener(listener)
            applyExtras(c.sessionExtras)
            refresh()
            startTicker()
        }
    }

    fun release() {
        ticker?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    private fun applyExtras(extras: Bundle) {
        _state.value = _state.value.copy(
            audioSessionId = extras.getInt(MusicService.EXTRA_AUDIO_SESSION, C.AUDIO_SESSION_ID_UNSET),
            sleepEndAt = extras.getLong(MusicService.EXTRA_SLEEP_END, 0),
            sleepEndOfTrack = extras.getBoolean(MusicService.EXTRA_SLEEP_EOT, false),
        )
    }

    private fun refresh() {
        val c = controller ?: return
        val mode = when {
            c.shuffleModeEnabled -> PlayMode.SHUFFLE
            c.repeatMode == Player.REPEAT_MODE_ONE -> PlayMode.REPEAT_ONE
            else -> PlayMode.REPEAT_ALL
        }
        val key = c.currentMediaItem?.mediaId
        _state.value = _state.value.copy(
            connected = true,
            currentKey = key,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.takeIf { it != C.TIME_UNSET } ?: 0,
            mode = mode,
            speed = c.playbackParameters.speed,
            hasVideo = key?.startsWith("v:") == true,
            queueKeys = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId },
            queueIndex = c.currentMediaItemIndex,
        )
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.value = _state.value.copy(positionMs = c.currentPosition.coerceAtLeast(0))
                }
                delay(250)
            }
        }
    }

    // ---------------- Commands ----------------

    fun play(tracks: List<Track>, startIndex: Int, displayTitle: (Track) -> String, displayArtist: (Track) -> String) {
        val c = controller ?: return
        val items = tracks.map { it.toMediaItem(displayTitle(it), displayArtist(it)) }
        c.setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0L)
        c.prepare()
        c.play()
    }

    fun playOrPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
            c.play()
        }
    }

    fun next() = controller?.seekToNext()

    /** ถ้าเล่นเกิน 3 วินาทีจะกลับไปต้นเพลง ไม่งั้นไปเพลงก่อนหน้า */
    fun previous() = controller?.seekToPrevious()

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _state.value = _state.value.copy(positionMs = ms)
    }

    fun skipToQueueIndex(i: Int) {
        controller?.seekTo(i, 0)
    }

    fun cycleMode(): PlayMode {
        val c = controller ?: return PlayMode.REPEAT_ALL
        val next = when (_state.value.mode) {
            PlayMode.REPEAT_ALL -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ALL
        }
        when (next) {
            PlayMode.REPEAT_ALL -> { c.shuffleModeEnabled = false; c.repeatMode = Player.REPEAT_MODE_ALL }
            PlayMode.REPEAT_ONE -> { c.shuffleModeEnabled = false; c.repeatMode = Player.REPEAT_MODE_ONE }
            PlayMode.SHUFFLE -> { c.shuffleModeEnabled = true; c.repeatMode = Player.REPEAT_MODE_ALL }
        }
        return next
    }

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
    }

    /** minutes > 0 = ตั้งเวลา, -1 = หยุดเมื่อจบเพลงนี้, 0 = ยกเลิก */
    fun setSleepTimer(minutes: Int) {
        controller?.sendCustomCommand(
            SessionCommand(MusicService.CMD_SLEEP, Bundle.EMPTY),
            Bundle().apply { putInt(MusicService.ARG_MINUTES, minutes) },
        )
    }

    /** เอาเพลงที่ถูกลบ/ซ่อนออกจากคิว */
    fun removeFromQueue(key: String) {
        val c = controller ?: return
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId == key) c.removeMediaItem(i)
        }
    }

    /** อัปเดตชื่อที่แสดงในการแจ้งเตือน หลังแก้ไขชื่อเพลง */
    fun updateMetadata(track: Track, title: String, artist: String) {
        val c = controller ?: return
        for (i in 0 until c.mediaItemCount) {
            if (c.getMediaItemAt(i).mediaId == track.key) {
                c.replaceMediaItem(i, track.toMediaItem(title, artist))
            }
        }
    }

    private fun Track.toMediaItem(title: String, artist: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(key)
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album.ifBlank { null })
                    .setArtworkUri(if (!isVideo) MediaScanner.albumArtUri(albumId) else null)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build(),
            )
            .build()
}
