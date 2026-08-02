import { router, promptAction } from '@kit.ArkUI';
import { media } from '@kit.MediaKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { Api } from '../common/Api';
import { Utils } from '../common/Utils';
import { MvInfo } from '../model/Models';

@Entry
@Component
struct MvPage {
  @State info: MvInfo = new MvInfo();
  @State loading: boolean = true;
  @State playing: boolean = false;
  private id: number = 0;
  private avPlayer: media.AVPlayer | undefined = undefined;
  private xController: XComponentController = new XComponentController();

  aboutToAppear(): void {
    const params = router.getParams();
    this.id = Utils.paramId(params, 'id');
    this.load();
  }

  aboutToDisappear(): void {
    this.release();
  }

  async load(): Promise<void> {
    try {
      this.info = await Api.mvDetail(this.id);
      const url = await Api.mvUrl(this.id);
      if (url.length === 0) {
        promptAction.showToast({ message: 'MV 暂无播放源' });
        this.loading = false;
        return;
      }
      await this.initPlayer(url);
    } catch (e) {
      promptAction.showToast({ message: 'MV 加载失败' });
    }
    this.loading = false;
  }

  async initPlayer(url: string): Promise<void> {
    try {
      const player: media.AVPlayer = await media.createAVPlayer();
      this.avPlayer = player;
      player.on('stateChange', (state: string) => {
        if (state === 'prepared') {
          player.play();
          this.playing = true;
        }
      });
      player.on('error', (err: BusinessError) => {
        console.error(`[MvPage] player error: ${err.code}`);
        promptAction.showToast({ message: 'MV 播放失败' });
      });
      const surfaceId = this.xController.getXComponentSurfaceId();
      player.setVideoSurface(surfaceId);
      player.setMediaSource(new media.AVMediaSource(url));
      player.prepare();
    } catch (e) {
      console.error(`[MvPage] init error: ${JSON.stringify(e)}`);
    }
  }

  release(): void {
    if (this.avPlayer !== undefined) {
      this.avPlayer.release();
      this.avPlayer = undefined;
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text(this.info.name).fontSize(16).fontWeight(FontWeight.Bold).layoutWeight(1)
          .maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      XComponent({ id: 'mvPlayer', type: XComponentType.SURFACE, controller: this.xController })
        .width('100%')
        .height(240)
        .backgroundColor('#000000')

      Column({ space: 6 }) {
        Text(this.info.name).fontSize(18).fontWeight(FontWeight.Bold)
        Text(`${this.info.artistName} · ${Utils.formatDuration(this.info.duration)} · 播放 ${Utils.formatPlayCount(this.info.playCount)} 次`)
          .fontSize(13)
          .fontColor('#999999')
      }
      .width('100%')
      .padding(16)
      .alignItems(HorizontalAlign.Start)

      Blank()
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
