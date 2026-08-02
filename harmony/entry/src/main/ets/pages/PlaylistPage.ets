import { router, promptAction } from '@kit.ArkUI';
import { Api } from '../common/Api';
import { PlayerManager } from '../common/PlayerManager';
import { Utils } from '../common/Utils';
import { Playlist, Song } from '../model/Models';

@Entry
@Component
struct PlaylistPage {
  @State playlist: Playlist = new Playlist();
  @State songs: Song[] = [];
  @State loading: boolean = true;
  @State subscribed: boolean = false;
  private id: number = 0;

  aboutToAppear(): void {
    const params = router.getParams();
    this.id = Utils.paramId(params, 'id');
    this.load();
  }

  async load(): Promise<void> {
    try {
      const p = await Api.playlistDetail(this.id);
      this.playlist = p;
      this.subscribed = p.subscribed;
      const tracks = await Api.playlistTracks(this.id);
      this.songs = tracks.length > 0 ? tracks : p.tracks;
    } catch (e) {
      promptAction.showToast({ message: '歌单加载失败，请检查服务器地址' });
    }
    this.loading = false;
  }

  playAll(): void {
    if (this.songs.length === 0) return;
    PlayerManager.getInstance().playQueue(this.songs, 0);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  playAt(index: number): void {
    if (this.songs.length === 0) return;
    PlayerManager.getInstance().playQueue(this.songs, index);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  toggleSubscribe(): void {
    Api.playlistSubscribe(this.id, this.subscribed ? 0 : 1).then((code: number) => {
      if (code === 200) {
        this.subscribed = !this.subscribed;
        promptAction.showToast({ message: this.subscribed ? '已收藏歌单' : '已取消收藏' });
      } else {
        promptAction.showToast({ message: '操作失败，请确认已登录' });
      }
    }).catch(() => {
      promptAction.showToast({ message: '操作失败，请确认已登录' });
    });
  }

  @Builder
  Header() {
    Row({ space: 12 }) {
      Image(this.playlist.coverImgUrl)
        .width(96)
        .height(96)
        .borderRadius(8)
        .objectFit(ImageFit.Cover)
      Column({ space: 6 }) {
        Text(this.playlist.name)
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(`by ${this.playlist.creator.nickname}`).fontSize(13).fontColor('#999999')
        Text(`播放 ${Utils.formatPlayCount(this.playlist.playCount)} 次 · ${this.playlist.trackCount} 首`)
          .fontSize(13)
          .fontColor('#999999')
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 12, bottom: 12 })

    Row({ space: 12 }) {
      Button('播放全部')
        .backgroundColor('#C62F2F')
        .onClick(() => {
          this.playAll();
        })
      Button(this.subscribed ? '取消收藏' : '收藏歌单')
        .backgroundColor('#F2F2F2')
        .fontColor('#333333')
        .onClick(() => {
          this.toggleSubscribe();
        })
    }
    .width('100%')
    .padding({ left: 16, right: 16, bottom: 8 })
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('歌单详情').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      if (this.loading) {
        Column() {
          LoadingProgress().width(48).height(48)
        }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        List({ space: 2 }) {
          ListItem() {
            this.Header()
          }
          ForEach(this.songs, (song: Song, idx: number) => {
            ListItem() {
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
          }, (song: Song) => String(song.id))
        }
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
