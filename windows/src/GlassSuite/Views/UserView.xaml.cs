using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using GlassSuite.Models;
using GlassSuite.ViewModels;

namespace GlassSuite.Views;

public partial class UserView : UserControl
{
    private readonly MainViewModel _vm;

    public UserView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        Loaded += async (_, _) => await _vm.LoadUserDataAsync();
    }

    private void Liked_Click(object sender, RoutedEventArgs e)
    {
        _ = _vm.LoadLikedAsync();
    }

    private void Playlist_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (UserPlaylistList.SelectedItem is Playlist p)
        {
            _ = _vm.OpenPlaylistAsync(p.Id);
        }
    }

    private void LikedSongList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (LikedSongList.SelectedItem is Song song)
        {
            var idx = _vm.LikedSongs.IndexOf(song);
            _vm.PlaySongs(_vm.LikedSongs, idx < 0 ? 0 : idx);
        }
    }

    private void Logout_Click(object sender, RoutedEventArgs e)
    {
        _vm.Logout();
    }

    private void Settings_Click(object sender, RoutedEventArgs e)
    {
        // 切换到设置视图由 MainWindow 处理
        SettingsRequested?.Invoke(this, EventArgs.Empty);
    }

    public event EventHandler? SettingsRequested;
}
