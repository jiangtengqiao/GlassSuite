# 璃光 GlassSuite — 多功能应用套件

![平台](https://img.shields.io/badge/平台-Android%20%7C%20Windows-blue)
![版本](https://img.shields.io/badge/版本-v1.0.0-red)
![许可](https://img.shields.io/badge/许可-MIT-green)
[![Release](https://img.shields.io/badge/下载-Releases-blueviolet)](https://github.com/jiangtengqiao/GlassSuite/releases)

> 开箱即用：**Android APK、Windows 安装包** 均发布在 [Releases](https://github.com/jiangtengqiao/GlassSuite/releases)，打 tag 即自动构建发布。

一个功能真实一致、可真实运行的多功能应用套件（音乐只是模块之一），覆盖 **Android / Windows** 双端。音乐数据经自托管的网易云音乐接口服务（NeteaseCloudMusicApi）实时拉取，支持真实在线播放、实时歌词、扫码/验证码登录、歌单、搜索、收藏点赞、锁屏媒体控制、主题定制等完整音乐功能。

> ⚠️ **合规提示**：本项目仅供学习交流使用。音乐内容版权归网易云音乐及相应权利人所有；对外发布前请替换 `docs/legal/` 中的占位符并取得必要授权。

## 📦 产物下载（Releases）

| 平台 | 产物 | 下载 |
|---|---|---|
| Android | `GlassSuite-v1.1.0.apk` | [GitHub Releases](https://github.com/jiangtengqiao/GlassSuite/releases) |
| Windows | `GlassSuite-win-x64.zip` | [GitHub Releases](https://github.com/jiangtengqiao/GlassSuite/releases) |

> 打 tag（`git tag vX.Y.Z && git push origin vX.Y.Z`）即自动构建发布新产物。
> 网络受限时可使用镜像加速：`https://ghproxy.net/https://github.com/jiangtengqiao/GlassSuite/releases`

## 功能总览

| 功能 | 说明 |
|---|---|
| 登录 | 扫码登录（二维码轮询）、手机验证码登录、登录态持久化、退出登录 |
| 发现 | 每日推荐、推荐歌单、新歌速递、排行榜、歌单广场（分类筛选） |
| 搜索 | 热搜词、输入建议、单曲/歌单/歌手/专辑/MV 多类型搜索 |
| 播放 | 真实在线播放、音质档位调节（标准/较高/极高/无损，VIP 曲目自动降档）、队列、循环/随机、进度拖动、后台播放 |
| 歌词 | 实时滚动高亮、原词/翻译/罗马音切换、DIY 歌词导入编辑、时间偏移微调、字号调节、点击歌词跳转 |
| 详情 | 歌曲详情、歌手页（热歌+专辑）、专辑页（全曲播放）、MV 播放 |
| 歌单 | 歌单详情+完整曲目、收藏/取消收藏、我的歌单、我喜欢（点赞列表） |
| 用户 | 资料展示、统计信息、退出登录 |
| 设置 | API 地址、主题色（6 种色板）、深色模式、歌词默认参数、DIY 歌词管理、关于与法律（9 份文档全文） |
| 系统集成 | Android：锁屏媒体通知+状态栏常驻；Windows：系统媒体信息（SMTC）尽力集成 |

## 技术栈

| 端 | 技术 | 播放引擎 |
|---|---|---|
| Android | Kotlin + Jetpack Compose (Material3) | Media3 ExoPlayer |
| Windows | C# / .NET 8 WPF | LibVLCSharp + VideoLAN LibVLC |
| API 服务 | Node.js（NeteaseCloudMusicApi，自托管） | — |

## 快速开始

### 从 Releases 下载
在 [Releases](https://github.com/jiangtengqiao/GlassSuite/releases) 页获取各平台产物：
- `*.apk` → Android 直接安装；
- `CloudMusic-win-x64.zip` → 解压运行（需 .NET 10 Desktop Runtime）；

### 开箱即用（推荐）

安装后**默认「自动直连网易云」**：客户端内置网易云官方接口加密协议（weapi/eapi，与 YesPlayMusic 同方案），
无需部署任何服务器，扫码/验证码登录、歌单、歌词、播放、MV 全部可用。设置页可随时切换：

- **自动直连网易云（推荐）**：内置加密直连 `music.163.com`，零部署
- **自托管服务器**：`cd server && docker compose up -d`（或 `bash start.sh`），
  在「设置 → 服务器地址」配置（模拟器 `http://10.0.2.2:3000` / 真机局域网 IP / Windows `localhost:3000`）

### 系统能力

- **错误上报体系**：全局崩溃捕获 → 本地滚动日志（含设备/系统/版本信息）→ 自动上传（Beta 服务器 / 自托管服务器双通道）→ 失败保留重试；设置页可查看/手动上传/清空
- **版本检测迭代**：启动即检 + 30 分钟轮询，失败自动退避（30m→1h→2h→4h→8h）；按发布通道过滤推送
- **严格分层 Beta**：申请评分决定层级——L1 Beta 尝鲜 / L2 Alpha 内测 / L3 开发者核心；层级决定可见更新通道（stable/beta/alpha/dev），尝鲜码由 `server/beta-server` 签发验证

构建与运行详见 [docs/03-构建指南.md](docs/03-构建指南.md)。

## 目录结构

```
├── docs/                  # 架构、部署、构建文档 + 法务文档（legal/）
├── server/                # API 服务（docker-compose / 启动脚本）
├── android/               # Android 端（Gradle + Compose）
└── windows/               # Windows 端（WPF .NET 8）
```

## 法律与合规

- [用户协议](docs/legal/01-用户协议.md)
- [隐私政策](docs/legal/02-隐私政策.md)
- [儿童个人信息保护规则](docs/legal/03-儿童个人信息保护规则.md)
- [版权声明与侵权投诉](docs/legal/04-版权声明与侵权投诉.md)
- [免责声明](docs/legal/05-免责声明.md)
- [第三方SDK与权限清单](docs/legal/06-第三方SDK与权限清单.md)
- [账号注销指引](docs/legal/07-账号注销指引.md)
- [投诉与举报](docs/legal/08-投诉与举报.md)
- [用户行为规范](docs/legal/09-用户行为规范.md)

以上文档已同步内置到三端应用「设置 → 关于与法律」页面。
