package com.kun.cloudmusic.ui.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.kun.cloudmusic.App
import com.kun.cloudmusic.data.Api
import com.kun.cloudmusic.data.AppConfig
import com.kun.cloudmusic.data.Settings
import com.kun.cloudmusic.ui.theme.AccentPresets
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
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()
    val settings by app.settings.data.collectAsState(initial = Settings())
    var apiUrl by remember { mutableStateOf(AppConfig.apiBaseUrl) }
    var diyText by remember { mutableStateOf(AppConfig.diyLyric) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    var openDoc by remember { mutableStateOf<String?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 服务器地址
            SectionTitle("服务器")
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("API 服务器地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    val url = apiUrl.trim().trimEnd('/')
                    AppConfig.apiBaseUrl = url
                    Api.rebuild()
                    scope.launch { app.settings.updateApiBase(url) }
                    savedMsg = "服务器地址已保存：$url"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存并应用") }
            savedMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            // 主题色
            SectionTitle("主题定制")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentPresets.forEach { (name, argb) ->
                    val hex = "#%06X".format(argb and 0xFFFFFF)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .clickable {
                                    AppConfig.accentHex = hex
                                    scope.launch { app.settings.setAccent(hex) }
                                    savedMsg = "主题色已切换：$name"
                                }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("深色模式", Modifier.weight(1f))
                Switch(
                    checked = settings.darkMode,
                    onCheckedChange = {
                        AppConfig.darkMode = it
                        scope.launch { app.settings.setDarkMode(it) }
                    },
                )
            }

            // 歌词设置
            SectionTitle("歌词设置")
            Text("歌词字号：${settings.lyricFontSize}sp", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.lyricFontSize.toFloat(),
                onValueChange = {
                    AppConfig.lyricFontSize = it.toInt()
                    scope.launch { app.settings.setLyricFontSize(it.toInt()) }
                },
                valueRange = 12f..32f,
                steps = 19,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("歌词时间偏移：${settings.lyricOffsetMs / 1000.0}s", Modifier.weight(1f))
                TextButton(onClick = {
                    AppConfig.lyricOffsetMs = 0
                    scope.launch { app.settings.setLyricOffset(0) }
                }) { Text("复位") }
            }
            Text("歌词模式", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf("原词", "翻译", "罗马音", "DIY")
                modes.forEachIndexed { i, name ->
                    FilterChip(
                        selected = settings.lyricMode == i,
                        onClick = {
                            AppConfig.lyricMode = i
                            scope.launch { app.settings.setLyricMode(i) }
                        },
                        label = { Text(name) },
                    )
                }
            }
            OutlinedTextField(
                value = diyText,
                onValueChange = { diyText = it },
                label = { Text("DIY 歌词（LRC 格式，粘贴后保存）") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    AppConfig.diyLyric = diyText
                    scope.launch { app.settings.setDiyLyric(diyText) }
                    savedMsg = "DIY 歌词已保存"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存 DIY 歌词") }

            // 关于与法律
            SectionTitle("关于与法律")
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
            Text(
                "乐云音乐 CloudMusic v1.0.0\n仅供学习交流使用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
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
        modifier = Modifier.padding(top = 8.dp),
    )
}
