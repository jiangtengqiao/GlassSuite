# ============================================================
#  GlassSuite Windows 端一键构建脚本
#  用法（任选其一）：
#   1. 双击 build.bat（推荐，自动调用本脚本）
#   2. 在 PowerShell 中执行：  .\build.ps1
#      （若提示禁止运行脚本，先执行：  Set-ExecutionPolicy -Scope Process Bypass）
#  产物输出到 windows\publish\GlassSuite-win-x64\ 并打包 zip
# ============================================================
param(
    [switch]$Standalone,   # 加 -Standalone 构建自包含版（免装 .NET 运行时，体积约 150MB）
    [switch]$SingleFile    # 加 -SingleFile 构建单文件 exe（约 130MB）
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot                        # 脚本所在目录 = windows 目录
$Project = Join-Path $Root "src\GlassSuite\GlassSuite.csproj"
$OutDir = Join-Path $Root "publish\GlassSuite-win-x64"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  GlassSuite Windows 构建" -ForegroundColor Cyan
Write-Host "  项目: $Project" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# 1. 检查 dotnet
$dotnet = Get-Command dotnet -ErrorAction SilentlyContinue
if (-not $dotnet) {
    Write-Host "[错误] 未找到 dotnet 命令。请先安装 .NET SDK 10：" -ForegroundColor Red
    Write-Host "       https://dotnet.microsoft.com/download/dotnet/10.0" -ForegroundColor Yellow
    Write-Host "       安装完成后重新打开终端再运行本脚本。" -ForegroundColor Yellow
    exit 1
}
Write-Host "[1/4] dotnet 版本: $(dotnet --version)" -ForegroundColor Green

# 2. 清理旧产物
if (Test-Path $OutDir) { Remove-Item -Recurse -Force $OutDir }
Write-Host "[2/4] 已清理旧产物目录" -ForegroundColor Green

# 3. 发布构建
Write-Host "[3/4] 开始构建（首次会还原 NuGet 包，需联网，约 1-3 分钟）..." -ForegroundColor Yellow
if ($Standalone) {
    dotnet publish $Project -c Release -r win-x64 --self-contained true -o $OutDir
}
elseif ($SingleFile) {
    dotnet publish $Project -c Release -r win-x64 --self-contained true `
        -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o $OutDir
}
else {
    dotnet publish $Project -c Release -r win-x64 --self-contained false -o $OutDir
}
if ($LASTEXITCODE -ne 0) {
    Write-Host "[错误] 构建失败，请检查上方错误信息（常见：网络不通导致 NuGet 还原失败）。" -ForegroundColor Red
    exit 1
}
Write-Host "[3/4] 构建成功" -ForegroundColor Green

# 4. 裁剪冗余的 x86 版 libvlc + 打包 zip
$LibX86 = Join-Path $OutDir "libvlc\win-x86"
if (Test-Path $LibX86) { Remove-Item -Recurse -Force $LibX86 }
$Zip = Join-Path $Root "publish\GlassSuite-win-x64.zip"
if (Test-Path $Zip) { Remove-Item -Force $Zip }
Compress-Archive -Path $OutDir -DestinationPath $Zip -Force
$size = [math]::Round((Get-Item $Zip).Length / 1MB, 1)

Write-Host "[4/4] 打包完成" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  可执行文件:  $OutDir\GlassSuite.exe" -ForegroundColor Green
Write-Host "  分发压缩包:  $Zip  ($size MB)" -ForegroundColor Green
Write-Host "  使用方法:    解压 zip → 双击 GlassSuite.exe" -ForegroundColor Green
Write-Host "  (框架依赖版需先装 .NET 10 Desktop Runtime，见教程 1.2 节)" -ForegroundColor Yellow
Write-Host "==============================================" -ForegroundColor Cyan
