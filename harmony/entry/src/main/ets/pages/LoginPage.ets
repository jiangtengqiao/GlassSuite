import { router, promptAction } from '@kit.ArkUI';
import { image } from '@kit.ImageKit';
import { Api } from '../common/Api';
import { Constants } from '../common/Constants';
import { Net } from '../common/Net';
import { Utils } from '../common/Utils';
import { Profile } from '../model/Models';

@Entry
@Component
struct LoginPage {
  @State tab: number = 0;
  @State qrPixelMap: image.PixelMap | undefined = undefined;
  @State qrStatus: string = '正在获取二维码…';
  @State phone: string = '';
  @State code: string = '';
  @State smsStatus: string = '';
  @State countdown: number = 0;
  @State sending: boolean = false;
  private qrKey: string = '';
  private qrBusy: boolean = false;
  private timer: number = -1;
  private cdTimer: number = -1;

  aboutToDisappear(): void {
    if (this.timer >= 0) clearInterval(this.timer);
    if (this.cdTimer >= 0) clearInterval(this.cdTimer);
  }

  startQr(): void {
    this.refreshQr();
    this.timer = setInterval(() => {
      this.refreshQr();
    }, 1500);
  }

  async refreshQr(): Promise<void> {
    if (this.qrBusy) return;
    this.qrBusy = true;
    try {
      if (this.qrKey.length === 0) {
        const key = await Api.qrKey();
        if (key.length === 0) {
          this.qrStatus = '获取二维码失败，请检查服务器地址';
        } else {
          this.qrKey = key;
          const qr = await Api.qrCreate(key);
          this.qrPixelMap = await Utils.base64DataUriToPixelMap(qr.qrimg);
          this.qrStatus = '请使用网易云音乐 App 扫码登录';
        }
      } else {
        const check = await Api.qrCheck(this.qrKey);
        if (check.code === 800) {
          this.qrKey = '';
          this.qrStatus = '二维码已过期，正在刷新…';
        } else if (check.code === 801) {
          this.qrStatus = '等待扫码…';
        } else if (check.code === 802) {
          this.qrStatus = '已扫码，请在手机端确认登录';
        } else if (check.code === 803) {
          this.qrStatus = '登录成功';
          Net.saveCookieString(check.cookie);
          const profile = await Api.loginStatus();
          this.completeLogin(profile);
        }
      }
    } catch (e) {
      this.qrStatus = '网络异常，请检查服务器地址';
    }
    this.qrBusy = false;
  }

  completeLogin(profile: Profile): void {
    if (profile.userId <= 0) {
      this.qrStatus = '登录态获取失败，重试中…';
      return;
    }
    Constants.userId = profile.userId;
    Constants.profileJson = JSON.stringify(profile);
    Net.put('user_id', profile.userId);
    Net.put('profile_json', Constants.profileJson);
    if (this.timer >= 0) clearInterval(this.timer);
    promptAction.showToast({ message: '登录成功' });
    router.replaceUrl({ url: 'pages/HomePage' });
  }

  sendCode(): void {
    if (this.phone.length !== 11) {
      this.smsStatus = '请输入 11 位手机号';
      return;
    }
    this.sending = true;
    Api.captchaSent(this.phone).then((code: number) => {
      this.sending = false;
      if (code === 200) {
        this.smsStatus = '验证码已发送';
        this.countdown = 60;
        this.cdTimer = setInterval(() => {
          this.countdown--;
          if (this.countdown <= 0 && this.cdTimer >= 0) {
            clearInterval(this.cdTimer);
            this.cdTimer = -1;
          }
        }, 1000);
      } else {
        this.smsStatus = '发送失败，请稍后重试';
      }
    }).catch(() => {
      this.sending = false;
      this.smsStatus = '发送失败：网络异常';
    });
  }

  smsLogin(): void {
    if (this.phone.length !== 11) {
      this.smsStatus = '请输入 11 位手机号';
      return;
    }
    if (this.code.length < 4) {
      this.smsStatus = '请输入验证码';
      return;
    }
    Api.loginCellphone(this.phone, this.code).then((res) => {
      if (res.code === 200) {
        Net.saveCookieString(res.cookie);
        this.completeLogin(res.profile);
      } else {
        this.smsStatus = res.message.length > 0 ? res.message : '登录失败（验证码错误或手机号未注册）';
      }
    }).catch(() => {
      this.smsStatus = '登录失败：网络异常';
    });
  }

  build() {
    Column() {
      Tabs({ barPosition: BarPosition.Start, index: this.tab }) {
        TabContent() {
          this.QrPanel()
        }.tabBar('扫码登录')

        TabContent() {
          this.SmsPanel()
        }.tabBar('验证码登录')
      }
      .onChange((index: number) => {
        this.tab = index;
        if (index === 0) this.startQr();
      })
      .height('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }

  @Builder
  QrPanel() {
    Column({ space: 20 }) {
      if (this.qrPixelMap !== undefined) {
        Image(this.qrPixelMap)
          .width(240)
          .height(240)
          .borderRadius(12)
      } else {
        LoadingProgress().width(48).height(48)
      }
      Text(this.qrStatus).fontSize(14).fontColor('#666666')
      Text('1. 打开网易云音乐 App\n2. 点击「我的」右上角扫码\n3. 扫描二维码并确认登录')
        .fontSize(12)
        .fontColor('#999999')
        .textAlign(TextAlign.Center)
        .lineHeight(20)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  SmsPanel() {
    Column({ space: 16 }) {
      TextInput({ text: this.phone, placeholder: '请输入手机号' })
        .type(InputType.PhoneNumber)
        .maxLength(11)
        .onChange((value: string) => {
          this.phone = value;
        })
        .width('88%')
        .height(48)
        .borderRadius(8)

      Row({ space: 12 }) {
        TextInput({ text: this.code, placeholder: '请输入验证码' })
          .type(InputType.Number)
          .maxLength(6)
          .onChange((value: string) => {
            this.code = value;
          })
          .layoutWeight(1)
          .height(48)
          .borderRadius(8)
        Button(this.countdown > 0 ? `${this.countdown}s` : '发送验证码')
          .enabled(this.countdown === 0 && !this.sending)
          .height(48)
          .onClick(() => {
            this.sendCode();
          })
      }
      .width('88%')

      Button('登 录')
        .width('88%')
        .height(48)
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .onClick(() => {
          this.smsLogin();
        })

      Text(this.smsStatus).fontSize(13).fontColor('#E84026')
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
