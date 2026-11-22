export const CuktechTopic = {
  GOC: 'cuktech/#',
  STATUS: 'cuktech/charger/status',
  SETTINGS: 'cuktech/charger/settings',
  TIEN_TO_CONG: 'cuktech/charger/port/',
  CMD_CONG: 'cuktech/charger/port',
  CMD_BLE: 'cuktech/charger/ble',
  CMD_SET: 'cuktech/charger/set',
  TIEN_TO_RSSI: 'cuktech/rssi/',
} as const;

export type CongSacId = 'C1' | 'C2' | 'C3' | 'USB_A';

export interface CongSacMeta {
  id: CongSacId;
  khoaStatus: string;
  khoaTopic: string;
  nhan: string;
  viTriBit: number;
}

export const CONG_SAC_LIST: CongSacMeta[] = [
  { id: 'C1', khoaStatus: '1', khoaTopic: 'c1', nhan: 'C1', viTriBit: 0 },
  { id: 'C2', khoaStatus: '2', khoaTopic: 'c2', nhan: 'C2', viTriBit: 1 },
  { id: 'C3', khoaStatus: '3', khoaTopic: 'c3', nhan: 'C3', viTriBit: 2 },
  { id: 'USB_A', khoaStatus: '4', khoaTopic: 'a', nhan: 'USB-A', viTriBit: 3 },
];

export function congSacTuTopic(duoi: string): CongSacMeta | undefined {
  const lower = duoi.toLowerCase();
  return CONG_SAC_LIST.find((c) => c.khoaTopic === lower);
}

export function congSacTuStatus(khoa: string): CongSacMeta | undefined {
  return CONG_SAC_LIST.find((c) => c.khoaStatus === khoa);
}

export function congSacMeta(id: CongSacId): CongSacMeta {
  return CONG_SAC_LIST.find((c) => c.id === id)!;
}

export const CuktechPiid = {
  KICH_BAN: 5,
  SANG_MAN: 6,
  SAC_NHO_GIOT_A: 15,
  TU_TAT_MAN: 19,
  KHOA_XOAY: 20,

  henGio(cong: CongSacId): number {
    switch (cong) {
      case 'C1': return 9;
      case 'C2': return 10;
      case 'C3': return 11;
      case 'USB_A': return 12;
    }
  },

  SANG_MAN_CHON: [
    [5, '1 phút'],
    [1, '5 phút'],
    [2, '10 phút'],
    [3, '30 phút'],
    [4, 'Luôn sáng'],
  ] as Array<[number, string]>,

  HEN_GIO_CHON: [
    [0, 'Tắt hẹn'],
    [15, '15 phút'],
    [30, '30 phút'],
    [60, '1 giờ'],
    [120, '2 giờ'],
  ] as Array<[number, string]>,
};

export interface KichBanInfo {
  ma: number;
  nhan: string;
}

export const KICH_BAN_LIST: KichBanInfo[] = [
  { ma: 1, nhan: 'AI' },
  { ma: 2, nhan: 'Kết hợp' },
  { ma: 3, nhan: 'Đơn' },
  { ma: 4, nhan: 'Cân bằng' },
];

export function kichBanTuMa(ma: number): KichBanInfo | null {
  return KICH_BAN_LIST.find((k) => k.ma === ma) ?? null;
}

export interface CuktechCaiDat {
  kichBan: KichBanInfo | null;
  thoiGianSangMan: string | null;
  tuTatManNhanRoi: boolean | null;
  khoaXoayMan: boolean | null;
  sacNhoGiotUsbA: boolean | null;
  congBat: Partial<Record<CongSacId, boolean>>;
  henGioPhut: Partial<Record<CongSacId, number>>;
  tho: Record<number, number>;
}

const MAN_SANG_MAP = new Map<number, string>(CuktechPiid.SANG_MAN_CHON);
const PIID_HEN_GIO: Array<[number, CongSacId]> = [
  [9, 'C1'],
  [10, 'C2'],
  [11, 'C3'],
  [12, 'USB_A'],
];

export function docCuktechCaiDat(o: Record<string, unknown>): CuktechCaiDat {
  const so = (piid: number): number | null => {
    const key = String(piid);
    if (!Object.prototype.hasOwnProperty.call(o, key) || o[key] == null) return null;
    const v = Number(o[key]);
    return Number.isFinite(v) ? Math.trunc(v) : null;
  };
  const co = (piid: number): boolean | null => {
    const v = so(piid);
    return v === null ? null : v !== 0;
  };

  const mask = so(16);
  const congBat: Partial<Record<CongSacId, boolean>> = {};
  if (mask !== null) {
    for (const c of CONG_SAC_LIST) {
      congBat[c.id] = ((mask >> c.viTriBit) & 1) === 1;
    }
  }

  const henGioPhut: Partial<Record<CongSacId, number>> = {};
  for (const [piid, cong] of PIID_HEN_GIO) {
    const v = so(piid);
    if (v !== null) henGioPhut[cong] = v;
  }

  const tho: Record<number, number> = {};
  for (const [k, raw] of Object.entries(o)) {
    if (!/^\d+$/.test(k) || raw == null) continue;
    const v = Number(raw);
    if (Number.isFinite(v)) tho[Number(k)] = Math.trunc(v);
  }

  const kbMa = so(5);
  const smMa = so(6);

  return {
    kichBan: kbMa !== null ? kichBanTuMa(kbMa) : null,
    thoiGianSangMan: smMa !== null ? (MAN_SANG_MAP.get(smMa) ?? null) : null,
    tuTatManNhanRoi: co(19),
    khoaXoayMan: co(20),
    sacNhoGiotUsbA: co(15),
    congBat,
    henGioPhut,
    tho,
  };
}

export interface CongSac {
  id: CongSacId;
  volt: number;
  ampe: number;
  oat: number;
  dangChay: boolean;
  giaoThuc: string;
  choPhep: boolean | null;
}

export function createEmptyCongSac(id: CongSacId): CongSac {
  return {
    id,
    volt: 0,
    ampe: 0,
    oat: 0,
    dangChay: false,
    giaoThuc: 'idle',
    choPhep: null,
  };
}

export function nhanGiaoThucCong(c: CongSac): string {
  return c.dangChay ? c.giaoThuc : 'Trống';
}

export type MucSongBle = 'TOT' | 'KHA' | 'YEU' | 'RAT_YEU' | 'KHONG_RO';

export const MUC_SONG_NHAN: Record<MucSongBle, string> = {
  TOT: 'Tốt',
  KHA: 'Khá',
  YEU: 'Yếu',
  RAT_YEU: 'Rất yếu',
  KHONG_RO: 'Không bắt được',
};

export interface SongBle {
  nguonId: string;
  rssi: number | null;
  nguonTen: string | null;
  tuoiGiay: number | null;
  dangNoi: boolean | null;
}

export function mucSongCua(s: SongBle): MucSongBle {
  if (s.rssi === null) return 'KHONG_RO';
  if (s.rssi >= -65) return 'TOT';
  if (s.rssi >= -80) return 'KHA';
  if (s.rssi >= -90) return 'YEU';
  return 'RAT_YEU';
}

export function nhanDbmSong(s: SongBle): string {
  return s.rssi !== null ? `${s.rssi} dBm` : '—';
}

export function songLaSoCu(s: SongBle): boolean {
  return (s.tuoiGiay ?? Number.MAX_SAFE_INTEGER) > 300;
}

export function docSongBle(nguonId: string, o: Record<string, unknown>): SongBle {
  const rssi = o.rssi != null && Number.isFinite(Number(o.rssi)) ? Math.trunc(Number(o.rssi)) : null;
  const nguonTen = typeof o.nguon === 'string' && o.nguon.trim() ? o.nguon.trim() : null;
  const tuoiGiay = o.tuoi_giay != null && Number.isFinite(Number(o.tuoi_giay))
    ? Math.trunc(Number(o.tuoi_giay))
    : null;
  const dangNoi = typeof o.dang_noi === 'boolean' ? o.dang_noi : null;
  return { nguonId, rssi, nguonTen, tuoiGiay, dangNoi };
}

export interface CuktechState {
  coSoThat: boolean;
  noiDuoc: boolean;
  xacThuc: boolean;
  model: string | null;
  firmware: string | null;
  cong: Partial<Record<CongSacId, CongSac>>;
  caiDat: CuktechCaiDat | null;
  song: Record<string, SongBle>;
  mocNhanMs: number;
}

export const CUKTECH_GIAY_COI_LA_CU = 60;

export function createEmptyCuktechState(): CuktechState {
  return {
    coSoThat: false,
    noiDuoc: false,
    xacThuc: false,
    model: null,
    firmware: null,
    cong: {},
    caiDat: null,
    song: {},
    mocNhanMs: 0,
  };
}

export function cuktechTongOat(ct: CuktechState): number {
  return Object.values(ct.cong).reduce((sum, c) => sum + (c?.oat ?? 0), 0);
}

export function cuktechSoCongDangChay(ct: CuktechState): number {
  return Object.values(ct.cong).filter((c) => c?.dangChay).length;
}

export function cuktechTyLePhanBo(ct: CuktechState): Partial<Record<CongSacId, number>> {
  const tong = cuktechTongOat(ct);
  if (tong <= 0) return {};
  const out: Partial<Record<CongSacId, number>> = {};
  for (const [id, c] of Object.entries(ct.cong) as Array<[CongSacId, CongSac]>) {
    if (c && c.oat > 0) {
      out[id] = (c.oat / tong) * 100;
    }
  }
  return out;
}

export function cuktechSongTotNhat(ct: CuktechState): SongBle | null {
  let best: SongBle | null = null;
  for (const s of Object.values(ct.song)) {
    if (s.rssi === null) continue;
    if (!best || (s.rssi > (best.rssi ?? -9999))) {
      best = s;
    }
  }
  return best;
}

export function cuktechCongBiTat(ct: CuktechState, id: CongSacId): boolean | null {
  const bat = ct.caiDat?.congBat[id];
  return typeof bat === 'boolean' ? !bat : null;
}

export function cuktechLaSoCu(ct: CuktechState, bayGioMs: number = Date.now()): boolean {
  return ct.mocNhanMs > 0 && bayGioMs - ct.mocNhanMs > CUKTECH_GIAY_COI_LA_CU * 1000;
}

export function cuktechMatKetNoiThat(ct: CuktechState, bayGioMs: number = Date.now()): boolean {
  return !ct.noiDuoc && cuktechLaSoCu(ct, bayGioMs);
}

export function cuktechCoMauThuan(ct: CuktechState, bayGioMs: number = Date.now()): boolean {
  return !ct.noiDuoc && !cuktechLaSoCu(ct, bayGioMs);
}

export function cuktechDanhSachCong(ct: CuktechState): CongSac[] {
  return CONG_SAC_LIST.map((m) => ct.cong[m.id] ?? createEmptyCongSac(m.id));
}

function docCong(id: CongSacId, o: Record<string, unknown>): CongSac {
  return {
    id,
    volt: typeof o.voltage === 'number' ? o.voltage : Number(o.voltage ?? 0) || 0,
    ampe: typeof o.current === 'number' ? o.current : Number(o.current ?? 0) || 0,
    oat: typeof o.power === 'number' ? o.power : Number(o.power ?? 0) || 0,
    dangChay: o.active === true,
    giaoThuc: typeof o.protocol === 'string' && o.protocol.trim() ? o.protocol : 'idle',
    choPhep: typeof o.enabled === 'boolean' ? o.enabled : null,
  };
}

function docStatus(cur: CuktechState, o: Record<string, unknown>): CuktechState {
  const ports = o.ports && typeof o.ports === 'object' ? (o.ports as Record<string, unknown>) : null;
  const cong: Partial<Record<CongSacId, CongSac>> = { ...cur.cong };
  if (ports) {
    for (const [khoa, val] of Object.entries(ports)) {
      const meta = congSacTuStatus(khoa);
      if (!meta || !val || typeof val !== 'object') continue;
      cong[meta.id] = docCong(meta.id, val as Record<string, unknown>);
    }
  }
  const settingsRaw = o.settings && typeof o.settings === 'object' ? (o.settings as Record<string, unknown>) : null;
  const caiDat = settingsRaw ? docCuktechCaiDat(settingsRaw) : cur.caiDat;

  return {
    ...cur,
    coSoThat: true,
    noiDuoc: typeof o.connected === 'boolean' ? o.connected : cur.noiDuoc,
    xacThuc: typeof o.authenticated === 'boolean' ? o.authenticated : cur.xacThuc,
    model: typeof o.device_model === 'string' && o.device_model.trim() ? o.device_model : cur.model,
    firmware: typeof o.firmware_version === 'string' && o.firmware_version.trim()
      ? o.firmware_version
      : cur.firmware,
    cong,
    caiDat,
  };
}

export function gopCuktech(
  cur: CuktechState,
  topic: string,
  payload: string,
  bayGioMs: number = Date.now(),
): CuktechState {
  if (topic.startsWith(CuktechTopic.TIEN_TO_RSSI)) {
    const nguon = topic.slice(CuktechTopic.TIEN_TO_RSSI.length);
    if (!nguon.trim() || nguon.includes('/')) return cur;
    if (!payload.trim()) {
      const nextSong = { ...cur.song };
      delete nextSong[nguon];
      return { ...cur, song: nextSong };
    }
    try {
      const obj = JSON.parse(payload) as Record<string, unknown>;
      return {
        ...cur,
        song: {
          ...cur.song,
          [nguon]: docSongBle(nguon, obj),
        },
      };
    } catch {
      return cur;
    }
  }

  if (!payload.trim()) return cur;
  try {
    if (topic === CuktechTopic.STATUS) {
      return {
        ...docStatus(cur, JSON.parse(payload) as Record<string, unknown>),
        mocNhanMs: bayGioMs,
      };
    }
    if (topic.startsWith(CuktechTopic.TIEN_TO_CONG)) {
      const meta = congSacTuTopic(topic.slice(CuktechTopic.TIEN_TO_CONG.length));
      if (!meta) return cur;
      return {
        ...cur,
        coSoThat: true,
        mocNhanMs: bayGioMs,
        cong: {
          ...cur.cong,
          [meta.id]: docCong(meta.id, JSON.parse(payload) as Record<string, unknown>),
        },
      };
    }
    if (topic === CuktechTopic.SETTINGS) {
      return {
        ...cur,
        coSoThat: true,
        mocNhanMs: bayGioMs,
        caiDat: docCuktechCaiDat(JSON.parse(payload) as Record<string, unknown>),
      };
    }
    return cur;
  } catch {
    return cur;
  }
}

