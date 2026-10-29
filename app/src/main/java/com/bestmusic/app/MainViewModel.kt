package com.bestmusic.app

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.media.RingtoneManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bestmusic.app.data.Folder
import com.bestmusic.app.data.MediaScanner
import com.bestmusic.app.data.Playlist
import com.bestmusic.app.data.Track
import com.bestmusic.app.data.UserPrefs
import com.bestmusic.app.playback.PlayMode
import com.bestmusic.app.playback.PlayerConnection
import com.bestmusic.app.playback.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bestmusic.app.data.Lyrics as LyricsData

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = UserPrefs(app)
    val player = PlayerConnection(app, viewModelScope)

    private val _rawTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** ทุกไฟล์ (รวมที่ซ่อน) พร้อมชื่อที่ผู้ใช้แก้ไขแล้ว */
    private val allTracks: StateFlow<List<Track>> =
        combine(_rawTracks, prefs.overrides) { raw, ov ->
            if (ov.isEmpty()) raw else raw.map { t ->
                val o = ov[t.key] ?: return@map t
                t.copy(title = o.title ?: t.title, artist = o.artist ?: t.artist)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val byKey: StateFlow<Map<String, Track>> =
        allTracks.map { list -> list.associateBy { it.key } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val tracks: StateFlow<List<Track>> =
        combine(allTracks, prefs.hidden) { list, hidden -> list.filterNot { it.key in hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<Folder>> =
        tracks.map { MediaScanner.groupByFolder(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val videos: StateFlow<List<Track>> =
        tracks.map { list -> list.filter { it.isVideo } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<Track>> =
        combine(tracks, prefs.favorites) { list, fav -> list.filter { it.key in fav } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> = prefs.playlists

    val playerState: StateFlow<PlayerState> = player.state

    val currentTrack: StateFlow<Track?> =
        combine(player.state, byKey) { s, map -> s.currentKey?.let { map[it] } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        player.connect()
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    fun scan() {
        viewModelScope.launch {
            _loading.value = true
            _rawTracks.value = withContext(Dispatchers.IO) {
                runCatching { MediaScanner.scan(getApplication()) }.getOrDefault(emptyList())
            }
            _loading.value = false
        }
    }

    fun trackByKey(key: String): Track? = byKey.value[key]
    fun folder(path: String): Folder? = folders.value.firstOrNull { it.path == path }
    fun playlistTracks(p: Playlist): List<Track> {
        val visible = tracks.value.associateBy { it.key }
        return p.keys.mapNotNull { visible[it] }
    }

    // ---------------- Playback ----------------

    fun play(list: List<Track>, index: Int) {
        if (list.isEmpty()) return
        player.play(list, index, { it.title }, { it.artist })
    }

    fun playOrPause() = player.playOrPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun cycleMode(): PlayMode = player.cycleMode()
    fun setSpeed(speed: Float) = player.setSpeed(speed)
    fun setSleepTimer(minutes: Int) = player.setSleepTimer(minutes)
    fun skipToQueueIndex(i: Int) = player.skipToQueueIndex(i)

    // ---------------- Library actions ----------------

    fun toggleFavorite(key: String) = prefs.toggleFavorite(key)
    fun createPlaylist(name: String, key: String?) = prefs.createPlaylist(name.trim(), key)
    fun addToPlaylist(name: String, key: String) = prefs.addToPlaylist(name, key)
    fun removeFromPlaylist(name: String, key: String) = prefs.removeFromPlaylist(name, key)
    fun deletePlaylist(name: String) = prefs.deletePlaylist(name)
    fun setBatterySaver(on: Boolean) = prefs.setBatterySaver(on)

    fun hide(track: Track) {
        prefs.hide(track.key)
        player.removeFromQueue(track.key)
    }

    fun unhideAll() = prefs.unhideAll()

    fun editInfo(track: Track, title: String, artist: String) {
        prefs.setOverride(track.key, title, artist)
        player.updateMetadata(track, title.ifBlank { track.title }, artist.ifBlank { track.artist })
    }

    suspend fun loadLyrics(track: Track): String? =
        prefs.lyrics(track.key) ?: withContext(Dispatchers.IO) { LyricsData.loadSidecar(track) }

    fun saveLyrics(track: Track, text: String) = prefs.setLyrics(track.key, text)

    /** true = ตั้งเสียงเรียกเข้าแล้ว, false = ต้องขออนุญาต "แก้ไขการตั้งค่าระบบ" ก่อน */
    fun canWriteSettings(): Boolean = Settings.System.canWrite(getApplication())

    fun setAsRingtone(track: Track): Boolean = runCatching {
        RingtoneManager.setActualDefaultRingtoneUri(getApplication(), RingtoneManager.TYPE_RINGTONE, track.uri)
        true
    }.getOrDefault(false)

    sealed interface DeleteResult {
        data object Done : DeleteResult
        data class NeedsConfirm(val sender: IntentSender) : DeleteResult
        data object Failed : DeleteResult
    }

    /** ลบไฟล์จริงออกจากเครื่อง (Android 10+ ระบบจะให้ผู้ใช้กดยืนยันก่อน) */
    fun requestDelete(track: Track): DeleteResult {
        val resolver = getApplication<Application>().contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return DeleteResult.NeedsConfirm(MediaStore.createDeleteRequest(resolver, listOf(track.uri)).intentSender)
        }
        return try {
            val rows = resolver.delete(track.uri, null, null)
            if (rows > 0) { onDeleted(track); DeleteResult.Done } else DeleteResult.Failed
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                DeleteResult.NeedsConfirm(e.userAction.actionIntent.intentSender)
            } else DeleteResult.Failed
        }
    }

    /** เรียกหลังผู้ใช้ยืนยันการลบ */
    fun confirmDeleted(track: Track) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            runCatching { getApplication<Application>().contentResolver.delete(track.uri, null, null) }
        }
        onDeleted(track)
    }

    private fun onDeleted(track: Track) {
        player.removeFromQueue(track.key)
        _rawTracks.value = _rawTracks.value.filterNot { it.key == track.key }
    }
}
