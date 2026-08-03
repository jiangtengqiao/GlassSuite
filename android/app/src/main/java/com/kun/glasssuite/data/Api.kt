package com.kun.glasssuite.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {
    // 登录
    suspend fun qrKey(): QrKeyResp
    suspend fun qrCreate(key: String, qrimg: Boolean = true): QrCreateResp
    suspend fun qrCheck(key: String): QrCheckResp
    suspend fun loginCellphone(
        phone: String,
        captcha: String,
        timestamp: Long = System.currentTimeMillis(),
    ): LoginResp
    suspend fun captchaSent(
        phone: String,
        timestamp: Long = System.currentTimeMillis(),
    ): SimpleResp
    suspend fun captchaVerify(
        phone: String,
        captcha: String,
        timestamp: Long = System.currentTimeMillis(),
    ): SimpleResp
    suspend fun loginStatus(): LoginStatusResp
    suspend fun userAccount(): AccountResp
    suspend fun userDetail(uid: Long): UserDetailResp

    // 用户歌单 / 喜欢
    suspend fun userPlaylist(uid: Long): UserPlaylistResp
    suspend fun likeList(uid: Long): LikeListResp
    suspend fun like(id: Long, like: Boolean): SimpleResp

    // 发现
    suspend fun recommendResource(): RecommendResp
    suspend fun recommendSongs(): DailySongsResp
    suspend fun newSongs(limit: Int = 30): NewsongResp
    suspend fun toplist(): ToplistResp
    suspend fun hotPlaylist(
        cat: String,
        order: String = "hot",
        limit: Int = 50,
        offset: Int = 0,
    ): HotPlaylistResp
    suspend fun catlist(): CatlistResp

    // 歌单
    suspend fun playlistDetail(id: Long): PlaylistDetailResp
    suspend fun playlistTracks(
        id: Long,
        limit: Int = 1000,
        offset: Int = 0,
    ): PlaylistTracksResp
    suspend fun playlistSubscribe(id: Long, t: Int): SimpleResp

    // 歌曲
    suspend fun songDetail(ids: String): SongDetailResp
    suspend fun songUrl(id: Long, level: String): SongUrlResp
    suspend fun lyric(id: Long): LyricResp

    // 搜索
    suspend fun search(
        keywords: String,
        type: Int,
        limit: Int = 50,
        offset: Int = 0,
    ): SearchResp
    suspend fun suggest(
        keywords: String,
        type: String = "mobile",
    ): SuggestResp
    suspend fun hotSearch(): HotSearchResp

    // 歌手 / 专辑 / MV
    suspend fun artist(id: Long): ArtistResp
    suspend fun artistTopSongs(id: Long): ArtistSongsResp
    suspend fun artistAlbums(id: Long, limit: Int = 30): ArtistAlbumResp
    suspend fun album(id: Long): AlbumResp
    suspend fun mvDetail(id: Long): MvDetailResp
    suspend fun mvUrl(id: Long, r: Int = 1080): MvUrlResp
}

interface ApiService : MusicApi {
    // 登录
    @GET("login/qr/key") override suspend fun qrKey(): QrKeyResp
    @GET("login/qr/create") override suspend fun qrCreate(
        @Query("key") key: String,
        @Query("qrimg") qrimg: Boolean ,
    ): QrCreateResp
    @GET("login/qr/check") override suspend fun qrCheck(@Query("key") key: String): QrCheckResp
    @GET("login/cellphone") override suspend fun loginCellphone(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String,
        @Query("timestamp") timestamp: Long ,
    ): LoginResp
    @GET("captcha/sent") override suspend fun captchaSent(
        @Query("phone") phone: String,
        @Query("timestamp") timestamp: Long ,
    ): SimpleResp
    @GET("captcha/verify") override suspend fun captchaVerify(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String,
        @Query("timestamp") timestamp: Long ,
    ): SimpleResp
    @GET("login/status") override suspend fun loginStatus(): LoginStatusResp
    @GET("user/account") override suspend fun userAccount(): AccountResp
    @GET("user/detail") override suspend fun userDetail(@Query("uid") uid: Long): UserDetailResp

    // 用户歌单 / 喜欢
    @GET("user/playlist") override suspend fun userPlaylist(@Query("uid") uid: Long): UserPlaylistResp
    @GET("likelist") override suspend fun likeList(@Query("uid") uid: Long): LikeListResp
    @GET("like") override suspend fun like(@Query("id") id: Long, @Query("like") like: Boolean): SimpleResp

    // 发现
    @GET("recommend/resource") override suspend fun recommendResource(): RecommendResp
    @GET("recommend/songs") override suspend fun recommendSongs(): DailySongsResp
    @GET("personalized/newsong") override suspend fun newSongs(@Query("limit") limit: Int ): NewsongResp
    @GET("toplist") override suspend fun toplist(): ToplistResp
    @GET("top/playlist") override suspend fun hotPlaylist(
        @Query("cat") cat: String,
        @Query("order") order: String ,
        @Query("limit") limit: Int ,
        @Query("offset") offset: Int ,
    ): HotPlaylistResp
    @GET("playlist/catlist") override suspend fun catlist(): CatlistResp

    // 歌单
    @GET("playlist/detail") override suspend fun playlistDetail(@Query("id") id: Long): PlaylistDetailResp
    @GET("playlist/track/all") override suspend fun playlistTracks(
        @Query("id") id: Long,
        @Query("limit") limit: Int ,
        @Query("offset") offset: Int ,
    ): PlaylistTracksResp
    @GET("playlist/subscribe") override suspend fun playlistSubscribe(@Query("id") id: Long, @Query("t") t: Int): SimpleResp

    // 歌曲
    @GET("song/detail") override suspend fun songDetail(@Query("ids") ids: String): SongDetailResp
    @GET("song/url/v1") override suspend fun songUrl(@Query("id") id: Long, @Query("level") level: String): SongUrlResp
    @GET("lyric") override suspend fun lyric(@Query("id") id: Long): LyricResp

    // 搜索
    @GET("search") override suspend fun search(
        @Query("keywords") keywords: String,
        @Query("type") type: Int,
        @Query("limit") limit: Int ,
        @Query("offset") offset: Int ,
    ): SearchResp
    @GET("search/suggest") override suspend fun suggest(
        @Query("keywords") keywords: String,
        @Query("type") type: String ,
    ): SuggestResp
    @GET("search/hot") override suspend fun hotSearch(): HotSearchResp

    // 歌手 / 专辑 / MV
    @GET("artist") override suspend fun artist(@Query("id") id: Long): ArtistResp
    @GET("artist/top/song") override suspend fun artistTopSongs(@Query("id") id: Long): ArtistSongsResp
    @GET("artist/album") override suspend fun artistAlbums(@Query("id") id: Long, @Query("limit") limit: Int ): ArtistAlbumResp
    @GET("album") override suspend fun album(@Query("id") id: Long): AlbumResp
    @GET("mv/detail") override suspend fun mvDetail(@Query("mvid") id: Long): MvDetailResp
    @GET("mv/url") override suspend fun mvUrl(@Query("id") id: Long, @Query("r") r: Int ): MvUrlResp
}

/** 持久化 Cookie（登录态） */
class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cloudmusic_cookies", Context.MODE_PRIVATE)

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        prefs.edit().apply {
            cookies.forEach { putString(it.name, it.value) }
        }.apply()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return prefs.all.mapNotNull { (k, v) ->
            runCatching {
                Cookie.Builder().name(k).value(v.toString()).domain(url.host).path("/").build()
            }.getOrNull()
        }
    }

    @Synchronized
    fun saveCookieString(cookieStr: String) {
        prefs.edit().apply {
            cookieStr.split(";").forEach { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0) {
                    val name = pair.substring(0, idx).trim()
                    val value = pair.substring(idx + 1).trim()
                    if (name.isNotEmpty()) putString(name, value)
                }
            }
        }.apply()
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }
}

/** 全局网络单例，支持 API 地址热切换 */
object Api {

    private lateinit var appContext: Context
    lateinit var cookieJar: PersistentCookieJar
        private set
    private var retrofit: Retrofit? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        cookieJar = PersistentCookieJar(appContext)
        rebuild()
    }

    fun rebuild() {
        val base = AppConfig.apiBaseUrl.trim().trimEnd('/')
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", UA)
                    .header("Referer", "https://music.163.com/")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(if (base.endsWith("/")) base else "$base/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }

    val service: MusicApi
        get() = if (AppConfig.directMode) {
            NeteaseDirect
        } else {
            retrofit?.create(ApiService::class.java)
                ?: throw IllegalStateException("Api 未初始化，请先在设置中配置服务器地址")
        }

    const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val QUALITY_LEVELS = listOf("standard", "higher", "exhigh", "lossless")
    val QUALITY_NAMES = mapOf(
        "standard" to "标准品质",
        "higher" to "较高品质",
        "exhigh" to "极高品质",
        "lossless" to "无损品质",
    )
}
