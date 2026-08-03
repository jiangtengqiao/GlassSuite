package com.kun.glasssuite.ui.glass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kun.glasssuite.data.UpdateChecker
import com.kun.glasssuite.ui.announcement.AnnouncementBanner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Aura 多功能首页：液态玻璃功能中心。
 * 音乐仅为模块之一；另有 GitHub 检索、更新中心、公告推送、设置。
 */
@Composable
fun GlassHomeScreen(
    dark: Boolean,
    onOpenMusic: () -> Unit,
    onOpenGitHub: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val hasUpdate by UpdateChecker.hasUpdate.collectAsState()
    val newAnn by UpdateChecker.newAnnouncements.collectAsState()

    GlassBackground(dark = dark) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            GlassHeader(
                title = "Aura",
                subtitle = "GlassSuite · 多功能应用套件",
                trailing = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "v${UpdateChecker.VERSION_NAME}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (dark) Color.White.copy(alpha = 0.5f) else Color(0xFF888888),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            "BETA 尝鲜版",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                .background(Color(0xFFE8890C)),
                        )
                    }
                },
            )

            // 公告横幅（推广/公告推送）
            AnnouncementBanner(dark = dark)

            Column(Modifier.padding(horizontal = 20.dp), content = {
                Spacer(Modifier.height(8.dp))
                GlassModuleCard(
                    title = "音乐",
                    subtitle = "网易云音乐 · 登录/播放/歌词/歌单/锁屏",
                    icon = Icons.Default.MusicNote,
                    iconColor = Color(0xFFEC4141),
                    dark = dark,
                    onClick = onOpenMusic,
                )
                Spacer(Modifier.height(14.dp))
                GlassModuleCard(
                    title = "GitHub 检索",
                    subtitle = "搜索仓库 · 热门趋势 · 直接访问",
                    icon = Icons.Default.Code,
                    iconColor = Color(0xFF3D7FFF),
                    dark = dark,
                    onClick = onOpenGitHub,
                )
                Spacer(Modifier.height(14.dp))
                GlassModuleCard(
                    title = "更新中心",
                    subtitle = "自查更新 · 折叠日志 · 一键下载",
                    icon = Icons.Default.SystemUpdateAlt,
                    iconColor = Color(0xFF00B578),
                    badge = if (hasUpdate) "有新版本" else null,
                    dark = dark,
                    onClick = onOpenUpdates,
                )
                Spacer(Modifier.height(14.dp))
                GlassModuleCard(
                    title = "公告与推送",
                    subtitle = "公告文件 · 推广信息 · 自动推送",
                    icon = Icons.Default.Campaign,
                    iconColor = Color(0xFFFF8000),
                    badge = if (newAnn > 0) "$newAnn 条新公告" else null,
                    dark = dark,
                    onClick = onOpenAnnouncements,
                )
                Spacer(Modifier.height(14.dp))
                GlassModuleCard(
                    title = "设置",
                    subtitle = "外观主题 · 服务器 · GitHub 仓库",
                    icon = Icons.Default.Settings,
                    iconColor = Color(0xFF7B5CFF),
                    dark = dark,
                    onClick = onOpenSettings,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    "© 2026 GlassSuite · 多功能应用套件",
                    fontSize = 11.sp,
                    color = if (dark) Color.White.copy(alpha = 0.35f) else Color(0xFFAAAAAA),
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            })
        }
    }
}
