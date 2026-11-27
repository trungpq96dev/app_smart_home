import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { WebSocket } from 'ws';
import { ConfigStore } from '../server/configStore.js';
import { proxyCameraRecorderHttp } from '../server/cameraRecorderProxy.js';
import { fetchStreamList, proxyGo2rtcHttp } from '../server/go2rtcProxy.js';
import { MqttBridge } from '../server/mqttBridge.js';

const tempDirs: string[] = [];

afterEach(() => {
  while (tempDirs.length > 0) {
    const dir = tempDirs.pop();
    if (dir && fs.existsSync(dir)) {
      fs.rmSync(dir, { recursive: true, force: true });
    }
  }
});

describe('ConfigStore password handling', () => {
  it('luu va an mat khau khi getPublicConfig, giu nguyen khi bo trong va xoa khi yeu cau', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-cfg-'));
    tempDirs.push(tmpDir);
    const cfgPath = path.join(tmpDir, 'config.json');

    const store = new ConfigStore(cfgPath);
    store.updateMqtt({
      host: '192.168.0.50',
      cong: 1883,
      tls: false,
      taiKhoan: 'iot',
      matKhau: 's3cr3t-mqtt',
      clientId: 'web-test',
    });
    store.updateCamera({
      host: '192.168.0.50',
      cong: 1984,
      tls: false,
      taiKhoan: 'admin',
      matKhau: 's3cr3t-cam',
    });

    const pub = store.getPublicConfig();
    expect(pub.mqtt.coMatKhau).toBe(true);
    expect(pub.camera.coMatKhau).toBe(true);
    expect(JSON.stringify(pub)).not.toContain('s3cr3t-mqtt');
    expect(JSON.stringify(pub)).not.toContain('s3cr3t-cam');

    // Bo trong mat khau -> giu nguyen mat khau cu
    store.updateMqtt({ host: '192.168.0.51', matKhau: '' });
    expect(store.getConfig().mqtt.matKhau).toBe('s3cr3t-mqtt');
    expect(store.getPublicConfig().mqtt.coMatKhau).toBe(true);

    // Xoa ro rang -> xoa mat khau
    store.updateMqtt({ xoaMatKhau: true });
    store.updateCamera({ xoaMatKhau: true });
    expect(store.getConfig().mqtt.matKhau).toBe('');
    expect(store.getConfig().camera.matKhau).toBe('');
    expect(store.getPublicConfig().mqtt.coMatKhau).toBe(false);
    expect(store.getPublicConfig().camera.coMatKhau).toBe(false);
  });

  it('luu bomNuoc, an brokerPass va chuan hoa espHost', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sh-bom-cfg-'));
    tempDirs.push(tmpDir);
    const cfgPath = path.join(tmpDir, 'config.json');

    const store = new ConfigStore(cfgPath);
    store.updateBomNuoc({
      espHost: 'http://192.168.0.120/',
      uuTienLan: true,
      brokerHost: 'mqtts://broker.example.test/',
      brokerPort: 8883,
      brokerTls: true,
      brokerUser: 'pump-user',
      brokerPass: 's3cr3t-pump',
    });

    const pub = store.getPublicConfig();
    expect(pub.bomNuoc.espHost).toBe('192.168.0.120');
    expect(pub.bomNuoc.brokerHost).toBe('broker.example.test');
    expect(pub.bomNuoc.coMatKhau).toBe(true);
    expect(JSON.stringify(pub)).not.toContain('s3cr3t-pump');

    store.updateBomNuoc({ uuTienLan: false, brokerPass: '' });
    expect(store.getConfig().bomNuoc.brokerPass).toBe('s3cr3t-pump');
    expect(store.getPublicConfig().bomNuoc.coMatKhau).toBe(true);

    store.updateBomNuoc({ xoaMatKhau: true });
    expect(store.getConfig().bomNuoc.brokerPass).toBe('');
    expect(store.getPublicConfig().bomNuoc.coMatKhau).toBe(false);
  });
});

describe('go2rtcProxy security guarantees', () => {
  it('chi tra danh sach ten luong da sap xep, khong bao gio lo producers[].url va strip www-authenticate', async () => {
    let seenAuth = '';
    const mockGo2rtc = http.createServer((req, res) => {
      seenAuth = String(req.headers.authorization || '');
      if (req.url === '/api/streams') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(
          JSON.stringify({
            cam_b: { producers: [{ url: 'tuya://secret:pass@cloud' }] },
            cam_a: { producers: [] },
          }),
        );
        return;
      }
      res.writeHead(401, {
        'Content-Type': 'text/plain',
        'WWW-Authenticate': 'Basic realm="go2rtc"',
      });
      res.end('Unauthorized');
    });

    await new Promise<void>((resolve) => mockGo2rtc.listen(0, '127.0.0.1', resolve));
    const addr = mockGo2rtc.address() as { port: number };

    try {
      const camCfg = {
        host: '127.0.0.1',
        cong: addr.port,
        congGhiHinh: 1985,
        tls: false,
        taiKhoan: 'camuser',
        matKhau: 'campass',
      };

      const res = await fetchStreamList(camCfg);
      expect(res).toEqual({ ok: true, streams: ['cam_a', 'cam_b'] });
      expect(JSON.stringify(res)).not.toContain('secret');
      expect(JSON.stringify(res)).not.toContain('pass');
      expect(JSON.stringify(res)).not.toContain('producers');
      expect(seenAuth).toBe(`Basic ${Buffer.from('camuser:campass', 'utf8').toString('base64')}`);

      // Kiem tra proxy HTTP strip header WWW-Authenticate
      const proxyHost = http.createServer((req, resHttp) => {
        proxyGo2rtcHttp(req, resHttp, camCfg);
      });
      await new Promise<void>((resolve) => proxyHost.listen(0, '127.0.0.1', resolve));
      const proxyAddr = proxyHost.address() as { port: number };

      try {
        const proxied = await fetch(`http://127.0.0.1:${proxyAddr.port}/go2rtc/protected`);
        expect(proxied.status).toBe(401);
        expect(proxied.headers.get('www-authenticate')).toBeNull();
      } finally {
        await new Promise<void>((resolve) => proxyHost.close(() => resolve()));
      }
    } finally {
      await new Promise<void>((resolve) => mockGo2rtc.close(() => resolve()));
    }
  });
});

describe('MqttBridge WebSocket & publish allowlist', () => {
  it('gui snapshot khi noi WS, chan topic dien/* va cho phep smarthome/control di qua allowlist', async () => {
    const bridge = new MqttBridge({
      host: '',
      cong: 1883,
      tls: false,
      taiKhoan: '',
      matKhau: '',
      clientId: '',
    });

    const server = http.createServer((_req, res) => {
      res.statusCode = 404;
      res.end();
    });
    server.on('upgrade', (req, socket, head) => {
      if ((req.url || '').startsWith('/ws/mqtt')) {
        bridge.handleUpgrade(req, socket, head);
      } else {
        socket.destroy();
      }
    });

    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    const addr = server.address() as { port: number };

    let ws: WebSocket | null = null;
    try {
      ws = new WebSocket(`ws://127.0.0.1:${addr.port}/ws/mqtt`);
      const messages: Array<Record<string, unknown>> = [];

      const waitNextMessage = () =>
        new Promise<Record<string, unknown>>((resolve, reject) => {
          const timer = setTimeout(() => reject(new Error('WS timeout')), 3000);
          ws!.once('message', (data) => {
            clearTimeout(timer);
            const parsed = JSON.parse(data.toString()) as Record<string, unknown>;
            messages.push(parsed);
            resolve(parsed);
          });
        });

      const snapshotPromise = waitNextMessage();
      await new Promise<void>((resolve, reject) => {
        ws!.once('open', () => resolve());
        ws!.once('error', reject);
      });

      const snapshot = await snapshotPromise;
      expect(snapshot.type).toBe('snapshot');
      expect(snapshot.status).toEqual({ state: 'chua_cau_hinh' });

      // 1. Chan topic dien/*
      const denyPromise = waitNextMessage();
      ws.send(
        JSON.stringify({
          type: 'publish',
          reqId: 'deny1',
          topic: 'dien/ngay/sanluong',
          payload: '999',
        }),
      );
      const denyAck = await denyPromise;
      expect(denyAck.type).toBe('publish_ack');
      expect(denyAck.reqId).toBe('deny1');
      expect(denyAck.ok).toBe(false);
      expect(String(denyAck.error || '')).toContain('không nằm trong danh sách được phép ghi');

      // 2. Cho phep topic smarthome/control qua allowlist (tra not_connected vi chua noi broker)
      const allowPromise = waitNextMessage();
      ws.send(
        JSON.stringify({
          type: 'publish',
          reqId: 'allow1',
          topic: 'smarthome/control',
          payload: '{}',
        }),
      );
      const allowAck = await allowPromise;
      expect(allowAck).toEqual({
        type: 'publish_ack',
        reqId: 'allow1',
        ok: false,
        reason: 'not_connected',
        error: 'Chưa nối broker MQTT',
      });

      // 3. Cho phep topic camera/cmd di qua allowlist (v0.9.4)
      const camAllowPromise = waitNextMessage();
      ws.send(
        JSON.stringify({
          type: 'publish',
          reqId: 'allowCam',
          topic: 'camera/cmd',
          payload: '{"action":"SET_DETECT_PERSON","value":true}',
        }),
      );
      const camAllowAck = await camAllowPromise;
      expect(camAllowAck).toEqual({
        type: 'publish_ack',
        reqId: 'allowCam',
        ok: false,
        reason: 'not_connected',
        error: 'Chưa nối broker MQTT',
      });
    } finally {
      ws?.close();
      bridge.disconnect();
      await new Promise<void>((resolve) => server.close(() => resolve()));
    }
  });
});

describe('cameraRecorderProxy Range 206 & timeline/snapshot forwarding', () => {
  it('chuyen tiep Basic Auth, HTTP Range 206 Partial Content va POST /snapshot', async () => {
    let seenAuth = '';
    let seenRange = '';
    const sampleMp4 = Buffer.from('0123456789ABCDEF-MP4-SEGMENT-DATA', 'utf8');

    const mockRecorder = http.createServer((req, res) => {
      seenAuth = String(req.headers.authorization || '');
      if (req.url?.startsWith('/api/days')) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(
          JSON.stringify({
            camera: 'cua_chinh',
            days: [{ date: '2026-11-26', segment_count: 12, recorded_sec: 7200, person_events: 2 }],
          }),
        );
        return;
      }
      if (req.url === '/api/video/42') {
        seenRange = String(req.headers.range || '');
        const slice = sampleMp4.subarray(0, 10);
        res.writeHead(206, {
          'Content-Type': 'video/mp4',
          'Accept-Ranges': 'bytes',
          'Content-Range': `bytes 0-9/${sampleMp4.length}`,
          'Content-Length': String(slice.length),
        });
        res.end(slice);
        return;
      }
      if (req.method === 'POST' && req.url?.startsWith('/api/snapshot')) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true, event_id: 99, message: 'Đã chụp ảnh thành công' }));
        return;
      }
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Not found' }));
    });

    await new Promise<void>((resolve) => mockRecorder.listen(0, '127.0.0.1', resolve));
    const recAddr = mockRecorder.address() as { port: number };

    const proxyServer = http.createServer((req, res) => {
      proxyCameraRecorderHttp(req, res, {
        host: '127.0.0.1',
        cong: 1984,
        congGhiHinh: recAddr.port,
        tls: false,
        taiKhoan: 'iotcam',
        matKhau: 'secretcam',
      });
    });
    await new Promise<void>((resolve) => proxyServer.listen(0, '127.0.0.1', resolve));
    const proxyAddr = proxyServer.address() as { port: number };

    try {
      const daysResp = await fetch(
        `http://127.0.0.1:${proxyAddr.port}/api/camera/recorder/days?cam=cua_chinh`,
      );
      expect(daysResp.status).toBe(200);
      const daysJson = (await daysResp.json()) as { days: Array<{ date: string }> };
      expect(daysJson.days[0].date).toBe('2026-11-26');
      expect(seenAuth).toBe(`Basic ${Buffer.from('iotcam:secretcam', 'utf8').toString('base64')}`);

      const videoResp = await fetch(
        `http://127.0.0.1:${proxyAddr.port}/api/camera/recorder/video/42.mp4`,
        { headers: { Range: 'bytes=0-9' } },
      );
      expect(videoResp.status).toBe(206);
      expect(seenRange).toBe('bytes=0-9');
      expect(videoResp.headers.get('content-range')).toBe(`bytes 0-9/${sampleMp4.length}`);
      const buf = Buffer.from(await videoResp.arrayBuffer()).toString('utf8');
      expect(buf).toBe('0123456789');

      const snapResp = await fetch(
        `http://127.0.0.1:${proxyAddr.port}/api/camera/recorder/snapshot?cam=cua_chinh`,
        { method: 'POST' },
      );
      expect(snapResp.status).toBe(200);
      const snapJson = (await snapResp.json()) as { ok: boolean; event_id: number };
      expect(snapJson).toEqual({ ok: true, event_id: 99, message: 'Đã chụp ảnh thành công' });
    } finally {
      await new Promise<void>((resolve) => proxyServer.close(() => resolve()));
      await new Promise<void>((resolve) => mockRecorder.close(() => resolve()));
    }
  });
});

