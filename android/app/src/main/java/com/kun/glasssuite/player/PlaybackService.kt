package com.kun.glasssuite.player

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.kun.glasssuite.R

/**
 * 媒体会话服务：锁屏媒体通知（标题/艺术家/进度/播放控制）+ 状态栏常驻。
 * 封面加载由 MediaSession 元数据携带（默认通知布局）。
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlayerManager.player

        mediaSession = MediaSession.Builder(this, player).build()

        notificationManager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
            .setChannelNameResourceId(R.string.app_name)
            .setChannelDescriptionResourceId(R.string.app_name)
            .setSmallIconResourceId(R.drawable.ic_notification)
            .build()
            .also {
                it.setPlayer(player)
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = PlayerManager.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "playback"
    }
}
