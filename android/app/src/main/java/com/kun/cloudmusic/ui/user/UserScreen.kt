package com.kun.cloudmusic.ui.user

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kun.cloudmusic.App
import com.kun.cloudmusic.data.Api
import com.kun.cloudmusic.data.AppConfig
import com.kun.cloudmusic.data.Playlist
import com.kun.cloudmusic.ui.common.CoverImage
import com.kun.cloudmusic.ui.common.LoadingBox
import com.kun.cloudmusic.ui.main.MainActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(actions: MainActions) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()
    val profile by AppConfig.profile.collectAsState()
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(profile?.userId) {
        loading = true
        val uid = AppConfig.userId
        if (uid > 0L) {
            runCatching { Api.service.userPlaylist(uid) }.getOrNull()?.playlist?.let {
                playlists = it
            }
        }
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("我的") }) }) { padding ->
        if (loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    ProfileHeader(profile)
                }
                item {
                    Row(Modifier.padding(16.dp)) {
                        MenuItem(Icons.Default.Favorite, "我喜欢") { actions.onOpenLiked() }
                        Spacer(Modifier.width(12.dp))
                        MenuItem(Icons.Default.Settings, "设置") { actions.onOpenSettings() }
                    }
                }
                item {
                    Text(
                        "我的歌单",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (playlists.isEmpty()) {
                    item { Text("暂无歌单", Modifier.padding(16.dp)) }
                } else {
                    items(playlists) { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { actions.onOpenPlaylist(p.id) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverImage(p.coverImgUrl, Modifier.size(48.dp), corner = 6)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(p.name ?: "", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${p.trackCount ?: 0} 首",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                app.settings.logout()
                                Api.cookieJar.clear()
                                AppConfig.userId = 0L
                                AppConfig.profileJson = ""
                                AppConfig.setProfile(null)
                                actions.onLogout()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("退出登录")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: com.kun.cloudmusic.data.Profile?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(profile?.avatarUrl, Modifier.size(64.dp), corner = 32)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                profile?.nickname ?: "未登录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (!profile?.signature.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    profile?.signature ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
