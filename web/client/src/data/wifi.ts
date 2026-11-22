export const WifiTopic = {
  GOC: 'wifi/#',
  INFO: 'wifi/info',
  DEVICES: 'wifi/devices',
  USAGE: 'wifi/usage',
  TRANG_THAI: 'wifi/trangthai',
  CAP_NHAT: 'wifi/capnhat',
  CMD: 'wifi/cmd',
} as const;

export interface WifiInfo {
  ssid: string;
  security: string;
  signalDbm: number;
  signalLabel: string;
  downloadMbps: number;
  uploadMbps: number;
  speedMbps: number;
  ipRouter: string;
  dns: string;
  gateway: string;
  isConnected: boolean;
  uptimeDays: number;
  uptimeHours: number;
}

export type WifiDeviceIconKey =
  | 'phone'
  | 'laptop'
  | 'tv'
  | 'videocam'
  | 'speaker'
  | 'lightbulb'
  | 'tablet'
  | 'devices';

export interface WifiDevice {
  id: string;
  name: string;
  icon: WifiDeviceIconKey;
  ip: string;
  isOnline: boolean;
  downloadMB: number;
  uploadMB: number;
  mac: string;
  brand: string;
  type: string;
  band: string;
  isNew: boolean;
  trusted: boolean;
  blocked: boolean;
  signal: string;
}

export interface UnknownDevice {
  id: string;
  name: string;
  ip: string;
  mac: string;
  time: string;
  reason: string;
}

export interface ConnectionLog {
  time: string;
  date: string;
  device: string;
  action: string;
  type: 'success' | 'warning' | 'danger' | 'info';
}

export interface SecuritySetting {
  key: string;
  title: string;
  desc: string;
  enabled: boolean;
}

export interface StatsDataset {
  title: string;
  downLabel: string;
  downVal: string;
  downSub: string;
  upLabel: string;
  upVal: string;
  upSub: string;
  totalLabel: string;
  totalVal: string;
  totalSub: string;
  peakVal: string;
  peakSub: string;
  labels: string[];
  down: number[];
  up: number[];
  categories: number[];
}

export interface ChannelNoise {
  channel: number;
  noise: number;
}

export interface WifiState {
  info: WifiInfo;
  devices: WifiDevice[];
  usageChart24h: number[];
  trangThai: string;
  capNhat: string | null;
  isRealData: boolean;
  unknownDevices: UnknownDevice[];
  connectionLogs: ConnectionLog[];
  securitySettings: SecuritySetting[];
  statsDay: StatsDataset;
  statsWeek: StatsDataset;
  statsMonth: StatsDataset;
  channelNoise: ChannelNoise[];
}

function doWifiIcon(name: string): WifiDeviceIconKey {
  const lower = name.toLowerCase();
  if (lower.includes('phone') || lower.includes('iphone') || lower.includes('samsung') || lower.includes('xiaomi') || lower.includes('oppo')) {
    return 'phone';
  }
  if (lower.includes('laptop') || lower.includes('pc') || lower.includes('desktop') || lower.includes('macbook') || lower.includes('thinkpad')) {
    return 'laptop';
  }
  if (lower.includes('tv') || lower.includes('tivi') || lower.includes('smart tv')) return 'tv';
  if (lower.includes('cam') || lower.includes('camera')) return 'videocam';
  if (lower.includes('loa') || lower.includes('speaker') || lower.includes('echo') || lower.includes('nest')) return 'speaker';
  if (lower.includes('đèn') || lower.includes('den') || lower.includes('light')) return 'lightbulb';
  if (lower.includes('tab') || lower.includes('ipad')) return 'tablet';
  return 'devices';
}

function numOr(o: Record<string, unknown>, key: string, fallback: number): number {
  if (!Object.prototype.hasOwnProperty.call(o, key) || o[key] == null) return fallback;
  const n = Number(o[key]);
  return Number.isFinite(n) ? n : fallback;
}

export function gopWifi(cur: WifiState, topic: string, payload: string): WifiState {
  if (!payload.trim()) return cur;
  try {
    switch (topic) {
      case WifiTopic.INFO: {
        const obj = JSON.parse(payload) as Record<string, unknown>;
        return {
          ...cur,
          info: {
            ssid: typeof obj.ssid === 'string' ? obj.ssid : cur.info.ssid,
            security: typeof obj.security === 'string' ? obj.security : cur.info.security,
            signalDbm: Math.trunc(numOr(obj, 'signal_dbm', cur.info.signalDbm)),
            signalLabel: typeof obj.signal_label === 'string' ? obj.signal_label : cur.info.signalLabel,
            downloadMbps: numOr(obj, 'download_mbps', cur.info.downloadMbps),
            uploadMbps: numOr(obj, 'upload_mbps', cur.info.uploadMbps),
            speedMbps: Math.trunc(numOr(obj, 'speed_mbps', cur.info.speedMbps)),
            ipRouter: typeof obj.router_ip === 'string' ? obj.router_ip : cur.info.ipRouter,
            dns: typeof obj.dns === 'string' ? obj.dns : cur.info.dns,
            gateway: typeof obj.gateway === 'string' ? obj.gateway : cur.info.gateway,
            isConnected: typeof obj.is_connected === 'boolean' ? obj.is_connected : cur.info.isConnected,
            uptimeDays: Math.trunc(numOr(obj, 'uptime_days', cur.info.uptimeDays)),
            uptimeHours: Math.trunc(numOr(obj, 'uptime_hours', cur.info.uptimeHours)),
          },
          isRealData: true,
        };
      }
      case WifiTopic.DEVICES: {
        const arr = JSON.parse(payload);
        if (!Array.isArray(arr)) return cur;
        const list: WifiDevice[] = arr
          .filter((o): o is Record<string, unknown> => Boolean(o && typeof o === 'object'))
          .map((o, i) => {
            const name = typeof o.name === 'string' ? o.name : 'Thiết bị';
            return {
              id: typeof o.id === 'string' ? o.id : String(i),
              name,
              icon: doWifiIcon(name),
              ip: typeof o.ip === 'string' ? o.ip : '',
              mac: typeof o.mac === 'string' ? o.mac : '',
              brand: typeof o.brand === 'string' ? o.brand : '',
              type: typeof o.type === 'string' ? o.type : '',
              band: typeof o.band === 'string' ? o.band : '5GHz',
              isOnline: typeof o.online === 'boolean' ? o.online : true,
              isNew: o.is_new === true,
              trusted: o.trusted === true,
              blocked: o.blocked === true,
              signal: typeof o.signal === 'string' ? o.signal : '',
              downloadMB: Number(o.download_mb ?? 0) || 0,
              uploadMB: Number(o.upload_mb ?? 0) || 0,
            };
          });
        return { ...cur, devices: list, isRealData: true };
      }
      case WifiTopic.USAGE: {
        const arr = JSON.parse(payload);
        if (!Array.isArray(arr)) return cur;
        const floats = arr.map((v) => (Number.isFinite(Number(v)) ? Number(v) : 0));
        return { ...cur, usageChart24h: floats, isRealData: true };
      }
      case WifiTopic.TRANG_THAI:
        return { ...cur, trangThai: payload.trim() };
      case WifiTopic.CAP_NHAT:
        return { ...cur, capNhat: payload.trim(), isRealData: true };
      default:
        return cur;
    }
  } catch {
    return cur;
  }
}

