package com.kun.glasssuite.ui.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kun.glasssuite.data.Api
import com.kun.glasssuite.data.Playlist
import com.kun.glasssuite.data.Song
import com.kun.glasssuite.player.PlayerManager
import com.kun.glasssuite.ui.common.CoverImage
import com.kun.glasssuite.ui.common.LoadingBox
import com.kun.glasssuite.ui.common.SongRow
import com.kun.glasssuite.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onOpenSong: (Long) -> Unit,
) {
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var subscribed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playlistId) {
        loading = true
        val detail = runCatching { Api.service.playlistDetail(playlistId) }.getOrNull()?.playlist
        playlist = detail
        subscribed = detail?.subscribed == true
        val tracks = runCatching { Api.service.playlistTracks(playlistId) }.getOrNull()?.songs
        if (!tracks.isNullOrEmpty()) songs = tracks
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(playlist?.name ?: "歌单") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
        )
    }) { padding ->
        if (loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    PlaylistHeader(
                        playlist,
                        subscribed,
                        onToggleSubscribe = {
                            val t = if (subscribed) 0 else 1
                            scope.launch {
                                val resp = runCatching { Api.service.playlistSubscribe(playlistId, t) }.getOrNull()
                                if (resp?.code == 200) subscribed = !subscribed
                            }
                        },
                    )
                }
                if (songs.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = { onPlayQueue(songs, 0) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("播放全部（${songs.size} 首）")
                        }
                    }
                }
                items(songs) { song ->
                    SongRow(song = song, onClick = {
                        onPlayQueue(songs, songs.indexOf(song).coerceAtLeast(0))
                    })
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist?,
    subscribed: Boolean,
    onToggleSubscribe: () -> Unit,
) {
    if (playlist == null) return
    Column {
        Row(Modifier.padding(16.dp)) {
            CoverImage(playlist.coverImgUrl, Modifier.size(120.dp), corner = 8)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${playlist.creator?.nickname ?: "未知"} · ${Utils.formatPlayCount(playlist.playCount)} 播放 · ${playlist.trackCount ?: 0} 首",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onToggleSubscribe) {
                    Icon(
                        if (subscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (subscribed) "已收藏" else "收藏歌单")
                }
            }
        }
        if (!playlist.description.isNullOrBlank()) {
            Text(
                playlist.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

// ==================== 我喜欢 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedScreen(
    onBack: () -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val uid = com.kun.glasssuite.data.AppConfig.userId
        if (uid > 0L) {
            val ids = runCatching { Api.service.likeList(uid) }.getOrNull()?.ids ?: emptyList()
            if (ids.isNotEmpty()) {
                val chunks = ids.chunked(500)
                val list = mutableListOf<Song>()
                chunks.forEach { chunk ->
                    val resp = runCatching {
                        Api.service.songDetail(chunk.joinToString(","))
                    }.getOrNull()
                    resp?.songs?.let { list.addAll(it) }
                }
                songs = list
            }
        }
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("我喜欢") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
        )
    }) { padding ->
        if (loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
        } else if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("还没有喜欢的歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(songs) { song ->
                    SongRow(song = song, onClick = {
                        onPlayQueue(songs, songs.indexOf(song).coerceAtLeast(0))
                    })
                }
            }
        }
    }
}
