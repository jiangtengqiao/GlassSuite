/**
 * 数据模型：字段与网易云音乐接口 JSON 对齐
 */

function toStr(r: Record<string, Object>, key: string): string {
  const v = r[key];
  return v === undefined || v === null ? '' : String(v);
}

function toNum(r: Record<string, Object>, key: string): number {
  const v = r[key];
  if (v === undefined || v === null) return 0;
  const n = Number(v);
  return isNaN(n) ? 0 : n;
}

function toArr(r: Record<string, Object>, key: string): Array<Object> {
  const v = r[key];
  return Array.isArray(v) ? (v as Array<Object>) : [];
}

export class Artist {
  id: number = 0;
  name: string = '';
  picUrl: string = '';
  static from(r: Record<string, Object>): Artist {
    const a = new Artist();
    a.id = toNum(r, 'id');
    a.name = toStr(r, 'name');
    a.picUrl = toStr(r, 'picUrl');
    return a;
  }
}

export class Album {
  id: number = 0;
  name: string = '';
  picUrl: string = '';
  publishTime: number = 0;
  artist: Artist = new Artist();
  static from(r: Record<string, Object>): Album {
    const a = new Album();
    a.id = toNum(r, 'id');
    a.name = toStr(r, 'name');
    a.picUrl = toStr(r, 'picUrl');
    a.publishTime = toNum(r, 'publishTime');
    const ar = r['artist'];
    if (ar !== undefined && ar !== null) {
      a.artist = Artist.from(ar as Record<string, Object>);
    }
    return a;
  }
}

export class Song {
  id: number = 0;
  name: string = '';
  artists: Artist[] = [];
  album: Album = new Album();
  duration: number = 0;
  mvid: number = 0;
  fee: number = 0;
  static from(r: Record<string, Object>): Song {
    const s = new Song();
    s.id = toNum(r, 'id');
    s.name = toStr(r, 'name');
    s.duration = toNum(r, 'dt') > 0 ? toNum(r, 'dt') : toNum(r, 'duration');
    s.mvid = toNum(r, 'mv') > 0 ? toNum(r, 'mv') : toNum(r, 'mvid');
    s.fee = toNum(r, 'fee');
    const arts = toArr(r, 'artists');
    if (arts.length > 0) {
      s.artists = arts.map((it: Object) => Artist.from(it as Record<string, Object>));
    } else {
      const ar = toArr(r, 'ar');
      s.artists = ar.map((it: Object) => Artist.from(it as Record<string, Object>));
    }
    const al = r['album'];
    if (al !== undefined && al !== null) {
      s.album = Album.from(al as Record<string, Object>);
    } else if (r['al'] !== undefined && r['al'] !== null) {
      s.album = Album.from(r['al'] as Record<string, Object>);
    }
    return s;
  }

  artistNames(): string {
    return this.artists.map((a: Artist) => a.name).join(' / ');
  }

  albumPic(): string {
    return this.album.picUrl;
  }
}

export class Profile {
  userId: number = 0;
  nickname: string = '';
  avatarUrl: string = '';
  signature: string = '';
  follows: number = 0;
  followeds: number = 0;
  static from(r: Record<string, Object>): Profile {
    const p = new Profile();
    p.userId = toNum(r, 'userId');
    p.nickname = toStr(r, 'nickname');
    p.avatarUrl = toStr(r, 'avatarUrl');
    p.signature = toStr(r, 'signature');
    p.follows = toNum(r, 'follows');
    p.followeds = toNum(r, 'followeds');
    return p;
  }
}

export class Creator {
  userId: number = 0;
  nickname: string = '';
  avatarUrl: string = '';
  static from(r: Record<string, Object>): Creator {
    const c = new Creator();
    c.userId = toNum(r, 'userId');
    c.nickname = toStr(r, 'nickname');
    c.avatarUrl = toStr(r, 'avatarUrl');
    return c;
  }
}

export class Playlist {
  id: number = 0;
  name: string = '';
  coverImgUrl: string = '';
  playCount: number = 0;
  trackCount: number = 0;
  description: string = '';
  creator: Creator = new Creator();
  subscribed: boolean = false;
  tracks: Song[] = [];
  static from(r: Record<string, Object>): Playlist {
    const p = new Playlist();
    p.id = toNum(r, 'id');
    p.name = toStr(r, 'name');
    p.coverImgUrl = toStr(r, 'coverImgUrl');
    p.playCount = toNum(r, 'playCount');
    p.trackCount = toNum(r, 'trackCount');
    p.description = toStr(r, 'description');
    p.subscribed = r['subscribed'] === true;
    const c = r['creator'];
    if (c !== undefined && c !== null) {
      p.creator = Creator.from(c as Record<string, Object>);
    }
    p.tracks = toArr(r, 'tracks').map((it: Object) => Song.from(it as Record<string, Object>));
    return p;
  }
}

export class ToplistItem {
  id: number = 0;
  name: string = '';
  coverImgUrl: string = '';
  playCount: number = 0;
  updateFrequency: string = '';
  static from(r: Record<string, Object>): ToplistItem {
    const t = new ToplistItem();
    t.id = toNum(r, 'id');
    t.name = toStr(r, 'name');
    t.coverImgUrl = toStr(r, 'coverImgUrl');
    t.playCount = toNum(r, 'playCount');
    t.updateFrequency = toStr(r, 'updateFrequency');
    return t;
  }
}

export class CatItem {
  name: string = '';
  category: number = 0;
  static from(r: Record<string, Object>): CatItem {
    const c = new CatItem();
    c.name = toStr(r, 'name');
    c.category = toNum(r, 'category');
    return c;
  }
}

export class SongUrlInfo {
  id: number = 0;
  url: string = '';
  level: string = '';
  static from(r: Record<string, Object>): SongUrlInfo {
    const s = new SongUrlInfo();
    s.id = toNum(r, 'id');
    s.url = toStr(r, 'url');
    s.level = toStr(r, 'level');
    return s;
  }
}

export class MvInfo {
  id: number = 0;
  name: string = '';
  artistName: string = '';
  duration: number = 0;
  playCount: number = 0;
  cover: string = '';
  static from(r: Record<string, Object>): MvInfo {
    const m = new MvInfo();
    m.id = toNum(r, 'id');
    m.name = toStr(r, 'name');
    m.artistName = toStr(r, 'artistName');
    m.duration = toNum(r, 'duration');
    m.playCount = toNum(r, 'playCount');
    m.cover = toStr(r, 'cover');
    return m;
  }
}

export class MvUrlInfo {
  id: number = 0;
  url: string = '';
  r: number = 0;
  static from(r: Record<string, Object>): MvUrlInfo {
    const m = new MvUrlInfo();
    m.id = toNum(r, 'id');
    m.url = toStr(r, 'url');
    m.r = toNum(r, 'r');
    return m;
  }
}

export class HotWord {
  first: string = '';
  static from(r: Record<string, Object>): HotWord {
    const h = new HotWord();
    h.first = toStr(r, 'first');
    return h;
  }
}
