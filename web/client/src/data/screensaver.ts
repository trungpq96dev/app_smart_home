import {
  bomChinhDangChay,
  bomApDangKhoa,
  daCauHinhBom,
  dangKhoaBom,
  nhanCheDoBon,
  phanTichLoiBomNuoc,
  type BomNuocState,
  type PublicBomNuocConfig,
  type TrangThaiKetNoiBom,
} from './bomnuoc.js';
import type { KetNoi } from './dien.js';
import type { DestRoute } from './models.js';

export interface ScreensaverConfig {
  enabled: boolean;
  idleMinutes: number;
  nightDimEnabled: boolean;
  nightStartHour: number;
  nightEndHour: number;
  webDimOpacity: number;
  docChaoBat: boolean;
  docChaoGioNgu: boolean;
  pirWakeEnabled: boolean;
  pirTopic: string;
  gasTopic: string;
}

export interface SuCoKhanCap {
  key: string;
  kind: 'gas' | 'pump' | 'server';
  title: string;
  detail: string;
  dest: DestRoute;
}

const STORAGE_KEY = 'smarthome_screensaver_v1';

export function defaultScreensaverConfig(): ScreensaverConfig {
  return {
    enabled: true,
    idleMinutes: 3,
    nightDimEnabled: true,
    nightStartHour: 22,
    nightEndHour: 6,
    webDimOpacity: 0.4,
    docChaoBat: true,
    docChaoGioNgu: false,
    pirWakeEnabled: true,
    pirTopic: 'smarthome/sensor/pir',
    gasTopic: 'smarthome/sensor/gas',
  };
}

export function loadScreensaverConfig(): ScreensaverConfig {
  const def = defaultScreensaverConfig();
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return def;
    const p = JSON.parse(raw) as Partial<ScreensaverConfig>;
    return {
      enabled: typeof p.enabled === 'boolean' ? p.enabled : def.enabled,
      idleMinutes: typeof p.idleMinutes === 'number' && Number.isFinite(p.idleMinutes)
        ? Math.min(60, Math.max(1, Math.trunc(p.idleMinutes)))
        : def.idleMinutes,
      nightDimEnabled: typeof p.nightDimEnabled === 'boolean' ? p.nightDimEnabled : def.nightDimEnabled,
      nightStartHour: typeof p.nightStartHour === 'number' && Number.isFinite(p.nightStartHour)
        ? Math.min(23, Math.max(0, Math.trunc(p.nightStartHour)))
        : def.nightStartHour,
      nightEndHour: typeof p.nightEndHour === 'number' && Number.isFinite(p.nightEndHour)
        ? Math.min(23, Math.max(0, Math.trunc(p.nightEndHour)))
        : def.nightEndHour,
      webDimOpacity: typeof p.webDimOpacity === 'number' && Number.isFinite(p.webDimOpacity)
        ? Math.min(0.75, Math.max(0.1, p.webDimOpacity))
        : def.webDimOpacity,
      docChaoBat: typeof p.docChaoBat === 'boolean' ? p.docChaoBat : def.docChaoBat,
      docChaoGioNgu: typeof p.docChaoGioNgu === 'boolean' ? p.docChaoGioNgu : def.docChaoGioNgu,
      pirWakeEnabled: typeof p.pirWakeEnabled === 'boolean' ? p.pirWakeEnabled : def.pirWakeEnabled,
      pirTopic: typeof p.pirTopic === 'string' && p.pirTopic.trim() ? p.pirTopic.trim() : def.pirTopic,
      gasTopic: typeof p.gasTopic === 'string' && p.gasTopic.trim() ? p.gasTopic.trim() : def.gasTopic,
    };
  } catch {
    return def;
  }
}

export function saveScreensaverConfig(cfg: ScreensaverConfig): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg));
  } catch {
    // ignore storage quota errors
  }
}

export function laKhungGioDem(hour: number, startHour: number, endHour: number): boolean {
  const h = ((Math.trunc(hour) % 24) + 24) % 24;
  const s = ((Math.trunc(startHour) % 24) + 24) % 24;
  const e = ((Math.trunc(endHour) % 24) + 24) % 24;
  if (s === e) return true;
  if (s > e) return h >= s || h < e;
  return h >= s && h < e;
}

export function parsePirMotionPayload(payload: string): boolean {
  const s = payload.trim();
  if (!s) return false;
  const lower = s.toLowerCase();
  if (
    lower === '1' ||
    lower === 'true' ||
    lower === 'on' ||
    lower === 'motion' ||
    lower === 'detected' ||
    lower === 'occupied'
  ) {
    return true;
  }
  if (lower === '0' || lower === 'false' || lower === 'off' || lower === 'clear' || lower === 'none') {
    return false;
  }
  try {
    const obj = JSON.parse(s) as Record<string, unknown>;
    if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
      return (
        obj.motion === true ||
        obj.detected === true ||
        obj.occupancy === true ||
        obj.pir === true ||
        obj.pir === 1 ||
        obj.state === 'motion' ||
        obj.state === 'ON'
      );
    }
  } catch {
    // ignore non-json
  }
  return false;
}

export function parseGasSensorPayload(
  payload: string,
  thresholdPpm: number = 200,
): { alert: boolean; ppm: number | null } {
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
      return { alert: flag || (ppm !== null && ppm >= thresholdPpm), ppm };
    }
  } catch {
    // ignore
  }
  return { alert: false, ppm: null };
}

export function kiemTraSuCoKhanCap(params: {
  kitchenGasAlert: boolean;
  mqttGasAlert: boolean;
  mqttGasPpm: number | null;
  bomNuoc: BomNuocState | null;
  ketNoiBom: TrangThaiKetNoiBom;
  mqttStatus: KetNoi;
  gatewayOnline: boolean;
}): SuCoKhanCap | null {
  const { kitchenGasAlert, mqttGasAlert, mqttGasPpm, bomNuoc, ketNoiBom, mqttStatus, gatewayOnline } = params;

  if (mqttGasAlert || kitchenGasAlert) {
    const ppmInfo = mqttGasPpm !== null ? ` (${Math.round(mqttGasPpm)} ppm)` : '';
    return {
      key: `gas:${mqttGasPpm ?? (kitchenGasAlert ? 'kitchen' : 'mqtt')}`,
      kind: 'gas',
      title: 'Cảnh báo rò rỉ Gas!',
      detail: `Phát hiện khí gas vượt ngưỡng an toàn${ppmInfo}. Mở cửa thông gió và kiểm tra van gas ngay.`,
      dest: 'sensors',
    };
  }

  if (bomNuoc !== null) {
    const fault = phanTichLoiBomNuoc(bomNuoc.fault);
    if (dangKhoaBom(bomNuoc) || bomApDangKhoa(bomNuoc) || fault?.mucDo === 'NGUY_HIEM') {
      return {
        key: `pump:${bomNuoc.fault ?? 'locked'}:${bomNuoc.locked}:${bomNuoc.bstLock}`,
        kind: 'pump',
        title: 'Bo bơm nước đang khóa bảo vệ!',
        detail: fault?.noiDung ?? 'Hệ thống bơm vừa gặp sự cố chạy cạn hoặc quá nhiệt và đã tự khóa.',
        dest: 'automation',
      };
    }
  }

  if (ketNoiBom.state === 'bo_offline') {
    return {
      key: 'pump:offline',
      kind: 'pump',
      title: 'Bo bơm nước mất kết nối!',
      detail: 'Gateway không liên lạc được với bo bơm ESP32.',
      dest: 'automation',
    };
  }

  if (!gatewayOnline) {
    return {
      key: 'server:gateway_offline',
      kind: 'server',
      title: 'Mất kết nối Node.js Gateway!',
      detail: 'Trình duyệt không liên lạc được với máy chủ Gateway.',
      dest: 'server',
    };
  }

  if (mqttStatus.state === 'loi') {
    return {
      key: `server:mqtt_loi:${mqttStatus.moTa}`,
      kind: 'server',
      title: 'Máy chủ MQTT ngoại tuyến!',
      detail: mqttStatus.moTa,
      dest: 'server',
    };
  }

  return null;
}

export function nhanBomChoManCho(
  bomNuoc: BomNuocState | null,
  ketNoiBom: TrangThaiKetNoiBom,
  bomConfig: PublicBomNuocConfig,
): string {
  if (!daCauHinhBom(bomConfig) && bomNuoc === null) return 'Chưa cấu hình';
  if (bomNuoc === null) {
    if (ketNoiBom.state === 'dang_noi') return 'Đang kết nối';
    return 'Chờ số liệu';
  }
  if (dangKhoaBom(bomNuoc) || bomApDangKhoa(bomNuoc)) return 'Đang khóa';
  if (bomChinhDangChay(bomNuoc)) return 'Đang bơm';
  const mode = nhanCheDoBon(bomNuoc.tankMode);
  return mode.charAt(0).toUpperCase() + mode.slice(1);
}

