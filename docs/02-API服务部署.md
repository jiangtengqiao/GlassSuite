# API 服务部署指南

音乐数据来自网易云音乐平台的开放接口，本项目使用开源社区项目 [NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi)（MIT 协议）自托管服务。

## 方式一：Docker（推荐）

```bash
cd server
docker compose up -d
# 服务监听 http://0.0.0.0:3000
```

验证：

```bash
curl "http://localhost:3000/personalized?limit=3"
# 返回 JSON，code 为 200 即正常
```

## 方式二：源码运行（需要 Node.js ≥ 16）

```bash
cd server
bash start.sh        # Windows 使用 .\start.ps1
```

脚本内容：克隆 NeteaseCloudMusicApi → 安装依赖 → 启动（端口 3000）。

## 局域网访问（真机调试）

1. 查看电脑局域网 IP：
   - Windows：`ipconfig`，找 `IPv4 地址`（如 `192.168.1.100`）
   - macOS/Linux：`ifconfig` 或 `ip addr`
2. 真机与电脑连同一 Wi-Fi，在应用「设置 → 服务器地址」填写 `http://<IP>:3000`。
3. 若无法访问，检查电脑防火墙是否放行 3000 端口：
   - Windows：`控制面板 → Windows Defender 防火墙 → 高级设置 → 入站规则` 放行 TCP 3000
   - 或临时：`netsh advfirewall firewall add rule name="cloudmusic-api" dir=in action=allow protocol=TCP localport=3000`

## 常见问题

| 问题 | 处理 |
|---|---|
| 登录报错 / 验证码发送失败 | 网易接口风控：需真实注册手机号；可改用扫码登录 |
| 部分歌曲无音源 | VIP/版权受限，应用内已自动降档，请登录后重试 |
| 接口 404 / 接口变动 | 升级 NeteaseCloudMusicApi 到最新版（`git pull` + 重启） |
| 端口被占用 | 修改 `docker-compose.yml` / 启动脚本中的端口，并在应用设置中同步修改 |
| 请求很慢 | 接口服务部署在国内服务器或本机效果最佳 |

## 安全说明

- 服务仅建议在可信网络（本机/内网）运行，勿直接暴露公网。
- 接口服务会请求网易云音乐真实数据，请遵守其服务条款，仅用于个人学习研究。
