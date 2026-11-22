import http, { type IncomingMessage, type ServerResponse } from 'node:http';
import https from 'node:https';
import type { Duplex } from 'node:stream';
import { WebSocket, WebSocketServer } from 'ws';
import type { CameraConfig } from './configStore.js';

const GO2RTC_TIMEOUT_MS = 6000;

export function isCameraConfigReady(cam: CameraConfig): boolean {
  return cam.host.trim().length > 0 && cam.cong >= 1 && cam.cong <= 65535;
}

function baseUrl(cam: CameraConfig): string {
  const proto = cam.tls ? 'https' : 'http';
  const host = cam.host.trim().replace(/\/+$/, '');
  return `${proto}://${host}:${cam.cong}`;
}

function wsBaseUrl(cam: CameraConfig): string {
  const proto = cam.tls ? 'wss' : 'ws';
  const host = cam.host.trim().replace(/\/+$/, '');
  return `${proto}://${host}:${cam.cong}`;
}

function basicAuthHeader(cam: CameraConfig): string | undefined {
  if (!cam.taiKhoan.trim()) return undefined;
  const raw = `${cam.taiKhoan}:${cam.matKhau}`;
  return `Basic ${Buffer.from(raw, 'utf8').toString('base64')}`;
}

function translateCameraError(err: unknown): string {
  const e = err as { code?: string; name?: string; message?: string; cause?: { code?: string } } | undefined;
  const code = e?.code || e?.cause?.code || '';
  const msg = e?.message || '';
  if (code === 'ENOTFOUND' || code === 'EAI_AGAIN') {
    return 'Không tìm thấy máy chạy go2rtc. Sai địa chỉ, hoặc máy chủ đang ở mạng khác.';
  }
  if (e?.name === 'TimeoutError' || e?.name === 'AbortError' || code === 'ETIMEDOUT') {
    return 'Máy chạy go2rtc không trả lời. Thường là tường lửa chặn cổng, hoặc go2rtc đã tắt.';
  }
  if (code === 'ECONNREFUSED') {
    return 'Máy có đó nhưng từ chối cổng này. Kiểm tra go2rtc còn chạy không và cổng có đúng không.';
  }
  return msg || 'Lỗi kết nối go2rtc không rõ.';
}

/**
 * Chi lay KHOA (ten luong) tu `/api/streams`, tuyet doi khong tra ve
 * `producers[].url` vi trong do co email/mat khau Tuya dang chu thuong.
 */
export async function fetchStreamList(
  cam: CameraConfig,
): Promise<{ ok: true; streams: string[] } | { ok: false; error: string }> {
  if (!isCameraConfigReady(cam)) {
    return { ok: false, error: 'Chưa khai báo go2rtc' };
  }

  const headers: Record<string, string> = {};
  const auth = basicAuthHeader(cam);
  if (auth) headers.Authorization = auth;

  try {
    const resp = await fetch(`${baseUrl(cam)}/api/streams`, {
      method: 'GET',
      headers,
      signal: AbortSignal.timeout(GO2RTC_TIMEOUT_MS),
    });

    if (resp.status === 401 || resp.status === 403) {
      return {
        ok: false,
        error: 'go2rtc đòi tài khoản. Điền tài khoản và mật khẩu go2rtc ở Cài đặt.',
      };
    }
    if (resp.status !== 200) {
      return {
        ok: false,
        error: `go2rtc trả mã ${resp.status}. Kiểm tra lại cổng.`,
      };
    }

    const json = (await resp.json()) as Record<string, unknown>;
    const streams = Object.keys(json).sort();
    return { ok: true, streams };
  } catch (err) {
    return { ok: false, error: translateCameraError(err) };
  }
}

export function proxyGo2rtcHttp(req: IncomingMessage, res: ServerResponse, cam: CameraConfig): void {
  if (!isCameraConfigReady(cam)) {
    res.statusCode = 503;
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.end(JSON.stringify({ error: 'Chưa khai báo go2rtc' }));
    return;
  }

  const targetPath = (req.url || '/').replace(/^\/go2rtc/, '') || '/';
  const transport = cam.tls ? https : http;
  const headers: http.OutgoingHttpHeaders = { ...req.headers };
  delete headers.host;
  delete headers.origin;
  delete headers.referer;
  const auth = basicAuthHeader(cam);
  if (auth) headers.authorization = auth;

  const proxyReq = transport.request(
    {
      hostname: cam.host.trim(),
      port: cam.cong,
      path: targetPath,
      method: req.method,
      headers,
    },
    (proxyRes) => {
      const respHeaders = { ...proxyRes.headers };
      // Chan popup Basic Auth cua trinh duyet khi go2rtc tra 401
      delete respHeaders['www-authenticate'];
      res.writeHead(proxyRes.statusCode || 502, respHeaders);
      proxyRes.pipe(res);
    },
  );

  proxyReq.setTimeout(15000, () => {
    proxyReq.destroy(new Error('Gateway timeout'));
  });

  proxyReq.on('error', (err) => {
    if (!res.headersSent) {
      res.statusCode = 502;
      res.setHeader('Content-Type', 'text/plain; charset=utf-8');
      res.end(translateCameraError(err));
    } else {
      res.end();
    }
  });

  req.pipe(proxyReq);
}

export class Go2rtcWsProxy {
  private readonly wss = new WebSocketServer({ noServer: true });

  handleUpgrade(req: IncomingMessage, socket: Duplex, head: Buffer, cam: CameraConfig): void {
    if (!isCameraConfigReady(cam)) {
      socket.destroy();
      return;
    }

    this.wss.handleUpgrade(req, socket, head, (clientWs) => {
      const targetPath = (req.url || '/').replace(/^\/go2rtc/, '') || '/';
      const upstreamUrl = `${wsBaseUrl(cam)}${targetPath}`;
      const auth = basicAuthHeader(cam);
      const protocolsHeader = req.headers['sec-websocket-protocol'];
      const protocols = typeof protocolsHeader === 'string'
        ? protocolsHeader.split(',').map((p) => p.trim()).filter(Boolean)
        : undefined;

      const upstreamWs = new WebSocket(upstreamUrl, protocols, {
        headers: auth ? { Authorization: auth } : undefined,
      });

      const pending: Array<{ data: Buffer | string; isBinary: boolean }> = [];

      upstreamWs.on('open', () => {
        while (pending.length > 0 && upstreamWs.readyState === WebSocket.OPEN) {
          const item = pending.shift()!;
          upstreamWs.send(item.data, { binary: item.isBinary });
        }
      });

      upstreamWs.on('message', (data, isBinary) => {
        if (clientWs.readyState === WebSocket.OPEN) {
          clientWs.send(data as Buffer, { binary: isBinary });
        }
      });

      clientWs.on('message', (data, isBinary) => {
        if (upstreamWs.readyState === WebSocket.OPEN) {
          upstreamWs.send(data as Buffer, { binary: isBinary });
        } else if (upstreamWs.readyState === WebSocket.CONNECTING) {
          pending.push({ data: data as Buffer, isBinary });
        }
      });

      const closeBoth = () => {
        if (clientWs.readyState === WebSocket.OPEN || clientWs.readyState === WebSocket.CONNECTING) {
          clientWs.close();
        }
        if (upstreamWs.readyState === WebSocket.OPEN || upstreamWs.readyState === WebSocket.CONNECTING) {
          upstreamWs.close();
        }
      };

      upstreamWs.on('close', closeBoth);
      upstreamWs.on('error', closeBoth);
      clientWs.on('close', closeBoth);
      clientWs.on('error', closeBoth);
    });
  }
}

