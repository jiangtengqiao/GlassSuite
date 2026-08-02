import { router, promptAction } from '@kit.ArkUI';
import { Api } from '../common/Api';
import { Constants } from '../common/Constants';
import { Net } from '../common/Net';
import { PlayerManager } from '../common/PlayerManager';
import { Playlist, Song, ToplistItem } from '../model/Models';
import { Utils } from '../common/Utils';

@Entry
@Component
struct HomePage {
  @State tab: number = 0;
  // 推荐
  @State dailySongs: Song[] = [];
  @State recommend: Playlist[] = [];
  @State newsongs: Song[] = [];
  @State loading: boolean = true;
  // 排行榜
  @State toplists: ToplistItem[] = [];
  // 歌单广场
  @State cats: string[] = ['全部', '华语', '流行', '摇滚', '民谣', '电子', '轻音乐', '影视原声', 'ACG', '怀旧'];
  @State selectedCat: string = '全部';
  @State square: Playlist[] = [];
  // 我的
  @State userNickname: string = '';
  @State userAvatar: string = '';
  @State userPlaylists: Playlist[] = [];

  aboutToAppear(): void {
    this.loadRecommend();
    this.loadToplists();
    this.loadSquare();
  }

  onPageShow(): void {
    if (this.tab === 2) {
      this.loadSquare();
    }
    if (this.tab === 3) {
      this.loadUser();
    }
  }

  async loadRecommend(): Promise<void> {
    this.loading = true;
    try {
      if (Constants.isLoggedIn()) {
        const daily = await Api.recommendSongs();
        if (daily.length > 0) this.dailySongs = daily;
      }
      const rec = await Api.recommendResource();
      this.recommend = rec.length > 0 ? rec.slice(0, 12) : this.recommend;
      const news = await Api.newsong();
      this.newsongs = news;
    } catch (e) {
      promptAction.showToast({ message: '加载失败，请检查服务器地址' });
    }
    this.loading = false;
  }

  async loadToplists(): Promise<void> {
    try {
      this.toplists = await Api.toplist();
    } catch (e) {
    }
  }

  async loadSquare(): Promise<void> {
    try {
      this.square = await Api.hotPlaylist(this.selectedCat);
    } catch (e) {
    }
  }

  selectCat(cat: string): void {
    this.selectedCat = cat;
    this.loadSquare();
  }

  loadUser(): void {
    if (!Constants.isLoggedIn()) {
      this.userNickname = '未登录';
      this.userPlaylists = [];
      return;
    }
    try {
      const json = Constants.profileJson;
      if (json.length > 0) {
        const p = JSON.parse(json) as Record<string, Object>;
        this.userNickname = String(p['nickname'] ?? '');
        this.userAvatar = String(p['avatarUrl'] ?? '');
      }
      Api.userPlaylist(Constants.userId).then((list: Playlist[]) => {
        this.userPlaylists = list;
      }).catch(() => {
      });
    } catch (e) {
    }
  }

  logout(): void {
    Net.put('user_id', 0);
    Net.put('profile_json', '');
    Constants.userId = 0;
    Constants.profileJson = '';
    promptAction.showToast({ message: '已退出登录' });
    router.replaceUrl({ url: 'pages/LoginPage' });
  }

  openPlaylist(id: number): void {
    router.pushUrl({ url: `pages/PlaylistPage?id=${id}` });
  }

  playSongs(songs: Song[], start: number): void {
    if (songs.length === 0) return;
    PlayerManager.getInstance().playQueue(songs, start);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  build() {
    Column() {
      Row() {
        Text('‹ 返回').fontSize(14).fontColor('#666666').onClick(() => {
          router.back();
        })
        Text('音乐').fontSize(20).fontWeight(FontWeight.Bold).margin({ left: 14 })
        Blank()
        Button('搜索')
          .fontSize(14)
          .height(32)
          .backgroundColor('#F2F2F2')
          .fontColor('#333333')
          .onClick(() => {
            router.pushUrl({ url: 'pages/SearchPage' });
          })
        Button('设置')
          .fontSize(14)
          .height(32)
          .backgroundColor('#F2F2F2')
          .fontColor('#333333')
          .onClick(() => {
            router.pushUrl({ url: 'pages/SettingsPage' });
          })
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 12, bottom: 8 })

      Tabs({ barPosition: BarPosition.Start, index: this.tab }) {
        TabContent() {
          this.RecommendTab()
        }.tabBar('推荐')

        TabContent() {
          this.ToplistTab()
        }.tabBar('排行榜')

        TabContent() {
          this.SquareTab()
        }.tabBar('歌单广场')

        TabContent() {
          this.UserTab()
        }.tabBar('我的')
      }
      .onChange((index: number) => {
        this.tab = index;
        if (index === 3) this.loadUser();
      })
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }

  @Builder
  RecommendTab() {
    Scroll() {
      Column() {
        if (this.dailySongs.length > 0) {
          Text('每日推荐').fontSize(16).fontWeight(FontWeight.Bold).margin({ left: 16, top: 8, bottom: 4 })
          ForEach(this.dailySongs.slice(0, 5), (song: Song, idx: number) => {
            this.SongLine(song, idx, this.dailySongs)
          }, (song: Song, idx: number) => `${song.id}-${idx}`)
        }

        Text('推荐歌单').fontSize(16).fontWeight(FontWeight.Bold).margin({ left: 16, top: 12, bottom: 4 })
        Grid() {
          ForEach(this.recommend, (p: Playlist, idx: number) => {
            GridItem() {
              this.PlaylistCard(p)
            }
          }, (p: Playlist, idx: number) => `${p.id}-${idx}`)
        }
        .columnsTemplate('1fr 1fr 1fr')
        .columnsGap(10)
        .rowsGap(14)
        .height(this.recommend.length > 0 ? Math.ceil(this.recommend.length / 3) * 170 : 170)
        .padding({ left: 16, right: 16 })

        Text('新歌速递').fontSize(16).fontWeight(FontWeight.Bold).margin({ left: 16, top: 12, bottom: 4 })
        ForEach(this.newsongs.slice(0, 10), (song: Song, idx: number) => {
          this.SongLine(song, idx, this.newsongs)
        }, (song: Song, idx: number) => `n-${song.id}-${idx}`)
      }
    }
    .width('100%')
    .height('100%')
    .align(Alignment.Top)
  }

  @Builder
  ToplistTab() {
    List() {
      ForEach(this.toplists, (t: ToplistItem, idx: number) => {
        ListItem() {
          Row({ space: 12 }) {
            Image(t.coverImgUrl)
              .width(56)
              .height(56)
              .borderRadius(8)
            Column({ space: 4 }) {
              Text(t.name).fontSize(15).fontWeight(FontWeight.Medium).maxLines(1)
              Text(`更新：${t.updateFrequency}`).fontSize(12).fontColor('#999999')
              Text(`播放 ${Utils.formatPlayCount(t.playCount)}`).fontSize(12).fontColor('#999999')
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 8, bottom: 8 })
          .onClick(() => {
            this.openPlaylist(t.id);
          })
        }
      }, (t: ToplistItem, idx: number) => `${t.id}-${idx}`)
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  SquareTab() {
    Column() {
      Scroll() {
        Row({ space: 8 }) {
          ForEach(this.cats, (cat: string, idx: number) => {
            Text(cat)
              .fontSize(13)
              .fontColor(this.selectedCat === cat ? '#FFFFFF' : '#333333')
              .backgroundColor(this.selectedCat === cat ? '#C62F2F' : '#F2F2F2')
              .borderRadius(14)
              .padding({ left: 14, right: 14, top: 6, bottom: 6 })
              .onClick(() => {
                this.selectCat(cat);
              })
          }, (cat: string, idx: number) => `${cat}-${idx}`)
        }
        .padding({ left: 16, right: 16, top: 12, bottom: 12 })
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .width('100%')
      .height(48)

      Grid() {
        ForEach(this.square, (p: Playlist, idx: number) => {
          GridItem() {
            this.PlaylistCard(p)
          }
        }, (p: Playlist, idx: number) => `s-${p.id}-${idx}`)
      }
      .columnsTemplate('1fr 1fr 1fr')
      .columnsGap(10)
      .rowsGap(14)
      .height(this.square.length > 0 ? Math.ceil(this.square.length / 3) * 170 : 170)
      .padding({ left: 16, right: 16 })
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  UserTab() {
    Scroll() {
      Column({ space: 10 }) {
        Row({ space: 14 }) {
          Image(this.userAvatar)
            .width(64)
            .height(64)
            .borderRadius(32)
            .objectFit(ImageFit.Cover)
            .backgroundColor('#F0F0F0')
          Text(this.userNickname).fontSize(20).fontWeight(FontWeight.Bold)
          Blank()
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16 })

        Row({ space: 12 }) {
          if (Constants.isLoggedIn()) {
            Button('我喜欢')
              .backgroundColor('#C62F2F')
              .onClick(() => {
                router.pushUrl({ url: 'pages/LikedPage' });
              })
            Button('退出登录')
              .backgroundColor('#F2F2F2')
              .fontColor('#E84026')
              .onClick(() => {
                this.logout();
              })
          } else {
            Button('去登录')
              .backgroundColor('#C62F2F')
              .onClick(() => {
                router.pushUrl({ url: 'pages/LoginPage' });
              })
          }
        }
        .width('100%')
        .padding({ left: 16, right: 16 })

        Text('我的歌单').fontSize(16).fontWeight(FontWeight.Bold).margin({ left: 16, top: 8 })
        if (this.userPlaylists.length === 0) {
          Text('暂无歌单，请先登录').fontSize(13).fontColor('#999999').margin({ left: 16, top: 8 })
        }
        ForEach(this.userPlaylists, (p: Playlist) => {
          Row({ space: 12 }) {
            Image(p.coverImgUrl).width(48).height(48).borderRadius(6)
            Column({ space: 3 }) {
              Text(p.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
              Text(`${p.trackCount} 首`).fontSize(12).fontColor('#999999')
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 6, bottom: 6 })
          .onClick(() => {
            this.openPlaylist(p.id);
          })
        }, (p: Playlist) => String(p.id))
      }
    }
    .width('100%')
    .height('100%')
    .align(Alignment.Top)
  }

  @Builder
  PlaylistCard(p: Playlist) {
    Column({ space: 6 }) {
      Stack({ alignContent: Alignment.BottomEnd }) {
        Image(p.coverImgUrl).width('100%').aspectRatio(1).borderRadius(8)
        Text(Utils.formatPlayCount(p.playCount))
          .fontSize(10)
          .fontColor('#FFFFFF')
          .backgroundColor('#66000000')
          .borderRadius({ topLeft: 6 })
          .padding({ left: 4, right: 4, top: 1, bottom: 1 })
      }
      .width('100%')
      Text(p.name).fontSize(12).maxLines(2).textOverflow({ overflow: TextOverflow.Ellipsis }).lineHeight(16)
    }
    .width('100%')
    .onClick(() => {
      this.openPlaylist(p.id);
    })
  }

  @Builder
  SongLine(song: Song, idx: number, songs: Song[]) {
    Row({ space: 12 }) {
      Text(`${idx + 1}`.padStart(2, '0')).fontSize(14).fontColor('#999999').width(28)
      Column({ space: 3 }) {
        Text(song.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(song.artistNames()).fontSize(12).fontColor('#999999').maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      Text(Utils.formatDuration(song.duration)).fontSize(12).fontColor('#BBBBBB')
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 8, bottom: 8 })
    .onClick(() => {
      this.playSongs(songs, idx);
    })
  }
}
