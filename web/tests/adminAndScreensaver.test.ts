import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { AdminAuthService } from '../server/adminAuth.js';
import { ConfigStore } from '../server/configStore.js';
import {
  kiemTraSuCoKhanCap,
  laKhungGioDem,
  parsePirMotionPayload,
} from '../client/src/data/screensaver.js';

const tempDirs: string[] = [];

afterEach(() => {
  while (tempDirs.length > 0) {
    const dir = tempDirs.pop();
    if (dir && fs.existsSync(dir)) {
      fs.rmSync(dir, { recursive: true, force: true });
    }
  }
});

describe('ConfigStore Telegram & AdminPin security', () => {
  it('an botToken, pinHash va pinSalt khoi getPublicConfig', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-sec-'));
    tempDirs.push(tmpDir);
    const store = new ConfigStore(path.join(tmpDir, 'config.json'));

    store.updateTelegram({
      enabled: true,
      botToken: '987654:SECRET-BOT-TOKEN',
      chatId: '-100123456',
      offlineMinutes: 7,
    });
    store.updateAdminPin({ enabled: true, newPin: '5678' });

    const pub = store.getPublicConfig();
    expect(pub.telegram.enabled).toBe(true);
    expect(pub.telegram.coBotToken).toBe(true);
    expect(pub.telegram.offlineMinutes).toBe(7);
    expect(pub.adminPin).toEqual({ enabled: true, daDatPin: true });
    expect(JSON.stringify(pub)).not.toContain('SECRET-BOT-TOKEN');
    expect(JSON.stringify(pub)).not.toContain('5678');
    expect(JSON.stringify(pub)).not.toContain('pinHash');
    expect(JSON.stringify(pub)).not.toContain('pinSalt');

    // Bo trong token -> giu nguyen token cu
    store.updateTelegram({ chatId: '-100999', botToken: '' });
    expect(store.getConfig().telegram.botToken).toBe('987654:SECRET-BOT-TOKEN');

    // Xoa ro rang -> xoa token
    store.updateTelegram({ xoaBotToken: true });
    expect(store.getConfig().telegram.botToken).toBe('');
    expect(store.getPublicConfig().telegram.coBotToken).toBe(false);
  });
});

describe('AdminAuthService PIN verification & lockout', () => {
  it('xac thuc PIN mac dinh 1234, khoa tam sau 5 lan sai va doi duoc PIN', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-pin-'));
    tempDirs.push(tmpDir);
    const store = new ConfigStore(path.join(tmpDir, 'config.json'));
    let now = 1_700_000_000_000;
    const auth = new AdminAuthService(store, () => now);

    // Sai 4 lan -> chua khoa
    for (let i = 0; i < 4; i++) {
      const bad = auth.verify('0000', '127.0.0.1');
      expect(bad.ok).toBe(false);
    }
    // Lan thu 5 -> khoa 30s
    const locked = auth.verify('0000', '127.0.0.1');
    expect(locked.ok).toBe(false);
    expect(locked.ok ? 0 : locked.lockedUntilMs).toBeGreaterThan(now);

    // Trong thoi gian khoa -> nhap dung van tu choi
    const blockedGood = auth.verify('1234', '127.0.0.1');
    expect(blockedGood.ok).toBe(false);

    // Sau 31s -> mo lai va cap token
    now += 31_000;
    const okRes = auth.verify('1234', '127.0.0.1');
    expect(okRes.ok).toBe(true);
    if (!okRes.ok) throw new Error('expected ok');
    expect(auth.isTokenValid(okRes.token)).toBe(true);

    // Doi PIN -> vo hieu hoa token cu
    const chg = auth.changePin('1234', '4321', true);
    expect(chg.ok).toBe(true);
    expect(auth.isTokenValid(okRes.token)).toBe(false);
    expect(auth.verify('1234', '127.0.0.1').ok).toBe(false);
    expect(auth.verify('4321', '127.0.0.1').ok).toBe(true);
  });
});

describe('Ambient Screensaver logic', () => {
  it('tinh dung khung gio dem qua nua dem', () => {
    expect(laKhungGioDem(22, 22, 6)).toBe(true);
    expect(laKhungGioDem(0, 22, 6)).toBe(true);
    expect(laKhungGioDem(5, 22, 6)).toBe(true);
    expect(laKhungGioDem(6, 22, 6)).toBe(false);
    expect(laKhungGioDem(14, 22, 6)).toBe(false);
  });

  it('doc dung tin hieu PIR va su co khan cap', () => {
    expect(parsePirMotionPayload('1')).toBe(true);
    expect(parsePirMotionPayload('{"motion":true}')).toBe(true);
    expect(parsePirMotionPayload('{"motion":false}')).toBe(false);

    expect(
      kiemTraSuCoKhanCap({
        kitchenGasAlert: false,
        mqttGasAlert: false,
        mqttGasPpm: null,
        bomNuoc: null,
        ketNoiBom: { state: 'chua_cau_hinh' },
        mqttStatus: { state: 'chua_cau_hinh' },
        gatewayOnline: true,
      }),
    ).toBeNull();

    const gas = kiemTraSuCoKhanCap({
      kitchenGasAlert: false,
      mqttGasAlert: true,
      mqttGasPpm: 350,
      bomNuoc: null,
      ketNoiBom: { state: 'chua_cau_hinh' },
      mqttStatus: { state: 'da_noi' },
      gatewayOnline: true,
    });
    expect(gas?.kind).toBe('gas');
    expect(gas?.dest).toBe('sensors');
  });
});

