import { describe, expect, it } from 'vitest';
import {
  createEmptyCuktechState,
  cuktechCoMauThuan,
  cuktechCongBiTat,
  cuktechDanhSachCong,
  cuktechLaSoCu,
  cuktechMatKetNoiThat,
  cuktechSoCongDangChay,
  cuktechSongTotNhat,
  cuktechTongOat,
  cuktechTyLePhanBo,
  CuktechPiid,
  CuktechTopic,
  gopCuktech,
  KICH_BAN_LIST,
  mucSongCua,
  nhanDbmSong,
  nhanGiaoThucCong,
  songLaSoCu,
  type SongBle,
} from '../client/src/data/cuktech.js';

const goiC1Dangsac = JSON.stringify({
  voltage: 20.1,
  current: 2.3,
  power: 46.2,
  active: true,
  status_raw: 1,
  protocol: 'PD',
  _raw_code: '0x03',
  _proto_num: 7,
  enabled: true,
});

const goiCongTrong = JSON.stringify({
  voltage: 0.0,
  current: 0.0,
  power: 0.0,
  active: false,
  protocol: 'idle',
});

const goiStatus = JSON.stringify({
  connected: true,
  authenticated: true,
  device_model: 'njcuk.fitting.ad1204_HS6621C',
  firmware_version: '2.1.2_0073',
  ports: {
    '1': { voltage: 20.1, current: 2.3, power: 46.2, active: true, protocol: 'PD', enabled: true },
    '2': { voltage: 0.0, current: 0.0, power: 0.0, active: false, protocol: 'idle', enabled: true },
    '3': { voltage: 0.0, current: 0.0, power: 0.0, active: false, protocol: 'idle', enabled: true },
    '4': { voltage: 5.1, current: 0.3, power: 1.5, active: true, protocol: '5V', enabled: true },
  },
});

const goiCaiDat = JSON.stringify({
  '21': 50532111,
  '5': 1,
  '6': 2,
  '8': 0,
  '9': 0,
  '10': 0,
  '11': 0,
  '12': 0,
  '13': 0,
  '15': 0,
  '16': 15,
  '17': 120391469,
  '18': 522,
  '19': 1,
  '20': 0,
});

describe('CuktechState', () => {
  it('trang thai rong khong bia so', () => {
    const s = createEmptyCuktechState();
    expect(s.coSoThat).toBe(false);
    expect(cuktechTongOat(s)).toBe(0);
    expect(cuktechSoCongDangChay(s)).toBe(0);
    expect(cuktechDanhSachCong(s).length).toBe(4);
    expect(cuktechDanhSachCong(s).every((c) => !c.dangChay)).toBe(true);
  });

  it('cong trong thieu enabled thi giu null', () => {
    const s = gopCuktech(createEmptyCuktechState(), `${CuktechTopic.TIEN_TO_CONG}c2`, goiCongTrong);
    const c2 = s.cong.C2!;
    expect(c2.dangChay).toBe(false);
    expect(c2.choPhep).toBeNull();
    expect(nhanGiaoThucCong(c2)).toBe('Trống');
  });

  it('status cho luon ca bon cong va tinh tong cong suat + ty le', () => {
    const s = gopCuktech(createEmptyCuktechState(), CuktechTopic.STATUS, goiStatus);
    expect(s.noiDuoc).toBe(true);
    expect(s.xacThuc).toBe(true);
    expect(cuktechTongOat(s)).toBeCloseTo(47.7, 2);
    expect(cuktechSoCongDangChay(s)).toBe(2);

    const tyLe = cuktechTyLePhanBo(s);
    expect(tyLe.C1).toBeCloseTo(96.8, 0);
    expect(tyLe.USB_A).toBeCloseTo(3.1, 0);
    expect(tyLe.C2).toBeUndefined();
  });

  it('giai duoc bang PIID va mat na bit PIID 16', () => {
    const s = gopCuktech(createEmptyCuktechState(), CuktechTopic.SETTINGS, goiCaiDat);
    expect(s.caiDat?.kichBan?.nhan).toBe('AI');
    expect(s.caiDat?.thoiGianSangMan).toBe('10 phút');
    expect(s.caiDat?.tuTatManNhanRoi).toBe(true);
    expect(s.caiDat?.khoaXoayMan).toBe(false);
    expect(s.caiDat?.sacNhoGiotUsbA).toBe(false);
    expect(cuktechCongBiTat(s, 'C1')).toBe(false);

    const tatC2 = gopCuktech(createEmptyCuktechState(), CuktechTopic.SETTINGS, '{"16":13}');
    expect(cuktechCongBiTat(tatC2, 'C1')).toBe(false);
    expect(cuktechCongBiTat(tatC2, 'C2')).toBe(true);
    expect(cuktechCongBiTat(tatC2, 'C3')).toBe(false);
    expect(cuktechCongBiTat(tatC2, 'USB_A')).toBe(false);
  });

  it('phan biet status connected:false cu vs mat ket noi that', () => {
    const t0 = 1_000_000;
    let s = gopCuktech(createEmptyCuktechState(), CuktechTopic.STATUS, '{"connected":false}', t0);
    s = gopCuktech(s, `${CuktechTopic.TIEN_TO_CONG}c1`, goiC1Dangsac, t0 + 5_000);
    expect(cuktechMatKetNoiThat(s, t0 + 6_000)).toBe(false);
    expect(cuktechCoMauThuan(s, t0 + 6_000)).toBe(true);

    const sau = t0 + 70_000;
    expect(cuktechLaSoCu(s, sau)).toBe(true);
    expect(cuktechMatKetNoiThat(s, sau)).toBe(true);
  });

  it('doc duoc RSSI, xoa khi payload rong, va chon diem manh nhat', () => {
    const rssiServer = JSON.stringify({
      rssi: -96,
      tuoi_giay: 24,
      dang_noi: null,
      nguon: 'serverubuntu (dongle USB)',
    });
    let st = gopCuktech(createEmptyCuktechState(), 'cuktech/rssi/server', rssiServer);
    expect(st.coSoThat).toBe(false);
    expect(st.song.server.rssi).toBe(-96);
    expect(mucSongCua(st.song.server)).toBe('RAT_YEU');
    expect(songLaSoCu(st.song.server)).toBe(false);

    st = gopCuktech(st, 'cuktech/rssi/esp', '{"rssi":-52,"nguon":"ESP32-C3"}');
    expect(cuktechSongTotNhat(st)?.rssi).toBe(-52);
    expect(mucSongCua(cuktechSongTotNhat(st)!)).toBe('TOT');

    st = gopCuktech(st, 'cuktech/rssi/server', '');
    expect(Object.keys(st.song)).toEqual(['esp']);

    const khongRo: SongBle = { nguonId: 'x', rssi: null, nguonTen: null, tuoiGiay: null, dangNoi: null };
    expect(mucSongCua(khongRo)).toBe('KHONG_RO');
    expect(nhanDbmSong(khongRo)).toBe('—');
  });

  it('cac gia tri PIID tren man hinh nam trong khoang hop le', () => {
    for (const [ma] of CuktechPiid.SANG_MAN_CHON) expect(ma >= 0 && ma <= 5).toBe(true);
    for (const [phut] of CuktechPiid.HEN_GIO_CHON) expect(phut >= 0 && phut <= 1440).toBe(true);
    for (const kb of KICH_BAN_LIST) expect(kb.ma >= 1 && kb.ma <= 4).toBe(true);
    expect(CuktechPiid.henGio('C1')).toBe(9);
    expect(CuktechPiid.henGio('USB_A')).toBe(12);
  });
});

