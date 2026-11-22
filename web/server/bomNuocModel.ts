import fs from 'node:fs';
import path from 'node:path';

/** Topic MQTT co dinh trong firmware `bom_nuoc` (`config.h:144-147`). */
export const BOM_NUOC_TOPICS = {
  STATE: 'bomnuoc/state',
  CMD: 'bomnuoc/cmd',
  STATUS: 'bomnuoc/status',
  USAGE: 'bomnuoc/usage',
  SYS: 'bomnuoc/sys',
} as const;

export const DUNG_TICH_BON_LIT = 1500;
export const CHO_XAC_NHAN_BOM_MS = 8_000;

export interface CamBienNhiet {
  ten: string;
  giaTriTho: number;
}

export interface ChanIo {
  ten: string;
  gpio: number;
  muc: number;
}

/** Mot thang trong mang `/api/usage` hoac topic `bomnuoc/usage` (toi da 12 thang, moi nhat truoc). */
export interface LuongNuocThang {
  thang: string;
  bomLit: number;
  nuocMayLit: number | null;
}

/** Trang thai day du cua bo `bom_nuoc` - khop voi `stateJson()` trong firmware. */
export interface BomNuocState {
  up: number | null;
  fw: string | null;
  hb: number | null;

  rssi: number | null;
  ssid: string | null;
  ip: string | null;

  aboveLow: number | null;
  isFull: number | null;
  lvl: number | null;
  litres: number | null;
  lvlRaw: number | null;
  lvlE: number | null;
  lvlF: number | null;

  tankOn: number | null;
  tankMode: string | null;
  tankRun: number | null;
  fTank: number | null;

  boostOn: number | null;
  boostRun: number | null;
  bstMode: string | null;
  bstLock: number | null;

  fCity: number | null;
  useC: number | null;
  cityPulses: number | null;

  den: number | null;
  locked: number | null;
  fault: string | null;
  temp: number | null;

  dryCheck: number | null;
  minRest: number | null;
  maxRun: number | null;
  pulses: number | null;
  minFlow: number | null;

  calPT: number | null;
  calLT: number | null;

  useM: string | null;
  useT: number | null;

  temps: CamBienNhiet[] | null;
  io: ChanIo[] | null;
}

export type KenhBomNuoc = 'LAN' | 'MQTT' | 'KHONG';

export type TrangThaiKetNoiBom =
  | { state: 'chua_cau_hinh' }
  | { state: 'dang_noi' }
  | { state: 'chi_co_broker' }
  | { state: 'truc_tiep'; kenh: KenhBomNuoc; tuoiGiay: number }
  | { state: 'so_cu'; tuoiGiay: number }
  | { state: 'bo_offline' }
  | { state: 'mat_broker' }
  | { state: 'sai_tai_khoan' }
  | { state: 'loi'; lyDo: string };

export type NhomNhatKy = 'pump' | 'water' | 'alarm' | 'system';
export type MucNhatKy = 'info' | 'warn' | 'error';

export interface SuKienBom {
  id: number;
  luc: number;
  nhom: NhomNhatKy;
  tieuDe: string;
  chiTiet: string;
  muc: MucNhatKy;
}

export type BomNuocCmd =
  | { kind: 'tank'; mode: 'auto' | 'on' | 'off' }
  | { kind: 'booster'; on: boolean }
  | { kind: 'led'; on: boolean }
  | { kind: 'clear' }
  | { kind: 'coiTat' }
  | { kind: 'coiThu' }
  | { kind: 'protection'; dryCheck: number; minRest: number; maxRun: number; pulses: number; minFlow: number }
  | { kind: 'lvlSetEmpty' }
  | { kind: 'lvlSetFull' }
  | { kind: 'calReset' }
  | { kind: 'usageClear' };

export type KetQuaLenhBomStatus =
  | 'da_xac_nhan'
  | 'chua_xac_nhan'
  | 'bi_khoa_bao_ve'
  | 'gui_that_bai'
  | 'dang_ban'
  | 'khong_hop_le';

export interface KetQuaLenhBomRes {
  ok: boolean;
  status: KetQuaLenhBomStatus;
  message: string;
}

export interface ChiTietLoiBom {
  goc: string;
  noiDung: string;
  mucDo: 'CANH_BAO' | 'NGUY_HIEM';
  xoaDuoc: boolean;
}

export interface ThamSoBaoVeDaLamTron {
  dryCheck: number;
  minRest: number;
  maxRun: number;
  pulses: number;
  minFlow: number;
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

function optStr(o: Record<string, unknown>, key: string): string | null {
  if (!Object.prototype.hasOwnProperty.call(o, key)) return null;
  const raw = o[key];
  if (raw === null || raw === undefined) return null;
  return String(raw);
}

function optNum(o: Record<string, unknown>, key: string): number | null {
  if (!Object.prototype.hasOwnProperty.call(o, key)) return null;
  const raw = o[key];
  if (raw === null || raw === undefined) return null;
  let v: number;
  if (typeof raw === 'number') {
    v = raw;
  } else if (typeof raw === 'string') {
    const trimmed = raw.trim();
    if (!trimmed) return null;
    v = Number(trimmed);
  } else {
    return null;
  }
  return Number.isFinite(v) ? v : null;
}

function optInt(o: Record<string, unknown>, key: string): number | null {
  const n = optNum(o, key);
  return n !== null ? Math.trunc(n) : null;
}

export function parseBomNuocState(raw: string): BomNuocState | null {
  try {
    const parsed: unknown = JSON.parse(raw.trim());
    if (!isRecord(parsed)) return null;

    const tempsRaw = parsed.temps;
    const temps: CamBienNhiet[] | null = Array.isArray(tempsRaw)
      ? tempsRaw.filter(isRecord).map((t) => ({
          ten: optStr(t, 'n') ?? '',
          giaTriTho: optNum(t, 'v') ?? -127.0,
        }))
      : null;

    const ioRaw = parsed.io;
    const io: ChanIo[] | null = Array.isArray(ioRaw)
      ? ioRaw.filter(isRecord).map((pin) => ({
          ten: optStr(pin, 'n') ?? '',
          gpio: optInt(pin, 'g') ?? -1,
          muc: optInt(pin, 'v') ?? 0,
        }))
      : null;

    return {
      up: optInt(parsed, 'up'),
      fw: optStr(parsed, 'fw'),
      hb: optInt(parsed, 'hb'),
      rssi: optInt(parsed, 'rssi'),
      ssid: optStr(parsed, 'ssid'),
      ip: optStr(parsed, 'ip'),
      aboveLow: optInt(parsed, 'aboveLow'),
      isFull: optInt(parsed, 'isFull'),
      lvl: optNum(parsed, 'lvl'),
      litres: optNum(parsed, 'litres'),
      lvlRaw: optInt(parsed, 'lvlRaw'),
      lvlE: optInt(parsed, 'lvlE'),
      lvlF: optInt(parsed, 'lvlF'),
      tankOn: optInt(parsed, 'tankOn'),
      tankMode: optStr(parsed, 'tankMode'),
      tankRun: optInt(parsed, 'tankRun'),
      fTank: optNum(parsed, 'fTank'),
      boostOn: optInt(parsed, 'boostOn'),
      boostRun: optInt(parsed, 'boostRun'),
      bstMode: optStr(parsed, 'bstMode'),
      bstLock: optInt(parsed, 'bstLock'),
      fCity: optNum(parsed, 'fCity'),
      useC: optNum(parsed, 'useC'),
      cityPulses: optNum(parsed, 'cityPulses'),
      den: optInt(parsed, 'den'),
      locked: optInt(parsed, 'locked'),
      fault: optStr(parsed, 'fault'),
      temp: optNum(parsed, 'temp'),
      dryCheck: optInt(parsed, 'dryCheck'),
      minRest: optNum(parsed, 'minRest'),
      maxRun: optInt(parsed, 'maxRun'),
      pulses: optNum(parsed, 'pulses'),
      minFlow: optNum(parsed, 'minFlow'),
      calPT: optInt(parsed, 'calPT'),
      calLT: optNum(parsed, 'calLT'),
      useM: optStr(parsed, 'useM'),
      useT: optNum(parsed, 'useT'),
      temps,
      io,
    };
  } catch {
    return null;
  }
}

export function parseLuongNuocThang(raw: string): LuongNuocThang[] | null {
  try {
    const parsed: unknown = JSON.parse(raw.trim());
    if (!Array.isArray(parsed)) return null;
    const out: LuongNuocThang[] = [];
    for (const item of parsed) {
      if (!isRecord(item)) continue;
      out.push({
        thang: optStr(item, 'm') ?? '',
        bomLit: optNum(item, 't') ?? 0.0,
        nuocMayLit: optNum(item, 'c'),
      });
    }
    return out;
  } catch {
    return null;
  }
}

// Sentinel helpers (khong bao gio bia so)
export function mucNuocPhanTram(s: BomNuocState): number | null {
  if (s.lvl === null || s.lvl < 0) return null;
  return Math.min(100, Math.max(0, s.lvl));
}

export function mucNuocLit(s: BomNuocState): number | null {
  if (s.litres === null || s.litres < 0) return null;
  return s.litres;
}

export function nhietDoNuoc(s: BomNuocState): number | null {
  if (s.temp === null || s.temp <= -100) return null;
  return s.temp;
}

export function wifiRssi(s: BomNuocState): number | null {
  if (s.rssi === null || s.rssi === 0) return null;
  return s.rssi;
}

export function coiDangKeu(s: BomNuocState): boolean | null {
  if (!s.io) return null;
  const pin = s.io.find((p) => p.gpio === 5);
  return pin ? pin.muc === 1 : null;
}

export function nhanCheDoBon(mode: string | null | undefined): string {
  switch (mode?.trim().toLowerCase()) {
    case 'auto': return 'tự động';
    case 'on': return 'bật tay';
    case 'off': return 'tắt tay';
    default: return '—';
  }
}

export function nguongImLangGiay(s: BomNuocState | null): number {
  return (s?.hb ?? 5) * 3 + 10;
}

interface QuyTacLoi {
  khop: string;
  mucDo: 'CANH_BAO' | 'NGUY_HIEM';
  xoaDuoc: boolean;
  taoCau: (raw: string) => string;
}

function soGiua(s: string, sau: string, truoc: string): string | null {
  const i = s.indexOf(sau);
  if (i < 0) return null;
  const start = i + sau.length;
  const j = s.indexOf(truoc, start);
  if (j < 0) return null;
  const sub = s.slice(start, j).trim();
  return sub.length > 0 ? sub : null;
}

const QUY_TAC_LOI: QuyTacLoi[] = [
  {
    khop: 'Bom len bon chay nhung khong co nuoc',
    mucDo: 'NGUY_HIEM',
    xoaDuoc: true,
    taoCau: () => 'Bơm lên bồn chạy nhưng không có nước. Đã tắt và khoá.',
  },
  {
    khop: 'Bom len bon chay qua',
    mucDo: 'NGUY_HIEM',
    xoaDuoc: true,
    taoCau: (raw) => `Bơm lên bồn chạy quá ${soGiua(raw, 'qua ', ' phut') ?? '?'} phút mà chưa đầy. Đã tắt và khoá.`,
  },
  {
    khop: 'Bom len bon qua nong',
    mucDo: 'NGUY_HIEM',
    xoaDuoc: true,
    taoCau: (raw) => `Bơm lên bồn quá nóng (${soGiua(raw, '(', ' do C') ?? '?'} °C). Đã tắt và khoá.`,
  },
  {
    khop: 'Bom ap chay lien tuc qua',
    mucDo: 'NGUY_HIEM',
    xoaDuoc: true,
    taoCau: (raw) => `Bơm tăng áp chạy liên tục quá ${soGiua(raw, 'qua ', ' phut') ?? '?'} phút. Đã cắt nguồn và khoá để kiểm tra.`,
  },
  {
    khop: 'Bom ap qua nong',
    mucDo: 'NGUY_HIEM',
    xoaDuoc: true,
    taoCau: (raw) => `Bơm tăng áp quá nóng (${soGiua(raw, '(', ' do C') ?? '?'} °C). Đã cắt nguồn và khoá.`,
  },
  {
    khop: 'Bom ap dang khoa vi su co lan truoc',
    mucDo: 'CANH_BAO',
    xoaDuoc: true,
    taoCau: () => 'Bơm tăng áp đang khoá vì sự cố lần trước. Bấm Xoá lỗi để mở.',
  },
  {
    khop: 'Dang khoa vi lan truoc bom chay can',
    mucDo: 'CANH_BAO',
    xoaDuoc: true,
    taoCau: () => 'Đang khoá vì lần trước bơm chạy cạn. Bấm Xoá lỗi để mở.',
  },
  {
    khop: 'Dang khoa vi lan truoc bom chay qua gio',
    mucDo: 'CANH_BAO',
    xoaDuoc: true,
    taoCau: () => 'Đang khoá vì lần trước bơm chạy quá giờ. Bấm Xoá lỗi để mở.',
  },
  {
    khop: 'Hai moc muc nuoc bi dao nguoc',
    mucDo: 'CANH_BAO',
    xoaDuoc: true,
    taoCau: () => 'Hai mốc mức nước bị đảo ngược. Vào mục Bảo vệ, ghi lại mốc CẠN lúc bồn cạn và mốc ĐẦY lúc bồn đầy.',
  },
  {
    khop: 'Nap firmware bi dut giua chung',
    mucDo: 'CANH_BAO',
    xoaDuoc: true,
    taoCau: () => 'Nạp firmware bị đứt giữa chừng. Firmware cũ vẫn nguyên vẹn.',
  },
];

export function phanTichLoiBomNuoc(raw: string | null | undefined): ChiTietLoiBom | null {
  const s = raw?.trim() ?? '';
  if (!s) return null;
  const match = QUY_TAC_LOI.find((q) => s.startsWith(q.khop) || s.includes(q.khop));
  if (match) {
    return {
      goc: s,
      noiDung: match.taoCau(s),
      mucDo: match.mucDo,
      xoaDuoc: match.xoaDuoc,
    };
  }
  return {
    goc: s,
    noiDung: s,
    mucDo: 'NGUY_HIEM',
    xoaDuoc: true,
  };
}

export function kiemTraBaoVe(
  dry: number | null | undefined,
  minFlow: number | null | undefined,
  maxRun: number | null | undefined,
  minRest: number | null | undefined,
  pulses: number | null | undefined,
): string | null {
  if (dry === null || dry === undefined || !Number.isInteger(dry) || dry < 5 || dry > 600) {
    return 'Thời gian kiểm tra chạy khô phải từ 5 đến 600 giây';
  }
  if (minFlow === null || minFlow === undefined || !Number.isFinite(minFlow) || minFlow < 0.1 || minFlow > 50.0) {
    return 'Ngưỡng chạy khô phải từ 0.1 đến 50 L/phút';
  }
  if (maxRun === null || maxRun === undefined || !Number.isInteger(maxRun) || maxRun < 1 || maxRun > 360) {
    return 'Thời gian chạy tối đa phải từ 1 đến 360 phút';
  }
  if (minRest === null || minRest === undefined || !Number.isFinite(minRest) || minRest < 0.1 || minRest > 180.0) {
    return 'Thời gian nghỉ phải từ 0.1 đến 180 phút';
  }
  if (pulses === null || pulses === undefined || !Number.isFinite(pulses) || pulses < 5.0 || pulses > 10000.0) {
    return 'Hiệu chuẩn cảm biến phải từ 5 đến 10000 xung/lít';
  }
  return null;
}

export function lamTronBaoVe(
  dry: number,
  rest: number,
  run: number,
  pulses: number,
  minFlow: number,
): ThamSoBaoVeDaLamTron {
  return {
    dryCheck: Math.trunc(dry),
    minRest: Math.round(rest * 10) / 10,
    maxRun: Math.trunc(run),
    pulses: Math.round(pulses * 10) / 10,
    minFlow: Math.round(minFlow * 100) / 100,
  };
}

export function dinhDangQueryBaoVe(p: ThamSoBaoVeDaLamTron): string {
  return `cfgDry=${p.dryCheck}&cfgRest=${p.minRest}&cfgRun=${p.maxRun}&cfgPulses=${p.pulses}&cfgMinFlow=${p.minFlow}`;
}

export function ganBang(a: number | null | undefined, b: number, saiSo: number): boolean {
  return a !== null && a !== undefined && Math.abs(a - b) < saiSo;
}

export function nhanKetNoiBom(link: TrangThaiKetNoiBom, kenh: KenhBomNuoc): string {
  switch (link.state) {
    case 'chua_cau_hinh': return 'Chưa cấu hình';
    case 'dang_noi': return 'Đang kết nối…';
    case 'chi_co_broker': return 'Nối được broker nhưng chưa thấy bo — kiểm tra nguồn bo hoặc tài khoản MQTT';
    case 'truc_tiep': return kenh === 'LAN' || link.kenh === 'LAN' ? 'LAN · 1s' : 'MQTT · 30s';
    case 'so_cu': return `Bo không gửi số liệu (${link.tuoiGiay}s)`;
    case 'bo_offline': return 'Bo đang offline';
    case 'mat_broker': return 'Mất kết nối broker';
    case 'sai_tai_khoan': return 'Sai tài khoản hoặc mật khẩu MQTT';
    case 'loi': return `Lỗi: ${link.lyDo.trim() || 'không nối được'}`;
  }
}

export class NhatKyBomNuocStore {
  static readonly TOI_DA_MUC = 100;
  private static readonly GIU_NGAY_MS = 90 * 24 * 3600 * 1000;

  private readonly filePath: string;
  private readonly truoc = new Map<string, string>();
  private idTiepTheo = 1;
  private danhSach: SuKienBom[] = [];

  constructor(filePath?: string) {
    this.filePath =
      filePath ||
      process.env.BOMNUOC_LOG_PATH ||
      path.resolve(process.cwd(), 'data', 'bom_nuoc_nhat_ky.json');
    this.napTuDia();
  }

  getDanhSach(): SuKienBom[] {
    return this.danhSach;
  }

  datLaiTheoDoi(): void {
    this.truoc.clear();
  }

  /** Quan sat trang thai moi. Lan dau tien cua moi khoa luon duoc bo qua. */
  quanSat(s: BomNuocState, luc: number = Date.now()): SuKienBom[] {
    const moi: SuKienBom[] = [];

    this.theoDoiKhoa('tank', s.tankOn === 1 ? '1' : '0', (on) => {
      const mode = nhanCheDoBon(s.tankMode);
      const pct = mucNuocPhanTram(s);
      const muc = pct !== null
        ? `mức ${Math.trunc(pct)}%`
        : s.isFull === 1
          ? 'phao báo đầy'
          : 'phao chưa đầy';
      moi.push(
        this.tao(
          luc,
          'pump',
          on === '1' ? 'Bơm lên bồn bắt đầu chạy' : 'Bơm lên bồn đã dừng',
          `Chế độ ${mode} · ${muc}`,
          'info',
        ),
      );
    });

    this.theoDoiKhoa('boost', s.boostOn === 1 ? '1' : '0', (on) => {
      moi.push(
        this.tao(
          luc,
          'pump',
          on === '1' ? 'Bơm tăng áp bật' : 'Bơm tăng áp tắt',
          'Điều khiển bằng tay',
          'info',
        ),
      );
    });

    this.theoDoiKhoa('mode', s.tankMode ?? '', (m) => {
      if (m.trim()) {
        moi.push(
          this.tao(luc, 'system', 'Đổi chế độ bồn', `Sang ${nhanCheDoBon(m)}`, 'info'),
        );
      }
    });

    if (s.fCity !== null) {
      this.theoDoiKhoa('city', s.fCity > 0.3 ? '1' : '0', (on) => {
        moi.push(
          this.tao(
            luc,
            'water',
            on === '1' ? 'Nước máy đang vào bồn' : 'Nước máy ngừng chảy',
            'Nhánh nước máy',
            'info',
          ),
        );
      });
    }

    this.theoDoiKhoa('full', s.isFull === 1 ? '1' : '0', (on) => {
      moi.push(
        this.tao(
          luc,
          'water',
          on === '1' ? 'Bồn đã đầy' : 'Bồn rời mức đầy',
          on === '1' ? 'Phao báo đầy' : 'Phao nhả',
          'info',
        ),
      );
    });

    this.theoDoiKhoa('fault', s.fault ?? '', (f) => {
      if (!f.trim()) {
        moi.push(
          this.tao(luc, 'alarm', 'Đã xoá lỗi, hệ thống mở khoá', 'Sẵn sàng chạy lại', 'info'),
        );
      } else {
        const parsed = phanTichLoiBomNuoc(f);
        moi.push(
          this.tao(
            luc,
            'alarm',
            `Sự cố: ${parsed?.noiDung ?? f}`,
            'Thiết bị đang bị khoá',
            parsed?.mucDo === 'NGUY_HIEM' ? 'error' : 'warn',
          ),
        );
      }
    });

    if (s.den !== null) {
      this.theoDoiKhoa('den', s.den === 1 ? '1' : '0', (on) => {
        moi.push(
          this.tao(
            luc,
            'system',
            on === '1' ? 'Đèn phụ bật' : 'Đèn phụ tắt',
            'Trong hộp điện',
            'info',
          ),
        );
      });
    }

    if (moi.length > 0) {
      this.danhSach = [...moi.reverse(), ...this.danhSach].slice(0, NhatKyBomNuocStore.TOI_DA_MUC);
      this.luuRaDia(this.danhSach);
    }

    return moi;
  }

  xoaTatCa(): void {
    this.danhSach = [];
    try {
      if (fs.existsSync(this.filePath)) {
        fs.unlinkSync(this.filePath);
      }
    } catch {
      // ignore delete error
    }
  }

  private theoDoiKhoa(khoa: string, giaTri: string, khiDoi: (v: string) => void): void {
    const cu = this.truoc.get(khoa);
    if (cu === undefined) {
      this.truoc.set(khoa, giaTri);
      return;
    }
    if (cu === giaTri) return;
    this.truoc.set(khoa, giaTri);
    khiDoi(giaTri);
  }

  private tao(
    luc: number,
    nhom: NhomNhatKy,
    tieuDe: string,
    chiTiet: string,
    muc: MucNhatKy,
  ): SuKienBom {
    return {
      id: this.idTiepTheo++,
      luc,
      nhom,
      tieuDe,
      chiTiet,
      muc,
    };
  }

  private napTuDia(): void {
    if (!fs.existsSync(this.filePath)) return;
    const mocCu = Date.now() - NhatKyBomNuocStore.GIU_NGAY_MS;
    try {
      const raw = fs.readFileSync(this.filePath, 'utf8');
      const parsed: unknown = JSON.parse(raw);
      if (!Array.isArray(parsed)) return;
      const loaded: SuKienBom[] = [];
      for (const item of parsed) {
        if (!isRecord(item)) continue;
        const luc = typeof item.luc === 'number' && Number.isFinite(item.luc) ? Math.trunc(item.luc) : 0;
        if (luc <= 0 || luc < mocCu) continue;
        let id = typeof item.id === 'number' && Number.isFinite(item.id) && item.id > 0
          ? Math.trunc(item.id)
          : this.idTiepTheo++;
        if (id >= this.idTiepTheo) this.idTiepTheo = id + 1;
        const nhomRaw = String(item.nhom || '');
        const nhom: NhomNhatKy =
          nhomRaw === 'pump' || nhomRaw === 'water' || nhomRaw === 'alarm' || nhomRaw === 'system'
            ? nhomRaw
            : 'system';
        const mucRaw = String(item.muc || '');
        const muc: MucNhatKy =
          mucRaw === 'info' || mucRaw === 'warn' || mucRaw === 'error' ? mucRaw : 'info';
        loaded.push({
          id,
          luc,
          nhom,
          tieuDe: String(item.tieuDe || ''),
          chiTiet: String(item.chiTiet || ''),
          muc,
        });
        if (loaded.length >= NhatKyBomNuocStore.TOI_DA_MUC) break;
      }
      this.danhSach = loaded;
    } catch {
      this.danhSach = [];
    }
  }

  private luuRaDia(ds: SuKienBom[]): void {
    try {
      const dir = path.dirname(this.filePath);
      fs.mkdirSync(dir, { recursive: true });
      const tmp = `${this.filePath}.tmp`;
      fs.writeFileSync(tmp, JSON.stringify(ds), { encoding: 'utf8', mode: 0o600 });
      fs.renameSync(tmp, this.filePath);
    } catch {
      // ignore disk write error
    }
  }
}
