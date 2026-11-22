export const DienTopic = {
  GOC: 'dien/#',
  TRANG_THAI: 'dien/trangthai',
  NGAY_SAN_LUONG: 'dien/ngay/sanluong',
  NGAY: 'dien/ngay/ngay',
  THANG_SAN_LUONG: 'dien/thang/sanluong',
  THANG_TIEN: 'dien/thang/tien',
  CONGTO_CHI_SO: 'dien/congto/chiso',
  CONGNO_SO_TIEN: 'dien/congno/sotien',
  LICH_SU: 'dien/lichsu',
  CAP_NHAT: 'dien/capnhat',
  LOI: 'dien/loi',
} as const;

export interface NgayKwh {
  ngay: string;
  kwh: number;
}

export type DoTuoi = 'MOI' | 'HOI_CU' | 'QUA_CU' | 'CHUA_CO';

export type KetNoi =
  | { state: 'chua_cau_hinh' }
  | { state: 'dang_noi' }
  | { state: 'da_noi' }
  | { state: 'loi'; moTa: string };

export interface DienState {
  trangThai: string | null;
  ngaySanLuong: number | null;
  ngay: string | null;
  thangSanLuong: number | null;
  thangTien: number | null;
  congToChiSo: number | null;
  congNoSoTien: number | null;
  lichSu: NgayKwh[];
  capNhat: string | null;
  loi: string | null;
}

export function createEmptyDienState(): DienState {
  return {
    trangThai: null,
    ngaySanLuong: null,
    ngay: null,
    thangSanLuong: null,
    thangTien: null,
    congToChiSo: null,
    congNoSoTien: null,
    lichSu: [],
    capNhat: null,
    loi: null,
  };
}

export function coSoLieuDien(d: DienState): boolean {
  return d.lichSu.length > 0 || d.ngaySanLuong !== null || d.thangSanLuong !== null;
}

export function parseCapNhatDate(capNhat: string | null): Date | null {
  if (!capNhat) return null;
  const s = capNhat.trim();
  const m = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})$/.exec(s);
  if (!m) return null;
  const year = Number(m[1]);
  const month = Number(m[2]);
  const day = Number(m[3]);
  const hour = Number(m[4]);
  const minute = Number(m[5]);
  const second = Number(m[6]);
  const dt = new Date(year, month - 1, day, hour, minute, second);
  if (
    dt.getFullYear() !== year ||
    dt.getMonth() !== month - 1 ||
    dt.getDate() !== day ||
    dt.getHours() !== hour ||
    dt.getMinutes() !== minute ||
    dt.getSeconds() !== second
  ) {
    return null;
  }
  return dt;
}

export function doTuoiDien(d: DienState, bayGio: Date = new Date()): DoTuoi {
  const t = parseCapNhatDate(d.capNhat);
  if (!t) return 'CHUA_CO';
  const diffMs = bayGio.getTime() - t.getTime();
  const gio = Math.trunc(diffMs / 3_600_000);
  if (gio < 8) return 'MOI';
  if (gio < 24) return 'HOI_CU';
  return 'QUA_CU';
}

export function tongKwhDien(d: DienState, soNgay: number): number | null {
  if (d.lichSu.length === 0) return null;
  const slice = soNgay > 0 ? d.lichSu.slice(-soNgay) : [];
  return slice.reduce((acc, item) => acc + item.kwh, 0);
}

export function chenhPhanTramDien(d: DienState, soNgay: number): number | null {
  if (d.lichSu.length < soNgay * 2 || soNgay <= 0) return null;
  const cuoi = d.lichSu.slice(-soNgay).reduce((a, b) => a + b.kwh, 0);
  const truoc = d.lichSu.slice(-soNgay * 2, -soNgay).reduce((a, b) => a + b.kwh, 0);
  if (truoc <= 0) return null;
  return Math.round(((cuoi - truoc) / truoc) * 100);
}

function parseSo(s: string): number | null {
  const trimmed = s.trim();
  if (!trimmed) return null;
  const n = Number(trimmed);
  return Number.isFinite(n) ? n : null;
}

export function docLichSuDien(json: string): NgayKwh[] {
  if (!json.trim()) return [];
  try {
    const arr = JSON.parse(json);
    if (!Array.isArray(arr)) return [];
    const out: NgayKwh[] = [];
    for (const item of arr) {
      if (!item || typeof item !== 'object') continue;
      const obj = item as Record<string, unknown>;
      const ngay = typeof obj.ngay === 'string' && obj.ngay.trim() ? obj.ngay.trim() : null;
      if (!ngay) continue;
      if (!Object.prototype.hasOwnProperty.call(obj, 'kwh')) continue;
      const kwh = typeof obj.kwh === 'number' ? obj.kwh : Number(obj.kwh);
      if (!Number.isFinite(kwh)) continue;
      out.push({ ngay, kwh });
    }
    return out;
  } catch {
    return [];
  }
}

export function gopDien(cu: DienState, topic: string, payload: string): DienState {
  const s = payload.trim();
  const rong = s.length === 0;
  switch (topic) {
    case DienTopic.TRANG_THAI:
      return { ...cu, trangThai: rong ? null : s };
    case DienTopic.NGAY_SAN_LUONG:
      return { ...cu, ngaySanLuong: parseSo(s) };
    case DienTopic.NGAY:
      return { ...cu, ngay: rong ? null : s };
    case DienTopic.THANG_SAN_LUONG:
      return { ...cu, thangSanLuong: parseSo(s) };
    case DienTopic.THANG_TIEN:
      return { ...cu, thangTien: parseSo(s) };
    case DienTopic.CONGTO_CHI_SO:
      return { ...cu, congToChiSo: parseSo(s) };
    case DienTopic.CONGNO_SO_TIEN:
      return { ...cu, congNoSoTien: parseSo(s) };
    case DienTopic.CAP_NHAT:
      return { ...cu, capNhat: rong ? null : s };
    case DienTopic.LOI:
      return { ...cu, loi: rong ? null : s };
    case DienTopic.LICH_SU:
      return { ...cu, lichSu: docLichSuDien(s) };
    default:
      return cu;
  }
}

export function trungBinhKwh(slice: NgayKwh[]): number | null {
  if (slice.length === 0) return null;
  const sum = slice.reduce((acc, d) => acc + d.kwh, 0);
  return sum / slice.length;
}

/**
 * Ước tính tiền điện của một ngày dựa theo đơn giá bình quân thực tế từ kỳ hiện tại.
 * Trả null nếu chưa có đủ dữ liệu lũy kế tháng (tuyệt đối không bịa đơn giá).
 */
export function uocTinhTienNgay(kwh: number, dien: DienState): number | null {
  if (dien.thangTien === null || dien.thangSanLuong === null || dien.thangSanLuong <= 0) {
    return null;
  }
  return kwh * (dien.thangTien / dien.thangSanLuong);
}

function parseIsoDateLocal(iso: string): Date | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso.trim());
  if (!m) return null;
  const y = Number(m[1]);
  const mon = Number(m[2]);
  const day = Number(m[3]);
  const dt = new Date(y, mon - 1, day);
  if (dt.getFullYear() !== y || dt.getMonth() !== mon - 1 || dt.getDate() !== day) return null;
  return dt;
}

export function dinhDangDdMm(iso: string): string {
  const m = /^\d{4}-(\d{2})-(\d{2})$/.exec(iso.trim());
  return m ? `${m[2]}/${m[1]}` : iso.slice(-5);
}

export function nhanThuNgayNgan(iso: string): string {
  const dt = parseIsoDateLocal(iso);
  if (!dt) return dinhDangDdMm(iso);
  const dow = dt.getDay(); // 0 = CN, 1 = T2 .. 6 = T7
  return dow === 0 ? 'CN' : `T${dow + 1}`;
}

export function nhanThuNgayDayDu(iso: string): string {
  const dt = parseIsoDateLocal(iso);
  if (!dt) return iso;
  const dow = dt.getDay();
  return dow === 0 ? 'Chủ nhật' : `Thứ ${dow + 1}`;
}


