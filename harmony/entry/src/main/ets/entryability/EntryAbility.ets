import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';
import { Net } from '../common/Net';
import { PlayerManager } from '../common/PlayerManager';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(0x0000, 'CloudMusic', 'EntryAbility onCreate');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    Net.init(this.context);
    PlayerManager.getInstance().init(this.context);
    windowStage.loadContent('pages/IndexPage', (err) => {
      if (err.code) {
        hilog.error(0x0000, 'CloudMusic', 'loadContent failed: %{public}s', JSON.stringify(err));
      }
    });
  }
}
