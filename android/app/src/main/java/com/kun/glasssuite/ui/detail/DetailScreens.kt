package com.kun.glasssuite.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import com.kun.glasssuite.data.Api
import com.kun.glasssuite.data.Album
import com.kun.glasssuite.data.Artist
import com.kun.glasssuite.data.Song
import com.kun.glasssuite.player.PlayerManager
import com.kun.glasssuite.ui.common.CoverImage
import com.kun.glasssuite.ui.common.EmptyBox
import com.kun.glasssuite.ui.common.LoadingBox
import com.kun.glasssuite.ui.common.SectionTitle
import com.kun.glasssuite.ui.common.SongRow

// ==================== 歌曲详情 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    songId: Long,
    onBack: () -> Unit,
    onOpenArtist: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenMv: (Long) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    var song by remember { mutableStateOf<Song?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(songId) {
        loading = true
        song = runCatching { Api.service.songDetail(songId.toString()) }
            .getOrNull()?.songs?.firstOrNull()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(song?.name ?: "歌曲详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        }
    ) { padding ->
        val s = song
        if (loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
        } else if (s == null) {
            EmptyBox("歌曲不存在或加载失败", Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(s.albumPic, Modifier.size(140.dp), corner = 12)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.name ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                s.artistNames,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { onPlayQueue(listOf(s), 0) }) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text("播放")
                            }
                        }
                    }
                }
                if (s.mvId > 0L) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMv(s.mvId) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text("▶ 查看 MV", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    SectionTitle("歌手")
                    s.artists?.forEach { artist ->
                        ArtistRow(artist) { onOpenArtist(artist.id) }
                    }
                }
                item {
                    SectionTitle("专辑")
                    s.album?.let { album ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAlbum(album.id) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverImage(album.picUrl, Modifier.size(48.dp), corner = 6)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(album.name ?: "", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "专辑 · ${album.size ?: ""} 首",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(artist.picUrl, Modifier.size(48.dp), corner = 24)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(artist.name ?: "", style = MaterialTheme.typography.bodyLarge)
            Text(
                "歌手",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ==================== 歌手页 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistId: Long,
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    var artist by remember { mutableStateOf<Artist?>(null) }
    var hotSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(artistId) {
        loading = true
        val a = runCatching { Api.service.artist(artistId) }.getOrNull()
        artist = a?.artist
        hotSongs = a?.hotSongs ?: emptyList()
        albums = runCatching { Api.service.artistAlbums(artistId) }.getOrNull()?.hotAlbums ?: emptyList()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artist?.name ?: "歌手") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        }
    ) { padding ->
        if (loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(artist?.picUrl, Modifier.size(100.dp), corner = 50)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                artist?.name ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "专辑 ${artist?.albumSize ?: 0} · MV ${artist?.mvSize ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    SectionTitle("热门歌曲")
                    Button(
                        onClick = { onPlayQueue(hotSongs, 0) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("播放全部")
                    }
                }
                itemsIndexed(hotSongs) { i, song ->
                    SongRow(song = song, index = i, onClick = { onPlayQueue(hotSongs, i) })
                }
                item { SectionTitle("专辑") }
                items(albums) { album ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAlbum(album.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(album.picUrl, Modifier.size(48.dp), corner = 6)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(album.name ?: "", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${album.size ?: 0} 首",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 专辑页 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumId: Long,
    onBack: () -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    var album by remember { mutableStateOf<Album?>(null) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(albumId) {
        loading = true
        val resp = runCatching { Api.service.album(albumId) }.getOrNull()
        album = resp?.album
        songs = resp?.songs ?: emptyList()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(album?.name ?: "专辑") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        }
    ) { padding ->
        if (loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(album?.picUrl, Modifier.size(110.dp), corner = 10)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                album?.name ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                album?.artist?.name ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { onPlayQueue(songs, 0) }) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text("播放全部（${songs.size} 首）")
                            }
                        }
                    }
                }
                itemsIndexed(songs) { i, song ->
                    SongRow(song = song, index = i, onClick = { onPlayQueue(songs, i) })
                }
            }
        }
    }
}

// ==================== MV 播放 ====================

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MvScreen(
    mvId: Long,
    onBack: () -> Unit,
) {
    var mvUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val mvContext = androidx.compose.ui.platform.LocalContext.current
    val player = remember(mvContext) {
        ExoPlayer.Builder(mvContext)
            .build()
    }

    LaunchedEffect(mvId) {
        loading = true
        mvUrl = runCatching { Api.service.mvUrl(mvId) }
            .getOrNull()?.data?.url
        loading = false
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MV") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                LoadingBox(Modifier.fillMaxSize())
            } else if (mvUrl == null) {
                EmptyBox("MV 不存在或无法播放", Modifier.fillMaxSize())
            } else {
                val url = mvUrl!!
                LaunchedEffect(url) {
                    player.setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
                    player.prepare()
                    player.playWhenReady = true
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                )
            }
        }
    }
}

