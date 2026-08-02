import { router, promptAction } from '@kit.ArkUI';import { GitHubApi, Announcement } from '../common/GitHubApi';
import { UpdateChecker } from '../common/UpdateChecker';

/** 公告与推送：在线公告 + 内置兜底，推广条目可跳转 */
@Entry
@Component
struct AnnouncementPage {
  @State list: Announcement[] = [];

  aboutToAppear(): void {
    this.load();
    UpdateChecker.getInstance().markSeen();
  }

  async load(): Promise<void> {
    const checker = UpdateChecker.getInstance();
    if (checker.announcements.length === 0) {
      await checker.checkNow();
    }
    this.list = checker.announcements.length > 0 ? checker.announcements : GitHubApi.builtinAnnouncements();
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('公告与推送').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      List({ space: 10 }) {
        ForEach(this.list, (item: Announcement) => {
          ListItem() {
            Column({ space: 6 }) {
              Row({ space: 8 }) {
                Text(item.isPromo ? '推广' : '公告')
                  .fontSize(10)
                  .fontColor('#FFFFFF')
                  .backgroundColor(item.isPromo ? '#FF8000' : '#3D7FFF')
                  .borderRadius(4)
                  .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                Text(item.title).fontSize(15).fontWeight(FontWeight.Bold).layoutWeight(1)
              }
              .width('100%')
              .alignItems(VerticalAlign.Center)
              if (item.date.length > 0) {
                Text(item.date).fontSize(11).fontColor('#999999')
              }
              Text(item.content).fontSize(13).fontColor('#555555').lineHeight(20).width('100%')
              if (item.url.length > 0) {
                Text('查看详情 ›').fontSize(12).fontWeight(FontWeight.Bold).fontColor('#C62F2F')
                  .onClick(() => {
                    router.openUrl({ url: item.url });
                  })
              }
            }
            .width('100%')
            .padding(14)
            .borderRadius(14)
            .backgroundColor('#59FFFFFF')
            .backdropBlur(20)
            .border({ width: 0.5, color: '#99FFFFFF' })
            .alignItems(HorizontalAlign.Start)
          }
        }, (item: Announcement) => item.id)
      }
      .width('100%')
      .layoutWeight(1)
      .padding({ left: 14, right: 14, top: 6 })

      Text(`公告实时从 GitHub 拉取（${Constants.ghOwner}/${Constants.ghRepo}/ANNOUNCEMENTS.md），离线时展示内置公告`)
        .fontSize(10)
        .fontColor('#AAAAAA')
        .margin({ bottom: 12 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F2F5FA')
  }
}
