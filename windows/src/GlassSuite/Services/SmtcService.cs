using System;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;

namespace GlassSuite.Services;

/// <summary>
/// 系统媒体信息集成（SMTC，尽力而为）。
/// 通过反射调用 Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager，
/// 更新任务栏/锁屏媒体浮层。若运行环境未携带 Windows SDK 投影（Microsoft.Windows.SDK.NET），
/// 自动静默降级，不影响任何主功能。
/// </summary>
public class SmtcService
{
    private object? _session;
    private bool _available;
    private bool _checked;

    public bool IsAvailable => _available;

    private void EnsureInit()
    {
        if (_checked) return;
        _checked = true;
        try
        {
            // Windows SDK 投影程序集仅在带版本的 windows TFM（如 net8.0-windows10.0.19041.0）下随应用分发
            var asm = AppDomain.CurrentDomain.GetAssemblies()
                .FirstOrDefault(a => a.GetName().Name == "Microsoft.Windows.SDK.NET");
            if (asm == null)
            {
                _available = false;
                return;
            }
            var mgrType = asm.GetType("Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager");
            if (mgrType == null)
            {
                _available = false;
                return;
            }
            var requestMethod = mgrType.GetMethod("RequestAsync", BindingFlags.Public | BindingFlags.Static);
            if (requestMethod == null)
            {
                _available = false;
                return;
            }
            _available = requestMethod != null;
        }
        catch
        {
            _available = false;
        }
    }

    public Task InitAsync()
    {
        EnsureInit();
        return Task.CompletedTask;
    }

    public async Task UpdateAsync(string title, string artist, string album, bool playing, long positionMs, long durationMs)
    {
        EnsureInit();
        if (!_available) return;
        try
        {
            var asm = AppDomain.CurrentDomain.GetAssemblies()
                .FirstOrDefault(a => a.GetName().Name == "Microsoft.Windows.SDK.NET");
            if (asm == null) return;
            var mgrType = asm.GetType("Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager");
            var sessionType = asm.GetType("Windows.Media.Control.GlobalSystemMediaTransportControlsSession");
            if (mgrType == null || sessionType == null) return;

            var requestMethod = mgrType.GetMethod("RequestAsync", BindingFlags.Public | BindingFlags.Static);
            var requestTask = (Task?)requestMethod?.Invoke(null, null);
            if (requestTask == null) return;
            await requestTask.ConfigureAwait(true);
            var manager = requestTask.GetType().GetProperty("Result")?.GetValue(requestTask);
            if (manager == null) return;

            var getSession = mgrType.GetMethod("GetCurrentSession");
            var session = getSession?.Invoke(manager, null);
            if (session == null) return;
            _session = session;

            // TryUpdatePlaybackInfoAsync(playbackInfo)
            var playbackInfoType = asm.GetType("Windows.Media.GlobalSystemMediaTransportControlsSessionPlaybackInfo");
            var controlsType = asm.GetType("Windows.Media.GlobalSystemMediaTransportControlsSessionPlaybackControls");
            if (playbackInfoType != null && controlsType != null)
            {
                var info = Activator.CreateInstance(playbackInfoType);
                var controls = Activator.CreateInstance(controlsType);
                controlsType.GetProperty("IsPlayEnabled")?.SetValue(controls, true);
                controlsType.GetProperty("IsPauseEnabled")?.SetValue(controls, true);
                controlsType.GetProperty("IsNextEnabled")?.SetValue(controls, true);
                controlsType.GetProperty("IsPreviousEnabled")?.SetValue(controls, true);
                controlsType.GetProperty("IsPlayPauseToggleEnabled")?.SetValue(controls, true);
                playbackInfoType.GetProperty("Controls")?.SetValue(info, controls);
                var statusType = asm.GetType("Windows.Media.MediaPlaybackStatus");
                var status = statusType == null ? null : Enum.Parse(statusType, playing ? "Playing" : "Paused");
                if (status == null) return;
                playbackInfoType.GetProperty("PlaybackStatus")?.SetValue(info, status);
                var upd = sessionType.GetMethod("TryUpdatePlaybackInfoAsync");
                var t1 = (Task?)upd?.Invoke(session, new[] { info });
                if (t1 != null) await t1.ConfigureAwait(true);
            }

            // TryUpdateTimelinePropertiesAsync(timeline)
            var timelineType = asm.GetType("Windows.Media.GlobalSystemMediaTransportControlsSessionTimelineProperties");
            if (timelineType != null)
            {
                var tl = Activator.CreateInstance(timelineType);
                timelineType.GetProperty("StartTime")?.SetValue(tl, TimeSpan.Zero);
                timelineType.GetProperty("EndTime")?.SetValue(tl, TimeSpan.FromMilliseconds(Math.Max(1, durationMs)));
                timelineType.GetProperty("Position")?.SetValue(tl, TimeSpan.FromMilliseconds(Math.Max(0, positionMs)));
                timelineType.GetProperty("MinSeekTime")?.SetValue(tl, TimeSpan.Zero);
                timelineType.GetProperty("MaxSeekTime")?.SetValue(tl, TimeSpan.FromMilliseconds(Math.Max(1, durationMs)));
                var upd = sessionType.GetMethod("TryUpdateTimelinePropertiesAsync");
                var t2 = (Task?)upd?.Invoke(session, new[] { tl });
                if (t2 != null) await t2.ConfigureAwait(true);
            }
        }
        catch
        {
            // 静默降级
        }
    }

    public async Task ClearAsync()
    {
        EnsureInit();
        if (!_available || _session == null) return;
        try
        {
            var asm = AppDomain.CurrentDomain.GetAssemblies()
                .FirstOrDefault(a => a.GetName().Name == "Microsoft.Windows.SDK.NET");
            var sessionType = asm?.GetType("Windows.Media.Control.GlobalSystemMediaTransportControlsSession");
            var playbackInfoType = asm?.GetType("Windows.Media.GlobalSystemMediaTransportControlsSessionPlaybackInfo");
            var statusType = asm?.GetType("Windows.Media.MediaPlaybackStatus");
            if (sessionType == null || playbackInfoType == null || statusType == null) return;
            var info = Activator.CreateInstance(playbackInfoType);
            playbackInfoType.GetProperty("PlaybackStatus")?.SetValue(info, statusType == null ? null : Enum.Parse(statusType, "Stopped"));
            var upd = sessionType.GetMethod("TryUpdatePlaybackInfoAsync");
            var t = (Task?)upd?.Invoke(_session, new[] { info });
            if (t != null) await t.ConfigureAwait(true);
        }
        catch
        {
        }
    }
}
