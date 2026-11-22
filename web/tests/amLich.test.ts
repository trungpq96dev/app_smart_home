import { describe, expect, it } from 'vitest';
import { AmLich } from '../client/src/data/amLich.js';

function am(d: number, m: number, y: number) {
  return AmLich.tuDuongLich(d, m, y);
}

describe('AmLich', () => {
  it('mung 1 Tet cac nam gan day roi dung ngay', () => {
    const tet: Array<[number, number, number]> = [
      [22, 1, 2023],
      [10, 2, 2024],
      [29, 1, 2025],
      [17, 2, 2026],
      [6, 2, 2027],
    ];
    for (const [d, m, y] of tet) {
      const a = am(d, m, y);
      expect(a.ngay).toBe(1);
      expect(a.thang).toBe(1);
      expect(a.nhuan).toBe(false);
    }
  });

  it('nam Can Chi dung ten', () => {
    expect(AmLich.canChi(am(22, 1, 2023)).nam).toBe('Quý Mão');
    expect(AmLich.canChi(am(10, 2, 2024)).nam).toBe('Giáp Thìn');
    expect(AmLich.canChi(am(29, 1, 2025)).nam).toBe('Ất Tỵ');
    expect(AmLich.canChi(am(17, 2, 2026)).nam).toBe('Bính Ngọ');
  });

  it('ngay truoc Tet van thuoc nam cu', () => {
    const a = am(16, 2, 2026);
    expect(a.thang).toBe(12);
    expect(AmLich.canChi(a).nam).toBe('Ất Tỵ');
  });

  it('nam 2025 co thang 6 nhuan', () => {
    let thayNhuan = false;
    const d = new Date(2025, 0, 1);
    while (d.getFullYear() === 2025) {
      const a = am(d.getDate(), d.getMonth() + 1, 2025);
      if (a.nhuan) {
        expect(a.thang).toBe(6);
        thayNhuan = true;
      }
      d.setDate(d.getDate() + 1);
    }
    expect(thayNhuan).toBe(true);
  });

  it('nam 2026 khong co thang nhuan', () => {
    const d = new Date(2026, 2, 1);
    while (d < new Date(2027, 0, 1)) {
      expect(am(d.getDate(), d.getMonth() + 1, 2026).nhuan).toBe(false);
      d.setDate(d.getDate() + 1);
    }
  });

  it('ngay am lich chay lien tuc tu 1 den 29 hoac 30', () => {
    let truoc = am(1, 1, 2026);
    const d = new Date(2026, 0, 2);
    while (d < new Date(2027, 0, 1)) {
      const nay = am(d.getDate(), d.getMonth() + 1, 2026);
      if (nay.ngay !== 1) {
        expect(nay.ngay).toBe(truoc.ngay + 1);
      } else {
        expect(truoc.ngay >= 29 && truoc.ngay <= 30).toBe(true);
      }
      expect(nay.ngay >= 1 && nay.ngay <= 30).toBe(true);
      expect(nay.thang >= 1 && nay.thang <= 12).toBe(true);
      truoc = nay;
      d.setDate(d.getDate() + 1);
    }
  });

  it('can chi ngay xoay vong dung 60 ngay', () => {
    const moc = am(20, 11, 2026);
    const sau60 = am(19, 1, 2027);
    expect(sau60.jd - moc.jd).toBe(60);
    expect(AmLich.canChi(sau60).ngay).toBe(AmLich.canChi(moc).ngay);
  });

  it('moi ngay co dung 6 gio hoang dao', () => {
    for (let chi = 0; chi <= 11; chi++) {
      const gio = AmLich.gioHoangDao(chi);
      expect(gio.length).toBe(6);
      expect(new Set(gio).size).toBe(6);
    }
  });

  it('su kien am lich duoc uu tien hon duong lich', () => {
    expect(AmLich.suKien(17, 2, am(17, 2, 2026))).toBe('Tết Nguyên Đán');
    expect(AmLich.suKien(20, 11, am(20, 11, 2026))).toBe('Nhà giáo Việt Nam');
  });

  it('ngay thuong thi khong co su kien', () => {
    const a = am(21, 11, 2026);
    if (a.ngay !== 1 && a.ngay !== 15) {
      expect(AmLich.suKien(21, 11, a)).toBeNull();
    }
  });

  it('nhan o lich kem thang khi la mung 1', () => {
    const tet = am(17, 2, 2026);
    expect(AmLich.nhanO(tet)).toBe('1/1');
    expect(AmLich.laMocAm(tet)).toBe(true);

    const thuong = am(18, 2, 2026);
    expect(AmLich.nhanO(thuong)).toBe('2');
    expect(AmLich.laMocAm(thuong)).toBe(false);
  });

  it('jd khop voi so ngay Julius chuan', () => {
    expect(AmLich.jdTuDuongLich(1, 1, 2000)).toBe(2451545);
  });

  it('doi duoc ca ngay truoc cai cach lich Gregory', () => {
    expect(am(4, 10, 1582)).toBeTruthy();
  });
});

