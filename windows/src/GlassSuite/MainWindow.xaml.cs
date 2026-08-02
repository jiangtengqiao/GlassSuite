using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using GlassSuite.Services;
using GlassSuite.ViewModels;
using GlassSuite.Views;

namespace GlassSuite;

public partial class MainWindow : Window
{
    private readonly MainViewModel _vm;
    private readonly Dictionary<string, object> _views = new();
    private bool _userDragging;
    private string _currentView = "";

    public MainWindow()
    {
        InitializeComponent();
        _vm = new MainViewModel(new ApiService(), new PlayerService(), new SettingsService(), new SmtcService());
        DataContext = _vm;
        _vm.PropertyChanged += OnVmChanged;
        _vm.NavigateRequested += ShowView;
        _ = InitAsync();
    }

    private async Task InitAsync()
    {
        // 播放器与 SMTC 初始化后切换到首页/登录页
        ShowView("center");
    }

    private void OnVmChanged(object? sender, PropertyChangedEventArgs e)
    {
        Dispatcher.Invoke(() =>
        {
            switch (e.PropertyName)
            {
                case nameof(MainViewModel.IsLoggedIn):
                    ShowView("center");
                    break;
                case nameof(MainViewModel.CurrentPlaylist):
                    if (_vm.CurrentPlaylist != null) ShowView("playlist");
                    break;
                case nameof(MainViewModel.DetailSong):
                    if (_vm.DetailSong != null) ShowView("detail");
                    break;
                case nameof(MainViewModel.DetailArtist):
                    if (_vm.DetailArtist != null) ShowView("detail");
                    break;
                case nameof(MainViewModel.DetailAlbum):
                    if (_vm.DetailAlbum != null) ShowView("detail");
                    break;
                case nameof(MainViewModel.CurrentSong):
                    PlayerBar.Visibility = _vm.CurrentSong == null ? Visibility.Collapsed : Visibility.Visible;
                    if (_vm.CurrentSong != null) Title = $"♪ {_vm.CurrentSong.Name} - {_vm.CurrentSong.ArtistNames}";
                    break;
                case nameof(MainViewModel.IsPlaying):
                    PlayBtn.Content = _vm.IsPlaying ? "⏸" : "▶";
                    break;
                case nameof(MainViewModel.Position):
                case nameof(MainViewModel.Duration):
                    UpdateProgress();
                    break;
                case nameof(MainViewModel.RepeatOne):
                    ModeBtn.Content = _vm.RepeatOne ? "单曲" : "顺序";
                    break;
                case nameof(MainViewModel.Shuffle):
                    ShuffleBtn.Content = _vm.Shuffle ? "随机" : "列表";
                    break;
                case nameof(MainViewModel.IsLiked):
                    LikeBtn.Content = _vm.IsLiked ? "♥" : "♡";
                    LikeBtn.Foreground = _vm.IsLiked
                        ? System.Windows.Media.Brushes.IndianRed
                        : System.Windows.Media.Brushes.Gray;
                    break;
                case nameof(MainViewModel.Quality):
                    QualityBtn.Content = ApiService.QualityNames.TryGetValue(_vm.Quality, out var qn) ? qn : _vm.Quality;
                    break;
                case nameof(MainViewModel.Lyrics):
                case nameof(MainViewModel.LyricMode):
                    LyricCtrl.SetLyrics(_vm.Lyrics);
                    break;
                case nameof(MainViewModel.CurrentLyricIndex):
                    LyricCtrl.SetCurrentIndex(_vm.CurrentLyricIndex);
                    break;
                case nameof(MainViewModel.LyricFontSize):
                    LyricCtrl.FontSize = _vm.LyricFontSize;
                    break;
            }
        });
    }

    private void UpdateProgress()
    {
        if (_userDragging) return;
        if (_vm.Duration > 0)
        {
            ProgressSlider.Maximum = _vm.Duration;
            ProgressSlider.Value = _vm.Position;
            PositionText.Text = FormatMs(_vm.Position);
            DurationText.Text = FormatMs(_vm.Duration);
        }
        else
        {
            PositionText.Text = "00:00";
            DurationText.Text = "00:00";
        }
    }

    private static string FormatMs(long ms)
    {
        var t = TimeSpan.FromMilliseconds(ms);
        return t.TotalHours >= 1 ? $"{t.Hours}:{t.Minutes:D2}:{t.Seconds:D2}" : $"{t.Minutes:D2}:{t.Seconds:D2}";
    }

    private void ShowView(string key)
    {
        if (_currentView == key) return;
        _currentView = key;
        ContentHost.Content = key switch
        {
            "login" => new LoginView(_vm),
            "center" => GetOrCreate("center", () => new HomeCenterView(_vm)),
            "github" => GetOrCreate("github", () => new GitHubView(_vm)),
            "updates" => GetOrCreate("updates", () => new UpdateCenterView(_vm)),
            "announcements" => GetOrCreate("announcements", () => new AnnouncementView(_vm)),
            "home" => GetOrCreate("home", () => new HomeView(_vm)),
            "search" => GetOrCreate("search", () => new SearchView(_vm)),
            "user" => GetOrCreate("user", () =>
            {
                var uv = new UserView(_vm);
                uv.SettingsRequested += (_, _) => ShowView("settings");
                return uv;
            }),
            "settings" => GetOrCreate("settings", () => new SettingsView(_vm)),
            "playlist" => GetOrCreate("playlist", () => new PlaylistView(_vm)),
            "detail" => GetOrCreate("detail", () => new DetailView(_vm)),
            _ => new TextBlock { Text = key, Margin = new Thickness(20) },
        };
    }

    private object GetOrCreate(string key, Func<object> factory)
    {
        if (!_views.TryGetValue(key, out var v))
        {
            v = factory();
            _views[key] = v;
        }
        return v;
    }

    private void NavCenter_Click(object sender, RoutedEventArgs e) => ShowView("center");
    private void NavHome_Click(object sender, RoutedEventArgs e) => ShowView("home");
    private void NavSearch_Click(object sender, RoutedEventArgs e) => ShowView("search");
    private void NavUser_Click(object sender, RoutedEventArgs e) => ShowView("user");
    private void NavSettings_Click(object sender, RoutedEventArgs e) => ShowView("settings");

    private void Play_Click(object sender, RoutedEventArgs e) => _vm.Toggle();
    private void Prev_Click(object sender, RoutedEventArgs e) => _vm.Prev();
    private void Next_Click(object sender, RoutedEventArgs e) => _vm.Next();
    private void Mode_Click(object sender, RoutedEventArgs e) => _vm.ToggleRepeatOne();
    private void Shuffle_Click(object sender, RoutedEventArgs e) => _vm.ToggleShuffle();
    private void Like_Click(object sender, RoutedEventArgs e) => _ = _vm.ToggleLikeAsync();

    private void ProgressSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (_userDragging || !IsLoaded) return;
        if (_vm.Duration > 0)
        {
            _vm.Seek((long)ProgressSlider.Value);
        }
    }

    private void ProgressSlider_DragStarted(object sender, System.Windows.Controls.Primitives.DragStartedEventArgs e)
    {
        _userDragging = true;
    }

    private void ProgressSlider_DragCompleted(object sender, System.Windows.Controls.Primitives.DragCompletedEventArgs e)
    {
        _userDragging = false;
        if (_vm.Duration > 0) _vm.Seek((long)ProgressSlider.Value);
    }

    private void LyricToggle_Click(object sender, RoutedEventArgs e)
    {
        LyricPanel.Visibility = LyricPanel.Visibility == Visibility.Visible ? Visibility.Collapsed : Visibility.Visible;
    }

    private void LyricClose_Click(object sender, RoutedEventArgs e) => LyricPanel.Visibility = Visibility.Collapsed;

    private void Queue_Click(object sender, RoutedEventArgs e)
    {
        if (_vm.Queue.Count == 0)
        {
            MessageBox.Show("播放队列为空", "璃光", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        var items = _vm.Queue.Select((s, i) => $"{(i == _vm.QueueIndex ? "▶ " : "  ")}{i + 1}. {s.Name} - {s.ArtistNames}");
        var win = new Window
        {
            Title = $"播放队列（{_vm.Queue.Count}）",
            Width = 420,
            Height = 480,
            WindowStartupLocation = WindowStartupLocation.CenterOwner,
            Owner = this,
            Content = new ListBox
            {
                ItemsSource = items,
                FontSize = 13,
                Padding = new Thickness(8),
            }
        };
        win.Show();
    }

    private void Quality_Click(object sender, RoutedEventArgs e)
    {
        var menu = new ContextMenu();
        foreach (var level in ApiService.QualityLevels)
        {
            var name = ApiService.QualityNames[level];
            var item = new MenuItem { Header = name + (level == _vm.Quality ? " ✓" : ""), Tag = level };
            item.Click += (_, _) => _vm.SetQuality((string)((MenuItem)item).Tag);
            menu.Items.Add(item);
        }
        menu.PlacementTarget = (FrameworkElement)sender;
        menu.IsOpen = true;
    }

    protected override void OnClosed(EventArgs e)
    {
        _vm.Dispose();
        base.OnClosed(e);
    }
}
