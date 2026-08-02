package com.kun.glasssuite.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 更新与公告中心：
 * - 实时自动自查更新（启动即查 + 每 30 分钟轮询 GitHub Releases）
 * - 在线公告拉取与解析
 * - 新版本/新公告事件流（供弹窗推送）
 */
object UpdateChecker {

    const val VERSION_NAME = "1.1.0"
    const val CHECK_INTERVAL_MS = 30 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _latest = MutableStateFlow<Release?>(null)
    val latest: StateFlow<Release?> = _latest.asStateFlow()

    private val _hasUpdate = MutableStateFlow(false)
    val hasUpdate: StateFlow<Boolean> = _hasUpdate.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _lastChecked = MutableStateFlow(0L)
    val lastChecked: StateFlow<Long> = _lastChecked.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    /** 新公告计数（自上次查看后） */
    private val _newAnnouncements = MutableStateFlow(0)
    val newAnnouncements: StateFlow<Int> = _newAnnouncements.asStateFlow()

    /** 事件流：新版本弹窗 / 公告更新提示 */
    private val _events = MutableSharedFlow<UpdateEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    sealed class UpdateEvent {
        data class NewVersion(val release: Release) : UpdateEvent()
        data class Announcements(val list: List<Announcement>) : UpdateEvent()
    }

    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            checkNow()
            while (true) {
                delay(CHECK_INTERVAL_MS)
                checkNow()
            }
        }
    }

    /** 手动检查（返回是否有更新）。有尝鲜权限推 Beta；否则仅正式版可被监测推送 */
    suspend fun checkNow(): Boolean {
        _checking.value = true
        try {
            val release = GitHubApi.latestRelease()
            _lastChecked.value = System.currentTimeMillis()
            if (release != null) {
                val prev = _latest.value
                _latest.value = release
                // 渠道过滤：无尝鲜权限时跳过 Beta/RC 版本
                val betaOk = BetaStore.betaAccess || !isPreRelease(release.tagName)
                val newer = betaOk && isNewer(release.tagName, VERSION_NAME)
                _hasUpdate.value = newer
                if (newer && prev?.tagName != release.tagName) {
                    _events.tryEmit(UpdateEvent.NewVersion(release))
                }
                // 同步拉取公告
                val md = GitHubApi.fetchAnnouncementsRaw()
                val list = GitHubApi.parseAnnouncements(md)
                if (list.isNotEmpty()) {
                    val oldIds = _announcements.value.map { it.id }.toSet()
                    val newOnes = list.filter { it.id !in oldIds }
                    _announcements.value = list
                    if (newOnes.isNotEmpty()) {
                        _newAnnouncements.value = newOnes.size
                        _events.tryEmit(UpdateEvent.Announcements(newOnes))
                    }
                }
            }
            return _hasUpdate.value
        } catch (e: Exception) {
            return _hasUpdate.value
        } finally {
            _checking.value = false
        }
    }

    fun markAnnouncementsSeen() {
        _newAnnouncements.value = 0
    }

    /** 是否为预发布版本（beta/rc 后缀） */
    fun isPreRelease(tag: String): Boolean = tag.contains("-beta") || tag.contains("-rc") || tag.contains("-alpha")

    /** 版本比较：x.y.z(-beta.n)，正式 > 同号 beta；支持逐位比较 */
    fun isNewer(tag: String, current: String): Boolean {
        data class Ver(val nums: List<Int>, val pre: Int, val preN: Int)
        fun parse(v: String): Ver {
            val core = v.removePrefix("v")
            val parts = core.split("-")
            val nums = parts[0].split(".").mapNotNull { it.toIntOrNull() }
            var pre = 0; var preN = 0
            if (parts.size > 1) {
                val seg = parts[1].split(".")
                pre = when {
                    seg[0].startsWith("alpha") -> 1
                    seg[0].startsWith("beta") -> 2
                    seg[0].startsWith("rc") -> 3
                    else -> 0
                }
                preN = seg.getOrNull(1)?.toIntOrNull() ?: 0
            }
            return Ver(nums, pre, preN)
        }
        val a = parse(tag); val b = parse(current)
        val n = maxOf(a.nums.size, b.nums.size)
        for (i in 0 until n) {
            val x = a.nums.getOrElse(i) { 0 }; val y = b.nums.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        if (a.pre != b.pre) return a.pre > b.pre      // 正式(0) > rc(3) > beta(2) > alpha(1)
        return a.preN > b.preN
    }

    /** 内置默认公告（在线公告不可用时兜底） */
    /** 内置默认公告（在线公告不可用时兜底） */
    fun builtinAnnouncements(): List<Announcement> = listOf(
        Announcement(
            id = "builtin-welcome",
            title = "欢迎使用璃光 GlassSuite",
            content = "GlassSuite 多功能应用套件正式开放 Beta 尝鲜：液态玻璃 UI、GitHub 检索、更新中心、公告推送与音乐模块。\n当前版本：$VERSION_NAME",
            isPromo = false,
            date = "2026-08-02",
        ),
        Announcement(
            id = "builtin-music",
            title = "音乐模块使用说明",
            content = "音乐数据由你部署的接口服务器提供：cd server && docker compose up -d，然后在「设置 → 服务器连接」填入地址并测试。\n模拟器默认 http://10.0.2.2:3000；真机填电脑局域网 IP。访客无需登录即可试听大部分曲目。",
            isPromo = false,
            date = "2026-08-02",
        ),
        Announcement(
            id = "builtin-beta",
            title = "开发者尝鲜计划开放申请",
            content = "提交申请并通过自动评分（≥60 分）即可获得 22~37 位唯一尝鲜码，提前体验 Beta 版本推送；未激活尝鲜码仅接收正式版推送。",
            isPromo = false,
            date = "2026-08-02",
        ),
        Announcement(
            id = "builtin-update",
            title = "更新中心已上线",
            content = "启动即自动检查更新，每 30 分钟轮询一次；发现新版本将弹窗推送，更新日志支持点击折叠展开。",
            isPromo = false,
            date = "2026-08-02",
        ),
        Announcement(
            id = "builtin-promo",
            title = "关注仓库获取最新动态",
            content = "Star 并 Watch 仓库，第一时间获取版本发布、公告与尝鲜名额动态。",
            isPromo = true,
            date = "2026-08-02",
            url = "https://github.com/jiangtengqiao/GlassSuite",
        ),
    )
}