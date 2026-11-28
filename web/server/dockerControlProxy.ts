import http from 'node:http';

export type DockerControlAction = 'start' | 'stop' | 'restart';

const ALLOWED_ACTIONS = new Set<string>(['start', 'stop', 'restart']);
const CONTAINER_NAME_RE = /^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$/;

export const CORE_PROTECTED_CONTAINERS = [
  'mosquitto',
  'server-collector',
  'smarthome-web',
] as const;

const CORE_PROTECTED_SUBSTRINGS = [
  'mosquitto',
  'server-collector',
  'server_collector',
  'smarthome-web',
  'smarthome_web',
];

export interface DockerControlConfig {
  host: string;
  port: number;
  token: string;
}

export interface DockerControlStatusResult {
  ok: boolean;
  configured: boolean;
  enabled: boolean;
  protectedContainers: string[];
  allowedContainers: string[];
  error?: string;
}

export interface DockerControlActionResult {
  ok: boolean;
  status: number;
  message?: string;
  error?: string;
  containers?: unknown[];
}

export function isCoreProtectedContainer(name: string): boolean {
  const low = name.trim().replace(/^\/+/, '').toLowerCase();
  if (!low) return true;
  return CORE_PROTECTED_SUBSTRINGS.some((sub) => low.includes(sub));
}

export function validateDockerControlInput(
  rawName: unknown,
  rawAction: unknown,
): { ok: true; name: string; action: DockerControlAction } | { ok: false; status: number; error: string } {
  const name = typeof rawName === 'string' ? rawName.trim().replace(/^\/+/, '') : '';
  const action = typeof rawAction === 'string' ? rawAction.trim().toLowerCase() : '';

  if (!name || !CONTAINER_NAME_RE.test(name)) {
    return { ok: false, status: 400, error: 'Tên container không hợp lệ.' };
  }
  if (!ALLOWED_ACTIONS.has(action)) {
    return {
      ok: false,
      status: 400,
      error: 'Hành động không hợp lệ (chỉ hỗ trợ start, stop, restart).',
    };
  }
  if (isCoreProtectedContainer(name)) {
    return {
      ok: false,
      status: 403,
      error: `Container lõi "${name}" nằm trong danh sách bảo vệ cứng, không được phép dừng hoặc khởi động lại từ xa.`,
    };
  }
  return { ok: true, name, action: action as DockerControlAction };
}

export function readDockerControlConfig(fallbackHost: string): DockerControlConfig {
  const host = (process.env.DOCKER_CONTROL_HOST || fallbackHost || '192.168.0.50').trim();
  const portParsed = Number.parseInt(process.env.DOCKER_CONTROL_PORT || '1986', 10);
  const port = Number.isFinite(portParsed) && portParsed >= 1 && portParsed <= 65535 ? portParsed : 1986;
  const token = (process.env.DOCKER_CONTROL_TOKEN || '').trim();
  return { host, port, token };
}

function requestJson(
  cfg: DockerControlConfig,
  method: 'GET' | 'POST',
  reqPath: string,
  bodyObj?: unknown,
  timeoutMs = 10_000,
): Promise<{ status: number; json: Record<string, unknown> }> {
  return new Promise((resolve, reject) => {
    const payload = bodyObj !== undefined ? JSON.stringify(bodyObj) : undefined;
    const headers: http.OutgoingHttpHeaders = {
      Authorization: `Bearer ${cfg.token}`,
      Accept: 'application/json',
    };
    if (payload !== undefined) {
      headers['Content-Type'] = 'application/json; charset=utf-8';
      headers['Content-Length'] = Buffer.byteLength(payload);
    }

    const req = http.request(
      {
        hostname: cfg.host,
        port: cfg.port,
        path: reqPath,
        method,
        headers,
        timeout: timeoutMs,
      },
      (res) => {
        const chunks: Buffer[] = [];
        res.on('data', (chunk: Buffer) => chunks.push(chunk));
        res.on('end', () => {
          const text = Buffer.concat(chunks).toString('utf8');
          let json: Record<string, unknown> = {};
          try {
            const parsed = JSON.parse(text);
            if (parsed && typeof parsed === 'object') {
              json = parsed as Record<string, unknown>;
            }
          } catch {
            json = { error: text || `Phản hồi không hợp lệ (HTTP ${res.statusCode || 502})` };
          }
          resolve({ status: res.statusCode || 502, json });
        });
      },
    );

    req.on('timeout', () => {
      req.destroy(new Error('Quá thời gian chờ phản hồi từ server-collector (:1986).'));
    });
    req.on('error', reject);

    if (payload !== undefined) {
      req.write(payload);
    }
    req.end();
  });
}

export async function fetchDockerControlStatus(
  cfg: DockerControlConfig,
): Promise<DockerControlStatusResult> {
  const defaultProtected = [...CORE_PROTECTED_CONTAINERS];
  if (!cfg.token || cfg.token.length < 16) {
    return {
      ok: true,
      configured: false,
      enabled: false,
      protectedContainers: defaultProtected,
      allowedContainers: [],
    };
  }

  try {
    const { status, json } = await requestJson(cfg, 'GET', '/api/docker/status', undefined, 5_000);
    if (status === 200 && json.ok) {
      const protectedContainers = Array.isArray(json.protected_containers)
        ? json.protected_containers.filter((x): x is string => typeof x === 'string')
        : defaultProtected;
      const allowedContainers = Array.isArray(json.allowed_containers)
        ? json.allowed_containers.filter((x): x is string => typeof x === 'string')
        : [];
      return {
        ok: true,
        configured: true,
        enabled: Boolean(json.enabled),
        protectedContainers,
        allowedContainers,
      };
    }
    return {
      ok: false,
      configured: true,
      enabled: false,
      protectedContainers: defaultProtected,
      allowedContainers: [],
      error: typeof json.error === 'string' ? json.error : `Lỗi kiểm tra Docker Control (HTTP ${status}).`,
    };
  } catch (err) {
    return {
      ok: false,
      configured: true,
      enabled: false,
      protectedContainers: defaultProtected,
      allowedContainers: [],
      error:
        err instanceof Error
          ? `Không gọi được server-collector (${cfg.host}:${cfg.port}): ${err.message}`
          : 'Không gọi được server-collector.',
    };
  }
}

export async function fetchDockerContainersLive(
  cfg: DockerControlConfig,
): Promise<{ ok: boolean; status: number; containers?: unknown[]; error?: string }> {
  if (!cfg.token || cfg.token.length < 16) {
    return {
      ok: false,
      status: 503,
      error: 'Chưa cấu hình DOCKER_CONTROL_TOKEN trên SmartHome Web.',
    };
  }
  try {
    const { status, json } = await requestJson(cfg, 'GET', '/api/docker/containers', undefined, 8_000);
    if (status === 200 && json.ok && Array.isArray(json.containers)) {
      return { ok: true, status: 200, containers: json.containers };
    }
    return {
      ok: false,
      status,
      error: typeof json.error === 'string' ? json.error : `Không lấy được danh sách container (HTTP ${status}).`,
    };
  } catch (err) {
    return {
      ok: false,
      status: 502,
      error:
        err instanceof Error
          ? `Không gọi được server-collector: ${err.message}`
          : 'Không gọi được server-collector.',
    };
  }
}

export async function executeDockerControlAction(
  cfg: DockerControlConfig,
  rawName: unknown,
  rawAction: unknown,
): Promise<DockerControlActionResult> {
  const checked = validateDockerControlInput(rawName, rawAction);
  if (!checked.ok) {
    return { ok: false, status: checked.status, error: checked.error };
  }

  if (!cfg.token || cfg.token.length < 16) {
    return {
      ok: false,
      status: 503,
      error: 'Điều khiển Docker chưa được cấu hình (thiếu DOCKER_CONTROL_TOKEN >= 16 ký tự trong .env).',
    };
  }

  try {
    const { status, json } = await requestJson(
      cfg,
      'POST',
      '/api/docker/action',
      { name: checked.name, action: checked.action },
      35_000,
    );
    if (status === 200 && json.ok) {
      return {
        ok: true,
        status: 200,
        message: typeof json.message === 'string' ? json.message : 'Đã thực thi lệnh Docker.',
        containers: Array.isArray(json.containers) ? json.containers : undefined,
      };
    }
    return {
      ok: false,
      status: status >= 400 ? status : 500,
      error:
        typeof json.error === 'string'
          ? json.error
          : typeof json.message === 'string'
            ? json.message
            : `Lệnh Docker thất bại (HTTP ${status}).`,
    };
  } catch (err) {
    return {
      ok: false,
      status: 502,
      error:
        err instanceof Error
          ? `Không kết nối được server-collector (${cfg.host}:${cfg.port}): ${err.message}`
          : 'Không kết nối được server-collector.',
    };
  }
}
