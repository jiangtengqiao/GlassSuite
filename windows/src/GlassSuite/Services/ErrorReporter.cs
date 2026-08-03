using System.IO;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using GlassSuite.Models;

namespace GlassSuite.Services;

/// <summary>
/// 错误上报体系（Windows 端）：
/// - 全局异常捕获（AppDomain + Dispatcher）
/// - 本地滚动日志（%AppData%/GlassSuite/errors/）
/// - 自动上传：优先 Beta 服务器 /api/error，其次自托管 API 服务器 /api/error
/// - 上传失败自动保留，下次启动/轮询重试
/// </summary>
public class ErrorReporter
{
    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "GlassSuite", "errors");
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(12) };
    private static readonly object Sync = new();

    public static bool Initialized { get; private set; }

    /// <summary>上传通道（由 MainViewModel 启动时注入）</summary>
    public static string BetaServerUrl { get; set; } = "";
    public static string ApiBaseUrl { get; set; } = "";

    /// <summary>注册全局异常捕获（应用启动时调用一次）</summary>
    public static void Init()
    {
        if (Initialized) return;
        Initialized = true;
        try { Directory.CreateDirectory(Dir); } catch { }

        AppDomain.CurrentDomain.UnhandledException += (_, e) =>
        {
            var ex = e.ExceptionObject as Exception;
            Log("Unhandled", ex?.ToString() ?? e.ExceptionObject?.ToString() ?? "unknown");
            TryUploadSync();
        };

        System.Windows.Application.Current.DispatcherUnhandledException += (_, e) =>
        {
            Log("Dispatcher", e.Exception.ToString());
            TryUploadSync();
        };

        TaskScheduler.UnobservedTaskException += (_, e) =>
        {
            Log("Task", e.Exception.ToString());
            e.SetObserved();
        };

        // 启动后台轮询上传（失败重试）
        _ = Task.Run(async () =>
        {
            while (true)
            {
                try { await UploadAllAsync(); } catch { }
                await Task.Delay(TimeSpan.FromMinutes(30));
            }
        });
    }

    /// <summary>记录一条错误日志（带设备信息）</summary>
    public static void Log(string tag, string message)
    {
        try
        {
            Directory.CreateDirectory(Dir);
            var name = $"error-{DateTime.Now:yyyyMMdd}.log";
            var file = Path.Combine(Dir, name);
            lock (Sync)
            {
                File.AppendAllText(file,
                    $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] [{tag}] {message}{Environment.NewLine}");
                TrimOldFiles();
            }
        }
        catch { }
    }

    public static string DeviceInfo()
    {
        try
        {
            var os = Environment.OSVersion.VersionString;
            var arch = Environment.Is64BitOperatingSystem ? "x64" : "x86";
            return JsonSerializer.Serialize(new
            {
                os,
                arch,
                runtime = Environment.Version.ToString(),
                app = $"GlassSuite 1.1.0",
                machine = Environment.MachineName,
            });
        }
        catch { return "{}"; }
    }

    public static int PendingCount()
    {
        try { return Directory.Exists(Dir) ? Directory.GetFiles(Dir, "*.log").Length : 0; }
        catch { return 0; }
    }

    public static string RecentLogs(int maxChars = 6000)
    {
        try
        {
            if (!Directory.Exists(Dir)) return "(无日志)";
            var files = Directory.GetFiles(Dir, "*.log").OrderByDescending(f => f).Take(3);
            var sb = new StringBuilder();
            foreach (var f in files)
            {
                var content = File.ReadAllText(f);
                sb.AppendLine($"===== {Path.GetFileName(f)} =====");
                sb.AppendLine(content.Length > maxChars ? content[^maxChars..] : content);
            }
            return sb.Length == 0 ? "(无日志)" : sb.ToString();
        }
        catch { return "(读取失败)"; }
    }

    public static async Task<int> UploadAllAsync()
    {
        if (!Directory.Exists(Dir)) return 0;
        var ok = 0;
        foreach (var f in Directory.GetFiles(Dir, "*.log"))
        {
            if (await UploadFileAsync(f)) { try { File.Delete(f); } catch { } ok++; }
        }
        return ok;
    }

    private static async Task<bool> UploadFileAsync(string file)
    {
        var log = File.ReadAllText(file);
        var body = JsonSerializer.Serialize(new { device = JsonDocument.Parse(DeviceInfo()).RootElement, log });
        var content = new StringContent(body, Encoding.UTF8, "application/json");
        try
        {
            using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(12) };
            // 通道 1：Beta 服务器
            var beta = BetaServerUrl;
            if (!string.IsNullOrEmpty(beta))
            {
                var r = await client.PostAsync(beta.TrimEnd('/') + "/api/error", content);
                if (r.IsSuccessStatusCode) return true;
            }
            // 通道 2：自托管音乐服务器（非直连模式）
            var api = ApiBaseUrl;
            if (!string.IsNullOrEmpty(api) && api.StartsWith("http"))
            {
                var r2 = await client.PostAsync(api.TrimEnd('/') + "/api/error", content);
                if (r2.IsSuccessStatusCode) return true;
            }
        }
        catch { }
        return false;
    }

    private static void TryUploadSync()
    {
        try { UploadAllAsync().GetAwaiter().GetResult(); } catch { }
    }

    private static void TrimOldFiles()
    {
        try
        {
            var files = Directory.GetFiles(Dir, "*.log").OrderByDescending(f => f).ToList();
            while (files.Count > 20)
            {
                File.Delete(files[^1]);
                files.RemoveAt(files.Count - 1);
            }
        }
        catch { }
    }
}
