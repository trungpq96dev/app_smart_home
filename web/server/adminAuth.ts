import crypto from 'node:crypto';
import type { NextFunction, Request, Response } from 'express';
import { isValidPin4, type ConfigStore } from './configStore.js';

const SESSION_TTL_MS = 5 * 60_000;
const MAX_FAIL_ATTEMPTS = 5;
const LOCKOUT_MS = 30_000;

interface FailRecord {
  count: number;
  lockedUntilMs: number;
}

/**
 * Quan ly phien mo khoa PIN 4 so cho khu vuc Ky thuat tren Node.js Gateway.
 * Session token giu trong bo nho RAM va tu het han sau 5 phut khong thao tac.
 */
export class AdminAuthService {
  private readonly tokens = new Map<string, number>();
  private readonly fails = new Map<string, FailRecord>();
  private readonly nowFn: () => number;

  constructor(
    private readonly configStore: ConfigStore,
    nowFn: () => number = () => Date.now(),
  ) {
    this.nowFn = nowFn;
  }

  verify(
    pin: string,
    clientKey: string = 'default',
  ): { ok: true; token: string; expiresAt: number } | { ok: false; error: string; lockedUntilMs?: number } {
    const now = this.nowFn();
    const rec = this.fails.get(clientKey);
    if (rec && rec.lockedUntilMs > now) {
      const waitSec = Math.max(1, Math.ceil((rec.lockedUntilMs - now) / 1000));
      return {
        ok: false,
        error: `Nhập sai PIN quá nhiều lần. Vui lòng thử lại sau ${waitSec} giây.`,
        lockedUntilMs: rec.lockedUntilMs,
      };
    }

    if (!this.configStore.verifyAdminPin(pin)) {
      const nextCount = (rec?.count ?? 0) + 1;
      if (nextCount >= MAX_FAIL_ATTEMPTS) {
        const lockedUntilMs = now + LOCKOUT_MS;
        this.fails.set(clientKey, { count: 0, lockedUntilMs });
        return {
          ok: false,
          error: 'Sai PIN 5 lần liên tiếp. Tạm khóa xác thực trong 30 giây.',
          lockedUntilMs,
        };
      }
      this.fails.set(clientKey, { count: nextCount, lockedUntilMs: 0 });
      return {
        ok: false,
        error: `Mã PIN không đúng (còn ${MAX_FAIL_ATTEMPTS - nextCount} lần thử).`,
      };
    }

    this.fails.delete(clientKey);
    this.purgeExpired(now);
    const token = crypto.randomBytes(32).toString('hex');
    const expiresAt = now + SESSION_TTL_MS;
    this.tokens.set(token, expiresAt);
    return { ok: true, token, expiresAt };
  }

  isTokenValid(token: string | undefined): boolean {
    if (!token) return false;
    const now = this.nowFn();
    this.purgeExpired(now);
    const exp = this.tokens.get(token);
    if (!exp || exp <= now) {
      this.tokens.delete(token);
      return false;
    }
    this.tokens.set(token, now + SESSION_TTL_MS);
    return true;
  }

  revokeToken(token: string | undefined): void {
    if (token) {
      this.tokens.delete(token);
    }
  }

  revokeAll(): void {
    this.tokens.clear();
  }

  changePin(
    currentPin: string,
    newPin: string | undefined,
    enabled: boolean | undefined,
  ): { ok: true } | { ok: false; error: string } {
    if (!this.configStore.verifyAdminPin(currentPin)) {
      return { ok: false, error: 'Mã PIN hiện tại không đúng.' };
    }
    if (newPin !== undefined && !isValidPin4(newPin)) {
      return { ok: false, error: 'Mã PIN mới phải gồm đúng 4 chữ số.' };
    }
    this.configStore.updateAdminPin({ enabled, newPin });
    if (newPin !== undefined || enabled === false) {
      this.revokeAll();
    }
    return { ok: true };
  }

  isEnabled(): boolean {
    return this.configStore.getConfig().adminPin.enabled;
  }

  requireAdmin = (req: Request, res: Response, next: NextFunction): void => {
    if (!this.isEnabled()) {
      next();
      return;
    }
    const token = req.header('x-admin-token');
    if (this.isTokenValid(token)) {
      next();
      return;
    }
    res.status(401).json({
      ok: false,
      requirePin: true,
      error: 'Cần mở khóa PIN kỹ thuật (4 số).',
    });
  };

  private purgeExpired(now: number): void {
    for (const [k, exp] of this.tokens.entries()) {
      if (exp <= now) this.tokens.delete(k);
    }
  }
}

