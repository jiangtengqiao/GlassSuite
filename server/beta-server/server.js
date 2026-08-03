/**
 * GlassSuite 开发者尝鲜系统（Beta 申请服务）
 *
 * 功能：
 *  - GET  /requirements          申请要求说明（应用内明了展示）
 *  - POST /apply                 提交申请 → 自动评分筛选（实时达标判定）
 *  - GET  /status?email=         查询申请状态
 *  - POST /verify {key}          尝鲜码实时比对
 *  - GET  /latest?key=           按权限返回可推送版本（beta 权限 → beta 渠道；否则仅正式版）
 *
 * 评分规则（自动排列与筛选，满分 90）：
 *  完整性 20 + 邮箱有效性 10 + 用途权重 20 + 理由深度 25 + 关键词 15
 *  得分 >= 60 通过并签发尝鲜码；否则驳回并给出原因。
 *
 * 尝鲜码：22~37 位随机字符（字母数字，去重），永不重复。
 *
 * 启动：node server.js  （默认端口 3100）
 */
const express = require('express');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const PORT = process.env.BETA_PORT || 3100;
const DATA_DIR = path.join(__dirname, 'data');
const DATA_FILE = path.join(DATA_DIR, 'applications.json');

const app = express();
app.use(express.json());

// ---------- 存储 ----------
function load() {
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
  } catch (e) {
    return { applications: [], keys: {} };
  }
}
function save(db) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2));
}

// ---------- 尝鲜码生成：22~37 位、永不重复 ----------
const KEY_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
function genKey(existing) {
  for (let attempt = 0; attempt < 20; attempt++) {
    const len = 22 + crypto.randomInt(16); // 22 ~ 37
    let key = '';
    for (let i = 0; i < len; i++) {
      key += KEY_CHARS[crypto.randomInt(KEY_CHARS.length)];
    }
    if (!existing.has(key)) return key;
  }
  return 'GS-' + crypto.randomBytes(12).toString('hex').toUpperCase();
}

// ---------- 自动评分筛选 ----------
const REQUIREMENTS = {
  title: 'GlassSuite 开发者尝鲜计划',
  intro: '提交申请并通过实时达标评估后，可获得尝鲜码，提前体验 Beta 版本。',
  items: [
    '邮箱：必须为有效邮箱格式，用于接收结果通知',
    '姓名/昵称：必填，1~30 个字符',
    '用途：开发者 / 测试 / 媒体评测 / 普通体验 四选一',
    '申请理由：不少于 15 字，说明你为什么需要尝鲜版',
    '设备信息：必填（如：Pixel 8 / Android 15；Windows 11 x64）',
    '评分 ≥ 60 分即通过；未通过将被驳回并可再次申请',
  ],
  scoreThreshold: 60,
  note: '尝鲜码长度 22~37 位，唯一且不可转让；通过后可体验 Beta 版本更新推送。',
};

const PURPOSE_WEIGHT = { 开发者: 20, 测试: 15, '媒体评测': 10, '普通体验': 5 };
const KEYWORDS = ['开发', '测试', '反馈', '内测', '尝鲜', '适配', '安全', '体验', '汉化', '自动化', '自动化测试'];

function scoreApplication(a) {
  let score = 0;
  const reasons = [];

  // 邮箱有效性
  if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(a.email || '')) {
    score += 10;
  } else {
    reasons.push('邮箱格式无效');
  }
  // 完整性
  const name = (a.name || '').trim();
  const purpose = (a.purpose || '').trim();
  const reason = (a.reason || '').trim();
  const device = (a.device || '').trim();
  if (name.length > 0 && name.length <= 30) { score += 5; } else { reasons.push('姓名/昵称缺失或过长'); }
  if (purpose.length > 0) { score += 5; } else { reasons.push('未选择用途'); }
  if (device.length > 0) { score += 10; } else { reasons.push('未填写设备信息'); }
  // 理由深度
  if (reason.length >= 30) { score += 25; }
  else if (reason.length >= 15) { score += 15; }
  else if (reason.length >= 5) { score += 5; }
  else { reasons.push('申请理由过短（不少于 15 字）'); }
  // 用途权重
  score += PURPOSE_WEIGHT[purpose] || 0;
  // 关键词
  const hit = KEYWORDS.filter((k) => reason.includes(k));
  score += Math.min(15, hit.length * 5);
  return { score: Math.min(90, score), reasons };
}

// ---------- 路由 ----------
app.get('/requirements', (req, res) => {
  res.json({ code: 200, data: REQUIREMENTS });
});

app.post('/apply', (req, res) => {
  const a = {
    email: String(req.body.email || '').trim().toLowerCase(),
    name: String(req.body.name || '').trim(),
    purpose: String(req.body.purpose || '').trim(),
    reason: String(req.body.reason || '').trim(),
    device: String(req.body.device || '').trim(),
    at: new Date().toISOString(),
  };
  const db = load();
  const dup = db.applications.find((x) => x.email === a.email);
  if (dup) {
    return res.json({ code: 200, status: dup.status, score: dup.score, key: dup.key || '', reasons: dup.reasons || [] });
  }
  const { score, reasons } = scoreApplication(a);
  const pass = score >= REQUIREMENTS.scoreThreshold;
  let key = '';
  let tier = 0;
  if (pass) {
    const existing = new Set(Object.values(db.keys));
    key = genKey(existing);
    // 严格分层：分数决定尝鲜层级（tier 1 Beta / 2 Alpha / 3 核心）
    tier = score >= 90 ? 3 : score >= 80 ? 2 : 1;
    db.keys[key] = { email: a.email, issuedAt: new Date().toISOString(), active: true, tier };
  }
  a.status = pass ? 'approved' : 'rejected';
  a.score = score;
  a.key = key;
  a.tier = tier;
  a.reasons = reasons;
  db.applications.push(a);
  save(db);
  res.json({
    code: 200,
    status: a.status,
    score,
    key,
    tier,
    reasons,
    message: pass ? `申请通过！尝鲜层级 L${tier}（${tierName(tier)}），请保存你的尝鲜码（22~37 位）。` : '申请未通过：' + reasons.join('；'),
  });
});

/** 层级名称 */
function tierName(t) {
  return { 1: 'Beta 尝鲜', 2: 'Alpha 内测', 3: '开发者核心' }[t] || '正式用户';
}

app.get('/status', (req, res) => {
  const email = String(req.query.email || '').trim().toLowerCase();
  const db = load();
  const a = db.applications.find((x) => x.email === email);
  if (!a) return res.json({ code: 200, status: 'none' });
  res.json({ code: 200, status: a.status, score: a.score, key: a.key || '', reasons: a.reasons || [] });
});

app.post('/verify', (req, res) => {
  const key = String(req.body.key || '').trim();
  const db = load();
  const rec = db.keys[key];
  if (rec && rec.active) {
    const tier = rec.tier || 1;
    res.json({ code: 200, valid: true, beta: true, tier, tierName: tierName(tier), email: rec.email });
  } else {
    res.json({ code: 200, valid: false, beta: false, tier: 0, tierName: '正式用户' });
  }
});

app.get('/latest', (req, res) => {
  // 按层级返回可推送通道：tier>=2 追加 alpha，tier>=1 追加 beta，正式用户仅 stable
  const key = String(req.query.key || '').trim();
  const db = load();
  const rec = key ? db.keys[key] : null;
  const tier = rec && rec.active ? (rec.tier || 1) : 0;
  const channels = ['stable'];
  if (tier >= 1) channels.push('beta');
  if (tier >= 2) channels.push('alpha');
  if (tier >= 3) channels.push('dev');
  res.json({ code: 200, tier, channels, source: 'github' });
});

// ---------- 错误上报接收（客户端崩溃/异常自动上传） ----------
const ERRORS_DIR = path.join(DATA_DIR, 'errors');
app.post('/api/error', (req, res) => {
  try {
    fs.mkdirSync(ERRORS_DIR, { recursive: true });
    const entry = {
      at: new Date().toISOString(),
      device: req.body.device || {},
      log: String(req.body.log || '').slice(0, 64 * 1024),
    };
    const file = path.join(ERRORS_DIR, `error-${Date.now()}-${crypto.randomBytes(3).toString('hex')}.json`);
    fs.writeFileSync(file, JSON.stringify(entry, null, 2));
    res.json({ code: 200, message: '已接收' });
  } catch (e) {
    res.json({ code: 500, message: '存储失败' });
  }
});

app.listen(PORT, () => {
  console.log(`[GlassSuite Beta] 尝鲜系统已启动: http://0.0.0.0:${PORT}`);
  console.log(`  - 申请要求:  GET /requirements`);
  console.log(`  - 提交申请:  POST /apply`);
  console.log(`  - 状态查询:  GET /status?email=xxx`);
  console.log(`  - 密钥比对:  POST /verify`);
  console.log(`  - 权限检查:  GET /latest?key=xxx`);
});
