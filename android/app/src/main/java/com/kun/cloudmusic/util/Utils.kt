package com.kun.cloudmusic.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object Utils {
    /** data:image/png;base64,xxx -> Bitmap */
    fun base64DataUriToBitmap(uri: String?): Bitmap? {
        if (uri.isNullOrBlank()) return null
        val idx = uri.indexOf(',')
        val b64 = if (idx >= 0) uri.substring(idx + 1) else uri
        return runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    fun formatPlayCount(count: Long?): String {
        if (count == null) return ""
        return when {
            count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
            count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
            else -> count.toString()
        }
    }
}
