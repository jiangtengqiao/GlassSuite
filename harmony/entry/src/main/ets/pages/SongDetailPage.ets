import { router, promptAction } from '@kit.ArkUI';
import { Api } from '../common/Api';
import { PlayerManager } from '../common/PlayerManager';
import { Utils } from '../common/Utils';
import { Song } from '../model/Models';

@Entry
@Component
struct SongDetailPage {
  @State song: Song = new Song();
  @State loaded: boolean = false;
  @State liked: boolean = false;
  private id: number = 0;

  aboutToAppear(): void {
    const params = router.getParams();
    this.id = Utils.paramId(params, 'id');
    this.load();
  }

  async load(): Promise<void> {
    try {
      const songs = await Api.songDetail(String(this.id));
      if (songs.length > 0) {
        this.song = songs[0];
      }
    } catch (e) {
      promptAction.showToast({ message: '歌曲信息加载失败' });
    }
    this.loaded = true;
  }

  play(): void {
    PlayerManager.getInstance().playSong(this.song);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  toggleLike(): void {
    PlayerManager.getInstance().toggleLike(this.song.id);
    this.liked = !this.liked;
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('歌曲详情').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      if (!this.loaded) {
        Column() {
          LoadingProgress().width(48).height(48)
        }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        Column({ space: 16 }) {
          Image(this.song.albumPic())
            .width(200)
            .height(200)
            .borderRadius(16)
            .objectFit(ImageFit.Cover)
            .margin({ top: 24 })
          Text(this.song.name).fontSize(20).fontWeight(FontWeight.Bold).textAlign(TextAlign.Center)
          Text(this.song.artistNames()).fontSize(14).fontColor('#999999').textAlign(TextAlign.Center)
          Text(Utils.formatDuration(this.song.duration)).fontSize(13).fontColor('#999999')

          Row({ space: 24 }) {
            Button('播放')
              .backgroundColor('#C62F2F')
              .onClick(() => {
                this.play();
              })
            Button('♥ 喜欢').backgroundColor('#F2F2F2').fontColor('#333333').onClick(() => {
              this.toggleLike();
            })
          }
          .margin({ top: 8 })

          Column({ space: 4 }) {
            Text('歌手').fontSize(14).fontWeight(FontWeight.Bold).margin({ top: 12 })
            Text(this.song.artistNames()).fontSize(14).fontColor('#C62F2F').onClick(() => {
              const a = this.song.artists[0];
              if (a !== undefined && a.id > 0) {
                router.pushUrl({ url: `pages/ArtistPage?id=${a.id}` });
              }
            })
            Text('专辑').fontSize(14).fontWeight(FontWeight.Bold).margin({ top: 12 })
            Text(this.song.album.name).fontSize(14).fontColor('#C62F2F').onClick(() => {
              if (this.song.album.id > 0) {
                router.pushUrl({ url: `pages/AlbumPage?id=${this.song.album.id}` });
              }
            })
            if (this.song.mvid > 0) {
              Text('MV').fontSize(14).fontWeight(FontWeight.Bold).margin({ top: 12 })
              Text('观看 MV ›').fontSize(14).fontColor('#C62F2F').onClick(() => {
                router.pushUrl({ url: `pages/MvPage?id=${this.song.mvid}` });
              })
            }
          }
          .width('100%')
          .padding({ left: 32, right: 32 })
          .alignItems(HorizontalAlign.Start)

          Blank()
        }
        .width('100%')
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
