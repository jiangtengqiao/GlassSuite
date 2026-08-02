/**
 * 全局播放器：AVPlayer 单例 + AVSession 系统媒体中心集成
 */
import { media } from '@kit.MediaKit';
import { mediaSession } from '@kit.AVSessionKit';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { image } from '@kit.ImageKit';
import { Api } from './Api';
import { Constants } from './Constants';
import { LrcLine, LrcParser } from './LrcParser';
import { Song } from '../model/Models';
import { Utils } from './Utils';

export class PlayerManager {
  private static instance: PlayerManager = new PlayerManager();
  static getInstance(): PlayerManager {
    return PlayerManager.instance;
  }

  private context: common.UIAbilityContext | undefined = undefined;
  private avPlayer: media.AVPlayer | undefined = undefined;
  private avSession: mediaSession.AVSession | undefined = undefined;
  private sessionReady: boolean = false;

  queue: Song[] = [];
  index: number = -1;
  currentSong: Song | undefined = undefined;
  playing: boolean = false;
  position: number = 0;
  duration: number = 0;
  quality: string = 'exhigh';
  repeatOne: boolean = false;
  shuffle: boolean = false;
  lyric: LrcLine[] = [];
  tlyric: LrcLine[] = [];
  romalrc: LrcLine[] = [];
  likedIds: Set<number> = new Set();

  private listeners: Array<() => void> = [];

  onChange(cb: () => void): void {
    this.listeners.push(cb);
  }

  private emit(): void {
    this.listeners.forEach((cb: () => void) => {
      cb();
    });
  }

  toast(msg: string): void {
    this.emit();
    console.info(`[PlayerManager] ${msg}`);
  }

  init(context: common.UIAbilityContext): void {
    this.context = context;
    this.quality = Constants.quality;
    this.ensurePlayer();
    this.ensureSession();
  }

  private ensurePlayer(): void {
    if (this.avPlayer !== undefined) return;
    media.createAVPlayer().then((player: media.AVPlayer) => {
      this.avPlayer = player;
      player.on('stateChange', (state: string) => {
        console.info(`[AVPlayer] state: ${state}`);
        if (state === 'prepared') {
          this.duration = player.duration;
          player.play();
        }
        if (state === 'completed' || state === 'stopped') {
          // endOfStream 事件处理
        }
      });
      player.on('timeUpdate', (time: number) => {
        this.position = time;
        this.emit();
      });
      player.on('durationUpdate', (dur: number) => {
        this.duration = dur;
      });
      player.on('endOfStream', () => {
        if (this.repeatOne) {
          player.seek(0, media.SeekMode.SEEK_PREV_SYNC);
          player.play();
        } else {
          this.next();
        }
      });
      player.on('error', (err: BusinessError) => {
        console.error(`[AVPlayer] error: ${err.code} ${err.message}`);
        this.downgradeAndRetry();
      });
    }).catch((err: BusinessError) => {
      console.error(`[AVPlayer] create error: ${err.code}`);
    });
  }

  private ensureSession(): void {
    if (this.context === undefined) return;
    mediaSession.createAVSession(this.context, 'cloudmusic', mediaSession.Type.AUDIO).then((session: mediaSession.AVSession) => {
      this.avSession = session;
      this.sessionReady = true;
      session.on('play', () => {
        this.resume();
      });
      session.on('pause', () => {
        this.pause();
      });
      session.on('next', () => {
        this.next();
      });
      session.on('previous', () => {
        this.prev();
      });
      session.on('seek', (time: number) => {
        this.seek(time);
      });
      session.activate();
    }).catch((err: BusinessError) => {
      console.error(`[AVSession] create error: ${err.code}`);
    });
  }

  private setSource(url: string): void {
    const p = this.avPlayer;
    if (p === undefined) return;
    // API 12+：使用 AVMediaSource 设置网络音源
    try {
      p.setMediaSource(new media.AVMediaSource(url));
    } catch (e) {
      console.error(`[PlayerManager] setMediaSource error: ${JSON.stringify(e)}`);
    }
    p.prepare();
  }

  // ==================== 对外操作 ====================

  playQueue(songs: Song[], start: number): void {
    if (songs.length === 0) return;
    this.queue = songs.slice();
    this.loadIndex(Math.max(0, Math.min(start, songs.length - 1)));
  }

  playSong(song: Song): void {
    this.playQueue([song], 0);
  }

  toggle(): void {
    const p = this.avPlayer;
    if (p === undefined) return;
    if (this.playing) {
      p.pause();
    } else {
      p.play();
    }
  }

  resume(): void {
    this.avPlayer?.play();
  }

  pause(): void {
    this.avPlayer?.pause();
  }

  next(): void {
    if (this.queue.length === 0) return;
    let target = 0;
    if (this.shuffle) {
      const candidates: number[] = [];
      for (let i = 0; i < this.queue.length; i++) {
        if (i !== this.index) candidates.push(i);
      }
      target = candidates.length > 0 ? candidates[Math.floor(Math.random() * candidates.length)] : 0;
    } else {
      target = (this.index + 1) >= this.queue.length ? 0 : this.index + 1;
    }
    this.loadIndex(target);
  }

  prev(): void {
    if (this.queue.length === 0) return;
    const target = (this.index - 1) < 0 ? this.queue.length - 1 : this.index - 1;
    this.loadIndex(target);
  }

  seek(ms: number): void {
    this.avPlayer?.seek(Math.max(0, ms), media.SeekMode.SEEK_PREV_SYNC);
  }

  toggleRepeatOne(): void {
    this.repeatOne = !this.repeatOne;
    if (this.repeatOne) this.shuffle = false;
  }

  toggleShuffle(): void {
    this.shuffle = !this.shuffle;
    if (this.shuffle) this.repeatOne = false;
  }

  setQuality(level: string): void {
    if (this.quality === level) return;
    this.quality = level;
    Constants.quality = level;
    const pos = this.position;
    if (this.index >= 0) {
      this.loadIndex(this.index, pos);
    }
  }

  isLiked(id: number): boolean {
    return this.likedIds.has(id);
  }

  toggleLike(id: number): void {
    Api.like(id, !this.isLiked(id)).then((code: number) => {
      if (code === 200) {
        if (this.likedIds.has(id)) {
          this.likedIds.delete(id);
        } else {
          this.likedIds.add(id);
        }
        this.emit();
      }
    }).catch(() => {
      console.error('[PlayerManager] like failed');
    });
  }

  // ==================== 内部 ====================

  private loadIndex(i: number, resumePosition: number = -1): void {
    if (i < 0 || i >= this.queue.length) return;
    this.index = i;
    this.currentSong = this.queue[i];
    this.playing = false;
    this.position = 0;
    const song = this.currentSong;
    if (song === undefined) return;
    this.emit();

    this.resolveUrl(song.id).then((url: string) => {
      if (url.length === 0) {
        console.warn(`[PlayerManager] 无音源: ${song.name}`);
        setTimeout(() => {
          if (this.currentSong !== undefined && this.currentSong.id === song.id) {
            this.next();
          }
        }, 1200);
        return;
      }
      this.setSource(url);
      if (resumePosition > 0) {
        setTimeout(() => {
          this.seek(resumePosition);
        }, 800);
      }
      this.playing = true;
      this.emit();
      this.updateSession(song, url);
    });

    Api.lyric(song.id).then((lrc: Record<string, string>) => {
      this.lyric = LrcParser.parse(lrc['lrc'] ?? '');
      this.tlyric = LrcParser.parse(lrc['tlyric'] ?? '');
      this.romalrc = LrcParser.parse(lrc['romalrc'] ?? '');
      this.emit();
    }).catch(() => {
      this.lyric = [];
      this.tlyric = [];
      this.romalrc = [];
    });

    if (Constants.userId > 0) {
      Api.likeList(Constants.userId).then((ids: number[]) => {
        this.likedIds = new Set(ids);
        this.emit();
      }).catch(() => {
      });
    }
  }

  /** 按当前音质请求直链，失败逐级降档 */
  private async resolveUrl(songId: number): Promise<string> {
    const levels = Constants.QUALITY_LEVELS;
    let startIdx = levels.indexOf(this.quality);
    if (startIdx < 0) startIdx = levels.length - 1;
    for (let idx = startIdx; idx >= 0; idx--) {
      try {
        const url = await Api.songUrl(songId, levels[idx]);
        if (url.length > 0) return url;
      } catch (e) {
        console.error(`[PlayerManager] songUrl error: ${JSON.stringify(e)}`);
      }
    }
    return '';
  }

  private downgradeAndRetry(): void {
    const levels = Constants.QUALITY_LEVELS;
    const idx = levels.indexOf(this.quality);
    if (idx > 0) {
      this.quality = levels[idx - 1];
      Constants.quality = this.quality;
      if (this.index >= 0) {
        this.loadIndex(this.index, this.position);
      }
    }
  }

  // ==================== AVSession ====================

  private updateSession(song: Song, url: string): void {
    if (!this.sessionReady || this.avSession === undefined) return;
    const meta: mediaSession.AVMetadata = {
      title: song.name,
      artist: song.artistNames(),
      album: song.album.name
    };
    this.avSession.setAVMetadata(meta).catch(() => {
    });
    // 异步加载封面到媒体中心
    const pic = song.albumPic();
    if (pic.length > 0) {
      Utils.urlToPixelMap(pic).then((pm: image.PixelMap | undefined) => {
        if (pm !== undefined) {
          this.avSession?.setAVMetadata({
            title: song.name,
            artist: song.artistNames(),
            album: song.album.name,
            mediaImage: [{ width: 300, height: 300, pixelMap: pm }]
          }).catch(() => {
          });
        }
      });
    }
  }

  updatePlaybackState(state: mediaSession.PlaybackState, position: number, duration: number): void {
    if (!this.sessionReady || this.avSession === undefined) return;
    this.avSession.setAVPlaybackState({
      state: state,
      position: position,
      duration: duration,
      speed: 1.0
    }).catch(() => {
    });
  }

  syncSessionState(): void {
    if (this.currentSong === undefined) return;
    if (this.playing) {
      this.updatePlaybackState(mediaSession.PlaybackState.PLAYING, this.position, this.duration);
    } else {
      this.updatePlaybackState(mediaSession.PlaybackState.PAUSED, this.position, this.duration);
    }
  }
}
