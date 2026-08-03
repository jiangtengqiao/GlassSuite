using System.Net.Http;
using System.Collections.ObjectModel;
using System.Text.Json;
using System.Windows.Threading;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using GlassSuite.Models;
using GlassSuite.Services;

namespace GlassSuite.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly ApiService _api;
    private readonly PlayerService _player;
    private readonly SettingsService _settingsSvc;
    private readonly SmtcService _smtc;
    private readonly DispatcherTimer _timer;
    private readonly GitHubService _github;
    private AppSettings _settings;

    public MainViewModel(ApiService api, PlayerService player, SettingsService settingsSvc, SmtcService smtc)
    {
        _api = api;
        _player = player;
        _github = new GitHubService();
        _settingsSvc = settingsSvc;
        _smtc = smtc;
        _settings = _settingsSvc.Load();

        _api.SetBaseUrl(_settings.ApiBaseUrl);
        _api.SetCookieString(_settings.Cookie);
        _api.DirectMode = _settings.DirectMode;
        ApiUrl = _settings.ApiBaseUrl;
        DirectMode = _settings.DirectMode;
        // 错误上报通道注入
        ErrorReporter.BetaServerUrl = _settings.BetaServerUrl;
        ErrorReporter.ApiBaseUrl = _settings.ApiBaseUrl;
        AccentHex = _settings.AccentHex;
        DarkMode = _settings.DarkMode;
        Quality = _settings.Quality;
        LyricMode = _settings.LyricMode;
        LyricFontSize = _settings.LyricFontSize;
        LyricOffset = _settings.LyricOffsetMs;
        DiyLyric = _settings.DiyLyric;
        UserId = _settings.UserId;
        ProfileJson = _settings.ProfileJson;
        BetaServerUrl = _settings.BetaServerUrl;
        BetaKey = _settings.BetaKey;
        BetaAccess = !string.IsNullOrEmpty(_settings.BetaKey);
        BetaTier = _settings.BetaTier;

        if (UserId > 0)
        {
            RestoreUser();
        }

        _player.TimeChanged += (_, ms) => Position = ms;
        _player.LengthChanged += (_, len) => Duration = len;
        _player.Playing += (_, _) => IsPlaying = true;
        _player.Paused += (_, _) => IsPlaying = false;
        _player.EndReached += (_, _) => Next();
        _player.Error += (_, _) => Status = "播放失败，尝试降档…";

        _timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(400) };
        _timer.Tick += (_, _) => OnTick();
        _timer.Start();
    }

    // ==================== 属性 ====================
    [ObservableProperty] private Profile? _user;
    [ObservableProperty] private bool _isLoggedIn;
    [ObservableProperty] private bool _directMode = true;
    [ObservableProperty] private long _userId;
    [ObservableProperty] private string _profileJson = "";

    [ObservableProperty] private string _qrImage = "";
    [ObservableProperty] private string _qrStatus = "正在获取二维码…";
    [ObservableProperty] private string _phone = "";
    [ObservableProperty] private string _smsCode = "";
    [ObservableProperty] private string _smsStatus = "";
    [ObservableProperty] private int _countdown;

    [ObservableProperty] private Song? _currentSong;
    [ObservableProperty] private bool _isPlaying;
    [ObservableProperty] private long _position;
    [ObservableProperty] private long _duration;
    [ObservableProperty] private string _quality;
    [ObservableProperty] private bool _repeatOne;
    [ObservableProperty] private bool _shuffle;
    [ObservableProperty] private bool _isLiked;
    [ObservableProperty] private ObservableCollection<Song> _queue = new();
    [ObservableProperty] private int _queueIndex = -1;

    [ObservableProperty] private List<LrcLine> _lyrics = new();
    [ObservableProperty] private List<LrcLine> _tlyrics = new();
    [ObservableProperty] private List<LrcLine> _romalrcs = new();
    [ObservableProperty] private int _lyricMode;
    [ObservableProperty] private int _lyricFontSize;
    [ObservableProperty] private int _lyricOffset;
    [ObservableProperty] private string _diyLyric = "";
    [ObservableProperty] private int _currentLyricIndex = -1;
    [ObservableProperty] private string _translation = "";

    [ObservableProperty] private List<Song> _dailySongs = new();
    [ObservableProperty] private List<Playlist> _recommend = new();
    [ObservableProperty] private List<Song> _newsongs = new();
    [ObservableProperty] private List<ToplistItem> _toplists = new();
    [ObservableProperty] private List<string> _cats = new() { "全部", "华语", "流行", "摇滚", "民谣", "电子", "轻音乐", "影视原声", "ACG", "怀旧" };
    [ObservableProperty] private string _selectedCat = "全部";
    [ObservableProperty] private List<Playlist> _square = new();

    [ObservableProperty] private string _query = "";
    [ObservableProperty] private List<string> _hotWords = new();
    [ObservableProperty] private List<Song> _searchSongs = new();
    [ObservableProperty] private List<Playlist> _searchPlaylists = new();
    [ObservableProperty] private List<Artist> _searchArtists = new();
    [ObservableProperty] private List<Album> _searchAlbums = new();
    [ObservableProperty] private List<Mv> _searchMvs = new();

    [ObservableProperty] private Playlist? _currentPlaylist;
    [ObservableProperty] private ObservableCollection<Song> _playlistSongs = new();
    [ObservableProperty] private List<Playlist> _userPlaylists = new();
    [ObservableProperty] private List<Song> _likedSongs = new();

    [ObservableProperty] private Song? _detailSong;
    [ObservableProperty] private Artist? _detailArtist;
    [ObservableProperty] private List<Song> _detailArtistSongs = new();
    [ObservableProperty] private List<Album> _detailAlbums = new();
    [ObservableProperty] private Album? _detailAlbum;
    [ObservableProperty] private List<Song> _detailAlbumSongs = new();

    [ObservableProperty] private string _status = "";
    [ObservableProperty] private string _apiUrl = "";
    [ObservableProperty] private string _accentHex = "#C62F2F";
    [ObservableProperty] private bool _darkMode;
    [ObservableProperty] private bool _lyricPanelVisible;
    [ObservableProperty] private string _windowTitle = "璃光 GlassSuite";
    [ObservableProperty] private int _searchTab;

    // ===== 多功能模块：GitHub / 更新 / 公告 =====
    [ObservableProperty] private List<GitHubRepo> _gitRepos = new();
    [ObservableProperty] private List<GitHubRelease> _releases = new();
    [ObservableProperty] private List<AnnouncementItem> _announcements = new();
    [ObservableProperty] private bool _hasUpdate;
    [ObservableProperty] private string _betaServerUrl = "http://localhost:3100";
    [ObservableProperty] private string _betaKey = "";
    [ObservableProperty] private bool _betaAccess;
    [ObservableProperty] private int _betaTier;

    public string BetaTierName => BetaTier switch
    {
        3 => "开发者核心",
        2 => "Alpha 内测",
        1 => "Beta 尝鲜",
        _ => "正式用户",
    };
    [ObservableProperty] private string _gitHubQuery = "";
    [ObservableProperty] private string _gitHubStatus = "";
    [ObservableProperty] private List<ReleaseRow> _releaseRows = new();

    /// <summary>导航请求（由主窗口订阅切换视图）</summary>
    public event Action<string>? NavigateRequested;

    private List<long> _likedIds = new();
    private string _qrKey = "";
    private bool _qrBusy;
    private readonly Dictionary<long, string> _transMap = new();

    public static readonly string[] AccentPresets = { "#EC4141", "#C62F2F", "#00B578", "#3D7FFF", "#7B5CFF", "#FF8000" };
    public static readonly string[] AccentNames = { "网易红", "绯红", "薄荷绿", "海盐蓝", "暗夜紫", "琥珀橙" };

    // ==================== 初始化 / 设置 ====================

    private void RestoreUser()
    {
        try
        {
            var p = JsonSerializer.Deserialize<Profile>(ProfileJson);
            if (p is { UserId: > 0 })
            {
                User = p;
                IsLoggedIn = true;
                _ = LoadUserDataAsync();
            }
        }
        catch
        {
        }
    }

    public void SaveSettings()
    {
        _settings.ApiBaseUrl = ApiUrl;
        _settings.DirectMode = DirectMode;
        _settings.AccentHex = AccentHex;
        _settings.DarkMode = DarkMode;
        _settings.LyricFontSize = LyricFontSize;
        _settings.LyricOffsetMs = LyricOffset;
        _settings.LyricMode = LyricMode;
        _settings.DiyLyric = DiyLyric;
        _settings.Quality = Quality;
        _settings.UserId = UserId;
        _settings.ProfileJson = ProfileJson;
        _settings.BetaServerUrl = BetaServerUrl;
        _settings.BetaKey = BetaKey;
        _settings.BetaTier = BetaTier;
        _settings.Cookie = _api.GetCookieString();
        _settingsSvc.Save(_settings);
    }

    [RelayCommand]
    public void SaveApiUrl()
    {
        var url = ApiUrl.Trim().TrimEnd('/');
        if (url.Length == 0) { Status = "地址不能为空"; return; }
        _api.SetBaseUrl(url);
        SaveSettings();
        Status = $"服务器地址已保存：{url}";
    }

    [RelayCommand]
    public void SetDirectMode(bool direct)
    {
        DirectMode = direct;
        _api.DirectMode = direct;
        SaveSettings();
        Status = direct
            ? "已切换：自动直连网易云（weapi/eapi 加密直连官方接口）"
            : "已切换：自托管服务器模式（请在下方配置服务器地址）";
    }

    [RelayCommand]
    public void SetAccent(string hex)
    {
        AccentHex = hex;
        SaveSettings();
        Status = "主题色已更新";
    }

    partial void OnDarkModeChanged(bool value)
    {
        SaveSettings();
    }

    partial void OnLyricModeChanged(int value) => SaveSettings();

    public string LyricOffsetText => $"{LyricOffset / 1000.0:0.0}s";

    partial void OnLyricOffsetChanged(int value) => OnPropertyChanged(nameof(LyricOffsetText));

    // ==================== 登录 ====================

    public void StartQr()
    {
        _ = QrLoopAsync();
    }

    private async Task QrLoopAsync()
    {
        while (true)
        {
            if (_qrBusy) { await Task.Delay(1500); continue; }
            _qrBusy = true;
            try
            {
                if (string.IsNullOrEmpty(_qrKey))
                {
                    var key = await _api.QrKey();
                    _qrKey = key.Data?.Unikey ?? "";
                    if (_qrKey.Length == 0)
                    {
                        QrStatus = "获取二维码失败，请检查服务器地址";
                    }
                    else
                    {
                        var qr = await _api.QrCreate(_qrKey);
                        QrImage = qr.Data?.Qrimg ?? "";
                        QrStatus = "请使用网易云音乐 App 扫码登录";
                    }
                }
                else
                {
                    var check = await _api.QrCheck(_qrKey);
                    switch (check.Code)
                    {
                        case 800: _qrKey = ""; QrStatus = "二维码已过期，正在刷新…"; break;
                        case 801: QrStatus = "等待扫码…"; break;
                        case 802: QrStatus = "已扫码，请在手机端确认登录"; break;
                        case 803:
                            _api.SetCookieString(check.Cookie ?? "");
                            var p = await _api.LoginStatus();
                            if (p is { UserId: > 0 }) { CompleteLogin(p); return; }
                            QrStatus = "登录态获取失败，重试中…";
                            break;
                    }
                }
            }
            catch
            {
                QrStatus = "网络异常，请检查服务器地址";
            }
            _qrBusy = false;
            await Task.Delay(1500);
        }
    }

    private void CompleteLogin(Profile p)
    {
        User = p;
        UserId = p.UserId;
        ProfileJson = JsonSerializer.Serialize(p);
        IsLoggedIn = true;
        SaveSettings();
        Status = "登录成功";
        _ = LoadUserDataAsync();
    }

    [RelayCommand]
    public async Task SendCodeAsync()
    {
        if (Phone.Length != 11) { SmsStatus = "请输入 11 位手机号"; return; }
        var r = await _api.CaptchaSent(Phone);
        SmsStatus = r.Code == 200 ? "验证码已发送" : "发送失败，请稍后重试";
        if (r.Code == 200) Countdown = 60;
    }

    [RelayCommand]
    public async Task SmsLoginAsync()
    {
        if (Phone.Length != 11) { SmsStatus = "请输入 11 位手机号"; return; }
        if (SmsCode.Length < 4) { SmsStatus = "请输入验证码"; return; }
        var r = await _api.LoginCellphone(Phone, SmsCode);
        if (r.Code == 200)
        {
            _api.SetCookieString(r.Cookie ?? "");
            if (r.Profile is { UserId: > 0 }) CompleteLogin(r.Profile);
            else { var p = await _api.LoginStatus(); if (p != null) CompleteLogin(p); }
        }
        else
        {
            SmsStatus = r.Message ?? "登录失败（验证码错误或手机号未注册）";
        }
    }

    [RelayCommand]
    public void Logout()
    {
        _api.ClearCookies();
        User = null;
        UserId = 0;
        ProfileJson = "";
        IsLoggedIn = false;
        SaveSettings();
        Status = "已退出登录";
        _ = _smtc.ClearAsync();
    }

    public async Task LoadUserDataAsync()
    {
        try
        {
            var list = await _api.UserPlaylist(UserId);
            UserPlaylists = list.Playlist ?? new();
            var liked = await _api.LikeList(UserId);
            _likedIds = liked.Ids ?? new();
            IsLiked = CurrentSong != null && _likedIds.Contains(CurrentSong.Id);
        }
        catch
        {
        }
    }

    // ==================== 发现 ====================

    public async Task LoadHomeAsync()
    {
        try
        {
            if (IsLoggedIn)
            {
                var daily = await _api.RecommendSongs();
                if (daily.Data?.DailySongs is { Count: > 0 }) DailySongs = daily.Data.DailySongs;
            }
            var rec = await _api.RecommendResource();
            if (rec.Recommend is { Count: > 0 }) Recommend = rec.Recommend.Take(12).ToList();
            var news = await _api.NewSongs();
            Newsongs = news.Result?.Select(i => i.Song).Where(s => s != null).Cast<Song>().ToList() ?? new();
        }
        catch
        {
            Status = "加载失败，请检查服务器地址";
        }
    }

    public async Task LoadToplistsAsync()
    {
        try
        {
            var r = await _api.Toplist();
            Toplists = r.List ?? new();
        }
        catch
        {
        }
    }

    public async Task LoadSquareAsync()
    {
        try
        {
            var r = await _api.HotPlaylist(SelectedCat);
            Square = r.Playlists ?? new();
        }
        catch
        {
        }
    }

    [RelayCommand]
    public async Task SelectCatAsync(string cat)
    {
        SelectedCat = cat;
        await LoadSquareAsync();
    }

    // ==================== 播放 ====================

    public void PlaySongs(List<Song> songs, int start)
    {
        if (songs.Count == 0) return;
        Queue = new ObservableCollection<Song>(songs);
        QueueIndex = start;
        LoadIndex(start);
    }

    public void PlaySong(Song song) => PlaySongs(new List<Song> { song }, 0);

    [RelayCommand]
    private void OpenPlaylist(long id) => _ = OpenPlaylistAsync(id);

    [RelayCommand]
    private void PlaySongCommand(Song song) => PlaySong(song);

    private async void LoadIndex(int i, long resumePos = -1)
    {
        if (Queue.Count == 0 || i < 0 || i >= Queue.Count) return;
        QueueIndex = i;
        CurrentSong = Queue[i];
        IsPlaying = false;
        IsLiked = _likedIds.Contains(CurrentSong.Id);

        var song = CurrentSong;
        var url = await ResolveUrlAsync(song.Id);
        if (string.IsNullOrEmpty(url))
        {
            Status = $"「{song.Name}」暂无可用音源（可能受版权或会员限制）";
            await Task.Delay(1200);
            if (ReferenceEquals(CurrentSong, song)) Next();
            return;
        }
        _player.PlayUrl(url);
        if (resumePos > 0)
        {
            await Task.Delay(300);
            _player.Seek(resumePos);
        }
        _ = LoadLyricAsync(song.Id);
        WindowTitle = $"♪ {song.Name} - {song.ArtistNames}";
    }

    private async Task<string?> ResolveUrlAsync(long id)
    {
        var levels = ApiService.QualityLevels;
        var start = Math.Max(0, Array.IndexOf(levels, Quality));
        for (var idx = start; idx >= 0; idx--)
        {
            try
            {
                var url = await _api.SongUrl(id, levels[idx]);
                if (!string.IsNullOrEmpty(url)) return url;
            }
            catch
            {
            }
        }
        return null;
    }

    private async Task LoadLyricAsync(long id)
    {
        try
        {
            var r = await _api.Lyric(id);
            Lyrics = LrcService.Parse(r.Lrc?.Lyric);
            Tlyrics = LrcService.Parse(r.Tlyric?.Lyric);
            Romalrcs = LrcService.Parse(r.Romalrc?.Lyric);
            _transMap.Clear();
            foreach (var kv in LrcService.Align(Tlyrics, Lyrics)) _transMap[kv.Key] = kv.Value;
        }
        catch
        {
            Lyrics = new(); Tlyrics = new(); Romalrcs = new();
        }
    }

    [RelayCommand]
    public void Toggle() => _player.Toggle();

    [RelayCommand]
    public void Next()
    {
        if (Queue.Count == 0) return;
        var target = Shuffle
            ? Queue.Where((_, idx) => idx != QueueIndex).Select((_, idx) => idx).OrderBy(_ => Guid.NewGuid()).FirstOrDefault()
            : (QueueIndex + 1 >= Queue.Count ? 0 : QueueIndex + 1);
        LoadIndex(target);
    }

    [RelayCommand]
    public void Prev()
    {
        if (Queue.Count == 0) return;
        var target = QueueIndex - 1 < 0 ? Queue.Count - 1 : QueueIndex - 1;
        LoadIndex(target);
    }

    [RelayCommand]
    public void Seek(long ms) => _player.Seek(ms);

    [RelayCommand]
    public void SetQuality(string level)
    {
        if (Quality == level) return;
        Quality = level;
        SaveSettings();
        if (CurrentSong != null)
        {
            var pos = Position;
            LoadIndex(QueueIndex, pos);
        }
    }

    [RelayCommand]
    public void ToggleRepeatOne()
    {
        RepeatOne = !RepeatOne;
        if (RepeatOne) Shuffle = false;
    }

    [RelayCommand]
    public void ToggleShuffle()
    {
        Shuffle = !Shuffle;
        if (Shuffle) RepeatOne = false;
    }

    [RelayCommand]
    public async Task ToggleLikeAsync()
    {
        if (CurrentSong == null) return;
        var liked = IsLiked;
        var r = await _api.Like(CurrentSong.Id, !liked);
        if (r.Code == 200)
        {
            if (liked) _likedIds.Remove(CurrentSong.Id);
            else _likedIds.Add(CurrentSong.Id);
            IsLiked = !liked;
            Status = IsLiked ? "已喜欢" : "已取消喜欢";
        }
        else Status = "操作失败，请确认已登录";
    }

    public void PlayQueueAt(int index)
    {
        if (index >= 0 && index < Queue.Count) LoadIndex(index);
    }

    // ==================== 歌词设置 ====================

    [RelayCommand]
    public void AdjustOffset(int delta)
    {
        LyricOffset = Math.Clamp(LyricOffset + delta, -10000, 10000);
        SaveSettings();
    }

    [RelayCommand]
    public void ResetOffset()
    {
        LyricOffset = 0;
        SaveSettings();
    }

    [RelayCommand]
    public void AdjustFontSize(int delta)
    {
        LyricFontSize = Math.Clamp(LyricFontSize + delta, 12, 32);
        SaveSettings();
    }

    [RelayCommand]
    public void SetLyricMode(int mode)
    {
        LyricMode = mode;
        SaveSettings();
        CurrentLyricIndex = -1;
    }

    [RelayCommand]
    public void SaveDiy()
    {
        SaveSettings();
        Status = "DIY 歌词已保存";
    }

    private void OnTick()
    {
        if (_player.IsPlaying) Position = _player.GetTime();
        var len = _player.GetLength();
        if (len > 0) Duration = len;

        var lines = LyricMode switch
        {
            1 => Lyrics,
            2 => Romalrcs.Count > 0 ? Romalrcs : Lyrics,
            3 => LrcService.Parse(DiyLyric).Count > 0 ? LrcService.Parse(DiyLyric) : Lyrics,
            _ => Lyrics,
        };
        if (lines.Count == 0)
        {
            CurrentLyricIndex = -1;
            Translation = "";
            return;
        }
        var adj = Position + LyricOffset;
        var idx = -1;
        for (var i = 0; i < lines.Count; i++)
        {
            if (lines[i].Time <= adj) idx = i;
            else break;
        }
        if (idx != CurrentLyricIndex)
        {
            CurrentLyricIndex = idx;
            Translation = idx >= 0 && _transMap.TryGetValue(lines[idx].Time, out var t) ? t : "";
        }
        _ = _smtc.UpdateAsync(CurrentSong?.Name ?? "", CurrentSong?.ArtistNames ?? "", CurrentSong?.AlbumName ?? "",
            _player.IsPlaying, Position, Duration);
    }

    // ==================== 搜索 ====================

    public async Task LoadHotAsync()
    {
        try
        {
            var r = await _api.HotSearch();
            HotWords = r.Result?.Hots?.Select(h => h.First).Where(s => !string.IsNullOrEmpty(s)).Cast<string>().ToList() ?? new();
        }
        catch
        {
        }
    }

    [RelayCommand]
    public async Task SearchAsync()
    {
        if (string.IsNullOrWhiteSpace(Query)) return;
        try
        {
            var songs = await _api.Search(Query, 1);
            SearchSongs = songs.Result?.Songs ?? new();
            var pl = await _api.Search(Query, 1000);
            SearchPlaylists = pl.Result?.Playlists ?? new();
            var ar = await _api.Search(Query, 100);
            SearchArtists = ar.Result?.Artists ?? new();
            var al = await _api.Search(Query, 10);
            SearchAlbums = al.Result?.Albums ?? new();
            var mv = await _api.Search(Query, 1004);
            SearchMvs = mv.Result?.Mvs ?? new();
        }
        catch
        {
            Status = "搜索失败，请检查服务器地址";
        }
    }

    [RelayCommand]
    public void SearchHot(string word)
    {
        Query = word;
        _ = SearchAsync();
    }

    // ==================== 歌单 / 详情 ====================

    public async Task OpenPlaylistAsync(long id)
    {
        try
        {
            var d = await _api.PlaylistDetail(id);
            CurrentPlaylist = d.Playlist;
            var t = await _api.PlaylistTracks(id);
            PlaylistSongs = new ObservableCollection<Song>(t.Songs ?? new());
        }
        catch
        {
            Status = "歌单加载失败";
        }
    }

    [RelayCommand]
    public async Task SubscribePlaylistAsync()
    {
        if (CurrentPlaylist == null) return;
        var t = CurrentPlaylist.Subscribed == true ? 0 : 1;
        var r = await _api.PlaylistSubscribe(CurrentPlaylist.Id, t);
        if (r.Code == 200)
        {
            CurrentPlaylist.Subscribed = t == 1;
            Status = CurrentPlaylist.Subscribed == true ? "已收藏歌单" : "已取消收藏";
        }
        else Status = "操作失败，请确认已登录";
    }

    public async Task OpenSongDetailAsync(long id)
    {
        try
        {
            var r = await _api.SongDetail(id.ToString());
            DetailSong = r.Songs?.FirstOrDefault();
        }
        catch
        {
            Status = "歌曲信息加载失败";
        }
    }

    public async Task OpenArtistAsync(long id)
    {
        try
        {
            var r = await _api.Artist(id);
            DetailArtist = r.Artist;
            DetailArtistSongs = r.HotSongs ?? new();
            var a = await _api.ArtistAlbums(id);
            DetailAlbums = a.HotAlbums ?? new();
        }
        catch
        {
            Status = "歌手信息加载失败";
        }
    }

    public async Task OpenAlbumAsync(long id)
    {
        try
        {
            var r = await _api.Album(id);
            DetailAlbum = r.Album;
            DetailAlbumSongs = r.Songs ?? new();
        }
        catch
        {
            Status = "专辑加载失败";
        }
    }

    public async Task<(MvDetail? Detail, string? Url)> LoadMvAsync(long id)
    {
        try
        {
            var d = await _api.MvDetail(id);
            var u = await _api.MvUrl(id);
            return (d.Data, u.Data?.Url);
        }
        catch
        {
            Status = "MV 加载失败";
            return (null, null);
        }
    }

    public async Task LoadLikedAsync()
    {
        try
        {
            var liked = await _api.LikeList(UserId);
            _likedIds = liked.Ids ?? new();
            var ids = _likedIds;
            var all = new List<Song>();
            foreach (var chunk in ids.Chunk(100))
            {
                var r = await _api.SongDetail(string.Join(",", chunk));
                if (r.Songs != null) all.AddRange(r.Songs);
            }
            LikedSongs = all;
        }
        catch
        {
            Status = "喜欢列表加载失败";
        }
    }

    public LibVLCSharp.Shared.LibVLC LibVlc => _player.LibVlc;

    // ==================== 多功能：GitHub / 更新 / 公告 ====================

    [RelayCommand]
    private void OpenMusic() => NavigateRequested?.Invoke("music");

    [RelayCommand]
    private void OpenGitHub() => NavigateRequested?.Invoke("github");

    [RelayCommand]
    private void OpenUpdates() => NavigateRequested?.Invoke("updates");

    [RelayCommand]
    private void OpenAnnouncements() => NavigateRequested?.Invoke("announcements");

    [RelayCommand]
    private void OpenSettings() => NavigateRequested?.Invoke("settings");

    [RelayCommand]
    private void OpenLink(string? url)
    {
        if (!string.IsNullOrEmpty(url))
        {
            try
            {
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(url) { UseShellExecute = true });
            }
            catch
            {
            }
        }
    }

    [RelayCommand]
    private void ToggleRelease(ReleaseRow row)
    {
        if (row == null) return;
        row.IsExpanded = !row.IsExpanded;
        ReleaseRows = ReleaseRows.ToList();
    }

    public async Task LoadGitHubAsync()
    {
        try
        {
            GitHubStatus = "加载中…";
            GitRepos = await _github.SearchReposAsync(GitHubQuery);
            GitHubStatus = GitRepos.Count > 0 ? $"共 {GitRepos.Count} 个仓库" : "未获取到数据";
        }
        catch
        {
            GitHubStatus = "加载失败（网络受限）";
        }
    }

    public async Task LoadReleasesAsync()
    {
        try
        {
            Releases = await _github.ReleasesAsync(10);
            ReleaseRows = Releases.Select(r => new ReleaseRow
            {
                TagName = r.TagName,
                Name = r.Name,
                Body = r.Body,
                PublishedAt = r.PublishedAt,
                HtmlUrl = r.HtmlUrl,
                Prerelease = r.Prerelease,
                Assets = r.Assets,
            }).ToList();
        }
        catch
        {
        }
    }

    public async Task<bool> CheckUpdateAsync()
    {
        try
        {
            var list = await _github.ReleasesAsync(1);
            var latest = list.FirstOrDefault();
            if (latest != null)
            {
                var channelOk = ChannelAllowed(latest.TagName);
                HasUpdate = channelOk && IsNewer(latest.TagName, "1.1.0");
            }
        }
        catch
        {
        }
        return HasUpdate;
    }

    // ==================== 开发者尝鲜 ====================

    private HttpClient BetaHttp() => new() { BaseAddress = new Uri(BetaServerUrl.TrimEnd('/') + "/"), Timeout = TimeSpan.FromSeconds(20) };

    public async Task<BetaRequirements> GetBetaRequirementsAsync()
    {
        try
        {
            using var c = BetaHttp();
            var r = await c.GetAsync("requirements");
            if (!r.IsSuccessStatusCode) return new();
            var d = System.Text.Json.JsonSerializer.Deserialize<BetaRequirements>(await r.Content.ReadAsStringAsync());
            return d ?? new();
        }
        catch { return new(); }
    }

    public async Task<BetaApplyResult> ApplyBetaAsync(string email, string name, string purpose, string reason, string device)
    {
        try
        {
            using var c = BetaHttp();
            var body = System.Text.Json.JsonSerializer.Serialize(new { email, name, purpose, reason, device });
            var r = await c.PostAsync("apply", new StringContent(body, System.Text.Encoding.UTF8, "application/json"));
            if (!r.IsSuccessStatusCode) return new() { Status = "error", Message = "服务不可达，请检查 Beta 服务器地址" };
            var d = System.Text.Json.JsonSerializer.Deserialize<BetaApplyResult>(await r.Content.ReadAsStringAsync()) ?? new();
            if (d.Status == "approved" && !string.IsNullOrEmpty(d.Key))
            {
                BetaKey = d.Key;
                BetaAccess = true;
                BetaTier = d.Tier > 0 ? d.Tier : 1;
                SaveSettings();
            }
            return d;
        }
        catch { return new() { Status = "error", Message = "服务不可达" }; }
    }

    public async Task<bool> VerifyBetaAsync(string key)
    {
        try
        {
            using var c = BetaHttp();
            var r = await c.PostAsync("verify", new StringContent(System.Text.Json.JsonSerializer.Serialize(new { key }), System.Text.Encoding.UTF8, "application/json"));
            if (!r.IsSuccessStatusCode) return false;
            var d = System.Text.Json.JsonSerializer.Deserialize<BetaVerifyResult>(await r.Content.ReadAsStringAsync());
            var ok = d?.Valid == true;
            if (ok)
            {
                BetaKey = key;
                BetaAccess = true;
                var tier = d?.Tier ?? 0;
                BetaTier = tier > 0 ? tier : 1;
                SaveSettings();
            }
            else if (d != null)
            {
                BetaTier = 0;
            }
            return ok;
        }
        catch { return false; }
    }

    public void SaveBetaServer()
    {
        SaveSettings();
        Status = "Beta 服务器已保存";
    }

    public async Task LoadAnnouncementsAsync()
    {
        try
        {
            var md = await _github.FetchAnnouncementsAsync();
            var list = GitHubService.ParseAnnouncements(md);
            Announcements = list.Count > 0 ? list : BuiltinAnnouncements();
        }
        catch
        {
            Announcements = BuiltinAnnouncements();
        }
    }

    /// <summary>严格通道过滤：tag 后缀决定通道，与用户层级匹配才放行</summary>
    public bool ChannelAllowed(string tag)
    {
        var t = tag.ToLowerInvariant();
        var channels = new List<string> { "stable" };
        if (BetaTier >= 1) channels.Add("beta");
        if (BetaTier >= 2) channels.Add("alpha");
        if (BetaTier >= 3) channels.Add("dev");
        if (t.Contains("-dev")) return channels.Contains("dev");
        if (t.Contains("-alpha")) return channels.Contains("alpha");
        if (t.Contains("-beta") || t.Contains("-rc")) return channels.Contains("beta");
        return channels.Contains("stable");
    }

    public static bool IsPreRelease(string tag) =>
        tag.Contains("-beta") || tag.Contains("-rc") || tag.Contains("-alpha");

    public static bool IsNewer(string tag, string current)
    {
        (int[] nums, int pre, int preN) Parse(string v)
        {
            var core = v.TrimStart('v');
            var parts = core.Split('-');
            var nums = parts[0].Split('.').Select(x => int.TryParse(x, out var n) ? n : 0).ToArray();
            var pre = 0; var preN = 0;
            if (parts.Length > 1)
            {
                var seg = parts[1].Split('.');
                pre = seg[0].StartsWith("alpha") ? 1 : seg[0].StartsWith("beta") ? 2 : seg[0].StartsWith("rc") ? 3 : 0;
                preN = seg.Length > 1 && int.TryParse(seg[1], out var n) ? n : 0;
            }
            return (nums, pre, preN);
        }
        var a = Parse(tag); var b = Parse(current);
        for (int i = 0; i < Math.Max(a.nums.Length, b.nums.Length); i++)
        {
            var x = i < a.nums.Length ? a.nums[i] : 0;
            var y = i < b.nums.Length ? b.nums[i] : 0;
            if (x != y) return x > y;
        }
        if (a.pre != b.pre) return a.pre > b.pre;
        return a.preN > b.preN;
    }

    private static List<AnnouncementItem> BuiltinAnnouncements() => new()
    {
        new AnnouncementItem
        {
            Id = "welcome", Title = "欢迎使用璃光 GlassSuite",
            Content = "多功能应用套件：音乐、GitHub 检索、更新中心与公告推送。",
            Date = "2026-08-02",
        },
    };

    public void Dispose()
    {
        _timer.Stop();
        SaveSettings();
        _player.Dispose();
    }
}
