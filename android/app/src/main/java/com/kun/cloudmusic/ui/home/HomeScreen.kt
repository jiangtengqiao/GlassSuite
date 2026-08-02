package com.kun.cloudmusic.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kun.cloudmusic.data.Api
import com.kun.cloudmusic.data.AppConfig
import com.kun.cloudmusic.data.Playlist
import com.kun.cloudmusic.data.Song
import com.kun.cloudmusic.player.PlayerManager
import com.kun.cloudmusic.ui.common.EmptyBox
import com.kun.cloudmusic.ui.common.LoadingBox
import com.kun.cloudmusic.ui.common.PlaylistCard
import com.kun.cloudmusic.ui.common.SectionTitle
import com.kun.cloudmusic.ui.common.SongRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPlaylist: (Long) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("发现音乐") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("推荐") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("排行榜") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("歌单广场") })
            }
            when (tab) {
                0 -> RecommendTab(onOpenPlaylist, onPlayQueue)
                1 -> ToplistTab(onOpenPlaylist)
                2 -> PlaylistSquareTab(onOpenPlaylist)
            }
        }
    }
}

// ==================== 推荐 ====================

@Composable
private fun RecommendTab(
    onOpenPlaylist: (Long) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var dailySongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var recommend by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var newsongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        if (AppConfig.isLoggedIn) {
            dailySongs = runCatching { Api.service.recommendSongs().data?.dailySongs ?: emptyList() }
                .getOrDefault(emptyList())
        }
        recommend = runCatching { Api.service.recommendResource().recommend ?: emptyList() }
            .getOrDefault(emptyList())
        newsongs = runCatching { Api.service.newSongs(20).result?.mapNotNull { it.song } ?: emptyList() }
            .getOrDefault(emptyList())
        loading = false
    }

    when {
        loading -> LoadingBox()
        dailySongs.isEmpty() && recommend.isEmpty() && newsongs.isEmpty() ->
            EmptyBox("暂无数据，请检查服务器地址设置")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            if (dailySongs.isNotEmpty()) {
                item { SectionTitle(if (AppConfig.isLoggedIn) "每日推荐" else "每日推荐（需登录）") }
                items(dailySongs.take(5)) { song ->
                    SongRow(song, onClick = { onPlayQueue(dailySongs, dailySongs.indexOf(song)) })
                }
            }
            if (recommend.isNotEmpty()) {
                item { SectionTitle("推荐歌单") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(recommend) { pl ->
                            PlaylistCard(pl, onClick = { onOpenPlaylist(pl.id) }, width = 130)
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(end = 12.dp))
                        }
                    }
                }
            }
            if (newsongs.isNotEmpty()) {
                item { SectionTitle("新歌速递") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(newsongs) { song ->
                            androidx.compose.foundation.layout.Column(Modifier.padding(end = 12.dp)) {
                                com.kun.cloudmusic.ui.common.CoverImage(
                                    song.albumPic,
                                    Modifier.height(130.dp).fillMaxWidth().padding(end = 0.dp),
                                )
                                Text(
                                    song.name ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ==================== 排行榜 ====================

@Composable
private fun ToplistTab(onOpenPlaylist: (Long) -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var list by remember { mutableStateOf<List<com.kun.cloudmusic.data.ToplistItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        list = runCatching { Api.service.toplist().list ?: emptyList() }.getOrDefault(emptyList())
        loading = false
    }

    when {
        loading -> LoadingBox()
        list.isEmpty() -> EmptyBox("暂无数据")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(list) { i, item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .androidx.compose.foundation.clickable { onOpenPlaylist(item.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (i + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (i < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    com.kun.cloudmusic.ui.common.CoverImage(item.coverImgUrl, Modifier.height(52.dp).fillMaxWidth(0.2f))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(item.name ?: "", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            item.updateFrequency ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ==================== 歌单广场 ====================

@Composable
private fun PlaylistSquareTab(onOpenPlaylist: (Long) -> Unit) {
    var cats by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCat by remember { mutableStateOf("全部") }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        cats = runCatching {
            Api.service.catlist().sub?.map { it.name ?: "" }?.distinct() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    LaunchedEffect(selectedCat) {
        loading = true
        val cat = if (selectedCat == "全部") "全部" else selectedCat
        playlists = runCatching { Api.service.hotPlaylist(cat, "hot", 50, 0).playlists ?: emptyList() }
            .getOrDefault(emptyList())
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
            items(listOf("全部") + cats) { cat ->
                FilterChip(
                    selected = selectedCat == cat,
                    onClick = { selectedCat = cat },
                    label = { Text(cat) },
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp),
                )
            }
        }
        when {
            loading -> LoadingBox()
            playlists.isEmpty() -> EmptyBox("该分类暂无歌单")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(playlists.chunked(3)) { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { pl ->
                            PlaylistCard(pl, onClick = { onOpenPlaylist(pl.id) }, width = 110)
                        }
                        if (row.size < 3) {
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
