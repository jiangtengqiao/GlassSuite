using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using CloudMusic.Models;
using CloudMusic.ViewModels;

namespace CloudMusic.Views;

public partial class DetailView : UserControl
{
    private readonly MainViewModel _vm;

    public DetailView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
    }

    private void Play_Click(object sender, RoutedEventArgs e)
    {
        if (_vm.DetailSong != null) _vm.PlaySong(_vm.DetailSong);
    }

    private void Like_Click(object sender, RoutedEventArgs e)
    {
        _ = _vm.ToggleLikeAsync();
    }

    private void Artist_Click(object sender, RoutedEventArgs e)
    {
        if (_vm.DetailSong?.Artists?.FirstOrDefault() is { } a)
        {
            _ = _vm.OpenArtistAsync(a.Id);
        }
    }

    private void Album_Click(object sender, RoutedEventArgs e)
    {
        if (_vm.DetailSong?.Album is { } a)
        {
            _ = _vm.OpenAlbumAsync(a.Id);
        }
    }

    private void Mv_Click(object sender, RoutedEventArgs e)
    {
        if (_vm.DetailSong is { MvId: > 0 } song)
        {
            var win = new MvWindow(_vm, song.MvId) { Owner = Window.GetWindow(this) };
            win.Show();
        }
    }

    private void ArtistPlay_Click(object sender, RoutedEventArgs e)
    {
        _vm.PlaySongs(_vm.DetailArtistSongs, 0);
    }

    private void ArtistSongList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (ArtistSongList.SelectedItem is Song song)
        {
            var idx = _vm.DetailArtistSongs.IndexOf(song);
            _vm.PlaySongs(_vm.DetailArtistSongs, idx < 0 ? 0 : idx);
        }
    }

    private void ArtistAlbum_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (AlbumList.SelectedItem is Album a)
        {
            _ = _vm.OpenAlbumAsync(a.Id);
        }
    }

    private void AlbumPlay_Click(object sender, RoutedEventArgs e)
    {
        _vm.PlaySongs(_vm.DetailAlbumSongs, 0);
    }

    private void AlbumSongList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (AlbumSongList.SelectedItem is Song song)
        {
            var idx = _vm.DetailAlbumSongs.IndexOf(song);
            _vm.PlaySongs(_vm.DetailAlbumSongs, idx < 0 ? 0 : idx);
        }
    }
}
