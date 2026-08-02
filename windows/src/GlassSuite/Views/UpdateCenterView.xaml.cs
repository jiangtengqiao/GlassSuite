using System.ComponentModel;
using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using GlassSuite.ViewModels;

namespace GlassSuite.Views;

public partial class UpdateCenterView : UserControl
{
    private readonly MainViewModel _vm;

    public UpdateCenterView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        Loaded += async (_, _) =>
        {
            await vm.LoadReleasesAsync();
            await vm.CheckUpdateAsync();
            UpdateStatus.Text = vm.HasUpdate ? "发现新版本！请前往 Releases 下载" : "已是最新版本";
            DownloadBtn.Visibility = vm.HasUpdate ? Visibility.Visible : Visibility.Collapsed;
            _vm.PropertyChanged += OnVmChanged;
        };
    }

    private void OnVmChanged(object? sender, PropertyChangedEventArgs e)
    {
        Dispatcher.Invoke(() =>
        {
            if (e.PropertyName == nameof(MainViewModel.HasUpdate))
            {
                UpdateStatus.Text = _vm.HasUpdate ? "发现新版本！请前往 Releases 下载" : "已是最新版本";
                DownloadBtn.Visibility = _vm.HasUpdate ? Visibility.Visible : Visibility.Collapsed;
            }
        });
    }

    private async void Check_Click(object sender, RoutedEventArgs e)
    {
        await _vm.CheckUpdateAsync();
        UpdateStatus.Text = _vm.HasUpdate ? "发现新版本！请前往 Releases 下载" : "已是最新版本";
        DownloadBtn.Visibility = _vm.HasUpdate ? Visibility.Visible : Visibility.Collapsed;
    }

    private void Download_Click(object sender, RoutedEventArgs e)
    {
        var latest = _vm.ReleaseRows.FirstOrDefault();
        if (latest != null)
        {
            try
            {
                Process.Start(new ProcessStartInfo(latest.HtmlUrl) { UseShellExecute = true });
            }
            catch
            {
            }
        }
    }
}
