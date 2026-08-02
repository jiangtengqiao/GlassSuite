/** 全局常量与用户配置（内存镜像，持久化于 preferences） */
export class Constants {
  static DEFAULT_API: string = 'http://10.0.2.2:3000';
  static UA: string =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';

  static QUALITY_LEVELS: string[] = ['standard', 'higher', 'exhigh', 'lossless'];
  static QUALITY_NAMES: Record<string, string> = {
    'standard': '标准品质',
    'higher': '较高品质',
    'exhigh': '极高品质',
    'lossless': '无损品质'
  };

  // 运行期配置（内存镜像）
  static apiBaseUrl: string = Constants.DEFAULT_API;
  static accentHex: string = '#C62F2F';
  static darkMode: boolean = false;
  static lyricFontSize: number = 18;
  static lyricOffsetMs: number = 0;
  static lyricMode: number = 0; // 0 原词 1 翻译 2 罗马音 3 DIY
  static diyLyric: string = '';
  static quality: string = 'exhigh';
  static userId: number = 0;
  static profileJson: string = '';

  static isLoggedIn(): boolean {
    return Constants.userId > 0;
  }
}
