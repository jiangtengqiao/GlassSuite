package com.kun.glasssuite.ui.login

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.kun.glasssuite.App
import com.kun.glasssuite.data.Api
import com.kun.glasssuite.data.AppConfig
import com.kun.glasssuite.data.Profile
import com.kun.glasssuite.util.Utils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("登录") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("扫码登录") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("验证码登录") })
            }
            when (tab) {
                0 -> QrLoginTab(onLoggedIn)
                1 -> SmsLoginTab(onLoggedIn)
            }
        }
    }
}

@Composable
private fun QrLoginTab(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("正在获取二维码…") }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val keyResp = Api.service.qrKey()
                val key = keyResp.data?.unikey
                if (key.isNullOrBlank()) {
                    status = "获取二维码失败，请检查服务器地址"
                    delay(2000)
                    continue
                }
                val createResp = Api.service.qrCreate(key, true)
                qr = Utils.base64DataUriToBitmap(createResp.data?.qrimg)
                status = "请使用网易云音乐 App 扫码登录"
                var refreshed = false
                while (!refreshed) {
                    delay(1500)
                    val check = Api.service.qrCheck(key)
                    when (check.code) {
                        800 -> {
                            status = "二维码已过期，正在刷新…"
                            refreshed = true
                        }
                        801 -> status = "等待扫码…"
                        802 -> status = "已扫码，请在手机端确认登录"
                        803 -> {
                            check.cookie?.let { Api.cookieJar.saveCookieString(it) }
                            val profile = fetchProfile()
                            if (profile != null) {
                                val json = Gson().toJson(profile)
                                AppConfig.userId = profile.userId
                                AppConfig.profileJson = json
                                AppConfig.setProfile(profile)
                                app.settings.setLogin(profile.userId, json)
                                status = "登录成功"
                                onLoggedIn()
                                return@LaunchedEffect
                            }
                            status = "登录态获取失败，重试中…"
                        }
                    }
                }
            } catch (e: Exception) {
                status = "网络异常：${e.message ?: "请检查服务器地址"}"
                delay(3000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        if (qr != null) {
            Image(
                bitmap = qr!!.asImageBitmap(),
                contentDescription = "登录二维码",
                modifier = Modifier.size(240.dp),
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "1. 打开网易云音乐 App\n2. 点击「我的」右上角扫码\n3. 扫描二维码并确认登录",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private suspend fun fetchProfile(): Profile? {
    val st = runCatching { Api.service.loginStatus() }.getOrNull()
    st?.data?.profile?.let { return it }
    return runCatching { Api.service.userAccount() }.getOrNull()?.profile
}

@Composable
private fun SmsLoginTab(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.take(11) },
            label = { Text("手机号") },
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.take(6) },
                label = { Text("验证码") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    if (phone.length != 11) {
                        status = "请输入 11 位手机号"
                        return@Button
                    }
                    // 发送验证码
                    scope.launch {
                        try {
                            val resp = Api.service.captchaSent(phone)
                            if (resp.code == 200) {
                                status = "验证码已发送"
                                countdown = 60
                            } else {
                                status = resp.message ?: "发送失败，请稍后重试"
                            }
                        } catch (e: Exception) {
                            status = "发送失败：${e.message ?: "网络异常"}"
                        }
                    }
                },
                enabled = countdown == 0,
            ) {
                Text(if (countdown > 0) "${countdown}s" else "发送验证码")
            }
        }
        Button(
            onClick = {
                if (phone.length != 11) {
                    status = "请输入 11 位手机号"
                    return@Button
                }
                if (code.length < 4) {
                    status = "请输入验证码"
                    return@Button
                }
                busy = true
                scope.launch {
                    try {
                        val resp = Api.service.loginCellphone(phone, code)
                        if (resp.code == 200) {
                            resp.cookie?.let { Api.cookieJar.saveCookieString(it) }
                            val profile = resp.profile
                            if (profile != null) {
                                val json = Gson().toJson(profile)
                                AppConfig.userId = profile.userId
                                AppConfig.profileJson = json
                                AppConfig.setProfile(profile)
                                app.settings.setLogin(profile.userId, json)
                                onLoggedIn()
                                return@launch
                            }
                            status = "登录成功，但未获取到用户信息"
                        } else {
                            status = resp.message ?: "登录失败（验证码错误或手机号未注册）"
                        }
                    } catch (e: Exception) {
                        status = "登录失败：${e.message ?: "网络异常"}"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("登录", fontWeight = FontWeight.Bold)
            }
        }
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
