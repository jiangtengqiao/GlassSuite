using System.Windows;
using CloudMusic.ViewModels;
using LibVLCSharp.Shared;

namespace CloudMusic.Views;

public partial class MvWindow : Window
{
    private readonly MainViewModel _vm;
    private readonly long _mvId;
    private MediaPlayer? _mediaPlayer;

    public MvWindow(MainViewModel vm, long mvId)
    {
        InitializeComponent();
        _vm = vm;
        _mvId = mvId;
        Loaded += async (_, _) =>
        {
            var (detail, url) = await _vm.LoadMvAsync(_mvId);
            if (detail != null)
            {
                Title = detail.Name ?? "MV 播放";
                InfoText.Text = $"{detail.Name} · {detail.ArtistName} · 播放 {detail.PlayCount} 次";
            }
            if (!string.IsNullOrEmpty(url))
            {
                try
                {
                    _mediaPlayer = new MediaPlayer(_vm.LibVlc);
                    VideoView.MediaPlayer = _mediaPlayer;
                    using var media = new Media(_vm.LibVlc, new Uri(url));
                    _mediaPlayer.Play(media);
                }
                catch (Exception ex)
                {
                    MessageBox.Show("MV 播放失败：" + ex.Message, "乐云音乐");
                }
            }
            else
            {
                InfoText.Text = "MV 暂无可用播放源";
            }
        };
        Closed += (_, _) =>
        {
            _mediaPlayer?.Stop();
            _mediaPlayer?.Dispose();
            _mediaPlayer = null;
        };
    }
}
