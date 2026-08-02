/**
 * API 接口封装：与 NeteaseCloudMusicApi 端点一一对应
 */
import { Net } from './Net';
import {
  Album, Artist, CatItem, HotWord, MvInfo, MvUrlInfo, Playlist, Profile,
  Song, SongUrlInfo, ToplistItem
} from '../model/Models';

export class QrKeyResult {
  unikey: string = '';
}

export class QrCreateResult {
  qrimg: string = '';
  qrurl: string = '';
}

export class QrCheckResult {
  code: number = 0;
  message: string = '';
  cookie: string = '';
}

export class LoginResult {
  code: number = 0;
  message: string = '';
  cookie: string = '';
  profile: Profile = new Profile();
}

export class Api {
  static async qrKey(): Promise<QrKeyResult> {
    const r = await Net.request('login/qr/key');
    const data = r['data'] as Record<string, Object>;
    const res = new QrKeyResult();
    if (data !== undefined && data !== null) res.unikey = String(data['unikey'] ?? '');
    return res;
  }

  static async qrCreate(key: string): Promise<QrCreateResult> {
    const r = await Net.request('login/qr/create', { 'key': key, 'qrimg': 'true' });
    const data = r['data'] as Record<string, Object>;
    const res = new QrCreateResult();
    if (data !== undefined && data !== null) {
      res.qrimg = String(data['qrimg'] ?? '');
      res.qrurl = String(data['qrurl'] ?? '');
    }
    return res;
  }

  static async qrCheck(key: string): Promise<QrCheckResult> {
    const r = await Net.request('login/qr/check', { 'key': key });
    const res = new QrCheckResult();
    res.code = Number(r['code'] ?? 0);
    res.message = String(r['message'] ?? '');
    res.cookie = String(r['cookie'] ?? '');
    return res;
  }

  static async captchaSent(phone: string): Promise<number> {
    const r = await Net.request('captcha/sent', { 'phone': phone, 'timestamp': String(Date.now()) });
    return Number(r['code'] ?? 0);
  }

  static async loginCellphone(phone: string, captcha: string): Promise<LoginResult> {
    const r = await Net.request('login/cellphone', {
      'phone': phone, 'captcha': captcha, 'timestamp': String(Date.now())
    });
    const res = new LoginResult();
    res.code = Number(r['code'] ?? 0);
    res.message = String(r['message'] ?? '');
    res.cookie = String(r['cookie'] ?? '');
    const p = r['profile'];
    if (p !== undefined && p !== null) res.profile = Profile.from(p as Record<string, Object>);
    return res;
  }

  static async loginStatus(): Promise<Profile> {
    const r = await Net.request('login/status');
    const data = r['data'] as Record<string, Object>;
    if (data !== undefined && data !== null) {
      const p = data['profile'];
      if (p !== undefined && p !== null) return Profile.from(p as Record<string, Object>);
    }
    const p2 = r['profile'];
    if (p2 !== undefined && p2 !== null) return Profile.from(p2 as Record<string, Object>);
    return new Profile();
  }

  static async userPlaylist(uid: number): Promise<Playlist[]> {
    const r = await Net.request('user/playlist', { 'uid': String(uid) });
    const list = r['playlist'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Playlist.from(it as Record<string, Object>)) : [];
  }

  static async likeList(uid: number): Promise<number[]> {
    const r = await Net.request('likelist', { 'uid': String(uid) });
    const ids = r['ids'] as Array<Object>;
    return Array.isArray(ids) ? ids.map((it: Object) => Number(it)) : [];
  }

  static async like(id: number, like: boolean): Promise<number> {
    const r = await Net.request('like', { 'id': String(id), 'like': like ? 'true' : 'false' });
    return Number(r['code'] ?? 0);
  }

  static async recommendResource(): Promise<Playlist[]> {
    const r = await Net.request('recommend/resource');
    const list = r['recommend'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Playlist.from(it as Record<string, Object>)) : [];
  }

  static async recommendSongs(): Promise<Song[]> {
    const r = await Net.request('recommend/songs');
    const data = r['data'] as Record<string, Object>;
    if (data === undefined || data === null) return [];
    const list = data['dailySongs'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Song.from(it as Record<string, Object>)) : [];
  }

  static async newsong(): Promise<Song[]> {
    const r = await Net.request('personalized/newsong', { 'limit': '30' });
    const list = r['result'] as Array<Object>;
    if (!Array.isArray(list)) return [];
    const songs: Song[] = [];
    list.forEach((it: Object) => {
      const item = it as Record<string, Object>;
      const s = item['song'];
      if (s !== undefined && s !== null) songs.push(Song.from(s as Record<string, Object>));
    });
    return songs;
  }

  static async toplist(): Promise<ToplistItem[]> {
    const r = await Net.request('toplist');
    const list = r['list'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => ToplistItem.from(it as Record<string, Object>)) : [];
  }

  static async hotPlaylist(cat: string): Promise<Playlist[]> {
    const r = await Net.request('top/playlist', { 'cat': cat, 'order': 'hot', 'limit': '50' });
    const list = r['playlists'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Playlist.from(it as Record<string, Object>)) : [];
  }

  static async catlist(): Promise<CatItem[]> {
    const r = await Net.request('playlist/catlist');
    const list = r['sub'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => CatItem.from(it as Record<string, Object>)) : [];
  }

  static async playlistDetail(id: number): Promise<Playlist> {
    const r = await Net.request('playlist/detail', { 'id': String(id) });
    const p = r['playlist'];
    return p !== undefined && p !== null ? Playlist.from(p as Record<string, Object>) : new Playlist();
  }

  static async playlistTracks(id: number): Promise<Song[]> {
    const r = await Net.request('playlist/track/all', { 'id': String(id), 'limit': '1000', 'offset': '0' });
    const list = r['songs'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Song.from(it as Record<string, Object>)) : [];
  }

  static async playlistSubscribe(id: number, t: number): Promise<number> {
    const r = await Net.request('playlist/subscribe', { 'id': String(id), 't': String(t) });
    return Number(r['code'] ?? 0);
  }

  static async songDetail(ids: string): Promise<Song[]> {
    const r = await Net.request('song/detail', { 'ids': ids });
    const list = r['songs'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Song.from(it as Record<string, Object>)) : [];
  }

  static async songUrl(id: number, level: string): Promise<string> {
    const r = await Net.request('song/url/v1', { 'id': String(id), 'level': level });
    const list = r['data'] as Array<Object>;
    if (Array.isArray(list) && list.length > 0) {
      const first = SongUrlInfo.from(list[0] as Record<string, Object>);
      return first.url;
    }
    return '';
  }

  static async lyric(id: number): Promise<Record<string, string>> {
    const r = await Net.request('lyric', { 'id': String(id) });
    const res: Record<string, string> = {};
    const lrc = r['lrc'] as Record<string, Object>;
    if (lrc !== undefined && lrc !== null) res['lrc'] = String(lrc['lyric'] ?? '');
    const tly = r['tlyric'] as Record<string, Object>;
    if (tly !== undefined && tly !== null) res['tlyric'] = String(tly['lyric'] ?? '');
    const roma = r['romalrc'] as Record<string, Object>;
    if (roma !== undefined && roma !== null) res['romalrc'] = String(roma['lyric'] ?? '');
    return res;
  }

  static async search(keywords: string, type: number): Promise<Song[]> {
    const r = await Net.request('search', { 'keywords': keywords, 'type': String(type), 'limit': '50' });
    const result = r['result'] as Record<string, Object>;
    if (result === undefined || result === null) return [];
    const list = result['songs'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Song.from(it as Record<string, Object>)) : [];
  }

  static async searchPlaylists(keywords: string): Promise<Playlist[]> {
    const r = await Net.request('search', { 'keywords': keywords, 'type': '1000', 'limit': '30' });
    const result = r['result'] as Record<string, Object>;
    if (result === undefined || result === null) return [];
    const list = result['playlists'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Playlist.from(it as Record<string, Object>)) : [];
  }

  static async searchArtists(keywords: string): Promise<Artist[]> {
    const r = await Net.request('search', { 'keywords': keywords, 'type': '100', 'limit': '30' });
    const result = r['result'] as Record<string, Object>;
    if (result === undefined || result === null) return [];
    const list = result['artists'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Artist.from(it as Record<string, Object>)) : [];
  }

  static async searchAlbums(keywords: string): Promise<Album[]> {
    const r = await Net.request('search', { 'keywords': keywords, 'type': '10', 'limit': '30' });
    const result = r['result'] as Record<string, Object>;
    if (result === undefined || result === null) return [];
    const list = result['albums'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Album.from(it as Record<string, Object>)) : [];
  }

  static async searchMvs(keywords: string): Promise<MvInfo[]> {
    const r = await Net.request('search', { 'keywords': keywords, 'type': '1004', 'limit': '30' });
    const result = r['result'] as Record<string, Object>;
    if (result === undefined || result === null) return [];
    const list = result['mvs'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => MvInfo.from(it as Record<string, Object>)) : [];
  }

  static async hotSearch(): Promise<HotWord[]> {
    const r = await Net.request('search/hot');
    const result = r['result'] as Record<string, Object>;
    if (result === undefined || result === null) return [];
    const list = result['hots'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => HotWord.from(it as Record<string, Object>)) : [];
  }

  static async artist(id: number): Promise<{ artist: Artist, hotSongs: Song[] }> {
    const r = await Net.request('artist', { 'id': String(id) });
    const artist = Artist.from((r['artist'] as Record<string, Object>) ?? {});
    const list = r['hotSongs'] as Array<Object>;
    const songs = Array.isArray(list) ? list.map((it: Object) => Song.from(it as Record<string, Object>)) : [];
    return { 'artist': artist, 'hotSongs': songs };
  }

  static async artistAlbums(id: number): Promise<Album[]> {
    const r = await Net.request('artist/album', { 'id': String(id), 'limit': '30' });
    const list = r['hotAlbums'] as Array<Object>;
    return Array.isArray(list) ? list.map((it: Object) => Album.from(it as Record<string, Object>)) : [];
  }

  static async album(id: number): Promise<{ album: Album, songs: Song[] }> {
    const r = await Net.request('album', { 'id': String(id) });
    const album = Album.from((r['album'] as Record<string, Object>) ?? {});
    const list = r['songs'] as Array<Object>;
    const songs = Array.isArray(list) ? list.map((it: Object) => Song.from(it as Record<string, Object>)) : [];
    return { 'album': album, 'songs': songs };
  }

  static async mvDetail(id: number): Promise<MvInfo> {
    const r = await Net.request('mv/detail', { 'mvid': String(id) });
    const data = r['data'] as Record<string, Object>;
    return data !== undefined && data !== null ? MvInfo.from(data) : new MvInfo();
  }

  static async mvUrl(id: number): Promise<string> {
    const r = await Net.request('mv/url', { 'id': String(id), 'r': '1080' });
    const data = r['data'] as Record<string, Object>;
    if (data !== undefined && data !== null) {
      return MvUrlInfo.from(data).url;
    }
    return '';
  }
}
