import { router, promptAction } from '@kit.ArkUI';
import { Api } from '../common/Api';
import { Constants } from '../common/Constants';
import { PlayerManager } from '../common/PlayerManager';
import { Utils } from '../common/Utils';
import { Song } from '../model/Models';

@Entry
@Component
struct LikedPage {
  @State songs: Song[] = [];
  @State loading: boolean = true;

  aboutToAppear(): void {
    this.load();
  }

  async load(): Promise<void> {
    if (Constants.userId <= 0) {
      this.loading = false;
      return;
    }
    try {
      const ids = await Api.likeList(Constants.userId);
      // 分批拉取歌曲详情（每批最多 1000）
      const chunks: number[][] = [];
      for (let i = 0; i < ids.length; i += 100) {
        chunks.push(ids.slice(i, i + 100));
      }
      const all: Song[] = [];
      for (let i = 0; i < chunks.length; i++) {
        const part = await Api.songDetail(chunks[i].join(','));
        all.push(...part);
      }
      this.songs = all;
    } catch (e) {
      promptAction.showToast({ message: '喜欢列表加载失败' });
    }
    this.loading = false;
  }

  playAt(index: number): void {
    if (this.songs.length === 0) return;
    PlayerManager.getInstance().playQueue(this.songs, index);
    router.pushUrl({ url: 'pages/PlayerPage' });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('我喜欢').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      if (this.loading) {
        Column() {
          LoadingProgress().width(48).height(48)
        }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else if (this.songs.length === 0) {
        Column() {
          Text('暂无喜欢的歌曲，请先登录并点赞').fontSize(14).fontColor('#999999')
        }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        List({ space: 2 }) {
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
