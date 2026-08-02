using System.Text.Json.Serialization;

namespace GlassSuite.Models;

public class SimpleResp
{
    [JsonPropertyName("code")] public int Code { get; set; }
    [JsonPropertyName("message")] public string? Message { get; set; }
}

public class Profile
{
    [JsonPropertyName("userId")] public long UserId { get; set; }
    [JsonPropertyName("nickname")] public string? Nickname { get; set; }
    [JsonPropertyName("avatarUrl")] public string? AvatarUrl { get; set; }
    [JsonPropertyName("signature")] public string? Signature { get; set; }
    [JsonPropertyName("backgroundUrl")] public string? BackgroundUrl { get; set; }
    [JsonPropertyName("follows")] public int Follows { get; set; }
    [JsonPropertyName("followeds")] public int Followeds { get; set; }
}

public class QrKeyResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("data")] public QrKeyData? Data { get; set; } }
public class QrKeyData { [JsonPropertyName("unikey")] public string? Unikey { get; set; } }

public class QrCreateResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("data")] public QrCreateData? Data { get; set; } }
public class QrCreateData { [JsonPropertyName("qrimg")] public string? Qrimg { get; set; } [JsonPropertyName("qrurl")] public string? Qrurl { get; set; } }

public class QrCheckResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("message")] public string? Message { get; set; } [JsonPropertyName("cookie")] public string? Cookie { get; set; } }

public class LoginResp
{
    [JsonPropertyName("code")] public int Code { get; set; }
    [JsonPropertyName("message")] public string? Message { get; set; }
    [JsonPropertyName("cookie")] public string? Cookie { get; set; }
    [JsonPropertyName("token")] public string? Token { get; set; }
    [JsonPropertyName("profile")] public Profile? Profile { get; set; }
    [JsonPropertyName("account")] public Account? Account { get; set; }
}

public class Account
{
    [JsonPropertyName("id")] public long Id { get; set; }
    [JsonPropertyName("userName")] public string? UserName { get; set; }
}

public class LoginStatusResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("data")] public LoginStatusData? Data { get; set; } }
public class LoginStatusData { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("account")] public Account? Account { get; set; } [JsonPropertyName("profile")] public Profile? Profile { get; set; } }

public class AccountResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("profile")] public Profile? Profile { get; set; } }

public class Artist
{
    [JsonPropertyName("id")] public long Id { get; set; }
    [JsonPropertyName("name")] public string? Name { get; set; }
    [JsonPropertyName("picUrl")] public string? PicUrl { get; set; }
    [JsonPropertyName("albumSize")] public int? AlbumSize { get; set; }
    [JsonPropertyName("mvSize")] public int? MvSize { get; set; }

    public string ArtistName => Name ?? "";
}

public class Album
{
    [JsonPropertyName("id")] public long Id { get; set; }
    [JsonPropertyName("name")] public string? Name { get; set; }
    [JsonPropertyName("picUrl")] public string? PicUrl { get; set; }
    [JsonPropertyName("publishTime")] public long? PublishTime { get; set; }
    [JsonPropertyName("artist")] public Artist? Artist { get; set; }
    [JsonPropertyName("size")] public int? Size { get; set; }
}

public class Song
{
    [JsonPropertyName("id")] public long Id { get; set; }
    [JsonPropertyName("name")] public string? Name { get; set; }
    [JsonPropertyName("artists")] public List<Artist>? Artists { get; set; }
    [JsonPropertyName("album")] public Album? Album { get; set; }
    [JsonPropertyName("duration")] public long? Duration { get; set; }
    [JsonPropertyName("dt")] public long? Dt { get; set; }
    [JsonPropertyName("mvid")] public long? Mvid { get; set; }
    [JsonPropertyName("mv")] public long? Mv { get; set; }
    [JsonPropertyName("fee")] public int? Fee { get; set; }
    [JsonPropertyName("ar")] public List<Artist>? Ar { get; set; }
    [JsonPropertyName("al")] public Album? Al { get; set; }

    public string ArtistNames => string.Join(" / ", (Artists ?? Ar ?? new List<Artist>()).Select(a => a.Name ?? ""));
    public string? AlbumName => (Album ?? Al)?.Name;
    public string? AlbumPic => (Album ?? Al)?.PicUrl;
    public long SongDuration => Dt ?? Duration ?? 0;
    public string IndexText { get; set; } = "";
    public string ArtistText => ArtistNames;
    public long MvId => Mv ?? Mvid ?? 0;
    public string DurationText => FormatDuration(SongDuration);

    public static string FormatDuration(long ms)
    {
        if (ms <= 0) return "00:00";
        var t = TimeSpan.FromMilliseconds(ms);
        return t.TotalHours >= 1 ? $"{t.Hours}:{t.Minutes:D2}:{t.Seconds:D2}" : $"{t.Minutes:D2}:{t.Seconds:D2}";
    }
}

public class SongDetailResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("songs")] public List<Song>? Songs { get; set; } }

public class SongUrl { [JsonPropertyName("id")] public long Id { get; set; } [JsonPropertyName("url")] public string? Url { get; set; } [JsonPropertyName("level")] public string? Level { get; set; } }
public class SongUrlResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("data")] public List<SongUrl>? Data { get; set; } }

public class LyricText { [JsonPropertyName("lyric")] public string? Lyric { get; set; } }
public class LyricResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("lrc")] public LyricText? Lrc { get; set; } [JsonPropertyName("tlyric")] public LyricText? Tlyric { get; set; } [JsonPropertyName("romalrc")] public LyricText? Romalrc { get; set; } }

public class Creator
{
    [JsonPropertyName("userId")] public long? UserId { get; set; }
    [JsonPropertyName("nickname")] public string? Nickname { get; set; }
    [JsonPropertyName("avatarUrl")] public string? AvatarUrl { get; set; }
}

public class Playlist
{
    [JsonPropertyName("id")] public long Id { get; set; }
    [JsonPropertyName("name")] public string? Name { get; set; }
    [JsonPropertyName("coverImgUrl")] public string? CoverImgUrl { get; set; }
    [JsonPropertyName("playCount")] public long? PlayCount { get; set; }
    [JsonPropertyName("trackCount")] public int? TrackCount { get; set; }
    [JsonPropertyName("description")] public string? Description { get; set; }
    [JsonPropertyName("creator")] public Creator? Creator { get; set; }
    [JsonPropertyName("tracks")] public List<Song>? Tracks { get; set; }
    [JsonPropertyName("userId")] public long? UserId { get; set; }
    [JsonPropertyName("subscribed")] public bool? Subscribed { get; set; }

    public string PlayCountText => FormatCount(PlayCount);
    public string CreatorNickname => Creator?.Nickname ?? "";

    public static string FormatCount(long? count)
    {
        if (count is null or 0) return "";
        var c = count.Value;
        return c >= 100_000_000 ? $"{c / 100_000_000.0:0.0}亿" :
               c >= 10_000 ? $"{c / 10_000.0:0.0}万" : c.ToString();
    }
    [JsonPropertyName("updateTime")] public long? UpdateTime { get; set; }
}

public class UserPlaylistResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("playlist")] public List<Playlist>? Playlist { get; set; } }
public class LikeListResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("ids")] public List<long>? Ids { get; set; } }
public class RecommendResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("recommend")] public List<Playlist>? Recommend { get; set; } }
public class DailySongsResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("data")] public DailySongsData? Data { get; set; } }
public class DailySongsData { [JsonPropertyName("dailySongs")] public List<Song>? DailySongs { get; set; } }
public class NewsongItem { [JsonPropertyName("song")] public Song? Song { get; set; } }
public class NewsongResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("result")] public List<NewsongItem>? Result { get; set; } }

public class ToplistItem
{
    [JsonPropertyName("id")] public long Id { get; set; }
    [JsonPropertyName("name")] public string? Name { get; set; }
    [JsonPropertyName("coverImgUrl")] public string? CoverImgUrl { get; set; }
    [JsonPropertyName("playCount")] public long? PlayCount { get; set; }
    [JsonPropertyName("updateFrequency")] public string? UpdateFrequency { get; set; }
}
public class ToplistResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("list")] public List<ToplistItem>? List { get; set; } }

public class HotPlaylistResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("playlists")] public List<Playlist>? Playlists { get; set; } }
public class CatItem { [JsonPropertyName("name")] public string? Name { get; set; } [JsonPropertyName("category")] public int Category { get; set; } }
public class CatlistResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("sub")] public List<CatItem>? Sub { get; set; } }
public class PlaylistDetailResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("playlist")] public Playlist? Playlist { get; set; } }
public class PlaylistTracksResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("songs")] public List<Song>? Songs { get; set; } }

public class SearchResult
{
    [JsonPropertyName("songs")] public List<Song>? Songs { get; set; }
    [JsonPropertyName("playlists")] public List<Playlist>? Playlists { get; set; }
    [JsonPropertyName("artists")] public List<Artist>? Artists { get; set; }
    [JsonPropertyName("albums")] public List<Album>? Albums { get; set; }
    [JsonPropertyName("mvs")] public List<Mv>? Mvs { get; set; }
}
public class SearchResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("result")] public SearchResult? Result { get; set; } }

public class SuggestResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("result")] public SearchResult? Result { get; set; } }
public class HotWord { [JsonPropertyName("first")] public string? First { get; set; } }
public class HotSearchResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("result")] public HotSearchResult? Result { get; set; } }
public class HotSearchResult { [JsonPropertyName("hots")] public List<HotWord>? Hots { get; set; } }

public class ArtistResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("artist")] public Artist? Artist { get; set; } [JsonPropertyName("hotSongs")] public List<Song>? HotSongs { get; set; } }
public class ArtistAlbumResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("hotAlbums")] public List<Album>? HotAlbums { get; set; } }
public class AlbumResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("album")] public Album? Album { get; set; } [JsonPropertyName("songs")] public List<Song>? Songs { get; set; } }

public class MvDetail { [JsonPropertyName("id")] public long Id { get; set; } [JsonPropertyName("name")] public string? Name { get; set; } [JsonPropertyName("artistName")] public string? ArtistName { get; set; } [JsonPropertyName("duration")] public long? Duration { get; set; } [JsonPropertyName("playCount")] public long? PlayCount { get; set; } [JsonPropertyName("cover")] public string? Cover { get; set; } }
public class MvDetailResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("data")] public MvDetail? Data { get; set; } }
public class MvUrlData { [JsonPropertyName("id")] public long Id { get; set; } [JsonPropertyName("url")] public string? Url { get; set; } }
public class MvUrlResp { [JsonPropertyName("code")] public int Code { get; set; } [JsonPropertyName("data")] public MvUrlData? Data { get; set; } }
public class Mv { [JsonPropertyName("id")] public long Id { get; set; } [JsonPropertyName("name")] public string? Name { get; set; } [JsonPropertyName("artistName")] public string? ArtistName { get; set; } [JsonPropertyName("duration")] public long? Duration { get; set; } [JsonPropertyName("playCount")] public long? PlayCount { get; set; } [JsonPropertyName("cover")] public string? Cover { get; set; } }
