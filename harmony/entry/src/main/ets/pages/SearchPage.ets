import { router, promptAction } from '@kit.ArkUI';
import { Api } from '../common/Api';
import { PlayerManager } from '../common/PlayerManager';
import { Utils } from '../common/Utils';
import { Album, Artist, HotWord, MvInfo, Playlist, Song } from '../model/Models';

@Entry
@Component
struct SearchPage {
  @State keyword: string = '';
  @State hotWords: HotWord[] = [];
  @State resultType: number = 0;
  @State songs: Song[] = [];
  @State playlists: Playlist[] = [];
  @State artists: Artist[] = [];
  @State albums: Album[] = [];
  @State mvs: MvInfo[] = [];
  @State searched: boolean = false;
  @State searching: boolean = false;
  private searchTimer: number = -1;

  aboutToAppear(): void {
    Api.hotSearch().then((list: HotWord[]) => {
      this.hotWords = list;
    }).catch(() => {
    });
  }

  doSearch(): void {
    const kw = this.keyword.trim();
    if (kw.length === 0) return;
    this.searched = true;
    this.searching = true;
    this.resultType = 0;
    Api.search(kw, 1).then((list: Song[]) => {
      this.songs = list;
      this.searching = false;
    }).catch(() => {
      this.searching = false;
    });
  }

  onInput(value: string): void {
    this.keyword = value;
    if (this.searchTimer >= 0) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      const kw = this.keyword.trim();
      if (kw.length > 0) this.doSearch();
    }, 600);
  }

  switchType(type: number): void {
    this.resultType = type;
    const kw = this.keyword.trim();
    if (kw.length === 0) return;
    if (type === 0) {
      Api.search(kw, 1).then((list: Song[]) => {
        this.songs = list;
      }).catch(() => {
      });
    } else if (type === 1) {
      Api.searchPlaylists(kw).then((list: Playlist[]) => {
        this.playlists = list;
      }).catch(() => {
      });
    } else if (type === 2) {
      Api.searchArtists(kw).then((list: Artist[]) => {
        this.artists = list;
      }).catch(() => {
      });
    } else if (type === 3) {
      Api.searchAlbums(kw).then((list: Album[]) => {
        this.albums = list;
      }).catch(() => {
      });
    } else if (type === 4) {
      Api.searchMvs(kw).then((list: MvInfo[]) => {
        this.mvs = list;
      }).catch(() => {
      });
    }
  }

  playAt(index: number): void {
    if (this.songs.length === 0) return;
    PlayerManager.getInstance().playQueue(this.songs, index);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  @Builder
  HotPanel() {
    Column({ space: 10 }) {
      Text('热搜榜').fontSize(16).fontWeight(FontWeight.Bold).width('100%').textAlign(TextAlign.Start)
      ForEach(this.hotWords, (w: HotWord, idx: number) => {
        Row({ space: 10 }) {
          Text(String(idx + 1)).fontSize(15).fontWeight(FontWeight.Bold)
            .fontColor(idx < 3 ? '#C62F2F' : '#999999').width(24)
          Text(w.first).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .width('100%')
        .padding({ top: 6, bottom: 6 })
        .onClick(() => {
          this.keyword = w.first;
          this.doSearch();
        })
      }, (w: HotWord) => w.first + String(idx))
    }
    .width('100%')
    .padding(16)
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  SongItem(song: Song, idx: number) {
    Row({ space: 10 }) {
      Text(String(idx + 1).padStart(2, '0')).fontSize(13).fontColor('#999999').width(30)
      Column({ space: 2 }) {
        Text(song.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(song.artistNames()).fontSize(12).fontColor('#999999').maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      Text(Utils.formatDuration(song.duration)).fontSize(12).fontColor('#999999')
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 10, bottom: 10 })
    .onClick(() => {
      this.playAt(idx);
    })
  }

  @Builder
  ResultPanel() {
    Column() {
      Tabs({ barPosition: BarPosition.Start, index: this.resultType, scrollable: true }) {
        TabContent() {
          List({ space: 2 }) {
            ForEach(this.songs, (song: Song, idx: number) => {
              ListItem() {
                this.SongItem(song, idx)
              }
            }, (song: Song) => String(song.id))
          }
          .layoutWeight(1)
        }.tabBar('单曲')

        TabContent() {
          List({ space: 2 }) {
            ForEach(this.playlists, (p: Playlist) => {
              ListItem() {
                Row({ space: 12 }) {
                  Image(p.coverImgUrl).width(48).height(48).borderRadius(6).objectFit(ImageFit.Cover)
                  Column({ space: 2 }) {
                    Text(p.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
                    Text(`${p.trackCount} 首`).fontSize(12).fontColor('#999999')
                  }
                  .layoutWeight(1)
                  .alignItems(HorizontalAlign.Start)
                }
                .width('100%')
                .padding({ left: 16, right: 16, top: 10, bottom: 10 })
                .onClick(() => {
                  router.pushUrl({ url: `pages/PlaylistPage?id=${p.id}` });
                })
              }
            }, (p: Playlist) => String(p.id))
          }
          .layoutWeight(1)
        }.tabBar('歌单')

        TabContent() {
          List({ space: 2 }) {
            ForEach(this.artists, (a: Artist) => {
              ListItem() {
                Row({ space: 12 }) {
                  Image(a.picUrl).width(48).height(48).borderRadius(24).objectFit(ImageFit.Cover)
                  Text(a.name).fontSize(15)
                }
                .width('100%')
                .padding({ left: 16, right: 16, top: 10, bottom: 10 })
                .onClick(() => {
                  router.pushUrl({ url: `pages/ArtistPage?id=${a.id}` });
                })
              }
            }, (a: Artist) => String(a.id))
          }
          .layoutWeight(1)
        }.tabBar('歌手')

        TabContent() {
          List({ space: 2 }) {
            ForEach(this.albums, (al: Album) => {
              ListItem() {
                Row({ space: 12 }) {
                  Image(al.picUrl).width(48).height(48).borderRadius(6).objectFit(ImageFit.Cover)
                  Column({ space: 2 }) {
                    Text(al.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
                    Text(al.artist.name).fontSize(12).fontColor('#999999')
                  }
                  .layoutWeight(1)
                  .alignItems(HorizontalAlign.Start)
                }
                .width('100%')
                .padding({ left: 16, right: 16, top: 10, bottom: 10 })
                .onClick(() => {
                  router.pushUrl({ url: `pages/AlbumPage?id=${al.id}` });
                })
              }
            }, (al: Album) => String(al.id))
          }
          .layoutWeight(1)
        }.tabBar('专辑')

        TabContent() {
          List({ space: 2 }) {
            ForEach(this.mvs, (mv: MvInfo) => {
              ListItem() {
                Row({ space: 12 }) {
                  Image(mv.cover).width(64).height(40).borderRadius(4).objectFit(ImageFit.Cover)
                  Column({ space: 2 }) {
                    Text(mv.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
                    Text(mv.artistName).fontSize(12).fontColor('#999999')
                  }
                  .layoutWeight(1)
                  .alignItems(HorizontalAlign.Start)
                }
                .width('100%')
                .padding({ left: 16, right: 16, top: 10, bottom: 10 })
                .onClick(() => {
                  router.pushUrl({ url: `pages/MvPage?id=${mv.id}` });
                })
              }
            }, (mv: MvInfo) => String(mv.id))
          }
          .layoutWeight(1)
        }.tabBar('MV')
      }
      .onChange((index: number) => {
        this.switchType(index);
      })
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    Column() {
      Row({ space: 8 }) {
        TextInput({ text: this.keyword, placeholder: '搜索歌曲、歌单、歌手、专辑、MV' })
          .layoutWeight(1)
          .height(40)
          .borderRadius(20)
          .onChange((value: string) => {
            this.onInput(value);
          })
          .onSubmit(() => {
            this.doSearch();
          })
        Text('搜索')
          .fontSize(15)
          .fontColor('#C62F2F')
          .onClick(() => {
            this.doSearch();
          })
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 8, bottom: 8 })

      if (this.searched) {
        this.ResultPanel()
      } else {
        Scroll() {
          this.HotPanel()
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
