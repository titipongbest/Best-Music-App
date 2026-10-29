package com.bestmusic.app.data

import android.net.Uri

/** เพลงหรือวิดีโอ (MV) หนึ่งไฟล์ในเครื่อง */
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val filePath: String,
    val folderPath: String,
    val folderName: String,
    val isVideo: Boolean,
    val dateAdded: Long,
) {
    /** คีย์ที่ไม่ซ้ำกันระหว่างเสียงกับวิดีโอ ใช้เป็น mediaId ของ ExoPlayer ด้วย */
    val key: String get() = keyOf(isVideo, id)

    companion object {
        fun keyOf(isVideo: Boolean, id: Long) = (if (isVideo) "v:" else "a:") + id
    }
}

data class Folder(
    val path: String,
    val name: String,
    val tracks: List<Track>,
) {
    val audioCount get() = tracks.count { !it.isVideo }
    val videoCount get() = tracks.count { it.isVideo }
}

data class Playlist(
    val name: String,
    val keys: List<String>,
)

data class TrackOverride(
    val title: String?,
    val artist: String?,
)
