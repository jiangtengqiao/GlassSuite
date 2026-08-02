package com.kun.glasssuite.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** GitHub 仓库 */
data class Repo(
    val id: Long = 0,
    val name: String = "",
    @SerializedName("full_name") val fullName: String = "",
    val description: String = "",
    @SerializedName("stargazers_count") val stargazersCount: Long = 0,
    @SerializedName("forks_count") val forksCount: Long = 0,
    val language: String? = null,
    @SerializedName("html_url") val htmlUrl: String = "",
    val owner: RepoOwner? = null,
)

data class RepoOwner(val login: String = "", @SerializedName("avatar_url") val avatarUrl: String = "")

/** GitHub Release */
data class Release(
    @SerializedName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerializedName("published_at") val publishedAt: String = "",
    @SerializedName("html_url") val htmlUrl: String = "",
    val assets: List<ReleaseAsset> = emptyList(),
    val prerelease: Boolean = false,
)

data class ReleaseAsset(
    val name: String = "",
    val size: Long = 0,
    @SerializedName("browser_download_url") val browserDownloadUrl: String = "",
)

/** 公告条目（从 ANNOUNCEMENTS.md 解析） */
data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val isPromo: Boolean,
    val url: String = "",
    val date: String = "",
)

/**
 * GitHub REST API 客户端：搜索/热门仓库、Releases、raw 文件。
 * 公开接口无需鉴权（有速率限制），异常由调用方捕获。
 */
object GitHubApi {

    var owner: String = "jiangtengqiao"
    var repo: String = "GlassSuite"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = com.google.gson.Gson()

    /** 是否启用国内加速代理（GitHub API 在国内直连不稳定时自动尝试） */
    var useProxy: Boolean = true

    private suspend fun <T> get(url: String, clazz: Class<T>): T? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val candidates = if (useProxy) listOf("https://ghproxy.net/" + url, url) else listOf(url)
        for (u in candidates) {
            try {
                val req = Request.Builder()
                    .url(u)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "GlassSuite")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) return@withContext gson.fromJson(resp.body?.string(), clazz)
                }
            } catch (e: Exception) {
            }
        }
        null
    }

    /** 内置热门仓库兜底（代理与直连均失败时展示，保证模块可用） */
    fun builtinTrending(): List<Repo> {
        val names = listOf(
            "JetBrains/kotlin" to "Kotlin 编程语言",
            "square/okhttp" to "高效的 HTTP 客户端",
            "google/compose-multiplatform" to "Compose 多平台 UI 框架",
            "golang/go" to "Go 编程语言",
            "microsoft/vscode" to "VS Code 编辑器",
            "torvalds/linux" to "Linux 内核",
            "flutter/flutter" to "Flutter UI 框架",
            "pytorch/pytorch" to "PyTorch 深度学习框架",
            "tensorflow/tensorflow" to "TensorFlow 机器学习框架",
            "rust-lang/rust" to "Rust 编程语言",
            "nodejs/node" to "Node.js 运行时",
            "spring-projects/spring-boot" to "Spring Boot 框架",
            "kubernetes/kubernetes" to "Kubernetes 容器编排",
            "docker/docker" to "Docker 容器平台",
            "git/git" to "Git 版本控制",
            "vuejs/vue" to "Vue.js 前端框架",
            "facebook/react" to "React 前端框架",
            "angular/angular" to "Angular 前端框架",
            "apache/spark" to "Apache Spark 大数据引擎",
            "elastic/elasticsearch" to "Elasticsearch 搜索引擎",
        )
        return names.mapIndexed { i, (full, desc) ->
            Repo(
                id = i + 1L,
                fullName = full,
                description = desc,
                stargazersCount = 100000L - i * 1000L,
                htmlUrl = "https://github.com/$full",
                language = full.substringAfter('/').substringBefore('-'),
            )
        }
    }

    /** 搜索仓库 */
    suspend fun searchRepos(query: String, sort: String = "stars", limit: Int = 30): List<Repo> {
        val q = query.trim().ifEmpty { "stars:>10000" }
        val url = "https://api.github.com/search/repositories?q=${java.net.URLEncoder.encode(q, "UTF-8")}&sort=$sort&order=desc&per_page=$limit"
        val resp = get(url, GitHubSearchResp::class.java)
        if (resp?.items != null) return resp.items!!
        // 网络不可达时用内置兜底，保证模块可用
        return builtinTrending().filter { q.isBlank() || it.fullName.contains(q, true) || it.description.contains(q, true) }
    }

    /** 热门仓库（按 star 排序） */
    suspend fun trendingRepos(limit: Int = 30): List<Repo> =
        searchRepos("stars:>10000 pushed:>2025-01-01", "stars", limit)

    /** Releases 列表（最新在前） */
    suspend fun releases(limit: Int = 10): List<Release> {
        val url = "https://api.github.com/repos/$owner/$repo/releases?per_page=$limit"
        val resp = get(url, Array<Release>::class.java) ?: return emptyList()
        return resp.toList()
    }

    suspend fun latestRelease(): Release? = releases(1).firstOrNull()

    /** 拉取在线公告（raw.githubusercontent.com） */
    suspend fun fetchAnnouncementsRaw(): String? {
        val url = "https://raw.githubusercontent.com/$owner/$repo/main/ANNOUNCEMENTS.md"
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).header("User-Agent", "GlassSuite").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /** 解析公告 Markdown：## [公告|推广] 标题 + 正文 */
    fun parseAnnouncements(md: String?): List<Announcement> {
        if (md.isNullOrBlank()) return emptyList()
        val result = mutableListOf<Announcement>()
        var current: MutableList<String>? = null
        var currentTitle = ""
        var currentIsPromo = false
        var currentUrl = ""
        var currentDate = ""
        md.lines().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("## ")) {
                if (current != null && currentTitle.isNotEmpty()) {
                    result += Announcement(
                        id = currentTitle.hashCode().toString(),
                        title = currentTitle,
                        content = current!!.joinToString("\n").trim(),
                        isPromo = currentIsPromo,
                        url = currentUrl,
                        date = currentDate,
                    )
                }
                current = mutableListOf()
                val title = line.removePrefix("## ").trim()
                currentTitle = title.removePrefix("[公告] ").removePrefix("[推广] ").trim()
                currentIsPromo = title.startsWith("[推广]")
            } else if (line.startsWith("- 链接:")) {
                currentUrl = line.removePrefix("- 链接:").trim()
            } else if (line.startsWith("- 日期:")) {
                currentDate = line.removePrefix("- 日期:").trim()
            } else if (current != null) {
                if (line.isNotEmpty() && !line.startsWith("#")) current!!.add(line)
            }
        }
        if (current != null && currentTitle.isNotEmpty()) {
            result += Announcement(
                id = currentTitle.hashCode().toString(),
                title = currentTitle,
                content = current!!.joinToString("\n").trim(),
                isPromo = currentIsPromo,
                url = currentUrl,
                date = currentDate,
            )
        }
        return result
    }
}

private data class GitHubSearchResp(val items: List<Repo>?)
