/** LRC 歌词解析 */
export class LrcLine {
  time: number = 0;
  text: string = '';

  constructor(time: number, text: string) {
    this.time = time;
    this.text = text;
  }
}

export class LrcParser {
  private static TIME_TAG = /\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]/g;

  static parse(lrc: string): LrcLine[] {
    const lines: LrcLine[] = [];
    if (lrc.length === 0) return lines;
    lrc.split('\n').forEach((raw: string) => {
      const line = raw.trim();
      if (line.length === 0) return;
      LrcParser.TIME_TAG.lastIndex = 0;
      const matches: RegExpExecArray[] = [];
      let m: RegExpExecArray | null = LrcParser.TIME_TAG.exec(line);
      while (m !== null) {
        matches.push(m);
        m = LrcParser.TIME_TAG.exec(line);
      }
      if (matches.length === 0) return;
      const text = line.replace(LrcParser.TIME_TAG, '').trim();
      matches.forEach((mm: RegExpExecArray) => {
        const min = parseInt(mm[1]) || 0;
        const sec = parseInt(mm[2]) || 0;
        let frac = (mm[3] ?? '0').padEnd(3, '0').substring(0, 3);
        const fracNum = parseInt(frac) || 0;
        lines.push(new LrcLine(min * 60000 + sec * 1000 + fracNum * 10, text));
      });
    });
    lines.sort((a: LrcLine, b: LrcLine) => a.time - b.time);
    return lines;
  }

  /** 翻译行对齐：按最近时间戳映射到原词时间 */
  static align(trans: LrcLine[], origin: LrcLine[]): Map<number, string> {
    const map: Map<number, string> = new Map();
    if (trans.length === 0 || origin.length === 0) return map;
    trans.forEach((t: LrcLine) => {
      let best: LrcLine = origin[0];
      let bestDiff = Number.MAX_SAFE_INTEGER;
      origin.forEach((o: LrcLine) => {
        const diff = Math.abs(o.time - t.time);
        if (diff < bestDiff) {
          bestDiff = diff;
          best = o;
        }
      });
      if (bestDiff <= 1500) {
        map.set(best.time, t.text);
      }
    });
    return map;
  }
}
