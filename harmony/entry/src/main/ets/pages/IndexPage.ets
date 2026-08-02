import { router } from '@kit.ArkUI';
import { Constants } from '../common/Constants';

@Entry
@Component
struct IndexPage {
  aboutToAppear(): void {
    setTimeout(() => {
      if (Constants.isLoggedIn()) {
        router.replaceUrl({ url: 'pages/HomePage' });
      } else {
        router.replaceUrl({ url: 'pages/LoginPage' });
      }
    }, 300);
  }

  build() {
    Column({ space: 16 }) {
      Text('乐云音乐').fontSize(30).fontWeight(FontWeight.Bold).fontColor('#C62F2F')
      LoadingProgress().width(40).height(40)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
