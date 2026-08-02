package com.kun.glasssuite.data

import com.google.gson.annotations.SerializedName

// ===================== 通用 =====================

data class SimpleResp(val code: Int, val message: String?)

data class Profile(
    val userId: Long,
    val nickname: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    val signature: String?,
    @SerializedName("backgroundUrl") val backgroundUrl: String?,
    @SerializedName("eventCount") val eventCount: Int?,
    @SerializedName("follows") val follows: Int?,
    @SerializedName("followeds") val followeds: Int?,
)

data class Account(val id: Long, @SerializedName("userName") val userName: String?)

// ===================== 登录 =====================

data class QrKeyResp(val code: Int, val data: QrKeyData?)
data class QrKeyData(val unikey: String?)

data class QrCreateResp(val code: Int, val data: QrCreateData?)
data class QrCreateData(val qrimg: String?, val qrurl: String?, val url: String?)

data class QrCheckResp(val code: Int, val message: String?, val cookie: String?)

data class LoginResp(
    val code: Int,
    val message: String?,
    val cookie: String?,
    val token: String?,
    val profile: Profile?,
    val account: Account?,
)

data class LoginStatusResp(val code: Int, val data: LoginStatusData?)
data class LoginStatusData(val code: Int, val account: Account?, val profile: Profile?)

data class AccountResp(val code: Int, val account: Account?, val profile: Profile?)

data class UserDetailResp(val code: Int, val profile: Profile?, val level: Int?)

// ===================== 歌曲 / 专辑 / 歌手 =====================

data class Artist(
    val id: Long,
    val name: String?,
    @SerializedName("picUrl") val picUrl: String?,
    @SerializedName("albumSize") val albumSize: Int?,
    @SerializedName("mvSize") val mvSize: Int?,
)

data class Album(
    val id: Long,
    val name: String?,
    @SerializedName("picUrl") val picUrl: String?,
    @SerializedName("publishTime") val publishTime: Long?,
    val artist: Artist?,
    val size: Int?,
)

data class Song(
    val id: Long,
    val name: String?,
    val artists: List<Artist>?,
    val album: Album?,
    val duration: Long?,
    val dt: Long?,
    val mvid: Long?,
    val mv: Long?,
    val fee: Int?,
    val ar: List<Artist>?,
    val al: Album?,
) {
    val artistNames: String
        get() = (artists ?: ar ?: emptyList()).joinToString(" / ") { it.name ?: "" }
    val albumName: String?
        get() = (album ?: al)?.name
    val albumPic: String?
        get() = (album ?: al)?.picUrl
    val songDuration: Long
        get() = dt ?: duration ?: 0L
    val mvId: Long
        get() = mv ?: mvid ?: 0L
}

data class SongDetailResp(val code: Int, val songs: List<Song>?)

data class SongUrl(val id: Long, val url: String?, val br: Int?, val size: Long?, val level: String?, val code: Int?)
data class SongUrlResp(val code: Int, val data: List<SongUrl>?)

data class LyricText(val lyric: String?)
data class LyricResp(
    val code: Int,
    val lrc: LyricText?,
    val tlyric: LyricText?,
    val romalrc: LyricText?,
)

data class ArtistResp(val code: Int, val artist: Artist?, @SerializedName("hotSongs") val hotSongs: List<Song>?)
data class ArtistSongsResp(val code: Int, val songs: List<Song>?)
data class ArtistAlbumResp(val code: Int, @SerializedName("hotAlbums") val hotAlbums: List<Album>?)
data class AlbumResp(val code: Int, val album: Album?, val songs: List<Song>?)

data class MvDetail(
    val id: Long,
    val name: String?,
    @SerializedName("artistName") val artistName: String?,
    val duration: Long?,
    @SerializedName("playCount") val playCount: Long?,
    val cover: String?,
    val artists: List<Artist>?,
)
data class MvDetailResp(val code: Int, val data: MvDetail?)
data class MvUrlData(val id: Long, val url: String?, val r: Int?, val size: Long?)
data class MvUrlResp(val code: Int, val data: MvUrlData?)
data class Mv(val id: Long, val name: String?, @SerializedName("artistName") val artistName: String?, val duration: Long?, @SerializedName("playCount") val playCount: Long?, val cover: String?)

// ===================== 歌单 =====================

data class Creator(val userId: Long?, val nickname: String?, @SerializedName("avatarUrl") val avatarUrl: String?)

data class Playlist(
    val id: Long,
    val name: String?,
    @SerializedName("coverImgUrl") val coverImgUrl: String?,
    @SerializedName("playCount") val playCount: Long?,
    @SerializedName("trackCount") val trackCount: Int?,
    val description: String?,
    val creator: Creator?,
    val tracks: List<Song>?,
    val userId: Long?,
    val subscribed: Boolean?,
    @SerializedName("updateTime") val updateTime: Long?,
    val tags: List<String>?,
)

data class UserPlaylistResp(val code: Int, val playlist: List<Playlist>?)
data class LikeListResp(val code: Int, val ids: List<Long>?)
data class RecommendResp(val code: Int, val recommend: List<Playlist>?)
data class DailySongsResp(val code: Int, val data: DailySongsData?)
data class DailySongsData(@SerializedName("dailySongs") val dailySongs: List<Song>?)
data class NewsongItem(val song: Song?)
data class NewsongResp(val code: Int, val result: List<NewsongItem>?)

data class ToplistItem(
    val id: Long,
    val name: String?,
    @SerializedName("coverImgUrl") val coverImgUrl: String?,
    @SerializedName("playCount") val playCount: Long?,
    @SerializedName("updateFrequency") val updateFrequency: String?,
)
data class ToplistResp(val code: Int, val list: List<ToplistItem>?)

data class HotPlaylistResp(val code: Int, val playlists: List<Playlist>?)
data class CatItem(val name: String?, val category: Int?)
data class CatlistResp(val code: Int, val sub: List<CatItem>?)
data class PlaylistDetailResp(val code: Int, val playlist: Playlist?)
data class PlaylistTracksResp(val code: Int, val songs: List<Song>?)

// ===================== 搜索 =====================

data class SearchResult(
    val songs: List<Song>?,
    val playlists: List<Playlist>?,
    val artists: List<Artist>?,
    val albums: List<Album>?,
    val mvs: List<Mv>?,
)
data class SearchResp(val code: Int, val result: SearchResult?)

data class SuggestResult(
    val songs: List<Song>?,
    val playlists: List<Playlist>?,
    val artists: List<Artist>?,
    val albums: List<Album>?,
)
data class SuggestResp(val code: Int, val result: SuggestResult?)

data class HotWord(val first: String?)
data class HotSearchResult(val hots: List<HotWord>?)
data class HotSearchResp(val code: Int, val result: HotSearchResult?)
