package com.kun.cloudmusic.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kun.cloudmusic.data.Api
import com.kun.cloudmusic.data.Album
import com.kun.cloudmusic.data.Artist
import com.kun.cloudmusic.data.Mv
import com.kun.cloudmusic.data.Playlist
import com.kun.cloudmusic.data.Song
import com.kun.cloudmusic.player.PlayerManager
import com.kun.cloudmusic.ui.common.CoverImage
import com.kun.cloudmusic.ui.common.EmptyBox
import com.kun.cloudmusic.ui.common.LoadingBox
import com.kun.cloudmusic.ui.common.SongRow
import com.kun.cloudmusic.ui.main.MainActions
import com.kun.cloudmusic.util.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TYPE_SONG = 1
private const val TYPE_PLAYLIST = 1000
private const val TYPE_ARTIST = 100
private const val TYPE_ALBUM = 10
private const val TYPE_MV = 1004

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(actions: MainActions) {
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var hotWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggest by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { Api.service.hotSearch() }.getOrNull()?.result?.hots?.let {
            hotWords = it.mapNotNull { w -> w.first }
        }
    }
    LaunchedEffect(query) {
        if (query.isNotBlank() && !submitted) {
            delay(400)
            runCatching { Api.service.suggest(query) }.getOrNull()?.result?.songs?.let {
                suggest = it.take(8)
            }
        } else {
            suggest = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        submitted = false
                    },
                    placeholder = { Text("搜索歌曲 / 歌单 / 歌手 / 专辑 / MV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { submitted = true }) {
                            Icon(Icons.AutoMirrored.Filled.Send, "搜索")
                        }
                    },
                )
            })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                submitted -> SearchResultContent(query, actions)
                query.isNotBlank() && suggest.isNotEmpty() -> SuggestContent(suggest) { s ->
                    query = s.name.orEmpty()
                    submitted = true
                }
                else -> HotContent(hotWords) { w ->
                    query = w
                    submitted = true
                }
            }
        }
    }
}

@Composable
private fun HotContent(hotWords: List<String>, onPick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("热门搜索", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }
        items(hotWords) { w ->
            Text(
                w,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(w) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SuggestContent(suggest: List<Song>, onPick: (Song) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(suggest) { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(s) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔍", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(10.dp))
                Text(s.name ?: "", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SearchResultContent(query: String, actions: MainActions) {
    var type by remember { mutableIntStateOf(TYPE_SONG) }
    var loading by remember { mutableStateOf(true) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var mvs by remember { mutableStateOf<List<Mv>>(emptyList()) }

    LaunchedEffect(query, type) {
        loading = true
        val resp = runCatching { Api.service.search(query, type) }.getOrNull()?.result
        songs = resp?.songs ?: emptyList()
        playlists = resp?.playlists ?: emptyList()
        artists = resp?.artists ?: emptyList()
        albums = resp?.albums ?: emptyList()
        mvs = resp?.mvs ?: emptyList()
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = when (type) {
            TYPE_SONG -> 0
            TYPE_PLAYLIST -> 1
            TYPE_ARTIST -> 2
            TYPE_ALBUM -> 3
            else -> 4
        }) {
            Tab(selected = type == TYPE_SONG, onClick = { type = TYPE_SONG }, text = { Text("单曲") })
            Tab(selected = type == TYPE_PLAYLIST, onClick = { type = TYPE_PLAYLIST }, text = { Text("歌单") })
            Tab(selected = type == TYPE_ARTIST, onClick = { type = TYPE_ARTIST }, text = { Text("歌手") })
            Tab(selected = type == TYPE_ALBUM, onClick = { type = TYPE_ALBUM }, text = { Text("专辑") })
            Tab(selected = type == TYPE_MV, onClick = { type = TYPE_MV }, text = { Text("MV") })
        }
        when {
            loading -> LoadingBox()
            type == TYPE_SONG -> LazyColumn {
                items(songs) { s ->
                    SongRow(s) {
                        PlayerManager.playQueue(songs, songs.indexOf(s))
                        PlayerManager.startService()
                        actions.onOpenPlayer()
                    }
                }
            }
            type == TYPE_PLAYLIST -> LazyColumn {
                items(playlists) { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { actions.onOpenPlaylist(p.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(p.coverImgUrl, Modifier.width(56.dp).height(56.dp), corner = 6)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(p.name ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                "${Utils.formatPlayCount(p.playCount)} 播放",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            type == TYPE_ARTIST -> LazyColumn {
                items(artists) { a ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { actions.onOpenArtist(a.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(a.picUrl, Modifier.width(48.dp).height(48.dp), corner = 24)
                        Spacer(Modifier.width(12.dp))
                        Text(a.name ?: "", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            type == TYPE_ALBUM -> LazyColumn {
                items(albums) { a ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { actions.onOpenAlbum(a.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(a.picUrl, Modifier.width(56.dp).height(56.dp), corner = 6)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(a.name ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                a.artist?.name ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            else -> LazyColumn {
                items(mvs) { mv ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { actions.onOpenMv(mv.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverImage(mv.cover, Modifier.width(64.dp).height(40.dp), corner = 4)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(mv.name ?: "", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                mv.artistName ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        if (!loading && (songs.isEmpty() && playlists.isEmpty() && artists.isEmpty() && albums.isEmpty() && mvs.isEmpty())) {
            EmptyBox("未找到相关结果")
        }
    }
}
