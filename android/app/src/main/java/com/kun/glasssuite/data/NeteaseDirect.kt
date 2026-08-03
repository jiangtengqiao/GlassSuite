package com.kun.glasssuite.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 网易云官方接口 weapi/eapi 加密直连实现（无需自托管服务器）。
 *
 * 采用与 YesPlayMusic 相同的加密方案：
 * - weapi：双重 AES-CBC（随机密钥 + 固定 nonce）+ RSA 加密随机密钥
 * - eapi：AES-CBC（固定 eapi key），用于播放地址等接口
 *
 * 所有端点直接请求 https://music.163.com，Cookie 由 Api.cookieJar 持久化，
 * 返回的数据结构与 ApiService（Binaryify 风格）完全一致，UI 层零改动。
 */
object NeteaseDirect : MusicApi {

    private const val HOST = "https://music.163.com"
    private const val NONCE = "0CoJUm6Qyw8W8jud"
    private const val EAPI_KEY = "e82ckenh8dichen8"
    private val IV = "0102030405060708".toByteArray(Charsets.UTF_8)

    private val RSA_MODULUS = BigInteger(
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b7251" +
            "52b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312e" +
            "cbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d" +
            "813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7",
        16,
    )
    private val RSA_EXP = BigInteger("010001", 16)

    private val gson = Gson()
    private val random = SecureRandom()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(Api.cookieJar)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", Api.UA)
                    .header("Referer", "https://music.163.com/")
                    .header("Origin", "https://music.163.com")
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ===================== 加密核心 =====================

    private fun aesEncrypt(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(IV))
        return cipher.doFinal(data)
    }

    private fun rsaEncrypt(data: ByteArray): String {
        val pub = KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(RSA_MODULUS, RSA_EXP))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, pub)
        return cipher.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    /** weapi 加密：返回 (params, encSecKey) */
    private fun weapiParams(body: Map<String, Any?>): Pair<String, String> {
        val json = gson.toJson(body)
        val secret = ByteArray(16).also { random.nextBytes(it) }
        val params = aesEncrypt(aesEncrypt(json.toByteArray(Charsets.UTF_8), secret), NONCE.toByteArray(Charsets.UTF_8))
        val encSecKey = rsaEncrypt(secret)
        return Base64.getEncoder().encodeToString(params) to encSecKey
    }

    /** eapi 加密：返回 query 用 params */
    private fun eapiParams(path: String, body: Any): String {
        val text = if (body is String) body else gson.toJson(body)
        val message = "nobody$path use$text music".replace("nobody$path use", "nobody${path}use")
        // 标准格式: nobody{path}use{text}music
        val msg = "nobody$path" + "use" + text + "music"
        val params = aesEncrypt(msg.toByteArray(Charsets.UTF_8), EAPI_KEY.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(params)
    }

    // ===================== HTTP 请求 =====================

    /** weapi 请求路径：/api/xxx → /weapi/xxx（网易云只接受该前缀的加密接口） */
    private fun weapiPath(path: String): String =
        if (path.startsWith("/api/")) "/weapi" + path.substring(4) else path

    /** eapi 请求路径：/api/xxx → /eapi/xxx */
    private fun eapiPath(path: String): String =
        if (path.startsWith("/api/")) "/eapi" + path.substring(4) else path

    private suspend fun weapiPost(path: String, body: Map<String, Any?>): JsonObject =
        withContext(Dispatchers.IO) {
            val (params, encSecKey) = weapiParams(body)
            val form = FormBody.Builder()
                .add("params", params)
                .add("encSecKey", encSecKey)
                .build()
            val req = Request.Builder().url("$HOST${weapiPath(path)}").post(form).build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                JsonParser.parseString(text).asJsonObject
            }
        }

    private suspend fun eapiPost(path: String, body: Any): JsonObject =
        withContext(Dispatchers.IO) {
            val eapiUrl = eapiPath(path)
            val params = eapiParams(eapiUrl, body)
            val url = "$HOST$eapiUrl?params=${java.net.URLEncoder.encode(params, "UTF-8")}"
            val req = Request.Builder().url(url).post(FormBody.Builder().build()).build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                JsonParser.parseString(text).asJsonObject
            }
        }

    // ===================== JSON 映射 =====================

    private fun obj(o: JsonObject, key: String): JsonObject? =
        o.get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun arr(o: JsonObject, key: String): JsonArray? =
        o.get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun str(o: JsonObject, key: String): String? =
        o.get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun lng(o: JsonObject, key: String): Long? =
        o.get(key)?.takeIf { !it.isJsonNull }?.asLong

    private fun int(o: JsonObject, key: String): Int? =
        o.get(key)?.takeIf { !it.isJsonNull }?.asInt

    private fun bool(o: JsonObject, key: String): Boolean? =
        o.get(key)?.takeIf { !it.isJsonNull }?.asBoolean

    private fun toArtist(o: JsonObject): Artist = Artist(
        id = o.get("id")?.asLong ?: 0L,
        name = str(o, "name"),
        picUrl = str(o, "picUrl") ?: str(o, "img1v1Url"),
        albumSize = int(o, "albumSize"),
        mvSize = int(o, "mvSize"),
    )

    private fun toAlbum(o: JsonObject): Album = Album(
        id = o.get("id")?.asLong ?: 0L,
        name = str(o, "name"),
        picUrl = str(o, "picUrl") ?: str(o, "blurPicUrl"),
        publishTime = lng(o, "publishTime"),
        artist = obj(o, "artist")?.let { toArtist(it) },
        size = int(o, "size"),
    )

    private fun toSong(o: JsonObject): Song = Song(
        id = o.get("id")?.asLong ?: 0L,
        name = str(o, "name"),
        artists = arr(o, "artists")?.map { toArtist(it.asJsonObject) },
        album = obj(o, "album")?.let { toAlbum(it) },
        duration = lng(o, "duration"),
        dt = lng(o, "dt"),
        mvid = lng(o, "mvid"),
        mv = lng(o, "mv"),
        fee = int(o, "fee"),
        ar = arr(o, "ar")?.map { toArtist(it.asJsonObject) },
        al = obj(o, "al")?.let { toAlbum(it) },
    )

    private fun toCreator(o: JsonObject): Creator = Creator(
        userId = lng(o, "userId"),
        nickname = str(o, "nickname"),
        avatarUrl = str(o, "avatarUrl"),
    )

    private fun toPlaylist(o: JsonObject): Playlist = Playlist(
        id = o.get("id")?.asLong ?: 0L,
        name = str(o, "name"),
        coverImgUrl = str(o, "coverImgUrl"),
        playCount = lng(o, "playCount"),
        trackCount = int(o, "trackCount"),
        description = str(o, "description"),
        creator = obj(o, "creator")?.let { toCreator(it) },
        tracks = arr(o, "tracks")?.map { toSong(it.asJsonObject) },
        userId = lng(o, "userId"),
        subscribed = bool(o, "subscribed"),
        updateTime = lng(o, "updateTime"),
        tags = arr(o, "tags")?.map { it.asString },
    )

    private fun toProfile(o: JsonObject): Profile = Profile(
        userId = o.get("userId")?.asLong ?: 0L,
        nickname = str(o, "nickname"),
        avatarUrl = str(o, "avatarUrl"),
        signature = str(o, "signature"),
        backgroundUrl = str(o, "backgroundUrl"),
        eventCount = int(o, "eventCount"),
        follows = int(o, "follows"),
        followeds = int(o, "followeds"),
    )

    private fun toAccount(o: JsonObject): Account = Account(
        id = o.get("id")?.asLong ?: 0L,
        userName = str(o, "userName"),
    )

    private fun toMv(o: JsonObject): Mv = Mv(
        id = o.get("id")?.asLong ?: 0L,
        name = str(o, "name"),
        artistName = str(o, "artistName"),
        duration = lng(o, "duration"),
        playCount = lng(o, "playCount"),
        cover = str(o, "cover") ?: str(o, "imgurl") ?: str(o, "imgurl16v9"),
    )

    /** 生成登录二维码（PNG data URI），与 Binaryify qrCreate 输出一致 */
    private fun buildQrImage(url: String): String? = runCatching {
        val size = 360
        val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val paint = android.graphics.Paint().apply { color = Color.BLACK }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (matrix[x, y]) canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray())
    }.getOrNull()

    // ===================== MusicApi 实现 =====================

    override suspend fun qrKey(): QrKeyResp {
        val json = weapiPost("/api/login/qrcode/unikey", mapOf("type" to 1))
        return QrKeyResp(
            code = int(json, "code") ?: -1,
            data = obj(json, "data")?.let { QrKeyData(str(it, "unikey")) },
        )
    }

    override suspend fun qrCreate(key: String, qrimg: Boolean): QrCreateResp {
        val url = "https://music.163.com/login?codekey=$key"
        return QrCreateResp(
            code = 200,
            data = QrCreateData(
                qrimg = if (qrimg) buildQrImage(url) else null,
                qrurl = url,
                url = url,
            ),
        )
    }

    override suspend fun qrCheck(key: String): QrCheckResp {
        val json = weapiPost("/api/login/qrcode/client/login", mapOf("key" to key, "type" to 1))
        return QrCheckResp(
            code = int(json, "code") ?: -1,
            message = str(json, "message"),
            cookie = null, // 登录 Cookie 由 Set-Cookie 自动写入 cookieJar
        )
    }

    override suspend fun loginCellphone(
        phone: String,
        captcha: String,
        timestamp: Long,
    ): LoginResp {
        val json = weapiPost(
            "/api/login/cellphone",
            mapOf("phone" to phone, "captcha" to captcha, "countrycode" to "86"),
        )
        return LoginResp(
            code = int(json, "code") ?: -1,
            message = str(json, "message"),
            cookie = str(json, "cookie"),
            token = str(json, "token"),
            profile = obj(json, "profile")?.let { toProfile(it) },
            account = obj(json, "account")?.let { toAccount(it) },
        )
    }

    override suspend fun captchaSent(phone: String, timestamp: Long): SimpleResp {
        val json = weapiPost("/api/sms/captcha/sent", mapOf("cellphone" to phone))
        return SimpleResp(int(json, "code") ?: -1, str(json, "message"))
    }

    override suspend fun captchaVerify(phone: String, captcha: String, timestamp: Long): SimpleResp {
        val json = weapiPost(
            "/api/sms/captcha/verify",
            mapOf("cellphone" to phone, "captcha" to captcha),
        )
        return SimpleResp(int(json, "code") ?: -1, str(json, "message"))
    }

    override suspend fun loginStatus(): LoginStatusResp {
        val json = weapiPost("/api/w/nuser/account/get", emptyMap())
        val code = int(json, "code") ?: -1
        return LoginStatusResp(
            code = code,
            data = LoginStatusData(
                code = code,
                account = obj(json, "account")?.let { toAccount(it) },
                profile = obj(json, "profile")?.let { toProfile(it) },
            ),
        )
    }

    override suspend fun userAccount(): AccountResp {
        val json = weapiPost("/api/w/nuser/account/get", emptyMap())
        return AccountResp(
            code = int(json, "code") ?: -1,
            account = obj(json, "account")?.let { toAccount(it) },
            profile = obj(json, "profile")?.let { toProfile(it) },
        )
    }

    override suspend fun userDetail(uid: Long): UserDetailResp {
        val json = weapiPost("/api/v1/user/detail/$uid", emptyMap())
        return UserDetailResp(
            code = int(json, "code") ?: -1,
            profile = obj(json, "profile")?.let { toProfile(it) },
            level = int(json, "level"),
        )
    }

    override suspend fun userPlaylist(uid: Long): UserPlaylistResp {
        val json = weapiPost(
            "/api/user/playlist",
            mapOf("uid" to uid, "limit" to 100, "offset" to 0),
        )
        return UserPlaylistResp(
            code = int(json, "code") ?: -1,
            playlist = arr(json, "playlist")?.map { toPlaylist(it.asJsonObject) },
        )
    }

    override suspend fun likeList(uid: Long): LikeListResp {
        val json = weapiPost("/api/song/like/get", mapOf("uid" to uid))
        return LikeListResp(
            code = int(json, "code") ?: -1,
            ids = arr(json, "ids")?.map { it.asLong },
        )
    }

    override suspend fun like(id: Long, like: Boolean): SimpleResp {
        val json = weapiPost(
            "/api/song/like",
            mapOf("like" to like, "trackId" to id, "id" to id),
        )
        return SimpleResp(int(json, "code") ?: -1, str(json, "message"))
    }

    override suspend fun recommendResource(): RecommendResp {
        val json = weapiPost("/api/v3/discovery/recommend/resource", emptyMap())
        return RecommendResp(
            code = int(json, "code") ?: -1,
            recommend = arr(json, "recommend")?.map { toPlaylist(it.asJsonObject) },
        )
    }

    override suspend fun recommendSongs(): DailySongsResp {
        val json = weapiPost("/api/v3/discovery/recommend/songs", emptyMap())
        return DailySongsResp(
            code = int(json, "code") ?: -1,
            data = obj(json, "data")?.let {
                DailySongsData(arr(it, "dailySongs")?.map { s -> toSong(s.asJsonObject) })
            },
        )
    }

    override suspend fun newSongs(limit: Int): NewsongResp {
        val json = weapiPost(
            "/api/personalized/newsong",
            mapOf("type" to 0, "limit" to limit),
        )
        return NewsongResp(
            code = int(json, "code") ?: -1,
            result = arr(json, "result")?.map {
                NewsongItem(obj(it.asJsonObject, "song")?.let { s -> toSong(s) })
            },
        )
    }

    override suspend fun toplist(): ToplistResp {
        val json = weapiPost("/api/toplist", emptyMap())
        return ToplistResp(
            code = int(json, "code") ?: -1,
            list = arr(json, "list")?.map {
                val o = it.asJsonObject
                ToplistItem(
                    id = o.get("id")?.asLong ?: 0L,
                    name = str(o, "name"),
                    coverImgUrl = str(o, "coverImgUrl"),
                    playCount = lng(o, "playCount"),
                    updateFrequency = str(o, "updateFrequency"),
                )
            },
        )
    }

    override suspend fun hotPlaylist(cat: String, order: String, limit: Int, offset: Int): HotPlaylistResp {
        val json = weapiPost(
            "/api/top/playlist",
            mapOf("cat" to cat, "order" to order, "limit" to limit, "offset" to offset),
        )
        return HotPlaylistResp(
            code = int(json, "code") ?: -1,
            playlists = arr(json, "playlists")?.map { toPlaylist(it.asJsonObject) },
        )
    }

    override suspend fun catlist(): CatlistResp {
        val json = weapiPost("/api/playlist/catalogue", emptyMap())
        return CatlistResp(
            code = int(json, "code") ?: -1,
            sub = arr(json, "sub")?.map {
                CatItem(str(it.asJsonObject, "name"), int(it.asJsonObject, "category"))
            },
        )
    }

    override suspend fun playlistDetail(id: Long): PlaylistDetailResp {
        val json = weapiPost(
            "/api/v6/playlist/detail",
            mapOf("id" to id, "n" to 100000),
        )
        return PlaylistDetailResp(
            code = int(json, "code") ?: -1,
            playlist = obj(json, "playlist")?.let { toPlaylist(it) },
        )
    }

    override suspend fun playlistTracks(id: Long, limit: Int, offset: Int): PlaylistTracksResp {
        val json = weapiPost(
            "/api/playlist/track/all",
            mapOf("id" to id, "limit" to limit, "offset" to offset),
        )
        return PlaylistTracksResp(
            code = int(json, "code") ?: -1,
            songs = arr(json, "songs")?.map { toSong(it.asJsonObject) },
        )
    }

    override suspend fun playlistSubscribe(id: Long, t: Int): SimpleResp {
        val json = weapiPost("/api/playlist/subscribe", mapOf("id" to id, "t" to t))
        return SimpleResp(int(json, "code") ?: -1, str(json, "message"))
    }

    override suspend fun songDetail(ids: String): SongDetailResp {
        val c = ids.split(",").joinToString(prefix = "[", postfix = "]") { "{\"id\":$it}" }
        val json = weapiPost("/api/v3/song/detail", mapOf("c" to c))
        return SongDetailResp(
            code = int(json, "code") ?: -1,
            songs = arr(json, "songs")?.map { toSong(it.asJsonObject) },
        )
    }

    override suspend fun songUrl(id: Long, level: String): SongUrlResp {
        val json = eapiPost(
            "/api/song/enhance/player/url/v1",
            mapOf("ids" to "[$id]", "level" to level, "encodeType" to "aac"),
        )
        return SongUrlResp(
            code = int(json, "code") ?: -1,
            data = arr(json, "data")?.map {
                val o = it.asJsonObject
                SongUrl(
                    id = o.get("id")?.asLong ?: 0L,
                    url = str(o, "url"),
                    br = int(o, "br"),
                    size = lng(o, "size"),
                    level = str(o, "level"),
                    code = int(o, "code"),
                )
            },
        )
    }

    override suspend fun lyric(id: Long): LyricResp {
        val json = weapiPost(
            "/api/song/lyric",
            mapOf("id" to id, "lv" to -1, "kv" to -1, "tv" to -1, "rv" to -1),
        )
        fun lyricOf(key: String): LyricText? = obj(json, key)?.let { LyricText(str(it, "lyric")) }
        return LyricResp(
            code = int(json, "code") ?: -1,
            lrc = lyricOf("lrc"),
            tlyric = lyricOf("tlyric"),
            romalrc = lyricOf("romalrc"),
        )
    }

    override suspend fun search(keywords: String, type: Int, limit: Int, offset: Int): SearchResp {
        val json = weapiPost(
            "/api/cloudsearch/pc",
            mapOf("s" to keywords, "type" to type, "limit" to limit, "offset" to offset),
        )
        val result = obj(json, "result")
        return SearchResp(
            code = int(json, "code") ?: -1,
            result = result?.let {
                SearchResult(
                    songs = arr(it, "songs")?.map { s -> toSong(s.asJsonObject) },
                    playlists = arr(it, "playlists")?.map { p -> toPlaylist(p.asJsonObject) },
                    artists = arr(it, "artists")?.map { a -> toArtist(a.asJsonObject) },
                    albums = arr(it, "albums")?.map { a -> toAlbum(a.asJsonObject) },
                    mvs = arr(it, "mvs")?.map { m -> toMv(m.asJsonObject) },
                )
            },
        )
    }

    override suspend fun suggest(keywords: String, type: String): SuggestResp {
        val json = weapiPost("/api/search/suggest/web", mapOf("s" to keywords))
        val result = obj(json, "result")
        return SuggestResp(
            code = int(json, "code") ?: -1,
            result = result?.let {
                SuggestResult(
                    songs = arr(it, "songs")?.map { s -> toSong(s.asJsonObject) },
                    playlists = arr(it, "playlists")?.map { p -> toPlaylist(p.asJsonObject) },
                    artists = arr(it, "artists")?.map { a -> toArtist(a.asJsonObject) },
                    albums = arr(it, "albums")?.map { a -> toAlbum(a.asJsonObject) },
                )
            },
        )
    }

    override suspend fun hotSearch(): HotSearchResp {
        val json = weapiPost("/api/search/hot", emptyMap())
        val result = obj(json, "result")
        return HotSearchResp(
            code = int(json, "code") ?: -1,
            result = result?.let {
                HotSearchResult(arr(it, "hots")?.map { h -> HotWord(str(h.asJsonObject, "first")) })
            },
        )
    }

    override suspend fun artist(id: Long): ArtistResp {
        val json = weapiPost("/api/artist/head/info/get", mapOf("id" to id))
        val artistObj = obj(json, "data")?.let { obj(it, "artist") } ?: obj(json, "artist")
        // 热门歌曲单独拉取，合并为 Binaryify /artist 同构响应
        val topJson = weapiPost("/api/artist/top/song", mapOf("id" to id, "top" to 50))
        return ArtistResp(
            code = int(json, "code") ?: -1,
            artist = artistObj?.let { toArtist(it) },
            hotSongs = arr(topJson, "songs")?.map { toSong(it.asJsonObject) },
        )
    }

    override suspend fun artistTopSongs(id: Long): ArtistSongsResp {
        val json = weapiPost("/api/artist/top/song", mapOf("id" to id, "top" to 50))
        return ArtistSongsResp(
            code = int(json, "code") ?: -1,
            songs = arr(json, "songs")?.map { toSong(it.asJsonObject) },
        )
    }

    override suspend fun artistAlbums(id: Long, limit: Int): ArtistAlbumResp {
        val json = weapiPost(
            "/api/artist/albums",
            mapOf("id" to id, "limit" to limit, "offset" to 0),
        )
        return ArtistAlbumResp(
            code = int(json, "code") ?: -1,
            hotAlbums = arr(json, "hotAlbums")?.map { toAlbum(it.asJsonObject) },
        )
    }

    override suspend fun album(id: Long): AlbumResp {
        val json = weapiPost("/api/v1/album/$id", emptyMap())
        return AlbumResp(
            code = int(json, "code") ?: -1,
            album = obj(json, "album")?.let { toAlbum(it) },
            songs = arr(json, "songs")?.map { toSong(it.asJsonObject) },
        )
    }

    override suspend fun mvDetail(id: Long): MvDetailResp {
        val json = weapiPost("/api/mv/detail", mapOf("id" to id))
        val data = obj(json, "data")
        return MvDetailResp(
            code = int(json, "code") ?: -1,
            data = data?.let {
                MvDetail(
                    id = it.get("id")?.asLong ?: 0L,
                    name = str(it, "name"),
                    artistName = str(it, "artistName"),
                    duration = lng(it, "duration"),
                    playCount = lng(it, "playCount"),
                    cover = str(it, "cover") ?: str(it, "imgurl16v9"),
                    artists = arr(it, "artists")?.map { a -> toArtist(a.asJsonObject) },
                )
            },
        )
    }

    override suspend fun mvUrl(id: Long, r: Int): MvUrlResp {
        val json = weapiPost("/api/mv/url", mapOf("id" to id))
        val data = obj(json, "data")
        return MvUrlResp(
            code = int(json, "code") ?: -1,
            data = data?.let {
                MvUrlData(
                    id = it.get("id")?.asLong ?: 0L,
                    url = str(it, "url"),
                    r = int(it, "r"),
                    size = lng(it, "size"),
                )
            },
        )
    }
}
