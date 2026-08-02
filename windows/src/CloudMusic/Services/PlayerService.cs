using LibVLCSharp.Shared;

namespace CloudMusic.Services;

/// <summary>基于 LibVLC 的音频播放器（支持网络流 + 自定义请求头）</summary>
public class PlayerService : IDisposable
{
    private const string UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private readonly LibVLC _libVlc;
    private readonly MediaPlayer _player;
    private Media? _current;

    public PlayerService()
    {
        Core.Initialize();
        _libVlc = new LibVLC();
        _player = new MediaPlayer(_libVlc);

        _player.TimeChanged += (_, e) => TimeChanged?.Invoke(this, e.Time);
        _player.LengthChanged += (_, e) => LengthChanged?.Invoke(this, e.Length);
        _player.Playing += (_, _) => Playing?.Invoke(this, EventArgs.Empty);
        _player.Paused += (_, _) => Paused?.Invoke(this, EventArgs.Empty);
        _player.EndReached += (_, _) => EndReached?.Invoke(this, EventArgs.Empty);
        _player.EncounteredError += (_, _) => Error?.Invoke(this, EventArgs.Empty);
    }

    public LibVLC LibVlc => _libVlc;
    public MediaPlayer Player => _player;

    public event EventHandler<long>? TimeChanged;
    public event EventHandler<long>? LengthChanged;
    public event EventHandler? Playing;
    public event EventHandler? Paused;
    public event EventHandler? EndReached;
    public event EventHandler? Error;

    public bool IsPlaying => _player.IsPlaying;

    public void PlayUrl(string url)
    {
        Stop();
        _current = new Media(_libVlc, new Uri(url),
            ":http-user-agent=" + UA,
            ":http-referrer=https://music.163.com/");
        _player.Play(_current);
    }

    public void Toggle()
    {
        if (_player.IsPlaying) _player.Pause();
        else _player.Play();
    }

    public void Pause() => _player.Pause();
    public void Resume() => _player.Play();
    public void Stop() => _player.Stop();
    public void Seek(long ms) => _player.Time = ms;
    public long GetTime() => _player.Time;
    public long GetLength() => _player.Length;

    public void Dispose()
    {
        _current?.Dispose();
        _player.Dispose();
        _libVlc.Dispose();
    }
}
