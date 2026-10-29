package com.bestmusic.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/** โหลดภาพปกจากไฟล์เพลง (ภาพที่ฝังในไฟล์) หรือเฟรมจากวิดีโอ พร้อมแคช */
object ArtworkLoader {

    private val cache = object : LruCache<String, Bitmap>(40 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val missing = HashSet<String>()

    suspend fun load(context: Context, track: Track, sizePx: Int): Bitmap? {
        val cacheKey = "${track.key}@$sizePx"
        cache.get(cacheKey)?.let { return it }
        synchronized(missing) { if (cacheKey in missing) return null }

        val bmp = withContext(Dispatchers.IO) {
            loadThumbnail(context, track, sizePx) ?: loadWithRetriever(context, track, sizePx)
        }
        if (bmp != null) cache.put(cacheKey, bmp) else synchronized(missing) { missing += cacheKey }
        return bmp
    }

    private fun loadThumbnail(context: Context, track: Track, sizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            context.contentResolver.loadThumbnail(track.uri, Size(sizePx, sizePx), null)
        }.getOrNull()
    }

    private fun loadWithRetriever(context: Context, track: Track, sizePx: Int): Bitmap? = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, track.uri)
            val bytes = r.embeddedPicture
            when {
                bytes != null -> decodeSampled(bytes, sizePx)
                track.isVideo -> r.getFrameAtTime(5_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { scaleDown(it, sizePx) }
                else -> null
            }
        } finally {
            runCatching { r.release() }
        }
    }.getOrNull()

    private fun decodeSampled(bytes: ByteArray, sizePx: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        var sample = 1
        while (max(opts.outWidth, opts.outHeight) / (sample * 2) >= sizePx) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun scaleDown(b: Bitmap, sizePx: Int): Bitmap {
        val longest = max(b.width, b.height)
        if (longest <= sizePx) return b
        val f = sizePx.toFloat() / longest
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt(), (b.height * f).toInt(), true)
    }
}

@Composable
fun rememberArtwork(track: Track?, sizePx: Int = 512): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, track?.key, sizePx) {
        value = track?.let { ArtworkLoader.load(context, it, sizePx)?.asImageBitmap() }
    }
}

/** ชุดสีของหน้าเล่นเพลง ดึงจากภาพปก (แบบในภาพตัวอย่าง: พื้นเข้ม + สีหลักพาสเทล) */
@Immutable
data class PlayerColors(
    val background: Color,
    val sheet: Color,
    val accent: Color,
    val onBackground: Color = Color.White,
    val secondary: Color = Color(0xFFB4B4B4),
) {
    companion object {
        val Default = PlayerColors(
            background = Color(0xFF2B2B2D),
            sheet = Color(0xFF353537),
            accent = Color(0xFFD9C3C4),
        )
        val BatterySaver = PlayerColors(
            background = Color.Black,
            sheet = Color(0xFF151515),
            accent = Color(0xFFE0E0E0),
        )

        fun from(bitmap: Bitmap?): PlayerColors {
            if (bitmap == null) return Default
            val soft = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE)
                bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
            val p = Palette.from(soft).maximumColorCount(24).generate()
            val dominant = p.dominantSwatch?.rgb ?: return Default
            val base = Color(p.darkMutedSwatch?.rgb ?: p.mutedSwatch?.rgb ?: dominant)
            val accentSrc = Color(
                p.vibrantSwatch?.rgb ?: p.lightVibrantSwatch?.rgb ?: p.lightMutedSwatch?.rgb ?: dominant
            )
            val bg = lerp(base, Color.Black, 0.72f).let { if (it.luminance() > 0.06f) lerp(it, Color.Black, 0.4f) else it }
            val accent = ensureLight(lerp(accentSrc, Color.White, 0.38f))
            return PlayerColors(
                background = bg,
                sheet = lerp(bg, Color.White, 0.06f),
                accent = accent,
            )
        }

        private fun ensureLight(c: Color): Color {
            var out = c
            var i = 0
            while (out.luminance() < 0.42f && i < 6) { out = lerp(out, Color.White, 0.2f); i++ }
            return out
        }

        private fun lerp(a: Color, b: Color, t: Float) = Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = 1f,
        )

        private fun Color.luminance(): Float {
            val argb = toArgb()
            return androidx.core.graphics.ColorUtils.calculateLuminance(argb).toFloat()
        }
    }
}

@Composable
fun rememberPlayerColors(track: Track?, batterySaver: Boolean): State<PlayerColors> {
    val context = LocalContext.current
    // เก็บสีเดิมไว้ระหว่างโหลดสีของเพลงใหม่ เพื่อให้สีพื้นหลังค่อย ๆ เปลี่ยนโดยไม่กระพริบ
    val state = remember { mutableStateOf(if (batterySaver) PlayerColors.BatterySaver else PlayerColors.Default) }
    LaunchedEffect(track?.key, batterySaver) {
        state.value = if (batterySaver) PlayerColors.BatterySaver else withContext(Dispatchers.Default) {
            val bmp = track?.let { ArtworkLoader.load(context, it, 512) }
            PlayerColors.from(bmp)
        }
    }
    return state
}
