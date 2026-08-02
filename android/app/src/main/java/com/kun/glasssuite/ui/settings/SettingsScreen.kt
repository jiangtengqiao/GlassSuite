package com.kun.glasssuite.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kun.glasssuite.App
import com.kun.glasssuite.data.Api
import com.kun.glasssuite.data.AppConfig
import com.kun.glasssuite.data.BetaStore
import com.kun.glasssuite.data.Settings
import com.kun.glasssuite.data.UpdateChecker
import com.kun.glasssuite.ui.theme.AccentPresets
import kotlinx.coroutines.launch

private val LEGAL_DOCS = listOf(
    "用户协议" to "01-用户协议.txt",
    "隐私政策" to "02-隐私政策.txt",
    "儿童个人信息保护规则" to "03-儿童个人信息保护规则.txt",
    "版权声明与侵权投诉" to "04-版权声明与侵权投诉.txt",
    "免责声明" to "05-免责声明.txt",
    "第三方SDK与权限清单" to "06-第三方SDK与权限清单.txt",
    "账号注销指引" to "07-账号注销指引.txt",
    "投诉与举报" to "08-投诉与举报.txt",
    "用户行为规范" to "09-用户行为规范.txt",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenBeta: () -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()
    val settings by app.settings.data.collectAsState(initial = Settings())
    var apiUrl by remember { mutableStateOf(AppConfig.apiBaseUrl) }
    var betaUrl by remember { mutableStateOf(BetaStore.betaServerUrl) }
    var connState by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    var openDoc by remember { mutableStateOf<String?>(null) }

    // 自动探测默认服务器地址（模拟器/真机）
    LaunchedEffect(Unit) {
        if (AppConfig.apiBaseUrl == AppConfig.DEFAULT_API) {
            val emu = "http://10.0.2.2:3000"
            val local = "http://127.0.0.1:3000"
            apiUrl = runCatching {
                val c = java.net.Socket()
                c.connect(java.net.InetSocketAddress("10.0.2.2", 3000), 800)
                c.close()
                emu
            }.getOrElse { local }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ===== 外观 =====
            SectionTitle("外观")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("跟随系统" to 0, "浅色" to 1, "深色" to 2).forEach { (name, m) ->
                    FilterChip(
                        selected = settings.themeMode == m,
                        onClick = {
                            AppConfig.themeMode = m
                            scope.launch { app.settings.setThemeMode(m) }
                        },
                        label = { Text(name) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentPresets.forEach { (name, argb) ->
                    val hex = "#%06X".format(argb and 0xFFFFFF)
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .clickable {
                                AppConfig.accentHex = hex
                                scope.launch { app.settings.setAccent(hex) }
                            }
                    )
                }
            }

            // ===== 服务器连接 =====
            SectionTitle("服务器连接")
            Text(
                "音乐数据由你部署的服务器提供。部署方式见仓库 README；默认地址适用于模拟器。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("服务器地址") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = {
                    val url = apiUrl.trim().trimEnd('/')
                    AppConfig.apiBaseUrl = url
                    Api.rebuild()
                    scope.launch {
                        app.settings.updateApiBase(url)
                        savedMsg = "已保存，测试连接…"
                        connState = runCatching {
                            val resp = retrofit2.Retrofit.Builder()
                                .baseUrl(if (url.endsWith("/")) url else "$url/")
                                .build()
                                .create(com.kun.glasssuite.data.ApiService::class.java)
                                .toplist()
                            if (resp.code == 200) "✅ 连接成功" else "⚠️ 服务器返回异常"
                        }.getOrElse { "❌ 无法连接（请确认服务器已启动）" }
                    }
                }) { Text("测试并保存") }
            }
            if (connState.isNotEmpty()) {
                Text(connState, color = if (connState.startsWith("✅")) Color(0xFF00B578) else Color(0xFFE84026))
            }
            savedMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

            // ===== 开发者尝鲜 =====
            SectionTitle("开发者尝鲜")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenBeta() }
                    .padding(vertical = 12.dp, horizontal = 10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("开发者尝鲜模式", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (BetaStore.betaAccess) "已激活 · 可接收 Beta 版推送" else "申请尝鲜码，提前体验 Beta 版本",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = betaUrl,
                    onValueChange = { betaUrl = it },
                    label = { Text("尝鲜服务器地址") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = {
                    BetaStore.betaServerUrl = betaUrl.trim()
                    scope.launch { app.settings.setBetaServer(betaUrl.trim()) }
                    savedMsg = "尝鲜服务器已保存"
                }) { Text("保存") }
            }

            // ===== 关于与法律 =====
            SectionTitle("关于与法律")
            Text(
                "版本 v${UpdateChecker.VERSION_NAME} · 联系邮箱：jiangtengqiao@qq.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LEGAL_DOCS.forEach { (title, _) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { openDoc = title }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    openDoc?.let { title ->
        val file = LEGAL_DOCS.firstOrNull { it.first == title }?.second ?: return
        LegalScreen(title = title, file = file, onClose = { openDoc = null })
    }
}

@Composable
fun LegalScreen(title: String, file: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("加载中…") }
    LaunchedEffect(file) {
        content = runCatching {
            context.assets.open("legal/$file").bufferedReader().use { it.readText() }
        }.getOrDefault("文档加载失败")
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(content, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("关闭") }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp),
    )
}
