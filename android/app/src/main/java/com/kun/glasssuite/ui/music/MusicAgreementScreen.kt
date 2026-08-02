package com.kun.glasssuite.ui.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 音乐功能专属协议与说明（自拟，独立于全局协议，置于音乐模块内展示）。
 * 与全局《用户协议》《免责声明》等共同适用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicAgreementScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音乐服务说明与协议") },
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
                .padding(20.dp),
        ) {
            Text("《音乐功能专属服务协议》", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("版本 V1.0 · 生效日期 2026 年 8 月", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            section("一、访客模式")
            para("您无需登录即可使用本音乐功能的检索、试听与基础播放能力（受版权与接口限制的曲目除外）。访客模式不收集、不上传您的任何个人信息。")
            section("二、登录与账号")
            para("登录用于同步您的歌单、我喜欢与收藏，登录凭证仅保存在您的设备本地。登录功能对接第三方音乐平台账号体系，相关平台规则以其官方说明为准。")
            section("三、播放与音质")
            para("播放音源由第三方音乐平台接口提供，音质档位（标准/较高/极高/无损）以实际授权为准；VIP 或版权受限曲目可能无法播放，或自动降档，请以实际结果为准。")
            section("四、内容来源与版权")
            para("本功能展示的词曲、录音、歌词、封面、MV 等内容的版权归相应权利人所有。您仅可在线试听，不得下载、录制、转存、传播或用于商业用途。")
            section("五、免责声明")
            para("因第三方接口调整、网络故障或版权变更导致的无法播放、内容缺失，本软件不承担违约责任，但会尽力通过更新改善。详细免责事项见全局《免责声明》。")
            section("六、联系我们")
            para("联系邮箱：jiangtengqiao@qq.com（无官网）。")
            section("七、其他")
            para("本协议为全局《用户协议》《隐私政策》的补充，未尽事宜适用全局协议。")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun section(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
}

@Composable
private fun para(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
}
