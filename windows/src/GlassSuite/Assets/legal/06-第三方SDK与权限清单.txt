# 乐云音乐第三方SDK与权限清单

**版本：V1.0**
**生效日期：20XX年XX月XX日**
**更新日期：20XX年XX月XX日**

为向您提供本软件的各项功能，我们可能接入第三方 SDK 及开源组件，并在各端申请相应权限。以下清单向您说明相关情况。

## 一、Android 端

### 1. 权限

| 权限 | 用途 | 是否可关闭 |
|---|---|---|
| INTERNET（网络） | 联网获取音乐数据、播放音乐 | 关闭后无法使用 |
| POST_NOTIFICATIONS（通知） | 展示播放状态通知（状态栏/锁屏媒体信息） | 可关闭，关闭后播放通知不再展示 |
| FOREGROUND_SERVICE / FOREGROUND_SERVICE_MEDIA_PLAYBACK | 后台与锁屏持续播放音乐 | 系统限制 |
| WAKE_LOCK（唤醒锁） | 锁屏播放时保持播放器运行 | 系统限制 |

### 2. 第三方组件

| 组件 | 用途 | 处理的数据 | 许可证 |
|---|---|---|---|
| AndroidX Media3 (ExoPlayer) | 音乐播放、锁屏媒体通知 | 播放地址、音频数据 | Apache-2.0 |
| Retrofit / OkHttp / Gson | 网络请求与数据解析 | 请求参数、Cookie | Apache-2.0 |
| Coil | 封面图片加载 | 图片 URL | Apache-2.0 |
| Jetpack DataStore | 本地设置存储 | 应用设置、登录凭证（本地） | Apache-2.0 |
| Jetpack Compose | 界面构建 | 无 | Apache-2.0 |

## 二、HarmonyOS 端

### 1. 权限

| 权限 | 用途 |
|---|---|
| ohos.permission.INTERNET | 联网获取音乐数据、播放音乐 |
| ohos.permission.KEEP_BACKGROUND_RUNNING | 后台持续播放音乐 |
| ohos.permission.GET_NETWORK_INFO | 检测网络状态 |

### 2. 系统能力（Kit）

| Kit | 用途 |
|---|---|
| NetworkKit（@kit.NetworkKit） | HTTP 网络请求 |
| MediaKit（@kit.MediaKit） | 音频播放（AVPlayer） |
| AVSessionKit（@kit.AVSessionKit） | 系统媒体中心/锁屏媒体信息与控制 |
| ArkData（@kit.ArkData） | 本地偏好设置存储 |

以上均为 HarmonyOS 系统内置能力，不涉及第三方 SDK。

## 三、Windows 端

| 组件 | 用途 | 许可证 |
|---|---|---|
| .NET 8 Runtime | 应用运行环境 | MIT |
| LibVLCSharp / VideoLAN LibVLC | 音乐播放（解码 mp3/flac 等） | LGPL-2.1（动态链接，遵循其许可证义务） |
| Windows.Media (SMTC) | 系统媒体信息集成 | 系统能力 |
| CommunityToolkit.Mvvm | MVVM 架构支持 | MIT |

## 四、服务端

| 组件 | 用途 | 许可证 |
|---|---|---|
| NeteaseCloudMusicApi（开源社区项目） | 网易云音乐接口封装 | MIT |
| Node.js 及其 npm 依赖 | 服务运行环境 | 各自许可证 |

## 五、说明

5.1 **我们不与任何第三方广告 SDK、统计分析 SDK 共享您的个人信息。** 本软件不包含广告推送、行为追踪类 SDK。

5.2 除实现播放鉴权等基本功能所必需的最小信息外，我们不向第三方接口提供您的个人信息；您的登录凭证仅保存在您的设备本地，用于向第三方平台接口发起鉴权请求。

5.3 上述组件清单将随版本更新而更新，最新清单以本软件「设置 → 关于与法律 → 第三方SDK与权限清单」展示内容为准。

（全文完）
