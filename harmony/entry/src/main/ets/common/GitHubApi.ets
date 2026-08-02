/**
 * GitHub REST API 客户端（鸿蒙端）：搜索/热门仓库、Releases、公告拉取
 * 公开接口无需鉴权；公告优先走 jsDelivr CDN（国内可直连），失败回退 raw.githubusercontent
 */
import { Net } from './Net';
import { Constants } from './Constants';

export class GitHubRepo {
  id: number = 0;
  fullName: string = '';
  description: string = '';
  stargazersCount: number = 0;
  language: string = '';
  htmlUrl: string = '';

  static from(r: Record<string, Object>): GitHubRepo {
    const g = new GitHubRepo();
    g.id = Number(r['id'] ?? 0);
    g.fullName = String(r['full_name'] ?? '');
    g.description = String(r['description'] ?? '');
    g.stargazersCount = Number(r['stargazers_count'] ?? 0);
    g.language = String(r['language'] ?? '');
    g.htmlUrl = String(r['html_url'] ?? '');
    return g;
  }
}

export class GitHubRelease {
  tagName: string = '';
  name: string = '';
  body: string = '';
  publishedAt: string = '';
  htmlUrl: string = '';
  prerelease: boolean = false;

  static from(r: Record<string, Object>): GitHubRelease {
    const g = new GitHubRelease();
    g.tagName = String(r['tag_name'] ?? '');
    g.name = String(r['name'] ?? '');
    g.body = String(r['body'] ?? '');
    g.publishedAt = String(r['published_at'] ?? '');
    g.htmlUrl = String(r['html_url'] ?? '');
    g.prerelease = r['prerelease'] === true;
    return g;
  }
}

export class Announcement {
  id: string = '';
  title: string = '';
  content: string = '';
  isPromo: boolean = false;
  url: string = '';
  date: string = '';
}

export class GitHubApi {
  static async searchRepos(query: string, limit: number = 30): Promise<GitHubRepo[]> {
    const q = query.trim().length > 0 ? query : 'stars:>10000';
    const url = `https://api.github.com/search/repositories?q=${encodeURIComponent(q)}&sort=stars&order=desc&per_page=${limit}`;
    const r = await Net.fetchJson(url);
    const items = r['items'] as Array<Object>;
    return Array.isArray(items) ? items.map((it: Object) => GitHubRepo.from(it as Record<string, Object>)) : [];
  }

  static async releases(limit: number = 10): Promise<GitHubRelease[]> {
    const url = `https://api.github.com/repos/${Constants.ghOwner}/${Constants.ghRepo}/releases?per_page=${limit}`;
    const arr = await Net.fetchJson(url) as Array<Object>;
    return Array.isArray(arr) ? arr.map((it: Object) => GitHubRelease.from(it as Record<string, Object>)) : [];
  }

  static async latestRelease(): Promise<GitHubRelease | undefined> {
    const list = await GitHubApi.releases(1);
    return list.length > 0 ? list[0] : undefined;
  }

  /** 拉取在线公告（jsDelivr 优先，raw 兜底） */
  static async fetchAnnouncements(): Promise<string> {
    const urls: string[] = [
      `https://cdn.jsdelivr.net/gh/${Constants.ghOwner}/${Constants.ghRepo}@main/ANNOUNCEMENTS.md`,
      `https://raw.githubusercontent.com/${Constants.ghOwner}/${Constants.ghRepo}/main/ANNOUNCEMENTS.md`,
    ];
    for (let i = 0; i < urls.length; i++) {
      try {
        const text = await Net.fetchText(urls[i]);
        if (text.length > 0) return text;
      } catch (e) {
      }
    }
    return '';
  }

  /** 解析 ANNOUNCEMENTS.md：## [公告|推广] 标题 + 正文 + - 链接/日期 */
  static parseAnnouncements(md: string): Announcement[] {
    const list: Announcement[] = [];
    if (md.length === 0) return list;
    let cur: Announcement | undefined = undefined;
    md.split('\n').forEach((raw: string) => {
      const line = raw.trim();
      if (line.startsWith('## ')) {
        if (cur !== undefined) list.push(cur);
        const title = line.substring(3).trim();
        cur = new Announcement();
        cur.id = title + '_' + list.length;
        cur.isPromo = title.startsWith('[推广]');
        cur.title = title.replace('[公告] ', '').replace('[推广] ', '').trim();
      } else if (cur !== undefined) {
        if (line.startsWith('- 链接:')) {
          cur.url = line.substring(5).trim();
        } else if (line.startsWith('- 日期:')) {
          cur.date = line.substring(5).trim();
        } else if (!line.startsWith('#') && line.length > 0) {
          cur.content += line + '\n';
        }
      }
    });
    if (cur !== undefined) list.push(cur);
    return list;
  }

  static builtinAnnouncements(): Announcement[] {
    const a = new Announcement();
    a.id = 'builtin-welcome';
    a.title = '欢迎使用璃光 GlassSuite';
    a.content = '多功能应用套件：音乐、GitHub 检索、更新中心与公告推送。\n音乐模块需在设置中配置网易云接口服务地址。';
    a.date = '2026-08-02';
    return [a];
  }

  static isNewer(tag: string, current: string): boolean {
    const a = tag.replace(/^v/, '').split('.').map((x: string) => parseInt(x) || 0);
    const b = current.split('.').map((x: string) => parseInt(x) || 0);
    const n = Math.max(a.length, b.length);
    for (let i = 0; i < n; i++) {
      const x = i < a.length ? a[i] : 0;
      const y = i < b.length ? b[i] : 0;
      if (x !== y) return x > y;
    }
    return false;
  }
}
