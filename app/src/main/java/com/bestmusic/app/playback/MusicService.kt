package com.bestmusic.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.bestmusic.app.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** บริการเล่นเพลงเบื้องหลัง (มีการแจ้งเตือน + ควบคุมจากหน้าจอล็อก/หูฟังได้) */
@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())

    private var sleepEndAt = 0L
    private var sleepAtEndOfTrack = false
    private val sleepRunnable = Runnable {
        player.pause()
        clearSleep()
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.repeatMode = Player.REPEAT_MODE_ALL

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                openEffectSession(audioSessionId)
                publishExtras()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (sleepAtEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    player.pause()
                    clearSleep()
                }
            }
        })

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(Callback())
            .build()
        openEffectSession(player.audioSessionId)
        publishExtras()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(sleepRunnable)
        closeEffectSession(player.audioSessionId)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    // ---------------- Sleep timer ----------------

    private fun setSleep(minutes: Int) {
        handler.removeCallbacks(sleepRunnable)
        sleepAtEndOfTrack = false
        sleepEndAt = 0L
        when {
            minutes > 0 -> {
                sleepEndAt = System.currentTimeMillis() + minutes * 60_000L
                handler.postDelayed(sleepRunnable, minutes * 60_000L)
            }
            minutes == SLEEP_END_OF_TRACK -> sleepAtEndOfTrack = true
        }
        publishExtras()
    }

    private fun clearSleep() {
        handler.removeCallbacks(sleepRunnable)
        sleepEndAt = 0L
        sleepAtEndOfTrack = false
        publishExtras()
    }

    private fun publishExtras() {
        session?.setSessionExtras(Bundle().apply {
            putInt(EXTRA_AUDIO_SESSION, player.audioSessionId)
            putLong(EXTRA_SLEEP_END, sleepEndAt)
            putBoolean(EXTRA_SLEEP_EOT, sleepAtEndOfTrack)
        })
    }

    // ---------------- Equalizer support ----------------

    private fun openEffectSession(id: Int) {
        if (id == C.AUDIO_SESSION_ID_UNSET) return
        sendBroadcast(Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        })
    }

    private fun closeEffectSession(id: Int) {
        if (id == C.AUDIO_SESSION_ID_UNSET) return
        sendBroadcast(Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        })
    }

    // ---------------- Session callback ----------------

    private inner class Callback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_SLEEP, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CMD_SLEEP) {
                setSleep(args.getInt(ARG_MINUTES, 0))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        /** MediaItem ที่ส่งข้ามมาจาก UI จะไม่มี URI ติดมา จึงต้องเติมกลับจาก requestMetadata */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) item
                else {
                    val uri: Uri? = item.requestMetadata.mediaUri
                    if (uri != null) item.buildUpon().setUri(uri).build() else item
                }
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }
    }

    companion object {
        const val CMD_SLEEP = "best_music.SLEEP"
        const val ARG_MINUTES = "minutes"
        const val SLEEP_END_OF_TRACK = -1

        const val EXTRA_AUDIO_SESSION = "audio_session"
        const val EXTRA_SLEEP_END = "sleep_end"
        const val EXTRA_SLEEP_EOT = "sleep_eot"
    }
}
