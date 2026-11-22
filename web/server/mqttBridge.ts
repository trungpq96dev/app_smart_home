import mqtt, { type MqttClient } from 'mqtt';
import { WebSocket, WebSocketServer } from 'ws';
import type { IncomingMessage } from 'node:http';
import type { Duplex } from 'node:stream';
import type { MqttConfig } from './configStore.js';

export type KetNoiStatus =
  | { state: 'chua_cau_hinh' }
  | { state: 'dang_noi' }
  | { state: 'da_noi' }
  | { state: 'loi'; moTa: string };

const SUB_TOPICS = ['dien/#', 'wifi/#', 'server/#', 'congtac/#', 'cuktech/#'];

/**
 * Chi cho phep publish vao dung cac topic dieu khien.
 * Tuyet doi khong cho phep ghi vao `dien/` (nguyen tac du an).
 */
const PUBLISH_ALLOWLIST = new Set([
  'smarthome/control',
  'wifi/cmd',
  'cuktech/charger/port',
  'cuktech/charger/ble',
  'cuktech/charger/set',
]);

function isConfigReady(c: MqttConfig): boolean {
  return c.host.trim().length > 0 && c.cong >= 1 && c.cong <= 65535;
}

function translateMqttError(err: unknown): string {
  const e = err as { code?: string | number; message?: string; reasonCode?: number } | undefined;
  const msg = e?.message || '';
  const code = String(e?.code || '');
  const lower = msg.toLowerCase();

  if (code === 'ENOTFOUND' || code === 'EAI_AGAIN' || lower.includes('enotfound')) {
    return 'Không tìm thấy máy chủ broker. Sai tên miền, hoặc máy chủ chưa vào được mạng có broker đó.';
  }
  if (
    lower.includes('wrong version number') ||
    lower.includes('wrong_version_number') ||
    lower.includes('packet length')
  ) {
    return 'Cổng này không nói TLS. Tắt TLS hoặc đổi sang cổng 8883.';
  }
  if (
    code === 'UNABLE_TO_VERIFY_LEAF_SIGNATURE' ||
    code === 'CERT_HAS_EXPIRED' ||
    code === 'DEPTH_ZERO_SELF_SIGNED_CERT' ||
    lower.includes('certificate') ||
    lower.includes('self-signed')
  ) {
    return 'Chứng chỉ TLS của broker không được tin cậy. Nếu dùng chứng chỉ tự ký, cần cấu hình CA trên máy chủ.';
  }
  if (code === 'ECONNREFUSED' || lower.includes('econnrefused')) {
    return 'Broker từ chối kết nối. Kiểm tra cổng và xem Mosquitto/EMQX có đang chạy không.';
  }
  if (code === 'ETIMEDOUT' || lower.includes('connack timeout') || lower.includes('timeout')) {
    return 'Không gọi được broker. Kiểm tra địa chỉ, cổng và mạng.';
  }
  if (e?.code === 4 || e?.reasonCode === 4 || lower.includes('bad username or password')) {
    return 'Sai tài khoản hoặc mật khẩu MQTT.';
  }
  if (e?.code === 5 || e?.reasonCode === 5 || lower.includes('not authorized')) {
    return 'Tài khoản MQTT không được phép kết nối. Kiểm tra quyền trên broker.';
  }
  return msg ? `Lỗi kết nối: ${msg}` : 'Lỗi kết nối broker không rõ nguyên nhân.';
}

export class MqttBridge {
  private client: MqttClient | null = null;
  private status: KetNoiStatus = { state: 'chua_cau_hinh' };
  private retainedCache = new Map<string, string>();
  private readonly wss = new WebSocketServer({ noServer: true });
  private readonly clients = new Set<WebSocket>();
  private generation = 0;

  constructor(initialConfig: MqttConfig) {
    this.wss.on('connection', (ws) => {
      this.clients.add(ws);
      this.sendJson(ws, {
        type: 'snapshot',
        status: this.status,
        messages: Array.from(this.retainedCache.entries()),
      });

      ws.on('message', (raw) => {
        this.handleClientMessage(ws, raw.toString());
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

  getStatus(): KetNoiStatus {
    return this.status;
  }

  handleUpgrade(req: IncomingMessage, socket: Duplex, head: Buffer): void {
    this.wss.handleUpgrade(req, socket, head, (ws) => {
      this.wss.emit('connection', ws, req);
    });
  }

  connect(cfg: MqttConfig): void {
    this.disconnect();
    this.retainedCache.clear();
    const gen = ++this.generation;

    if (!isConfigReady(cfg)) {
      this.setStatus({ state: 'chua_cau_hinh' });
      this.broadcast({ type: 'snapshot', status: this.status, messages: [] });
      return;
    }

    const protocol = cfg.tls ? 'mqtts' : 'mqtt';
    const url = `${protocol}://${cfg.host.trim()}:${cfg.cong}`;
    const clientId =
      cfg.clientId.trim() ||
      `web-smarthome-${Math.floor(100000 + Math.random() * 900000)}`;

    this.setStatus({ state: 'dang_noi' });

    const client = mqtt.connect(url, {
      clientId,
      clean: true,
      reconnectPeriod: 4000,
      connectTimeout: 15000,
      keepalive: 45,
      username: cfg.taiKhoan.trim() || undefined,
      password: cfg.matKhau || undefined,
    });

    this.client = client;

    client.on('connect', () => {
      if (this.generation !== gen) return;
      this.setStatus({ state: 'da_noi' });
      client.subscribe(SUB_TOPICS, { qos: 1 }, (err, granted) => {
        if (this.generation !== gen) return;
        if (err) {
          this.setStatus({
            state: 'loi',
            moTa: `Không đăng ký được topic: ${err.message}`,
          });
          return;
        }
        if (granted) {
          if (granted.length !== SUB_TOPICS.length) {
            this.setStatus({
              state: 'loi',
              moTa: `Broker trả về ${granted.length} kết quả cho ${SUB_TOPICS.length} topic. Không biết topic nào được cấp quyền.`,
            });
            return;
          }
          const rejected = granted.filter((g) => g.qos === 128).map((g) => g.topic);
          if (rejected.length > 0) {
            this.setStatus({
              state: 'loi',
              moTa: `Broker từ chối cho đọc: ${rejected.join(', ')}. Cần cấp quyền subscribe cho tài khoản này trên Mosquitto.`,
            });
          }
        }
      });
    });

    client.on('reconnect', () => {
      if (this.generation !== gen) return;
      this.setStatus({ state: 'dang_noi' });
    });

    client.on('offline', () => {
      if (this.generation !== gen) return;
      if (this.status.state === 'da_noi') {
        this.setStatus({
          state: 'loi',
          moTa: 'Mất kết nối broker. Đang tự động nối lại...',
        });
      }
    });

    client.on('error', (err) => {
      if (this.generation !== gen) return;
      this.setStatus({ state: 'loi', moTa: translateMqttError(err) });
    });

    client.on('message', (topic, buffer) => {
      if (this.generation !== gen) return;
      const payload = buffer.toString('utf8');
      if (topic.startsWith('cuktech/rssi/') && payload.trim() === '') {
        this.retainedCache.delete(topic);
      } else {
        this.retainedCache.set(topic, payload);
      }
      this.broadcast({
        type: 'message',
        topic,
        payload,
        ts: Date.now(),
      });
    });
  }

  disconnect(): void {
    this.generation++;
    const c = this.client;
    this.client = null;
    if (c) {
      try {
        c.removeAllListeners();
        c.end(true);
      } catch {
        // ignore close error
      }
    }
  }

  private setStatus(status: KetNoiStatus): void {
    this.status = status;
    this.broadcast({ type: 'status', status });
  }

  private handleClientMessage(ws: WebSocket, raw: string): void {
    let msg: { type?: string; reqId?: string; topic?: string; payload?: string };
    try {
      msg = JSON.parse(raw);
    } catch {
      return;
    }

    if (msg.type !== 'publish') return;
    const reqId = typeof msg.reqId === 'string' ? msg.reqId : '';
    const topic = typeof msg.topic === 'string' ? msg.topic : '';
    const payload = typeof msg.payload === 'string' ? msg.payload : '';

    if (!PUBLISH_ALLOWLIST.has(topic)) {
      this.sendJson(ws, {
        type: 'publish_ack',
        reqId,
        ok: false,
        reason: 'error',
        error: `Topic "${topic}" không nằm trong danh sách được phép ghi.`,
      });
      return;
    }

    if (!this.client || !this.client.connected) {
      this.sendJson(ws, {
        type: 'publish_ack',
        reqId,
        ok: false,
        reason: 'not_connected',
        error: 'Chưa nối broker MQTT',
      });
      return;
    }

    this.client.publish(topic, payload, { qos: 1, retain: false }, (err) => {
      if (err) {
        this.sendJson(ws, {
          type: 'publish_ack',
          reqId,
          ok: false,
          reason: 'error',
          error: err.message || 'không gửi được lệnh lên broker',
        });
      } else {
        this.sendJson(ws, { type: 'publish_ack', reqId, ok: true });
      }
    });
  }

  private sendJson(ws: WebSocket, data: unknown): void {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data));
    }
  }

  private broadcast(data: unknown): void {
    const serialized = JSON.stringify(data);
    for (const ws of this.clients) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(serialized);
      }
    }
  }
}

