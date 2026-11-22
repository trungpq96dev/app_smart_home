import { describe, expect, it } from 'vitest';
import {
  chenhPhanTramDien,
  coSoLieuDien,
  createEmptyDienState,
  DienTopic,
  docLichSuDien,
  doTuoiDien,
  gopDien,
  parseCapNhatDate,
  tongKwhDien,
} from '../client/src/data/dien.js';

describe('DienState', () => {
  it('doc duoc lich su binh thuong', () => {
    const ds = docLichSuDien('[{"ngay":"2026-11-01","kwh":12.4},{"ngay":"2026-11-02","kwh":9}]');
    expect(ds).toEqual([
      { ngay: '2026-11-01', kwh: 12.4 },
      { ngay: '2026-11-02', kwh: 9 },
    ]);
  });

  it('bo rieng dong hong chu khong vut ca mang', () => {
    const ds = docLichSuDien(
      '[{"ngay":"2026-11-01","kwh":12.4},{"kwh":5},{"ngay":"2026-11-03"},{"ngay":"2026-11-04","kwh":7.1}]',
    );
    expect(ds.length).toBe(2);
    expect(ds[0].ngay).toBe('2026-11-01');
    expect(ds[1].ngay).toBe('2026-11-04');
  });

  it('json rac thi tra rong chu khong nem loi', () => {
    expect(docLichSuDien('khong phai json')).toEqual([]);
    expect(docLichSuDien('')).toEqual([]);
    expect(docLichSuDien('<html>loi 500</html>')).toEqual([]);
  });

  it('gop dung tung topic', () => {
    let s = createEmptyDienState();
    s = gopDien(s, DienTopic.TRANG_THAI, 'ok');
    s = gopDien(s, DienTopic.NGAY_SAN_LUONG, '12.4');
    s = gopDien(s, DienTopic.NGAY, '2026-11-16');
    s = gopDien(s, DienTopic.THANG_SAN_LUONG, '209');
    s = gopDien(s, DienTopic.THANG_TIEN, '480650');
    s = gopDien(s, DienTopic.CONGTO_CHI_SO, '15234');
    s = gopDien(s, DienTopic.CONGNO_SO_TIEN, '0');
    s = gopDien(s, DienTopic.CAP_NHAT, '2026-11-16 18:00:00');

    expect(s.trangThai).toBe('ok');
    expect(s.ngaySanLuong).toBeCloseTo(12.4, 3);
    expect(s.ngay).toBe('2026-11-16');
    expect(s.thangSanLuong).toBeCloseTo(209, 3);
    expect(s.thangTien).toBeCloseTo(480650, 3);
    expect(s.congToChiSo).toBeCloseTo(15234, 3);
    expect(s.congNoSoTien).toBeCloseTo(0, 3);
  });

  it('payload rong la khong co gia tri chu khong phai so khong', () => {
    const s = gopDien(createEmptyDienState(), DienTopic.NGAY_SAN_LUONG, '');
    expect(s.ngaySanLuong).toBeNull();
  });

  it('dien loi rong nghia la khong sao', () => {
    let s = gopDien(createEmptyDienState(), DienTopic.LOI, 'EVN tra 500');
    expect(s.loi).toBe('EVN tra 500');
    s = gopDien(s, DienTopic.LOI, '');
    expect(s.loi).toBeNull();
  });

  it('tong kwh va chenh phan tram', () => {
    const lichSu = Array.from({ length: 14 }, (_, idx) => {
      const i = idx + 1;
      return {
        ngay: `2026-11-${String(i).padStart(2, '0')}`,
        kwh: i <= 7 ? 10 : 5,
      };
    });
    const s = { ...createEmptyDienState(), lichSu };
    expect(tongKwhDien(s, 7)).toBeCloseTo(35, 3);
    expect(chenhPhanTramDien(s, 7)).toBe(-50);
  });

  it('chua du du lieu thi tra null chu khong doan', () => {
    const rong = createEmptyDienState();
    expect(tongKwhDien(rong, 7)).toBeNull();
    expect(chenhPhanTramDien(rong, 7)).toBeNull();

    const motTuan = {
      ...rong,
      lichSu: Array.from({ length: 7 }, (_, i) => ({ ngay: `2026-11-0${i + 1}`, kwh: 10 })),
    };
    expect(tongKwhDien(motTuan, 7)).toBeCloseTo(70, 3);
    expect(chenhPhanTramDien(motTuan, 7)).toBeNull();
  });

  it('do tuoi theo nhip 4 cu mot ngay', () => {
    const bayGio = new Date(2026, 10, 17, 12, 0, 0);
    const tuoi = (capNhat: string) =>
      doTuoiDien({ ...createEmptyDienState(), capNhat }, bayGio);

    expect(tuoi('2026-11-17 08:00:00')).toBe('MOI');
    expect(tuoi('2026-11-17 01:00:00')).toBe('HOI_CU');
    expect(tuoi('2026-11-15 12:00:00')).toBe('QUA_CU');
    expect(doTuoiDien(createEmptyDienState(), bayGio)).toBe('CHUA_CO');
  });

  it('cap nhat sai dinh dang thi coi nhu chua co', () => {
    const s = { ...createEmptyDienState(), capNhat: 'hom qua' };
    expect(doTuoiDien(s)).toBe('CHUA_CO');
    expect(parseCapNhatDate(s.capNhat)).toBeNull();
  });

  it('co so lieu chi bat khi that su co so', () => {
    expect(coSoLieuDien(createEmptyDienState())).toBe(false);
    expect(coSoLieuDien({ ...createEmptyDienState(), trangThai: 'ok', capNhat: '2026-11-17 08:00:00' })).toBe(false);
    expect(coSoLieuDien({ ...createEmptyDienState(), lichSu: [{ ngay: '2026-11-01', kwh: 1 }] })).toBe(true);
    expect(coSoLieuDien({ ...createEmptyDienState(), ngaySanLuong: 3 })).toBe(true);
  });
});

