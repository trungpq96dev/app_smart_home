import fs from 'node:fs';
import type { IncomingMessage } from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { TunnelWatcher } from '../server/tunnelWatcher.js';
import { WebAccessAuthService } from '../server/webAccessAuth.js';

function fakeReq(opts: {
  remoteAddress?: string;
  headers?: Record<string, string>;
}): IncomingMessage {
  return {
    headers: opts.headers ?? {},
    socket: { remoteAddress: opts.remoteAddress ?? '172.18.0.3' },
  } as unknown as IncomingMessage;
}

describe('WebAccessAuthService (4G Remote Gate)', () => {
  it('blocks external tunnel requests until valid 30-day cookie is presented', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-webauth-'));
    let now = 1_700_000_000_000;
    const auth = new WebAccessAuthService({
      accessPassword: 'MatKhau4GSieuManh',
      bypassLan: true,
      secretFilePath: path.join(tmpDir, 'secret.key'),
      nowFn: () => now,
    });

    const tunnelReq = fakeReq({
      remoteAddress: '172.18.0.4',
      headers: {
        'cf-connecting-ip': '27.72.100.55',
        'cf-ray': '8abc12345-SIN',
        'x-forwarded-proto': 'https',
      },
    });
    expect(auth.isRequestAuthenticated(tunnelReq)).toBe(false);

    // Direct LAN without proxy headers is allowed seamlessly
    const lanReq = fakeReq({ remoteAddress: '::ffff:192.168.0.28' });
    expect(auth.isRequestAuthenticated(lanReq)).toBe(true);

    // Forged LAN IP with Cloudflare header must NOT bypass
    const forgedReq = fakeReq({
      remoteAddress: '192.168.0.28',
      headers: { 'cf-ray': '8abc-SIN' },
    });
    expect(auth.isRequestAuthenticated(forgedReq)).toBe(false);

    // Login & verify cookie
    const res = auth.verifyPasswordAndIssueToken('MatKhau4GSieuManh', '27.72.100.55');
    expect(res.ok).toBe(true);
    if (!res.ok) return;

    const setCookie = auth.buildSetCookieHeader(tunnelReq, res.token);
    expect(setCookie).toContain('HttpOnly');
    expect(setCookie).toContain('Secure');
    const cookiePair = setCookie.split(';')[0];

    const authedTunnelReq = fakeReq({
      remoteAddress: '172.18.0.4',
      headers: {
        'cf-connecting-ip': '27.72.100.55',
        'cf-ray': '8abc12345-SIN',
        cookie: cookiePair,
      },
    });
    expect(auth.isRequestAuthenticated(authedTunnelReq)).toBe(true);

    // After 31 days cookie expires
    now += 31 * 24 * 3600 * 1000;
    expect(auth.isRequestAuthenticated(authedTunnelReq)).toBe(false);

    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it('locks out brute-force attempts after 5 wrong passwords and invalidates cookies on password change', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-webauth-bf-'));
    const secretFile = path.join(tmpDir, 'secret.key');
    let now = 1_700_000_000_000;

    const auth1 = new WebAccessAuthService({
      accessPassword: 'PassCu123',
      secretFilePath: secretFile,
      nowFn: () => now,
    });
    const issued = auth1.verifyPasswordAndIssueToken('PassCu123', '1.2.3.4');
    expect(issued.ok).toBe(true);

    // Changing password invalidates old cookie even with same secret file
    const auth2 = new WebAccessAuthService({
      accessPassword: 'PassMoi456',
      secretFilePath: secretFile,
      nowFn: () => now,
    });
    if (issued.ok) {
      const reqWithOldCookie = fakeReq({
        remoteAddress: '172.18.0.4',
        headers: {
          'cf-ray': '123',
          cookie: `sh_web_session=${encodeURIComponent(issued.token)}`,
        },
      });
      expect(auth2.isRequestAuthenticated(reqWithOldCookie)).toBe(false);
    }

    for (let i = 0; i < 4; i++) {
      const bad = auth2.verifyPasswordAndIssueToken('sai', '9.9.9.9');
      expect(bad.ok).toBe(false);
      if (!bad.ok) expect(bad.status).toBe(401);
    }
    const fifth = auth2.verifyPasswordAndIssueToken('sai', '9.9.9.9');
    expect(fifth.ok).toBe(false);
    if (!fifth.ok) expect(fifth.status).toBe(429);

    // Even right password during lockout window is rejected
    const blockedRight = auth2.verifyPasswordAndIssueToken('PassMoi456', '9.9.9.9');
    expect(blockedRight.ok).toBe(false);

    // After 5 minutes + 1s, login works again
    now += 5 * 60 * 1000 + 1000;
    const afterLock = auth2.verifyPasswordAndIssueToken('PassMoi456', '9.9.9.9');
    expect(afterLock.ok).toBe(true);

    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it('extracts latest trycloudflare URL from log tail', async () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-tunnel-'));
    const logPath = path.join(tmpDir, 'cloudflared.log');
    fs.writeFileSync(
      logPath,
      [
        '2026-11-27 INF Requesting new quick Tunnel on trycloudflare.com...',
        '2026-11-27 INF |  https://old-house-demo.trycloudflare.com  |',
        '2026-11-27 INF Restarted...',
        '2026-11-27 INF |  https://smart-villa-vn.trycloudflare.com  |',
      ].join('\n'),
      'utf8',
    );
    const watcher = new TunnelWatcher(
      () => ({
        enabled: false,
        botToken: '',
        chatId: '',
        offlineMinutes: 5,
        electricSpikePct: 40,
        electricSpikeMinKwh: 3,
        gasThresholdPpm: 200,
        cooldownMinutes: 30,
        pirTopic: '',
        gasTopic: '',
      }),
      { logPath, statePath: path.join(tmpDir, 'last.txt') },
    );
    await watcher.scanOnce(false);
    expect(watcher.getStatus().url).toBe('https://smart-villa-vn.trycloudflare.com');
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });
});
