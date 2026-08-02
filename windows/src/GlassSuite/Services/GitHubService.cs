using System.Net.Http;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GlassSuite.Services;

public class GitHubRepo
{
    [JsonPropertyName("full_name")] public string FullName { get; set; } = "";
    [JsonPropertyName("description")] public string? Description { get; set; }
    [JsonPropertyName("stargazers_count")] public long StargazersCount { get; set; }
    [JsonPropertyName("forks_count")] public long ForksCount { get; set; }
    [JsonPropertyName("language")] public string? Language { get; set; }
    [JsonPropertyName("html_url")] public string HtmlUrl { get; set; } = "";
}

public class GitHubRelease
{
    [JsonPropertyName("tag_name")] public string TagName { get; set; } = "";
    [JsonPropertyName("name")] public string? Name { get; set; }
    [JsonPropertyName("body")] public string? Body { get; set; }
    [JsonPropertyName("published_at")] public string PublishedAt { get; set; } = "";
    [JsonPropertyName("html_url")] public string HtmlUrl { get; set; } = "";
    [JsonPropertyName("prerelease")] public bool Prerelease { get; set; }
    [JsonPropertyName("assets")] public List<GitHubAsset> Assets { get; set; } = new();
}

public class GitHubAsset
{
    [JsonPropertyName("name")] public string Name { get; set; } = "";
    [JsonPropertyName("size")] public long Size { get; set; }
    [JsonPropertyName("browser_download_url")] public string BrowserDownloadUrl { get; set; } = "";
}

public class AnnouncementItem
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string Content { get; set; } = "";
    public bool IsPromo { get; set; }
    public string Url { get; set; } = "";
    public string Date { get; set; } = "";
    public bool IsLinkVisible => !string.IsNullOrEmpty(Url);
}

public class ReleaseRow : GitHubRelease
{
    public bool IsExpanded { get; set; }
}

/// <summary>GitHub REST API 客户端（公开接口，国内访问可配置镜像前缀）</summary>
public class GitHubService
{
    private readonly HttpClient _http = new() { Timeout = TimeSpan.FromSeconds(30) };
    public string Owner { get; set; } = "jiangtengqiao";
    public string Repo { get; set; } = "GlassSuite";
    public string ApiPrefix { get; set; } = "";   // 可填 ghproxy 等加速前缀

    private static readonly JsonSerializerOptions Opts = new()
    {
        PropertyNameCaseInsensitive = true,
    };

    private async Task<T?> GetAsync<T>(string url)
    {
        try
        {
            _http.DefaultRequestHeaders.UserAgent.ParseAdd("GlassSuite");
            var resp = await _http.GetAsync(url);
            if (!resp.IsSuccessStatusCode) return default;
            var json = await resp.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<T>(json, Opts);
        }
        catch
        {
            return default;
        }
    }

    public async Task<List<GitHubRepo>> SearchReposAsync(string query, string sort = "stars", int limit = 30)
    {
        var q = string.IsNullOrWhiteSpace(query) ? "stars:>10000" : query;
        var url = $"{ApiPrefix}https://api.github.com/search/repositories?q={Uri.EscapeDataString(q)}&sort={sort}&order=desc&per_page={limit}";
        var resp = await GetAsync<GitHubSearchResp>(url);
        return resp?.Items ?? new();
    }

    public async Task<List<GitHubRelease>> ReleasesAsync(int limit = 10)
    {
        var url = $"{ApiPrefix}https://api.github.com/repos/{Owner}/{Repo}/releases?per_page={limit}";
        var arr = await GetAsync<GitHubRelease[]>(url);
        return arr?.ToList() ?? new();
    }

    public async Task<string?> FetchAnnouncementsAsync()
    {
        // 优先 jsDelivr CDN（国内可直连），失败回退 raw.githubusercontent
        var urls = new[]
        {
            $"https://cdn.jsdelivr.net/gh/{Owner}/{Repo}@main/ANNOUNCEMENTS.md",
            $"{ApiPrefix}https://raw.githubusercontent.com/{Owner}/{Repo}/main/ANNOUNCEMENTS.md",
        };
        foreach (var u in urls)
        {
            try
            {
                var resp = await _http.GetAsync(u);
                if (resp.IsSuccessStatusCode) return await resp.Content.ReadAsStringAsync();
            }
            catch
            {
            }
        }
        return null;
    }

    public static List<AnnouncementItem> ParseAnnouncements(string? md)
    {
        var list = new List<AnnouncementItem>();
        if (string.IsNullOrWhiteSpace(md)) return list;
        AnnouncementItem? cur = null;
        foreach (var raw in md.Split('\n'))
        {
            var line = raw.Trim();
            if (line.StartsWith("## "))
            {
                if (cur != null) list.Add(cur);
                var title = line[3..].Trim();
                cur = new AnnouncementItem
                {
                    Id = title.GetHashCode().ToString(),
                    Title = title.Replace("[公告] ", "").Replace("[推广] ", "").Trim(),
                    IsPromo = title.StartsWith("[推广]"),
                };
            }
            else if (cur != null)
            {
                if (line.StartsWith("- 链接:")) cur.Url = line[5..].Trim();
                else if (line.StartsWith("- 日期:")) cur.Date = line[5..].Trim();
                else if (!line.StartsWith("#") && line.Length > 0) cur.Content += line + "\n";
            }
        }
        if (cur != null) list.Add(cur);
        return list;
    }
}

internal class GitHubSearchResp
{
    [JsonPropertyName("items")] public List<GitHubRepo>? Items { get; set; }
}
