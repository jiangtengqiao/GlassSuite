using System.Windows;
using System.Windows.Controls;
using System.Windows.Media.Imaging;
using GlassSuite.Models;
using GlassSuite.ViewModels;

namespace GlassSuite.Views;

public partial class HomeView : UserControl
{
    private readonly MainViewModel _vm;

    public HomeView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        Loaded += async (_, _) =>
        {
            await vm.LoadHomeAsync();
            await vm.LoadToplistsAsync();
            await vm.LoadSquareAsync();
        };
    }

    private void DailySong_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is Song song)
        {
            var idx = _vm.DailySongs.IndexOf(song);
            _vm.PlaySongs(_vm.DailySongs, idx < 0 ? 0 : idx);
        }
    }

    private void Recommend_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is Playlist p)
        {
            _ = _vm.OpenPlaylistAsync(p.Id);
        }
    }

    private void Newsong_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is Song song)
        {
            var idx = _vm.Newsongs.IndexOf(song);
            _vm.PlaySongs(_vm.Newsongs, idx < 0 ? 0 : idx);
        }
    }

    private void Toplist_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is ToplistItem t)
        {
            _ = _vm.OpenPlaylistAsync(t.Id);
        }
    }

    private void Cat_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is string cat)
        {
            _vm.SelectedCat = cat;
            _ = _vm.LoadSquareAsync();
        }
    }

    private void Square_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is Playlist p)
        {
            _ = _vm.OpenPlaylistAsync(p.Id);
        }
    }

    private void UserPlaylist_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is Playlist p)
        {
            _ = _vm.OpenPlaylistAsync(p.Id);
        }
    }

    private void PlaylistCard_ImageLoaded(object sender, RoutedEventArgs e)
    {
        // 占位：图片加载失败时保持空白
    }

    private void Tabs_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (Tabs.SelectedIndex == 2)
        {
            _ = _vm.LoadSquareAsync();
        }
    }

    private void ToplistBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (ToplistBox.SelectedItem is ToplistItem t)
        {
            ToplistBox.SelectedItem = null;
            _ = _vm.OpenPlaylistAsync(t.Id);
        }
    }

    private void CatBtn_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is string cat)
        {
            _vm.SelectedCat = cat;
            _ = _vm.LoadSquareAsync();
        }
    }
}
