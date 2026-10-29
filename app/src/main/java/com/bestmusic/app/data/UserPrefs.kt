package com.bestmusic.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * เก็บข้อมูลของผู้ใช้ไว้ในเครื่อง: รายการโปรด เพลย์ลิสต์ เพลงที่ซ่อน
 * ชื่อเพลงที่แก้ไข เนื้อเพลง และโหมดประหยัดพลังงาน
 */
class UserPrefs(context: Context) {

    private val sp = context.getSharedPreferences("best_music", Context.MODE_PRIVATE)
    private val lyricsSp = context.getSharedPreferences("best_music_lyrics", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(sp.getStringSet(K_FAV, emptySet())!!.toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _hidden = MutableStateFlow(sp.getStringSet(K_HIDDEN, emptySet())!!.toSet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    private val _playlists = MutableStateFlow(readPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _overrides = MutableStateFlow(readOverrides())
    val overrides: StateFlow<Map<String, TrackOverride>> = _overrides.asStateFlow()

    private val _batterySaver = MutableStateFlow(sp.getBoolean(K_SAVER, false))
    val batterySaver: StateFlow<Boolean> = _batterySaver.asStateFlow()

    private val _sortOrder = MutableStateFlow(
        runCatching { SortOrder.valueOf(sp.getString(K_SORT, SortOrder.DEFAULT.name)!!) }.getOrDefault(SortOrder.DEFAULT),
    )
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        sp.edit().putString(K_SORT, order.name).apply()
    }

    // ---------- Pinned folders ----------
    private val _pinned = MutableStateFlow(
        sp.getString(K_PINNED, "")!!.split('\n').filter { it.isNotBlank() },
    )
    /** โฟลเดอร์ที่ปักหมุด (อันที่ปักล่าสุดอยู่บนสุด) */
    val pinnedFolders: StateFlow<List<String>> = _pinned.asStateFlow()

    fun togglePin(path: String) {
        val list = _pinned.value.toMutableList()
        if (!list.remove(path)) list.add(0, path)
        _pinned.value = list
        sp.edit().putString(K_PINNED, list.joinToString("\n")).apply()
    }

    // ---------- Favorites ----------
    fun toggleFavorite(key: String) {
        val s = _favorites.value.toMutableSet()
        if (!s.add(key)) s.remove(key)
        _favorites.value = s
        sp.edit().putStringSet(K_FAV, s).apply()
    }

    // ---------- Hidden ----------
    fun hide(key: String) = hideAll(listOf(key))

    fun hideAll(keys: Collection<String>) {
        val s = _hidden.value + keys
        _hidden.value = s
        sp.edit().putStringSet(K_HIDDEN, s).apply()
    }

    fun unhideAll() {
        _hidden.value = emptySet()
        sp.edit().remove(K_HIDDEN).apply()
    }

    // ---------- Playlists ----------
    fun createPlaylist(name: String, firstKey: String? = null) {
        if (_playlists.value.any { it.name == name }) {
            if (firstKey != null) addToPlaylist(name, firstKey)
            return
        }
        savePlaylists(_playlists.value + Playlist(name, listOfNotNull(firstKey)))
    }

    fun addToPlaylist(name: String, key: String) = addAllToPlaylist(name, listOf(key))

    fun addAllToPlaylist(name: String, keys: List<String>) {
        if (_playlists.value.none { it.name == name }) {
            savePlaylists(_playlists.value + Playlist(name, keys.distinct()))
            return
        }
        savePlaylists(_playlists.value.map { p ->
            if (p.name == name) p.copy(keys = p.keys + keys.filter { it !in p.keys }.distinct()) else p
        })
    }

    fun removeFromPlaylist(name: String, key: String) = removeAllFromPlaylist(name, listOf(key))

    fun removeAllFromPlaylist(name: String, keys: Collection<String>) {
        val drop = keys.toSet()
        savePlaylists(_playlists.value.map { if (it.name == name) it.copy(keys = it.keys.filterNot { k -> k in drop }) else it })
    }

    fun deletePlaylist(name: String) {
        savePlaylists(_playlists.value.filterNot { it.name == name })
    }

    private fun savePlaylists(list: List<Playlist>) {
        _playlists.value = list
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().put("name", p.name).put("keys", JSONArray(p.keys)))
        }
        sp.edit().putString(K_PLAYLISTS, arr.toString()).apply()
    }

    private fun readPlaylists(): List<Playlist> = runCatching {
        val arr = JSONArray(sp.getString(K_PLAYLISTS, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val keys = o.getJSONArray("keys")
            Playlist(o.getString("name"), (0 until keys.length()).map { keys.getString(it) })
        }
    }.getOrDefault(emptyList())

    // ---------- Title / artist overrides ----------
    fun setOverride(key: String, title: String?, artist: String?) {
        val m = _overrides.value.toMutableMap()
        m[key] = TrackOverride(title?.takeIf { it.isNotBlank() }, artist?.takeIf { it.isNotBlank() })
        _overrides.value = m
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, JSONObject().put("t", v.title ?: "").put("a", v.artist ?: "")) }
        sp.edit().putString(K_OVERRIDES, o.toString()).apply()
    }

    private fun readOverrides(): Map<String, TrackOverride> = runCatching {
        val o = JSONObject(sp.getString(K_OVERRIDES, "{}")!!)
        o.keys().asSequence().associateWith { k ->
            val v = o.getJSONObject(k)
            TrackOverride(v.optString("t").ifBlank { null }, v.optString("a").ifBlank { null })
        }
    }.getOrDefault(emptyMap())

    // ---------- Lyrics ----------
    fun lyrics(key: String): String? = lyricsSp.getString(key, null)
    fun setLyrics(key: String, text: String) {
        lyricsSp.edit().apply { if (text.isBlank()) remove(key) else putString(key, text) }.apply()
    }

    // ---------- Battery saver ----------
    fun setBatterySaver(on: Boolean) {
        _batterySaver.value = on
        sp.edit().putBoolean(K_SAVER, on).apply()
    }

    private companion object {
        const val K_FAV = "favorites"
        const val K_HIDDEN = "hidden"
        const val K_PLAYLISTS = "playlists"
        const val K_OVERRIDES = "overrides"
        const val K_SAVER = "battery_saver"
        const val K_SORT = "sort_order"
        const val K_PINNED = "pinned_folders"
    }
}
