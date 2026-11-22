import crypto from 'node:crypto';
import type { IncomingMessage } from 'node:http';
import type { Duplex } from 'node:stream';
import mqtt, { type MqttClient } from 'mqtt';
import { WebSocket, WebSocketServer } from 'ws';
import {
  BOM_NUOC_TOPICS,
  CHO_XAC_NHAN_BOM_MS,
  dinhDangQueryBaoVe,
  ganBang,
  kiemTraBaoVe,
  lamTronBaoVe,
  nguongImLangGiay,
  NhatKyBomNuocStore,
  nhanCheDoBon,
  parseBomNuocState,
  parseLuongNuocThang,
  type BomNuocCmd,
  type BomNuocState,
  type KenhBomNuoc,
  type KetQuaLenhBomRes,
  type LuongNuocThang,
  type SuKienBom,
  type TrangThaiKetNoiBom,
} from './bomNuocModel.js';
import type { BomNuocConfig } from './configStore.js';

export interface BomNuocSnapshot {
  state: BomNuocState | null;
  usage: LuongNuocThang[];
  link: TrangThaiKetNoiBom;
  kenh: KenhBomNuoc;
  pending: string[];
  log: SuKienBom[];
}

export interface BomNuocBridgeOptions {
  logPath?: string;
  pollFastMs?: number;
  pollSlowMs?: number;
  pollUsageMs?: number;
  lanProbeMs?: number;
  cmdTimeoutMs?: number;
  fetchFn?: typeof fetch;
}

interface CommandWaiter {
  seqAfter: number;
  confirm: (s: BomNuocState) => boolean;
  resolve: (confirmed: boolean) => void;
}

interface ParsedBomCmd {
  cmd: BomNuocCmd;
  khoa: string;
  query: string;
  confirm: (s: BomNuocState) => boolean;
  successMsg: string;
}

const CHUA_THAY_BO_SAU_MS = 15_000;

function hasLanConfig(c: BomNuocConfig): boolean {
  return c.espHost.trim().length > 0;
}

function hasMqttConfig(c: BomNuocConfig): boolean {
  return c.brokerHost.trim().length > 0 && c.brokerPort >= 1 && c.brokerPort <= 65535;
}

function isMqttAuthError(err: unknown): boolean {
  const e = err as { code?: string | number; reasonCode?: number; message?: string } | undefined;
  const msg = (e?.message || '').toLowerCase();
  if (
    e?.code === 4 ||
    e?.code === 5 ||
    e?.reasonCode === 4 ||
    e?.reasonCode === 5 ||
    msg.includes('bad username') ||
    msg.includes('bad user') ||
    msg.includes('not authorized') ||
    msg.includes('password')
  ) {
    return true;
  }
  return false;
}

function validateAndPlanCmd(raw: unknown): ParsedBomCmd | { error: string } {
  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) {
    return { error: 'Yêu cầu lệnh bơm không hợp lệ.' };
  }
  const body = raw as Record<string, unknown>;
  const kind = typeof body.kind === 'string' ? body.kind.trim() : '';

  switch (kind) {
    case 'tank': {
      const mode = typeof body.mode === 'string' ? body.mode.trim().toLowerCase() : '';
      if (mode !== 'auto' && mode !== 'on' && mode !== 'off') {
        return { error: 'Chế độ bồn chỉ nhận: auto, on, off.' };
      }
      return {
        cmd: { kind: 'tank', mode },
        khoa: 'tank',
        query: `tank=${mode}`,
        confirm: (s) => s.tankMode === mode,
        successMsg: `Bơm lên bồn: đã chuyển sang ${nhanCheDoBon(mode)}.`,
      };
    }

    case 'booster': {
      if (typeof body.on !== 'boolean') {
        return { error: 'Thiếu trạng thái bật/tắt bơm tăng áp.' };
      }
      const want = body.on ? 'on' : 'off';
      return {
        cmd: { kind: 'booster', on: body.on },
        khoa: 'booster',
        query: `booster=${want}`,
        confirm: (s) => s.bstMode === want,
        successMsg: body.on ? 'Đã bật bơm tăng áp.' : 'Đã tắt bơm tăng áp.',
      };
    }

    case 'led': {
      if (typeof body.on !== 'boolean') {
        return { error: 'Thiếu trạng thái bật/tắt đèn phụ.' };
      }
      const want = body.on ? 1 : 0;
      return {
        cmd: { kind: 'led', on: body.on },
        khoa: 'led',
        query: `led=${body.on ? 'on' : 'off'}`,
        confirm: (s) => s.den === want,
        successMsg: body.on ? 'Đã bật đèn phụ hộp điện.' : 'Đã tắt đèn phụ hộp điện.',
      };
    }

    case 'clear':
      return {
        cmd: { kind: 'clear' },
        khoa: 'clear',
        query: 'clear=1',
        confirm: (s) => !(s.fault && s.fault.trim().length > 0) && s.locked !== 1 && s.bstLock !== 1,
        successMsg: 'Đã xoá lỗi và mở khoá hệ thống bơm.',
      };

    case 'coiTat':
      return {
        cmd: { kind: 'coiTat' },
        khoa: 'coiTat',
        query: 'coiTat=1',
        confirm: () => true,
        successMsg: 'Đã gửi lệnh tắt còi báo động.',
      };

    case 'coiThu':
      return {
        cmd: { kind: 'coiThu' },
        khoa: 'coiThu',
        query: 'coiThu=1',
        confirm: () => true,
        successMsg: 'Đã gửi lệnh thử còi báo động.',
      };

    case 'protection': {
      const dryCheck = Number(body.dryCheck);
      const minFlow = Number(body.minFlow);
      const maxRun = Number(body.maxRun);
      const minRest = Number(body.minRest);
      const pulses = Number(body.pulses);
      const err = kiemTraBaoVe(dryCheck, minFlow, maxRun, minRest, pulses);
      if (err) {
        return { error: err };
      }
      const p = lamTronBaoVe(dryCheck, minRest, maxRun, pulses, minFlow);
      return {
        cmd: { kind: 'protection', ...p },
        khoa: 'protection',
        query: dinhDangQueryBaoVe(p),
        confirm: (s) =>
          s.dryCheck === p.dryCheck &&
          ganBang(s.minRest, p.minRest, 0.05) &&
          s.maxRun === p.maxRun &&
          ganBang(s.pulses, p.pulses, 0.05) &&
          ganBang(s.minFlow, p.minFlow, 0.005),
        successMsg: 'Bo đã nhận và lưu cài đặt bảo vệ bơm.',
      };
    }

    case 'lvlSetEmpty':
      return {
        cmd: { kind: 'lvlSetEmpty' },
        khoa: 'lvlSetEmpty',
        query: 'lvlSetEmpty=1',
        confirm: () => true,
        successMsg: 'Đã ghi mốc CẠN cho cảm biến mức nước.',
      };

    case 'lvlSetFull':
      return {
        cmd: { kind: 'lvlSetFull' },
        khoa: 'lvlSetFull',
        query: 'lvlSetFull=1',
        confirm: () => true,
        successMsg: 'Đã ghi mốc ĐẦY cho cảm biến mức nước.',
      };

    case 'calReset':
      return {
        cmd: { kind: 'calReset' },
        khoa: 'calReset',
        query: 'calReset=1',
        confirm: (s) => s.calPT === 0,
        successMsg: 'Đã đặt lại bộ đếm hiệu chuẩn lưu lượng.',
      };

    case 'usageClear':
      return {
        cmd: { kind: 'usageClear' },
        khoa: 'usageClear',
        query: 'usageClear=1',
        confirm: () => true,
        successMsg: 'Đã xoá lịch sử lượng nước trên bo.',
      };

    default:
      return { error: `Lệnh "${kind}" không được hỗ trợ.` };
  }
}

export class BomNuocBridge {
  private readonly wss = new WebSocketServer({ noServer: true });
  private readonly clients = new Map<WebSocket, { viewing: boolean }>();
  private readonly nhatKy: NhatKyBomNuocStore;
  private readonly pollFastMs: number;
  private readonly pollSlowMs: number;
  private readonly pollUsageMs: number;
  private readonly lanProbeMs: number;
  private readonly cmdTimeoutMs: number;
  private readonly fetchFn: typeof fetch;

  private config: BomNuocConfig;
  private generation = 0;

  // Shared published snapshot
  private state: BomNuocState | null = null;
  private usage: LuongNuocThang[] = [];
  private link: TrangThaiKetNoiBom = { state: 'chua_cau_hinh' };
  private kenh: KenhBomNuoc = 'KHONG';
  private stateSeq = 0;

  // Per-channel state
  private lanState: BomNuocState | null = null;
  private lanUsage: LuongNuocThang[] | null = null;
  private lanLink: TrangThaiKetNoiBom = { state: 'chua_cau_hinh' };

  private mqttClient: MqttClient | null = null;
  private mqttState: BomNuocState | null = null;
  private mqttUsage: LuongNuocThang[] | null = null;
  private mqttLink: TrangThaiKetNoiBom = { state: 'chua_cau_hinh' };
  private mqttLastStateAt = 0;
  private mqttConnectedAt = 0;
  private mqttDeviceOnline = false;
  private mqttAuthRejected = false;

  // Timers & serialized HTTP mutex
  private lanPollTimer: NodeJS.Timeout | null = null;
  private lanUsageTimer: NodeJS.Timeout | null = null;
  private lanProbeTimer: NodeJS.Timeout | null = null;
  private mqttWatchdogTimer: NodeJS.Timeout | null = null;
  private httpChain: Promise<unknown> = Promise.resolve();

  // CommandBus
  private readonly pendingKhoa = new Set<string>();
  private readonly waiters = new Set<CommandWaiter>();

  constructor(initialConfig: BomNuocConfig, options: BomNuocBridgeOptions = {}) {
    this.config = initialConfig;
    this.nhatKy = new NhatKyBomNuocStore(options.logPath);
    this.pollFastMs = options.pollFastMs ?? 1000;
    this.pollSlowMs = options.pollSlowMs ?? 5000;
    this.pollUsageMs = options.pollUsageMs ?? 30_000;
    this.lanProbeMs = options.lanProbeMs ?? 15_000;
    this.cmdTimeoutMs = options.cmdTimeoutMs ?? CHO_XAC_NHAN_BOM_MS;
    this.fetchFn = options.fetchFn ?? globalThis.fetch;

    this.wss.on('connection', (ws) => {
      this.clients.set(ws, { viewing: false });
      this.sendJson(ws, { type: 'snapshot', ...this.getSnapshot() });

      ws.on('message', (raw) => {
        try {
          const msg = JSON.parse(raw.toString()) as { type?: string; active?: boolean };
          if (msg.type === 'viewing' && typeof msg.active === 'boolean') {
            this.clients.set(ws, { viewing: msg.active });
          }
        } catch {
          // ignore malformed WS frame
        }
      });

      ws.on('close', () => {
        this.clients.delete(ws);
      });
      ws.on('error', () => {
        this.clients.delete(ws);
      });
    });

    this.connect(initialConfig);
  }

  handleUpgrade(req: IncomingMessage, socket: Duplex, head: Buffer): void {
    this.wss.handleUpgrade(req, socket, head, (ws) => {
      this.wss.emit('connection', ws, req);
    });
  }

  getLink(): TrangThaiKetNoiBom {
    return this.link;
  }

  getKenh(): KenhBomNuoc {
    return this.kenh;
  }

  getSnapshot(): BomNuocSnapshot {
    return {
      state: this.state,
      usage: this.usage,
      link: this.link,
      kenh: this.kenh,
      pending: Array.from(this.pendingKhoa),
      log: this.nhatKy.getDanhSach(),
    };
  }

  clearLog(): void {
    this.nhatKy.xoaTatCa();
    this.broadcast({ type: 'log', log: this.nhatKy.getDanhSach() });
  }

  connect(cfg: BomNuocConfig): void {
    this.disconnect();
    this.config = cfg;
    const gen = ++this.generation;
    this.nhatKy.datLaiTheoDoi();

    const coLan = hasLanConfig(cfg);
    const coMqtt = hasMqttConfig(cfg);

    if (!coLan && !coMqtt) {
      this.state = null;
      this.usage = [];
      this.link = { state: 'chua_cau_hinh' };
      this.kenh = 'KHONG';
      this.broadcast({ type: 'snapshot', ...this.getSnapshot() });
      return;
    }

    if (coMqtt) {
      this.startMqtt(cfg, gen);
    }

    if (coLan) {
      this.kenh = coMqtt ? 'MQTT' : 'LAN';
      this.lanLink = { state: 'dang_noi' };
      this.scheduleLanProbe(gen, 0);
      this.scheduleLanPoll(gen, this.pollFastMs);
      this.scheduleLanUsage(gen, 1500);
    } else {
      this.kenh = 'MQTT';
      this.setLink(this.mqttLink);
    }
  }

  disconnect(): void {
    this.generation++;

    if (this.lanPollTimer) { clearTimeout(this.lanPollTimer); this.lanPollTimer = null; }
    if (this.lanUsageTimer) { clearTimeout(this.lanUsageTimer); this.lanUsageTimer = null; }
    if (this.lanProbeTimer) { clearTimeout(this.lanProbeTimer); this.lanProbeTimer = null; }
    if (this.mqttWatchdogTimer) { clearInterval(this.mqttWatchdogTimer); this.mqttWatchdogTimer = null; }

    for (const w of Array.from(this.waiters)) {
      w.resolve(false);
    }
    this.waiters.clear();

    const c = this.mqttClient;
    this.mqttClient = null;
    if (c) {
      try {
        c.removeAllListeners();
        c.end(true);
      } catch {
        // ignore
      }
    }

    this.lanState = null;
    this.lanUsage = null;
    this.lanLink = { state: 'chua_cau_hinh' };
    this.mqttState = null;
    this.mqttUsage = null;
    this.mqttLink = { state: 'chua_cau_hinh' };
    this.mqttLastStateAt = 0;
    this.mqttConnectedAt = 0;
    this.mqttDeviceOnline = false;
    this.mqttAuthRejected = false;
    this.kenh = 'KHONG';
    this.link = { state: 'chua_cau_hinh' };
  }

  destroy(): void {
    this.disconnect();
    for (const ws of Array.from(this.clients.keys())) {
      try {
        ws.close();
      } catch {
        // ignore
      }
    }
    this.clients.clear();
    try {
      this.wss.close();
    } catch {
      // ignore
    }
  }

  async executeCommand(raw: unknown): Promise<KetQuaLenhBomRes> {
    const planned = validateAndPlanCmd(raw);
    if ('error' in planned) {
      return { ok: false, status: 'khong_hop_le', message: planned.error };
    }

    const { cmd, khoa, query, confirm, successMsg } = planned;
    if (this.pendingKhoa.has(khoa)) {
      return { ok: false, status: 'dang_ban', message: 'Lệnh trước đang chờ bo xác nhận…' };
    }

    this.pendingKhoa.add(khoa);
    this.broadcast({ type: 'pending', pending: Array.from(this.pendingKhoa) });

    try {
      const seqBefore = this.stateSeq;
      let resolved = false;
      let timer: NodeJS.Timeout | null = null;
      let waiter: CommandWaiter | null = null;

      const confirmPromise = new Promise<boolean>((resolve) => {
        waiter = {
          seqAfter: seqBefore,
          confirm,
          resolve: (ok) => {
            if (resolved) return;
            resolved = true;
            if (timer) clearTimeout(timer);
            if (waiter) this.waiters.delete(waiter);
            resolve(ok);
          },
        };
        this.waiters.add(waiter);
        timer = setTimeout(() => {
          waiter?.resolve(false);
        }, this.cmdTimeoutMs);
      });

      const sendRes = await this.sendViaPreferredChannel(query);
      if (!sendRes.ok) {
        waiter!.resolve(false);
        return {
          ok: false,
          status: 'gui_that_bai',
          message: `Chưa gửi được lệnh bơm: ${sendRes.error}`,
        };
      }

      const confirmed = await confirmPromise;
      if (!confirmed) {
        return {
          ok: false,
          status: 'chua_xac_nhan',
          message: 'Bo bơm chưa xác nhận lệnh. Kiểm tra bo còn online không rồi thử lại.',
        };
      }

      if (cmd.kind === 'booster' && cmd.on) {
        const s = this.state;
        if (s && s.boostOn !== 1 && (s.bstLock === 1 || s.locked === 1)) {
          return {
            ok: false,
            status: 'bi_khoa_bao_ve',
            message: 'Bo nhận lệnh nhưng không bật được vì đang khoá bảo vệ. Bấm Xoá lỗi trước.',
          };
        }
      }

      if (cmd.kind === 'usageClear' && this.kenh === 'LAN' && hasLanConfig(this.config)) {
        void this.fetchLanUsage(this.generation);
      }

      return { ok: true, status: 'da_xac_nhan', message: successMsg };
    } finally {
      this.pendingKhoa.delete(khoa);
      this.broadcast({ type: 'pending', pending: Array.from(this.pendingKhoa) });
    }
  }

  // ---- HTTP LAN serialized with mutex ----

  private withHttpLock<T>(fn: () => Promise<T>): Promise<T> {
    const run = this.httpChain.then(fn, fn);
    this.httpChain = run.catch(() => undefined);
    return run;
  }

  private isFastPoll(): boolean {
    for (const info of this.clients.values()) {
      if (info.viewing) return true;
    }
    return this.lanState?.tankOn === 1 || this.lanState?.boostOn === 1;
  }

  private lanUrl(apiPath: string): string {
    return `http://${this.config.espHost.trim()}${apiPath}`;
  }

  private async httpGetText(apiPath: string, timeoutMs: number): Promise<string> {
    const resp = await this.fetchFn(this.lanUrl(apiPath), {
      method: 'GET',
      signal: AbortSignal.timeout(timeoutMs),
    });
    if (!resp.ok) {
      throw new Error(`HTTP ${resp.status}`);
    }
    return resp.text();
  }

  private async probeLanOnce(gen: number): Promise<boolean> {
    if (gen !== this.generation || !hasLanConfig(this.config)) return false;
    return this.withHttpLock(async () => {
      if (gen !== this.generation || !hasLanConfig(this.config)) return false;
      try {
        const text = await this.httpGetText('/api/state', 2500);
        const parsed = parseBomNuocState(text);
        if (!parsed) return false;
        this.onLanState(parsed, gen);
        return true;
      } catch {
        return false;
      }
    });
  }

  private scheduleLanProbe(gen: number, delayMs: number): void {
    if (gen !== this.generation || !hasLanConfig(this.config)) return;
    this.lanProbeTimer = setTimeout(() => {
      void this.runLanProbe(gen);
    }, delayMs);
  }

  private async runLanProbe(gen: number): Promise<void> {
    if (gen !== this.generation || !hasLanConfig(this.config)) return;
    const goiDuoc = this.config.uuTienLan && (await this.probeLanOnce(gen));
    if (gen !== this.generation) return;

    const kenhMoi: KenhBomNuoc = goiDuoc
      ? 'LAN'
      : hasMqttConfig(this.config)
        ? 'MQTT'
        : hasLanConfig(this.config)
          ? 'LAN'
          : 'KHONG';

    if (kenhMoi !== this.kenh) {
      this.kenh = kenhMoi;
      this.dongBoTuKenh(kenhMoi);
    }

    this.scheduleLanProbe(gen, this.lanProbeMs);
  }

  private scheduleLanPoll(gen: number, delayMs: number): void {
    if (gen !== this.generation || !hasLanConfig(this.config)) return;
    this.lanPollTimer = setTimeout(() => {
      void this.runLanPoll(gen);
    }, delayMs);
  }

  private async runLanPoll(gen: number): Promise<void> {
    if (gen !== this.generation || !hasLanConfig(this.config)) return;
    await this.withHttpLock(async () => {
      if (gen !== this.generation || !hasLanConfig(this.config)) return;
      try {
        const text = await this.httpGetText('/api/state', 4000);
        const parsed = parseBomNuocState(text);
        if (!parsed) throw new Error('JSON trạng thái không hợp lệ');
        this.onLanState(parsed, gen);
      } catch {
        if (gen !== this.generation) return;
        this.lanLink = { state: 'bo_offline' };
        if (this.lanOwns()) {
          this.setLink(this.lanLink);
        }
      }
    });

    if (gen === this.generation) {
      const nextMs = this.isFastPoll() ? this.pollFastMs : this.pollSlowMs;
      this.scheduleLanPoll(gen, nextMs);
    }
  }

  private scheduleLanUsage(gen: number, delayMs: number): void {
    if (gen !== this.generation || !hasLanConfig(this.config)) return;
    this.lanUsageTimer = setTimeout(() => {
      void this.runLanUsage(gen);
    }, delayMs);
  }

  private async runLanUsage(gen: number): Promise<void> {
    if (gen !== this.generation || !hasLanConfig(this.config)) return;
    if (this.lanLink.state === 'truc_tiep' || this.lanState !== null) {
      await this.fetchLanUsage(gen);
    }
    if (gen === this.generation) {
      this.scheduleLanUsage(gen, this.lanUsage === null ? 3000 : this.pollUsageMs);
    }
  }

  private async fetchLanUsage(gen: number): Promise<void> {
    if (gen !== this.generation || !hasLanConfig(this.config)) return;
    await this.withHttpLock(async () => {
      if (gen !== this.generation || !hasLanConfig(this.config)) return;
      try {
        const text = await this.httpGetText('/api/usage', 4000);
        const parsed = parseLuongNuocThang(text);
        if (!parsed) return;
        this.lanUsage = parsed;
        if (this.lanOwns()) {
          this.applyUsage(parsed);
        }
      } catch {
        // ignore transient usage fetch errors
      }
    });
  }

  private lanOwns(): boolean {
    return this.kenh === 'LAN' || !hasMqttConfig(this.config);
  }

  private onLanState(s: BomNuocState, gen: number): void {
    if (gen !== this.generation) return;
    this.lanState = s;
    this.lanLink = { state: 'truc_tiep', kenh: 'LAN', tuoiGiay: 0 };
    if (this.lanOwns()) {
      this.applyState(s);
      this.setLink(this.lanLink);
    }
  }

  // ---- MQTT transport ----

  private startMqtt(cfg: BomNuocConfig, gen: number): void {
    const protocol = cfg.brokerTls ? 'mqtts' : 'mqtt';
    const url = `${protocol}://${cfg.brokerHost.trim()}:${cfg.brokerPort}`;
    const clientId = `web-sh-bom-${crypto.randomBytes(4).toString('hex')}`;

    this.mqttLink = { state: 'dang_noi' };
    if (this.kenh !== 'LAN') this.setLink(this.mqttLink);

    const client = mqtt.connect(url, {
      clientId,
      clean: true,
      reconnectPeriod: 4000,
      connectTimeout: 15_000,
      keepalive: 30,
      username: cfg.brokerUser.trim() || undefined,
      password: cfg.brokerPass || undefined,
    });
    this.mqttClient = client;

    client.on('connect', () => {
      if (this.generation !== gen) return;
      this.mqttConnectedAt = Date.now();
      client.subscribe(
        [BOM_NUOC_TOPICS.STATE, BOM_NUOC_TOPICS.STATUS, BOM_NUOC_TOPICS.USAGE],
        { qos: 1 },
        (err) => {
          if (this.generation !== gen || !err) return;
          this.mqttLink = { state: 'loi', lyDo: err.message || 'Không đăng ký được topic bơm' };
          if (this.kenh !== 'LAN') this.setLink(this.mqttLink);
        },
      );
    });

    client.on('offline', () => {
      if (this.generation !== gen || this.mqttAuthRejected) return;
      this.mqttDeviceOnline = false;
      this.mqttLink = { state: 'mat_broker' };
      if (this.kenh !== 'LAN') this.setLink(this.mqttLink);
    });

    client.on('error', (err) => {
      if (this.generation !== gen) return;
      if (isMqttAuthError(err)) {
        this.mqttAuthRejected = true;
        this.mqttLink = { state: 'sai_tai_khoan' };
        if (this.kenh !== 'LAN') this.setLink(this.mqttLink);
        try {
          client.end(true);
        } catch {
          // ignore
        }
        return;
      }
      this.mqttLink = { state: 'loi', lyDo: (err as Error)?.message || 'lỗi broker bơm' };
      if (this.kenh !== 'LAN') this.setLink(this.mqttLink);
    });

    client.on('message', (topic, buf) => {
      if (this.generation !== gen) return;
      const payload = buf.toString('utf8');
      if (topic === BOM_NUOC_TOPICS.STATUS) {
        this.mqttDeviceOnline = payload.trim() === 'online';
      } else if (topic === BOM_NUOC_TOPICS.STATE) {
        const parsed = parseBomNuocState(payload);
        if (parsed) {
          this.mqttState = parsed;
          this.mqttLastStateAt = Date.now();
          this.mqttDeviceOnline = true;
          if (this.kenh !== 'LAN') {
            this.applyState(parsed);
          }
        }
      } else if (topic === BOM_NUOC_TOPICS.USAGE) {
        const parsed = parseLuongNuocThang(payload);
        if (parsed) {
          this.mqttUsage = parsed;
          if (this.kenh !== 'LAN') {
            this.applyUsage(parsed);
          }
        }
      }
    });

    this.mqttWatchdogTimer = setInterval(() => {
      if (this.generation !== gen) return;
      if (this.mqttAuthRejected) return;
      const connected = client.connected;
      const last = this.mqttLastStateAt;
      let nextLink: TrangThaiKetNoiBom;
      if (!connected) {
        nextLink =
          this.mqttLink.state === 'dang_noi' ? { state: 'dang_noi' } : { state: 'mat_broker' };
      } else if (last === 0) {
        nextLink =
          this.mqttConnectedAt > 0 && Date.now() - this.mqttConnectedAt > CHUA_THAY_BO_SAU_MS
            ? { state: 'chi_co_broker' }
            : { state: 'dang_noi' };
      } else {
        const ageSec = Math.max(0, Math.trunc((Date.now() - last) / 1000));
        const threshold = nguongImLangGiay(this.mqttState);
        if (!this.mqttDeviceOnline) {
          nextLink = { state: 'bo_offline' };
        } else if (ageSec > threshold) {
          nextLink = { state: 'so_cu', tuoiGiay: ageSec };
        } else {
          nextLink = { state: 'truc_tiep', kenh: 'MQTT', tuoiGiay: ageSec };
        }
      }
      this.mqttLink = nextLink;
      if (this.kenh !== 'LAN') {
        this.setLink(nextLink);
      }
    }, 1000);
  }

  private dongBoTuKenh(k: KenhBomNuoc): void {
    if (k === 'LAN') {
      this.setLink(this.lanLink);
      if (this.lanState) this.applyState(this.lanState);
      if (this.lanUsage) this.applyUsage(this.lanUsage);
    } else if (k === 'MQTT') {
      this.setLink(this.mqttLink);
      if (this.mqttState) this.applyState(this.mqttState);
      if (this.mqttUsage) this.applyUsage(this.mqttUsage);
    } else {
      this.setLink({ state: 'chua_cau_hinh' });
    }
    this.broadcast({ type: 'link', link: this.link, kenh: this.kenh });
  }

  private async sendViaPreferredChannel(query: string): Promise<{ ok: true } | { ok: false; error: string }> {
    const gen = this.generation;
    const coLan = hasLanConfig(this.config);
    const coMqtt = hasMqttConfig(this.config);

    if (this.kenh === 'LAN' && coLan) {
      const lanRes = await this.withHttpLock(async () => {
        if (gen !== this.generation) return { ok: false as const, error: 'Cấu hình đã đổi' };
        try {
          const text = await this.httpGetText(`/api/cmd?${query}`, 4000);
          const parsed = parseBomNuocState(text);
          if (!parsed) {
            return { ok: false as const, error: 'JSON phản hồi không hợp lệ' };
          }
          this.onLanState(parsed, gen);
          return { ok: true as const };
        } catch (err) {
          return {
            ok: false as const,
            error: err instanceof Error ? err.message : 'lỗi HTTP LAN',
          };
        }
      });
      if (lanRes.ok) return lanRes;
      if (!coMqtt) return lanRes;
    }

    if (coMqtt) {
      const c = this.mqttClient;
      if (!c || !c.connected) {
        return { ok: false, error: 'chưa nối được broker bơm' };
      }
      return new Promise((resolve) => {
        c.publish(BOM_NUOC_TOPICS.CMD, query, { qos: 1, retain: false }, (err) => {
          if (err) resolve({ ok: false, error: err.message || 'không publish được lệnh MQTT' });
          else resolve({ ok: true });
        });
      });
    }

    return { ok: false, error: 'chưa cấu hình kết nối bo bơm' };
  }

  private applyState(s: BomNuocState): void {
    this.state = s;
    this.stateSeq++;
    const logBeforeLen = this.nhatKy.getDanhSach().length;
    const added = this.nhatKy.quanSat(s);
    this.broadcast({ type: 'state', state: s });
    if (added.length > 0 || this.nhatKy.getDanhSach().length !== logBeforeLen) {
      this.broadcast({ type: 'log', log: this.nhatKy.getDanhSach() });
    }
    for (const w of Array.from(this.waiters)) {
      if (this.stateSeq > w.seqAfter && w.confirm(s)) {
        w.resolve(true);
      }
    }
  }

  private applyUsage(u: LuongNuocThang[]): void {
    this.usage = u;
    this.broadcast({ type: 'usage', usage: u });
  }

  private setLink(next: TrangThaiKetNoiBom): void {
    const prevKey = JSON.stringify(this.link);
    const nextKey = JSON.stringify(next);
    this.link = next;
    if (prevKey !== nextKey) {
      this.broadcast({ type: 'link', link: next, kenh: this.kenh });
    }
  }

  private sendJson(ws: WebSocket, data: unknown): void {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data));
    }
  }

  private broadcast(data: unknown): void {
    const serialized = JSON.stringify(data);
    for (const ws of this.clients.keys()) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(serialized);
      }
    }
  }
}
