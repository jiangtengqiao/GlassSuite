using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using GlassSuite.Models;

namespace GlassSuite.Services;

/// <summary>
/// 网易云官方接口 weapi/eapi 加密直连实现（无需自托管服务器）。
/// 与 Android 端 NeteaseDirect.kt 同协议：weapi 双重 AES-CBC + RSA-PKCS1，eapi AES-CBC。
/// 所有方法返回与 ApiService（Binaryify 风格）完全一致的数据模型。
/// </summary>
public static class NeteaseDirect
{
    private const string Host = "https://music.163.com";
    private const string Nonce = "0CoJUm6Qyw8W8jud";
    private const string EapiKey = "e82ckenh8dichen8";
    private static readonly byte[] Iv = Encoding.UTF8.GetBytes("0102030405060708");

    private const string ModulusHex =
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b7251" +
        "52b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312e" +
        "cbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d" +
        "813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";

    private const string UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static readonly JsonSerializerOptions JsonOpts = new() { PropertyNameCaseInsensitive = true };
    private static readonly CookieContainer Jar = new();
    private static readonly HttpClient Client = CreateClient();

    private static HttpClient CreateClient()
    {
        var handler = new HttpClientHandler { CookieContainer = Jar, UseCookies = true };
        var c = new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(30) };
        c.DefaultRequestHeaders.UserAgent.ParseAdd(UA);
        c.DefaultRequestHeaders.Referrer = new Uri("https://music.163.com/");
        return c;
    }

    // ===================== 加密核心 =====================

    private static byte[] AesEncrypt(byte[] data, byte[] key)
    {
        using var aes = Aes.Create();
        aes.Mode = CipherMode.CBC;
        aes.Padding = PaddingMode.PKCS7;
        aes.Key = key;
        aes.IV = Iv;
        using var enc = aes.CreateEncryptor();
        return enc.TransformFinalBlock(data, 0, data.Length);
    }

    private static string RsaEncrypt(byte[] data)
    {
        var modulus = Convert.FromHexString(ModulusHex);
        if (modulus[0] == 0) modulus = modulus[1..];
        using var rsa = new RSACryptoServiceProvider();
        rsa.ImportParameters(new RSAParameters { Modulus = modulus, Exponent = new byte[] { 0x01, 0x00, 0x01 } });
        var enc = rsa.Encrypt(data, false); // PKCS1 v1.5
        return Convert.ToHexString(enc).ToLower();
    }

    private static (string Params, string EncSecKey) Weapi(Dictionary<string, object?> body)
    {
        var json = JsonSerializer.Serialize(body);
        var secret = RandomNumberGenerator.GetBytes(16);
        var p1 = AesEncrypt(Encoding.UTF8.GetBytes(json), secret);
        var p2 = AesEncrypt(p1, Encoding.UTF8.GetBytes(Nonce));
        return (Convert.ToBase64String(p2), RsaEncrypt(secret));
    }

    private static string Eapi(string path, object body)
    {
        var text = body is string s ? s : JsonSerializer.Serialize(body);
        var msg = $"nobody{path}use{text}music";
        var enc = AesEncrypt(Encoding.UTF8.GetBytes(msg), Encoding.UTF8.GetBytes(EapiKey));
        return Convert.ToBase64String(enc);
    }

    // ===================== HTTP =====================

    private static string WeapiPath(string path) =>
        path.StartsWith("/api/") ? "/weapi" + path[4..] : path;

    private static string EapiPath(string path) =>
        path.StartsWith("/api/") ? "/eapi" + path[4..] : path;

    private static async Task<JsonDocument> WeapiPostAsync(string path, Dictionary<string, object?> body)
    {
        var (p, es) = Weapi(body);
        var form = new FormUrlEncodedContent(new Dictionary<string, string> { ["params"] = p, ["encSecKey"] = es });
        var resp = await Client.PostAsync(Host + WeapiPath(path), form);
        resp.EnsureSuccessStatusCode();
        var json = await resp.Content.ReadAsStringAsync();
        return JsonDocument.Parse(json);
    }

    private static async Task<JsonDocument> EapiPostAsync(string path, object body)
    {
        var eapiUrl = EapiPath(path);
        var p = Eapi(eapiUrl, body);
        var url = $"{Host}{eapiUrl}?params={Uri.EscapeDataString(p)}";
        var resp = await Client.PostAsync(url, new ByteArrayContent(Array.Empty<byte>()));
        resp.EnsureSuccessStatusCode();
        var json = await resp.Content.ReadAsStringAsync();
        return JsonDocument.Parse(json);
    }

    private static T Deserialize<T>(JsonDocument doc) =>
        JsonSerializer.Deserialize<T>(doc.RootElement.GetRawText(), JsonOpts) ?? throw new InvalidOperationException("空响应");

    // ===================== Cookie 管理 =====================

    public static void SetCookieString(string cookie)
    {
        try { Jar.SetCookies(new Uri(Host), cookie); } catch { /* 忽略非法 cookie */ }
    }

    public static string GetCookieString()
    {
        var sb = new StringBuilder();
        foreach (Cookie c in Jar.GetCookies(new Uri(Host)))
            sb.Append($"{c.Name}={c.Value}; ");
        return sb.ToString().Trim();
    }

    public static void ClearCookies()
    {
        foreach (Cookie c in Jar.GetCookies(new Uri(Host)).ToList()) c.Expired = true;
    }

    // ===================== 登录 =====================

    public static async Task<QrKeyResp> QrKey()
    {
        using var doc = await WeapiPostAsync("/api/login/qrcode/unikey", new() { ["type"] = 1 });
        return Deserialize<QrKeyResp>(doc);
    }

    public static Task<QrCreateResp> QrCreate(string key)
    {
        var url = $"https://music.163.com/login?codekey={key}";
        return Task.FromResult(new QrCreateResp
        {
            Code = 200,
            Data = new QrCreateData { Qrurl = url, Qrimg = QrCode.GeneratePngDataUri(url) },
        });
    }

    public static async Task<QrCheckResp> QrCheck(string key)
    {
        using var doc = await WeapiPostAsync("/api/login/qrcode/client/login", new() { ["key"] = key, ["type"] = 1 });
        return Deserialize<QrCheckResp>(doc); // Cookie 由 Set-Cookie 自动写入 Jar
    }

    public static async Task<LoginResp> LoginCellphone(string phone, string captcha)
    {
        using var doc = await WeapiPostAsync("/api/login/cellphone",
            new() { ["phone"] = phone, ["captcha"] = captcha, ["countrycode"] = "86" });
        return Deserialize<LoginResp>(doc);
    }

    public static async Task<SimpleResp> CaptchaSent(string phone)
    {
        using var doc = await WeapiPostAsync("/api/sms/captcha/sent", new() { ["cellphone"] = phone });
        return Deserialize<SimpleResp>(doc);
    }

    public static async Task<Profile?> LoginStatus()
    {
        using var doc = await WeapiPostAsync("/api/w/nuser/account/get", new Dictionary<string, object?>());
        var root = doc.RootElement;
        if (root.TryGetProperty("profile", out var p) && p.ValueKind == JsonValueKind.Object)
            return JsonSerializer.Deserialize<Profile>(p.GetRawText(), JsonOpts);
        return null;
    }

    // ===================== 用户 =====================

    public static async Task<UserPlaylistResp> UserPlaylist(long uid)
    {
        using var doc = await WeapiPostAsync("/api/user/playlist",
            new() { ["uid"] = uid, ["limit"] = 100, ["offset"] = 0 });
        return Deserialize<UserPlaylistResp>(doc);
    }

    public static async Task<LikeListResp> LikeList(long uid)
    {
        using var doc = await WeapiPostAsync("/api/song/like/get", new() { ["uid"] = uid });
        return Deserialize<LikeListResp>(doc);
    }

    public static async Task<SimpleResp> Like(long id, bool like)
    {
        using var doc = await WeapiPostAsync("/api/song/like",
            new() { ["like"] = like, ["trackId"] = id, ["id"] = id });
        return Deserialize<SimpleResp>(doc);
    }

    // ===================== 发现 =====================

    public static async Task<RecommendResp> RecommendResource()
    {
        using var doc = await WeapiPostAsync("/api/v3/discovery/recommend/resource", new Dictionary<string, object?>());
        return Deserialize<RecommendResp>(doc);
    }

    public static async Task<DailySongsResp> RecommendSongs()
    {
        using var doc = await WeapiPostAsync("/api/v3/discovery/recommend/songs", new Dictionary<string, object?>());
        return Deserialize<DailySongsResp>(doc);
    }

    public static async Task<NewsongResp> NewSongs()
    {
        using var doc = await WeapiPostAsync("/api/personalized/newsong", new() { ["type"] = 0, ["limit"] = 30 });
        return Deserialize<NewsongResp>(doc);
    }

    public static async Task<ToplistResp> Toplist()
    {
        using var doc = await WeapiPostAsync("/api/toplist", new Dictionary<string, object?>());
        return Deserialize<ToplistResp>(doc);
    }

    public static async Task<HotPlaylistResp> HotPlaylist(string cat)
    {
        using var doc = await WeapiPostAsync("/api/top/playlist",
            new() { ["cat"] = cat, ["order"] = "hot", ["limit"] = 50, ["offset"] = 0 });
        return Deserialize<HotPlaylistResp>(doc);
    }

    public static async Task<CatlistResp> Catlist()
    {
        using var doc = await WeapiPostAsync("/api/playlist/catalogue", new Dictionary<string, object?>());
        return Deserialize<CatlistResp>(doc);
    }

    // ===================== 歌单 =====================

    public static async Task<PlaylistDetailResp> PlaylistDetail(long id)
    {
        using var doc = await WeapiPostAsync("/api/v6/playlist/detail", new() { ["id"] = id, ["n"] = 100000 });
        return Deserialize<PlaylistDetailResp>(doc);
    }

    public static async Task<PlaylistTracksResp> PlaylistTracks(long id)
    {
        using var doc = await WeapiPostAsync("/api/playlist/track/all",
            new() { ["id"] = id, ["limit"] = 1000, ["offset"] = 0 });
        return Deserialize<PlaylistTracksResp>(doc);
    }

    public static async Task<SimpleResp> PlaylistSubscribe(long id, int t)
    {
        using var doc = await WeapiPostAsync("/api/playlist/subscribe", new() { ["id"] = id, ["t"] = t });
        return Deserialize<SimpleResp>(doc);
    }

    // ===================== 歌曲 =====================

    public static async Task<SongDetailResp> SongDetail(string ids)
    {
        var c = "[" + string.Join(",", ids.Split(',').Select(i => $"{{\"id\":{i}}}")) + "]";
        using var doc = await WeapiPostAsync("/api/v3/song/detail", new() { ["c"] = c });
        return Deserialize<SongDetailResp>(doc);
    }

    public static async Task<string?> SongUrl(long id, string level)
    {
        using var doc = await EapiPostAsync("/api/song/enhance/player/url/v1",
            new Dictionary<string, object?> { ["ids"] = $"[{id}]", ["level"] = level, ["encodeType"] = "aac" });
        var resp = Deserialize<SongUrlResp>(doc);
        return resp.Data?.FirstOrDefault()?.Url;
    }

    public static async Task<LyricResp> Lyric(long id)
    {
        using var doc = await WeapiPostAsync("/api/song/lyric",
            new() { ["id"] = id, ["lv"] = -1, ["kv"] = -1, ["tv"] = -1, ["rv"] = -1 });
        return Deserialize<LyricResp>(doc);
    }

    // ===================== 搜索 =====================

    public static async Task<SearchResp> Search(string keywords, int type)
    {
        using var doc = await WeapiPostAsync("/api/cloudsearch/pc",
            new() { ["s"] = keywords, ["type"] = type, ["limit"] = 50, ["offset"] = 0 });
        return Deserialize<SearchResp>(doc);
    }

    public static async Task<SuggestResp> Suggest(string keywords)
    {
        using var doc = await WeapiPostAsync("/api/search/suggest/web", new() { ["s"] = keywords });
        return Deserialize<SuggestResp>(doc);
    }

    public static async Task<HotSearchResp> HotSearch()
    {
        using var doc = await WeapiPostAsync("/api/search/hot", new Dictionary<string, object?>());
        return Deserialize<HotSearchResp>(doc);
    }

    // ===================== 歌手/专辑/MV =====================

    public static async Task<ArtistResp> Artist(long id)
    {
        using var doc = await WeapiPostAsync("/api/artist/head/info/get", new() { ["id"] = id });
        var root = doc.RootElement;
        var artist = root.TryGetProperty("data", out var d) && d.ValueKind == JsonValueKind.Object &&
                     d.TryGetProperty("artist", out var a) && a.ValueKind == JsonValueKind.Object
            ? JsonSerializer.Deserialize<Artist>(a.GetRawText(), JsonOpts)
            : null;
        using var top = await WeapiPostAsync("/api/artist/top/song", new() { ["id"] = id, ["top"] = 50 });
        var topResp = Deserialize<ArtistResp>(top);
        return new ArtistResp { Code = artist != null ? 200 : (topResp.Code), Artist = artist, HotSongs = topResp.HotSongs };
    }

    public static async Task<ArtistAlbumResp> ArtistAlbums(long id)
    {
        using var doc = await WeapiPostAsync("/api/artist/albums", new() { ["id"] = id, ["limit"] = 30, ["offset"] = 0 });
        return Deserialize<ArtistAlbumResp>(doc);
    }

    public static async Task<AlbumResp> Album(long id)
    {
        using var doc = await WeapiPostAsync($"/api/v1/album/{id}", new Dictionary<string, object?>());
        return Deserialize<AlbumResp>(doc);
    }

    public static async Task<MvDetailResp> MvDetail(long id)
    {
        using var doc = await WeapiPostAsync("/api/mv/detail", new() { ["id"] = id });
        return Deserialize<MvDetailResp>(doc);
    }

    public static async Task<MvUrlResp> MvUrl(long id)
    {
        using var doc = await WeapiPostAsync("/api/mv/url", new() { ["id"] = id });
        return Deserialize<MvUrlResp>(doc);
    }

    // ===================== 二维码（QRCoder 生成 PNG data URI） =====================

    private static class QrCode
    {
        public static string? GeneratePngDataUri(string text)
        {
            try
            {
                using var qr = QRCoder.QRCodeGenerator.GenerateQrCode(text, QRCoder.QRCodeGenerator.ECCLevel.M);
                var png = new QRCoder.PngByteQRCode(qr).GetGraphic(8);
                return "data:image/png;base64," + Convert.ToBase64String(png);
            }
            catch
            {
                return null;
            }
        }
    }
}
