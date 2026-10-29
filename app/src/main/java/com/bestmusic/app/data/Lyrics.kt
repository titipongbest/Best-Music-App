package com.bestmusic.app.data

import java.io.File

data class LyricLine(val timeMs: Long, val text: String)

/** เนื้อเพลง: ถ้ามีเวลากำกับ (.lrc) จะไฮไลต์บรรทัดตามเพลงได้ */
data class Lyrics(val lines: List<LyricLine>, val synced: Boolean) {

    fun activeIndex(positionMs: Long): Int {
        if (!synced) return -1
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }

    companion object {
        private val tag = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

        fun parse(raw: String): Lyrics {
            val synced = ArrayList<LyricLine>()
            val plain = ArrayList<LyricLine>()
            raw.lineSequence().forEach { line ->
                val matches = tag.findAll(line).toList()
                val text = line.replace(tag, "").trim()
                if (matches.isEmpty()) {
                    // ข้ามแท็ก metadata เช่น [ar:...] [ti:...]
                    if (!Regex("""^\[[a-zA-Z]+:.*]$""").matches(line.trim())) plain += LyricLine(0, line.trim())
                } else {
                    matches.forEach { m ->
                        val min = m.groupValues[1].toLong()
                        val sec = m.groupValues[2].toLong()
                        val fracStr = m.groupValues[3]
                        val frac = when (fracStr.length) {
                            0 -> 0L
                            1 -> fracStr.toLong() * 100
                            2 -> fracStr.toLong() * 10
                            else -> fracStr.take(3).toLong()
                        }
                        synced += LyricLine(min * 60_000 + sec * 1000 + frac, text)
                    }
                }
            }
            return if (synced.isNotEmpty()) Lyrics(synced.sortedBy { it.timeMs }, true)
            else Lyrics(plain.dropWhile { it.text.isEmpty() }, false)
        }

        /** หาไฟล์ .lrc / .txt ชื่อเดียวกับเพลงในโฟลเดอร์เดียวกัน */
        fun loadSidecar(track: Track): String? {
            if (track.filePath.isBlank()) return null
            val base = track.filePath.substringBeforeLast('.')
            for (ext in listOf(".lrc", ".LRC", ".txt")) {
                val f = File(base + ext)
                val text = runCatching { if (f.canRead()) f.readText() else null }.getOrNull()
                if (!text.isNullOrBlank()) return text
            }
            return null
        }
    }
}
