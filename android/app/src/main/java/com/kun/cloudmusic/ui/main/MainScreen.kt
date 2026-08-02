package com.kun.cloudmusic.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kun.cloudmusic.data.Song
import com.kun.cloudmusic.ui.common.MiniPlayer
import com.kun.cloudmusic.ui.home.HomeScreen
import com.kun.cloudmusic.ui.search.SearchScreen
import com.kun.cloudmusic.ui.user.UserScreen

data class MainActions(
    val onOpenPlayer: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenLiked: () -> Unit = {},
    val onOpenPlaylist: (Long) -> Unit = {},
    val onOpenSong: (Long) -> Unit = {},
    val onOpenArtist: (Long) -> Unit = {},
    val onOpenAlbum: (Long) -> Unit = {},
    val onOpenMv: (Long) -> Unit = {},
    val onPlayQueue: (List<Song>, Int) -> Unit = { _, _ -> },
    val onLogout: () -> Unit = {},
)

@Composable
fun MainScreen(actions: MainActions) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(onOpen = actions.onOpenPlayer)
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("发现") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.Search, null) },
                        label = { Text("搜索") },
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("我的") },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    onOpenPlaylist = actions.onOpenPlaylist,
                    onPlayQueue = actions.onPlayQueue,
                )
                1 -> SearchScreen(actions)
                2 -> UserScreen(actions)
            }
        }
    }
}
