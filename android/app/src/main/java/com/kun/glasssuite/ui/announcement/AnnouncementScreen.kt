package com.kun.glasssuite.ui.announcement

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kun.glasssuite.data.Announcement
import com.kun.glasssuite.data.UpdateChecker
import com.kun.glasssuite.ui.glass.GlassCard
import kotlinx.coroutines.launch

/** 首页公告横幅：显示最新一条公告/推广，点击进入公告页 */
@Composable
fun AnnouncementBanner(dark: Boolean, onClick: () -> Unit = {}) {
    val announcements by UpdateChecker.announcements.collectAsState()
    val newCount by UpdateChecker.newAnnouncements.collectAsState()
    val list = announcements.ifEmpty { UpdateChecker.builtinAnnouncements() }
    val latest = list.firstOrNull() ?: return

    GlassCard(dark = dark, modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 4.dp)
        .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Campaign,
                null,
                tint = if (dark) Color(0xFFFFD27F) else Color(0xFFE8890C),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (latest.isPromo) "推广 · ${latest.title}" else "公告 · ${latest.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) Color.White else Color(0xFF333333),
                        maxLines = 1,
                    )
                    if (newCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "NEW",
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                .background(Color(0xFFEC4141))
                                .padding(0.dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    latest.content.replace('\n', ' ').take(60),
                    fontSize = 11.sp,
                    color = if (dark) Color.White.copy(alpha = 0.55f) else Color(0xFF777777),
                    maxLines = 1,
                )
            }
            Text("›", fontSize = 20.sp, color = if (dark) Color.White.copy(alpha = 0.4f) else Color(0xFFBBBBBB))
        }
    }
}

/** 公告中心：在线公告 + 内置兜底公告，推广条目带跳转 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val announcements by UpdateChecker.announcements.collectAsState()
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (announcements.isEmpty()) {
            refreshing = true
            UpdateChecker.checkNow()
            refreshing = false
        }
        UpdateChecker.markAnnouncementsSeen()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("公告与推送") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        }
    ) { padding ->
        val list = announcements.ifEmpty { UpdateChecker.builtinAnnouncements() }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list) { item ->
                AnnouncementCard(item) {
                    if (item.url.isNotEmpty()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "公告实时从 GitHub 拉取（${com.kun.glasssuite.data.GitHubApi.owner}/${com.kun.glasssuite.data.GitHubApi.repo}/ANNOUNCEMENTS.md），离线时展示内置公告。",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(item: Announcement, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (item.isPromo) "推广" else "公告",
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .background(if (item.isPromo) Color(0xFFFF8000) else Color(0xFF3D7FFF)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (item.date.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(item.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )
            if (item.url.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "查看详情 ›",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onClick)
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
