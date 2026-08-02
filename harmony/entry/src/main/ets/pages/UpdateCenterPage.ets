import { router, promptAction } from '@kit.ArkUI';
import { GitHubApi, GitHubRelease } from '../common/GitHubApi';
import { UpdateChecker } from '../common/UpdateChecker';

/** 更新中心：当前/最新版本、手动检查、可折叠更新日志、跳转下载 */
@Entry
@Component
struct UpdateCenterPage {
  @State latest: GitHubRelease | undefined = undefined;
  @State hasUpdate: boolean = false;
  @State checking: boolean = false;
  @State releases: GitHubRelease[] = [];
  @State expandedTag: string = '';
  @State loaded: boolean = false;

  aboutToAppear(): void {
    this.load();
  }

  async load(): Promise<void> {
    try {
      this.releases = await GitHubApi.releases(10);
    } catch (e) {
      this.releases = [];
    }
    this.loaded = true;
  }

  async check(): Promise<void> {
    this.checking = true;
    this.hasUpdate = await UpdateChecker.getInstance().checkNow();
    this.latest = UpdateChecker.getInstance().latest;
    this.checking = false;
  }

  toggle(tag: string): void {
    this.expandedTag = this.expandedTag === tag ? '' : tag;
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('更新中心').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      // 版本状态卡
      Row() {
        Column({ space: 6 }) {
          Text('当前版本：v' + UpdateChecker.VERSION).fontSize(15).fontWeight(FontWeight.Bold)
          Text(this.checking ? '正在检查更新…' :
            (this.hasUpdate ? `发现新版本：${this.latest?.tagName ?? ''}` :
              (this.latest === undefined ? '尚未检查（启动后自动检查）' : '已是最新版本')))
            .fontSize(12)
            .fontColor(this.hasUpdate ? '#E8890C' : '#999999')
          Row({ space: 10 }) {
            Button('立即检查').backgroundColor('#C62F2F').fontSize(13).height(34).onClick(() => {
              this.check();
            })
            if (this.hasUpdate) {
              Button('前往下载').backgroundColor('#C62F2F').fontSize(13).height(34).onClick(() => {
                const u = this.latest;
                if (u !== undefined && u.htmlUrl.length > 0) {
                  router.openUrl({ url: u.htmlUrl });
                }
              })
            }
          }
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .width('100%')
      .padding(16)
      .margin({ left: 14, right: 14, top: 6 })
      .borderRadius(16)
      .backgroundColor('#59FFFFFF')
      .backdropBlur(20)
      .border({ width: 0.5, color: '#99FFFFFF' })

      Text('更新日志（点击展开）').fontSize(14).fontWeight(FontWeight.Bold)
        .margin({ left: 16, top: 12, bottom: 6 }).width('100%')

      if (!this.loaded) {
        Column() {
          LoadingProgress().width(40).height(40)
        }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else if (this.releases.length === 0) {
        Column() {
          Text('暂无发布记录（请检查网络）').fontSize(13).fontColor('#999999')
        }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        List({ space: 8 }) {
          ForEach(this.releases, (r: GitHubRelease) => {
            ListItem() {
              Column({ space: 6 }) {
                Row() {
                  Column({ space: 2 }) {
                    Text(r.tagName + (r.prerelease ? '（预发布）' : '')).fontSize(14).fontWeight(FontWeight.Bold)
                    Text(`发布于 ${r.publishedAt.substring(0, 10)}`).fontSize(11).fontColor('#999999')
                  }
                  .layoutWeight(1)
                  .alignItems(HorizontalAlign.Start)
                  Text(this.expandedTag === r.tagName ? '▲' : '▼').fontSize(12).fontColor('#999999')
                }
                .width('100%')
                .onClick(() => {
                  this.toggle(r.tagName);
                })
                if (this.expandedTag === r.tagName) {
                  Text(r.body.length > 0 ? r.body : '（无详细说明）').fontSize(12).fontColor('#555555')
                    .lineHeight(18).width('100%')
                  if (r.htmlUrl.length > 0) {
                    Text('前往 Releases 下载 ›').fontSize(12).fontWeight(FontWeight.Bold).fontColor('#C62F2F')
                      .margin({ top: 4 }).onClick(() => {
                      router.openUrl({ url: r.htmlUrl });
                    })
                  }
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
          }, (r: GitHubRelease) => r.tagName)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 14, right: 14, top: 6 })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F2F5FA')
  }
}
