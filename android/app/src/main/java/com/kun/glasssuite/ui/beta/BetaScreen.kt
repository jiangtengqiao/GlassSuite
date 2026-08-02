package com.kun.glasssuite.ui.beta

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.kun.glasssuite.App
import com.kun.glasssuite.data.BetaStore
import com.kun.glasssuite.data.BetaStore.ApplyResult
import com.kun.glasssuite.data.BetaStore.Requirements
import kotlinx.coroutines.launch

/**
 * 开发者尝鲜模式：
 * 申请要求展示 → 填写申请 → 服务端实时评分（≥60 通过）→ 通过显示尝鲜码 / 驳回显示原因
 * 输入尝鲜码（22~37 位）实时比对激活 → 更新中心按权限推送 Beta 或正式版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()

    var req by remember { mutableStateOf<Requirements?>(null) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("开发者") }
    var reason by remember { mutableStateOf("") }
    var device by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ApplyResult?>(null) }
    var activeKey by remember { mutableStateOf(BetaStore.betaKey) }
    var activateMsg by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        req = BetaStore.requirements()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("开发者尝鲜") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 状态卡
            GlassStatusCard(
                betaAccess = BetaStore.betaAccess,
                version = com.kun.glasssuite.data.UpdateChecker.VERSION_NAME,
            )

            // 申请要求
            Text("申请要求（实时达标评估）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            req?.items?.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("• ", color = MaterialTheme.colorScheme.primary)
                    Text(item, style = MaterialTheme.typography.bodySmall)
                }
            }
            req?.let {
                Text(
                    "评分 ≥ ${it.threshold} 分即通过；未通过将被驳回并可再次申请。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 申请表单
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("姓名 / 昵称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("开发者", "测试", "媒体评测", "普通体验").forEach { p ->
                    FilterChip(
                        selected = purpose == p,
                        onClick = { purpose = p },
                        label = { Text(p) },
                    )
                }
            }
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("申请理由（不少于 15 字）") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = device,
                onValueChange = { device = it },
                label = { Text("设备信息（如：Pixel 8 / Android 15）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    busy = true
                    result = null
                    scope.launch {
                        result = BetaStore.apply(email, name, purpose, reason, device)
                        if (result?.status == "approved") {
                            app.settings.setBetaKey(result?.key ?: "")
                            BetaStore.betaKey = result?.key ?: ""
                            BetaStore.betaAccess = true
                            activeKey = BetaStore.betaKey
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("提交申请", fontWeight = FontWeight.Bold)
                }
            }

            // 结果卡
            result?.let { r ->
                when (r.status) {
                    "approved" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00B578))
                            Spacer(Modifier.width(8.dp))
                            Text("申请通过（评分 ${r.score}）", fontWeight = FontWeight.Bold, color = Color(0xFF00B578))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("你的尝鲜码（已自动激活，请妥善保存）：", style = MaterialTheme.typography.bodySmall)
                        Text(
                            r.key,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    "rejected" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, null, tint = Color(0xFFE84026))
                            Spacer(Modifier.width(8.dp))
                            Text("申请未通过（评分 ${r.score}）", fontWeight = FontWeight.Bold, color = Color(0xFFE84026))
                        }
                        r.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        Text("可完善信息后再次申请。", style = MaterialTheme.typography.bodySmall)
                    }
                    else -> Text(r.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // 尝鲜码激活
            Text("尝鲜码激活（22~37 位）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = activeKey,
                    onValueChange = { activeKey = it },
                    label = { Text("输入尝鲜码") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = {
                    scope.launch {
                        val ok = BetaStore.verify(activeKey.trim())
                        activateMsg = if (ok) "✓ 激活成功，已开启 Beta 渠道推送" else "✗ 尝鲜码无效，请检查后重试"
                        if (ok) app.settings.setBetaKey(BetaStore.betaKey)
                    }
                }) { Text("激活") }
            }
            if (activateMsg.isNotEmpty()) {
                Text(
                    activateMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (activateMsg.startsWith("✓")) Color(0xFF00B578) else Color(0xFFE84026),
                )
            }

            // 状态查询
            Text("申请状态查询", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "输入提交申请时的邮箱查询审批结果。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱（查询）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = {
                scope.launch {
                    result = BetaStore.status(email)
                }
            }) { Text("查询状态") }

            Text(
                "渠道说明：激活尝鲜码后，更新中心将监测并推送 Beta 版本；未激活时仅监测正式版本。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GlassStatusCard(betaAccess: Boolean, version: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .background(Color(0x22EC4141), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (betaAccess) "尝鲜成员 · Beta 渠道已开启" else "普通用户 · 仅正式版渠道",
                fontWeight = FontWeight.Bold,
                color = if (betaAccess) Color(0xFFEC4141) else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "当前版本：v$version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (betaAccess) "BETA" else "STABLE",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .background(if (betaAccess) Color(0xFFEC4141) else Color(0xFF888888))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
