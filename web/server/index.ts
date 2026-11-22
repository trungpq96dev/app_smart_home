import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import { BomNuocBridge } from './bomNuocBridge.js';
import { ConfigStore } from './configStore.js';
import { MqttBridge } from './mqttBridge.js';
import { fetchStreamList, Go2rtcWsProxy, proxyGo2rtcHttp } from './go2rtcProxy.js';

const APP_VERSION = '0.9.0-web';
const PORT = Number.parseInt(process.env.PORT || '3000', 10);
const HOST = process.env.HOST || '0.0.0.0';

const configStore = new ConfigStore();
const mqttBridge = new MqttBridge(configStore.getConfig().mqtt);
const bomNuocBridge = new BomNuocBridge(configStore.getConfig().bomNuoc);
const go2rtcWsProxy = new Go2rtcWsProxy();

const app = express();
app.disable('x-powered-by');

// Proxy go2rtc HTTP truoc express.json() de giu nguyen raw stream/body
app.use('/go2rtc', (req, res) => {
  // Rewrite req.url so proxyGo2rtcHttp preserves the full /go2rtc prefix stripping
  const orig = req.originalUrl || req.url;
  req.url = orig;
  proxyGo2rtcHttp(req, res, configStore.getConfig().camera);
});

app.use(express.json({ limit: '1mb' }));

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

app.put('/api/config/mqtt', (req, res) => {
  const updated = configStore.updateMqtt(req.body || {});
  mqttBridge.connect(updated.mqtt);
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
    mqttStatus: mqttBridge.getStatus(),
  });
});

app.put('/api/config/camera', async (req, res) => {
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

app.put('/api/config/aquacontrol', (req, res) => {
  const url = typeof req.body?.aquaControlUrl === 'string' ? req.body.aquaControlUrl : '';
  configStore.updateAquaControlUrl(url);
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
  });
});

app.put('/api/config/bomnuoc', (req, res) => {
  const updated = configStore.updateBomNuoc(req.body || {});
  bomNuocBridge.connect(updated.bomNuoc);
  res.json({
    ok: true,
    config: configStore.getPublicConfig(),
    link: bomNuocBridge.getLink(),
    kenh: bomNuocBridge.getKenh(),
  });
});

app.get('/api/bomnuoc/state', (_req, res) => {
  res.json(bomNuocBridge.getSnapshot());
});

app.post('/api/bomnuoc/cmd', async (req, res) => {
  const result = await bomNuocBridge.executeCommand(req.body);
  res.json(result);
});

app.delete('/api/bomnuoc/log', (_req, res) => {
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

// Phuc vu client da build o che do production (chiu duoc moi thu muc lam viec)
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const candidateNearDist = path.resolve(__dirname, '..', 'client');
const candidateCwd = path.resolve(process.cwd(), 'dist', 'client');
const clientDist = fs.existsSync(path.join(candidateNearDist, 'index.html'))
  ? candidateNearDist
  : candidateCwd;
const indexHtml = path.join(clientDist, 'index.html');
if (fs.existsSync(indexHtml)) {
  app.use(express.static(clientDist));
  app.use((req, res, next) => {
    if (
      req.method !== 'GET' ||
      req.path.startsWith('/api/') ||
      req.path.startsWith('/go2rtc') ||
      req.path.startsWith('/ws/')
    ) {
      return next();
    }
    res.sendFile(indexHtml);
  });
}

const server = http.createServer(app);

server.on('upgrade', (req, socket, head) => {
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

