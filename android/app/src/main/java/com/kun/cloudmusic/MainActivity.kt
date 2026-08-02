package com.kun.cloudmusic

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kun.cloudmusic.data.AppConfig
import com.kun.cloudmusic.data.Settings
import com.kun.cloudmusic.player.PlayerManager
import com.kun.cloudmusic.ui.detail.AlbumScreen
import com.kun.cloudmusic.ui.detail.ArtistScreen
import com.kun.cloudmusic.ui.detail.MvScreen
import com.kun.cloudmusic.ui.detail.SongDetailScreen
import com.kun.cloudmusic.ui.login.LoginScreen
import com.kun.cloudmusic.ui.main.MainActions
import com.kun.cloudmusic.ui.main.MainScreen
import com.kun.cloudmusic.ui.player.PlayerScreen
import com.kun.cloudmusic.ui.playlist.LikedScreen
import com.kun.cloudmusic.ui.playlist.PlaylistScreen
import com.kun.cloudmusic.ui.settings.SettingsScreen
import com.kun.cloudmusic.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            val settings by app.settings.data.collectAsState(initial = Settings())
            val nav = rememberNavController()

            // 全局 Toast（来自播放器/页面）
            LaunchedEffect(Unit) {
                PlayerManager.toast.collectLatest { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            AppTheme(accentHex = settings.accentHex, dark = settings.darkMode) {
                NavHost(navController = nav, startDestination = "splash") {

                    composable("splash") {
                        LaunchedEffect(Unit) {
                            delay(400)
                            nav.navigate(if (AppConfig.isLoggedIn) "main" else "login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    composable("login") {
                        LoginScreen(onLoggedIn = {
                            nav.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }

                    composable("main") {
                        MainScreen(
                            MainActions(
                                onOpenPlayer = { nav.navigate("player") },
                                onOpenSettings = { nav.navigate("settings") },
                                onOpenLiked = { nav.navigate("liked") },
                                onOpenPlaylist = { id -> nav.navigate("playlist/$id") },
                                onOpenSong = { id -> nav.navigate("song/$id") },
                                onOpenArtist = { id -> nav.navigate("artist/$id") },
                                onOpenAlbum = { id -> nav.navigate("album/$id") },
                                onOpenMv = { id -> nav.navigate("mv/$id") },
                                onPlayQueue = { songs, start ->
                                    PlayerManager.playQueue(songs, start)
                                    PlayerManager.startService()
                                },
                                onLogout = {
                                    nav.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                            )
                        )
                    }

                    composable("player") {
                        PlayerScreen(onBack = { nav.popBackStack() })
                    }

                    composable(
                        "playlist/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) {
                        PlaylistScreen(
                            playlistId = it.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                            onPlayQueue = { songs, start ->
                                PlayerManager.playQueue(songs, start)
                                PlayerManager.startService()
                            },
                        )
                    }

                    composable(
                        "song/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) {
                        SongDetailScreen(
                            songId = it.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                            onOpenArtist = { id -> nav.navigate("artist/$id") },
                            onOpenAlbum = { id -> nav.navigate("album/$id") },
                            onOpenMv = { id -> nav.navigate("mv/$id") },
                        )
                    }

                    composable(
                        "artist/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) {
                        ArtistScreen(
                            artistId = it.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                            onOpenAlbum = { id -> nav.navigate("album/$id") },
                            onOpenSong = { id -> nav.navigate("song/$id") },
                        )
                    }

                    composable(
                        "album/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) {
                        AlbumScreen(
                            albumId = it.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                            onPlayQueue = { songs, start ->
                                PlayerManager.playQueue(songs, start)
                                PlayerManager.startService()
                            },
                        )
                    }

                    composable(
                        "mv/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) {
                        MvScreen(
                            mvId = it.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                        )
                    }

                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }

                    composable("liked") {
                        LikedScreen(
                            onBack = { nav.popBackStack() },
                            onPlayQueue = { songs, start ->
                                PlayerManager.playQueue(songs, start)
                                PlayerManager.startService()
                            },
                        )
                    }
                }
            }
        }
    }
}
