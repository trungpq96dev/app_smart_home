import { describe, expect, it } from 'vitest';
import {
  createEmptyDienState,
  dinhDangDdMm,
  nhanThuNgayDayDu,
  nhanThuNgayNgan,
  trungBinhKwh,
  uocTinhTienNgay,
} from '../client/src/data/dien.js';
import { DEST_ITEMS } from '../client/src/data/models.js';
import { DemoData } from '../client/src/data/demoData.js';
import { tinhCanhBaoSidebar } from '../client/src/data/sidebarStatus.js';
import { SH } from '../client/src/theme/colors.js';

describe('Sidebar grouping & status indicators', () => {
  it('tach dung 2 cum HANG NGAY va HE THONG theo thu tu thiet ke', () => {
    const daily = DEST_ITEMS.filter((d) => d.group === 'DAILY').map((d) => d.route);
    const system = DEST_ITEMS.filter((d) => d.group === 'SYSTEM').map((d) => d.route);

    expect(daily).toEqual(['home', 'devices', 'camera', 'utilities']);
    expect(system).toEqual(['automation', 'sensors', 'stats', 'server', 'settings']);
  });

  it('tinhCanhBaoSidebar chi hien cham do/vang khi co su co that', () => {
    const clean = tinhCanhBaoSidebar({
      bomNuoc: null,
      ketNoiBom: { state: 'chua_cau_hinh' },
      mqttStatus: { state: 'chua_cau_hinh' },
      gatewayOnline: true,
      server: DemoData.initialServerState(),
    });
    expect(clean.automation).toBeUndefined();
    expect(clean.server).toBeUndefined();

    const lockedAndOffline = tinhCanhBaoSidebar({
      bomNuoc: {
        up: 100,
        fw: '6.26',
        hb: 30,
        rssi: -60,
        ssid: 'Home',
        ip: '192.168.0.120',
        aboveLow: 1,
        isFull: 0,
        lvl: 40,
        litres: 600,
        lvlRaw: 8000,
        lvlE: 2600,
        lvlF: 17500,
        tankOn: 0,
        tankMode: 'auto',
        tankRun: 0,
        fTank: 0,
        boostOn: 0,
        boostRun: 0,
        bstMode: 'off',
        bstLock: 0,
        fCity: null,
        useC: null,
        cityPulses: null,
        den: 0,
        locked: 1,
        fault: 'Dang khoa vi lan truoc bom chay can',
        temp: 28,
        dryCheck: 45,
        minRest: 2,
        maxRun: 90,
        pulses: 450,
        minFlow: 1,
        calPT: 900,
        calLT: 2,
        useM: '2026-11',
        useT: 500,
        temps: null,
        io: null,
      },
      ketNoiBom: { state: 'truc_tiep', kenh: 'LAN', tuoiGiay: 1 },
      mqttStatus: { state: 'loi', moTa: 'timeout' },
      gatewayOnline: true,
      server: DemoData.initialServerState(),
    });
    expect(lockedAndOffline.automation).toBe(SH.Red);
    expect(lockedAndOffline.server).toBe(SH.Red);

    const staleAndExited = tinhCanhBaoSidebar({
      bomNuoc: null,
      ketNoiBom: { state: 'so_cu', tuoiGiay: 60 },
      mqttStatus: { state: 'da_noi' },
      gatewayOnline: true,
      server: {
        ...DemoData.initialServerState(),
        isRealData: true,
        metric: { ...DemoData.initialServerState().metric, cpuTempC: 55 },
        containers: [
          {
            name: 'mosquitto',
            image: 'eclipse-mosquitto',
            state: 'exited',
            status: 'Exited',
            icon: 'hub',
            cpuPercent: null,
            memMb: null,
            memLimitMb: null,
            memPercent: null,
            protected: true,
            controllable: false,
          },
        ],
      },
    });
    expect(staleAndExited.automation).toBe(SH.Amber);
    expect(staleAndExited.server).toBe(SH.Amber);
  });
});

describe('Energy chart helpers', () => {
  it('tinh trung binh, uoc tinh chi phi theo don gia that va dinh dang thu ngay', () => {
    const slice = [
      { ngay: '2026-11-08', kwh: 8.4 },
      { ngay: '2026-11-09', kwh: 9.2 },
      { ngay: '2026-11-10', kwh: 12.8 },
    ];
    expect(trungBinhKwh([])).toBeNull();
    expect(trungBinhKwh(slice)).toBeCloseTo(10.1333, 3);

    const dienNoMonth = createEmptyDienState();
    expect(uocTinhTienNgay(12.8, dienNoMonth)).toBeNull();

    const dienWithMonth = {
      ...dienNoMonth,
      thangSanLuong: 200,
      thangTien: 500_000,
    };
    expect(uocTinhTienNgay(12.8, dienWithMonth)).toBeCloseTo(32_000, 1);

    expect(nhanThuNgayNgan('2026-11-08')).toBe('CN');
    expect(nhanThuNgayNgan('2026-11-10')).toBe('T3');
    expect(nhanThuNgayDayDu('2026-11-08')).toBe('Chủ nhật');
    expect(nhanThuNgayDayDu('2026-11-10')).toBe('Thứ 3');
    expect(dinhDangDdMm('2026-11-10')).toBe('10/11');
  });
});

