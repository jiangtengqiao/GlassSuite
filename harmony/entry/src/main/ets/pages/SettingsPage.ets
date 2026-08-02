import { router, promptAction } from '@kit.ArkUI';
import { Constants } from '../common/Constants';
import { Net } from '../common/Net';
import { BetaStore } from '../common/BetaStore';
import { Utils } from '../common/Utils';

interface LegalDoc {
  title: string;
  file: string;
}

@Entry
@Component
struct SettingsPage {
  @State apiUrl: string = Constants.apiBaseUrl;
  @State accentHex: string = Constants.accentHex;
  @State darkMode: boolean = Constants.darkMode;
  @State lyricFontSize: number = Constants.lyricFontSize;
  @State lyricOffset: number = Constants.lyricOffsetMs;
  @State lyricMode: number = Constants.lyricMode;
  @State diyText: string = Constants.diyLyric;
  @State themeMode: number = 0;
  @State ghOwner: string = Constants.ghOwner;
  @State ghRepo: string = Constants.ghRepo;
  @State betaUrl: string = BetaStore.betaServerUrl;
  @State savedMsg: string = '';
  @State showDoc: boolean = false;
  @State docTitle: string = '';
  @State docContent: string = '';

  private colors: string[] = ['#EC4141', '#C62F2F', '#00B578', '#3D7FFF', '#7B5CFF', '#FF8000'];
  private colorNames: string[] = ['网易红', '绯红', '薄荷绿', '海盐蓝', '暗夜紫', '琥珀橙'];
  private docs: LegalDoc[] = [
    { title: '用户协议', file: '01-用户协议.txt' },
    { title: '隐私政策', file: '02-隐私政策.txt' },
    { title: '儿童个人信息保护规则', file: '03-儿童个人信息保护规则.txt' },
    { title: '版权声明与侵权投诉', file: '04-版权声明与侵权投诉.txt' },
    { title: '免责声明', file: '05-免责声明.txt' },
    { title: '第三方SDK与权限清单', file: '06-第三方SDK与权限清单.txt' },
    { title: '账号注销指引', file: '07-账号注销指引.txt' },
    { title: '投诉与举报', file: '08-投诉与举报.txt' },
    { title: '用户行为规范', file: '09-用户行为规范.txt' }
  ];

  saveApi(): void {
    const url = this.apiUrl.trim();
    if (url.length === 0) {
      this.savedMsg = '地址不能为空';
      return;
    }
    Constants.apiBaseUrl = url;
    Net.put('api_base', url);
    this.savedMsg = `服务器地址已保存：${url}`;
    promptAction.showToast({ message: this.savedMsg });
  }

  setAccent(hex: string, name: string): void {
    Constants.accentHex = hex;
    Net.put('accent', hex);
    promptAction.showToast({ message: `主题色已切换：${name}` });
  }

  setDark(v: boolean): void {
    this.darkMode = v;
    Constants.darkMode = v;
    Net.put('dark', v);
  }

  setMode(mode: number): void {
    this.lyricMode = mode;
    Constants.lyricMode = mode;
    Net.put('lyric_mode', mode);
  }

  saveDiy(): void {
    Constants.diyLyric = this.diyText;
    Net.put('diy_lyric', this.diyText);
    promptAction.showToast({ message: 'DIY 歌词已保存' });
  }

  resetOffset(): void {
    this.lyricOffset = 0;
    Constants.lyricOffsetMs = 0;
    Net.put('lyric_offset', 0);
  }

  openDoc(doc: LegalDoc): void {
    this.docTitle = doc.title;
    this.docContent = '加载中…';
    this.showDoc = true;
    try {
      getContext(this).resourceManager.getRawFileContent(`legal/${doc.file}`).then((data: Uint8Array) => {
        const decoder = util.TextDecoder.create('utf-8');
        this.docContent = decoder.decodeToString(data);
      }).catch(() => {
        this.docContent = '文档加载失败';
      });
    } catch (e) {
      this.docContent = '文档加载失败';
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('设置').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      Scroll() {
        Column({ space: 14 }) {
          // 服务器
          Text('服务器').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#C62F2F').margin({ top: 8 })
          TextInput({ text: this.apiUrl, placeholder: 'API 服务器地址' })
            .onChange((value: string) => {
              this.apiUrl = value;
            })
            .height(44)
            .borderRadius(8)
          Button('保存并应用')
            .backgroundColor('#C62F2F')
            .width('100%')
            .onClick(() => {
              this.saveApi();
            })
          if (this.savedMsg.length > 0) {
            Text(this.savedMsg).fontSize(12).fontColor('#C62F2F')
          }

          // 主题定制
          Text('主题定制').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#C62F2F').margin({ top: 8 })
          Row({ space: 12 }) {
            ForEach(this.colors, (hex: string, idx: number) => {
              Column({ space: 4 }) {
                Circle({ width: 36, height: 36 })
                  .fill(hex)
                  .stroke(this.accentHex === hex ? '#333333' : 'transparent')
                  .strokeWidth(2)
                  .onClick(() => {
                    this.accentHex = hex;
                    this.setAccent(hex, this.colorNames[idx]);
                  })
                Text(this.colorNames[idx]).fontSize(10).fontColor('#999999')
              }
            }, (hex: string, idx: number) => `${hex}-${idx}`)
          }

          Row({ space: 8 }) {
            Text('深色模式').fontSize(15).layoutWeight(1)
            Toggle({ type: ToggleType.Switch, isOn: this.darkMode })
              .selectedColor('#C62F2F')
              .onChange((isOn: boolean) => {
                this.setDark(isOn);
              })
          }
          .padding({ top: 4 })

          // 外观模式（三态）
          Text('外观模式').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#C62F2F').margin({ top: 8 })
          Row({ space: 8 }) {
            ForEach(['跟随系统', '浅色', '深色'], (name: string, idx: number) => {
              Text(name)
                .fontSize(13)
                .fontColor(this.themeMode === idx ? '#FFFFFF' : '#666666')
                .backgroundColor(this.themeMode === idx ? '#C62F2F' : '#F2F2F2')
                .borderRadius(14)
                .padding({ left: 14, right: 14, top: 5, bottom: 5 })
                .onClick(() => {
                  this.themeMode = idx;
                  Net.put('theme_mode', idx);
                })
            }, (name: string, idx: number) => `${name}-${idx}`)
          }

          // 开发者尝鲜
          Text('开发者尝鲜').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#C62F2F').margin({ top: 8 })
          Button('开发者尝鲜模式（申请/激活尝鲜码）')
            .backgroundColor('#C62F2F')
            .width('100%')
            .onClick(() => {
              router.pushUrl({ url: 'pages/BetaPage' });
            })
          Row({ space: 8 }) {
            TextInput({ text: this.betaUrl, placeholder: 'Beta 服务器地址' })
              .layoutWeight(1)
              .height(40)
              .borderRadius(8)
              .onChange((v: string) => {
                this.betaUrl = v;
              })
            Button('保存').backgroundColor('#C62F2F').onClick(() => {
              BetaStore.betaServerUrl = this.betaUrl.trim();
              Net.put('beta_url', this.betaUrl.trim());
              promptAction.showToast({ message: 'Beta 服务器已保存' });
            })
          }

          // GitHub 仓库（更新/公告数据源）
          Text('GitHub 仓库').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#C62F2F').margin({ top: 8 })
          Row({ space: 8 }) {
            TextInput({ text: this.ghOwner, placeholder: 'Owner' })
              .layoutWeight(1)
              .height(40)
              .borderRadius(8)
              .onChange((v: string) => {
                this.ghOwner = v;
              })
            TextInput({ text: this.ghRepo, placeholder: 'Repo' })
              .layoutWeight(1)
              .height(40)
              .borderRadius(8)
              .onChange((v: string) => {
                this.ghRepo = v;
              })
          }
          Button('保存 GitHub 仓库')
            .backgroundColor('#C62F2F')
            .width('100%')
            .onClick(() => {
              Constants.ghOwner = this.ghOwner.trim();
              Constants.ghRepo = this.ghRepo.trim();
              Net.put('gh_owner', Constants.ghOwner);
              Net.put('gh_repo', Constants.ghRepo);
              promptAction.showToast({ message: `已保存：${Constants.ghOwner}/${Constants.ghRepo}` });
            })

          // 歌词设置
          Text('歌词设置').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#C62F2F').margin({ top: 8 })
          Row({ space: 8 }) {
            Text('歌词字号：').fontSize(14)
            Text(`${this.lyricFontSize}`).fontSize(14)
            Text('−').fontSize(22).fontColor('#C62F2F').onClick(() => {
              this.lyricFontSize = Math.max(12, this.lyricFontSize - 1);
              Constants.lyricFontSize = this.lyricFontSize;
              Net.put('lyric_size', this.lyricFontSize);
            })
            Text('+').fontSize(22).fontColor('#C62F2F').onClick(() => {
              this.lyricFontSize = Math.min(32, this.lyricFontSize + 1);
              Constants.lyricFontSize = this.lyricFontSize;
              Net.put('lyric_size', this.lyricFontSize);
            })
            Blank()
            Text(`偏移 ${(this.lyricOffset / 1000).toFixed(1)}s`).fontSize(14)
            Text('复位').fontSize(14).fontColor('#C62F2F').onClick(() => {
              this.resetOffset();
            })
          }

          Row({ space: 8 }) {
            Text('歌词模式').fontSize(14)
            ForEach(['原词', '翻译', '罗马音', 'DIY'], (name: string, idx: number) => {
              Text(name)
                .fontSize(13)
                .fontColor(this.lyricMode === idx ? '#FFFFFF' : '#666666')
                .backgroundColor(this.lyricMode === idx ? '#C62F2F' : '#F2F2F2')
                .borderRadius(14)
                .padding({ left: 12, right: 12, top: 5, bottom: 5 })
                .onClick(() => {
                  this.setMode(idx);
                })
            }, (name: string, idx: number) => `${name}-${idx}`)
          }

          TextArea({ text: this.diyText, placeholder: 'DIY 歌词（LRC 格式，粘贴后保存）' })
            .height(110)
            .fontSize(13)
            .onChange((value: string) => {
              this.diyText = value;
            })
          Button('保存 DIY 歌词')
            .backgroundColor('#C62F2F')
            .width('100%')
            .onClick(() => {
              this.saveDiy();
            })

          // 关于与法律
          Text('关于与法律').fontSize(14).fontWeight(FontWeight.Bold).fontColor('#C62F2F').margin({ top: 8 })
          ForEach(this.docs, (doc: LegalDoc) => {
            Row() {
              Text(doc.title).fontSize(15).layoutWeight(1)
              Text('›').fontColor('#BBBBBB').fontSize(18)
            }
            .width('100%')
            .padding({ top: 10, bottom: 10 })
            .onClick(() => {
              this.openDoc(doc);
            })
          }, (doc: LegalDoc) => doc.file)

          Text('璃光 GlassSuite v1.1.0\n仅供学习交流使用')
            .fontSize(12)
            .fontColor('#BBBBBB')
            .textAlign(TextAlign.Center)
            .width('100%')
            .margin({ top: 12, bottom: 24 })
        }
        .width('100%')
        .padding({ left: 16, right: 16 })
      }
      .layoutWeight(1)

      if (this.showDoc) {
        Column() {
          Row() {
            Text('‹').fontSize(26).onClick(() => {
              this.showDoc = false;
            }).width(40).height(40).textAlign(TextAlign.Center)
            Text(this.docTitle).fontSize(16).fontWeight(FontWeight.Bold).layoutWeight(1)
              .textAlign(TextAlign.Center)
            Blank().width(40)
          }
          .width('100%')
          .height(44)

          Scroll() {
            Text(this.docContent).fontSize(13).lineHeight(20).fontColor('#333333')
              .padding({ left: 20, right: 20, top: 8, bottom: 24 })
          }
          .layoutWeight(1)
          .scrollBar(BarState.Auto)
        }
        .width('100%')
        .height('100%')
        .backgroundColor('#FFFFFF')
        .position({ x: 0, y: 0 })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}

import { util } from '@kit.ArkTS';
