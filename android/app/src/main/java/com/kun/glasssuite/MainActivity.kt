package com.kun.glasssuite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kun.glasssuite.data.AppConfig
import com.kun.glasssuite.data.GitHubApi
import com.kun.glasssuite.data.Release
import com.kun.glasssuite.data.Settings
import com.kun.glasssuite.data.UpdateChecker
import com.kun.glasssuite.player.PlayerManager
import com.kun.glasssuite.ui.announcement.AnnouncementScreen
import com.kun.glasssuite.ui.beta.BetaScreen
import com.kun.glasssuite.ui.detail.AlbumScreen
import com.kun.glasssuite.ui.detail.ArtistScreen
import com.kun.glasssuite.ui.detail.MvScreen
import com.kun.glasssuite.ui.detail.SongDetailScreen
import com.kun.glasssuite.ui.github.GitHubSearchScreen
import com.kun.glasssuite.ui.glass.GlassHomeScreen
import com.kun.glasssuite.ui.login.LoginScreen
import com.kun.glasssuite.ui.main.MainActions
import com.kun.glasssuite.ui.main.MainScreen
import com.kun.glasssuite.ui.player.PlayerScreen
import com.kun.glasssuite.ui.playlist.LikedScreen
import com.kun.glasssuite.ui.playlist.PlaylistScreen
import com.kun.glasssuite.ui.settings.SettingsScreen
import com.kun.glasssuite.ui.theme.AppTheme
import com.kun.glasssuite.ui.update.UpdateCenterScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as App
        GitHubApi.owner = AppConfig.ghOwner
        GitHubApi.repo = AppConfig.ghRepo
        UpdateChecker.start()
        setContent {
            val settings by app.settings.data.collectAsState(initial = Settings())
            val nav = rememberNavController()
            val dark = when (AppConfig.themeMode) {
                1 -> false
                2 -> true
                else -> null
            }

            // 全局 Toast
            LaunchedEffect(Unit) {
                PlayerManager.toast.collectLatest { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            // 自动更新弹窗推送
            var updateDialog by remember { mutableStateOf<Release?>(null) }
            LaunchedEffect(Unit) {
                UpdateChecker.events.collectLatest { event ->
                    when (event) {
                        is UpdateChecker.UpdateEvent.NewVersion -> updateDialog = event.release
                        is UpdateChecker.UpdateEvent.Announcements -> {
                            Toast.makeText(
                                this@MainActivity,
                                "收到 ${event.list.size} 条新公告",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            AppTheme(accentHex = settings.accentHex, dark = dark) {
                NavHost(navController = nav, startDestination = "splash") {

                    composable("splash") {
                        LaunchedEffect(Unit) {
                            delay(400)
                            nav.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    // ===== 多功能首页（液态玻璃功能中心）=====
                    composable("home") {
                        GlassHomeScreen(
                            dark = dark == true,
                            onOpenMusic = { nav.navigate("music") },
                            onOpenGitHub = { nav.navigate("github") },
                            onOpenUpdates = { nav.navigate("updates") },
                            onOpenAnnouncements = { nav.navigate("announcements") },
                            onOpenSettings = { nav.navigate("settings") },
                        )
                    }

                    // ===== 音乐模块 =====
                    composable("music") {
                        MainScreen(
                            MainActions(
                                onLogin = { nav.navigate("login") },
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
                                        popUpTo("music") { inclusive = false }
                                    }
                                },
                            )
                        )
                    }

                    composable("login") {
                        LoginScreen(onLoggedIn = {
                            nav.popBackStack()
                        })
                    }

                    // ===== 多功能模块 =====
                    composable("github") {
                        GitHubSearchScreen(dark = dark == true, onBack = { nav.popBackStack() })
                    }

                    composable("updates") {
                        UpdateCenterScreen(dark = dark == true, onBack = { nav.popBackStack() })
                    }

                    composable("announcements") {
                        AnnouncementScreen(onBack = { nav.popBackStack() })
                    }

                    composable("beta") {
                        BetaScreen(onBack = { nav.popBackStack() })
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
                            onOpenSong = { id -> nav.navigate("song/$id") },
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
                            onPlayQueue = { songs, start ->
                                PlayerManager.playQueue(songs, start)
                                PlayerManager.startService()
                            },
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
                            onPlayQueue = { songs, start ->
                                PlayerManager.playQueue(songs, start)
                                PlayerManager.startService()
                            },
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

            // 更新弹窗
            updateDialog?.let { release ->
                UpdateDialog(release) {
                    updateDialog = null
                }
            }
        }

        // 通知权限：首次启动引导弹窗（推送更新/公告信息所需）
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun UpdateDialog(release: Release, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 ${release.tagName}") },
        text = {
            Text(
                (release.name.ifBlank { "GlassSuite 更新" }) + "\n\n" +
                    (release.body?.take(400) ?: "") +
                    "\n\n是否前往 Releases 下载？",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl.ifBlank { "https://github.com/${GitHubApi.owner}/${GitHubApi.repo}/releases" }))
                )
                onDismiss()
            }) { Text("前往下载") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("稍后") }
        },
    )
}
