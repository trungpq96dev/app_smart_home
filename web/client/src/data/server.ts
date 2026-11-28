export const ServerTopic = {
  GOC: 'server/#',
  INFO: 'server/info',
  METRICS: 'server/metrics',
  DOCKER: 'server/docker',
  NHIET_DO: 'server/nhietdo',
  DIEN_NANG: 'server/diennang',
  DIEN_NANG_NGAY: 'server/diennang_ngay',
  CAP_NHAT: 'server/capnhat',
} as const;

export type DockerContainerAction = 'start' | 'stop' | 'restart';

const CORE_PROTECTED_SUBSTRINGS = [
  'mosquitto',
  'server-collector',
  'server_collector',
  'smarthome-web',
  'smarthome_web',
];

export function isCoreProtectedContainer(name: string): boolean {
  const low = name.trim().replace(/^\/+/, '').toLowerCase();
  if (!low) return true;
  return CORE_PROTECTED_SUBSTRINGS.some((sub) => low.includes(sub));
}

export interface ServerInfo {
  hostname: string;
  os: string;
  kernel: string;
  cpuModel: string;
  cpuCores: number;
  ramTotalGb: number;
  diskTotalGb: number;
  uptimeDays: number;
  uptimeHours: number;
}

export interface ServerMetric {
  cpuPercent: number;
  ramUsedGb: number;
  ramPercent: number;
  diskUsedGb: number;
  diskPercent: number;
  cpuTempC: number | null;
  load1: number;
  load5: number;
  load15: number;
  netRxMbps: number;
  netTxMbps: number;
}

export type ServerContainerIconKey = 'hub' | 'bolt' | 'router' | 'videocam' | 'dns' | 'storage' | 'widgets';

export interface ServerContainer {
  name: string;
  image: string;
  state: string;
  status: string;
  icon: ServerContainerIconKey;
  cpuPercent: number | null;
  memMb: number | null;
  memLimitMb: number | null;
  memPercent: number | null;
  protected: boolean;
  controllable: boolean;
}

export interface ServerTemp {
  label: string;
  tempC: number;
}

export interface ServerPower {
  batVoltageV: number | null;
  batCurrentA: number | null;
  batPowerW: number | null;
  batStatus: string | null;
  cpuPackageW: number | null;
  cpuCoreW: number | null;
  cpuUncoreW: number | null;
}

export interface ServerDay {
  date: string;
  wh: number;
  tempMin: number | null;
  tempMax: number | null;
}

export interface ServerState {
  info: ServerInfo;
  metric: ServerMetric;
  containers: ServerContainer[];
  temps: ServerTemp[];
  tempMinToday: number | null;
  tempMaxToday: number | null;
  power: ServerPower;
  days: ServerDay[];
  capNhat: string | null;
  isRealData: boolean;
}

function optNum(o: Record<string, unknown>, key: string): number | null {
  if (!Object.prototype.hasOwnProperty.call(o, key) || o[key] == null) return null;
  const n = Number(o[key]);
  return Number.isFinite(n) ? n : null;
}

function numOr(o: Record<string, unknown>, key: string, fallback: number): number {
  const n = optNum(o, key);
  return n !== null ? n : fallback;
}

function doContainerIcon(name: string, image: string): ServerContainerIconKey {
  const s = `${name} ${image}`.toLowerCase();
  if (s.includes('mosquitto') || s.includes('mqtt')) return 'hub';
  if (s.includes('dien') || s.includes('evn')) return 'bolt';
  if (s.includes('wifi') || s.includes('router')) return 'router';
  if (s.includes('go2rtc') || s.includes('cam') || s.includes('rtsp')) return 'videocam';
  if (s.includes('cockpit') || s.includes('server') || s.includes('beszel')) return 'dns';
  if (s.includes('db') || s.includes('sql') || s.includes('postgres') || s.includes('mysql') || s.includes('redis')) {
    return 'storage';
  }
  return 'widgets';
}

export function parseServerContainers(arr: unknown): ServerContainer[] {
  if (!Array.isArray(arr)) return [];
  return arr
    .filter((c): c is Record<string, unknown> => Boolean(c && typeof c === 'object'))
    .map((c) => {
      const name = typeof c.name === 'string' ? c.name : 'container';
      const image = typeof c.image === 'string' ? c.image : '';
      const isProtected = Boolean(c.protected) || isCoreProtectedContainer(name);
      return {
        name,
        image,
        state: typeof c.state === 'string' ? c.state : 'running',
        status: typeof c.status === 'string' ? c.status : '',
        icon: doContainerIcon(name, image),
        cpuPercent: optNum(c, 'cpu_percent') ?? optNum(c, 'cpuPercent'),
        memMb: optNum(c, 'mem_mb') ?? optNum(c, 'memMb'),
        memLimitMb: optNum(c, 'mem_limit_mb') ?? optNum(c, 'memLimitMb'),
        memPercent: optNum(c, 'mem_percent') ?? optNum(c, 'memPercent'),
        protected: isProtected,
        controllable: !isProtected && Boolean(c.controllable),
      };
    });
}

export function gopServer(cur: ServerState, topic: string, payload: string): ServerState {
  if (!payload.trim()) return cur;
  try {
    switch (topic) {
      case ServerTopic.INFO: {
        const o = JSON.parse(payload) as Record<string, unknown>;
        return {
          ...cur,
          info: {
            hostname: typeof o.hostname === 'string' ? o.hostname : cur.info.hostname,
            os: typeof o.os === 'string' ? o.os : cur.info.os,
            kernel: typeof o.kernel === 'string' ? o.kernel : cur.info.kernel,
            cpuModel: typeof o.cpu_model === 'string' ? o.cpu_model : cur.info.cpuModel,
            cpuCores: Math.trunc(numOr(o, 'cpu_cores', cur.info.cpuCores)),
            ramTotalGb: numOr(o, 'ram_total_gb', cur.info.ramTotalGb),
            diskTotalGb: numOr(o, 'disk_total_gb', cur.info.diskTotalGb),
            uptimeDays: Math.trunc(numOr(o, 'uptime_days', cur.info.uptimeDays)),
            uptimeHours: Math.trunc(numOr(o, 'uptime_hours', cur.info.uptimeHours)),
          },
          isRealData: true,
        };
      }
      case ServerTopic.METRICS: {
        const o = JSON.parse(payload) as Record<string, unknown>;
        return {
          ...cur,
          metric: {
            cpuPercent: numOr(o, 'cpu_percent', cur.metric.cpuPercent),
            ramUsedGb: numOr(o, 'ram_used_gb', cur.metric.ramUsedGb),
            ramPercent: numOr(o, 'ram_percent', cur.metric.ramPercent),
            diskUsedGb: numOr(o, 'disk_used_gb', cur.metric.diskUsedGb),
            diskPercent: numOr(o, 'disk_percent', cur.metric.diskPercent),
            cpuTempC: optNum(o, 'cpu_temp_c'),
            load1: numOr(o, 'load1', cur.metric.load1),
            load5: numOr(o, 'load5', cur.metric.load5),
            load15: numOr(o, 'load15', cur.metric.load15),
            netRxMbps: numOr(o, 'net_rx_mbps', cur.metric.netRxMbps),
            netTxMbps: numOr(o, 'net_tx_mbps', cur.metric.netTxMbps),
          },
          isRealData: true,
        };
      }
      case ServerTopic.DOCKER: {
        const arr = JSON.parse(payload);
        if (!Array.isArray(arr)) return cur;
        return { ...cur, containers: parseServerContainers(arr), isRealData: true };
      }
      case ServerTopic.NHIET_DO: {
        const o = JSON.parse(payload) as Record<string, unknown>;
        const sensors = Array.isArray(o.sensors) ? o.sensors : [];
        const temps: ServerTemp[] = sensors
          .filter((s): s is Record<string, unknown> => Boolean(s && typeof s === 'object'))
          .map((s) => ({
            label: typeof s.label === 'string' ? s.label : 'cảm biến',
            tempC: Number(s.temp_c ?? 0) || 0,
          }));
        return {
          ...cur,
          temps,
          tempMinToday: optNum(o, 'today_min'),
          tempMaxToday: optNum(o, 'today_max'),
          isRealData: true,
        };
      }
      case ServerTopic.DIEN_NANG: {
        const o = JSON.parse(payload) as Record<string, unknown>;
        return {
          ...cur,
          power: {
            batVoltageV: optNum(o, 'bat_voltage_v'),
            batCurrentA: optNum(o, 'bat_current_a'),
            batPowerW: optNum(o, 'bat_power_w'),
            batStatus: typeof o.bat_status === 'string' ? o.bat_status : null,
            cpuPackageW: optNum(o, 'cpu_package_w'),
            cpuCoreW: optNum(o, 'cpu_core_w'),
            cpuUncoreW: optNum(o, 'cpu_uncore_w'),
          },
          isRealData: true,
        };
      }
      case ServerTopic.DIEN_NANG_NGAY: {
        const arr = JSON.parse(payload);
        if (!Array.isArray(arr)) return cur;
        const days: ServerDay[] = arr
          .filter((d): d is Record<string, unknown> => Boolean(d && typeof d === 'object'))
          .map((d) => ({
            date: typeof d.date === 'string' ? d.date : '',
            wh: Number(d.wh ?? 0) || 0,
            tempMin: optNum(d, 'temp_min'),
            tempMax: optNum(d, 'temp_max'),
          }));
        return { ...cur, days, isRealData: true };
      }
      case ServerTopic.CAP_NHAT:
        return { ...cur, capNhat: payload.trim(), isRealData: true };
      default:
        return cur;
    }
  } catch {
    return cur;
  }
}
