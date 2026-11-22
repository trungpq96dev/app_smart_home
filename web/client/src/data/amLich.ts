/**
 * Doi duong lich sang am lich Viet Nam.
 *
 * Thuat toan cua Ho Ngoc Duc (port nguyen he so tu data/lunar/AmLich.kt).
 * Mui gio co dinh +7 (Ha Noi). Luon dung Math.floor cho cac buoc cat nguyen.
 */

export interface NgayAm {
  ngay: number;
  thang: number;
  nam: number;
  nhuan: boolean;
  jd: number;
}

export interface CanChi {
  nam: string;
  thang: string;
  ngay: string;
  chiNgay: number;
}

const MUI_GIO_VN = 7.0;

export const CAN = ['Giáp', 'Ất', 'Bính', 'Đinh', 'Mậu', 'Kỷ', 'Canh', 'Tân', 'Nhâm', 'Quý'];
export const CHI = ['Tý', 'Sửu', 'Dần', 'Mão', 'Thìn', 'Tỵ', 'Ngọ', 'Mùi', 'Thân', 'Dậu', 'Tuất', 'Hợi'];
export const GIO = [
  'Tý (23-1h)',
  'Sửu (1-3h)',
  'Dần (3-5h)',
  'Mão (5-7h)',
  'Thìn (7-9h)',
  'Tỵ (9-11h)',
  'Ngọ (11-13h)',
  'Mùi (13-15h)',
  'Thân (15-17h)',
  'Dậu (17-19h)',
  'Tuất (19-21h)',
  'Hợi (21-23h)',
];

const GIO_HOANG_DAO = [
  [0, 1, 3, 6, 8, 9],
  [2, 3, 5, 8, 10, 11],
  [0, 1, 4, 5, 7, 10],
  [0, 2, 3, 6, 7, 9],
  [2, 4, 5, 8, 9, 11],
  [1, 4, 6, 7, 10, 11],
];

const LE_DUONG: Record<string, string> = {
  '1/1': 'Tết Dương lịch',
  '14/2': 'Lễ tình nhân',
  '8/3': 'Quốc tế Phụ nữ',
  '30/4': 'Giải phóng miền Nam',
  '1/5': 'Quốc tế Lao động',
  '1/6': 'Quốc tế Thiếu nhi',
  '2/9': 'Quốc khánh',
  '20/10': 'Phụ nữ Việt Nam',
  '20/11': 'Nhà giáo Việt Nam',
  '22/12': 'Quân đội nhân dân VN',
  '24/12': 'Lễ Giáng sinh',
};

const LE_AM: Record<string, string> = {
  '1/1': 'Tết Nguyên Đán',
  '2/1': 'Mùng 2 Tết',
  '3/1': 'Mùng 3 Tết',
  '15/1': 'Tết Nguyên Tiêu',
  '3/3': 'Tết Hàn Thực',
  '10/3': 'Giỗ Tổ Hùng Vương',
  '15/4': 'Lễ Phật Đản',
  '5/5': 'Tết Đoan Ngọ',
  '15/7': 'Lễ Vu Lan',
  '15/8': 'Tết Trung Thu',
  '9/9': 'Tết Trùng Cửu',
  '23/12': 'Ông Công Ông Táo',
  '30/12': 'Giao thừa',
};

function INT(d: number): number {
  return Math.floor(d);
}

export function jdTuDuongLich(dd: number, mm: number, yy: number): number {
  const a = INT((14 - mm) / 12);
  const y = yy + 4800 - a;
  const m = mm + 12 * a - 3;
  let jd =
    dd +
    INT((153 * m + 2) / 5) +
    365 * y +
    INT(y / 4) -
    INT(y / 100) +
    INT(y / 400) -
    32045;
  if (jd < 2299161) {
    jd = dd + INT((153 * m + 2) / 5) + 365 * y + INT(y / 4) - 32083;
  }
  return jd;
}

function ngayTrangMoi(k: number, muiGio: number): number {
  const t = k / 1236.85;
  const t2 = t * t;
  const t3 = t2 * t;
  const dr = Math.PI / 180.0;
  let jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3;
  jd1 += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr);
  const m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3;
  const mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3;
  const f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3;
  const c1 =
    (0.1734 - 0.000393 * t) * Math.sin(m * dr) +
    0.0021 * Math.sin(2 * dr * m) -
    0.4068 * Math.sin(mpr * dr) +
    0.0161 * Math.sin(dr * 2 * mpr) -
    0.0004 * Math.sin(dr * 3 * mpr) +
    0.0104 * Math.sin(dr * 2 * f) -
    0.0051 * Math.sin(dr * (m + mpr)) -
    0.0074 * Math.sin(dr * (m - mpr)) +
    0.0004 * Math.sin(dr * (2 * f + m)) -
    0.0004 * Math.sin(dr * (2 * f - m)) -
    0.0006 * Math.sin(dr * (2 * f + mpr)) +
    0.001 * Math.sin(dr * (2 * f - mpr)) +
    0.0005 * Math.sin(dr * (2 * mpr + m));
  const deltat =
    t < -11
      ? 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
      : -0.000278 + 0.000265 * t + 0.000262 * t2;
  return INT(jd1 + c1 - deltat + 0.5 + muiGio / 24.0);
}

function kinhDoMatTroi(jdn: number, muiGio: number): number {
  const t = (jdn - 2451545.5 - muiGio / 24.0) / 36525.0;
  const t2 = t * t;
  const dr = Math.PI / 180.0;
  const m = 357.5291 + 35999.0503 * t - 0.0001559 * t2 - 0.00000048 * t * t2;
  const l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2;
  const dl =
    (1.9146 - 0.004817 * t - 0.000014 * t2) * Math.sin(dr * m) +
    (0.019993 - 0.000101 * t) * Math.sin(dr * 2 * m) +
    0.00029 * Math.sin(dr * 3 * m);
  let l = (l0 + dl) * dr;
  l -= Math.PI * 2 * INT(l / (Math.PI * 2));
  return INT((l / Math.PI) * 6);
}

function thang11AmLich(yy: number, muiGio: number): number {
  const off = jdTuDuongLich(31, 12, yy) - 2415021;
  const k = INT(off / 29.530588853);
  let nm = ngayTrangMoi(k, muiGio);
  if (kinhDoMatTroi(nm, muiGio) >= 9) nm = ngayTrangMoi(k - 1, muiGio);
  return nm;
}

function viTriThangNhuan(a11: number, muiGio: number): number {
  const k = INT((a11 - 2415021.076998695) / 29.530588853 + 0.5);
  let last: number;
  let i = 1;
  let arc = kinhDoMatTroi(ngayTrangMoi(k + i, muiGio), muiGio);
  do {
    last = arc;
    i++;
    arc = kinhDoMatTroi(ngayTrangMoi(k + i, muiGio), muiGio);
  } while (arc !== last && i < 14);
  return i - 1;
}

export const AmLich = {
  CAN,
  CHI,
  GIO,
  jdTuDuongLich,

  tuDuongLich(dd: number, mm: number, yy: number, muiGio: number = MUI_GIO_VN): NgayAm {
    const soNgay = jdTuDuongLich(dd, mm, yy);
    const k = INT((soNgay - 2415021.076998695) / 29.530588853);
    let dauThang = ngayTrangMoi(k + 1, muiGio);
    if (dauThang > soNgay) dauThang = ngayTrangMoi(k, muiGio);

    let a11 = thang11AmLich(yy, muiGio);
    let b11 = a11;
    let namAm: number;
    if (a11 >= dauThang) {
      namAm = yy;
      a11 = thang11AmLich(yy - 1, muiGio);
    } else {
      namAm = yy + 1;
      b11 = thang11AmLich(yy + 1, muiGio);
    }

    const ngayAm = soNgay - dauThang + 1;
    const chenh = INT((dauThang - a11) / 29.0);
    let nhuan = false;
    let thangAm = chenh + 11;
    if (b11 - a11 > 365) {
      const viTri = viTriThangNhuan(a11, muiGio);
      if (chenh >= viTri) {
        thangAm = chenh + 10;
        if (chenh === viTri) nhuan = true;
      }
    }
    if (thangAm > 12) thangAm -= 12;
    if (thangAm >= 11 && chenh < 4) namAm -= 1;

    return { ngay: ngayAm, thang: thangAm, nam: namAm, nhuan, jd: soNgay };
  },

  canChi(am: NgayAm): CanChi {
    const chiNgay = (am.jd + 1) % 12;
    return {
      nam: `${CAN[(am.nam + 6) % 10]} ${CHI[(am.nam + 8) % 12]}`,
      thang: `${CAN[(am.nam * 12 + am.thang + 3) % 10]} ${CHI[(am.thang + 1) % 12]}`,
      ngay: `${CAN[(am.jd + 9) % 10]} ${CHI[chiNgay]}`,
      chiNgay,
    };
  },

  gioHoangDao(chiNgay: number): string[] {
    return GIO_HOANG_DAO[chiNgay % 6].map((idx) => GIO[idx]);
  },

  suKien(dd: number, mm: number, am: NgayAm): string | null {
    if (!am.nhuan) {
      const leAm = LE_AM[`${am.ngay}/${am.thang}`];
      if (leAm) return leAm;
    }
    return LE_DUONG[`${dd}/${mm}`] ?? null;
  },

  nhanO(am: NgayAm): string {
    return am.ngay === 1 ? `${am.ngay}/${am.thang}` : String(am.ngay);
  },

  laMocAm(am: NgayAm): boolean {
    return am.ngay === 1 || am.ngay === 15;
  },
};

