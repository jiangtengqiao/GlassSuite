import { router, promptAction } from '@kit.ArkUI';
import { Api } from '../common/Api';
import { PlayerManager } from '../common/PlayerManager';
import { Utils } from '../common/Utils';
import { Album, Song } from '../model/Models';

@Entry
@Component
struct AlbumPage {
  @State album: Album = new Album();
  @State songs: Song[] = [];
  @State loading: boolean = true;
  private id: number = 0;

  aboutToAppear(): void {
    const params = router.getParams();
    this.id = Utils.paramId(params, 'id');
    this.load();
  }

  async load(): Promise<void> {
    try {
      const res = await Api.album(this.id);
      this.album = res.album;
      this.songs = res.songs;
    } catch (e) {
      promptAction.showToast({ message: '专辑加载失败' });
    }
    this.loading = false;
  }

  playAll(): void {
    if (this.songs.length === 0) return;
    PlayerManager.getInstance().playQueue(this.songs, 0);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('专辑').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
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
            Row({ space: 14 }) {
              Image(this.album.picUrl).width(110).height(110).borderRadius(10).objectFit(ImageFit.Cover)
              Column({ space: 8 }) {
                Text(this.album.name).fontSize(17).fontWeight(FontWeight.Bold).maxLines(2)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                Text(this.album.artist.name).fontSize(13).fontColor('#999999')
                Button('播放全部').backgroundColor('#C62F2F').fontSize(13).height(34).onClick(() => {
                  this.playAll();
                })
              }
              .layoutWeight(1)
              .alignItems(HorizontalAlign.Start)
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 16, bottom: 16 })
          }
          ForEach(this.songs, (song: Song, idx: number) => {
            ListItem() {
              Row({ space: 10 }) {
                Text(String(idx + 1).padStart(2, '0')).fontSize(13).fontColor('#999999').width(30)
                Text(song.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
                  .layoutWeight(1)
                Text(Utils.formatDuration(song.duration)).fontSize(12).fontColor('#999999')
              }
              .width('100%')
              .padding({ left: 16, right: 16, top: 10, bottom: 10 })
              .onClick(() => {
                PlayerManager.getInstance().playQueue(this.songs, idx);
                router.pushUrl({ url: 'pages/PlayerPage' });
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
