import { router, promptAction } from '@kit.ArkUI';
import { UpdateChecker } from '../common/UpdateChecker';
import { GitHubApi, Announcement } from '../common/GitHubApi';
import { Constants } from '../common/Constants';

/**
 * 璃光 GlassSuite 多功能首页（液态玻璃功能中心）：
 * 音乐 / GitHub 检索 / 更新中心 / 公告推送 / 设置
 * 自动更新检查：启动即查 + 30 分钟轮询，发现新版本弹窗推送
 */
@Entry
@Component
struct GlassHomePage {
  @State hasUpdate: boolean = false;
  @State newAnn: number = 0;
  @State latestTag: string = '';
  @State annTitle: string = '';
  @State annContent: string = '';
  @State annIsPromo: boolean = false;
  @State showUpdateDialog: boolean = false;
  @State showAnnDialog: boolean = false;
  @State annDialogContent: string = '';
  @State annDialogTitle: string = '';
  private checker = UpdateChecker.getInstance();

  aboutToAppear(): void {
    this.checker.start();
    this.sync();
    this.checker.onChange(() => {
      this.sync();
    });
  }

  sync(): void {
    this.hasUpdate = this.checker.hasUpdate;
    this.newAnn = this.checker.newAnnouncements;
    this.latestTag = this.checker.latest?.tagName ?? '';
    const list = this.checker.announcements.length > 0 ? this.checker.announcements : GitHubApi.builtinAnnouncements();
    if (list.length > 0) {
      this.annTitle = list[0].title;
      this.annContent = list[0].content.replace(/\n/g, ' ').substring(0, 40);
      this.annIsPromo = list[0].isPromo;
    }
    if (this.hasUpdate) {
      this.showUpdateDialog = true;
    }
  }

  openAnnouncement(): void {
    const list = this.checker.announcements.length > 0 ? this.checker.announcements : GitHubApi.builtinAnnouncements();
    if (list.length > 0) {
      this.annDialogTitle = (list[0].isPromo ? '推广 · ' : '公告 · ') + list[0].title;
      this.annDialogContent = list[0].content;
      this.showAnnDialog = true;
    }
    this.checker.markSeen();
    this.newAnn = 0;
  }

  openModule(url: string): void {
    router.pushUrl({ url: url });
  }

  @Builder
  GlassCard(title: string, subtitle: string, color: string, badge: string, url: string) {
    Column({ space: 6 }) {
      Row() {
        Text(title).fontSize(17).fontWeight(FontWeight.Bold).fontColor('#222222')
        if (badge.length > 0) {
          Text(badge).fontSize(10).fontColor('#FFFFFF').backgroundColor('#EC4141')
            .borderRadius(8).padding({ left: 6, right: 6, top: 2, bottom: 2 }).margin({ left: 8 })
        }
      }
      .width('100%')
      Text(subtitle).fontSize(12).fontColor('#666666').maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis }).width('100%')
    }
    .width('100%')
    .padding(16)
    .borderRadius(18)
    .backgroundColor('#59FFFFFF')
    .backdropBlur(20)
    .border({ width: 0.5, color: '#99FFFFFF' })
    .shadow({ radius: 12, color: '#14000000', offsetY: 4 })
    .onClick(() => {
      this.openModule(url);
    })
    .alignItems(HorizontalAlign.Start)
  }

  build() {
    Scroll() {
      Column() {
        // 标题区
        Row() {
          Column({ space: 4 }) {
            Text('璃光').fontSize(30).fontWeight(FontWeight.Bold).fontColor('#1A1A1A')
            Text('GlassSuite · 多功能应用套件').fontSize(12).fontColor('#777777')
          }
          .alignItems(HorizontalAlign.Start)
          Blank()
          Text(`v${UpdateChecker.VERSION}`).fontSize(11).fontColor('#888888').margin({ top: 10 })
        }
        .width('100%')
        .padding({ left: 20, right: 20, top: 20, bottom: 8 })

        // 公告横幅
        Row({ space: 10 }) {
          Text('📢').fontSize(18)
          Column({ space: 2 }) {
            Text((this.annIsPromo ? '推广 · ' : '公告 · ') + this.annTitle)
              .fontSize(13).fontWeight(FontWeight.Bold).fontColor('#333333').maxLines(1)
            Text(this.annContent).fontSize(11).fontColor('#777777').maxLines(1)
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
          if (this.newAnn > 0) {
            Text('NEW').fontSize(9).fontColor('#FFFFFF').backgroundColor('#EC4141')
              .borderRadius(6).padding({ left: 5, right: 5, top: 2, bottom: 2 })
          }
          Text('›').fontSize(20).fontColor('#BBBBBB')
        }
        .width('100%')
        .padding(14)
        .borderRadius(16)
        .backgroundColor('#59FFFFFF')
        .backdropBlur(20)
        .border({ width: 0.5, color: '#99FFFFFF' })
        .margin({ left: 20, right: 20, top: 8, bottom: 14 })
        .onClick(() => {
          this.openAnnouncement();
        })

        // 模块网格
        Grid() {
          GridItem() {
            this.GlassCard('♪ 音乐', '播放 · 歌词 · 歌单 · 锁屏', '#EC4141', '', 'pages/HomePage')
          }
          GridItem() {
            this.GlassCard('⌥ GitHub 检索', '热门仓库 · 关键字搜索', '#3D7FFF', '', 'pages/GitHubPage')
          }
          GridItem() {
            this.GlassCard('↻ 更新中心', '自查更新 · 折叠日志', '#00B578', this.hasUpdate ? '有新版本' : '', 'pages/UpdateCenterPage')
          }
          GridItem() {
            this.GlassCard('📢 公告推送', '公告文件 · 推广信息', '#FF8000', this.newAnn > 0 ? `${this.newAnn} 新` : '', 'pages/AnnouncementPage')
          }
          GridItem() {
            this.GlassCard('⚙ 设置', '主题 · 服务器 · 仓库', '#7B5CFF', '', 'pages/SettingsPage')
          }
        }
        .columnsTemplate('1fr 1fr')
        .columnsGap(12)
        .rowsGap(12)
        .height(430)
        .padding({ left: 20, right: 20 })

        Text('© 2026 GlassSuite · 多功能应用套件').fontSize(11).fontColor('#AAAAAA')
          .margin({ top: 16, bottom: 24 })
      }
    }
    .width('100%')
    .height('100%')
    .background(LinearGradient({
      colors: [['#EAF1FB', 0], ['#F7F3FF', 0.5], ['#E8F4FA', 1]]
    }))
    .scrollBar(BarState.Off)

    // 更新弹窗
    if (this.showUpdateDialog) {
      this.UpdateDialog()
    }
    // 公告详情弹窗
    if (this.showAnnDialog) {
      this.AnnDialog()
    }
  }

  @Builder
  UpdateDialog() {
    Column({ space: 12 }) {
      Text(`发现新版本 ${this.latestTag}`).fontSize(17).fontWeight(FontWeight.Bold).fontColor('#222222')
      Text('新版本已发布，是否前往 GitHub Releases 下载？').fontSize(13).fontColor('#666666')
      Row({ space: 12 }) {
        Button('稍后').backgroundColor('#F2F2F2').fontColor('#666666').onClick(() => {
          this.showUpdateDialog = false;
        })
        Button('前往下载').backgroundColor('#EC4141').onClick(() => {
          this.showUpdateDialog = false;
          router.openUrl({
            url: `https://github.com/${Constants.ghOwner}/${Constants.ghRepo}/releases`
          });
        })
      }
    }
    .width('82%')
    .padding(20)
    .borderRadius(20)
    .backgroundColor('#F2FFFFFF')
    .backdropBlur(30)
    .border({ width: 0.5, color: '#99FFFFFF' })
    .shadow({ radius: 24, color: '#33000000' })
    .position({ x: '9%', y: '38%' })
  }

  @Builder
  AnnDialog() {
    Column({ space: 10 }) {
      Text(this.annDialogTitle).fontSize(16).fontWeight(FontWeight.Bold).fontColor('#222222')
      Text(this.annDialogContent).fontSize(13).fontColor('#555555').lineHeight(20)
      Button('知道了').backgroundColor('#EC4141').width('100%').onClick(() => {
        this.showAnnDialog = false;
      })
    }
    .width('82%')
    .padding(20)
    .borderRadius(20)
    .backgroundColor('#F2FFFFFF')
    .backdropBlur(30)
    .border({ width: 0.5, color: '#99FFFFFF' })
    .shadow({ radius: 24, color: '#33000000' })
    .position({ x: '9%', y: '35%' })
  }
}
