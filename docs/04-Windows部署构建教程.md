# GlassSuite Windows 端部署与构建教程（保姆级·逐步操作版）

> 适用：Windows 10 / 11（64 位）｜ 版本：v1.1.0+
> 本教程每一步都写明：**在哪里操作 → 用什么工具 → 输入什么 → 应当看到什么**。
> 看不懂专业名词也没关系，照着做即可。

---

## 目录

- [第一部分：直接使用（部署给任何一台电脑）](#第一部分直接使用部署给任何一台电脑)
  - [第 0 步：准备工作](#第-0-步准备工作)
  - [第 1 步：下载安装包](#第-1-步下载安装包)
  - [第 2 步：解压](#第-2-步解压)
  - [第 3 步：安装 .NET 运行环境（仅首次）](#第-3-步安装-net-运行环境仅首次)
  - [第 4 步：启动软件](#第-4-步启动软件)
  - [第 5 步：验证安装成功](#第-5-步验证安装成功)
- [第二部分：从源码自行构建 EXE](#第二部分从源码自行构建-exe)
  - [第 0 步：准备构建环境](#第-0-步准备构建环境)
  - [第 1 步：打开命令行终端](#第-1-步打开命令行终端)
  - [第 2 步：获取源码](#第-2-步获取源码)
  - [第 3 步：标准构建（推荐，体积最小）](#第-3-步标准构建推荐体积最小)
  - [第 4 步：免安装运行时的自包含构建](#第-4-步免安装运行时的自包含构建)
  - [第 5 步：单文件 EXE 构建](#第-5-步单文件-exe-构建)
  - [第 6 步：一键脚本构建（懒人模式）](#第-6-步一键脚本构建懒人模式)
  - [第 7 步：发布到 GitHub Releases](#第-7-步发布到-github-releases)
- [第三部分：构建产物说明](#第三部分构建产物说明)
- [第四部分：常见问题（按步骤排查）](#第四部分常见问题按步骤排查)

---

# 第一部分：直接使用（部署给任何一台电脑）

> 目标：把软件装到一台 Windows 电脑上正常使用。全程**不需要安装任何编程工具**。

## 第 0 步：准备工作

| 事项 | 说明 |
|---|---|
| 电脑系统 | Windows 10（64 位）或 Windows 11，任意品牌均可 |
| 硬盘空间 | 解压后约 300MB，请留出 1GB 以上 |
| 网络 | 能访问互联网（下载安装包 + 首次联网获取音乐数据） |
| 账号权限 | 普通用户即可；安装 .NET 时需要管理员权限（会弹窗询问，点「是」） |

## 第 1 步：下载安装包

**在哪里**：打开浏览器（Edge / Chrome 都行）。

**方法 A（推荐，来自 GitHub Release）：**
1. 浏览器地址栏输入：`https://github.com/jiangtengqiao/GlassSuite/releases` 后回车
2. 在页面中找到最新版本（v1.1.0），找到文件名：`GlassSuite-win-x64-v1.1.0.zip`
3. 点击该文件名，浏览器开始下载（下载完成后在浏览器右上角或「下载」文件夹可见）

**方法 B（国内网络访问 GitHub 慢时，用加速地址）：**
1. 浏览器打开：`https://ghproxy.net/https://github.com/jiangtengqiao/GlassSuite/releases/download/v1.1.0/GlassSuite-win-x64-v1.1.0.zip`
2. 页面出现下载即成功

**方法 C（本机已构建好，直接复制）：**
- 文件位置：`E:\Kun\default_workspace\windows\publish\GlassSuite-win-x64.zip`
- 用 U 盘/网盘/微信文件传输助手复制到目标电脑即可

> ✅ 看到什么：浏览器出现下载进度条，下载完成后得到一个 `GlassSuite-win-x64-v1.1.0.zip` 文件（约 62MB）。

## 第 2 步：解压

**在哪里**：Windows 资源管理器（就是桌面上的「此电脑」/「文件资源管理器」）。

1. 打开「文件资源管理器」（快捷键：`Win + E`）
2. 进入刚才下载文件所在的位置（一般是 `C:\Users\你的用户名\Downloads`，即「下载」文件夹）
3. **右键点击** `GlassSuite-win-x64-v1.1.0.zip`
4. 在弹出菜单中点击 **「全部解压缩」**（Windows 11）或 **「解压到当前文件夹」**（Windows 10）
5. 在弹出的窗口中点击 **「提取」** / **「解压」** 按钮，等待几秒
6. 解压后，同一文件夹下会出现一个名为 `GlassSuite-win-x64-v1.1.0` 的**文件夹**

> ✅ 看到什么：多了一个文件夹 `GlassSuite-win-x64-v1.1.0`，双击进入可以看到 `GlassSuite.exe` 等文件。
> ⚠️ 注意：**不要**直接在压缩包里双击 exe，必须先解压。

## 第 3 步：安装 .NET 运行环境（仅首次）

**为什么**：这个软件基于 .NET 10 开发，第一次使用需要安装运行环境；只需装一次，以后不用再装。

**在哪里**：浏览器。

1. 浏览器打开：`https://dotnet.microsoft.com/download/dotnet/10.0`
2. 页面往下滚动，找到 **`.NET Desktop Runtime 10.0.x`**（注意一定是 **Desktop Runtime**，不是 SDK，不是 ASP.NET）
3. 点击该行右侧的 **「Download x64」** 蓝色按钮
4. 下载完成后，双击下载的 `dotnet-runtime-10.0.x-win-x64.exe` 文件
5. 安装向导出现后，一路点击 **「下一步」→「安装」**（如弹出「用户账户控制」窗口，点 **「是」**）
6. 等待进度条走完，点击 **「关闭」**

> ✅ 看到什么：安装完成后桌面上不会有图标，属正常。可跳过后面的「第 5 步验证」中的验证命令确认。

**如何确认已安装成功**：按 `Win + R` → 输入 `cmd` 回车 → 在黑窗口中输入以下命令后回车：
```
dotnet --list-runtimes
```
看到列表中有 `Microsoft.WindowsDesktop.App 10.0.x` 即成功，然后输入 `exit` 回车关闭窗口。

## 第 4 步：启动软件

**在哪里**：Windows 资源管理器。

1. 进入解压出的文件夹 `GlassSuite-win-x64-v1.1.0`
2. 双击文件 **`GlassSuite.exe`**（图标是一个彩色圆形玻璃样式；如果看不到后缀名，认准名字是 GlassSuite 且类型为「应用程序」）
3. 首次启动如果弹出 Windows 安全提示（SmartScreen），点击 **「更多信息」→「仍要运行」**（因为是自编译软件，未签名属正常）

> ✅ 看到什么：软件窗口打开，显示「璃光 GlassSuite」主界面（发现 / 搜索 / 我的 三个底部标签）。

## 第 5 步：验证安装成功

| 检查项 | 操作 | 正常结果 |
|---|---|---|
| 软件能打开 | 双击 exe | 主界面出现 |
| 音乐能加载 | 主界面「发现」页 | 几秒后出现推荐歌单/排行榜（默认自动直连网易云） |
| 能搜索 | 点「搜索」标签，输入任意歌名回车 | 出现搜索结果 |
| 设置可用 | 主界面右上角「设置」 | 设置页可切换主题色、歌词模式等 |

如果「发现」页一直空白，见 [第四部分 问题 4](#问题-4音乐列表空白)。

---

# 第二部分：从源码自行构建 EXE

> 目标：在自己的电脑上把源码编译成可执行的 EXE 安装包。适合开发者或想自定义功能的人。

## 第 0 步：准备构建环境

### 0.1 安装 .NET SDK（必须）

**在哪里**：浏览器。

1. 打开：`https://dotnet.microsoft.com/download/dotnet/10.0`
2. 找到 **`.NET SDK 10.0.x`**（注意是 **SDK**，不是 Runtime），点击 **「Download x64」**
3. 双击下载的 `dotnet-sdk-10.0.x-win-x64.exe`，一路「下一步」→「安装」
4. 安装完成后**关闭所有已打开的黑窗口/终端**（让环境变量生效）

**验证**：
- 按 `Win + R` → 输入 `cmd` 回车 → 输入：
```
dotnet --version
```
- ✅ 正常显示类似 `10.0.x` 的数字即成功。

### 0.2 安装 Git（获取源码用，可选）

**在哪里**：浏览器。

1. 打开：`https://git-scm.com/download/win`
2. 点击 **64-bit Git for Windows Setup** 下载
3. 双击安装，全程默认「Next → Install → Finish」即可

**验证**：新开黑窗口输入 `git --version`，显示 `git version 2.x.x` 即成功。

## 第 1 步：打开命令行终端

任选一种方式（推荐第 1 种）：

**方式 A：PowerShell（推荐）**
1. 按 `Win` 键（键盘左下角田字格图标）
2. 直接输入 `powershell`
3. 在搜索结果中点击 **「Windows PowerShell」**（或「终端」）
4. 出现一个蓝底（或黑底）窗口，光标前有 `PS C:\Users\你的用户名>` 字样，说明已就绪

**方式 B：命令提示符 CMD**
1. 按 `Win + R`
2. 输入 `cmd`，回车

**方式 C：在文件夹内打开终端（最方便，自动定位到当前目录）**
1. 打开资源管理器，进入你存放代码的文件夹（比如 `D:\`）
2. 在窗口顶部的**地址栏**里直接输入 `powershell` 并回车
3. 打开的终端会自动定位到这个文件夹

## 第 2 步：获取源码

在刚打开的终端（PowerShell）中，**逐条**输入以下命令，每输入一条按一次回车：

```powershell
# 1. 进入 D 盘（或你想放代码的盘）
cd D:\

# 2. 把源码下载到本地（会创建一个 GlassSuite 文件夹）
git clone https://github.com/jiangtengqiao/GlassSuite.git

# 3. 进入源码目录（之后的命令都在这执行）
cd GlassSuite

# 4. 确认当前在源码目录（应显示 ...\GlassSuite）
pwd
```

> ✅ 看到什么：`git clone` 时进度条滚动，结束后 `D:\GlassSuite` 文件夹出现。
> ⚠️ 如果国内网络 clone 慢/失败，可换镜像：`git clone https://gitee.com/mirrors/GlassSuite.git`（或稍后重试）。

## 第 3 步：标准构建（推荐，体积最小）

在终端中（当前位于 `D:\GlassSuite`），依次输入：

```powershell
# 1. 进入 windows 工程目录
cd windows

# 2. 执行发布构建（关键命令，耗时 1~3 分钟）
dotnet publish src/GlassSuite/GlassSuite.csproj -c Release -r win-x64 --self-contained false -o publish/GlassSuite-win-x64
```

**参数逐项解释**（不用记，了解即可）：

| 参数 | 含义 |
|---|---|
| `-c Release` | 发布版（优化后），不是调试版 |
| `-r win-x64` | 目标平台：64 位 Windows |
| `--self-contained false` | 不打包 .NET 运行时（体积小，但目标电脑需装运行时；见第 4 步的自包含模式） |
| `-o publish/GlassSuite-win-x64` | 输出到 `windows\publish\GlassSuite-win-x64` 文件夹 |

> ✅ 看到什么：输出 `已成功生成` / `Build succeeded`，最后出现 `GlassSuite -> D:\GlassSuite\windows\publish\GlassSuite-win-x64\GlassSuite.dll`。

**打包成 zip（分发用）**，继续输入：

```powershell
# 3. 删除用不到的 x86 版播放库（省空间）
Remove-Item -Recurse -Force publish\GlassSuite-win-x64\libvlc\win-x86

# 4. 压缩成 zip 安装包（生成 publish\GlassSuite-win-x64.zip）
Compress-Archive -Path publish\GlassSuite-win-x64 -DestinationPath publish\GlassSuite-win-x64.zip -Force
```

> ✅ 看到什么：`publish` 文件夹中出现 `GlassSuite-win-x64.zip`（约 62MB）。
> 此时即可按【第一部分】把该 zip 拷给别人使用。

## 第 4 步：免安装运行时的自包含构建

> 适用：目标电脑不方便安装 .NET 运行环境。代价是包更大（约 150MB）。

继续在终端（`D:\GlassSuite\windows`）输入：

```powershell
dotnet publish src/GlassSuite/GlassSuite.csproj -c Release -r win-x64 --self-contained true -o publish/GlassSuite-win-x64-standalone
```

> ✅ 看到什么：`publish\GlassSuite-win-x64-standalone\GlassSuite.exe` 生成，该文件夹**复制到任何 x64 的 Win10/11 电脑上双击即运行**，无需安装任何东西。

## 第 5 步：单文件 EXE 构建

> 适用：想要「一个 exe 文件」分发。打包时把运行时和库都塞进单个 exe（约 130MB），首次运行会自解压稍慢。

```powershell
dotnet publish src/GlassSuite/GlassSuite.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o publish/GlassSuite-single
```

> ✅ 看到什么：`publish\GlassSuite-single\GlassSuite.exe` 单个文件。

## 第 6 步：一键脚本构建（懒人模式）

仓库已内置一键脚本，**不需要手敲命令**：

**方法 A（双击 bat）：**
1. 打开资源管理器，进入 `D:\GlassSuite\windows`
2. 双击 **`build.bat`**
3. 脚本自动执行：发布 → 裁剪 → 打包 zip，窗口显示进度
4. 完成后按任意键关闭，产物在 `D:\GlassSuite\windows\publish\GlassSuite-win-x64.zip`

**方法 B（PowerShell 脚本，可指定模式）：**
在终端执行：
```powershell
cd D:\GlassSuite\windows
.\build.ps1                # 标准构建（框架依赖 + zip）
.\build.ps1 -SelfContained # 自包含构建
.\build.ps1 -SingleFile    # 单文件 exe
```

## 第 7 步：发布到 GitHub Releases

> 把构建产物发布到 GitHub，别人就能直接下载。需要 GitHub 账号（已配置好 `git` 的登录凭据）。

在终端（`D:\GlassSuite`）执行：

```powershell
# 1. 提交所有改动
git add -A
git commit -m "发布 v1.1.0"

# 2. 打版本标签
git tag v1.1.0

# 3. 推送代码 + 标签（触发 GitHub 自动构建发布）
git push origin main --tags
```

等待 10~15 分钟，打开 `https://github.com/jiangtengqiao/GlassSuite/releases`，即可看到自动生成的：
- `GlassSuite-v1.1.0.apk`（Android 包，由 GitHub 服务器自动构建，不需要你本地装 Android 环境）
- `GlassSuite-win-x64-v1.1.0.zip`（Windows 包）

> 如果只想用 GitHub 自动构建、不想本地构建，**跳过第 3~6 步，直接执行第 7 步即可**。

---

# 第三部分：构建产物说明

```
GlassSuite-win-x64/
├── GlassSuite.exe            ← 主程序（双击运行）
├── GlassSuite.dll            程序集（主逻辑）
├── GlassSuite.pdb             调试符号（可删除）
├── libvlc/                    音视频解码库（VLC）
│   ├── win-x64/               64 位解码库（运行必需）
│   └── win-x86/               32 位解码库（可删除，省约 10MB）
├── Assets/
│   └── legal/                 内置 9 份法律文档（应用内「设置→关于与法律」展示）
└── *.dll / *.json             其余 .NET 组件（框架依赖模式需要系统已装 .NET 10 Desktop Runtime）
```

**运行时产生的数据位置**（用户电脑上，与程序目录无关）：

| 数据 | 路径 | 说明 |
|---|---|---|
| 用户设置 | `%AppData%\GlassSuite\settings.json` | 登录态、API 地址、主题色、歌词设置 |
| 错误日志 | `%AppData%\GlassSuite\errors\` | 崩溃/异常自动采集，可上传至 Beta 服务器 |
| 音乐接口服务器（可选） | 源码 `server\` 目录 | `docker compose up -d` 或 `bash start.sh`，端口 3000 |
| 尝鲜系统服务器（可选） | 源码 `server\beta-server\` | `npm install && node server.js`，端口 3100 |

---

# 第四部分：常见问题（按步骤排查）

### 问题 1：双击 exe 没反应 / 报「找不到 .NET Runtime」
- **原因**：没装 .NET 10 Desktop Runtime（或装了 SDK 没装 Runtime）
- **解决**：回到【第一部分 第 3 步】安装 `.NET Desktop Runtime 10.0.x (x64)`；或改用自包含构建（第二部分第 4 步）

### 问题 2：SmartScreen 提示「已保护你的电脑」
- **原因**：软件未做数字签名（个人项目正常现象）
- **解决**：点「更多信息」→「仍要运行」；确认是自 GitHub Release 下载的即可放心

### 问题 3：解压时提示「找不到压缩文件」/ 文件损坏
- **解决**：重新下载一次；确认下载完成后再解压（文件约 62MB，若只有几 KB 说明下载没完成）

### 问题 4：音乐列表空白
- **解决步骤**（按顺序试）：
  1. 设置 →「连接方式」确认是「自动直连网易云（推荐）」
  2. 关闭代理软件/加速器后重试（代理会干扰）
  3. 若仍空白，切换到「自托管服务器」：在源码 `server\` 目录执行 `docker compose up -d` 或 `bash start.sh`，设置中填 `http://localhost:3000`，点「保存并应用」
  4. 换网络（手机热点）试一次，排除运营商屏蔽

### 问题 5：扫码登录二维码不显示 / 验证码收不到
- **原因**：当前网络到 `music.163.com` 被限制，或该出口 IP 被网易云风控
- **解决**：换网络（如手机热点）重试；仍失败则使用自托管服务器模式

### 问题 6：播放到一半失败提示「降档」
- **原因**：该曲目为 VIP 受限，或音频 CDN 被网络限速
- **解决**：应用会自动降档重试（标准→较高→极高→无损逐级降）；可手动在播放页切音质；VIP 歌曲需登录对应会员账号

### 问题 7：`dotnet` 命令提示「不是内部或外部命令」
- **原因**：.NET SDK 没装，或装完后没重开终端
- **解决**：重装 SDK（第二部分第 0.1 步）；装完**关闭并重新打开**终端再试

### 问题 8：`git clone` 很慢或失败
- **解决**：用镜像：`git clone https://ghproxy.net/https://github.com/jiangtengqiao/GlassSuite.git`；或挂代理后重试

### 问题 9：`Compress-Archive` 报错「文件正在被使用」
- **解决**：先关闭正在运行的 GlassSuite.exe；或删除旧的 `publish\GlassSuite-win-x64.zip` 再执行

### 问题 10：构建报错 `NETSDK1045`（SDK 版本不支持）
- **原因**：.NET SDK 版本低于 10
- **解决**：`dotnet --version` 确认 ≥10.0；升级 SDK 后重开终端重试

---

*仅供学习交流使用；音乐内容版权归网易云音乐及权利人所有。*
