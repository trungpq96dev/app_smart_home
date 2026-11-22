export interface PublicBomNuocConfig {
  espHost: string;
  uuTienLan: boolean;
  brokerHost: string;
  brokerPort: number;
  brokerTls: boolean;
  brokerUser: string;
  coMatKhau: boolean;
}

export interface BomNuocConfigForm {
  espHost: string;
  uuTienLan: boolean;
  brokerHost: string;
  brokerPort: number;
  brokerTls: boolean;
  brokerUser: string;
  brokerPass?: string;
  xoaMatKhau?: boolean;
}

export interface CamBienNhiet {
  ten: string;
  giaTriTho: number;
}

export interface ChanIo {
  ten: string;
  gpio: number;
  muc: number;
}

export interface LuongNuocThang {
  thang: string;
  bomLit: number;
  nuocMayLit: number | null;
}

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

export const DUNG_TICH_BON_LIT = 1500;

export const NHOM_NHAT_KY_LIST: Array<{ ma: NhomNhatKy; nhan: string }> = [
  { ma: 'pump', nhan: 'Bơm' },
  { ma: 'water', nhan: 'Nước' },
  { ma: 'alarm', nhan: 'Cảnh báo' },
  { ma: 'system', nhan: 'Hệ thống' },
];

export function daCauHinhBom(c: PublicBomNuocConfig): boolean {
  const coLan = c.espHost.trim().length > 0;
  const coMqtt = c.brokerHost.trim().length > 0 && c.brokerPort >= 1 && c.brokerPort <= 65535;
  return coLan || coMqtt;
}

export function coCamBienMuc(s: BomNuocState): boolean {
  return s.lvl !== null && s.lvl >= 0;
}

export function mucNuocPhanTram(s: BomNuocState): number | null {
  if (!coCamBienMuc(s)) return null;
  return Math.min(100, Math.max(0, s.lvl!));
}

export function mucNuocLit(s: BomNuocState): number | null {
  if (s.litres === null || s.litres < 0) return null;
  return s.litres;
}

export function nhietDoNuoc(s: BomNuocState): number | null {
  if (s.temp === null || s.temp <= -100) return null;
  return s.temp;
}

export function camBienNhietGiaTri(c: CamBienNhiet | undefined): number | null {
  if (!c || c.giaTriTho <= -100) return null;
  return c.giaTriTho;
}

export function nhietDoBomBon(s: BomNuocState): number | null {
  return camBienNhietGiaTri(s.temps?.[1]);
}

export function nhietDoBomAp(s: BomNuocState): number | null {
  return camBienNhietGiaTri(s.temps?.[2]);
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

export function coLoiBom(s: BomNuocState): boolean {
  return Boolean(s.fault && s.fault.trim().length > 0);
}

export function bomChinhDangChay(s: BomNuocState): boolean {
  return s.tankOn === 1;
}

export function bomApDangChay(s: BomNuocState): boolean {
  return s.boostOn === 1;
}

export function dangKhoaBom(s: BomNuocState): boolean {
  return s.locked === 1;
}

export function bomApDangKhoa(s: BomNuocState): boolean {
  return s.bstLock === 1;
}

export function bonDay(s: BomNuocState): boolean {
  return s.isFull === 1;
}

export function denPhuBat(s: BomNuocState): boolean {
  return s.den === 1;
}

export function coDenPhu(s: BomNuocState): boolean {
  return s.den !== null;
}

export function coNhanhNuocMay(s: BomNuocState): boolean {
  return s.fCity !== null;
}

export function nuocMayDangChay(s: BomNuocState): boolean {
  return (s.fCity ?? 0) > 0.3;
}

export function nhanCheDoBon(mode: string | null | undefined): string {
  switch (mode?.trim().toLowerCase()) {
    case 'auto': return 'tự động';
    case 'on': return 'bật tay';
    case 'off': return 'tắt tay';
    default: return '—';
  }
}

export function laThangChuaRo(m: LuongNuocThang): boolean {
  return m.thang === 'chua ro thang' || m.thang.trim().length === 0;
}

export function nhanThangNgan(m: LuongNuocThang): string {
  if (laThangChuaRo(m)) return '?';
  const idx = m.thang.indexOf('-');
  if (idx >= 0) {
    const num = Number.parseInt(m.thang.slice(idx + 1), 10);
    if (Number.isFinite(num)) return `T${num}`;
  }
  return m.thang;
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

function soGiua(s: string, sau: string, truoc: string): string | null {
  const i = s.indexOf(sau);
  if (i < 0) return null;
  const start = i + sau.length;
  const j = s.indexOf(truoc, start);
  if (j < 0) return null;
  const sub = s.slice(start, j).trim();
  return sub.length > 0 ? sub : null;
}

const QUY_TAC_LOI: Array<{
  khop: string;
  mucDo: 'CANH_BAO' | 'NGUY_HIEM';
  xoaDuoc: boolean;
  taoCau: (raw: string) => string;
}> = [
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

export function dinhDangThoiGianChay(giay: number): string {
  const g = Math.max(0, Math.trunc(giay));
  const h = Math.floor(g / 3600);
  const m = Math.floor((g % 3600) / 60);
  const s = g % 60;
  if (h > 0) return `${h} giờ ${String(m).padStart(2, '0')} phút`;
  if (m > 0) return `${m} phút ${String(s).padStart(2, '0')} giây`;
  return `${s} giây`;
}

export function catSoThapPhan(v: number): string {
  return Number.isInteger(v) ? String(v) : v.toFixed(1);
}
