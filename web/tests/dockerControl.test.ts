import http from 'node:http';
import { describe, expect, it } from 'vitest';
import {
  executeDockerControlAction,
  fetchDockerControlStatus,
  isCoreProtectedContainer,
  validateDockerControlInput,
} from '../server/dockerControlProxy.js';
import { DemoData } from '../client/src/data/demoData.js';
import { gopServer, ServerTopic } from '../client/src/data/server.js';

describe('dockerControlProxy security & policy', () => {
  it('chan cung 3 container loi mosquitto, server-collector, smarthome-web va bien the', () => {
    for (const name of [
      'mosquitto',
      'homeserver-mosquitto-1',
      'server-collector',
      'server_collector',
      'smarthome-web',
      'smarthome_web',
    ]) {
      expect(isCoreProtectedContainer(name)).toBe(true);
      const check = validateDockerControlInput(name, 'stop');
      expect(check.ok).toBe(false);
      if (!check.ok) {
        expect(check.status).toBe(403);
      }
    }
  });

  it('chan hanh dong ngoai start/stop/restart va ten container chua path traversal', () => {
    const badAction = validateDockerControlInput('camera-recorder', 'rm');
    expect(badAction.ok).toBe(false);
    if (!badAction.ok) expect(badAction.status).toBe(400);

    const badName = validateDockerControlInput('../images', 'stop');
    expect(badName.ok).toBe(false);
    if (!badName.ok) expect(badName.status).toBe(400);

    const ok = validateDockerControlInput('camera-recorder', 'stop');
    expect(ok).toEqual({ ok: true, name: 'camera-recorder', action: 'stop' });
  });

  it('khong goi mang khi chua cau hinh DOCKER_CONTROL_TOKEN >= 16 ky tu', async () => {
    const st = await fetchDockerControlStatus({ host: '127.0.0.1', port: 1986, token: '' });
    expect(st.configured).toBe(false);
    expect(st.enabled).toBe(false);
    expect(st.protectedContainers).toContain('mosquitto');

    const res = await executeDockerControlAction(
      { host: '127.0.0.1', port: 1986, token: 'short' },
      'camera-recorder',
      'stop',
    );
    expect(res.ok).toBe(false);
    expect(res.status).toBe(503);
  });

  it('chuyen tiep Bearer token toi server-collector :1986 va nhan danh sach container moi', async () => {
    const token = 'secret-docker-control-token-2026';
    let seenAuth = '';
    let seenBody = '';

    const mockCollector = http.createServer((req, res) => {
      seenAuth = String(req.headers.authorization || '');
      if (req.method === 'GET' && req.url === '/api/docker/status') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(
          JSON.stringify({
            ok: true,
            enabled: true,
            protected_containers: ['mosquitto', 'server-collector', 'smarthome-web'],
            allowed_containers: ['camera-recorder', 'beszel'],
          }),
        );
        return;
      }
      if (req.method === 'POST' && req.url === '/api/docker/action') {
        const chunks: Buffer[] = [];
        req.on('data', (c: Buffer) => chunks.push(c));
        req.on('end', () => {
          seenBody = Buffer.concat(chunks).toString('utf8');
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(
            JSON.stringify({
              ok: true,
              message: "Container 'camera-recorder' đã dừng.",
              containers: [
                {
                  name: 'camera-recorder',
                  image: 'smarthome-camera-recorder:1.0',
                  state: 'exited',
                  status: 'Exited (0) 1 second ago',
                  cpu_percent: null,
                  mem_mb: null,
                  mem_limit_mb: null,
                  mem_percent: null,
                  protected: false,
                  controllable: true,
                },
              ],
            }),
          );
        });
        return;
      }
      res.writeHead(404);
      res.end();
    });

    await new Promise<void>((resolve) => mockCollector.listen(0, '127.0.0.1', resolve));
    const addr = mockCollector.address() as { port: number };

    try {
      const cfg = { host: '127.0.0.1', port: addr.port, token };

      const status = await fetchDockerControlStatus(cfg);
      expect(status.ok).toBe(true);
      expect(status.enabled).toBe(true);
      expect(status.allowedContainers).toEqual(['camera-recorder', 'beszel']);
      expect(seenAuth).toBe(`Bearer ${token}`);

      // Core container van bi chan truoc khi goi mang
      const blocked = await executeDockerControlAction(cfg, 'smarthome-web', 'stop');
      expect(blocked.ok).toBe(false);
      expect(blocked.status).toBe(403);

      // Allowed container thanh cong
      const act = await executeDockerControlAction(cfg, 'camera-recorder', 'stop');
      expect(act.ok).toBe(true);
      expect(act.status).toBe(200);
      expect(JSON.parse(seenBody)).toEqual({ name: 'camera-recorder', action: 'stop' });
      expect(act.containers).toHaveLength(1);
    } finally {
      await new Promise<void>((resolve) => mockCollector.close(() => resolve()));
    }
  });
});

describe('gopServer parses per-container CPU%, RAM MB and protection flags', () => {
  it('doc cpu_percent, mem_mb, mem_limit_mb, protected va controllable tu server/docker', () => {
    const initial = DemoData.initialServerState();
    const payload = JSON.stringify([
      {
        name: 'mosquitto',
        image: 'eclipse-mosquitto:2',
        state: 'running',
        status: 'Up 5 days',
        cpu_percent: 0.5,
        mem_mb: 16.4,
        mem_limit_mb: null,
        mem_percent: null,
        protected: false, // ke ca server quen dat flag, client van nhan dien container loi
        controllable: true,
      },
      {
        name: 'camera-recorder',
        image: 'smarthome-camera-recorder:1.0',
        state: 'running',
        status: 'Up 1 hour',
        cpu_percent: 58.3,
        mem_mb: 432.1,
        mem_limit_mb: 1024,
        mem_percent: 42.2,
        protected: false,
        controllable: true,
      },
    ]);

    const next = gopServer(initial, ServerTopic.DOCKER, payload);
    expect(next.isRealData).toBe(true);
    expect(next.containers).toHaveLength(2);
    expect(next.containers[0]).toMatchObject({
      name: 'mosquitto',
      cpuPercent: 0.5,
      memMb: 16.4,
      protected: true,
      controllable: false,
    });
    expect(next.containers[1]).toMatchObject({
      name: 'camera-recorder',
      cpuPercent: 58.3,
      memMb: 432.1,
      memLimitMb: 1024,
      memPercent: 42.2,
      protected: false,
      controllable: true,
    });
  });
});
