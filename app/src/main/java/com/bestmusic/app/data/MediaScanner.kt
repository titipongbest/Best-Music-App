package com.bestmusic.app.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/** อ่านไฟล์เพลงและวิดีโอทั้งหมดจาก MediaStore แล้วจัดกลุ่มตามโฟลเดอร์ */
object MediaScanner {

    private const val MIN_DURATION_MS = 10_000L

    fun scan(context: Context): List<Track> =
        (queryAudio(context) + queryVideo(context))
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    fun groupByFolder(tracks: List<Track>): List<Folder> =
        tracks.groupBy { it.folderPath }
            .map { (path, list) -> Folder(path, list.first().folderName, list) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    private fun queryAudio(context: Context): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.RELATIVE_PATH)
        }.toTypedArray()
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf(MIN_DURATION_MS.toString())

        val out = ArrayList<Track>()
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.long(MediaStore.Audio.Media._ID)
                val path = c.str(MediaStore.Audio.Media.DATA)
                val displayName = c.str(MediaStore.Audio.Media.DISPLAY_NAME)
                val (folderPath, folderName) = folderOf(path, c.strOrNull(MediaStore.MediaColumns.RELATIVE_PATH))
                val rawTitle = c.str(MediaStore.Audio.Media.TITLE).ifBlank { displayName.substringBeforeLast('.') }
                val (title, artist) = cleanTitleArtist(rawTitle, c.str(MediaStore.Audio.Media.ARTIST))
                out += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = title,
                    artist = artist,
                    album = c.str(MediaStore.Audio.Media.ALBUM),
                    albumId = c.long(MediaStore.Audio.Media.ALBUM_ID),
                    durationMs = c.long(MediaStore.Audio.Media.DURATION),
                    filePath = path,
                    folderPath = folderPath,
                    folderName = folderName,
                    isVideo = false,
                    dateAdded = c.long(MediaStore.Audio.Media.DATE_ADDED),
                    dateModified = c.long(MediaStore.Audio.Media.DATE_MODIFIED)
                        .takeIf { it > 0 } ?: (fileModifiedSec(path)),
                )
            }
        }
        return out
    }

    private fun queryVideo(context: Context): List<Track> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.DATA)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.DATE_MODIFIED)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.ARTIST)
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        val out = ArrayList<Track>()
        context.contentResolver.query(collection, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.long(MediaStore.Video.Media._ID)
                val path = c.str(MediaStore.Video.Media.DATA)
                val displayName = c.str(MediaStore.Video.Media.DISPLAY_NAME)
                val (folderPath, folderName) = folderOf(path, c.strOrNull(MediaStore.MediaColumns.RELATIVE_PATH))
                val rawTitle = c.str(MediaStore.Video.Media.TITLE).ifBlank { displayName.substringBeforeLast('.') }
                val (title, artist) = cleanTitleArtist(rawTitle, c.strOrNull(MediaStore.MediaColumns.ARTIST) ?: "")
                out += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = title,
                    artist = artist,
                    album = "",
                    albumId = -1,
                    durationMs = c.long(MediaStore.Video.Media.DURATION),
                    filePath = path,
                    folderPath = folderPath,
                    folderName = folderName,
                    isVideo = true,
                    dateAdded = c.long(MediaStore.Video.Media.DATE_ADDED),
                    dateModified = c.long(MediaStore.Video.Media.DATE_MODIFIED)
                        .takeIf { it > 0 } ?: (fileModifiedSec(path)),
                )
            }
        }
        return out
    }

    /** ถ้าไม่มีชื่อศิลปิน ลองแยกจากชื่อไฟล์รูปแบบ "ศิลปิน - ชื่อเพลง" */
    private fun cleanTitleArtist(title: String, artist: String): Pair<String, String> {
        val unknown = artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)
        if (!unknown) return title to artist
        val idx = title.indexOf(" - ")
        if (idx > 0) {
            val guessed = title.substring(0, idx)
                .split(Regex("(?i)\\s+(feat\\.?|ft\\.?|x)\\s+")).first().trim()
            if (guessed.isNotBlank()) return title to guessed
        }
        return title to "ไม่ทราบศิลปิน"
    }

    private fun folderOf(dataPath: String, relativePath: String?): Pair<String, String> {
        if (dataPath.isNotBlank()) {
            val parent = File(dataPath).parentFile
            if (parent != null) return parent.absolutePath to (parent.name.ifBlank { "/" })
        }
        val rel = relativePath?.trimEnd('/') ?: ""
        return rel to (rel.substringAfterLast('/').ifBlank { "Music" })
    }

    private fun fileModifiedSec(path: String): Long =
        runCatching { File(path).lastModified() / 1000 }.getOrDefault(0L)

    private fun Cursor.idx(col: String) = getColumnIndex(col)
    private fun Cursor.long(col: String): Long = idx(col).let { if (it >= 0 && !isNull(it)) getLong(it) else 0L }
    private fun Cursor.str(col: String): String = strOrNull(col) ?: ""
    private fun Cursor.strOrNull(col: String): String? = idx(col).let { if (it >= 0 && !isNull(it)) getString(it) else null }

    fun albumArtUri(albumId: Long): Uri? =
        if (albumId > 0) ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId) else null
}
