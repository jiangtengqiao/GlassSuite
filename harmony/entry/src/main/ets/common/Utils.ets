/** 通用工具 */
import { image } from '@kit.ImageKit';
import { util } from '@kit.ArkTS';
import { Net } from './Net';

export class Utils {
  /** data:image/png;base64,xxx -> PixelMap */
  static async base64DataUriToPixelMap(uri: string): Promise<image.PixelMap | undefined> {
    try {
      if (uri.length === 0) return undefined;
      const idx = uri.indexOf(',');
      const b64 = idx >= 0 ? uri.substring(idx + 1) : uri;
      const helper = new util.Base64Helper();
      const bytes = helper.decodeSync(b64);
      const src = image.createImageSource(bytes);
      const pm = await src.createPixelMap();
      src.release();
      return pm;
    } catch (e) {
      console.error(`[Utils] decode base64 error: ${JSON.stringify(e)}`);
      return undefined;
    }
  }

  /** 网络图片 URL -> PixelMap（用于媒体中心封面） */
  static async urlToPixelMap(url: string): Promise<image.PixelMap | undefined> {
    try {
      const buffer = await Net.fetchBuffer(url);
      const src = image.createImageSource(buffer);
      const pm = await src.createPixelMap();
      src.release();
      return pm;
    } catch (e) {
      console.error(`[Utils] urlToPixelMap error: ${JSON.stringify(e)}`);
      return undefined;
    }
  }

  static formatDuration(ms: number): string {
    if (ms <= 0) return '00:00';
    const total = Math.floor(ms / 1000);
    const m = Math.floor(total / 60);
    const s = total % 60;
    return `${m < 10 ? '0' + m : m}:${s < 10 ? '0' + s : s}`;
  }

  static formatPlayCount(count: number): string {
    if (count >= 100000000) return (count / 100000000).toFixed(1) + '亿';
    if (count >= 10000) return (count / 10000).toFixed(1) + '万';
    return String(count);
  }

  /** 从路由参数取 id */
  static paramId(params: Object, key: string): number {
    const r = params as Record<string, Object>;
    const v = r[key];
    return v === undefined || v === null ? 0 : Number(v);
  }
}
