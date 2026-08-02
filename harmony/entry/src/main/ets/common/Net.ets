/**
 * 网络封装：HTTP + Cookie 持久化（preferences）
 */
import { http } from '@kit.NetworkKit';
import { preferences } from '@kit.ArkData';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { Constants } from './Constants';

export class Net {
  private static prefs: preferences.Preferences | undefined = undefined;
  private static cookieStr: string = '';

  static init(context: common.UIAbilityContext): void {
    preferences.getPreferences(context, 'cloudmusic_store').then((p: preferences.Preferences) => {
      Net.prefs = p;
      Net.cookieStr = p.getSync('cookie', '') as string;
      const base = p.getSync('api_base', Constants.DEFAULT_API) as string;
      Constants.apiBaseUrl = base;
      Constants.accentHex = p.getSync('accent', '#C62F2F') as string;
      Constants.darkMode = p.getSync('dark', false) as boolean;
      Constants.lyricFontSize = p.getSync('lyric_size', 18) as number;
      Constants.lyricOffsetMs = p.getSync('lyric_offset', 0) as number;
      Constants.lyricMode = p.getSync('lyric_mode', 0) as number;
      Constants.diyLyric = p.getSync('diy_lyric', '') as string;
      Constants.quality = p.getSync('quality', 'exhigh') as string;
      Constants.userId = p.getSync('user_id', 0) as number;
      Constants.profileJson = p.getSync('profile_json', '') as string;
      Constants.betaKey = p.getSync('beta_key', '') as string;
    }).catch((err: BusinessError) => {
      console.error(`[Net] init error: ${err.code} ${err.message}`);
    });
  }

  static put(key: string, value: preferences.ValueType): void {
    if (Net.prefs === undefined) return;
    Net.prefs.put(key, value).then(() => {
      Net.prefs?.flush();
    }).catch((err: BusinessError) => {
      console.error(`[Net] put error: ${err.code}`);
    });
  }

  static getCookie(): string {
    return Net.cookieStr;
  }

  static saveCookieString(cookie: string): void {
    if (cookie.length === 0) return;
    Net.cookieStr = cookie;
    Net.put('cookie', cookie);
  }

  private static mergeCookies(setCookies: Array<string>): void {
    if (setCookies.length === 0) return;
    const map: Record<string, string> = {};
    if (Net.cookieStr.length > 0) {
      Net.cookieStr.split(';').forEach((pair: string) => {
        const idx = pair.indexOf('=');
        if (idx > 0) {
          map[pair.substring(0, idx).trim()] = pair.substring(idx + 1).trim();
        }
      });
    }
    setCookies.forEach((c: string) => {
      const first = c.split(';')[0];
      const idx = first.indexOf('=');
      if (idx > 0) {
        map[first.substring(0, idx).trim()] = first.substring(idx + 1).trim();
      }
    });
    let merged = '';
    Object.keys(map).forEach((k: string) => {
      merged += `${k}=${map[k]}; `;
    });
    Net.cookieStr = merged.trim();
    Net.put('cookie', Net.cookieStr);
  }

  private static buildUrl(path: string, params?: Record<string, string>): string {
    let url = `${Constants.apiBaseUrl}/${path}`;
    if (params !== undefined) {
      const keys = Object.keys(params);
      if (keys.length > 0) {
        url += '?';
        keys.forEach((k: string, i: number) => {
          url += `${i > 0 ? '&' : ''}${k}=${encodeURIComponent(params[k])}`;
        });
      }
    }
    return url;
  }

  static async request(path: string, params?: Record<string, string>): Promise<Record<string, Object>> {
    const url = Net.buildUrl(path, params);
    const httpRequest = http.createHttp();
    try {
      const res: http.HttpResponse = await httpRequest.request(url, {
        method: http.RequestMethod.GET,
        header: {
          'User-Agent': Constants.UA,
          'Referer': 'https://music.163.com/',
          'Cookie': Net.cookieStr
        },
        expectDataType: http.HttpDataType.JSON,
        connectTimeout: 15000,
        readTimeout: 30000
      });
      if (res.responseCode !== 200) {
        throw new Error(`HTTP ${res.responseCode}`);
      }
      if (res.cookies !== undefined && res.cookies.length > 0) {
        Net.mergeCookies(res.cookies);
      }
      const result = res.result as string;
      const obj = JSON.parse(result) as Record<string, Object>;
      if (obj['code'] !== undefined && obj['code'] !== null && Number(obj['code']) === 301) {
        throw new Error('需要登录');
      }
      return obj;
    } finally {
      httpRequest.destroy();
    }
  }

  /** 通用 JSON POST（Beta 尝鲜系统等） */
  static async postJson(url: string, body: Record<string, string>): Promise<Object> {
    const httpRequest = http.createHttp();
    try {
      const res: http.HttpResponse = await httpRequest.request(url, {
        method: http.RequestMethod.POST,
        header: {
          'Content-Type': 'application/json',
          'User-Agent': 'GlassSuite'
        },
        extraData: JSON.stringify(body),
        expectDataType: http.HttpDataType.JSON,
        connectTimeout: 15000,
        readTimeout: 30000
      });
      if (res.responseCode !== 200 && res.responseCode !== 201) {
        throw new Error(`HTTP ${res.responseCode}`);
      }
      return JSON.parse(res.result as string) as Object;
    } finally {
      httpRequest.destroy();
    }
  }

  /** 通用 JSON 请求（GitHub 等外部 API，支持数组返回） */
  static async fetchJson(url: string): Promise<Object> {
    const httpRequest = http.createHttp();
    try {
      const res: http.HttpResponse = await httpRequest.request(url, {
        method: http.RequestMethod.GET,
        header: { 'User-Agent': 'GlassSuite' },
        expectDataType: http.HttpDataType.JSON,
        connectTimeout: 15000,
        readTimeout: 30000
      });
      if (res.responseCode !== 200) {
        throw new Error(`HTTP ${res.responseCode}`);
      }
      return JSON.parse(res.result as string) as Object;
    } finally {
      httpRequest.destroy();
    }
  }

  /** 通用文本请求（公告 md 等） */
  static async fetchText(url: string): Promise<string> {
    const httpRequest = http.createHttp();
    try {
      const res: http.HttpResponse = await httpRequest.request(url, {
        method: http.RequestMethod.GET,
        header: { 'User-Agent': 'GlassSuite' },
        expectDataType: http.HttpDataType.STRING,
        connectTimeout: 15000,
        readTimeout: 30000
      });
      if (res.responseCode !== 200) {
        throw new Error(`HTTP ${res.responseCode}`);
      }
      return res.result as string;
    } finally {
      httpRequest.destroy();
    }
  }

  /** 二进制下载（封面、MV 无需；二维码 base64 由 JSON 返回） */
  static async fetchBuffer(url: string): Promise<ArrayBuffer> {
    const httpRequest = http.createHttp();
    try {
      const res: http.HttpResponse = await httpRequest.request(url, {
        method: http.RequestMethod.GET,
        header: { 'User-Agent': Constants.UA },
        expectDataType: http.HttpDataType.ARRAY_BUFFER,
        connectTimeout: 15000,
        readTimeout: 30000
      });
      return res.result as ArrayBuffer;
    } finally {
      httpRequest.destroy();
    }
  }
}
