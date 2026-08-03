package com.kun.glasssuite.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 内存中的全局配置（由 DataStore 初始化并同步） */
object AppConfig {
    const val DEFAULT_API = "http://10.0.2.2:3000"

    var apiBaseUrl: String = DEFAULT_API
    /** true=直连网易云官方接口（默认，无需部署服务器）；false=自托管 NeteaseCloudMusicApi */
    var directMode: Boolean = true
    var accentHex: String = "#4D7CFE"
    var darkMode: Boolean = false
    var themeMode: Int = 0          // 0 跟随系统 1 浅色 2 深色
    var lyricFontSize: Int = 18
    var lyricOffsetMs: Int = 0
    var lyricMode: Int = 0          // 0 原词 1 翻译 2 罗马音 3 DIY
    var diyLyric: String = ""
    var quality: String = "exhigh"  // standard/higher/exhigh/lossless
    var ghOwner: String = "jiangtengqiao"
    var ghRepo: String = "GlassSuite"
    var userId: Long = 0L
    var profileJson: String = ""

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile

    fun setProfile(p: Profile?) {
        _profile.value = p
    }

    val isLoggedIn: Boolean get() = userId > 0L
}
