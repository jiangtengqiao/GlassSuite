import { router, promptAction } from '@kit.ArkUI';
import { PlayerManager } from '../common/PlayerManager';
import { Constants } from '../common/Constants';
import { LrcLine, LrcParser } from '../common/LrcParser';
import { Net } from '../common/Net';
import { Utils } from '../common/Utils';

@Entry
@Component
struct PlayerPage {
  @State songName: string = '';
  @State artist: string = '';
  @State cover: string = '';
  @State playing: boolean = false;
  @State position: number = 0;
  @State duration: number = 0;
  @State quality: string = Constants.quality;
  @State repeatOne: boolean = false;
  @State shuffle: boolean = false;
  @State liked: boolean = false;
  @State lyricLines: LrcLine[] = [];
  @State curIndex: number = -1;
  @State lyricMode: number = Constants.lyricMode;
  @State lyricFontSize: number = Constants.lyricFontSize;
  @State lyricOffset: number = Constants.lyricOffsetMs;
  @State diyText: string = Constants.diyLyric;
  @State showQuality: boolean = false;
  @State showLyricSet: boolean = false;
  @State showQueue: boolean = false;
  @State queueNames: string[] = [];
  @State queueIndex: number = -1;
  private scroller: Scroller = new Scroller();
  private pm = PlayerManager.getInstance();
  private refreshTimer: number = -1;
  private diyParsed: LrcLine[] = [];

  aboutToAppear(): void {
    this.sync();
    this.refreshTimer = setInterval(() => {
      this.sync();
    }, 500);
  }

  aboutToDisappear(): void {
    if (this.refreshTimer >= 0) clearInterval(this.refreshTimer);
  }

  sync(): void {
    const m = this.pm;
    this.songName = m.currentSong === undefined ? '' : m.currentSong.name;
    this.artist = m.currentSong === undefined ? '' : m.currentSong.artistNames();
    this.cover = m.currentSong === undefined ? '' : m.currentSong.albumPic();
    this.playing = m.playing;
    this.position = m.position;
    this.duration = m.duration;
    this.quality = m.quality;
    this.repeatOne = m.repeatOne;
    this.shuffle = m.shuffle;
    this.liked = m.currentSong === undefined ? false : m.isLiked(m.currentSong.id);

    // 歌词行（按模式）
    let lines: LrcLine[] = [];
    if (this.lyricMode === 3) {
      lines = this.diyParsed.length > 0 ? this.diyParsed : m.lyric;
    } else if (this.lyricMode === 1) {
      lines = m.lyric;
    } else if (this.lyricMode === 2) {
      lines = m.romalrc.length > 0 ? m.romalrc : m.lyric;
    } else {
      lines = m.lyric;
    }
    this.lyricLines = lines;
    // 当前行
    const adj = this.position + this.lyricOffset;
    let idx = -1;
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].time <= adj) idx = i;
      else break;
    }
    if (idx !== this.curIndex) {
      this.curIndex = idx;
      if (idx >= 2) {
        this.scroller.scrollToIndex(Math.max(0, idx - 2), false);
      }
    }
    // 队列信息
    this.queueNames = m.queue.map((s) => s.name);
    this.queueIndex = m.index;
  }

  togglePlay(): void {
    this.pm.toggle();
  }

  seekBySlider(value: number): void {
    this.pm.seek(value);
  }

  applyOffset(delta: number): void {
    this.lyricOffset = Math.max(-10000, Math.min(10000, this.lyricOffset + delta));
    Constants.lyricOffsetMs = this.lyricOffset;
    Net.put('lyric_offset', this.lyricOffset);
  }

  resetOffset(): void {
    this.lyricOffset = 0;
    Constants.lyricOffsetMs = 0;
    Net.put('lyric_offset', 0);
  }

  applyFontSize(delta: number): void {
    this.lyricFontSize = Math.max(12, Math.min(32, this.lyricFontSize + delta));
    Constants.lyricFontSize = this.lyricFontSize;
    Net.put('lyric_size', this.lyricFontSize);
  }

  setMode(mode: number): void {
    this.lyricMode = mode;
    Constants.lyricMode = mode;
    Net.put('lyric_mode', mode);
    this.sync();
  }

  saveDiy(): void {
    Constants.diyLyric = this.diyText;
    Net.put('diy_lyric', this.diyText);
    this.diyParsed = LrcParser.parse(this.diyText);
    this.sync();
    promptAction.showToast({ message: 'DIY 歌词已应用' });
  }

  setQuality(level: string): void {
    this.showQuality = false;
    this.pm.setQuality(level);
    promptAction.showToast({ message: `已切换至${Constants.QUALITY_NAMES[level] ?? level}` });
  }

  playQueueAt(index: number): void {
    const m = this.pm;
    if (index < 0 || index >= m.queue.length) return;
    this.showQueue = false;
    m.playQueue(m.queue, index);
  }

  @Builder
  TopBar() {
    Row() {
      Text('‹').fontSize(28).onClick(() => {
        router.back();
      }).width(40).height(40).textAlign(TextAlign.Center)
      Column({ space: 2 }) {
        Text(this.songName).fontSize(17).fontWeight(FontWeight.Bold).maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(this.artist).fontSize(12).fontColor('#999999').maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Center)
      Blank().width(40)
    }
    .width('100%')
    .height(52)
    .alignItems(VerticalAlign.Center)
  }

  @Builder
  CoverArea() {
    Column() {
      Image(this.cover)
        .width(220)
        .height(220)
        .borderRadius(16)
        .objectFit(ImageFit.Cover)
        .shadow({ radius: 20, color: '#33000000', offsetY: 8 })
      Text(Utils.formatDuration(this.position) + ' / ' + Utils.formatDuration(this.duration))
        .fontSize(12)
        .fontColor('#999999')
        .margin({ top: 12 })
    }
    .width('100%')
    .justifyContent(FlexAlign.Center)
    .padding({ top: 16, bottom: 8 })
  }

  @Builder
  LyricArea() {
    List({ scroller: this.scroller, space: 10 }) {
      ForEach(this.lyricLines, (line: LrcLine, idx: number) => {
        ListItem() {
          Text(line.text.length > 0 ? line.text : '…')
            .fontSize(this.lyricFontSize)
            .fontWeight(idx === this.curIndex ? FontWeight.Bold : FontWeight.Normal)
            .fontColor(idx === this.curIndex ? '#C62F2F' : '#888888')
            .textAlign(TextAlign.Center)
            .width('100%')
            .padding({ left: 24, right: 24 })
            .onClick(() => {
              this.pm.seek(line.time - this.lyricOffset);
            })
        }
      }, (line: LrcLine, idx: number) => `${line.time}_${idx}`)
    }
    .layoutWeight(1)
    .scrollBar(BarState.Off)
  }

  @Builder
  Controls() {
    Column() {
      Slider({
        value: this.duration > 0 ? this.position : 0,
        min: 0,
        max: this.duration > 0 ? this.duration : 1,
        step: 1000
      })
        .onChange((value: number) => {
          this.seekBySlider(value);
        })
        .blockColor('#C62F2F')
        .selectedColor('#C62F2F')

      Row({ space: 28 }) {
        Text(this.repeatOne ? '单曲' : '顺序')
          .fontSize(12)
          .fontColor(this.repeatOne ? '#C62F2F' : '#666666')
          .onClick(() => {
            this.pm.toggleRepeatOne();
          })
        Text('⏮').fontSize(30).onClick(() => {
          this.pm.prev();
        })
        Text(this.playing ? '⏸' : '▶')
          .fontSize(44)
          .fontColor('#C62F2F')
          .onClick(() => {
            this.togglePlay();
          })
        Text('⏭').fontSize(30).onClick(() => {
          this.pm.next();
        })
        Text(this.shuffle ? '随机' : '列表')
          .fontSize(12)
          .fontColor(this.shuffle ? '#C62F2F' : '#666666')
          .onClick(() => {
            this.pm.toggleShuffle();
          })
      }
      .width('100%')
      .justifyContent(FlexAlign.Center)
      .padding({ bottom: 10 })

      Row({ space: 24 }) {
        Text(this.liked ? '♥' : '♡')
          .fontSize(24)
          .fontColor(this.liked ? '#C62F2F' : '#999999')
          .onClick(() => {
            const s = this.pm.currentSong;
            if (s !== undefined) this.pm.toggleLike(s.id);
          })
        Text('音质')
          .fontSize(14)
          .fontColor('#666666')
          .onClick(() => {
            this.showQuality = true;
          })
        Text('歌词设置').fontSize(14).fontColor('#666666').onClick(() => {
          this.showLyricSet = true;
        })
        Text('队列').fontSize(14).fontColor('#666666').onClick(() => {
          this.showQueue = true;
        })
      }
      .width('100%')
      .justifyContent(FlexAlign.Center)
      .padding({ bottom: 24 })
    }
    .width('100%')
  }

  @Builder
  QualitySheet() {
    Column({ space: 8 }) {
      Text('选择音质').fontSize(16).fontWeight(FontWeight.Bold).margin({ bottom: 8 })
      ForEach(Constants.QUALITY_LEVELS, (level: string) => {
        Row() {
          Text(Constants.QUALITY_NAMES[level] ?? level).fontSize(15).layoutWeight(1)
          Text(this.quality === level ? '●' : '○').fontColor('#C62F2F')
        }
        .width('100%')
        .padding(12)
        .onClick(() => {
          this.setQuality(level);
        })
      }, (level: string) => level)
      Text('提示：无损/高解析音质需登录且为 VIP 曲目，受限时自动降档')
        .fontSize(11)
        .fontColor('#999999')
        .margin({ top: 8 })
    }
    .width('100%')
    .padding({ left: 24, right: 24, top: 16 })
  }

  @Builder
  LyricSetSheet() {
    Column({ space: 10 }) {
      Text('歌词设置').fontSize(16).fontWeight(FontWeight.Bold)

      Row({ space: 8 }) {
        Text('模式').fontSize(14)
        ForEach(['原词', '翻译', '罗马音', 'DIY'], (name: string, idx: number) => {
          Text(name)
            .fontSize(13)
            .fontColor(this.lyricMode === idx ? '#FFFFFF' : '#666666')
            .backgroundColor(this.lyricMode === idx ? '#C62F2F' : '#F2F2F2')
            .borderRadius(14)
            .padding({ left: 12, right: 12, top: 5, bottom: 5 })
            .onClick(() => {
              this.setMode(idx);
            })
        }, (name: string, idx: number) => `${name}_${idx}`)
      }

      Row({ space: 12 }) {
        Text('时间偏移').fontSize(14)
        Text('−0.5s').fontSize(14).fontColor('#C62F2F').onClick(() => {
          this.applyOffset(-500);
        })
        Text(`${(this.lyricOffset / 1000).toFixed(1)}s`).fontSize(14)
        Text('+0.5s').fontSize(14).fontColor('#C62F2F').onClick(() => {
          this.applyOffset(500);
        })
        Text('复位').fontSize(14).fontColor('#999999').onClick(() => {
          this.resetOffset();
        })
      }

      Row({ space: 12 }) {
        Text('字号').fontSize(14)
        Text('−').fontSize(20).fontColor('#C62F2F').onClick(() => {
          this.applyFontSize(-1);
        })
        Text(`${this.lyricFontSize}`).fontSize(14)
        Text('+').fontSize(20).fontColor('#C62F2F').onClick(() => {
          this.applyFontSize(1);
        })
      }

      TextArea({ text: this.diyText, placeholder: '粘贴 LRC 格式歌词（DIY）' })
        .height(120)
        .fontSize(13)
        .onChange((value: string) => {
          this.diyText = value;
        })
      Button('应用 DIY 歌词')
        .backgroundColor('#C62F2F')
        .width('100%')
        .onClick(() => {
          this.saveDiy();
        })
    }
    .width('100%')
    .padding({ left: 24, right: 24, top: 16, bottom: 16 })
  }

  @Builder
  QueueSheet() {
    Column({ space: 6 }) {
      Text(`播放队列（${this.queueNames.length}）`).fontSize(16).fontWeight(FontWeight.Bold).margin({ bottom: 8 })
      ForEach(this.queueNames, (name: string, idx: number) => {
        Row() {
          Text(idx === this.queueIndex ? '▶ ' : '').fontColor('#C62F2F').fontSize(14)
          Text(name)
            .fontSize(14)
            .fontColor(idx === this.queueIndex ? '#C62F2F' : '#333333')
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .layoutWeight(1)
        }
        .width('100%')
        .padding({ top: 8, bottom: 8 })
        .onClick(() => {
          this.playQueueAt(idx);
        })
      }, (name: string, idx: number) => `${name}_${idx}`)
    }
    .width('100%')
    .padding({ left: 24, right: 24, top: 16 })
  }

  build() {
    Column() {
      this.TopBar()
      if (this.showQuality) {
        this.QualitySheet()
      } else if (this.showLyricSet) {
        this.LyricSetSheet()
      } else if (this.showQueue) {
        this.QueueSheet()
      } else if (this.lyricLines.length > 0) {
        this.LyricArea()
      } else {
        this.CoverArea()
      }
      this.Controls()
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
