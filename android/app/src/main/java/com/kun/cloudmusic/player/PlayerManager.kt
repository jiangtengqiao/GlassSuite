package com.kun.cloudmusic.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import com.kun.cloudmusic.data.Api
import com.kun.cloudmusic.data.AppConfig
import com.kun.cloudmusic.data.SettingsStore
import com.kun.cloudmusic.data.Song
import com.kun.cloudmusic.util.LrcLine
import com.kun.cloudmusic.util.LrcParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 全局播放器：队列、音质、歌词、喜欢状态。
 * 播放地址实时请求（/song/url/v1），VIP/版权受限自动降档重试。
 */
object PlayerManager {

    lateinit var appContext: Context
        private set
    lateinit var settings: SettingsStore
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _quality = MutableStateFlow(AppConfig.quality)
    val quality: StateFlow<String> = _quality.asStateFlow()

    private val _lyric = MutableStateFlow<List<LrcLine>>(emptyList())
    val lyric: StateFlow<List<LrcLine>> = _lyric.asStateFlow()

    private val _tlyric = MutableStateFlow<List<LrcLine>>(emptyList())
    val tlyric: StateFlow<List<LrcLine>> = _tlyric.asStateFlow()

    private val _romalrc = MutableStateFlow<List<LrcLine>>(emptyList())
    val romalrc: StateFlow<List<LrcLine>> = _romalrc.asStateFlow()

    private val _likedIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedIds: StateFlow<Set<Long>> = _likedIds.asStateFlow()

    private val _repeatOne = MutableStateFlow(false)
    val repeatOne: StateFlow<Boolean> = _repeatOne.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: kotlinx.coroutines.flow.SharedFlow<String> = _toast.asSharedFlow()

    private var positionTicker: Job? = null

    val player: ExoPlayer by lazy {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Api.UA)
            .setDefaultRequestProperties(
                mapOf("Referer" to "https://music.163.com/", "User-Agent" to Api.UA)
            )
        ExoPlayer.Builder(appContext)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
            .apply { addListener(playerListener) }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playing.value = isPlaying
            if (isPlaying) startTicker() else stopTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> _duration.value = player.duration.coerceAtLeast(0)
                Player.STATE_ENDED -> {
                    if (_repeatOne.value) {
                        player.seekTo(0)
                        player.play()
                    } else {
                        next()
                    }
                }
                Player.STATE_IDLE -> {
                    if (player.playerError != null) {
                        val e = player.playerError
                        if (e?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            e?.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                        ) {
                            downgradeAndRetry()
                        }
                    }
                }
            }
        }
    }

    fun init(context: Context, store: SettingsStore) {
        appContext = context.applicationContext
        settings = store
        // 触发播放器懒初始化
        runCatching { player }
    }

    // ================= 对外操作 =================

    fun playQueue(songs: List<Song>, start: Int) {
        if (songs.isEmpty()) return
        _queue.value = songs
        loadIndex(start.coerceIn(0, songs.size - 1))
    }

    fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun toggle() {
        if (player.mediaItemCount == 0 && _currentSong.value == null) return
        if (player.playWhenReady) player.pause() else player.play()
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return
        val cur = _index.value
        val target = if (_shuffle.value) {
            (0 until q.size).filter { it != cur }.randomOrNull() ?: 0
        } else {
            if (cur + 1 >= q.size) 0 else cur + 1
        }
        loadIndex(target)
    }

    fun prev() {
        val q = _queue.value
        if (q.isEmpty()) return
        val cur = _index.value
        val target = if (cur - 1 < 0) q.size - 1 else cur - 1
        loadIndex(target)
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceAtLeast(0))
    }

    fun toggleRepeatOne() {
        _repeatOne.value = !_repeatOne.value
        if (_repeatOne.value) _shuffle.value = false
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        if (_shuffle.value) _repeatOne.value = false
    }

    /** 切换音质：保持当前进度重新加载 */
    fun setQuality(level: String) {
        if (_quality.value == level) return
        _quality.value = level
        AppConfig.quality = level
        scope.launch { settings.setQuality(level) }
        val pos = _position.value
        val song = _currentSong.value ?: return
        if (song.id > 0) {
            _toast.tryEmit("正在切换音质…")
            loadIndex(_index.value, resumePosition = pos)
        }
    }

    fun refreshQueue(songs: List<Song>) {
        _queue.value = songs
    }

    // ================= 内部加载 =================

    private fun loadIndex(i: Int, resumePosition: Long = -1L) {
        val q = _queue.value
        if (q.isEmpty() || i !in q.indices) return
        _index.value = i
        val song = q[i]
        _currentSong.value = song
        _playing.value = false
        stopTicker()

        scope.launch {
            val url = fetchUrlWithFallback(song.id)
            if (url == null) {
                _toast.tryEmit("「${song.name}」暂无可用音源（可能受版权或会员限制）")
                delay(1200)
                if (_currentSong.value?.id == song.id) next()
                return@launch
            }
            val item = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.name ?: "未知歌曲")
                        .setArtist(song.artistNames)
                        .setAlbumTitle(song.albumName)
                        .setArtworkUri((song.albumPic ?: "").toUri())
                        .build()
                )
                .build()
            player.setMediaItem(item)
            player.prepare()
            if (resumePosition > 0) player.seekTo(resumePosition)
            player.play()
        }

        scope.launch { loadLyric(song.id) }
        scope.launch { updateLiked(song.id) }
    }

    /** 按当前音质请求直链，失败则逐级降档 */
    private suspend fun fetchUrlWithFallback(songId: Long): String? {
        val levels = Api.QUALITY_LEVELS
        val startIdx = levels.indexOf(_quality.value).coerceAtLeast(0)
        for (idx in startIdx downTo 0) {
            val level = levels[idx]
            val resp = runCatching { Api.service.songUrl(songId, level) }.getOrNull()
            val url = resp?.data?.firstOrNull()?.url
            if (!url.isNullOrBlank()) {
                if (idx < startIdx) _toast.tryEmit("已自动切换至${Api.QUALITY_NAMES[level]}")
                return url
            }
        }
        return null
    }

    private suspend fun loadLyric(songId: Long) {
        val resp = runCatching { Api.service.lyric(songId) }.getOrNull()
        if (resp == null) {
            _lyric.value = emptyList()
            _tlyric.value = emptyList()
            _romalrc.value = emptyList()
            return
        }
        val lrc = LrcParser.parse(resp.lrc?.lyric)
        val tly = LrcParser.parse(resp.tlyric?.lyric)
        val roma = LrcParser.parse(resp.romalrc?.lyric)
        _lyric.value = lrc
        _tlyric.value = tly
        _romalrc.value = roma
    }

    private suspend fun updateLiked(songId: Long) {
        val uid = AppConfig.userId
        if (uid <= 0L) return
        val resp = runCatching { Api.service.likeList(uid) }.getOrNull()
        if (resp?.ids != null) {
            _likedIds.value = resp.ids.toSet()
        }
    }

    fun isLiked(songId: Long): Boolean = songId in _likedIds.value

    fun toggleLike(songId: Long) {
        val liked = isLiked(songId)
        scope.launch {
            val resp = runCatching { Api.service.like(songId, !liked) }.getOrNull()
            if (resp?.code == 200) {
                _likedIds.value = if (liked) _likedIds.value - songId else _likedIds.value + songId
                _toast.tryEmit(if (liked) "已取消喜欢" else "已喜欢")
            } else {
                _toast.tryEmit("操作失败，请确认已登录")
            }
        }
    }

    private fun downgradeAndRetry() {
        val currentLevel = _quality.value
        val idx = Api.QUALITY_LEVELS.indexOf(currentLevel)
        if (idx > 0) {
            _quality.value = Api.QUALITY_LEVELS[idx - 1]
            AppConfig.quality = _quality.value
            scope.launch { settings.setQuality(_quality.value) }
            val i = _index.value
            if (i >= 0) {
                _toast.tryEmit("播放失败，已降档重试")
                loadIndex(i)
            }
        }
    }

    private fun startTicker() {
        stopTicker()
        positionTicker = scope.launch {
            while (true) {
                _position.value = player.currentPosition.coerceAtLeast(0)
                val d = player.duration
                if (d > 0) _duration.value = d
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        positionTicker?.cancel()
        positionTicker = null
    }

    fun startService() {
        val intent = Intent(appContext, PlaybackService::class.java).apply {
            action = Intent.ACTION_PLAY
        }
        runCatching {
            appContext.startForegroundService(intent)
        }
    }
}
