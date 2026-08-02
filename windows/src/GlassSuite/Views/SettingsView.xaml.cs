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
