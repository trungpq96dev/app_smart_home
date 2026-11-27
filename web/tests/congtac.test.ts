import { describe, expect, it } from 'vitest';
import {
  buildOThietBiThat,
  CongTacTopic,
  createEmptyCongTacState,
  gopCongTac,
  HangChoCongTac,
  isBridgeSong,
  laCongTacThat,
  LenhCongTac,
  timCongTac,
  trangThaiCongTacCua,
} from '../client/src/data/congtac.js';
import { DemoData } from '../client/src/data/demoData.js';
import {
  THU_TU_PHONG,
  tomTatDanhSachPhong,
  tomTatPhong,
} from '../client/src/screens/DevicesScreen.js';

const INFO = JSON.stringify({
  bridge: 'congtac_bridge 1.0',
  cap_nhat: '2026-11-21 00:40:32',
  thiet_bi: [
    {
      id: 'bed1_den',
      ten: 'Đèn Phòng ngủ 1',
      phong: 'bed1',
      loai: 'den',
      driver: 'tuya',
      ip: '192.168.0.100',
    },
    {
      id: 'bed1_quat',
      ten: 'Quạt Phòng ngủ 1',
      phong: 'bed1',
      loai: 'quat',
      driver: 'cozylife',
      ip: '192.168.0.103',
    },
  ],
});

function nenTang() {
  let s = createEmptyCongTacState();
  s = gopCongTac(s, CongTacTopic.TRANG_THAI, 'online');
  s = gopCongTac(s, CongTacTopic.INFO, INFO);
  return s;
}

describe('CongTacState', () => {
  it('doc duoc danh sach thiet bi', () => {
    const s = nenTang();
    expect(s.danhSach.length).toBe(2);
    expect(laCongTacThat(s, 'bed1', 'den')).toBe(true);
    expect(laCongTacThat(s, 'bed1', 'quat')).toBe(true);
    expect(timCongTac(s, 'bed1', 'den')?.ten).toBe('Đèn Phòng ngủ 1');
    expect(timCongTac(s, 'bed1', 'quat')?.he).toBe('cozylife');
  });

  it('thiet bi khong duoc khai thi van la mau', () => {
    const s = nenTang();
    expect(laCongTacThat(s, 'bed1', 'dieu_hoa')).toBe(false);
    expect(laCongTacThat(s, 'living', 'den')).toBe(false);
  });

  it('bo rieng phan tu hong chu khong vut ca mang', () => {
    const s = gopCongTac(
      createEmptyCongTacState(),
      CongTacTopic.INFO,
      '{"thiet_bi":[{"ten":"thieu id"},{"id":"bed1_quat","phong":"bed1","loai":"quat","ten":"Quạt"}]}',
    );
    expect(s.danhSach.length).toBe(1);
    expect(laCongTacThat(s, 'bed1', 'quat')).toBe(true);
  });

  it('info mang rong thi xoa het thiet bi, con payload rong thi giu nguyen', () => {
    const cleared = gopCongTac(nenTang(), CongTacTopic.INFO, '{"thiet_bi":[]}');
    expect(cleared.danhSach.length).toBe(0);

    const kept = gopCongTac(nenTang(), CongTacTopic.INFO, '');
    expect(kept.danhSach.length).toBe(2);
  });

  it('LWT khong bi dem di parse JSON', () => {
    const s = gopCongTac(createEmptyCongTacState(), CongTacTopic.TRANG_THAI, 'online');
    expect(s.bridge).toBe('online');
    expect(isBridgeSong(s)).toBe(true);
    expect(Object.keys(s.trangThai).length).toBe(0);

    const off = gopCongTac(s, CongTacTopic.TRANG_THAI, 'offline');
    expect(isBridgeSong(off)).toBe(false);
  });

  it('thieu is_on hoac offline thi bat la null chu khong phai false', () => {
    const missing = gopCongTac(
      nenTang(),
      `${CongTacTopic.TIEN_TO_TB}bed1_den`,
      '{"online":true,"doc_luc":"2026-11-21 00:40:32"}',
    );
    expect(trangThaiCongTacCua(missing, 'bed1', 'den')?.bat).toBeNull();

    const offline = gopCongTac(
      nenTang(),
      `${CongTacTopic.TIEN_TO_TB}bed1_den`,
      '{"online":false,"is_on":true,"ly_do_loi":"mat dien"}',
    );
    const tt = trangThaiCongTacCua(offline, 'bed1', 'den')!;
    expect(tt.online).toBe(false);
    expect(tt.bat).toBeNull();
    expect(tt.lyDoLoi).toBe('mat dien');
  });

  it('LenhCongTac json giu dung khuon payload', () => {
    const j = JSON.parse(LenhCongTac.json('bed1', 'den', 'bat', null, LenhCongTac.NGUON_MAN_HINH, 'abc123'));
    expect(j.device).toBe('bed1_den');
    expect(j.room).toBe('bed1');
    expect(j.action).toBe('BAT');
    expect(j.source).toBe('man_hinh');
    expect(j.id).toBe('abc123');
  });

  it('HangChoCongTac khop ma lenh, khong go lenh cu, va timeout 6s', () => {
    const h = new HangChoCongTac(6);
    h.ganh('bed1_den', true, 'a1', 1000);
    expect(h.dangCho('bed1_den')).toBe(true);
    expect(h.nhanTrangThai('bed1_den', 'wrong')).toBe(false);
    expect(h.nhanTrangThai('bed1_den', null)).toBe(false);
    expect(h.nhanTrangThai('bed1_den', 'a1')).toBe(true);
    expect(h.dangCho('bed1_den')).toBe(false);

    h.ganh('bed1_den', true, 'b1', 1000);
    expect(h.quaHan(5000)).toEqual([]);
    expect(h.quaHan(7000)).toEqual(['bed1_den']);
    expect(h.dangCho('bed1_den')).toBe(false);
  });

  it('OThietBi.That kiem tra gatDuoc va ghiChu', () => {
    const matKetNoi = buildOThietBiThat({
      ten: 'Đèn',
      bridgeSong: true,
      online: false,
      bat: null,
      dangCho: false,
      mong: null,
      lyDoLoi: 'mat dien',
    });
    expect(matKetNoi.kind === 'that' && !matKetNoi.gatDuoc).toBe(true);
    expect(matKetNoi.kind === 'that' && matKetNoi.ghiChu).toBe('mat dien');

    const ok = buildOThietBiThat({
      ten: 'Đèn',
      bridgeSong: true,
      online: true,
      bat: false,
      dangCho: false,
      mong: null,
      lyDoLoi: null,
    });
    expect(ok.kind === 'that' && ok.gatDuoc).toBe(true);
    expect(ok.kind === 'that' && ok.ghiChu).toBeNull();
  });

  it('LenhCongTac.json dung khuon ON/OFF khop hop dong congtac_bridge.py', () => {
    const onCmd = JSON.parse(
      LenhCongTac.json('bed1', 'den', 'ON', null, LenhCongTac.NGUON_MAN_HINH, 'cmd12345'),
    ) as Record<string, string>;
    expect(onCmd).toEqual({
      device: 'bed1_den',
      room: 'bed1',
      action: 'ON',
      source: 'man_hinh',
      id: 'cmd12345',
    });

    const offCmd = JSON.parse(
      LenhCongTac.json('bed1', 'quat', 'OFF', null, LenhCongTac.NGUON_GIONG_NOI, 'cmd67890'),
    ) as Record<string, string>;
    expect(offCmd).toEqual({
      device: 'bed1_quat',
      room: 'bed1',
      action: 'OFF',
      source: 'voice',
      id: 'cmd67890',
    });
  });
});

describe('DevicesScreen summary (1:1 voi DevicesScreen.kt tren Tablet)', () => {
  it('sap xep dung thu tu 6 phong tren luoi 3x2 va tong 18 o thiet bi', () => {
    const rooms = DemoData.rooms();
    const { dsPhong, tongDangBat, tongThietBi } = tomTatDanhSachPhong(
      rooms,
      createEmptyCongTacState(),
    );
    expect(dsPhong.map((p) => p.room.id)).toEqual([...THU_TU_PHONG]);
    expect(tongThietBi).toBe(18);
    expect(tongDangBat).toBe(9);
    expect(dsPhong.every((p) => !p.coCongTacThat && p.mucThietBi.length === 3)).toBe(true);
  });

  it('cong tac that mat ket noi hoac bridge chet thi hien "—" chu khong bia "Tắt"', () => {
    const bed1 = DemoData.rooms().find((r) => r.id === 'bed1')!;
    let ct = nenTang();
    ct = gopCongTac(
      ct,
      `${CongTacTopic.TIEN_TO_TB}bed1_den`,
      '{"online":false,"is_on":true,"ly_do_loi":"mat dien"}',
    );
    ct = gopCongTac(
      ct,
      `${CongTacTopic.TIEN_TO_TB}bed1_quat`,
      '{"online":true,"is_on":true}',
    );

    const tt = tomTatPhong(bed1, ct);
    expect(tt.coCongTacThat).toBe(true);
    expect(tt.mucThietBi[0].ten).toBe('Đèn');
    expect(tt.mucThietBi[0].roRang).toBe(false);
    expect(tt.mucThietBi[0].bat).toBe(false);
    expect(tt.mucThietBi[0].chuTrangThai).toBe('—');

    expect(tt.mucThietBi[1].ten).toBe('Quạt');
    expect(tt.mucThietBi[1].roRang).toBe(true);
    expect(tt.mucThietBi[1].bat).toBe(true);
    expect(tt.mucThietBi[1].chuTrangThai).toBe('Bật');

    // Bridge offline du retained van con is_on=true -> van hien "—"
    const bridgeDead = gopCongTac(ct, CongTacTopic.TRANG_THAI, 'offline');
    const ttDead = tomTatPhong(bed1, bridgeDead);
    expect(ttDead.mucThietBi[1].roRang).toBe(false);
    expect(ttDead.mucThietBi[1].chuTrangThai).toBe('—');
  });

  it('tom tat dung 3 o dac thu cua Nha bep va Hanh lang', () => {
    const rooms = DemoData.rooms();
    const kitchen = { ...rooms.find((r) => r.id === 'kitchen')!, fanOn: true, hoodSpeed: 2, stoveOn: true, stoveLevel: 6 };
    const hall = rooms.find((r) => r.id === 'hall')!;

    const kt = tomTatPhong(kitchen, createEmptyCongTacState());
    expect(kt.mucThietBi.map((m) => [m.ten, m.chuTrangThai])).toEqual([
      ['Đèn', 'Bật'],
      ['Hút mùi', 'Cấp 2'],
      ['Bếp từ', 'Mức 6'],
    ]);

    const hl = tomTatPhong(hall, createEmptyCongTacState());
    expect(hl.mucThietBi.map((m) => [m.ten, m.chuTrangThai])).toEqual([
      ['Đèn', '3/3 bật'],
      ['Khóa cửa', 'Đã khóa'],
      ['Camera', 'Bật'],
    ]);
  });
});

