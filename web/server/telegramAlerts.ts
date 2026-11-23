import {
  mucNuocPhanTram,
  nhietDoNuoc,
  phanTichLoiBomNuoc,
  type BomNuocState,
} from './bomNuocModel.js';
import type { TelegramConfig } from './configStore.js';
import type { KetNoiStatus } from './mqttBridge.js';

export interface TelegramAlertLogEntry {
  id: number;
  ts: number;
  key: string;
  title: string;
  message: string;
  ok: boolean;
  error?: string;
}

export interface TelegramAlertsStatus {
  enabled: boolean;
  configured: boolean;
  activeIncidents: {
    pumpLocked: boolean;
    gasLeak: boolean;
    serverOffline: boolean;
    routerOffline: boolean;
  };
  history: TelegramAlertLogEntry[];
}

export interface GasSensorReading {
  alert: boolean;
  ppm: number | null;
}

export interface ElectricSpikeResult {
  spike: boolean;
  ngay: string;
  kwh: number;
  avgKwh: number;
  pctIncrease: number;
  estCostVnd: number | null;
}

export function parseGasSensorPayload(
  payload: string,
  thresholdPpm: number = 200,
): GasSensorReading {
  const s = payload.trim();
  if (!s) return { alert: false, ppm: null };

  const lower = s.toLowerCase();
  if (lower === '1' || lower === 'true' || lower === 'on' || lower === 'leak' || lower === 'danger' || lower === 'alert') {
    return { alert: true, ppm: null };
  }
  if (lower === '0' || lower === 'false' || lower === 'off' || lower === 'safe' || lower === 'normal' || lower === 'ok') {
    return { alert: false, ppm: null };
  }

  const numOnly = Number(s);
  if (Number.isFinite(numOnly)) {
    return { alert: numOnly >= thresholdPpm, ppm: numOnly };
  }

  try {
    const obj = JSON.parse(s) as Record<string, unknown>;
    if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
      const rawPpm = obj.ppm ?? obj.gasPpm ?? obj.gas_ppm ?? obj.value;
      const ppmNum = rawPpm !== undefined && rawPpm !== null ? Number(rawPpm) : Number.NaN;
      const ppm = Number.isFinite(ppmNum) ? ppmNum : null;
      const flag = obj.alert === true || obj.leak === true || obj.gas_alert === true || obj.gasAlert === true;
      const alert = flag || (ppm !== null && ppm >= thresholdPpm);
      return { alert, ppm };
    }
  } catch {
    // ignore non-json payload
  }

  return { alert: false, ppm: null };
}

export function kiemTraDienTangVot(
  lichSu: Array<{ ngay: string; kwh: number }>,
  thangSanLuong: number | null,
  thangTien: number | null,
  spikePct: number,
  minDeltaKwh: number,
): ElectricSpikeResult | null {
  if (lichSu.length < 4) return null;
  const valid = lichSu.filter((d) => d.ngay.trim().length > 0 && Number.isFinite(d.kwh) && d.kwh >= 0);
  if (valid.length < 4) return null;

  const latest = valid[valid.length - 1];
  const prevSlice = valid.slice(Math.max(0, valid.length - 8), valid.length - 1);
  if (prevSlice.length < 3) return null;

  const avgKwh = prevSlice.reduce((sum, item) => sum + item.kwh, 0) / prevSlice.length;
  if (avgKwh <= 0) return null;

  const deltaKwh = latest.kwh - avgKwh;
  const pctIncrease = Math.round(((latest.kwh - avgKwh) / avgKwh) * 100);
  const spike = pctIncrease >= spikePct && deltaKwh >= minDeltaKwh;

  const estCostVnd =
    thangTien !== null && thangSanLuong !== null && thangSanLuong > 0
      ? Math.round(latest.kwh * (thangTien / thangSanLuong))
      : null;

  return {
    spike,
    ngay: latest.ngay,
    kwh: latest.kwh,
    avgKwh,
    pctIncrease,
    estCostVnd,
  };
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function formatTimeVn(ts: number): string {
  const d = new Date(ts);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  const mo = String(d.getMonth() + 1).padStart(2, '0');
  return `${hh}:${mm} ${dd}/${mo}`;
}

export class TelegramAlertService {
  private readonly fetchFn: typeof fetch;
  private readonly nowFn: () => number;
  private watchdogTimer: NodeJS.Timeout | null = null;

  private nextLogId = 1;
  private history: TelegramAlertLogEntry[] = [];
  private readonly lastSentAt = new Map<string, number>();
  private readonly alertedElectricDates = new Set<string>();

  // Incident states
  private pumpLocked = false;
  private gasLeak = false;
  private serverOffline = false;
  private routerOffline = false;

  // Heartbeat / telemetry tracking
  private hasSeenServer = false;
  private lastServerSeenMs = 0;
  private hasConnectedMqtt = false;
  private mqttDownSinceMs = 0;

  private hasSeenWifi = false;
  private lastWifiSeenMs = 0;
  private routerDownSinceMs = 0;

  private dienLichSu: Array<{ ngay: string; kwh: number }> = [];
  private dienThangSanLuong: number | null = null;
  private dienThangTien: number | null = null;

  private latestPumpState: BomNuocState | null = null;

  constructor(
    private readonly getConfig: () => TelegramConfig,
    options: {
      fetchFn?: typeof fetch;
      nowFn?: () => number;
      watchdogIntervalMs?: number;
    } = {},
  ) {
    this.fetchFn = options.fetchFn ?? globalThis.fetch;
    this.nowFn = options.nowFn ?? (() => Date.now());

    const intervalMs = options.watchdogIntervalMs ?? 30_000;
    if (intervalMs > 0) {
      this.watchdogTimer = setInterval(() => {
        void this.checkWatchdog();
      }, intervalMs);
    }
  }

  destroy(): void {
    if (this.watchdogTimer) {
      clearInterval(this.watchdogTimer);
      this.watchdogTimer = null;
    }
  }

  getStatus(): TelegramAlertsStatus {
    const cfg = this.getConfig();
    return {
      enabled: cfg.enabled,
      configured: cfg.botToken.trim().length > 0 && cfg.chatId.trim().length > 0,
      activeIncidents: {
        pumpLocked: this.pumpLocked,
        gasLeak: this.gasLeak,
        serverOffline: this.serverOffline,
        routerOffline: this.routerOffline,
      },
      history: this.history,
    };
  }

  observeBomNuocState(s: BomNuocState): void {
    this.latestPumpState = s;
    const faultInfo = phanTichLoiBomNuoc(s.fault);
    const maxPumpTemp = s.temps?.reduce<number | null>((acc, t) => {
      if (t.giaTriTho <= -100) return acc;
      return acc === null ? t.giaTriTho : Math.max(acc, t.giaTriTho);
    }, null) ?? null;

    const overheat = maxPumpTemp !== null && maxPumpTemp >= 68;
    const isDanger =
      s.locked === 1 ||
      s.bstLock === 1 ||
      faultInfo?.mucDo === 'NGUY_HIEM' ||
      overheat;

    if (isDanger) {
      const wasActive = this.pumpLocked;
      this.pumpLocked = true;
      const pct = mucNuocPhanTram(s);
      const waterTemp = nhietDoNuoc(s);
      const lines = [
        '🚨 <b>CẢNH BÁO BƠM NƯỚC KHẨN</b>',
        `• Sự cố: <b>${escapeHtml(faultInfo?.noiDung ?? (overheat ? `Quá nhiệt bơm (${maxPumpTemp?.toFixed(1)} °C)` : 'Hệ thống bơm bị khóa bảo vệ'))}</b>`,
        `• Bồn nước: ${pct !== null ? `${Math.trunc(pct)}%` : '—'}${s.isFull === 1 ? ' (Đầy)' : ''}`,
        `• Trạng thái: Bơm bồn ${s.tankOn === 1 ? 'ĐANG CHẠY' : 'TẮT'} · Bơm áp ${s.bstLock === 1 ? 'KHOÁ' : s.boostOn === 1 ? 'CHẠY' : 'TẮT'}`,
      ];
      if (maxPumpTemp !== null) lines.push(`• Nhiệt độ bơm: ${maxPumpTemp.toFixed(1)} °C`);
      if (waterTemp !== null) lines.push(`• Nhiệt độ nước: ${waterTemp.toFixed(1)} °C`);
      lines.push(`• Lúc: ${formatTimeVn(this.nowFn())}`);
      void this.emitAlert('pump_danger', 'Bơm nước khóa / sự cố nguy hiểm', lines.join('\n'), !wasActive);
    } else if (this.pumpLocked && s.locked !== 1 && s.bstLock !== 1 && !(s.fault && s.fault.trim())) {
      this.pumpLocked = false;
      void this.emitAlert(
        'pump_recovered',
        'Bơm nước đã mở khóa',
        `✅ <b>Hệ thống bơm nước đã mở khóa</b> và hoạt động bình thường trở lại (${formatTimeVn(this.nowFn())}).`,
        true,
        0,
      );
    }
  }

  observeMqttStatus(status: KetNoiStatus): void {
    const now = this.nowFn();
    if (status.state === 'da_noi') {
      this.hasConnectedMqtt = true;
      this.mqttDownSinceMs = 0;
      if (this.serverOffline && (!this.hasSeenServer || now - this.lastServerSeenMs < this.offlineLimitMs())) {
        this.serverOffline = false;
        void this.emitAlert(
          'server_recovered',
          'Server / MQTT đã trực tuyến lại',
          `✅ <b>Kết nối Server & Broker MQTT đã phục hồi</b> lúc ${formatTimeVn(now)}.`,
          true,
          0,
        );
      }
    } else if (status.state === 'loi' && this.hasConnectedMqtt && this.mqttDownSinceMs === 0) {
      this.mqttDownSinceMs = now;
    }
  }

  observeMqttMessage(topic: string, payload: string): void {
    const cfg = this.getConfig();
    const now = this.nowFn();

    if (topic === cfg.gasTopic || topic === 'smarthome/sensor/gas') {
      const reading = parseGasSensorPayload(payload, cfg.gasThresholdPpm);
      if (reading.alert) {
        const wasGas = this.gasLeak;
        this.gasLeak = true;
        const ppmText = reading.ppm !== null ? `${Math.round(reading.ppm)} ppm` : 'Vượt ngưỡng';
        const text = [
          '🔥 <b>CẢNH BÁO RÒ RỈ GAS KHẨN CẤP</b>',
          `• Nồng độ đo được: <b>${escapeHtml(ppmText)}</b>`,
          `• Ngưỡng cảnh báo: ${cfg.gasThresholdPpm} ppm`,
          `• Lúc: ${formatTimeVn(now)}`,
          'Vui lòng kiểm tra van gas và bật thông gió ngay!',
        ].join('\n');
        // Gas ro ri nguy hiem cao: lap lai toi da moi 5 phut neu chua xu ly
        void this.emitAlert('gas_leak', 'Cảnh báo rò rỉ Gas', text, !wasGas, Math.min(cfg.cooldownMinutes, 5) * 60_000);
      } else if (this.gasLeak) {
        this.gasLeak = false;
        const ppmText = reading.ppm !== null ? ` (${Math.round(reading.ppm)} ppm)` : '';
        void this.emitAlert(
          'gas_recovered',
          'Cảm biến Gas trở lại an toàn',
          `✅ <b>Cảm biến Gas đã về mức an toàn${escapeHtml(ppmText)}</b> lúc ${formatTimeVn(now)}.`,
          true,
          0,
        );
      }
      return;
    }

    if (topic.startsWith('server/')) {
      this.hasSeenServer = true;
      this.lastServerSeenMs = now;
      if (this.serverOffline && this.mqttDownSinceMs === 0) {
        this.serverOffline = false;
        void this.emitAlert(
          'server_recovered',
          'Server đã trực tuyến lại',
          `✅ <b>Máy chủ serverubuntu đã gửi dữ liệu trở lại</b> lúc ${formatTimeVn(now)}.`,
          true,
          0,
        );
      }
      return;
    }

    if (topic.startsWith('wifi/')) {
      this.hasSeenWifi = true;
      this.lastWifiSeenMs = now;
      let routerDownNow = false;
      if (topic === 'wifi/info') {
        try {
          const obj = JSON.parse(payload) as { is_connected?: unknown };
          if (obj.is_connected === false) routerDownNow = true;
        } catch {
          // ignore
        }
      } else if (topic === 'wifi/trangthai') {
        const s = payload.trim().toLowerCase();
        if (s === 'offline' || s === 'disconnected' || s === 'down' || s === '0') {
          routerDownNow = true;
        }
      }

      if (routerDownNow) {
        if (this.routerDownSinceMs === 0) this.routerDownSinceMs = now;
      } else if (topic === 'wifi/info' || topic === 'wifi/trangthai') {
        this.routerDownSinceMs = 0;
        if (this.routerOffline) {
          this.routerOffline = false;
          void this.emitAlert(
            'router_recovered',
            'Router đã kết nối lại',
            `✅ <b>Router / WiFi gia đình đã trực tuyến trở lại</b> lúc ${formatTimeVn(now)}.`,
            true,
            0,
          );
        }
      }
      return;
    }

    if (topic === 'dien/thang/sanluong') {
      const n = Number(payload.trim());
      if (Number.isFinite(n)) this.dienThangSanLuong = n;
      return;
    }

    if (topic === 'dien/thang/tien') {
      const n = Number(payload.trim());
      if (Number.isFinite(n)) this.dienThangTien = n;
      return;
    }

    if (topic === 'dien/lichsu') {
      try {
        const arr = JSON.parse(payload);
        if (!Array.isArray(arr)) return;
        const parsed: Array<{ ngay: string; kwh: number }> = [];
        for (const item of arr) {
          if (!item || typeof item !== 'object') continue;
          const o = item as Record<string, unknown>;
          const ngay = typeof o.ngay === 'string' ? o.ngay.trim() : '';
          const kwh = Number(o.kwh);
          if (ngay && Number.isFinite(kwh)) parsed.push({ ngay, kwh });
        }
        this.dienLichSu = parsed;
        this.evaluateElectricSpike();
      } catch {
        // ignore invalid json
      }
    }
  }

  async checkWatchdog(): Promise<void> {
    const cfg = this.getConfig();
    if (!cfg.enabled) return;

    const now = this.nowFn();
    const limitMs = this.offlineLimitMs();

    const serverStale = this.hasSeenServer && now - this.lastServerSeenMs >= limitMs;
    const brokerDown = this.mqttDownSinceMs > 0 && now - this.mqttDownSinceMs >= limitMs;

    if ((serverStale || brokerDown) && !this.serverOffline) {
      this.serverOffline = true;
      const reason = brokerDown
        ? 'Mất kết nối MQTT Broker'
        : `Không nhận được dữ liệu từ serverubuntu quá ${cfg.offlineMinutes} phút`;
      const pumpNote = this.latestPumpState
        ? `\n• Bo bơm hiện tại: ${this.latestPumpState.tankOn === 1 ? 'Đang bơm' : 'Tắt'} (còn nối qua Gateway)`
        : '';
      await this.emitAlert(
        'server_offline',
        `Server rớt mạng quá ${cfg.offlineMinutes} phút`,
        `🔴 <b>CẢNH BÁO SERVER RỚT MẠNG</b>\n• Lý do: ${escapeHtml(reason)}\n• Thời điểm: ${formatTimeVn(now)}${pumpNote}`,
        true,
      );
    }

    const routerDownFlag = this.routerDownSinceMs > 0 && now - this.routerDownSinceMs >= limitMs;
    const routerStale = this.hasSeenWifi && now - this.lastWifiSeenMs >= limitMs;
    if ((routerDownFlag || routerStale) && !this.routerOffline) {
      this.routerOffline = true;
      const detail = routerDownFlag
        ? `Router báo mất kết nối WAN/Internet quá ${cfg.offlineMinutes} phút`
        : `Không nhận được nhịp Router WiFi quá ${cfg.offlineMinutes} phút`;
      await this.emitAlert(
        'router_offline',
        `Router rớt mạng quá ${cfg.offlineMinutes} phút`,
        `📡 <b>CẢNH BÁO ROUTER / MẠNG NHÀ</b>\n• Chi tiết: ${escapeHtml(detail)}\n• Thời điểm: ${formatTimeVn(now)}`,
        true,
      );
    }
  }

  private evaluateElectricSpike(): void {
    const cfg = this.getConfig();
    const res = kiemTraDienTangVot(
      this.dienLichSu,
      this.dienThangSanLuong,
      this.dienThangTien,
      cfg.electricSpikePct,
      cfg.electricSpikeMinKwh,
    );
    if (!res || !res.spike || this.alertedElectricDates.has(res.ngay)) return;
    this.alertedElectricDates.add(res.ngay);

    const lines = [
      '⚡ <b>CẢNH BÁO ĐIỆN EVN TĂNG VỌT</b>',
      `• Ngày ghi nhận: <b>${escapeHtml(res.ngay)}</b>`,
      `• Sản lượng: <b>${res.kwh.toFixed(1)} kWh</b> (+${res.pctIncrease}% so với TB ${res.avgKwh.toFixed(1)} kWh/ngày)`,
    ];
    if (res.estCostVnd !== null) {
      lines.push(`• Ước tính chi phí: <b>${res.estCostVnd.toLocaleString('vi-VN')} đ</b>`);
    }
    lines.push('Kiểm tra các thiết bị công suất lớn (điều hòa, bình nóng lạnh, bơm).');

    void this.emitAlert(`electric_spike:${res.ngay}`, `Điện ngày ${res.ngay} tăng vọt`, lines.join('\n'), true);
  }

  async sendTestAlert(): Promise<{ ok: boolean; error?: string }> {
    const cfg = this.getConfig();
    if (!cfg.botToken.trim() || !cfg.chatId.trim()) {
      return { ok: false, error: 'Chưa nhập Bot Token hoặc Chat ID Telegram.' };
    }
    const text = [
      '🔔 <b>Kiểm tra kết nối Telegram — SmartHome Gateway</b>',
      `• Trạng thái cảnh báo tự động: ${cfg.enabled ? 'ĐANG BẬT' : 'Đang tắt (chỉ thử nghiệm)'}`,
      `• Ngưỡng rớt mạng: ${cfg.offlineMinutes} phút`,
      `• Ngưỡng tăng vọt điện: +${cfg.electricSpikePct}%`,
      `• Lúc: ${formatTimeVn(this.nowFn())}`,
    ].join('\n');
    const res = await this.sendTelegramMessage(text);
    this.recordHistory('test', 'Gửi thử Telegram', 'Kiểm tra kết nối từ Gateway', res.ok, res.error);
    return res;
  }

  async sendTelegramMessage(text: string): Promise<{ ok: boolean; error?: string }> {
    const cfg = this.getConfig();
    const token = cfg.botToken.trim();
    const chatId = cfg.chatId.trim();
    if (!token || !chatId) {
      return { ok: false, error: 'Thiếu Bot Token hoặc Chat ID' };
    }

    const url = `https://api.telegram.org/bot${encodeURIComponent(token)}/sendMessage`;
    try {
      const resp = await this.fetchFn(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chat_id: chatId,
          text,
          parse_mode: 'HTML',
          disable_web_page_preview: true,
        }),
        signal: AbortSignal.timeout(10_000),
      });
      if (!resp.ok) {
        const body = await resp.text().catch(() => '');
        return { ok: false, error: `Telegram HTTP ${resp.status}${body ? `: ${body.slice(0, 120)}` : ''}` };
      }
      return { ok: true };
    } catch (err) {
      return { ok: false, error: err instanceof Error ? err.message : 'Lỗi gọi Telegram API' };
    }
  }

  private offlineLimitMs(): number {
    return Math.max(1, this.getConfig().offlineMinutes) * 60_000;
  }

  private async emitAlert(
    key: string,
    title: string,
    htmlMessage: string,
    bypassCooldown: boolean,
    customCooldownMs?: number,
  ): Promise<void> {
    const cfg = this.getConfig();
    if (!cfg.enabled) return;
    if (!cfg.botToken.trim() || !cfg.chatId.trim()) return;

    const now = this.nowFn();
    const cooldownMs = customCooldownMs ?? Math.max(1, cfg.cooldownMinutes) * 60_000;
    const prev = this.lastSentAt.get(key) ?? 0;
    if (!bypassCooldown && now - prev < cooldownMs) return;

    this.lastSentAt.set(key, now);
    const res = await this.sendTelegramMessage(htmlMessage);
    this.recordHistory(key, title, htmlMessage.replace(/<[^>]+>/g, ''), res.ok, res.error);
  }

  private recordHistory(
    key: string,
    title: string,
    message: string,
    ok: boolean,
    error?: string,
  ): void {
    const entry: TelegramAlertLogEntry = {
      id: this.nextLogId++,
      ts: this.nowFn(),
      key,
      title,
      message,
      ok,
      ...(error ? { error } : {}),
    };
    this.history = [entry, ...this.history].slice(0, 30);
  }
}
