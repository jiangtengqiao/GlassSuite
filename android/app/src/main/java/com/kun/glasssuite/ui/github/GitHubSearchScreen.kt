package com.kun.glasssuite.ui.github

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kun.glasssuite.data.GitHubApi
import com.kun.glasssuite.data.Repo
import com.kun.glasssuite.ui.glass.GlassCard
import kotlinx.coroutines.launch

/**
 * GitHub 检索模块：热门仓库 + 关键字搜索。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubSearchScreen(dark: Boolean, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var repos by remember { mutableStateOf<List<Repo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        repos = if (searched) {
            GitHubApi.searchRepos(query)
        } else {
            GitHubApi.trendingRepos(30)
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == 0) "GitHub 热门" else "GitHub 检索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0; searched = false; scope.launch { load() } }, text = { Text("热门") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("搜索") })
            }
            if (tab == 1) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("搜索仓库，如：kotlin compose") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    IconButton(onClick = {
                        searched = query.isNotBlank()
                        scope.launch { load() }
                    }) {
                        Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                repos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无数据（网络受限或 API 限流）", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(repos) { repo ->
                        RepoCard(repo, dark)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RepoCard(repo: Repo, dark: Boolean) {
    val context = LocalContext.current
    GlassCard(dark = dark, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repo.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) Color.White else Color(0xFF222222),
                    modifier = Modifier.weight(1f),
                )
                Text("★ ${formatCount(repo.stargazersCount)}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFF5A623))
            }
            if (!repo.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dark) Color.White.copy(alpha = 0.65f) else Color(0xFF666666),
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!repo.language.isNullOrBlank()) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .padding(0.dp)
                    ) {
                        Box(Modifier.size(10.dp).background(Color(0xFF3D7FFF), androidx.compose.foundation.shape.CircleShape))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        repo.language,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dark) Color.White.copy(alpha = 0.5f) else Color(0xFF888888),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "打开 ↗",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF3D7FFF),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        val ctx = context
        Modifier.clickable {
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl)))
            }
        }
    }
}

private fun formatCount(n: Long): String = when {
    n >= 10000 -> String.format("%.1f万", n / 10000.0)
    else -> n.toString()
}
