using System.Net.Http;
using System.Net;
using System.Text;
using System.Text.Json;
using GlassSuite.Models;

namespace GlassSuite.Services;

/// <summary>API 客户端：HttpClient + CookieContainer + System.Text.Json</summary>
public class ApiService
{
    private const string UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static readonly string[] QualityLevels = { "standard", "higher", "exhigh", "lossless" };
    public static readonly Dictionary<string, string> QualityNames = new()
    {
        ["standard"] = "标准品质",
        ["higher"] = "较高品质",
        ["exhigh"] = "极高品质",
        ["lossless"] = "无损品质",
    };

    private readonly HttpClient _client;
    private readonly CookieContainer _cookies = new();
    private string _baseUrl = "http://localhost:3000";

    public ApiService()
    {
        var handler = new HttpClientHandler { CookieContainer = _cookies, UseCookies = true };
        _client = new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(30) };
        _client.DefaultRequestHeaders.UserAgent.ParseAdd(UA);
        _client.DefaultRequestHeaders.Referrer = new Uri("https://music.163.com/");
    }

    public string BaseUrl => _baseUrl;

    public void SetBaseUrl(string url)
    {
        _baseUrl = url.Trim().TrimEnd('/');
    }

    public void SetCookieString(string cookie)
    {
        _cookies.SetCookies(new Uri(_baseUrl), cookie);
    }

    public string GetCookieString()
    {
        var sb = new StringBuilder();
        foreach (Cookie c in _cookies.GetCookies(new Uri(_baseUrl)))
        {
            sb.Append($"{c.Name}={c.Value}; ");
        }
        return sb.ToString().Trim();
    }

    public void ClearCookies() => _cookies.GetCookies(new Uri(_baseUrl)).ToList().ForEach(c => c.Expired = true);

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        PropertyNameCaseInsensitive = true,
    };

    private async Task<T> GetAsync<T>(string path, Dictionary<string, string>? query = null)
    {
        var url = $"{_baseUrl}/{path}";
        if (query is { Count: > 0 })
        {
            url += "?" + string.Join("&", query.Select(kv => $"{Uri.EscapeDataString(kv.Key)}={Uri.EscapeDataString(kv.Value)}"));
        }
        var resp = await _client.GetAsync(url);
        resp.EnsureSuccessStatusCode();
        var json = await resp.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<T>(json, JsonOpts) ?? throw new InvalidOperationException("空响应");
    }

    // ================= 登录 =================
    public Task<QrKeyResp> QrKey() => GetAsync<QrKeyResp>("login/qr/key");
    public Task<QrCreateResp> QrCreate(string key) => GetAsync<QrCreateResp>("login/qr/create", new() { ["key"] = key, ["qrimg"] = "true" });
    public Task<QrCheckResp> QrCheck(string key) => GetAsync<QrCheckResp>("login/qr/check", new() { ["key"] = key });
    public Task<SimpleResp> CaptchaSent(string phone) => GetAsync<SimpleResp>("captcha/sent", new() { ["phone"] = phone, ["timestamp"] = Now() });
    public Task<LoginResp> LoginCellphone(string phone, string captcha) => GetAsync<LoginResp>("login/cellphone", new() { ["phone"] = phone, ["captcha"] = captcha, ["timestamp"] = Now() });
    public async Task<Profile?> LoginStatus()
    {
        try
        {
            var r = await GetAsync<LoginStatusResp>("login/status");
            return r.Data?.Profile;
        }
        catch
        {
            var r2 = await GetAsync<AccountResp>("user/account");
            return r2.Profile;
        }
    }

    // ================= 用户 =================
    public Task<UserPlaylistResp> UserPlaylist(long uid) => GetAsync<UserPlaylistResp>("user/playlist", new() { ["uid"] = uid.ToString() });
    public Task<LikeListResp> LikeList(long uid) => GetAsync<LikeListResp>("likelist", new() { ["uid"] = uid.ToString() });
    public Task<SimpleResp> Like(long id, bool like) => GetAsync<SimpleResp>("like", new() { ["id"] = id.ToString(), ["like"] = like ? "true" : "false" });

    // ================= 发现 =================
    public Task<RecommendResp> RecommendResource() => GetAsync<RecommendResp>("recommend/resource");
    public Task<DailySongsResp> RecommendSongs() => GetAsync<DailySongsResp>("recommend/songs");
    public Task<NewsongResp> NewSongs() => GetAsync<NewsongResp>("personalized/newsong", new() { ["limit"] = "30" });
    public Task<ToplistResp> Toplist() => GetAsync<ToplistResp>("toplist");
    public Task<HotPlaylistResp> HotPlaylist(string cat) => GetAsync<HotPlaylistResp>("top/playlist", new() { ["cat"] = cat, ["order"] = "hot", ["limit"] = "50" });
    public Task<CatlistResp> Catlist() => GetAsync<CatlistResp>("playlist/catlist");

    // ================= 歌单 =================
    public Task<PlaylistDetailResp> PlaylistDetail(long id) => GetAsync<PlaylistDetailResp>("playlist/detail", new() { ["id"] = id.ToString() });
    public Task<PlaylistTracksResp> PlaylistTracks(long id) => GetAsync<PlaylistTracksResp>("playlist/track/all", new() { ["id"] = id.ToString(), ["limit"] = "1000", ["offset"] = "0" });
    public Task<SimpleResp> PlaylistSubscribe(long id, int t) => GetAsync<SimpleResp>("playlist/subscribe", new() { ["id"] = id.ToString(), ["t"] = t.ToString() });

    // ================= 歌曲 =================
    public Task<SongDetailResp> SongDetail(string ids) => GetAsync<SongDetailResp>("song/detail", new() { ["ids"] = ids });
    public async Task<string?> SongUrl(long id, string level)
    {
        var r = await GetAsync<SongUrlResp>("song/url/v1", new() { ["id"] = id.ToString(), ["level"] = level });
        return r.Data?.FirstOrDefault()?.Url;
    }
    public Task<LyricResp> Lyric(long id) => GetAsync<LyricResp>("lyric", new() { ["id"] = id.ToString() });

    // ================= 搜索 =================
    public Task<SearchResp> Search(string keywords, int type) => GetAsync<SearchResp>("search", new() { ["keywords"] = keywords, ["type"] = type.ToString(), ["limit"] = "50" });
    public Task<SuggestResp> Suggest(string keywords) => GetAsync<SuggestResp>("search/suggest", new() { ["keywords"] = keywords });
    public Task<HotSearchResp> HotSearch() => GetAsync<HotSearchResp>("search/hot");

    // ================= 歌手/专辑/MV =================
    public Task<ArtistResp> Artist(long id) => GetAsync<ArtistResp>("artist", new() { ["id"] = id.ToString() });
    public Task<ArtistAlbumResp> ArtistAlbums(long id) => GetAsync<ArtistAlbumResp>("artist/album", new() { ["id"] = id.ToString(), ["limit"] = "30" });
    public Task<AlbumResp> Album(long id) => GetAsync<AlbumResp>("album", new() { ["id"] = id.ToString() });
    public Task<MvDetailResp> MvDetail(long id) => GetAsync<MvDetailResp>("mv/detail", new() { ["mvid"] = id.ToString() });
    public Task<MvUrlResp> MvUrl(long id) => GetAsync<MvUrlResp>("mv/url", new() { ["id"] = id.ToString(), ["r"] = "1080" });

    private static string Now() => DateTimeOffset.Now.ToUnixTimeMilliseconds().ToString();
}
