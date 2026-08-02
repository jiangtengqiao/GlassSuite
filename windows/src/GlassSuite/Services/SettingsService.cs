using System.IO;
using System.Text.Json;

namespace GlassSuite.Services;

public class AppSettings
{
    public string ApiBaseUrl { get; set; } = "http://localhost:3000";
    public string AccentHex { get; set; } = "#C62F2F";
    public bool DarkMode { get; set; }
    public int LyricFontSize { get; set; } = 18;
    public int LyricOffsetMs { get; set; }
    public int LyricMode { get; set; }
    public string DiyLyric { get; set; } = "";
    public string Quality { get; set; } = "exhigh";
    public long UserId { get; set; }
    public string ProfileJson { get; set; } = "";
    public string Cookie { get; set; } = "";
}

public class SettingsService
{
    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "GlassSuite");
    private static readonly string FilePath = Path.Combine(Dir, "settings.json");

    public AppSettings Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var json = File.ReadAllText(FilePath);
                return JsonSerializer.Deserialize<AppSettings>(json) ?? new AppSettings();
            }
        }
        catch
        {
            // 配置损坏时使用默认值
        }
        return new AppSettings();
    }

    public void Save(AppSettings s)
    {
        try
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(s, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch
        {
            // 忽略写入失败
        }
    }
}
