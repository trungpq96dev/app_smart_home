import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import { AdminAuthService } from './adminAuth.js';
import { BomNuocBridge } from './bomNuocBridge.js';
import { ConfigStore } from './configStore.js';
import { MqttBridge } from './mqttBridge.js';
import { proxyCameraRecorderHttp } from './cameraRecorderProxy.js';
import { startCockpitProxyServer } from './cockpitProxy.js';
import {
  executeDockerControlAction,
  fetchDockerContainersLive,
  fetchDockerControlStatus,
  readDockerControlConfig,
} from './dockerControlProxy.js';
import { fetchStreamList, Go2rtcWsProxy, proxyGo2rtcHttp } from './go2rtcProxy.js';
import { TelegramAlertService } from './telegramAlerts.js';
import { TunnelWatcher } from './tunnelWatcher.js';
import { WebAccessAuthService } from './webAccessAuth.js';

const APP_VERSION = '0.9.4-web';
const PORT = Number.parseInt(process.env.PORT || '3000', 10);
const HOST = process.env.HOST || '0.0.0.0';

const ADMIN_PROTECTED_PUMP_KINDS = new Set([
  'protection',
  'lvlSetEmpty',
  'lvlSetFull',
  'calReset',
  'usageClear',
]);

const configStore = new ConfigStore();
const adminAuth = new AdminAuthService(configStore);
const webAccessAuth = new WebAccessAuthService();
const mqttBridge = new MqttBridge(configStore.getConfig().mqtt);
const bomNuocBridge = new BomNuocBridge(configStore.getConfig().bomNuoc);
const go2rtcWsProxy = new Go2rtcWsProxy();
const telegramAlerts = new TelegramAlertService(() => configStore.getConfig().telegram);
const tunnelWatcher = new TunnelWatcher(() => configStore.getConfig().telegram);
tunnelWatcher.start();

mqttBridge.onMessage((topic, payload) => {
  telegramAlerts.observeMqttMessage(topic, payload);
});
mqttBridge.onStatus((status) => {
  telegramAlerts.observeMqttStatus(status);
});
bomNuocBridge.onState((state) => {
  telegramAlerts.observeBomNuocState(state);
});

const app = express();
app.disable('x-powered-by');

// Proxy go2rtc HTTP & Dau ghi Camera 24/24 truoc express.json() de giu nguyen raw stream/Range
app.use('/go2rtc', webAccessAuth.requireWebAccess, (req, res) => {
  const orig = req.originalUrl || req.url;
  req.url = orig;
  proxyGo2rtcHttp(req, res, configStore.getConfig().camera);
});

app.use('/api/camera/recorder', webAccessAuth.requireWebAccess, (req, res) => {
  proxyCameraRecorderHttp(req, res, configStore.getConfig().camera);
});

app.use(express.json({ limit: '1mb' }));

// ---- Web Remote Access Auth (4G / Internet Password Gate) ----

app.get('/api/auth/session', (req, res) => {
  res.json(webAccessAuth.getSessionState(req));
});

app.post('/api/auth/login', (req, res) => {
  const password = typeof req.body?.password === 'string' ? req.body.password.trim() : '';
  const clientKey = webAccessAuth.getClientIpKey(req);
  const result = webAccessAuth.verifyPasswordAndIssueToken(password, clientKey);
  if (!result.ok) {
    res.status(result.status).json({
      ok: false,
      error: result.error,
      lockedUntilMs: result.lockedUntilMs,
    });
    return;
  }
  if (result.token) {
    res.setHeader('Set-Cookie', webAccessAuth.buildSetCookieHeader(req, result.token));
  }
  res.json({ ok: true, expiresAt: result.expiresAt });
});

app.post('/api/auth/logout', (req, res) => {
  res.setHeader('Set-Cookie', webAccessAuth.buildClearCookieHeader(req));
  res.json({ ok: true });
});

// Cho phep Docker HEALTHCHECK loopback 127.0.0.1 goi /api/health, chan moi API khac neu chua dang nhap
app.use('/api', (req, res, next) => {
  if (req.path === '/health' && webAccessAuth.isLoopbackHealthCheck(req)) {
    next();
    return;
  }
  webAccessAuth.requireWebAccess(req, res, next);
});

app.get('/api/tunnel/status', (_req, res) => {
  res.json(tunnelWatcher.getStatus());
});

app.get('/api/health', (_req, res) => {
  res.json({
    ok: true,
    version: APP_VERSION,
    mqttStatus: mqttBridge.getStatus(),
    bomNuocLink: bomNuocBridge.getLink(),
  });
});

app.get('/api/config', (_req, res) => {
  res.json(configStore.getPublicConfig());
});

// ---- Admin PIN Auth (4 digits) ----

app.get('/api/admin/session', (req, res) => {
  const enabled = adminAuth.isEnabled();
  const unlocked = !enabled || adminAuth.isTokenValid(req.header('x-admin-token'));
  res.json({ enabled, unlocked });
});

app.post('/api/admin/verify', (req, res) => {
  const pin = typeof req.body?.pin === 'string' ? req.body.pin.trim() : '';
  const clientKey = req.ip || 'local';
  const result = adminAuth.verify(pin, clientKey);
  if (!result.ok) {
    res.status(401).json(result);
    return;
  }
  res.json(result);
});

app.post('/api/admin/lock', (req, res) => {
  adminAuth.revokeToken(req.header('x-admin-token'));
  res.json({ ok: true });
});

app.put('/api/admin/pin', (req, res) => {
  const currentPin = typeof req.body?.currentPin === 'string' ? req.body.currentPin.trim() : '';
  const newPin = typeof req.body?.newPin === 'string' && req.body.newPin.trim()
    ? req.body.newPin.trim()
    : undefined;
  const enabled = typeof req.body?.enabled === 'boolean' ? req.body.enabled : undefined;

  const result = adminAuth.changePin(currentPin, newPin, enabled);
  if (!result.ok) {
    res.status(400).json(result);
    return;
  }
  res.json({ ok: true, config: configStore.getPublicConfig() });
});

// ---- Protected Service Config Endpoints ----

app.put('/api/config/mqtt', adminAuth.requireAdmin, (req, res) => {
  const updated = configStore.updateMqtt(req.body || {});
  mqttBridge.connect(updated.mqtt);
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
    mqttStatus: mqttBridge.getStatus(),
  });
});

app.put('/api/config/camera', adminAuth.requireAdmin, async (req, res) => {
  const updated = configStore.updateCamera(req.body || {});
  const probe = await fetchStreamList(updated.camera);
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
    probe,
  });
});

app.put('/api/config/weather', (req, res) => {
  configStore.updateWeather(req.body || {});
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
  });
});

app.put('/api/config/aquacontrol', adminAuth.requireAdmin, (req, res) => {
  const url = typeof req.body?.aquaControlUrl === 'string' ? req.body.aquaControlUrl : '';
  configStore.updateAquaControlUrl(url);
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
  });
});

app.put('/api/config/bomnuoc', adminAuth.requireAdmin, (req, res) => {
  const updated = configStore.updateBomNuoc(req.body || {});
  bomNuocBridge.connect(updated.bomNuoc);
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
    link: bomNuocBridge.getLink(),
    kenh: bomNuocBridge.getKenh(),
  });
});

// ---- Telegram Alerts ----

app.get('/api/alerts/status', (_req, res) => {
  res.json(telegramAlerts.getStatus());
});

app.put('/api/config/telegram', adminAuth.requireAdmin, (req, res) => {
  configStore.updateTelegram(req.body || {});
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
    alertsStatus: telegramAlerts.getStatus(),
  });
});

app.post('/api/alerts/telegram/test', adminAuth.requireAdmin, async (_req, res) => {
  const result = await telegramAlerts.sendTestAlert();
  res.json({ ...result, alertsStatus: telegramAlerts.getStatus() });
});

// ---- Pump Control & Logs ----

app.get('/api/bomnuoc/state', (_req, res) => {
  res.json(bomNuocBridge.getSnapshot());
});

app.post('/api/bomnuoc/cmd', async (req, res, next) => {
  const kind = typeof req.body?.kind === 'string' ? req.body.kind.trim() : '';
  if (ADMIN_PROTECTED_PUMP_KINDS.has(kind)) {
    adminAuth.requireAdmin(req, res, async () => {
      const result = await bomNuocBridge.executeCommand(req.body);
      res.json(result);
    });
    return;
  }
  const result = await bomNuocBridge.executeCommand(req.body);
  res.json(result);
  void next;
});

app.delete('/api/bomnuoc/log', adminAuth.requireAdmin, (_req, res) => {
  bomNuocBridge.clearLog();
  res.json({ ok: true });
});

app.get('/api/go2rtc/streams', async (_req, res) => {
  const result = await fetchStreamList(configStore.getConfig().camera);
  if (result.ok) {
    res.json(result);
  } else {
    res.status(200).json(result);
  }
});

// ---- Safe Docker Container Control (KHONG qua MQTT; yeu cau PIN Admin + Bearer token) ----

app.get('/api/server/docker/status', async (_req, res) => {
  const cfg = readDockerControlConfig(configStore.getConfig().mqtt.host);
  const result = await fetchDockerControlStatus(cfg);
  res.json(result);
});

app.get('/api/server/docker/containers', async (_req, res) => {
  const cfg = readDockerControlConfig(configStore.getConfig().mqtt.host);
  const result = await fetchDockerContainersLive(cfg);
  res.status(result.status).json(result);
});

app.post('/api/server/docker/action', adminAuth.requireAdmin, async (req, res) => {
  const cfg = readDockerControlConfig(configStore.getConfig().mqtt.host);
  const result = await executeDockerControlAction(cfg, req.body?.name, req.body?.action);
  res.status(result.status).json(result);
});

// Phuc vu client da build o che do production (chiu duoc moi thu muc lam viec)
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const candidateNearDist = path.resolve(__dirname, '..', 'client');
const candidateCwd = path.resolve(process.cwd(), 'dist', 'client');
const clientDist = fs.existsSync(path.join(candidateNearDist, 'index.html'))
  ? candidateNearDist
  : candidateCwd;
const indexHtml = path.join(clientDist, 'index.html');
if (fs.existsSync(indexHtml)) {
  app.use(
    express.static(clientDist, {
      index: false,
      setHeaders: (res, filePath) => {
        if (filePath.endsWith('.html') || filePath.endsWith('.webmanifest')) {
          res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate');
        }
      },
    }),
  );
  app.use((req, res, next) => {
    if (
      req.method !== 'GET' ||
      req.path.startsWith('/api/') ||
      req.path.startsWith('/go2rtc') ||
      req.path.startsWith('/ws/')
    ) {
      return next();
    }
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate');
    res.sendFile(indexHtml);
  });
}

const server = http.createServer(app);

server.on('upgrade', (req, socket, head) => {
  if (!webAccessAuth.isRequestAuthenticated(req)) {
    socket.write('HTTP/1.1 401 Unauthorized\r\nConnection: close\r\n\r\n');
    socket.destroy();
    return;
  }
  const url = req.url || '';
  if (url === '/ws/mqtt' || url.startsWith('/ws/mqtt?')) {
    mqttBridge.handleUpgrade(req, socket, head);
    return;
  }
  if (url === '/ws/bomnuoc' || url.startsWith('/ws/bomnuoc?')) {
    bomNuocBridge.handleUpgrade(req, socket, head);
    return;
  }
  if (url.startsWith('/go2rtc')) {
    go2rtcWsProxy.handleUpgrade(req, socket, head, configStore.getConfig().camera);
    return;
  }
  socket.destroy();
});

server.listen(PORT, HOST, () => {
  console.log(`SmartHome Web Gateway đang chạy tại http://${HOST}:${PORT}`);
});

const cockpitProxyPort = Number.parseInt(process.env.COCKPIT_PROXY_PORT || '0', 10);
if (cockpitProxyPort > 0) {
  const cockpitHost = (process.env.COCKPIT_HOST || configStore.getConfig().mqtt.host || '192.168.0.50').trim();
  const cockpitPort = Number.parseInt(process.env.COCKPIT_PORT || '9090', 10) || 9090;
  startCockpitProxyServer(webAccessAuth, {
    targetHost: cockpitHost,
    targetPort: cockpitPort,
    listenPort: cockpitProxyPort,
  });
}
