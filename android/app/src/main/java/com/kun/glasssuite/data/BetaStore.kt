package com.kun.glasssuite.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/**
 * 开发者尝鲜系统客户端：
 * - 申请提交（自动评分筛选由服务端实时判定）
 * - 尝鲜码（22~37 位）激活与实时比对
 * - 申请状态查询
 * - betaAccess 决定更新渠道（有权限推 Beta，否则仅正式版）
 */
object BetaStore {

    var betaServerUrl: String = "http://10.0.2.2:3100"
    var betaKey: String = ""
    var betaAccess: Boolean = false
    /** 尝鲜层级：0 正式用户 / 1 Beta 尝鲜 / 2 Alpha 内测 / 3 开发者核心 */
    var betaTier: Int = 0

    val tierName: String
        get() = when (betaTier) {
            3 -> "开发者核心"
            2 -> "Alpha 内测"
            1 -> "Beta 尝鲜"
            else -> "正式用户"
        }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private suspend fun get(path: String): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(betaServerUrl.trimEnd('/') + path).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else JSONObject(resp.body?.string() ?: "{}")
            }
        }.getOrNull()
    }

    private suspend fun post(path: String, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(betaServerUrl.trimEnd('/') + path)
                .post(body.toString().toRequestBody(jsonType))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else JSONObject(resp.body?.string() ?: "{}")
            }
        }.getOrNull()
    }

    data class Requirements(
        val title: String = "",
        val intro: String = "",
        val items: List<String> = emptyList(),
        val threshold: Int = 60,
        val note: String = "",
    )

    data class ApplyResult(
        val status: String = "none",   // approved / rejected
        val score: Int = 0,
        val key: String = "",
        val reasons: List<String> = emptyList(),
        val message: String = "",
    )

    suspend fun requirements(): Requirements {
        val r = get("/requirements") ?: return Requirements()
        val data = r.optJSONObject("data") ?: return Requirements()
        val items = mutableListOf<String>()
        data.optJSONArray("items")?.let { arr ->
            for (i in 0 until arr.length()) items.add(arr.optString(i))
        }
        return Requirements(
            title = data.optString("title"),
            intro = data.optString("intro"),
            items = items,
            threshold = data.optInt("scoreThreshold", 60),
            note = data.optString("note"),
        )
    }

    suspend fun apply(email: String, name: String, purpose: String, reason: String, device: String): ApplyResult {
        val body = JSONObject()
            .put("email", email)
            .put("name", name)
            .put("purpose", purpose)
            .put("reason", reason)
            .put("device", device)
        val r = post("/apply", body) ?: return ApplyResult(status = "error", message = "服务不可达，请检查 Beta 服务器地址")
        val reasons = mutableListOf<String>()
        r.optJSONArray("reasons")?.let { arr ->
            for (i in 0 until arr.length()) reasons.add(arr.optString(i))
        }
        return ApplyResult(
            status = r.optString("status", "none"),
            score = r.optInt("score", 0),
            key = r.optString("key", ""),
            reasons = reasons,
            message = r.optString("message", ""),
        )
    }

    suspend fun status(email: String): ApplyResult {
        val r = get("/status?email=${java.net.URLEncoder.encode(email, "UTF-8")}") ?: return ApplyResult(status = "error")
        val reasons = mutableListOf<String>()
        r.optJSONArray("reasons")?.let { arr ->
            for (i in 0 until arr.length()) reasons.add(arr.optString(i))
        }
        return ApplyResult(
            status = r.optString("status", "none"),
            score = r.optInt("score", 0),
            key = r.optString("key", ""),
            reasons = reasons,
        )
    }

    /** 尝鲜码实时比对；成功则激活 beta 渠道 */
    suspend fun verify(key: String): Boolean {
        val body = JSONObject().put("key", key)
        val r = post("/verify", body) ?: return false
        val valid = r.optBoolean("valid", false)
        if (valid) {
            betaKey = key
            betaAccess = true
            betaTier = r.optInt("tier", 1)
        } else {
            betaTier = 0
        }
        return valid
    }

    /** 当前层级可用的更新通道 */
    fun channels(): List<String> {
        val c = mutableListOf("stable")
        if (betaTier >= 1) c.add("beta")
        if (betaTier >= 2) c.add("alpha")
        if (betaTier >= 3) c.add("dev")
        return c
    }
}
