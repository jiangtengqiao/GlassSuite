using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using CloudMusic.Models;
using CloudMusic.ViewModels;

namespace CloudMusic.Views;

public partial class PlaylistView : UserControl
{
    private readonly MainViewModel _vm;

    public PlaylistView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
    }

    private void PlayAll_Click(object sender, RoutedEventArgs e)
    {
        _vm.PlaySongs(_vm.PlaylistSongs.ToList(), 0);
    }

    private void Subscribe_Click(object sender, RoutedEventArgs e)
    {
        _ = _vm.SubscribePlaylistAsync();
    }

    private void SongList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (SongList.SelectedItem is Song song)
        {
            var idx = _vm.PlaylistSongs.IndexOf(song);
            _vm.PlaySongs(_vm.PlaylistSongs.ToList(), idx < 0 ? 0 : idx);
        }
    }

    private void SongRow_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is Song song)
        {
            _ = _vm.OpenSongDetailAsync(song.Id);
        }
    }
}
