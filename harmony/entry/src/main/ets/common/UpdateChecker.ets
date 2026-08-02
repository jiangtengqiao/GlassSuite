/**
 * 更新与公告中心（鸿蒙端）：
 * - 实时自查更新：启动即查 + 每 30 分钟轮询 GitHub Releases
 * - 在线公告拉取与解析
 * - 新版本/新公告回调（供页面弹窗推送）
 */
import { GitHubApi, GitHubRelease, Announcement } from './GitHubApi';
import { Constants } from './Constants';

export class UpdateChecker {
  private static instance: UpdateChecker = new UpdateChecker();
  static getInstance(): UpdateChecker {
    return UpdateChecker.instance;
  }

  static VERSION: string = '1.1.0';
  static INTERVAL_MS: number = 30 * 60 * 1000;

  latest: GitHubRelease | undefined = undefined;
  hasUpdate: boolean = false;
  checking: boolean = false;
  announcements: Announcement[] = [];
  newAnnouncements: number = 0;
  private listeners: Array<() => void> = [];
  private started: boolean = false;

  onChange(cb: () => void): void {
    this.listeners.push(cb);
  }

  private emit(): void {
    this.listeners.forEach((cb: () => void) => {
      cb();
    });
  }

  start(): void {
    if (this.started) return;
    this.started = true;
    this.checkNow();
    setInterval(() => {
      this.checkNow();
    }, UpdateChecker.INTERVAL_MS);
  }

  async checkNow(): Promise<boolean> {
    if (this.checking) return this.hasUpdate;
    this.checking = true;
    try {
      const release = await GitHubApi.latestRelease();
      if (release !== undefined) {
        const prev = this.latest === undefined ? '' : this.latest.tagName;
        this.latest = release;
        // 渠道过滤：无尝鲜权限仅正式版可被监测推送
        const betaOk = BetaStore.betaAccess || !UpdateChecker.isPreRelease(release.tagName);
        this.hasUpdate = betaOk && GitHubApi.isNewer(release.tagName, UpdateChecker.VERSION);
        if (this.hasUpdate && prev !== release.tagName) {
          // 新版本事件：由页面监听 hasUpdate 弹窗
          this.emit();
        }
      }
      // 公告同步拉取
      const md = await GitHubApi.fetchAnnouncements();
      const list = GitHubApi.parseAnnouncements(md);
      if (list.length > 0) {
        const oldIds: Set<string> = new Set(this.announcements.map((a: Announcement) => a.id));
        const fresh: Announcement[] = list.filter((a: Announcement) => !oldIds.has(a.id));
        this.announcements = list;
        if (fresh.length > 0) {
          this.newAnnouncements = fresh.length;
        }
      }
    } catch (e) {
    }
    this.checking = false;
    this.emit();
    return this.hasUpdate;
  }

  markSeen(): void {
    this.newAnnouncements = 0;
    this.emit();
  }
}
