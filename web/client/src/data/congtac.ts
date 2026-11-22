export const CongTacTopic = {
  GOC: 'congtac/#',
  INFO: 'congtac/info',
  TRANG_THAI: 'congtac/trangthai',
  TIEN_TO_TB: 'congtac/thietbi/',
  CAP_NHAT: 'congtac/capnhat',
  KET_QUA: 'congtac/cmd/result',
} as const;

export const DieuKhienTopic = {
  CMD: 'smarthome/control',
} as const;

export interface CongTac {
  ma: string;
  phong: string;
  thietBi: string;
  ten: string;
  he: string;
}

export interface TrangThaiCongTac {
  ma: string;
  online: boolean;
  bat: boolean | null;
  luc: string | null;
  traLoiId: string | null;
  lyDoLoi: string | null;
}

export interface CongTacState {
  danhSach: CongTac[];
  trangThai: Record<string, TrangThaiCongTac>;
  bridge: string | null;
  capNhat: string | null;
}

export function createEmptyCongTacState(): CongTacState {
  return {
    danhSach: [],
    trangThai: {},
    bridge: null,
    capNhat: null,
  };
}

export function isBridgeSong(ct: CongTacState): boolean {
  return ct.bridge === 'online';
}

export function timCongTac(ct: CongTacState, phong: string, thietBi: string): CongTac | undefined {
  return ct.danhSach.find((tb) => tb.phong === phong && tb.thietBi === thietBi);
}

export function laCongTacThat(ct: CongTacState, phong: string, thietBi: string): boolean {
  return timCongTac(ct, phong, thietBi) !== undefined;
}

export function trangThaiCongTacCua(
  ct: CongTacState,
  phong: string,
  thietBi: string,
): TrangThaiCongTac | undefined {
  const tb = timCongTac(ct, phong, thietBi);
  return tb ? ct.trangThai[tb.ma] : undefined;
}

export function tenThietBiThat(ct: CongTacState): string[] {
  return ct.danhSach.map((tb) => tb.ten);
}

export function docDanhSachCongTac(payload: string): CongTac[] {
  const parsed = JSON.parse(payload) as { thiet_bi?: unknown };
  const arr = Array.isArray(parsed?.thiet_bi) ? parsed.thiet_bi : [];
  const ds: CongTac[] = [];
  for (const item of arr) {
    if (!item || typeof item !== 'object') continue;
    const o = item as Record<string, unknown>;
    const ma = typeof o.id === 'string' ? o.id.trim() : '';
    const phong = typeof o.phong === 'string' ? o.phong.trim() : '';
    const thietBi = typeof o.loai === 'string' ? o.loai.trim() : '';
    if (!ma || !phong || !thietBi) continue;
    const tenRaw = typeof o.ten === 'string' ? o.ten.trim() : '';
    const heRaw = typeof o.driver === 'string' ? o.driver.trim() : '';
    ds.push({
      ma,
      phong,
      thietBi,
      ten: tenRaw || ma,
      he: heRaw || '?',
    });
  }
  return ds;
}

export function docTrangThaiCongTac(ma: string, payload: string): TrangThaiCongTac {
  const o = JSON.parse(payload) as Record<string, unknown>;
  const online = o.online === true;
  const bat = online && typeof o.is_on === 'boolean' ? o.is_on : null;
  const luc = typeof o.doc_luc === 'string' && o.doc_luc.trim() ? o.doc_luc : null;
  const traLoiId = typeof o.tra_loi_id === 'string' && o.tra_loi_id.trim() ? o.tra_loi_id : null;
  const lyDoLoi = typeof o.ly_do_loi === 'string' && o.ly_do_loi.trim() ? o.ly_do_loi : null;
  return { ma, online, bat, luc, traLoiId, lyDoLoi };
}

export function gopCongTac(cur: CongTacState, topic: string, payload: string): CongTacState {
  if (!payload.trim()) return cur;
  try {
    if (topic === CongTacTopic.TRANG_THAI) {
      return { ...cur, bridge: payload.trim() };
    }
    if (topic === CongTacTopic.CAP_NHAT) {
      return { ...cur, capNhat: payload.trim() };
    }
    if (topic === CongTacTopic.INFO) {
      return { ...cur, danhSach: docDanhSachCongTac(payload) };
    }
    if (topic.startsWith(CongTacTopic.TIEN_TO_TB)) {
      const ma = topic.slice(CongTacTopic.TIEN_TO_TB.length);
      if (!ma.trim()) return cur;
      return {
        ...cur,
        trangThai: {
          ...cur.trangThai,
          [ma]: docTrangThaiCongTac(ma, payload),
        },
      };
    }
    return cur;
  } catch {
    return cur;
  }
}

export const LenhCongTac = {
  NGUON_MAN_HINH: 'man_hinh',
  NGUON_GIONG_NOI: 'voice',

  maLenh(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID().slice(0, 8);
    }
    return Math.random().toString(16).slice(2, 10).padEnd(8, '0');
  },

  json(
    phong: string,
    thietBi: string,
    hanhDong: string,
    giaTri: string | null,
    nguon: string,
    id: string,
  ): string {
    const obj: Record<string, string> = {
      device: phong === 'nhanh' ? `nhanh_${thietBi}` : `${phong}_${thietBi}`,
      room: phong,
      action: hanhDong.toUpperCase(),
      source: nguon,
      id,
    };
    if (giaTri !== null) obj.value = giaTri;
    return JSON.stringify(obj);
  },
};

export type OThietBi =
  | { kind: 'mau'; bat: boolean }
  | {
      kind: 'that';
      ten: string;
      bridgeSong: boolean;
      online: boolean;
      bat: boolean | null;
      dangCho: boolean;
      mong: boolean | null;
      lyDoLoi: string | null;
      gatDuoc: boolean;
      ghiChu: string | null;
    };

export function buildOThietBiThat(params: {
  ten: string;
  bridgeSong: boolean;
  online: boolean;
  bat: boolean | null;
  dangCho: boolean;
  mong: boolean | null;
  lyDoLoi: string | null;
}): OThietBi {
  const { ten, bridgeSong, online, bat, dangCho, mong, lyDoLoi } = params;
  const gatDuoc = bridgeSong && online && bat !== null && !dangCho;
  let ghiChu: string | null = null;
  if (dangCho) ghiChu = 'Đang gửi lệnh…';
  else if (!bridgeSong) ghiChu = 'Bộ nối chưa chạy trên máy chủ';
  else if (!online) ghiChu = lyDoLoi || 'Mất kết nối';
  else if (bat === null) ghiChu = lyDoLoi || 'Chưa đọc được công tắc';
  return {
    kind: 'that',
    ten,
    bridgeSong,
    online,
    bat,
    dangCho,
    mong,
    lyDoLoi,
    gatDuoc,
    ghiChu,
  };
}

export interface ChoCongTac {
  mong: boolean;
  id: string;
  mocMs: number;
}

export class HangChoCongTac {
  static readonly GIAY_CHO_MAC_DINH = 6;
  private ds = new Map<string, ChoCongTac>();
  private readonly giayCho: number;

  constructor(giayCho = HangChoCongTac.GIAY_CHO_MAC_DINH) {
    this.giayCho = giayCho;
  }

  cua(ma: string): ChoCongTac | undefined {
    return this.ds.get(ma);
  }

  dangCho(ma: string): boolean {
    return this.ds.has(ma);
  }

  ganh(ma: string, mong: boolean, id: string, bayGioMs: number): void {
    this.ds.set(ma, { mong, id, mocMs: bayGioMs });
  }

  go(ma: string): void {
    this.ds.delete(ma);
  }

  anhChup(): Record<string, boolean> {
    const out: Record<string, boolean> = {};
    for (const [k, v] of this.ds.entries()) {
      out[k] = v.mong;
    }
    return out;
  }

  nhanTrangThai(ma: string, traLoiId: string | null): boolean {
    const cho = this.ds.get(ma);
    if (!cho) return false;
    if (!traLoiId || traLoiId !== cho.id) return false;
    this.ds.delete(ma);
    return true;
  }

  quaHan(bayGioMs: number): string[] {
    const het: string[] = [];
    for (const [k, v] of this.ds.entries()) {
      if (bayGioMs - v.mocMs >= this.giayCho * 1000) {
        het.push(k);
      }
    }
    for (const k of het) {
      this.ds.delete(k);
    }
    return het;
  }
}

