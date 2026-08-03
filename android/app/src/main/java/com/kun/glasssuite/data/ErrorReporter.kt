package com.kun.glasssuite.data

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 报错上传体系：
 * - 全局未捕获异常/崩溃捕获（Java + Kotlin + 崩溃后落盘）
 * - 滚动错误日志（按天分文件，保留最近 20 个）
 * - 设备与版本信息采集
 * - 自动上传：优先 Beta 服务器 /api/error，其次自托管服务器 /api/error；
 *   失败保留本地，启动时与定时器自动重试（指数退避）
 * - 设置页可查看 / 手动上传 / 导出 / 清空
 */
object ErrorReporter {

    private const val MAX_FILES = 20
    private const val MAX_BYTES_PER_FILE = 1024 * 1024 // 1MB/天
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private lateinit var dir: File

    /** 应用内最近一次未上报错误数（供设置页显示） */
    @Volatile
    var pendingCount: Int = 0
        private set

    @Volatile
    var lastUploadAt: Long = 0L
        private set

    fun init(context: Context) {
        dir = File(context.filesDir, "errors").apply { mkdirs() }
        // 注册全局崩溃捕获（替换默认 handler，崩溃信息落盘后仍走默认行为）
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log("crash", "线程 ${thread.name} 崩溃", throwable)
            pendingCount = countPending()
            // 尝试同步上报一次（崩溃场景下尽力而为）
            runCatching { uploadPendingBlocking() }
            android.util.Log.e("GlassSuite", "未捕获异常", throwable)
        }
        pendingCount = countPending()
        // 启动时静默补传
        scope.launch {
            delay(5000)
            retryLoop()
        }
    }

    /** 记录一条业务/捕获错误 */
    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val sb = StringBuilder()
        sb.append("[${timestamp()}] [$tag] ").append(message).append('\n')
        throwable?.let {
            sb.append(it.toString()).append('\n')
            it.stackTrace?.take(40)?.forEach { f -> sb.append("    at ").append(f).append('\n') }
            it.cause?.let { c -> sb.append("Caused by: ").append(c).append('\n') }
        }
        appendToFile(sb.toString())
        pendingCount = countPending()
    }

    /** 采集设备与应用信息（错误包头部） */
    fun deviceInfoJson(): String = gson.toJson(
        mapOf(
            "app" to "GlassSuite",
            "version" to UpdateChecker.VERSION_NAME,
            "os" to "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "arch" to Build.SUPPORTED_ABIS.joinToString(","),
            "directMode" to AppConfig.directMode,
            "apiBase" to AppConfig.apiBaseUrl,
            "userId" to AppConfig.userId,
            "time" to timestamp(),
        ),
    )

    /** 待上报文件列表（未上传的错误文件） */
    fun pendingFiles(): List<File> =
        dir.listFiles { f -> f.name.endsWith(".pending.log") }?.sortedBy { it.lastModified() } ?: emptyList()

    /** 最近日志（含已上传历史，供查看） */
    fun recentLogs(limit: Int = 200): String {
        val files = dir.listFiles { f -> f.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
        val sb = StringBuilder()
        sb.append("设备信息：\n").append(deviceInfoJson()).append("\n\n")
        var remain = limit
        for (f in files) {
            if (remain <= 0) break
            val lines = f.readLines().takeLast(remain)
            sb.append("── ${f.name} ──\n")
            sb.append(lines.joinToString("\n")).append("\n")
            remain -= lines.size
        }
        return sb.toString()
    }

    /** 手动上传全部待上报错误 */
    suspend fun uploadAll(): Int = withContext(Dispatchers.IO) {
        var ok = 0
        for (f in pendingFiles()) {
            if (uploadFile(f)) {
                f.delete()
                ok++
            }
        }
        pendingCount = countPending()
        if (ok > 0) lastUploadAt = System.currentTimeMillis()
        ok
    }

    /** 清空所有错误日志 */
    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
        pendingCount = 0
    }

    // ---------- 内部 ----------

    private fun countPending(): Int = pendingFiles().size

    private fun appendToFile(text: String) {
        if (!::dir.isInitialized) return
        val name = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val f = File(dir, "$name.log")
        if (f.length() > MAX_BYTES_PER_FILE) {
            // 当天的日志太大则归档为待上报文件
            val archive = File(dir, "$name-${System.currentTimeMillis()}.pending.log")
            f.renameTo(archive)
            File(dir, "$name.log").writeText(text)
        } else {
            f.appendText(text)
        }
        // 滚动清理：保留最近 20 个文件
        val all = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (all.size > MAX_FILES) {
            all.drop(MAX_FILES).forEach { it.delete() }
        }
    }

    private suspend fun retryLoop() {
        var backoff = 1
        while (true) {
            if (pendingCount > 0) {
                val ok = uploadAll()
                backoff = if (ok > 0) 1 else (backoff * 2).coerceAtMost(128)
            }
            delay(backoff * 60_000L)
        }
    }

    private fun uploadFile(f: File): Boolean {
        val body = buildString {
            append("{\"device\":")
            append(deviceInfoJson())
            append(",\"log\":")
            append(gson.toJson(f.readText().takeLast(64 * 1024)))
            append("}")
        }.toRequestBody("application/json; charset=utf-8".toMediaType())

        // 通道 1：Beta 服务器
        if (BetaStore.betaServerUrl.isNotBlank()) {
            val req = Request.Builder()
                .url(BetaStore.betaServerUrl.trimEnd('/') + "/api/error")
                .post(body)
                .build()
            runCatching {
                client.newCall(req).execute().use { if (it.isSuccessful) return true }
            }
        }
        // 通道 2：自托管音乐服务器
        if (!AppConfig.directMode && AppConfig.apiBaseUrl.isNotBlank()) {
            val req = Request.Builder()
                .url(AppConfig.apiBaseUrl.trimEnd('/') + "/api/error")
                .post(body)
                .build()
            runCatching {
                client.newCall(req).execute().use { if (it.isSuccessful) return true }
            }
        }
        return false
    }

    /** 崩溃场景同步尽力上传（不依赖协程） */
    private fun uploadPendingBlocking() {
        for (f in pendingFiles()) {
            if (uploadFile(f)) f.delete()
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}
