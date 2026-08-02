package com.kun.glasssuite.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 液态玻璃设计体系组件：
 * 半透明层叠 + 高光边框 + 柔和阴影 + 渐变氛围背景。
 */

@Composable
fun GlassBackground(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val brush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF14141A),
                Color(0xFF1E1E2E),
                Color(0xFF16162A),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFEAF1FB),
                Color(0xFFF7F3FF),
                Color(0xFFE8F4FA),
            )
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(brush)
    ) {
        // 氛围光斑
        Box(
            Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(if (dark) Color(0x33EC4141) else Color(0x1FEC4141))
                .align(Alignment.TopEnd)
                .padding(0.dp)
        )
        Box(
            Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(if (dark) Color(0x2E3D7FFF) else Color(0x1E3D7FFF))
                .align(Alignment.BottomStart)
        )
        Box(
            Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(if (dark) Color(0x2E00B578) else Color(0x1C00B578))
                .align(Alignment.CenterEnd)
        )
        content()
    }
}

/** 液态玻璃卡片：半透明底 + 细亮边框 + 阴影 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    corner: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    val bg = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.55f)
    val border = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.9f)
    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(corner), ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(corner)),
    ) {
        content()
    }
}

/** 模块入口卡片 */
@Composable
fun GlassModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    badge: String? = null,
    dark: Boolean = false,
    onClick: () -> Unit,
) {
    GlassCard(dark = dark, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = if (dark) 0.25f else 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) Color.White else Color(0xFF222222),
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            badge,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEC4141))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dark) Color.White.copy(alpha = 0.6f) else Color(0xFF666666),
                )
            }
            Text("›", color = if (dark) Color.White.copy(alpha = 0.4f) else Color(0xFFBBBBBB), fontSize = 22.sp)
        }
    }
}

/** 液态玻璃标题区 */
@Composable
fun GlassHeader(
    title: String,
    subtitle: String,
    dark: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color.White else Color(0xFF1A1A1A),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = if (dark) Color.White.copy(alpha = 0.55f) else Color(0xFF777777),
            )
        }
        if (trailing != null) trailing()
    }
}
