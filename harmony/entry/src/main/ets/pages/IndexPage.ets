import { router } from '@kit.ArkUI';
import { Constants } from '../common/Constants';

/** 启动门卫：直接进入多功能首页（音乐模块内再处理登录） */
@Entry
@Component
struct IndexPage {
  aboutToAppear(): void {
    setTimeout(() => {
      router.replaceUrl({ url: 'pages/GlassHomePage' });
    }, 300);
  }

  build() {
    Column({ space: 16 }) {
      Text('璃光 GlassSuite').fontSize(30).fontWeight(FontWeight.Bold).fontColor('#C62F2F')
      Text('多功能应用套件').fontSize(14).fontColor('#999999')
      LoadingProgress().width(40).height(40)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#F2F5FA')
  }
}
