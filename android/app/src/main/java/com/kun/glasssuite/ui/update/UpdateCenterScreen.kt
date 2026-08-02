package com.kun.glasssuite.ui.update

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kun.glasssuite.data.Release
import com.kun.glasssuite.data.UpdateChecker
import com.kun.glasssuite.ui.glass.GlassCard
import kotlinx.coroutines.launch

/**
 * 更新中心：当前版本 / 最新版本 / 手动检查 / 更新日志（可折叠查看）/ 一键下载。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCenterScreen(dark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest by UpdateChecker.latest.collectAsState()
    val hasUpdate by UpdateChecker.hasUpdate.collectAsState()
    val checking by UpdateChecker.checking.collectAsState()
    var releases by remember { mutableStateOf<List<Release>>(emptyList()) }
    var expandedTag by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        releases = com.kun.glasssuite.data.GitHubApi.releases(10)
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更新中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { UpdateChecker.checkNow() } }) {
                        if (checking) {
                            CircularProgressIndicator(Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, "检查更新")
                        }
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // 版本状态卡
            GlassCard(dark = dark, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "当前版本：v${UpdateChecker.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) Color.White else Color(0xFF222222),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hasUpdate) Icons.Default.Download else Icons.Default.CheckCircle,
                            null,
                            tint = if (hasUpdate) Color(0xFFFF8000) else Color(0xFF00B578),
                            modifier = Modifier.width(18.dp).height(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                checking -> "正在检查更新…"
                                hasUpdate -> "发现新版本：${latest?.tagName}"
                                latest != null -> "已是最新版本（${latest?.tagName}）"
                                else -> "尚未检查（保持联网后自动检查）"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dark) Color.White.copy(alpha = 0.7f) else Color(0xFF555555),
                        )
                    }
                    if (hasUpdate && latest != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latest!!.htmlUrl)))
                            }
                        }) {
                            Icon(Icons.Default.Download, null, Modifier.width(18.dp).height(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("前往下载最新版本")
                        }
                    }
                }
            }

            Text(
                "更新日志（点击展开）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color.White else Color(0xFF222222),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (!loaded) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (releases.isEmpty()) {
                Text(
                    "暂无发布记录（请检查网络）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                releases.forEach { release ->
                    val expanded = expandedTag == release.tagName
                    GlassCard(dark = dark, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    expandedTag = if (expanded) null else release.tagName
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        release.tagName + if (release.prerelease) "（预发布）" else "",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dark) Color.White else Color(0xFF222222),
                                    )
                                    Text(
                                        "发布于 ${release.publishedAt.take(10)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (dark) Color.White.copy(alpha = 0.4f) else Color(0xFF999999),
                                    )
                                }
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    null,
                                    tint = if (dark) Color.White.copy(alpha = 0.5f) else Color(0xFF999999),
                                )
                            }
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                Column {
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    Text(
                                        release.body.ifBlank { "（无详细说明）" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (dark) Color.White.copy(alpha = 0.7f) else Color(0xFF555555),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (release.assets.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "附件：" + release.assets.joinToString("、") { it.name },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF3D7FFF),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
