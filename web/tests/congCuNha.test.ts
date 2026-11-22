import { describe, expect, it } from 'vitest';
import { CongCuNha, LoiLenh, type HanhDongNha } from '../client/src/data/giongnoi/congCuNha.js';

function createNhaGia() {
  const daGoi: string[] = [];
  const den: Record<string, boolean> = { living: true, bed1: false };
  const oNhanh: Record<string, boolean> = { light: false, curtain: false };
  const thatCua = new Set<string>();
  const khongRo = new Set<string>();
  const nemKhiDat: Record<string, string> = {};
  let nhietDo = 26;
  let manDangMo: string | null = null;
  let soLanGuiMqtt = 0;

  const nha: HanhDongNha = {
    maPhong: () => ['living', 'bed1'],
    tenPhong: (ma) => (ma === 'living' ? 'Phòng khách' : ma === 'bed1' ? 'Phòng ngủ 1' : null),
    datDen: (phong, bat) => {
      const err = nemKhiDat[`${phong}:den`];
      if (err) throw new LoiLenh(err);
      den[phong] = bat;
      daGoi.push(`datDen(${phong},${bat})`);
    },
    datQuat: (phong, bat) => {
      const err = nemKhiDat[`${phong}:quat`];
      if (err) throw new LoiLenh(err);
      daGoi.push(`datQuat(${phong},${bat})`);
    },
    datDieuHoa: (phong, bat) => daGoi.push(`datDieuHoa(${phong},${bat})`),
    datLichHen: (phong, bat) => daGoi.push(`datLichHen(${phong},${bat})`),
    datTv: (phong, bat) => daGoi.push(`datTv(${phong},${bat})`),
    datRem: (phong, mo) => daGoi.push(`datRem(${phong},${mo})`),
    datBepTu: (phong, bat) => daGoi.push(`datBepTu(${phong},${bat})`),
    datNhietDo: (phong, doC) => {
      nhietDo = Math.min(30, Math.max(16, doC));
      daGoi.push(`datNhietDo(${phong},${doC})`);
      return nhietDo;
    },
    chinhNhietDo: (phong, buoc) => {
      nhietDo = Math.min(30, Math.max(16, nhietDo + buoc));
      daGoi.push(`chinhNhietDo(${phong},${buoc})`);
      return nhietDo;
    },
    datCheDo: (phong, cheDo) => daGoi.push(`datCheDo(${phong},${cheDo})`),
    datONhanh: (id, bat) => {
      oNhanh[id] = bat;
      daGoi.push(`datONhanh(${id},${bat})`);
    },
    laThat: (phong, tb) => thatCua.has(`${phong}:${tb}`),
    thietBiThat: () => Array.from(thatCua).map((x) => `Thiết bị ${x}`),
    dangBat: (phong, tb) => {
      if (khongRo.has(`${phong}:${tb}`)) return null;
      if (phong === 'nhanh') return oNhanh[tb] ?? null;
      if (tb === 'den') return den[phong] ?? null;
      return null;
    },
    moManHinh: (dich) => {
      const ok = dich === 'camera' || dich === 'thong_ke' || dich === 'phong:living';
      if (ok) manDangMo = dich;
      return ok;
    },
    docSoLieu: (muc) => ({ co_so: true, muc }),
    chupTrangThai: () => '- Phòng khách: đèn bật',
    guiMqtt: () => {
      soLanGuiMqtt++;
      return true;
    },
  };

  return {
    nha,
    daGoi,
    den,
    oNhanh,
    thatCua,
    khongRo,
    nemKhiDat,
    getNhietDo: () => nhietDo,
    getManDangMo: () => manDangMo,
    getSoLanGuiMqtt: () => soLanGuiMqtt,
  };
}

describe('CongCuNha', () => {
  it('bat den mot phong va khong dao khi dang bat', () => {
    const ctx = createNhaGia();
    const r = new CongCuNha(ctx.nha).chay(CongCuNha.DIEU_KHIEN, {
      phong: 'living',
      thiet_bi: 'den',
      hanh_dong: 'bat',
    });
    expect(r.ok).toBe(true);
    expect(ctx.den.living).toBe(true);
  });

  it('dao den theo trang thai hien tai', () => {
    const ctx = createNhaGia();
    const cc = new CongCuNha(ctx.nha);
    cc.chay(CongCuNha.DIEU_KHIEN, { phong: 'living', thiet_bi: 'den', hanh_dong: 'dao' });
    expect(ctx.den.living).toBe(false);
    cc.chay(CongCuNha.DIEU_KHIEN, { phong: 'living', thiet_bi: 'den', hanh_dong: 'dao' });
    expect(ctx.den.living).toBe(true);
  });

  it('kep nhiet do 16..30 va bao loi neu thieu so', () => {
    const ctx = createNhaGia();
    const cc = new CongCuNha(ctx.nha);
    const bad = cc.chay(CongCuNha.DIEU_KHIEN, {
      phong: 'living',
      thiet_bi: 'nhiet_do',
      hanh_dong: 'dat',
    });
    expect(bad.ok).toBe(false);

    const clamped = cc.chay(CongCuNha.DIEU_KHIEN, {
      phong: 'living',
      thiet_bi: 'nhiet_do',
      hanh_dong: 'dat',
      gia_tri: '99',
    });
    expect(clamped.ok).toBe(true);
    expect(ctx.getNhietDo()).toBe(30);
  });

  it('phan biet that vs mau va tu choi dao cong tac that mat ket noi', () => {
    const ctx = createNhaGia();
    ctx.thatCua.add('bed1:den');
    const cc = new CongCuNha(ctx.nha, true);

    const rThat = cc.chay(CongCuNha.DIEU_KHIEN, {
      phong: 'bed1',
      thiet_bi: 'den',
      hanh_dong: 'bat',
    });
    expect(rThat.ok).toBe(true);
    if (rThat.ok && 'that' in rThat) {
      expect(rThat.that).toBe(true);
      expect(rThat.ket_qua).toContain('đã gửi lệnh');
    }
    // Thiet bi that khong gui trung lap lan 2 qua guiMqtt
    expect(ctx.getSoLanGuiMqtt()).toBe(0);

    // Tat ca co 1 mau -> that = false
    const rAll = cc.chay(CongCuNha.DIEU_KHIEN, {
      phong: 'tat_ca',
      thiet_bi: 'den',
      hanh_dong: 'bat',
    });
    if (rAll.ok && 'that' in rAll) {
      expect(rAll.that).toBe(false);
    }

    ctx.khongRo.add('bed1:den');
    const rDao = cc.chay(CongCuNha.DIEU_KHIEN, {
      phong: 'bed1',
      thiet_bi: 'den',
      hanh_dong: 'dao',
    });
    expect(rDao.ok).toBe(false);
  });

  it('mo man hinh va doc so lieu', () => {
    const ctx = createNhaGia();
    const cc = new CongCuNha(ctx.nha);
    const m1 = cc.chay(CongCuNha.MO_MAN_HINH, { dich: 'camera' });
    expect(m1.ok).toBe(true);
    expect(ctx.getManDangMo()).toBe('camera');

    const d1 = cc.chay(CongCuNha.DOC_SO_LIEU, { muc: 'cong_tac' });
    expect(d1.ok).toBe(true);
    const dBad = cc.chay(CongCuNha.DOC_SO_LIEU, { muc: 'gia_vang' });
    expect(dBad.ok).toBe(false);
  });
});

