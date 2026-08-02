package com.kun.cloudmusic.data

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

interface ApiService {
    // 登录
    @GET("login/qr/key") suspend fun qrKey(): QrKeyResp
    @GET("login/qr/create") suspend fun qrCreate(
        @Query("key") key: String,
        @Query("qrimg") qrimg: Boolean = true,
    ): QrCreateResp
    @GET("login/qr/check") suspend fun qrCheck(@Query("key") key: String): QrCheckResp
    @GET("login/cellphone") suspend fun loginCellphone(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): LoginResp
    @GET("captcha/sent") suspend fun captchaSent(
        @Query("phone") phone: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): SimpleResp
    @GET("captcha/verify") suspend fun captchaVerify(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis(),
    ): SimpleResp
    @GET("login/status") suspend fun loginStatus(): LoginStatusResp
    @GET("user/account") suspend fun userAccount(): AccountResp
    @GET("user/detail") suspend fun userDetail(@Query("uid") uid: Long): UserDetailResp

    // 用户歌单 / 喜欢
    @GET("user/playlist") suspend fun userPlaylist(@Query("uid") uid: Long): UserPlaylistResp
    @GET("likelist") suspend fun likeList(@Query("uid") uid: Long): LikeListResp
    @GET("like") suspend fun like(@Query("id") id: Long, @Query("like") like: Boolean): SimpleResp

    // 发现
    @GET("recommend/resource") suspend fun recommendResource(): RecommendResp
    @GET("recommend/songs") suspend fun recommendSongs(): DailySongsResp
    @GET("personalized/newsong") suspend fun newSongs(@Query("limit") limit: Int = 30): NewsongResp
    @GET("toplist") suspend fun toplist(): ToplistResp
    @GET("top/playlist") suspend fun hotPlaylist(
        @Query("cat") cat: String,
        @Query("order") order: String = "hot",
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): HotPlaylistResp
    @GET("playlist/catlist") suspend fun catlist(): CatlistResp

    // 歌单
    @GET("playlist/detail") suspend fun playlistDetail(@Query("id") id: Long): PlaylistDetailResp
    @GET("playlist/track/all") suspend fun playlistTracks(
        @Query("id") id: Long,
        @Query("limit") limit: Int = 1000,
        @Query("offset") offset: Int = 0,
    ): PlaylistTracksResp
    @GET("playlist/subscribe") suspend fun playlistSubscribe(@Query("id") id: Long, @Query("t") t: Int): SimpleResp

    // 歌曲
    @GET("song/detail") suspend fun songDetail(@Query("ids") ids: String): SongDetailResp
    @GET("song/url/v1") suspend fun songUrl(@Query("id") id: Long, @Query("level") level: String): SongUrlResp
    @GET("lyric") suspend fun lyric(@Query("id") id: Long): LyricResp

    // 搜索
    @GET("search") suspend fun search(
        @Query("keywords") keywords: String,
        @Query("type") type: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SearchResp
    @GET("search/suggest") suspend fun suggest(
        @Query("keywords") keywords: String,
        @Query("type") type: String = "mobile",
    ): SuggestResp
    @GET("search/hot") suspend fun hotSearch(): HotSearchResp

    // 歌手 / 专辑 / MV
    @GET("artist") suspend fun artist(@Query("id") id: Long): ArtistResp
    @GET("artist/top/song") suspend fun artistTopSongs(@Query("id") id: Long): ArtistSongsResp
    @GET("artist/album") suspend fun artistAlbums(@Query("id") id: Long, @Query("limit") limit: Int = 30): ArtistAlbumResp
    @GET("album") suspend fun album(@Query("id") id: Long): AlbumResp
    @GET("mv/detail") suspend fun mvDetail(@Query("mvid") id: Long): MvDetailResp
    @GET("mv/url") suspend fun mvUrl(@Query("id") id: Long, @Query("r") r: Int = 1080): MvUrlResp
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

    val service: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("Api 未初始化，请先在设置中配置服务器地址")

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
