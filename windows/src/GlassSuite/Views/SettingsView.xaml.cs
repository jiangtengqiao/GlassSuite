using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using GlassSuite.ViewModels;

namespace GlassSuite.Views;

public partial class SettingsView : UserControl
{
    private readonly MainViewModel _vm;

    public SettingsView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        ApiBox.Text = _vm.ApiUrl;
        DiyBox.Text = _vm.DiyLyric;
        BetaUrlBox.Text = _vm.BetaServerUrl;
        UpdateModeUi(_vm.DirectMode);
    }

    private void UpdateModeUi(bool direct)
    {
        ServerPanel.Visibility = direct ? Visibility.Collapsed : Visibility.Visible;
        DirectBtn.FontWeight = direct ? FontWeights.Bold : FontWeights.Normal;
        ServerBtn.FontWeight = direct ? FontWeights.Normal : FontWeights.Bold;
        ModeHint.Text = direct
            ? "无需部署服务器：内置网易云官方接口加密协议（weapi/eapi），直连 music.163.com，扫码/验证码登录、歌单、歌词、播放全部可用。"
            : "使用自建的 NeteaseCloudMusicApi 服务（仓库 server/ 目录），适合固定出口 IP 或自定义部署场景。";
    }

    private void DirectMode_Click(object sender, RoutedEventArgs e)
    {
        var direct = (sender as FrameworkElement)?.Tag is bool b && b;
        _vm.SetDirectMode(direct);
        UpdateModeUi(direct);
    }

    private void SaveApi_Click(object sender, RoutedEventArgs e)
    {
        _vm.ApiUrl = ApiBox.Text.Trim();
        _vm.SaveApiUrl();
    }

    private void Accent_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.Tag is string hex)
        {
            _vm.SetAccent(hex);
        }
    }

    private void DarkMode_Checked(object sender, RoutedEventArgs e)
    {
        _vm.DarkMode = DarkModeBox.IsChecked == true;
    }

    private void Mode_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.Tag is int mode)
        {
            _vm.SetLyricMode(mode);
        }
    }

    private void OffsetMinus_Click(object sender, RoutedEventArgs e) => _vm.AdjustOffset(-500);
    private void OffsetPlus_Click(object sender, RoutedEventArgs e) => _vm.AdjustOffset(500);
    private void OffsetReset_Click(object sender, RoutedEventArgs e) => _vm.ResetOffset();
    private void FontMinus_Click(object sender, RoutedEventArgs e) => _vm.AdjustFontSize(-1);
    private void FontPlus_Click(object sender, RoutedEventArgs e) => _vm.AdjustFontSize(1);

    private void SaveDiy_Click(object sender, RoutedEventArgs e)
    {
        _vm.DiyLyric = DiyBox.Text;
        _vm.SaveDiy();
    }

    private void LegalDoc_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.Tag is string file)
        {
            ShowDoc(file);
        }
    }

    private void Beta_Click(object sender, RoutedEventArgs e)
    {
        BetaRequested?.Invoke(this, EventArgs.Empty);
    }

    private void SaveBetaUrl_Click(object sender, RoutedEventArgs e)
    {
        _vm.BetaServerUrl = BetaUrlBox.Text.Trim();
        _vm.SaveBetaServer();
    }

    private async void ErrLog_Click(object sender, RoutedEventArgs e)
    {
        ErrBox.Text = GlassSuite.Services.ErrorReporter.RecentLogs();
        ErrBox.Visibility = Visibility.Visible;
        ErrStatus.Text = $"待上传日志：{GlassSuite.Services.ErrorReporter.PendingCount()} 条";
    }

    private async void ErrUpload_Click(object sender, RoutedEventArgs e)
    {
        ErrStatus.Text = "上传中…";
        try
        {
            var n = await GlassSuite.Services.ErrorReporter.UploadAllAsync();
            ErrStatus.Text = n > 0 ? $"✅ 已上传 {n} 条错误日志" : "无待上传日志";
        }
        catch (Exception ex)
        {
            ErrStatus.Text = "上传失败：" + ex.Message;
        }
    }

    private void ErrClear_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            var dir = System.IO.Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "GlassSuite", "errors");
            if (System.IO.Directory.Exists(dir))
            {
                foreach (var f in System.IO.Directory.GetFiles(dir, "*.log")) System.IO.File.Delete(f);
            }
            ErrBox.Visibility = Visibility.Collapsed;
            ErrStatus.Text = "已清空本地错误日志";
        }
        catch (Exception ex)
        {
            ErrStatus.Text = "清空失败：" + ex.Message;
        }
    }

    public event EventHandler? BetaRequested;

    private void ShowDoc(string file)
    {
        try
        {
            var path = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Assets", "legal", file);
            if (!File.Exists(path))
            {
                MessageBox.Show("文档文件缺失：" + file, "璃光");
                return;
            }
            var win = new Window
            {
                Title = file.Replace(".txt", ""),
                Width = 720,
                Height = 640,
                WindowStartupLocation = WindowStartupLocation.CenterOwner,
                Owner = Window.GetWindow(this),
                Content = new ScrollViewer
                {
                    Padding = new Thickness(20),
                    Content = new TextBlock
                    {
                        Text = File.ReadAllText(path),
                        FontSize = 13,
                        TextWrapping = TextWrapping.Wrap,
                        Foreground = new SolidColorBrush(Color.FromRgb(0x33, 0x33, 0x33)),
                    }
                }
            };
            win.ShowDialog();
        }
        catch (Exception ex)
        {
            MessageBox.Show("打开文档失败：" + ex.Message, "璃光");
        }
    }
}
