package com.kun.cloudmusic.util

data class LrcLine(val time: Long, val text: String)

object LrcParser {

    private val TIME_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    /** 解析 LRC 文本为按时间排序的行 */
    fun parse(lrc: String?): List<LrcLine> {
        if (lrc.isNullOrBlank()) return emptyList()
        val result = mutableListOf<LrcLine>()
        lrc.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val matches = TIME_TAG.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.replace(TIME_TAG, "").trim()
            matches.forEach { m ->
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val frac = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                result += LrcLine(min * 60_000 + sec * 1000 + frac * 10, text)
            }
        }
        return result.sortedBy { it.time }
    }

    /** 将翻译行按最近时间戳对齐到原词行时间 */
    fun align(trans: List<LrcLine>, origin: List<LrcLine>): Map<Long, String> {
        if (trans.isEmpty() || origin.isEmpty()) return emptyMap()
        val map = mutableMapOf<Long, String>()
        for (t in trans) {
            val nearest = origin.minByOrNull { kotlin.math.abs(it.time - t.time) } ?: continue
            if (kotlin.math.abs(nearest.time - t.time) <= 1500) {
                map[nearest.time] = t.text
            }
        }
        return map
    }

    /** 合并原词与翻译，生成展示行 */
    fun merge(origin: List<LrcLine>, transMap: Map<Long, String>): List<Pair<LrcLine, String?>> {
        return origin.map { it to transMap[it.time] }
    }
}
