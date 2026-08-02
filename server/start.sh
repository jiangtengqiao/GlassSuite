#!/usr/bin/env bash
# 乐云音乐 CloudMusic - API 服务启动脚本（Linux/macOS/Git Bash）
# 依赖：Node.js >= 16
set -e

PORT="${PORT:-3000}"
DIR="$(cd "$(dirname "$0")" && pwd)"
API_DIR="$DIR/NeteaseCloudMusicApi"

if [ ! -d "$API_DIR/.git" ]; then
  echo "[1/3] 克隆 NeteaseCloudMusicApi ..."
  git clone --depth 1 https://github.com/Binaryify/NeteaseCloudMusicApi.git "$API_DIR"
else
  echo "[1/3] 检测到已存在仓库，拉取最新代码 ..."
  git -C "$API_DIR" pull --ff-only || echo "（拉取失败，使用本地版本继续）"
fi

echo "[2/3] 安装依赖 ..."
cd "$API_DIR"
if [ ! -d node_modules ]; then
  npm install --registry=https://registry.npmmirror.com || npm install
fi

echo "[3/3] 启动服务 (http://0.0.0.0:$PORT) ..."
PORT="$PORT" node app.js
