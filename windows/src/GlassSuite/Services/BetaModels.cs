using System.Text.Json.Serialization;

namespace GlassSuite.Services;

public class BetaRequirements
{
    [JsonPropertyName("title")] public string Title { get; set; } = "";
    [JsonPropertyName("intro")] public string Intro { get; set; } = "";
    [JsonPropertyName("items")] public List<string> Items { get; set; } = new();
    [JsonPropertyName("scoreThreshold")] public int ScoreThreshold { get; set; } = 60;
    [JsonPropertyName("note")] public string Note { get; set; } = "";
}

public class BetaApplyResult
{
    [JsonPropertyName("status")] public string Status { get; set; } = "none";
    [JsonPropertyName("score")] public int Score { get; set; }
    [JsonPropertyName("key")] public string Key { get; set; } = "";
    [JsonPropertyName("reasons")] public List<string> Reasons { get; set; } = new();
    [JsonPropertyName("message")] public string Message { get; set; } = "";
}

public class BetaVerifyResult
{
    [JsonPropertyName("valid")] public bool Valid { get; set; }
    [JsonPropertyName("beta")] public bool Beta { get; set; }
}
