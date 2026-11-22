import fs from 'node:fs';
import path from 'node:path';

export interface MqttConfig {
  host: string;
  cong: number;
  tls: boolean;
  taiKhoan: string;
  matKhau: string;
  clientId: string;
}

export interface CameraConfig {
  host: string;
  cong: number;
  tls: boolean;
  taiKhoan: string;
  matKhau: string;
}

export interface WeatherConfig {
  ten: string;
  lat: number;
  lon: number;
}

export interface BomNuocConfig {
  espHost: string;
  uuTienLan: boolean;
  brokerHost: string;
  brokerPort: number;
  brokerTls: boolean;
  brokerUser: string;
  brokerPass: string;
}

export interface AppServerConfig {
  mqtt: MqttConfig;
  camera: CameraConfig;
  weather: WeatherConfig;
  aquaControlUrl: string;
  bomNuoc: BomNuocConfig;
}

export interface PublicAppConfig {
  mqtt: Omit<MqttConfig, 'matKhau'> & { coMatKhau: boolean };
  camera: Omit<CameraConfig, 'matKhau'> & { coMatKhau: boolean };
  weather: WeatherConfig;
  aquaControlUrl: string;
  bomNuoc: Omit<BomNuocConfig, 'brokerPass'> & { coMatKhau: boolean };
}

export function chuanHoaHostBom(raw: string): string {
  return raw
    .trim()
    .replace(/^(https?|wss?|mqtts?):\/\//i, '')
    .split('/')[0]
    .trim();
}

function parseBool(v: string | undefined, fallback: boolean): boolean {
  if (v === undefined) return fallback;
  const s = v.trim().toLowerCase();
  if (s === 'true' || s === '1' || s === 'yes') return true;
  if (s === 'false' || s === '0' || s === 'no') return false;
  return fallback;
}

function parsePort(v: string | undefined, fallback: number): number {
  if (!v) return fallback;
  const n = Number.parseInt(v, 10);
  return Number.isFinite(n) && n >= 1 && n <= 65535 ? n : fallback;
}

function parseNum(v: string | undefined, fallback: number): number {
  if (!v) return fallback;
  const n = Number.parseFloat(v);
  return Number.isFinite(n) ? n : fallback;
}

function defaultFromEnv(): AppServerConfig {
  return {
    mqtt: {
      host: process.env.MQTT_HOST?.trim() || '192.168.0.50',
      cong: parsePort(process.env.MQTT_PORT, 1883),
      tls: parseBool(process.env.MQTT_TLS, false),
      taiKhoan: process.env.MQTT_USER?.trim() ?? 'iot',
      matKhau: process.env.MQTT_PASS ?? '',
      clientId: process.env.MQTT_CLIENT_ID?.trim() ?? '',
    },
    camera: {
      host: process.env.GO2RTC_HOST?.trim() || '192.168.0.50',
      cong: parsePort(process.env.GO2RTC_PORT, 1984),
      tls: parseBool(process.env.GO2RTC_TLS, false),
      taiKhoan: process.env.GO2RTC_USER?.trim() ?? '',
      matKhau: process.env.GO2RTC_PASS ?? '',
    },
    weather: {
      ten: process.env.WEATHER_NAME?.trim() || 'Hà Nội',
      lat: parseNum(process.env.WEATHER_LAT, 21.0278),
      lon: parseNum(process.env.WEATHER_LON, 105.8342),
    },
    aquaControlUrl: process.env.AQUACONTROL_URL?.trim() ?? '',
    bomNuoc: {
      espHost: chuanHoaHostBom(process.env.BOMNUOC_ESP_HOST ?? ''),
      uuTienLan: parseBool(process.env.BOMNUOC_PREFER_LAN, true),
      brokerHost: chuanHoaHostBom(process.env.BOMNUOC_MQTT_HOST ?? ''),
      brokerPort: parsePort(process.env.BOMNUOC_MQTT_PORT, 8883),
      brokerTls: parseBool(process.env.BOMNUOC_MQTT_TLS, true),
      brokerUser: process.env.BOMNUOC_MQTT_USER?.trim() ?? '',
      brokerPass: process.env.BOMNUOC_MQTT_PASS ?? '',
    },
  };
}

export class ConfigStore {
  private readonly filePath: string;
  private config: AppServerConfig;

  constructor(filePath?: string) {
    this.filePath =
      filePath ||
      process.env.CONFIG_PATH ||
      path.resolve(process.cwd(), 'data', 'config.json');
    this.config = this.load();
  }

  private load(): AppServerConfig {
    const base = defaultFromEnv();
    try {
      if (!fs.existsSync(this.filePath)) {
        return base;
      }
      const raw = fs.readFileSync(this.filePath, 'utf8');
      const parsed = JSON.parse(raw) as Partial<AppServerConfig>;
      return {
        mqtt: {
          host: typeof parsed.mqtt?.host === 'string' ? parsed.mqtt.host.trim() : base.mqtt.host,
          cong: typeof parsed.mqtt?.cong === 'number' && parsed.mqtt.cong >= 1 && parsed.mqtt.cong <= 65535
            ? Math.trunc(parsed.mqtt.cong)
            : base.mqtt.cong,
          tls: typeof parsed.mqtt?.tls === 'boolean' ? parsed.mqtt.tls : base.mqtt.tls,
          taiKhoan: typeof parsed.mqtt?.taiKhoan === 'string' ? parsed.mqtt.taiKhoan.trim() : base.mqtt.taiKhoan,
          matKhau: typeof parsed.mqtt?.matKhau === 'string' ? parsed.mqtt.matKhau : base.mqtt.matKhau,
          clientId: typeof parsed.mqtt?.clientId === 'string' ? parsed.mqtt.clientId.trim() : base.mqtt.clientId,
        },
        camera: {
          host: typeof parsed.camera?.host === 'string' ? parsed.camera.host.trim() : base.camera.host,
          cong: typeof parsed.camera?.cong === 'number' && parsed.camera.cong >= 1 && parsed.camera.cong <= 65535
            ? Math.trunc(parsed.camera.cong)
            : base.camera.cong,
          tls: typeof parsed.camera?.tls === 'boolean' ? parsed.camera.tls : base.camera.tls,
          taiKhoan: typeof parsed.camera?.taiKhoan === 'string' ? parsed.camera.taiKhoan.trim() : base.camera.taiKhoan,
          matKhau: typeof parsed.camera?.matKhau === 'string' ? parsed.camera.matKhau : base.camera.matKhau,
        },
        weather: {
          ten: typeof parsed.weather?.ten === 'string' && parsed.weather.ten.trim() ? parsed.weather.ten.trim() : base.weather.ten,
          lat: typeof parsed.weather?.lat === 'number' && parsed.weather.lat >= -90 && parsed.weather.lat <= 90
            ? parsed.weather.lat
            : base.weather.lat,
          lon: typeof parsed.weather?.lon === 'number' && parsed.weather.lon >= -180 && parsed.weather.lon <= 180
            ? parsed.weather.lon
            : base.weather.lon,
        },
        aquaControlUrl: typeof parsed.aquaControlUrl === 'string' ? parsed.aquaControlUrl.trim() : base.aquaControlUrl,
        bomNuoc: {
          espHost: typeof parsed.bomNuoc?.espHost === 'string' ? chuanHoaHostBom(parsed.bomNuoc.espHost) : base.bomNuoc.espHost,
          uuTienLan: typeof parsed.bomNuoc?.uuTienLan === 'boolean' ? parsed.bomNuoc.uuTienLan : base.bomNuoc.uuTienLan,
          brokerHost: typeof parsed.bomNuoc?.brokerHost === 'string' ? chuanHoaHostBom(parsed.bomNuoc.brokerHost) : base.bomNuoc.brokerHost,
          brokerPort: typeof parsed.bomNuoc?.brokerPort === 'number' && parsed.bomNuoc.brokerPort >= 1 && parsed.bomNuoc.brokerPort <= 65535
            ? Math.trunc(parsed.bomNuoc.brokerPort)
            : base.bomNuoc.brokerPort,
          brokerTls: typeof parsed.bomNuoc?.brokerTls === 'boolean' ? parsed.bomNuoc.brokerTls : base.bomNuoc.brokerTls,
          brokerUser: typeof parsed.bomNuoc?.brokerUser === 'string' ? parsed.bomNuoc.brokerUser.trim() : base.bomNuoc.brokerUser,
          brokerPass: typeof parsed.bomNuoc?.brokerPass === 'string' ? parsed.bomNuoc.brokerPass : base.bomNuoc.brokerPass,
        },
      };
    } catch {
      return base;
    }
  }

  private persist(): void {
    const dir = path.dirname(this.filePath);
    fs.mkdirSync(dir, { recursive: true });
    const tmp = `${this.filePath}.tmp`;
    fs.writeFileSync(tmp, JSON.stringify(this.config, null, 2), { encoding: 'utf8', mode: 0o600 });
    fs.renameSync(tmp, this.filePath);
  }

  getConfig(): AppServerConfig {
    return this.config;
  }

  getPublicConfig(): PublicAppConfig {
    const { matKhau: mqttPass, ...mqttRest } = this.config.mqtt;
    const { matKhau: camPass, ...camRest } = this.config.camera;
    const { brokerPass: bomPass, ...bomRest } = this.config.bomNuoc;
    return {
      mqtt: { ...mqttRest, coMatKhau: mqttPass.length > 0 },
      camera: { ...camRest, coMatKhau: camPass.length > 0 },
      weather: { ...this.config.weather },
      aquaControlUrl: this.config.aquaControlUrl,
      bomNuoc: { ...bomRest, coMatKhau: bomPass.length > 0 },
    };
  }

  updateMqtt(input: Partial<MqttConfig> & { xoaMatKhau?: boolean }): AppServerConfig {
    const prev = this.config.mqtt;
    const nextPass = input.xoaMatKhau
      ? ''
      : typeof input.matKhau === 'string' && input.matKhau.length > 0
        ? input.matKhau
        : prev.matKhau;
    const cong = typeof input.cong === 'number' && Number.isFinite(input.cong)
      ? Math.min(65535, Math.max(1, Math.trunc(input.cong)))
      : prev.cong;
    this.config = {
      ...this.config,
      mqtt: {
        host: typeof input.host === 'string' ? input.host.trim() : prev.host,
        cong,
        tls: typeof input.tls === 'boolean' ? input.tls : prev.tls,
        taiKhoan: typeof input.taiKhoan === 'string' ? input.taiKhoan.trim() : prev.taiKhoan,
        matKhau: nextPass,
        clientId: typeof input.clientId === 'string' ? input.clientId.trim() : prev.clientId,
      },
    };
    this.persist();
    return this.config;
  }

  updateCamera(input: Partial<CameraConfig> & { xoaMatKhau?: boolean }): AppServerConfig {
    const prev = this.config.camera;
    const nextPass = input.xoaMatKhau
      ? ''
      : typeof input.matKhau === 'string' && input.matKhau.length > 0
        ? input.matKhau
        : prev.matKhau;
    const cong = typeof input.cong === 'number' && Number.isFinite(input.cong)
      ? Math.min(65535, Math.max(1, Math.trunc(input.cong)))
      : prev.cong;
    this.config = {
      ...this.config,
      camera: {
        host: typeof input.host === 'string' ? input.host.trim() : prev.host,
        cong,
        tls: typeof input.tls === 'boolean' ? input.tls : prev.tls,
        taiKhoan: typeof input.taiKhoan === 'string' ? input.taiKhoan.trim() : prev.taiKhoan,
        matKhau: nextPass,
      },
    };
    this.persist();
    return this.config;
  }

  updateWeather(input: Partial<WeatherConfig>): AppServerConfig {
    const prev = this.config.weather;
    const ten = typeof input.ten === 'string' && input.ten.trim() ? input.ten.trim() : prev.ten;
    const lat = typeof input.lat === 'number' && Number.isFinite(input.lat) && input.lat >= -90 && input.lat <= 90
      ? input.lat
      : prev.lat;
    const lon = typeof input.lon === 'number' && Number.isFinite(input.lon) && input.lon >= -180 && input.lon <= 180
      ? input.lon
      : prev.lon;
    this.config = {
      ...this.config,
      weather: { ten, lat, lon },
    };
    this.persist();
    return this.config;
  }

  updateAquaControlUrl(url: string): AppServerConfig {
    this.config = {
      ...this.config,
      aquaControlUrl: url.trim(),
    };
    this.persist();
    return this.config;
  }

  updateBomNuoc(input: Partial<BomNuocConfig> & { xoaMatKhau?: boolean }): AppServerConfig {
    const prev = this.config.bomNuoc;
    const nextPass = input.xoaMatKhau
      ? ''
      : typeof input.brokerPass === 'string' && input.brokerPass.length > 0
        ? input.brokerPass
        : prev.brokerPass;
    const brokerPort =
      typeof input.brokerPort === 'number' && Number.isFinite(input.brokerPort)
        ? Math.min(65535, Math.max(1, Math.trunc(input.brokerPort)))
        : prev.brokerPort;
    this.config = {
      ...this.config,
      bomNuoc: {
        espHost: typeof input.espHost === 'string' ? chuanHoaHostBom(input.espHost) : prev.espHost,
        uuTienLan: typeof input.uuTienLan === 'boolean' ? input.uuTienLan : prev.uuTienLan,
        brokerHost: typeof input.brokerHost === 'string' ? chuanHoaHostBom(input.brokerHost) : prev.brokerHost,
        brokerPort,
        brokerTls: typeof input.brokerTls === 'boolean' ? input.brokerTls : prev.brokerTls,
        brokerUser: typeof input.brokerUser === 'string' ? input.brokerUser.trim() : prev.brokerUser,
        brokerPass: nextPass,
      },
    };
    this.persist();
    return this.config;
  }
}

