import { router, promptAction } from '@kit.ArkUI';
import { BetaStore, BetaRequirements, BetaApplyResult } from '../common/BetaStore';

/** 开发者尝鲜模式：要求展示 → 申请（实时评分）→ 通过显示尝鲜码/驳回原因 → 密钥激活 */
@Entry
@Component
struct BetaPage {
  @State req: BetaRequirements = new BetaRequirements();
  @State email: string = '';
  @State name: string = '';
  @State purpose: number = 0;
  @State reason: string = '';
  @State device: string = '';
  @State busy: boolean = false;
  @State result: BetaApplyResult = new BetaApplyResult();
  @State hasResult: boolean = false;
  @State keyInput: string = '';
  @State activateMsg: string = '';
  @State activateColor: string = '#00B578';
  private purposes: string[] = ['开发者', '测试', '媒体评测', '普通体验'];

  aboutToAppear(): void {
    BetaStore.requirements().then((r: BetaRequirements) => {
      this.req = r;
    });
    if (BetaStore.betaAccess) {
      this.activateMsg = `已激活尝鲜权限 · 尝鲜码：${BetaStore.betaKey}`;
    }
  }

  submit(): void {
    if (this.busy) return;
    this.busy = true;
    BetaStore.apply(this.email, this.name, this.purposes[this.purpose], this.reason, this.device)
      .then((r: BetaApplyResult) => {
        this.result = r;
        this.hasResult = true;
        if (r.status === 'approved') {
          this.activateMsg = '✅ 申请通过，尝鲜码已自动激活！可接收 Beta 版推送';
          this.activateColor = '#00B578';
        } else {
          this.activateMsg = '';
        }
        this.busy = false;
      });
  }

  verifyKey(): void {
    BetaStore.verify(this.keyInput.trim()).then((ok: boolean) => {
      this.activateColor = ok ? '#00B578' : '#E84026';
      this.activateMsg = ok
        ? '✅ 尝鲜码有效，已激活！可接收 Beta 版推送（当前 v1.1.0-beta.1）'
        : '❌ 尝鲜码无效或已被停用，请核对后重试';
    });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('开发者尝鲜').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      Scroll() {
        Column({ space: 10 }) {
          // 申请要求
          Column({ space: 6 }) {
            Text(this.req.title.length > 0 ? this.req.title : '开发者尝鲜计划').fontSize(16).fontWeight(FontWeight.Bold)
            Text(this.req.intro).fontSize(12).fontColor('#666666')
            ForEach(this.req.items, (item: string, idx: number) => {
              Text(`${idx + 1}. ${item}`).fontSize(12).fontColor('#555555').lineHeight(18).width('100%')
            }, (item: string, idx: number) => `r-${idx}`)
            Text(`评分阈值：≥ ${this.req.threshold} 分通过 · 尝鲜码 22~37 位唯一`)
              .fontSize(11).fontColor('#E8890C')
          }
          .width('100%')
          .padding(14)
          .borderRadius(14)
          .backgroundColor('#59FFFFFF')
          .backdropBlur(20)
          .border({ width: 0.5, color: '#99FFFFFF' })
          .alignItems(HorizontalAlign.Start)

          // 申请表单
          Column({ space: 8 }) {
            TextInput({ text: this.email, placeholder: '邮箱（用于接收结果）' })
              .onChange((v: string) => { this.email = v; })
            TextInput({ text: this.name, placeholder: '姓名/昵称' })
              .onChange((v: string) => { this.name = v; })
            Row({ space: 6 }) {
              ForEach(this.purposes, (p: string, idx: number) => {
                Text(p)
                  .fontSize(12)
                  .fontColor(this.purpose === idx ? '#FFFFFF' : '#666666')
                  .backgroundColor(this.purpose === idx ? '#C62F2F' : '#F2F2F2')
                  .borderRadius(12)
                  .padding({ left: 10, right: 10, top: 5, bottom: 5 })
                  .onClick(() => { this.purpose = idx; })
              }, (p: string, idx: number) => `p-${idx}`)
            }
            TextArea({ text: this.reason, placeholder: '申请理由（不少于 15 字）' })
              .height(80)
              .onChange((v: string) => { this.reason = v; })
            TextInput({ text: this.device, placeholder: '设备信息（如：Pixel 8 / Android 15）' })
              .onChange((v: string) => { this.device = v; })
            Button(this.busy ? '评估中…' : '提交申请（实时达标评估）')
              .backgroundColor('#C62F2F')
              .width('100%')
              .enabled(!this.busy)
              .onClick(() => { this.submit(); })
          }
          .width('100%')
          .padding(14)
          .borderRadius(14)
          .backgroundColor('#59FFFFFF')
          .backdropBlur(20)
          .border({ width: 0.5, color: '#99FFFFFF' })

          // 结果
          if (this.hasResult) {
            Column({ space: 6 }) {
              Text(this.result.status === 'approved' ? '✅ 申请通过！' : '❌ 申请未通过（可修改后重新申请）')
                .fontSize(15)
                .fontWeight(FontWeight.Bold)
                .fontColor(this.result.status === 'approved' ? '#00B578' : '#E84026')
              Text(`评分：${this.result.score} / 90（阈值 60）`).fontSize(12).fontColor('#666666')
              if (this.result.status === 'approved') {
                Text(`你的尝鲜码（${this.result.key.length} 位）：\n${this.result.key}`)
                  .fontSize(13)
                  .fontWeight(FontWeight.Bold)
                  .fontColor('#C62F2F')
                  .lineHeight(20)
              } else {
                ForEach(this.result.reasons, (r: string) => {
                  Text(`· ${r}`).fontSize(12).fontColor('#E84026').width('100%')
                }, (r: string) => `reason-${r}`)
              }
              Text(this.result.message).fontSize(12).fontColor('#555555').lineHeight(18)
            }
            .width('100%')
            .padding(14)
            .borderRadius(14)
            .backgroundColor('#59FFFFFF')
            .backdropBlur(20)
            .border({ width: 0.5, color: '#99FFFFFF' })
            .alignItems(HorizontalAlign.Start)
          }

          // 激活
          Column({ space: 8 }) {
            Text('已有尝鲜码？输入激活（22~37 位）').fontSize(14).fontWeight(FontWeight.Bold)
            TextInput({ text: this.keyInput, placeholder: '粘贴尝鲜码' })
              .onChange((v: string) => { this.keyInput = v; })
            Button('激活尝鲜码').backgroundColor('#C62F2F').width('100%').onClick(() => { this.verifyKey(); })
            if (this.activateMsg.length > 0) {
              Text(this.activateMsg).fontSize(12).fontColor(this.activateColor).lineHeight(18)
            }
          }
          .width('100%')
          .padding(14)
          .borderRadius(14)
          .backgroundColor('#59FFFFFF')
          .backdropBlur(20)
          .border({ width: 0.5, color: '#99FFFFFF' })
          .alignItems(HorizontalAlign.Start)

          Text('未通过评估时，更新中心仅监测并推送正式版；通过后可接收 Beta 版推送。')
            .fontSize(11).fontColor('#AAAAAA').lineHeight(16).margin({ bottom: 20 })
        }
        .width('100%')
        .padding({ left: 14, right: 14, top: 6 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F2F5FA')
  }
}
