# GlassSuite Windows 端部署与构建教程（完整版）

> 适用版本：v1.1.0+ ｜ 平台：Windows 10/11 x64 ｜ 目标：把应用部署给任意用户，或从源码自行构建 EXE

---

## 目录
1. [零基础部署（直接下载安装包）](#一零基础部署直接下载安装包)
2. [从源码构建 Windows 包](#二从源码构建-windows-包)
3. [构建产物说明](#三构建产物说明)
4. [常见问题排查](#四常见问题排查)

---

## 一、零基础部署（直接下载安装包）

### 1.1 获取安装包

| 来源 | 地址 |
|---|---|
| GitHub Releases（推荐） | `https://github.com/jiangtengqiao/GlassSuite/releases` → 下载 `GlassSuite-win-x64-<版本>.zip` |
| 本地构建产物 | `windows/publish/GlassSuite-win-x64.zip` |

国内网络访问 GitHub 慢时，可用加速前缀：`https://ghproxy.net/https://github.com/jiangtengqiao/GlassSuite/releases/download/v1.1.0/GlassSuite-win-x64-v1.1.0.zip`

### 1.2 安装运行（三步）

1. **解压**：将 zip 解压到任意目录，例如 `D:\GlassSuite\`（路径不要包含中文/空格更稳妥）。
2. **安装 .NET 10 Desktop Runtime**（仅首次需要）：
   - 下载：`https://dotnet.microsoft.com/download/dotnet/10.0` → 选择 **.NET Desktop Runtime 10.x (x64)** → 安装
   - 检查是否已装：开始菜单搜索 "dotnet"，或命令行执行 `dotnet --list-runtimes`（能看到 `Microsoft.WindowsDesktop.App 10.0.x` 即已安装）
3. **双击运行**：进入解压目录 → 双击 `GlassSuite.exe` 启动。

> 若不想装 .NET 运行时，可在构建时使用「自包含」模式（见 2.4），产物体积约 150MB 但免运行时。

### 1.3 首次使用配置（无需配置，开箱即用）

- **音乐**：默认「自动直连网易云」（内置 weapi/eapi 官方接口加密协议），打开即用；扫码/验证码登录、歌单、歌词、播放、MV 全部可用。
- **开发者尝鲜（可选）**：设置 → 开发者尝鲜，填入 Beta 服务器地址（默认 `http://localhost:3100`，需自行部署 `server/beta-server`），申请尝鲜码后按层级接收 Beta/Alpha 推送。
- **错误上报（自动）**：崩溃/异常自动采集并上传至 Beta 服务器 `/api/error`（未配置则本地保留，可在设置页手动上传/查看/清空）。

---

## 二、从源码构建 Windows 包

### 2.1 环境要求

| 工具 | 版本 | 获取方式 |
|---|---|---|
| Windows 10/11 x64 | — | — |
| .NET SDK | 10.0+ | `https://dotnet.microsoft.com/download/dotnet/10.0` |
| Git（可选） | 任意 | `https://git-scm.com/` |

验证环境：
```bat
dotnet --version        :: 应显示 10.0.x
```

### 2.2 获取源码

```bat
git clone https://github.com/jiangtengqiao/GlassSuite.git
cd GlassSuite
```

### 2.3 标准发布构建（框架依赖，体积小，推荐）

```bat
cd windows
dotnet publish src/GlassSuite/GlassSuite.csproj -c Release -r win-x64 --self-contained false -o publish/GlassSuite-win-x64
```

可选裁剪与打包（移除 x86 版 libvlc 冗余，压缩为 zip）：
```bat
rmdir /s /q publish\GlassSuite-win-x64\libvlc\win-x86
powershell -Command "Compress-Archive -Path 'publish\GlassSuite-win-x64' -DestinationPath 'publish\GlassSuite-win-x64.zip' -Force"
```

产物：
- `publish\GlassSuite-win-x64\GlassSuite.exe`（可直接运行）
- `publish\GlassSuite-win-x64.zip`（分发用）

### 2.4 自包含构建（免装 .NET 运行时，体积大）

```bat
dotnet publish src/GlassSuite/GlassSuite.csproj -c Release -r win-x64 --self-contained true -o publish/GlassSuite-win-x64-standalone
```
产物约 150MB，复制到任何 x64 Windows 10/11 可直接运行，无需安装 .NET。

### 2.5 单文件构建（一个 exe 文件分发）

```bat
dotnet publish src/GlassSuite/GlassSuite.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o publish/GlassSuite-single
```
产物为单个 `GlassSuite.exe`（约 130MB），双击即运行。

### 2.6 发布到 GitHub Releases（CI 自动构建）

推送代码并打 tag，GitHub Actions 自动完成构建+发布（无需本地环境）：
```bat
git add -A && git commit -m "v1.1.0"
git tag v1.1.0
git push origin main --tags
```
等待几分钟，`https://github.com/jiangtengqiao/GlassSuite/releases` 自动出现：
- `GlassSuite-<版本>.apk`（Android）
- `GlassSuite-win-x64-<版本>.zip`（Windows）

---

## 三、构建产物说明

```
GlassSuite-win-x64/
├── GlassSuite.exe              ← 主程序（双击运行）
├── GlassSuite.dll              程序集
├── GlassSuite.pdb              调试符号（可删）
├── libvlc/                     VLC 解码库（音频/视频播放，含 win-x64 与 win-x86 两个子目录）
├── Assets/
│   └── legal/                  内置法律文档（用户协议/隐私政策等 9 份，应用内「关于与法律」展示）
└── ...                         其余为 .NET 运行时组件（框架依赖模式需系统已装 .NET 10 Desktop Runtime）
```

**重要文件/数据位置**（用户机器上）：
| 数据 | 路径 |
|---|---|
| 应用设置（含登录态、API 地址、主题、歌词设置） | `%AppData%\GlassSuite\settings.json` |
| 错误上报日志 | `%AppData%\GlassSuite\errors\` |
| 音乐接口（可选自托管） | `server/` 目录（Docker 或 Node 启动，端口 3000） |
| 尝鲜系统（可选） | `server/beta-server/`（Node 启动，端口 3100） |

---

## 四、常见问题排查

| 现象 | 原因与解决 |
|---|---|
| 双击 exe 无反应 / 报缺运行时 | 未装 .NET 10 Desktop Runtime → 按 1.2 安装；或改用 2.4/2.5 自包含构建 |
| 音乐列表空白 | 确认「设置 → 连接方式」为「自动直连网易云」；若网络运营商屏蔽 music.163.com，切换「自托管服务器」并部署 `server/` |
| 扫码登录二维码不显示 | 网络到 `music.163.com` 不通；或本机 IP 被网易云风控（换网络/稍后再试） |
| 播放失败提示降档 | 该曲目 VIP 受限，应用自动降档重试；仍失败说明网络限制音频 CDN |
| 错误日志一直不上传 | 未配置 Beta 服务器/自托管服务器时仅本地保留，可在设置页「错误上报」手动上传或查看 |
| 版本更新不提示 | 检查「设置 → 开发者尝鲜」层级：正式用户仅接收正式版（stable）推送；Beta/Alpha 版本仅对应层级可见 |
| 找不到 exe | 使用 zip 包时请**完整解压**后运行目录内 `GlassSuite.exe`，不要直接在压缩包内双击 |

---

*仅供学习交流使用；音乐内容版权归网易云音乐及权利人所有。*
