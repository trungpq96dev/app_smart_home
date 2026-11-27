import http, { type IncomingMessage, type ServerResponse } from 'node:http';
import https from 'node:https';
import tls from 'node:tls';
import type { Duplex } from 'node:stream';
import type { WebAccessAuthService } from './webAccessAuth.js';

const LOGIN_ROUTE = '/__sh_cockpit_login';

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function renderGateHtml(errorMsg?: string): string {
  const errBlock = errorMsg
    ? `<div style="border:1px solid rgba(239,68,68,.45);background:rgba(239,68,68,.12);color:#FCA5A5;border-radius:12px;padding:10px 12px;font-size:12.5px;margin-bottom:14px;">${escapeHtml(errorMsg)}</div>`
    : '';
  return `<!doctype html>
<html lang="vi">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
  <title>Khóa bảo vệ Server 9090 — SmartHome</title>
  <style>
    * { box-sizing: border-box; font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    body { margin: 0; min-height: 100dvh; display: flex; align-items: center; justify-content: center; background: #0B1220; color: #E6EDF7; padding: 16px; }
    .card { width: 100%; max-width: 380px; background: #16213A; border: 1px solid #253459; border-radius: 24px; padding: 24px; box-shadow: 0 20px 50px -15px rgba(0,0,0,.75); }
    .badge { display: inline-block; background: rgba(168,85,247,.16); border: 1px solid rgba(168,85,247,.4); color: #D8B4FE; border-radius: 999px; padding: 4px 12px; font-size: 11px; font-weight: 600; margin-bottom: 8px; }
    h1 { font-size: 18px; margin: 4px 0 6px; }
    p { font-size: 12.5px; color: #8A9BB8; margin: 0 0 18px; line-height: 1.5; }
    label { display: block; font-size: 12px; color: #8A9BB8; margin-bottom: 6px; }
    input[type="password"] { width: 100%; padding: 11px 13px; border-radius: 12px; border: 1px solid #253459; background: #0E1627; color: #E6EDF7; font-size: 16px; outline: none; margin-bottom: 14px; }
    input[type="password"]:focus { border-color: #4F8BFF; }
    button { width: 100%; padding: 11px; border: none; border-radius: 12px; background: linear-gradient(90deg, #2563EB, #4F8BFF); color: #fff; font-weight: 700; font-size: 14px; cursor: pointer; }
    .foot { margin-top: 14px; text-align: center; font-size: 11px; color: #5F6F8C; }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0,0,0,0); }
  </style>
</head>
<body>
  <div class="card">
    <div style="text-align:center;">
      <span class="badge">Lớp khóa từ xa #1 · Cockpit :9090</span>
      <h1>Quản trị Server Ubuntu</h1>
      <p>Nhập mật khẩu truy cập từ xa SmartHome trước khi mở giao diện Cockpit 9090.</p>
    </div>
    ${errBlock}
    <form method="POST" action="${LOGIN_ROUTE}">
      <input type="text" name="username" autocomplete="username" value="smarthome-cockpit" tabindex="-1" class="sr-only" />
      <label>Mật khẩu truy cập từ xa</label>
      <input type="password" name="password" autocomplete="current-password" placeholder="Nhập mật khẩu…" autofocus required />
      <button type="submit">Mở khóa vào Cockpit 9090</button>
    </form>
    <div class="foot">Tự động lưu phiên 30 ngày trên iPhone / trình duyệt này.</div>
  </div>
</body>
</html>`;
}

function readFormBody(req: IncomingMessage, maxBytes = 8192): Promise<string> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    let total = 0;
    req.on('data', (chunk: Buffer) => {
      total += chunk.length;
      if (total > maxBytes) {
        reject(new Error('Payload too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

export interface CockpitProxyOptions {
  targetHost: string;
  targetPort: number;
  listenPort: number;
  listenHost?: string;
}

/**
 * Reverse Proxy 2 lop bao ve danh rieng cho Cockpit (:9090) khi di qua Cloudflare Tunnel:
 *  - Lop 1: Yeu cau cookie `sh_web_session` (cung mat khau 4G cua SmartHome, khoa IP neu sai 5 lan).
 *  - Lop 2: Chuyen tiep nguyen ban HTTP + WebSocket (`/cockpit/socket`) sang TLS `https://192.168.0.50:9090`
 *           giu nguyen `Host` & `Origin` de Cockpit tu khop Content-Security-Policy va dang nhap Linux binh thuong.
 */
export function startCockpitProxyServer(
  webAccessAuth: WebAccessAuthService,
  opts: CockpitProxyOptions,
): http.Server {
  const httpsAgent = new https.Agent({
    rejectUnauthorized: false,
    keepAlive: true,
  });

  const server = http.createServer(async (req: IncomingMessage, res: ServerResponse) => {
    const url = req.url || '/';
    const method = (req.method || 'GET').toUpperCase();

    if (method === 'POST' && url.startsWith(LOGIN_ROUTE)) {
      try {
        const raw = await readFormBody(req);
        const params = new URLSearchParams(raw);
        const password = (params.get('password') || '').trim();
        const clientKey = webAccessAuth.getClientIpKey(req);
        const result = webAccessAuth.verifyPasswordAndIssueToken(password, clientKey);
        if (!result.ok) {
          const html = renderGateHtml(result.error);
          res.writeHead(result.status, {
            'Content-Type': 'text/html; charset=utf-8',
            'Cache-Control': 'no-store',
          });
          res.end(html);
          return;
        }
        const headers: http.OutgoingHttpHeaders = {
          Location: '/',
          'Cache-Control': 'no-store',
        };
        if (result.token) {
          headers['Set-Cookie'] = webAccessAuth.buildSetCookieHeader(req, result.token);
        }
        res.writeHead(303, headers);
        res.end();
        return;
      } catch {
        res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('Yêu cầu đăng nhập không hợp lệ.');
        return;
      }
    }

    if (!webAccessAuth.isRequestAuthenticated(req)) {
      const html = renderGateHtml();
      res.writeHead(401, {
        'Content-Type': 'text/html; charset=utf-8',
        'Cache-Control': 'no-store',
      });
      res.end(html);
      return;
    }

    // Forward HTTP request to https://targetHost:targetPort
    const outHeaders: http.OutgoingHttpHeaders = { ...req.headers };
    delete outHeaders['accept-encoding']; // tranh loi nen kep khi qua cloudflared
    outHeaders['x-forwarded-proto'] = 'https';
    if (req.headers.host) {
      outHeaders.host = req.headers.host;
    }

    const proxyReq = https.request(
      {
        hostname: opts.targetHost,
        port: opts.targetPort,
        path: url,
        method,
        headers: outHeaders,
        agent: httpsAgent,
        rejectUnauthorized: false,
      },
      (proxyRes) => {
        const respHeaders: http.OutgoingHttpHeaders = { ...proxyRes.headers };
        res.writeHead(proxyRes.statusCode || 502, respHeaders);
        proxyRes.pipe(res);
      },
    );

    proxyReq.on('error', () => {
      if (!res.headersSent) {
        res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('Không kết nối được tới dịch vụ Cockpit (:9090) trên serverubuntu.');
      } else {
        res.end();
      }
    });

    req.pipe(proxyReq);
  });

  // Forward WebSocket upgrade (/cockpit/socket) over TLS to Cockpit 9090
  server.on('upgrade', (req: IncomingMessage, socket: Duplex, head: Buffer) => {
    if (!webAccessAuth.isRequestAuthenticated(req)) {
      socket.write('HTTP/1.1 401 Unauthorized\r\nConnection: close\r\n\r\n');
      socket.destroy();
      return;
    }

    const upstream = tls.connect(
      {
        host: opts.targetHost,
        port: opts.targetPort,
        rejectUnauthorized: false,
      },
      () => {
        const reqPath = req.url || '/';
        const lines: string[] = [`GET ${reqPath} HTTP/1.1`];
        for (let i = 0; i < req.rawHeaders.length; i += 2) {
          const k = req.rawHeaders[i];
          const v = req.rawHeaders[i + 1];
          if (!k || v === undefined) continue;
          lines.push(`${k}: ${v}`);
        }
        if (!req.headers['x-forwarded-proto']) {
          lines.push('X-Forwarded-Proto: https');
        }
        lines.push('', '');
        upstream.write(lines.join('\r\n'));
        if (head && head.length > 0) {
          upstream.write(head);
        }
        socket.pipe(upstream);
        upstream.pipe(socket);
      },
    );

    upstream.setNoDelay(true);
    upstream.on('error', () => socket.destroy());
    socket.on('error', () => upstream.destroy());
    socket.on('close', () => upstream.destroy());
    upstream.on('close', () => socket.destroy());
  });

  server.listen(opts.listenPort, opts.listenHost || '0.0.0.0', () => {
    console.log(
      `Cockpit 4G Protected Proxy đang lắng nghe nội bộ tại 0.0.0.0:${opts.listenPort} -> https://${opts.targetHost}:${opts.targetPort}`,
    );
  });

  return server;
}
