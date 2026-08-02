import { router, promptAction } from '@kit.ArkUI';
import { Api } from '../common/Api';
import { PlayerManager } from '../common/PlayerManager';
import { Utils } from '../common/Utils';
import { Album, Artist, Song } from '../model/Models';

@Entry
@Component
struct ArtistPage {
  @State artist: Artist = new Artist();
  @State hotSongs: Song[] = [];
  @State albums: Album[] = [];
  @State loading: boolean = true;
  private id: number = 0;

  aboutToAppear(): void {
    const params = router.getParams();
    this.id = Utils.paramId(params, 'id');
    this.load();
  }

  async load(): Promise<void> {
    try {
      const res = await Api.artist(this.id);
      this.artist = res.artist;
      this.hotSongs = res.hotSongs;
      this.albums = await Api.artistAlbums(this.id);
    } catch (e) {
      promptAction.showToast({ message: '歌手信息加载失败' });
    }
    this.loading = false;
  }

  playAll(): void {
    if (this.hotSongs.length === 0) return;
    PlayerManager.getInstance().playQueue(this.hotSongs, 0);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('歌手').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
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
        List({ space: 4 }) {
          ListItem() {
            Column({ space: 10 }) {
              Image(this.artist.picUrl).width(120).height(120).borderRadius(60).objectFit(ImageFit.Cover)
              Text(this.artist.name).fontSize(20).fontWeight(FontWeight.Bold)
              Button('播放热门 50 首').backgroundColor('#C62F2F').onClick(() => {
                this.playAll();
              })
            }
            .width('100%')
            .padding({ top: 16, bottom: 12 })
          }
          ListItem() {
            Text('热门歌曲').fontSize(16).fontWeight(FontWeight.Bold).padding({ left: 16, top: 8, bottom: 8 })
          }
          ForEach(this.hotSongs, (song: Song, idx: number) => {
            ListItem() {
              Row({ space: 10 }) {
                Text(String(idx + 1).padStart(2, '0')).fontSize(13).fontColor('#999999').width(30)
                Column({ space: 2 }) {
                  Text(song.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
                  Text(song.album.name).fontSize(12).fontColor('#999999').maxLines(1)
                    .textOverflow({ overflow: TextOverflow.Ellipsis })
                }
                .layoutWeight(1)
                .alignItems(HorizontalAlign.Start)
              }
              .width('100%')
              .padding({ left: 16, right: 16, top: 10, bottom: 10 })
              .onClick(() => {
                PlayerManager.getInstance().playQueue(this.hotSongs, idx);
                router.pushUrl({ url: 'pages/PlayerPage' });
              })
            }
          }, (song: Song) => String(song.id))
          ListItem() {
            Text('专辑').fontSize(16).fontWeight(FontWeight.Bold).padding({ left: 16, top: 12, bottom: 8 })
          }
          ForEach(this.albums, (album: Album) => {
            ListItem() {
              Row({ space: 12 }) {
                Image(album.picUrl).width(56).height(56).borderRadius(6).objectFit(ImageFit.Cover)
                Column({ space: 4 }) {
                  Text(album.name).fontSize(15).maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
                  Text(`${album.size} 首`).fontSize(12).fontColor('#999999')
                }
                .layoutWeight(1)
                .alignItems(HorizontalAlign.Start)
              }
              .width('100%')
              .padding({ left: 16, right: 16, top: 8, bottom: 8 })
              .onClick(() => {
                router.pushUrl({ url: `pages/AlbumPage?id=${album.id}` });
              })
            }
          }, (album: Album) => String(album.id))
        }
        .layoutWeight(1)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
