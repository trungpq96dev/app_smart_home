import http, { type IncomingMessage, type ServerResponse } from 'node:http';
import https from 'node:https';
import { URL } from 'node:url';
import type { CameraConfig } from './configStore.js';

const FAST_TIMEOUT_MS = 10000;
const SNAPSHOT_TIMEOUT_MS = 20000;
const THUMB_TIMEOUT_MS = 30000;
// Khi phat doan video cu (tier === 'drive'), serverubuntu can keo file MP4 10 phut tu Google Drive qua rclone
const VIDEO_STREAM_TIMEOUT_MS = 120000;

export function isRecorderConfigReady(cam: CameraConfig): boolean {
  const port = cam.congGhiHinh || 1985;
  return cam.host.trim().length > 0 && port >= 1 && port <= 65535;
}

function resolveRecorderPort(cam: CameraConfig, portQuery: string | null): number {
  if (portQuery) {
    const parsed = Number.parseInt(portQuery, 10);
    if (Number.isFinite(parsed) && parsed >= 1 && parsed <= 65535) {
      return parsed;
    }
  }
  return cam.congGhiHinh >= 1 && cam.congGhiHinh <= 65535 ? cam.congGhiHinh : 1985;
}

function basicAuthHeader(cam: CameraConfig): string | undefined {
  if (!cam.taiKhoan.trim()) return undefined;
  const raw = `${cam.taiKhoan}:${cam.matKhau}`;
  return `Basic ${Buffer.from(raw, 'utf8').toString('base64')}`;
}

export function translateRecorderError(err: unknown, port: number): string {
  const e = err as { code?: string; name?: string; message?: string; cause?: { code?: string } } | undefined;
  const code = e?.code || e?.cause?.code || '';
  const msg = e?.message || '';
  if (code === 'ENOTFOUND' || code === 'EAI_AGAIN') {
    return 'Không tìm thấy máy chủ đầu ghi camera. Kiểm tra lại địa chỉ IP trong Cài đặt.';
  }
  if (e?.name === 'TimeoutError' || e?.name === 'AbortError' || code === 'ETIMEDOUT') {
    return `Đầu ghi camera (cổng ${port}) phản hồi quá lâu. Kiểm tra container camera_recorder trên serverubuntu.`;
  }
  if (code === 'ECONNREFUSED') {
    return `Cổng đầu ghi ${port} chưa mở. Kiểm tra dịch vụ camera_recorder đã chạy trên serverubuntu chưa.`;
  }
  return msg || `Lỗi kết nối đầu ghi camera (cổng ${port}).`;
}

function sendJson(res: ServerResponse, status: number, body: unknown): void {
  if (res.headersSent) return;
  const payload = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': String(Buffer.byteLength(payload)),
  });
  res.end(payload);
}

/**
 * Proxy toan bo HTTP Playback API cua `scripts/camera_recorder/api.py` (mac dinh cong 1985):
 *  - GET  /api/camera/recorder/health
 *  - GET  /api/camera/recorder/days?cam=<ten>
 *  - GET  /api/camera/recorder/timeline?cam=<ten>&date=YYYY-MM-DD
 *  - GET  /api/camera/recorder/video/<segId>       (ho tro HTTP Range 206 Partial Content & keo Google Drive)
 *  - GET  /api/camera/recorder/thumb/<evId>.jpg    (anh JPEG thu nho co khoanh khung YOLO)
 *  - POST /api/camera/recorder/snapshot?cam=<ten>  (chup anh tuc thi -> luu SQLite/Drive & gui Telegram)
 */
export function proxyCameraRecorderHttp(
  req: IncomingMessage & { originalUrl?: string },
  res: ServerResponse,
  cam: CameraConfig,
): void {
  if (!isRecorderConfigReady(cam)) {
    sendJson(res, 503, {
      ok: false,
      error: 'Chưa khai báo địa chỉ máy chủ camera trong Cài đặt.',
    });
    return;
  }

  const rawUrl = req.originalUrl || req.url || '/';
  const parsed = new URL(rawUrl, 'http://localhost');
  const subPath = parsed.pathname.replace(/^\/api\/camera\/recorder/, '') || '/';
  const port = resolveRecorderPort(cam, parsed.searchParams.get('port'));

  // Khong chuyen tham so noi bo `port` xuong camera_recorder
  parsed.searchParams.delete('port');
  const queryStr = parsed.searchParams.toString();

  let upstreamPath: string | null = null;
  let timeoutMs = FAST_TIMEOUT_MS;
  const method = (req.method || 'GET').toUpperCase();

  if (method === 'GET' && subPath === '/health') {
    upstreamPath = '/api/health';
  } else if (method === 'GET' && subPath === '/days') {
    upstreamPath = `/api/days${queryStr ? `?${queryStr}` : ''}`;
  } else if (method === 'GET' && subPath === '/timeline') {
    upstreamPath = `/api/timeline${queryStr ? `?${queryStr}` : ''}`;
  } else if (method === 'GET' && subPath.startsWith('/video/')) {
    const segRaw = subPath.slice('/video/'.length).replace(/\.mp4$/i, '');
    if (!/^\d+$/.test(segRaw)) {
      sendJson(res, 400, { ok: false, error: 'Mã đoạn video không hợp lệ.' });
      return;
    }
    upstreamPath = `/api/video/${segRaw}`;
    timeoutMs = VIDEO_STREAM_TIMEOUT_MS;
  } else if (method === 'GET' && subPath.startsWith('/thumb/')) {
    const evRaw = subPath.slice('/thumb/'.length).replace(/\.jpg$/i, '');
    if (!/^\d+$/.test(evRaw)) {
      sendJson(res, 400, { ok: false, error: 'Mã ảnh sự kiện không hợp lệ.' });
      return;
    }
    upstreamPath = `/api/thumb/${evRaw}.jpg`;
    timeoutMs = THUMB_TIMEOUT_MS;
  } else if (method === 'GET' && subPath === '/events') {
    upstreamPath = `/api/events${queryStr ? `?${queryStr}` : ''}`;
  } else if (method === 'GET' && subPath.startsWith('/clip/')) {
    const evRaw = subPath.slice('/clip/'.length).replace(/\.mp4$/i, '');
    if (!/^\d+$/.test(evRaw)) {
      sendJson(res, 400, { ok: false, error: 'Mã clip sự kiện không hợp lệ.' });
      return;
    }
    upstreamPath = `/api/clip/${evRaw}.mp4`;
    timeoutMs = VIDEO_STREAM_TIMEOUT_MS;
  } else if (method === 'GET' && subPath.startsWith('/face/snapshot/')) {
    const name = subPath.slice('/face/snapshot/'.length);
    if (!name || name.includes('..') || name.includes('/')) {
      sendJson(res, 400, { ok: false, error: 'Tên ảnh khuôn mặt không hợp lệ.' });
      return;
    }
    upstreamPath = `/api/face/snapshot/${name}`;
    timeoutMs = THUMB_TIMEOUT_MS;
  } else if (method === 'GET' && subPath === '/face/people') {
    upstreamPath = '/api/face/people';
  } else if (method === 'GET' && subPath.startsWith('/face/sample/')) {
    const rest = subPath.slice('/face/sample/'.length);
    if (!rest || rest.includes('..')) {
      sendJson(res, 400, { ok: false, error: 'Đường dẫn ảnh mẫu không hợp lệ.' });
      return;
    }
    upstreamPath = `/api/face/sample/${rest}`;
    timeoutMs = THUMB_TIMEOUT_MS;
  } else if (method === 'GET' && subPath === '/face/unknown') {
    upstreamPath = '/api/face/unknown';
  } else if (method === 'GET' && subPath === '/ptz/presets') {
    upstreamPath = `/api/ptz/presets${queryStr ? `?${queryStr}` : ''}`;
  } else if (method === 'GET' && subPath === '/tts') {
    upstreamPath = `/api/tts${queryStr ? `?${queryStr}` : ''}`;
    timeoutMs = SNAPSHOT_TIMEOUT_MS;
  } else if (method === 'POST' && subPath === '/snapshot') {
    upstreamPath = `/api/snapshot${queryStr ? `?${queryStr}` : ''}`;
    timeoutMs = SNAPSHOT_TIMEOUT_MS;
  } else if (
    method === 'POST' &&
    (subPath === '/ptz/move' ||
      subPath === '/ptz/preset' ||
      subPath === '/privacy' ||
      subPath === '/home_mode' ||
      subPath === '/face/promote' ||
      subPath === '/face/enroll/camera' ||
      subPath === '/face/enroll/upload' ||
      subPath === '/face/delete')
  ) {
    upstreamPath = `/api${subPath}${queryStr ? `?${queryStr}` : ''}`;
    timeoutMs = SNAPSHOT_TIMEOUT_MS;
  } else {
    sendJson(res, 404, { ok: false, error: 'Đường dẫn Playback API không tồn tại.' });
    return;
  }

  const transport = cam.tls ? https : http;
  const headers: http.OutgoingHttpHeaders = {};
  if (req.headers.range) {
    headers.range = req.headers.range;
  }
  if (req.headers['if-none-match']) {
    headers['if-none-match'] = req.headers['if-none-match'];
  }
  if (req.headers['if-modified-since']) {
    headers['if-modified-since'] = req.headers['if-modified-since'];
  }
  if (method === 'POST') {
    if (req.headers['content-type']) {
      headers['content-type'] = req.headers['content-type'];
    }
    if (req.headers['content-length']) {
      headers['content-length'] = req.headers['content-length'];
    }
  }
  const auth = basicAuthHeader(cam);
  if (auth) {
    headers.authorization = auth;
  }

  const proxyReq = transport.request(
    {
      hostname: cam.host.trim(),
      port,
      path: upstreamPath,
      method,
      headers,
    },
    (proxyRes) => {
      const statusCode = proxyRes.statusCode || 502;
      if (statusCode === 401 || statusCode === 403) {
        proxyRes.resume();
        sendJson(res, 401, {
          ok: false,
          error: 'Sai hoặc thiếu tài khoản xác thực Playback API (dùng chung tài khoản go2rtc ở Cài đặt).',
        });
        return;
      }

      const respHeaders: http.OutgoingHttpHeaders = { ...proxyRes.headers };
      delete respHeaders['www-authenticate'];
      res.writeHead(statusCode, respHeaders);
      proxyRes.pipe(res);
    },
  );

  proxyReq.setTimeout(timeoutMs, () => {
    proxyReq.destroy(Object.assign(new Error('Timeout'), { code: 'ETIMEDOUT' }));
  });

  proxyReq.on('error', (err) => {
    if (!res.headersSent) {
      sendJson(res, 502, {
        ok: false,
        error: translateRecorderError(err, port),
      });
    } else {
      res.end();
    }
  });

  // Huy luong keo video phia sau ngay khi trinh duyet ngat / tua sang doan khac
  res.on('close', () => {
    if (!res.writableEnded) {
      proxyReq.destroy();
    }
  });

  if (method === 'POST') {
    req.pipe(proxyReq);
  } else {
    proxyReq.end();
  }
}
