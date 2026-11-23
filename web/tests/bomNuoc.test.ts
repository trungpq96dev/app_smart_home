import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { WebSocket } from 'ws';
import { BomNuocBridge } from '../server/bomNuocBridge.js';
import {
  bomTrucTuyen,
  coTheDieuKhienBom,
  type BomNuocState,
  type TrangThaiKetNoiBom,
} from '../client/src/data/bomnuoc.js';
import {
  coiDangKeu,
  dinhDangQueryBaoVe,
  kiemTraBaoVe,
  lamTronBaoVe,
  mucNuocLit,
  mucNuocPhanTram,
  NhatKyBomNuocStore,
  nhietDoNuoc,
  parseBomNuocState,
  parseLuongNuocThang,
  phanTichLoiBomNuoc,
  wifiRssi,
} from '../server/bomNuocModel.js';

const tempDirs: string[] = [];

afterEach(() => {
  while (tempDirs.length > 0) {
    const dir = tempDirs.pop();
    if (dir && fs.existsSync(dir)) {
      fs.rmSync(dir, { recursive: true, force: true });
    }
  }
});

describe('bomNuocModel parsing & sentinels', () => {
  it('parse goi state day du va doc dung cam bien/GPIO 5', () => {
    const raw = JSON.stringify({
      up: 3725,
      fw: '6.261111.2',
      hb: 30,
      rssi: -58,
      ssid: 'Nha_5G',
      ip: '192.168.0.120',
      aboveLow: 0,
      isFull: 0,
      lvl: 72.4,
      litres: 1086.0,
      lvlRaw: 13400,
      lvlE: 2600,
      lvlF: 17500,
      tankOn: 1,
      tankMode: 'auto',
      tankRun: 145,
      fTank: 18.6,
      boostOn: 0,
      boostRun: 0,
      bstMode: 'off',
      bstLock: 0,
      fCity: 4.2,
      useC: 320.0,
      cityPulses: 450.0,
      den: 1,
      locked: 0,
      fault: '',
      temp: 27.5,
      dryCheck: 45,
      minRest: 2.0,
      maxRun: 90,
      pulses: 450.0,
      minFlow: 1.0,
      calPT: 900,
      calLT: 2.0,
      useM: '2026-11',
      useT: 1450.0,
      temps: [
        { n: 'Nhiệt độ nước', v: 27.5 },
        { n: 'Bơm lên bồn', v: 41.2 },
        { n: 'Bơm áp', v: -127.0 },
      ],
      io: [
        { n: 'Còi', g: 5, v: 1 },
        { n: 'Bơm', g: 19, v: 1 },
      ],
    });

    const s = parseBomNuocState(raw);
    expect(s).not.toBeNull();
    expect(s!.fw).toBe('6.261111.2');
    expect(mucNuocPhanTram(s!)).toBeCloseTo(72.4, 2);
    expect(mucNuocLit(s!)).toBeCloseTo(1086.0, 2);
    expect(nhietDoNuoc(s!)).toBeCloseTo(27.5, 2);
    expect(wifiRssi(s!)).toBe(-58);
    expect(coiDangKeu(s!)).toBe(true);
    expect(s!.temps?.[2]?.giaTriTho).toBe(-127.0);
  });

  it('loc gia tri sentinel -1 / -127 / rssi=0, tuyet doi khong bia so', () => {
    const raw = JSON.stringify({
      rssi: 0,
      lvl: -1,
      litres: -1,
      temp: -127,
      isFull: 1,
      tankOn: 0,
      tankMode: 'off',
    });
    const s = parseBomNuocState(raw);
    expect(s).not.toBeNull();
    expect(wifiRssi(s!)).toBeNull();
    expect(mucNuocPhanTram(s!)).toBeNull();
    expect(mucNuocLit(s!)).toBeNull();
    expect(nhietDoNuoc(s!)).toBeNull();
    expect(coiDangKeu(s!)).toBeNull();
    expect(s!.isFull).toBe(1);
    expect(s!.fCity).toBeNull();
    expect(s!.den).toBeNull();
  });

  it('parse luong nuoc theo thang gom ca nhanh nuoc may va thang chua ro', () => {
    const raw = JSON.stringify([
      { m: '2026-11', t: 1450.0, c: 320.0 },
      { m: 'chua ro thang', t: 200.0 },
    ]);
    const list = parseLuongNuocThang(raw);
    expect(list).toHaveLength(2);
    expect(list![0]).toEqual({ thang: '2026-11', bomLit: 1450.0, nuocMayLit: 320.0 });
    expect(list![1]).toEqual({ thang: 'chua ro thang', bomLit: 200.0, nuocMayLit: null });
  });

  it('dich fault khong dau giu nguyen so va giu nguyen loi la', () => {
    const f1 = phanTichLoiBomNuoc('Bom len bon chay qua 90 phut ma chua day');
    expect(f1?.noiDung).toContain('90 phút');
    expect(f1?.mucDo).toBe('NGUY_HIEM');

    const f2 = phanTichLoiBomNuoc('Bom ap qua nong (72.5 do C)');
    expect(f2?.noiDung).toContain('72.5 °C');
    expect(f2?.mucDo).toBe('NGUY_HIEM');

    const f3 = phanTichLoiBomNuoc('Bom ap dang khoa vi su co lan truoc');
    expect(f3?.mucDo).toBe('CANH_BAO');

    const f4 = phanTichLoiBomNuoc('Loi phan cung la');
    expect(f4?.noiDung).toBe('Loi phan cung la');
    expect(f4?.mucDo).toBe('NGUY_HIEM');

    expect(phanTichLoiBomNuoc('   ')).toBeNull();
  });

  it('kiem tra va lam tron tham so bao ve dung khoang NVS', () => {
    expect(kiemTraBaoVe(45, 1.0, 90, 2.0, 450.0)).toBeNull();
    expect(kiemTraBaoVe(2, 1.0, 90, 2.0, 450.0)).not.toBeNull();
    expect(kiemTraBaoVe(45, 0.01, 90, 2.0, 450.0)).not.toBeNull();

    const r = lamTronBaoVe(45, 2.36, 90, 450.14, 1.256);
    expect(r.minRest).toBe(2.4);
    expect(r.pulses).toBe(450.1);
    expect(r.minFlow).toBe(1.26);
    expect(dinhDangQueryBaoVe(r)).toBe(
      'cfgDry=45&cfgRest=2.4&cfgRun=90&cfgPulses=450.1&cfgMinFlow=1.26',
    );
  });
});

describe('NhatKyBomNuocStore', () => {
  it('bo qua lan dau, ghi khi doi trang thai va nap lai tu JSON', () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-bom-log-'));
    tempDirs.push(dir);
    const logPath = path.join(dir, 'nhat_ky.json');

    const store = new NhatKyBomNuocStore(logPath);
    const now = Date.now();
    const base = parseBomNuocState(
      JSON.stringify({ tankOn: 0, tankMode: 'auto', boostOn: 0, isFull: 0, fault: '' }),
    )!;

    // Quan sat dau -> khong ghi su kien gia
    expect(store.quanSat(base, now - 20_000)).toHaveLength(0);
    expect(store.getDanhSach()).toHaveLength(0);

    // Doi sang chay + loi
    const next = { ...base, tankOn: 1, fault: 'Bom len bon qua nong (71 do C)' };
    const added = store.quanSat(next, now - 10_000);
    expect(added).toHaveLength(2);
    expect(store.getDanhSach().some((e) => e.nhom === 'pump' && e.tieuDe.includes('bắt đầu chạy'))).toBe(true);
    expect(store.getDanhSach().some((e) => e.nhom === 'alarm' && e.tieuDe.includes('71 °C'))).toBe(true);

    // Nap lai tu dia
    const reloaded = new NhatKyBomNuocStore(logPath);
    expect(reloaded.getDanhSach()).toHaveLength(2);

    reloaded.xoaTatCa();
    expect(reloaded.getDanhSach()).toHaveLength(0);
    expect(fs.existsSync(logPath)).toBe(false);
  });
});

describe('BomNuocBridge HTTP LAN + CommandBus + WS', () => {
  it('poll /api/state + /api/usage, gui lenh co xac nhan, phat hien khoa bao ve va day WS snapshot', async () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-bom-bridge-'));
    tempDirs.push(dir);

    let espState: Record<string, unknown> = {
      up: 100,
      fw: '6.261111.2',
      hb: 30,
      rssi: -55,
      isFull: 0,
      lvl: 60,
      litres: 900,
      tankOn: 0,
      tankMode: 'off',
      boostOn: 0,
      bstMode: 'off',
      bstLock: 0,
      locked: 0,
      fault: '',
      temp: 26.0,
      dryCheck: 30,
      minRest: 2.0,
      maxRun: 60,
      pulses: 450,
      minFlow: 1.0,
      calPT: 0,
      calLT: 0,
      useM: '2026-11',
      useT: 500,
    };
    const seenCmds: string[] = [];

    const mockEsp = http.createServer((req, res) => {
      const u = new URL(req.url || '/', 'http://127.0.0.1');
      if (u.pathname === '/api/state') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(espState));
        return;
      }
      if (u.pathname === '/api/usage') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify([{ m: '2026-11', t: 500 }]));
        return;
      }
      if (u.pathname === '/api/cmd') {
        seenCmds.push(u.search);
        const tank = u.searchParams.get('tank');
        if (tank) {
          espState = { ...espState, tankMode: tank, tankOn: tank === 'off' ? 0 : 1 };
        }
        const booster = u.searchParams.get('booster');
        if (booster === 'on') {
          // Gia lap bo dang khoa bao ve: nhan bstMode=on nhung boostOn=0 va bstLock=1
          espState = { ...espState, bstMode: 'on', boostOn: 0, bstLock: 1 };
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(espState));
        return;
      }
      res.writeHead(404);
      res.end();
    });

    await new Promise<void>((resolve) => mockEsp.listen(0, '127.0.0.1', resolve));
    const espAddr = mockEsp.address() as { port: number };

    const bridge = new BomNuocBridge(
      {
        espHost: `127.0.0.1:${espAddr.port}`,
        uuTienLan: true,
        brokerHost: '',
        brokerPort: 8883,
        brokerTls: true,
        brokerUser: '',
        brokerPass: '',
      },
      {
        logPath: path.join(dir, 'log.json'),
        pollFastMs: 80,
        pollSlowMs: 200,
        pollUsageMs: 5000,
        lanProbeMs: 5000,
        cmdTimeoutMs: 1500,
      },
    );

    const wsHostServer = http.createServer((_req, res) => {
      res.statusCode = 404;
      res.end();
    });
    wsHostServer.on('upgrade', (req, socket, head) => {
      bridge.handleUpgrade(req, socket, head);
    });
    await new Promise<void>((resolve) => wsHostServer.listen(0, '127.0.0.1', resolve));
    const wsAddr = wsHostServer.address() as { port: number };

    let ws: WebSocket | null = null;
    try {
      // Cho probe LAN dau tien
      await new Promise((r) => setTimeout(r, 120));
      expect(bridge.getKenh()).toBe('LAN');
      expect(bridge.getLink()).toEqual({ state: 'truc_tiep', kenh: 'LAN', tuoiGiay: 0 });
      expect(bridge.getSnapshot().state?.fw).toBe('6.261111.2');

      // Kiem tra WS snapshot
      ws = new WebSocket(`ws://127.0.0.1:${wsAddr.port}/ws/bomnuoc`);
      const firstFrame = await new Promise<Record<string, unknown>>((resolve, reject) => {
        const t = setTimeout(() => reject(new Error('WS snapshot timeout')), 2000);
        ws!.once('message', (data) => {
          clearTimeout(t);
          resolve(JSON.parse(data.toString()) as Record<string, unknown>);
        });
      });
      expect(firstFrame.type).toBe('snapshot');
      expect((firstFrame.state as { fw?: string })?.fw).toBe('6.261111.2');

      // Gui lenh doi che do bon -> xac nhan ngay tu /api/cmd
      const rTank = await bridge.executeCommand({ kind: 'tank', mode: 'auto' });
      expect(rTank.ok).toBe(true);
      expect(rTank.status).toBe('da_xac_nhan');
      expect(seenCmds).toContain('?tank=auto');

      // Gui lenh bom tang ap khi bstLock=1 -> phai bao bi_khoa_bao_ve
      const rBoost = await bridge.executeCommand({ kind: 'booster', on: true });
      expect(rBoost.ok).toBe(false);
      expect(rBoost.status).toBe('bi_khoa_bao_ve');

      // Gui tham so bao ve ngoai khoang -> khong hop le ngay lap tuc
      const rBad = await bridge.executeCommand({
        kind: 'protection',
        dryCheck: 1,
        minRest: 2,
        maxRun: 60,
        pulses: 450,
        minFlow: 1,
      });
      expect(rBad.ok).toBe(false);
      expect(rBad.status).toBe('khong_hop_le');
    } finally {
      ws?.close();
      bridge.destroy();
      await new Promise<void>((resolve) => wsHostServer.close(() => resolve()));
      await new Promise<void>((resolve) => mockEsp.close(() => resolve()));
    }
  });
});

describe('coTheDieuKhienBom & bomTrucTuyen', () => {
  it('chi cho phep dieu khien khi da co state va link truc_tiep', () => {
    const sampleState = parseBomNuocState(
      JSON.stringify({ tankOn: 0, tankMode: 'auto', boostOn: 0, bstMode: 'off' }),
    ) as unknown as BomNuocState;

    const lan: TrangThaiKetNoiBom = { state: 'truc_tiep', kenh: 'LAN', tuoiGiay: 0 };
    const mqtt: TrangThaiKetNoiBom = { state: 'truc_tiep', kenh: 'MQTT', tuoiGiay: 4 };

    expect(bomTrucTuyen(lan)).toBe(true);
    expect(bomTrucTuyen(mqtt)).toBe(true);
    expect(coTheDieuKhienBom(sampleState, lan)).toBe(true);
    expect(coTheDieuKhienBom(sampleState, mqtt)).toBe(true);

    expect(coTheDieuKhienBom(null, lan)).toBe(false);
    expect(coTheDieuKhienBom(null, mqtt)).toBe(false);

    const offlineLinks: TrangThaiKetNoiBom[] = [
      { state: 'chua_cau_hinh' },
      { state: 'dang_noi' },
      { state: 'chi_co_broker' },
      { state: 'so_cu', tuoiGiay: 45 },
      { state: 'bo_offline' },
      { state: 'mat_broker' },
      { state: 'sai_tai_khoan' },
      { state: 'loi', lyDo: 'timeout' },
    ];

    for (const link of offlineLinks) {
      expect(bomTrucTuyen(link)).toBe(false);
      expect(coTheDieuKhienBom(sampleState, link)).toBe(false);
    }
  });
});
