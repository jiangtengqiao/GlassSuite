/**
 * 开发者尝鲜系统客户端（鸿蒙端）：
 * 申请提交（服务端自动评分实时判定）、尝鲜码（22~37 位）激活比对、状态查询
 */
import { Net } from './Net';
import { Constants } from './Constants';

export class BetaRequirements {
  title: string = '';
  intro: string = '';
  items: string[] = [];
  threshold: number = 60;
  note: string = '';
}

export class BetaApplyResult {
  status: string = 'none';
  score: number = 0;
  key: string = '';
  reasons: string[] = [];
  message: string = '';
}

export class BetaStore {
  static betaServerUrl: string = 'http://10.0.2.2:3100';
  static betaKey: string = '';
  static betaAccess: boolean = false;

  private static base(): string {
    return BetaStore.betaServerUrl.trim();
  }

  static async requirements(): Promise<BetaRequirements> {
    try {
      const r = await Net.fetchJson(`${BetaStore.base()}/requirements`) as Record<string, Object>;
      const data = r['data'] as Record<string, Object>;
      const req = new BetaRequirements();
      if (data !== undefined && data !== null) {
        req.title = String(data['title'] ?? '');
        req.intro = String(data['intro'] ?? '');
        req.threshold = Number(data['scoreThreshold'] ?? 60);
        req.note = String(data['note'] ?? '');
        const items = data['items'] as Array<Object>;
        if (Array.isArray(items)) {
          req.items = items.map((it: Object) => String(it));
        }
      }
      return req;
    } catch (e) {
      return new BetaRequirements();
    }
  }

  static async apply(email: string, name: string, purpose: string, reason: string, device: string): Promise<BetaApplyResult> {
    const result = new BetaApplyResult();
    try {
      const r = await Net.postJson(`${BetaStore.base()}/apply`, {
        'email': email, 'name': name, 'purpose': purpose, 'reason': reason, 'device': device
      }) as Record<string, Object>;
      result.status = String(r['status'] ?? 'none');
      result.score = Number(r['score'] ?? 0);
      result.key = String(r['key'] ?? '');
      result.message = String(r['message'] ?? '');
      const reasons = r['reasons'] as Array<Object>;
      if (Array.isArray(reasons)) {
        result.reasons = reasons.map((it: Object) => String(it));
      }
      if (result.status === 'approved' && result.key.length > 0) {
        BetaStore.betaKey = result.key;
        BetaStore.betaAccess = true;
        Net.put('beta_key', result.key);
      }
    } catch (e) {
      result.status = 'error';
      result.message = '服务不可达，请检查 Beta 服务器地址';
    }
    return result;
  }

  static async verify(key: string): Promise<boolean> {
    try {
      const r = await Net.postJson(`${BetaStore.base()}/verify`, { 'key': key }) as Record<string, Object>;
      const valid = r['valid'] === true;
      if (valid) {
        BetaStore.betaKey = key;
        BetaStore.betaAccess = true;
        Net.put('beta_key', key);
      }
      return valid;
    } catch (e) {
      return false;
    }
  }

  static restore(): void {
    // 由 Net.init 后调用：恢复持久化密钥
    BetaStore.betaKey = Constants.betaKey;
    BetaStore.betaAccess = Constants.betaKey.length > 0;
  }
}
