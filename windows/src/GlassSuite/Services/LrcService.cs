namespace GlassSuite.Services;

public class LrcLine
{
    public long Time { get; set; }
    public string Text { get; set; } = "";

    public LrcLine(long time, string text)
    {
        Time = time;
        Text = text;
    }
}

public static class LrcService
{
    private static readonly System.Text.RegularExpressions.Regex TimeTag =
        new(@"\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]", System.Text.RegularExpressions.RegexOptions.Compiled);

    public static List<LrcLine> Parse(string? lrc)
    {
        var lines = new List<LrcLine>();
        if (string.IsNullOrWhiteSpace(lrc)) return lines;
        foreach (var raw in lrc.Split('\n'))
        {
            var line = raw.Trim();
            if (line.Length == 0) continue;
            var matches = TimeTag.Matches(line);
            if (matches.Count == 0) continue;
            var text = TimeTag.Replace(line, "").Trim();
            foreach (System.Text.RegularExpressions.Match m in matches)
            {
                var min = long.TryParse(m.Groups[1].Value, out var mi) ? mi : 0;
                var sec = long.TryParse(m.Groups[2].Value, out var se) ? se : 0;
                var fracStr = m.Groups[3].Value.PadRight(3, '0');
                if (fracStr.Length > 3) fracStr = fracStr[..3];
                var frac = long.TryParse(fracStr, out var fr) ? fr : 0;
                lines.Add(new LrcLine(min * 60_000 + sec * 1000 + frac * 10, text));
            }
        }
        return lines.OrderBy(l => l.Time).ToList();
    }

    /// <summary>翻译行按最近时间戳对齐到原词时间</summary>
    public static Dictionary<long, string> Align(List<LrcLine> trans, List<LrcLine> origin)
    {
        var map = new Dictionary<long, string>();
        if (trans.Count == 0 || origin.Count == 0) return map;
        foreach (var t in trans)
        {
            var nearest = origin.OrderBy(o => Math.Abs(o.Time - t.Time)).FirstOrDefault();
            if (nearest != null && Math.Abs(nearest.Time - t.Time) <= 1500)
            {
                map[nearest.Time] = t.Text;
            }
        }
        return map;
    }
}
