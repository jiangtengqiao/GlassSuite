using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using CloudMusic.Models;
using CloudMusic.ViewModels;

namespace CloudMusic.Views;

public partial class SearchView : UserControl
{
    private readonly MainViewModel _vm;

    public SearchView(MainViewModel vm)
    {
        InitializeComponent();
        _vm = vm;
        DataContext = vm;
        Loaded += async (_, _) => await vm.LoadHotAsync();
    }

    private void SearchBox_TextChanged(object sender, TextChangedEventArgs e)
    {
        var q = SearchBox.Text.Trim();
        if (string.IsNullOrEmpty(q))
        {
            HotPanel.Visibility = Visibility.Visible;
            ResultPanel.Visibility = Visibility.Collapsed;
        }
    }

    private void SearchBox_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter)
        {
            DoSearch();
        }
    }

    private void SearchBtn_Click(object sender, RoutedEventArgs e) => DoSearch();

    private void HotWord_Click(object sender, RoutedEventArgs e)
    {
        if ((sender as FrameworkElement)?.DataContext is string word)
        {
            SearchBox.Text = word;
            DoSearch();
        }
    }

    private async void DoSearch()
    {
        var q = SearchBox.Text.Trim();
        if (string.IsNullOrEmpty(q)) return;
        _vm.Query = q;
        await _vm.SearchAsync();
        HotPanel.Visibility = Visibility.Collapsed;
        ResultPanel.Visibility = Visibility.Visible;
    }

    private void SongList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (SongList.SelectedItem is Song song)
        {
            var idx = _vm.SearchSongs.IndexOf(song);
            _vm.PlaySongs(_vm.SearchSongs, idx < 0 ? 0 : idx);
        }
    }

    private void PlaylistList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (PlaylistList.SelectedItem is Playlist p)
        {
            _ = _vm.OpenPlaylistAsync(p.Id);
        }
    }

    private void ArtistList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (ArtistList.SelectedItem is Artist a)
        {
            _ = _vm.OpenArtistAsync(a.Id);
        }
    }

    private void AlbumList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (AlbumList.SelectedItem is Album a)
        {
            _ = _vm.OpenAlbumAsync(a.Id);
        }
    }

    private void MvList_MouseDoubleClick(object sender, MouseButtonEventArgs e)
    {
        if (MvList.SelectedItem is Mv mv)
        {
            var win = new MvWindow(_vm, mv.Id) { Owner = Window.GetWindow(this) };
            win.Show();
        }
    }
}
