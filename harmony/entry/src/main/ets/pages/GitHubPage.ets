import { router, promptAction } from '@kit.ArkUI';
import { GitHubApi, GitHubRepo } from '../common/GitHubApi';

/** GitHub 检索：热门仓库 + 关键字搜索 */
@Entry
@Component
struct GitHubPage {
  @State tab: number = 0;
  @State query: string = '';
  @State repos: GitHubRepo[] = [];
  @State loading: boolean = true;
  @State searched: boolean = false;

  aboutToAppear(): void {
    this.load();
  }

  async load(): Promise<void> {
    this.loading = true;
    try {
      this.repos = await GitHubApi.searchRepos(this.query, 30);
    } catch (e) {
      this.repos = [];
    }
    this.loading = false;
  }

  doSearch(): void {
    this.searched = this.query.trim().length > 0;
    this.load();
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(26).onClick(() => {
          router.back();
        }).width(40).height(40).textAlign(TextAlign.Center)
        Text('GitHub 检索').fontSize(17).fontWeight(FontWeight.Bold).layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(40)
      }
      .width('100%')
      .height(48)
      .alignItems(VerticalAlign.Center)

      Tabs({ barPosition: BarPosition.Start, index: this.tab }) {
        TabContent() {
          this.RepoList()
        }.tabBar('热门')
        TabContent() {
          Column({ space: 10 }) {
            Row({ space: 8 }) {
              TextInput({ text: this.query, placeholder: '搜索仓库，如 kotlin compose' })
                .layoutWeight(1)
                .height(40)
                .borderRadius(20)
                .onChange((v: string) => {
                  this.query = v;
                })
                .onSubmit(() => {
                  this.doSearch();
                })
              Button('搜索').backgroundColor('#C62F2F').onClick(() => {
                this.doSearch();
              })
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 8 })
            this.RepoList()
          }
          .width('100%')
          .height('100%')
        }.tabBar('搜索')
      }
      .onChange((index: number) => {
        this.tab = index;
        if (index === 0) this.load();
      })
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }

  @Builder
  RepoList() {
    if (this.loading) {
      Column() {
        LoadingProgress().width(40).height(40)
      }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
    } else if (this.repos.length === 0) {
      Column() {
        Text('暂无数据（网络受限或 API 限流）').fontSize(13).fontColor('#999999')
      }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
    } else {
      List({ space: 8 }) {
        ForEach(this.repos, (repo: GitHubRepo) => {
          ListItem() {
            Column({ space: 6 }) {
              Row() {
                Text(repo.fullName).fontSize(15).fontWeight(FontWeight.Bold).layoutWeight(1)
                  .maxLines(1).textOverflow({ overflow: TextOverflow.Ellipsis })
                Text(`★ ${repo.stargazersCount}`).fontSize(12).fontColor('#F5A623')
              }
              .width('100%')
              if (repo.description.length > 0) {
                Text(repo.description).fontSize(12).fontColor('#666666').maxLines(2)
                  .textOverflow({ overflow: TextOverflow.Ellipsis }).width('100%')
              }
              Row() {
                Text(repo.language).fontSize(11).fontColor('#3D7FFF')
                Blank()
                Text('打开 ↗').fontSize(12).fontColor('#3D7FFF')
              }
              .width('100%')
            }
            .width('100%')
            .padding(14)
            .borderRadius(14)
            .backgroundColor('#59FFFFFF')
            .backdropBlur(20)
            .border({ width: 0.5, color: '#99FFFFFF' })
            .onClick(() => {
              router.openUrl({ url: repo.htmlUrl });
            })
            .alignItems(HorizontalAlign.Start)
          }
        }, (repo: GitHubRepo) => repo.fullName)
      }
      .width('100%')
      .layoutWeight(1)
      .padding({ left: 14, right: 14, top: 6 })
    }
  }
}
