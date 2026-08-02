package com.kun.cloudmusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.kun.cloudmusic.data.Api
import com.kun.cloudmusic.data.AppConfig
import com.kun.cloudmusic.data.SettingsStore
import com.kun.cloudmusic.player.PlayerManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class App : Application() {

    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        runBlocking {
            val s = settings.data.first()
            AppConfig.apiBaseUrl = s.apiBaseUrl
            AppConfig.accentHex = s.accentHex
            AppConfig.darkMode = s.darkMode
            AppConfig.lyricFontSize = s.lyricFontSize
            AppConfig.lyricOffsetMs = s.lyricOffsetMs
            AppConfig.lyricMode = s.lyricMode
            AppConfig.diyLyric = s.diyLyric
            AppConfig.quality = s.quality
            AppConfig.userId = s.userId
            AppConfig.profileJson = s.profileJson
        }
        Api.init(this)
        PlayerManager.init(this, settings)
        createNotificationChannel()
        // 恢复登录态内存镜像
        runCatching {
            val p = com.google.gson.Gson().fromJson(AppConfig.profileJson, com.kun.cloudmusic.data.Profile::class.java)
            AppConfig.setProfile(p)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "playback",
            "播放控制",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "显示当前播放歌曲与锁屏控制" }
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            .createNotificationChannel(channel)
    }
}
