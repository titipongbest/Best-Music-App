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

enum class SortOrder(val label: String) {
    DEFAULT("ค่าเริ่มต้น"),
    TITLE_ASC("ชื่อเพลง (ก-ฮ, A-Z)"),
    TITLE_DESC("ชื่อเพลง (ฮ-ก, Z-A)"),
    ARTIST("ศิลปิน"),
    DATE_NEW("เพิ่มล่าสุด"),
    DATE_OLD("เพิ่มเก่าสุด"),
    DURATION_LONG("ความยาว (มากไปน้อย)"),
    DURATION_SHORT("ความยาว (น้อยไปมาก)");

    fun apply(list: List<Track>): List<Track> = when (this) {
        DEFAULT -> list
        TITLE_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        TITLE_DESC -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
        ARTIST -> list.sortedWith(
            compareBy<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )
        DATE_NEW -> list.sortedByDescending { it.dateAdded }
        DATE_OLD -> list.sortedBy { it.dateAdded }
        DURATION_LONG -> list.sortedByDescending { it.durationMs }
        DURATION_SHORT -> list.sortedBy { it.durationMs }
    }
}
