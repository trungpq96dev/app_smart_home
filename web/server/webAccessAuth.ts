import crypto from 'node:crypto';
import fs from 'node:fs';
import type { IncomingMessage } from 'node:http';
import path from 'node:path';
import type { NextFunction, Request, Response } from 'express';

const COOKIE_NAME = 'sh_web_session';
const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000; // 30 ngay
const MAX_FAIL_ATTEMPTS = 5;
const LOCKOUT_MS = 5 * 60 * 1000; // Khoa 5 phut neu doan sai 5 lan

interface FailRecord {
  count: number;
  lockedUntilMs: number;
}

const PROXY_HEADERS = [
  'cf-connecting-ip',
  'cf-ray',
  'cf-visitor',
  'cdn-loop',
  'x-forwarded-for',
  'x-forwarded-host',
  'x-forwarded-proto',
  'x-real-ip',
];

function parseBool(v: string | undefined, fallback: boolean): boolean {
  if (v === undefined) return fallback;
  const s = v.trim().toLowerCase();
  if (s === 'true' || s === '1' || s === 'yes') return true;
  if (s === 'false' || s === '0' || s === 'no') return false;
  return fallback;
}

function normalizeIp(raw: string | undefined): string {
  if (!raw) return '';
  const trimmed = raw.trim();
  if (trimmed.startsWith('::ffff:')) {
    return trimmed.slice('::ffff:'.length);
  }
  return trimmed;
}

export function isLoopbackIp(raw: string | undefined): boolean {
  const ip = normalizeIp(raw);
  return ip === '127.0.0.1' || ip === '::1';
}

/**
 * Chi coi la mang LAN / Tailscale noi bo truc tiep (KHONG bao gom 172.16.0.0/12
 * vi container cloudflared chay tren Docker bridge 172.x.x.x).
 */
export function isDirectHomeLanIp(raw: string | undefined): boolean {
  const ip = normalizeIp(raw);
  if (!ip) return false;
  if (ip === '127.0.0.1' || ip === '::1') return true;
  if (ip.startsWith('192.168.')) return true;
  if (ip.startsWith('10.')) return true;
  // Tailscale CGNAT 100.64.0.0/10
  const m = /^100\.(\d{1,3})\.\d{1,3}\.\d{1,3}$/.exec(ip);
  if (m) {
    const second = Number.parseInt(m[1], 10);
    if (second >= 64 && second <= 127) return true;
  }
  return false;
}

export function hasReverseProxyHeaders(req: IncomingMessage): boolean {
  for (const h of PROXY_HEADERS) {
    const val = req.headers[h];
    if (typeof val === 'string' && val.trim().length > 0) return true;
    if (Array.isArray(val) && val.length > 0) return true;
  }
  return false;
}

function parseCookies(cookieHeader: string | undefined): Record<string, string> {
  const out: Record<string, string> = {};
  if (!cookieHeader) return out;
  for (const part of cookieHeader.split(';')) {
    const idx = part.indexOf('=');
    if (idx <= 0) continue;
    const k = part.slice(0, idx).trim();
    const v = part.slice(idx + 1).trim();
    if (k) {
      try {
        out[k] = decodeURIComponent(v);
      } catch {
        out[k] = v;
      }
    }
  }
  return out;
}

function loadOrCreateSecret(secretFilePath: string): string {
  try {
    if (fs.existsSync(secretFilePath)) {
      const existing = fs.readFileSync(secretFilePath, 'utf8').trim();
      if (existing.length >= 32) return existing;
    }
    fs.mkdirSync(path.dirname(secretFilePath), { recursive: true });
    const created = crypto.randomBytes(32).toString('hex');
    fs.writeFileSync(secretFilePath, created, { encoding: 'utf8', mode: 0o600 });
    return created;
  } catch {
    return crypto.randomBytes(32).toString('hex');
  }
}

export interface WebAccessOptions {
  accessPassword?: string;
  bypassLan?: boolean;
  secretFilePath?: string;
  nowFn?: () => number;
}

export class WebAccessAuthService {
  private readonly password: string;
  private readonly bypassLan: boolean;
  private readonly secret: string;
  private readonly passFingerprint: string;
  private readonly fails = new Map<string, FailRecord>();
  private readonly nowFn: () => number;

  constructor(opts: WebAccessOptions = {}) {
    this.password = (opts.accessPassword ?? process.env.WEB_ACCESS_PASS ?? '').trim();
    this.bypassLan = opts.bypassLan ?? parseBool(process.env.WEB_AUTH_BYPASS_LAN, true);
    const defaultSecretPath = path.resolve(
      path.dirname(process.env.CONFIG_PATH || path.resolve(process.cwd(), 'data', 'config.json')),
      'web_session_secret.key',
    );
    this.secret = loadOrCreateSecret(opts.secretFilePath || defaultSecretPath);
    this.passFingerprint = crypto
      .createHash('sha256')
      .update(`pass:${this.password}`)
      .digest('hex')
      .slice(0, 16);
    this.nowFn = opts.nowFn ?? (() => Date.now());
  }

  isEnabled(): boolean {
    return this.password.length > 0;
  }

  getClientIpKey(req: IncomingMessage): string {
    const cfIp = req.headers['cf-connecting-ip'];
    if (typeof cfIp === 'string' && cfIp.trim()) return cfIp.trim();
    const xff = req.headers['x-forwarded-for'];
    if (typeof xff === 'string' && xff.trim()) return xff.split(',')[0].trim();
    return normalizeIp(req.socket?.remoteAddress) || 'unknown';
  }

  isLoopbackHealthCheck(req: IncomingMessage): boolean {
    return !hasReverseProxyHeaders(req) && isLoopbackIp(req.socket?.remoteAddress);
  }

  isDirectLanAllowed(req: IncomingMessage): boolean {
    if (!this.bypassLan) return false;
    if (hasReverseProxyHeaders(req)) return false;
    return isDirectHomeLanIp(req.socket?.remoteAddress);
  }

  getSessionState(req: IncomingMessage): {
    required: boolean;
    authenticated: boolean;
    mode: 'disabled' | 'lan' | 'cookie' | 'unauthenticated';
  } {
    if (!this.isEnabled()) {
      return { required: false, authenticated: true, mode: 'disabled' };
    }
    if (this.isDirectLanAllowed(req)) {
      return { required: false, authenticated: true, mode: 'lan' };
    }
    if (this.hasValidCookie(req)) {
      return { required: true, authenticated: true, mode: 'cookie' };
    }
    return { required: true, authenticated: false, mode: 'unauthenticated' };
  }

  isRequestAuthenticated(req: IncomingMessage): boolean {
    return this.getSessionState(req).authenticated;
  }

  verifyPasswordAndIssueToken(
    candidatePassword: string,
    clientKey: string,
  ):
    | { ok: true; token: string; expiresAt: number }
    | { ok: false; status: number; error: string; lockedUntilMs?: number } {
    if (!this.isEnabled()) {
      return { ok: true, token: '', expiresAt: this.nowFn() + SESSION_TTL_MS };
    }

    const now = this.nowFn();
    const rec = this.fails.get(clientKey);
    if (rec && rec.lockedUntilMs > now) {
      const waitSec = Math.max(1, Math.ceil((rec.lockedUntilMs - now) / 1000));
      return {
        ok: false,
        status: 429,
        error: `Nhập sai quá nhiều lần. Vui lòng thử lại sau ${waitSec} giây.`,
        lockedUntilMs: rec.lockedUntilMs,
      };
    }

    const candHash = crypto.createHash('sha256').update(candidatePassword).digest();
    const realHash = crypto.createHash('sha256').update(this.password).digest();
    const match = candidatePassword.length > 0 && crypto.timingSafeEqual(candHash, realHash);

    if (!match) {
      const nextCount = (rec?.count ?? 0) + 1;
      if (nextCount >= MAX_FAIL_ATTEMPTS) {
        const lockedUntilMs = now + LOCKOUT_MS;
        this.fails.set(clientKey, { count: 0, lockedUntilMs });
        return {
          ok: false,
          status: 429,
          error: 'Sai mật khẩu 5 lần liên tiếp. Tạm khóa đăng nhập từ IP này trong 5 phút.',
          lockedUntilMs,
        };
      }
      this.fails.set(clientKey, { count: nextCount, lockedUntilMs: 0 });
      return {
        ok: false,
        status: 401,
        error: `Mật khẩu không đúng (còn ${MAX_FAIL_ATTEMPTS - nextCount} lần thử).`,
      };
    }

    this.fails.delete(clientKey);
    const expiresAt = now + SESSION_TTL_MS;
    const nonce = crypto.randomBytes(16).toString('hex');
    const sig = this.signToken(expiresAt, nonce);
    const token = `${expiresAt}.${nonce}.${sig}`;
    return { ok: true, token, expiresAt };
  }

  buildSetCookieHeader(req: IncomingMessage, token: string): string {
    const isSecure =
      Boolean((req as Request).secure) ||
      req.headers['x-forwarded-proto'] === 'https' ||
      Boolean(req.headers['cf-ray']);
    const maxAgeSec = Math.floor(SESSION_TTL_MS / 1000);
    return `${COOKIE_NAME}=${encodeURIComponent(token)}; Path=/; Max-Age=${maxAgeSec}; HttpOnly; SameSite=Lax${
      isSecure ? '; Secure' : ''
    }`;
  }

  buildClearCookieHeader(req: IncomingMessage): string {
    const isSecure =
      Boolean((req as Request).secure) ||
      req.headers['x-forwarded-proto'] === 'https' ||
      Boolean(req.headers['cf-ray']);
    return `${COOKIE_NAME}=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax${isSecure ? '; Secure' : ''}`;
  }

  requireWebAccess = (req: Request, res: Response, next: NextFunction): void => {
    if (this.isRequestAuthenticated(req)) {
      next();
      return;
    }
    res.status(401).json({
      ok: false,
      requireWebLogin: true,
      error: 'Vui lòng đăng nhập mật khẩu để truy cập nhà thông minh từ xa.',
    });
  };

  private hasValidCookie(req: IncomingMessage): boolean {
    const cookies = parseCookies(req.headers.cookie);
    const raw = cookies[COOKIE_NAME];
    if (!raw) return false;
    const parts = raw.split('.');
    if (parts.length !== 3) return false;
    const [expStr, nonce, sig] = parts;
    if (!/^\d+$/.test(expStr) || !/^[0-9a-f]{32}$/.test(nonce) || !/^[0-9a-f]{64}$/.test(sig)) {
      return false;
    }
    const expiresAt = Number(expStr);
    if (!Number.isFinite(expiresAt) || expiresAt <= this.nowFn()) {
      return false;
    }
    const expected = this.signToken(expiresAt, nonce);
    const a = Buffer.from(sig, 'hex');
    const b = Buffer.from(expected, 'hex');
    if (a.length !== b.length) return false;
    return crypto.timingSafeEqual(a, b);
  }

  private signToken(expiresAt: number, nonce: string): string {
    return crypto
      .createHmac('sha256', this.secret)
      .update(`v1:${this.passFingerprint}:${expiresAt}:${nonce}`)
      .digest('hex');
  }
}
