package com.kun.cloudmusic.player

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.kun.cloudmusic.App
import com.kun.cloudmusic.R
import io.coil.Coil
import io.coil.imageLoader
import io.coil.request.ImageRequest
import io.coil.size.Size

/**
 * 媒体会话服务：锁屏媒体通知（封面/进度/播放控制）+ 状态栏常驻播放信息。
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
            .setImageLoader(object : PlayerNotificationManager.BitmapLoader {
                override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
                    val future = SettableFuture.create<Bitmap>()
                    val request = ImageRequest.Builder(this@PlaybackService)
                        .data(uri)
                        .allowHardware(false)
                        .size(Size.ORIGINAL)
                        .target(
                            onSuccess = { result -> future.set(result) },
                            onError = { throwable ->
                                future.setException(throwable ?: RuntimeException("load failed"))
                            }
                        )
                        .build()
                    Coil.imageLoader(this@PlaybackService).enqueue(request)
                    return future
                }

                override fun clear() = Unit
            })
            .build()
            .also {
                it.setPlayer(player)
                it.setPriority(PlayerNotificationManager.PRIORITY_DEFAULT)
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

        fun start(context: android.content.Context) {
            val intent = Intent(context, PlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
