import fs from 'node:fs';
import path from 'node:path';
import type { TelegramConfig } from './configStore.js';

const TRYCLOUDFLARE_RE = /https:\/\/[a-z0-9-]+\.trycloudflare\.com/gi;
const TAIL_BYTES = 65536;

export interface TunnelStatus {
  active: boolean;
  url: string | null;
  cockpitUrl: string | null;
  updatedAt: number | null;
}

function extractLatestTunnelUrl(logPath: string): string | null {
  try {
    if (!fs.existsSync(logPath)) return null;
    const stat = fs.statSync(logPath);
    if (stat.size <= 0) return null;

    const readLen = Math.min(stat.size, TAIL_BYTES);
    const startPos = Math.max(0, stat.size - readLen);
    const fd = fs.openSync(logPath, 'r');
    const buf = Buffer.alloc(readLen);
    try {
      fs.readSync(fd, buf, 0, readLen, startPos);
    } finally {
      fs.closeSync(fd);
    }

    const text = buf.toString('utf8');
    const matches = text.match(TRYCLOUDFLARE_RE);
    if (!matches || matches.length === 0) return null;
    return matches[matches.length - 1].toLowerCase();
  } catch {
    return null;
  }
}

/**
 * Theo doi file log cua 2 container `cloudflared` (SmartHome Web + Cockpit 9090):
 *  1) Hien thi ca 2 link 4G `https://....trycloudflare.com` tren giao dien Web.
 *  2) Tu dong gui tin nhan tong hop ve Telegram ngay khi co link moi (sau khi khoi dong lai).
 */
export class TunnelWatcher {
  private readonly logPath: string;
  private readonly cockpitLogPath: string;
  private readonly statePath: string;
  private currentUrl: string | null = null;
  private currentCockpitUrl: string | null = null;
  private updatedAt: number | null = null;
  private lastNotifiedSignature: string = '';
  private timer: NodeJS.Timeout | null = null;
  private debounceTimer: NodeJS.Timeout | null = null;
  private notifying = false;

  constructor(
    private readonly getTelegramConfig: () => TelegramConfig,
    opts?: { logPath?: string; cockpitLogPath?: string; statePath?: string },
  ) {
    const dataDir = path.dirname(
      process.env.CONFIG_PATH || path.resolve(process.cwd(), 'data', 'config.json'),
    );
    this.logPath =
      opts?.logPath ||
      process.env.CLOUDFLARED_LOG_PATH ||
      path.join(dataDir, 'cloudflared.log');
    this.cockpitLogPath =
      opts?.cockpitLogPath ||
      process.env.COCKPIT_CLOUDFLARED_LOG_PATH ||
      path.join(dataDir, 'cockpit_cloudflared.log');
    this.statePath =
      opts?.statePath || path.join(dataDir, 'last_notified_tunnel_url.txt');

    try {
      if (fs.existsSync(this.statePath)) {
        const raw = fs.readFileSync(this.statePath, 'utf8').trim();
        this.lastNotifiedSignature = raw;
        const [u1, u2] = raw.split('|');
        if (u1?.startsWith('https://')) this.currentUrl = u1;
        if (u2?.startsWith('https://')) this.currentCockpitUrl = u2;
      }
    } catch {
      // ignore
    }
  }

  start(pollMs = 4000): void {
    void this.scanOnce(true);
    if (this.timer) clearInterval(this.timer);
    this.timer = setInterval(() => {
      void this.scanOnce(true);
    }, pollMs);
    this.timer.unref?.();
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = null;
    }
  }

  getStatus(): TunnelStatus {
    return {
      active: Boolean(this.currentUrl || this.currentCockpitUrl),
      url: this.currentUrl,
      cockpitUrl: this.currentCockpitUrl,
      updatedAt: this.updatedAt,
    };
  }

  async scanOnce(useDebounce = false): Promise<void> {
    const webUrl = extractLatestTunnelUrl(this.logPath);
    const cpUrl = extractLatestTunnelUrl(this.cockpitLogPath);

    let changed = false;
    if (webUrl && webUrl !== this.currentUrl) {
      this.currentUrl = webUrl;
      changed = true;
    }
    if (cpUrl && cpUrl !== this.currentCockpitUrl) {
      this.currentCockpitUrl = cpUrl;
      changed = true;
    }
    if (changed) {
      this.updatedAt = Date.now();
    }

    const sig = `${this.currentUrl || ''}|${this.currentCockpitUrl || ''}`;
    if (sig === '|' || sig === this.lastNotifiedSignature) return;

    if (useDebounce) {
      // Cho 3.5 giay de neu ca 2 container cloudflared cung vua bat thi gop chung 1 tin nhan Telegram
      if (this.debounceTimer) return;
      this.debounceTimer = setTimeout(() => {
        this.debounceTimer = null;
        void this.flushNotify();
      }, 3500);
      this.debounceTimer.unref?.();
    } else {
      await this.flushNotify();
    }
  }

  private async flushNotify(): Promise<void> {
    if (this.notifying) return;
    const sig = `${this.currentUrl || ''}|${this.currentCockpitUrl || ''}`;
    if (sig === '|' || sig === this.lastNotifiedSignature) return;

    const tg = this.getTelegramConfig();
    if (!tg.botToken.trim() || !tg.chatId.trim()) {
      return;
    }

    this.notifying = true;
    try {
      const lines = ['🌐 SmartHome 4G từ xa đã sẵn sàng!'];
      if (this.currentUrl) {
        lines.push('', '🏠 Điều khiển Nhà & Camera 24/24:', this.currentUrl);
      }
      if (this.currentCockpitUrl) {
        lines.push('', '🖥️ Quản trị Server Ubuntu (:9090 Cockpit):', this.currentCockpitUrl);
      }
      lines.push('', '🔒 Cả 2 đường dẫn đều được khóa bảo vệ bằng mật khẩu từ xa (lưu phiên 30 ngày).');

      const resp = await fetch(`https://api.telegram.org/bot${tg.botToken.trim()}/sendMessage`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chat_id: tg.chatId.trim(),
          text: lines.join('\n'),
          disable_web_page_preview: true,
        }),
      });

      if (resp.ok) {
        this.lastNotifiedSignature = sig;
        fs.mkdirSync(path.dirname(this.statePath), { recursive: true });
        fs.writeFileSync(this.statePath, sig, { encoding: 'utf8', mode: 0o600 });
      }
    } catch {
      // thu lai o chu ky sau
    } finally {
      this.notifying = false;
    }
  }
}
