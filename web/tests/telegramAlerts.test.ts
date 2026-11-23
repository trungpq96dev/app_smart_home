import { afterEach, describe, expect, it } from 'vitest';
import type { BomNuocState } from '../server/bomNuocModel.js';
import type { TelegramConfig } from '../server/configStore.js';
import {
  kiemTraDienTangVot,
  parseGasSensorPayload,
  TelegramAlertService,
} from '../server/telegramAlerts.js';

function baseTelegramConfig(overrides: Partial<TelegramConfig> = {}): TelegramConfig {
  return {
    enabled: true,
    botToken: '12345:fake-token',
    chatId: '-100999',
    offlineMinutes: 5,
    electricSpikePct: 40,
    electricSpikeMinKwh: 3,
    gasThresholdPpm: 200,
    cooldownMinutes: 30,
    pirTopic: 'smarthome/sensor/pir',
    gasTopic: 'smarthome/sensor/gas',
    ...overrides,
  };
}

function makePumpState(overrides: Partial<BomNuocState> = {}): BomNuocState {
  return {
    up: 100,
    fw: '5.1.0',
    hb: 5,
    rssi: -55,
    ssid: 'Home',
    ip: '192.168.0.120',
    aboveLow: 1,
    isFull: 0,
    lvl: 62,
    litres: 930,
    lvlRaw: 2100,
    lvlE: 1000,
    lvlF: 3000,
    tankOn: 0,
    tankMode: 'auto',
    tankRun: 0,
    fTank: 0,
    boostOn: 0,
    boostRun: 0,
    bstMode: 'off',
    bstLock: 0,
    fCity: 0,
    useC: 0,
    cityPulses: 0,
    den: 0,
    locked: 0,
    fault: '',
    temp: 28,
    dryCheck: 30,
    minRest: 5,
    maxRun: 30,
    pulses: 450,
    minFlow: 1.5,
    calPT: 0,
    calLT: 0,
    useM: '2026-11',
    useT: 5000,
    temps: null,
    io: null,
    ...overrides,
  };
}

describe('TelegramAlertService', () => {
  const services: TelegramAlertService[] = [];

  afterEach(() => {
    while (services.length > 0) {
      services.pop()?.destroy();
    }
  });

  it('gui canh bao Telegram khi bom khoa chay can / qua nhiet va gui tin phuc hoi khi xoa loi', async () => {
    const sentTexts: string[] = [];
    let now = 1_700_000_000_000;
    const svc = new TelegramAlertService(() => baseTelegramConfig(), {
      nowFn: () => now,
      watchdogIntervalMs: 0,
      fetchFn: (async (_url, init) => {
        const body = JSON.parse(String(init?.body || '{}')) as { text?: string };
        sentTexts.push(body.text || '');
        return new Response('{"ok":true}', { status: 200 });
      }) as typeof fetch,
    });
    services.push(svc);

    svc.observeBomNuocState(
      makePumpState({
        locked: 1,
        fault: 'Bom len bon chay nhung khong co nuoc',
      }),
    );
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(1);
    expect(sentTexts[0]).toContain('CẢNH BÁO BƠM NƯỚC KHẨN');
    expect(sentTexts[0]).toContain('không có nước');

    // Lap lai lien tuc trong thoi gian cooldown -> khong spam
    now += 60_000;
    svc.observeBomNuocState(
      makePumpState({
        locked: 1,
        fault: 'Bom len bon chay nhung khong co nuoc',
      }),
    );
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(1);

    // Xoa loi -> gui thong bao phuc hoi
    svc.observeBomNuocState(makePumpState({ locked: 0, bstLock: 0, fault: '' }));
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(2);
    expect(sentTexts[1]).toContain('đã mở khóa');

    // Qua nhiet 72 do C
    svc.observeBomNuocState(
      makePumpState({
        locked: 1,
        fault: 'Bom len bon qua nong (72 do C)',
        temps: [{ ten: 'Nuoc', giaTriTho: 29 }, { ten: 'Bom', giaTriTho: 72 }],
      }),
    );
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(3);
    expect(sentTexts[2]).toContain('72');
  });

  it('gui canh bao khan khi phat hien ro ri Gas qua MQTT va phuc hoi khi an toan', async () => {
    const sentTexts: string[] = [];
    const svc = new TelegramAlertService(() => baseTelegramConfig(), {
      watchdogIntervalMs: 0,
      fetchFn: (async (_url, init) => {
        const body = JSON.parse(String(init?.body || '{}')) as { text?: string };
        sentTexts.push(body.text || '');
        return new Response('{"ok":true}', { status: 200 });
      }) as typeof fetch,
    });
    services.push(svc);

    expect(parseGasSensorPayload('{"ppm":380}', 200)).toEqual({ alert: true, ppm: 380 });
    expect(parseGasSensorPayload('safe', 200)).toEqual({ alert: false, ppm: null });

    svc.observeMqttMessage('smarthome/sensor/gas', '{"ppm":380,"alert":true}');
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(1);
    expect(sentTexts[0]).toContain('RÒ RỈ GAS');
    expect(sentTexts[0]).toContain('380 ppm');

    svc.observeMqttMessage('smarthome/sensor/gas', '{"ppm":25,"alert":false}');
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(2);
    expect(sentTexts[1]).toContain('an toàn');
  });

  it('canh bao Server va Router rot mang qua 5 phut va phuc hoi khi online lai', async () => {
    const sentTexts: string[] = [];
    let now = 1_700_000_000_000;
    const svc = new TelegramAlertService(() => baseTelegramConfig({ offlineMinutes: 5 }), {
      nowFn: () => now,
      watchdogIntervalMs: 0,
      fetchFn: (async (_url, init) => {
        const body = JSON.parse(String(init?.body || '{}')) as { text?: string };
        sentTexts.push(body.text || '');
        return new Response('{"ok":true}', { status: 200 });
      }) as typeof fetch,
    });
    services.push(svc);

    svc.observeMqttMessage('server/capnhat', '2026-11-23 10:00:00');
    svc.observeMqttMessage('wifi/info', JSON.stringify({ is_connected: false }));

    // 4 phut 59 giay -> chua duoc gui canh bao
    now += 4 * 60_000 + 59_000;
    await svc.checkWatchdog();
    expect(sentTexts).toHaveLength(0);

    // Qua 5 phut -> ban canh bao cho ca Server va Router
    now += 2_000;
    await svc.checkWatchdog();
    expect(sentTexts).toHaveLength(2);
    expect(sentTexts.some((t) => t.includes('SERVER RỚT MẠNG'))).toBe(true);
    expect(sentTexts.some((t) => t.includes('ROUTER'))).toBe(true);

    // Online tro lai -> gui tin phuc hoi
    svc.observeMqttMessage('server/capnhat', '2026-11-23 10:06:00');
    svc.observeMqttMessage('wifi/info', JSON.stringify({ is_connected: true }));
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(4);
  });

  it('phat hien dien EVN hom qua tang vot bat thuong va khong bia don gia khi thieu du lieu thang', async () => {
    const sentTexts: string[] = [];
    const svc = new TelegramAlertService(() => baseTelegramConfig(), {
      watchdogIntervalMs: 0,
      fetchFn: (async (_url, init) => {
        const body = JSON.parse(String(init?.body || '{}')) as { text?: string };
        sentTexts.push(body.text || '');
        return new Response('{"ok":true}', { status: 200 });
      }) as typeof fetch,
    });
    services.push(svc);

    const history = [
      { ngay: '2026-11-18', kwh: 7.0 },
      { ngay: '2026-11-19', kwh: 6.8 },
      { ngay: '2026-11-20', kwh: 7.2 },
      { ngay: '2026-11-21', kwh: 7.0 },
      { ngay: '2026-11-22', kwh: 12.6 },
    ];

    const noCost = kiemTraDienTangVot(history, null, null, 40, 3);
    expect(noCost?.spike).toBe(true);
    expect(noCost?.estCostVnd).toBeNull();

    svc.observeMqttMessage('dien/thang/sanluong', '200');
    svc.observeMqttMessage('dien/thang/tien', '500000');
    svc.observeMqttMessage('dien/lichsu', JSON.stringify(history));
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(1);
    expect(sentTexts[0]).toContain('ĐIỆN EVN TĂNG VỌT');
    expect(sentTexts[0]).toContain('12.6 kWh');
    expect(sentTexts[0]).toContain('31.500');

    // Nhan lai cung lich su -> khong gui lap
    svc.observeMqttMessage('dien/lichsu', JSON.stringify(history));
    await new Promise((r) => setTimeout(r, 10));
    expect(sentTexts).toHaveLength(1);
  });
});

