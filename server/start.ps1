# 乐云音乐 CloudMusic - API 服务启动脚本（Windows PowerShell）
# 依赖：Node.js >= 16
$ErrorActionPreference = "Stop"

$Port = if ($env:PORT) { $env:PORT } else { "3000" }
$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ApiDir = Join-Path $Dir "NeteaseCloudMusicApi"

if (-not (Test-Path (Join-Path $ApiDir ".git"))) {
  Write-Host "[1/3] 克隆 NeteaseCloudMusicApi ..."
  git clone --depth 1 https://github.com/Binaryify/NeteaseCloudMusicApi.git $ApiDir
} else {
  Write-Host "[1/3] 检测到已存在仓库，拉取最新代码 ..."
  Push-Location $ApiDir
  git pull --ff-only
  Pop-Location
}

Write-Host "[2/3] 安装依赖 ..."
Push-Location $ApiDir
if (-not (Test-Path "node_modules")) {
  npm install --registry=https://registry.npmmirror.com
  if ($LASTEXITCODE -ne 0) { npm install }
}
Pop-Location

Write-Host "[3/3] 启动服务 (http://0.0.0.0:$Port) ..."
$env:PORT = $Port
Push-Location $ApiDir
node app.js
Pop-Location
