import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { AmLich } from '../data/amLich.js';
import {
  bomApDangChay,
  bomApDangKhoa,
  bomChinhDangChay,
  bomTrucTuyen,
  bonDay,
  daCauHinhBom,
  dangKhoaBom,
  mucNuocLit,
  mucNuocPhanTram,
  nhanCheDoBon,
  nhanKetNoiBom,
  nhietDoBomAp,
  nhietDoBomBon,
  nhietDoNuoc,
  phanTichLoiBomNuoc,
  type BomNuocCmd,
  type BomNuocConfigForm,
  type BomNuocState,
  type KenhBomNuoc,
  type KetQuaLenhBomRes,
  type LuongNuocThang,
  type PublicBomNuocConfig,
  type SuKienBom,
  type TrangThaiKetNoiBom,
} from '../data/bomnuoc.js';
import {
  CameraPlaybackClient,
  CameraTopic,
  createEmptyCameraRecorderState,
  gopCameraRecorder,
  isFaceGreetFresh,
  parseFaceGreetEvent,
  type CameraRecorderState,
  type FaceGreetEvent,
} from '../data/cameraRecorder.js';
import { phatChaoGiongNoi } from '../data/chaoGiongNoi.js';
import type { ConfirmPromptState } from '../components/ConfirmDialog.js';
import {
  buildOThietBiThat,
  createEmptyCongTacState,
  gopCongTac,
  HangChoCongTac,
  isBridgeSong,
  LenhCongTac,
  tenThietBiThat,
  timCongTac,
  trangThaiCongTacCua,
  type CongTacState,
  type OThietBi,
} from '../data/congtac.js';
import {
  createEmptyCuktechState,
  CuktechTopic,
  cuktechTongOat,
  gopCuktech,
  type CongSacId,
  type CuktechState,
} from '../data/cuktech.js';
import { DemoData } from '../data/demoData.js';
import {
  coSoLieuDien,
  createEmptyDienState,
  doTuoiDien,
  gopDien,
  tongKwhDien,
  type DienState,
  type KetNoi,
} from '../data/dien.js';
import { CongCuNha, type HanhDongNha, LoiLenh } from '../data/giongnoi/congCuNha.js';
import { TroLyGiongNoi } from '../data/giongnoi/troLyGiongNoi.js';
import {
  createEmptyPhienGiongNoi,
  loadVoiceConfig,
  loadVoiceUsage,
  MucDung,
  saveVoiceConfig,
  saveVoiceUsage,
  type CauHinhGiongNoi,
  type MucDungNgay,
  type PhienGiongNoi,
} from '../data/giongnoi/types.js';
import {
  HALLWAY_VIEWS,
  stoveWatts,
  tenCheDoPhong,
  type CameraFeed,
  type CameraIconKey,
  type DestRoute,
  type HallwayView,
  type QuickItem,
  type RoomMode,
  type RoomUi,
} from '../data/models.js';
import {
  gopServer,
  isCoreProtectedContainer,
  parseServerContainers,
  type DockerContainerAction,
  type ServerState,
} from '../data/server.js';
import {
  kiemTraSuCoKhanCap,
  loadScreensaverConfig,
  parseGasSensorPayload,
  parsePirMotionPayload,
  saveScreensaverConfig,
  type ScreensaverConfig,
  type SuCoKhanCap,
} from '../data/screensaver.js';
import { layThoiTiet, OpenMeteo, type ThoiTietUi } from '../data/thoitiet.js';
import { gopWifi, WifiTopic, type WifiState } from '../data/wifi.js';
import { SH } from '../theme/colors.js';

export type TrangThaiCam =
  | { state: 'chua_cau_hinh' }
  | { state: 'dang_do' }
  | { state: 'co'; ds: string[] }
  | { state: 'loi'; moTa: string };

export interface PublicMqttConfig {
  host: string;
  cong: number;
  tls: boolean;
  taiKhoan: string;
  clientId: string;
  coMatKhau: boolean;
}

export interface PublicCameraConfig {
  host: string;
  cong: number;
  congGhiHinh: number;
  tls: boolean;
  taiKhoan: string;
  coMatKhau: boolean;
}

export interface WeatherConfig {
  ten: string;
  lat: number;
  lon: number;
}

export interface PublicTelegramConfig {
  enabled: boolean;
  coBotToken: boolean;
  chatId: string;
  offlineMinutes: number;
  electricSpikePct: number;
  electricSpikeMinKwh: number;
  gasThresholdPpm: number;
  cooldownMinutes: number;
  pirTopic: string;
  gasTopic: string;
}

export interface TelegramAlertLogEntry {
  id: number;
  ts: number;
  key: string;
  title: string;
  message: string;
  ok: boolean;
  error?: string;
}

export interface TelegramAlertsStatus {
  enabled: boolean;
  configured: boolean;
  activeIncidents: {
    pumpLocked: boolean;
    gasLeak: boolean;
    serverOffline: boolean;
    routerOffline: boolean;
  };
  history: TelegramAlertLogEntry[];
}

export interface PublicAppConfig {
  mqtt: PublicMqttConfig;
  camera: PublicCameraConfig;
  weather: WeatherConfig;
  aquaControlUrl: string;
  bomNuoc: PublicBomNuocConfig;
  telegram: PublicTelegramConfig;
  adminPin: { enabled: boolean; daDatPin: boolean };
}

export interface ToastMessage {
  id: string;
  text: string;
  tone: 'info' | 'success' | 'warning' | 'error';
}

export interface DockerControlStatus {
  checked: boolean;
  configured: boolean;
  enabled: boolean;
  protectedContainers: string[];
  allowedContainers: string[];
  error?: string;
}

export interface GithubReleaseInfo {
  tagName: string;
  name: string;
  body: string;
  htmlUrl: string;
  publishedAt: string;
}

export type UpdateCheckState =
  | { status: 'idle' }
  | { status: 'checking' }
  | { status: 'up_to_date'; latestTag: string }
  | { status: 'available'; release: GithubReleaseInfo }
  | { status: 'error'; message: string };

export const APP_WEB_VERSION = '0.9.10-web';
export const ANDROID_BASE_VERSION = '0.9.10';

function isNewerVersion(remoteRaw: string, localRaw: string): boolean {
  const parse = (v: string) =>
    v
      .replace(/^v/i, '')
      .split('-')[0]
      .split('.')
      .map((n) => Number.parseInt(n, 10) || 0);
  const r = parse(remoteRaw);
  const l = parse(localRaw);
  const len = Math.max(r.length, l.length);
  for (let i = 0; i < len; i++) {
    const rv = r[i] ?? 0;
    const lv = l[i] ?? 0;
    if (rv > lv) return true;
    if (rv < lv) return false;
  }
  return false;
}

function goiYCameraFeed(streamKey: string, idx: number): CameraFeed {
  const norm = streamKey
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');

  const palette = [SH.Blue, SH.Green, SH.Amber, SH.Purple, SH.Cyan, SH.Teal];
  let name = streamKey;
  let icon: CameraIconKey = 'videocam';
  let accent = palette[idx % palette.length];

  if (['cuachinh', 'cua', 'front', 'door', 'gate', 'cong'].some((k) => norm.includes(k))) {
    name = streamKey === 'cua_chinh' || streamKey === 'front' ? 'Cửa chính' : streamKey;
    icon = 'door';
    accent = SH.Blue;
  } else if (['san', 'vuon', 'yard', 'garden', 'ngoai'].some((k) => norm.includes(k))) {
    name = streamKey === 'san_vuon' || streamKey === 'yard' ? 'Sân vườn' : streamKey;
    icon = 'grass';
    accent = SH.Green;
  } else if (['bep', 'kitchen'].some((k) => norm.includes(k))) {
    name = streamKey === 'nha_bep' || streamKey === 'kitchen' ? 'Nhà bếp' : streamKey;
    icon = 'kitchen';
    accent = SH.Amber;
  } else if (['hanhlang', 'hall', 'stairs', 'cauthang'].some((k) => norm.includes(k))) {
    name = streamKey === 'hanh_lang' || streamKey === 'hall' ? 'Hành lang' : streamKey;
    icon = 'stairs';
    accent = SH.Purple;
  }

  return {
    id: `cam-${streamKey}`,
    name,
    icon,
    accent,
    luong: streamKey,
  };
}

const GIAY_CHO_CUKTECH = 25;
const GIAY_CHO_NOI_LAI = 90;

const CHE_DO_HOP_LE: Record<string, RoomMode> = {
  SLEEP: 'SLEEP',
  NGU: 'SLEEP',
  NGỦ: 'SLEEP',
  HOME: 'HOME',
  NHA: 'HOME',
  O_NHA: 'HOME',
  NHÀ: 'HOME',
  AWAY: 'AWAY',
  VANG_NHA: 'AWAY',
  VẮNG: 'AWAY',
  'VẮNG NHÀ': 'AWAY',
  CUSTOM: 'CUSTOM',
  TUY_CHINH: 'CUSTOM',
  'TÙY CHỈNH': 'CUSTOM',
};

const MAN_HINH_HOP_LE: Record<string, { route?: DestRoute; sub?: 'chi_tiet_dien' | 'wifi' }> = {
  trang_chu: { route: 'home' },
  thiet_bi: { route: 'devices' },
  camera: { route: 'camera' },
  cam_bien: { route: 'sensors' },
  tu_dong_hoa: { route: 'automation' },
  cai_dat: { route: 'settings' },
  server: { route: 'server' },
  thong_ke: { route: 'stats' },
  tien_ich: { route: 'utilities' },
  chi_tiet_dien: { sub: 'chi_tiet_dien' },
  wifi: { sub: 'wifi' },
};

function defaultPublicConfig(): PublicAppConfig {
  return {
    mqtt: { host: '', cong: 1883, tls: false, taiKhoan: '', clientId: '', coMatKhau: false },
    camera: { host: '', cong: 1984, congGhiHinh: 1985, tls: false, taiKhoan: '', coMatKhau: false },
    weather: { ten: OpenMeteo.TEN_DIA_DIEM, lat: OpenMeteo.LAT, lon: OpenMeteo.LON },
    aquaControlUrl: '',
    bomNuoc: {
      espHost: '',
      uuTienLan: true,
      brokerHost: '',
      brokerPort: 8883,
      brokerTls: true,
      brokerUser: '',
      coMatKhau: false,
    },
    telegram: {
      enabled: false,
      coBotToken: false,
      chatId: '',
      offlineMinutes: 5,
      electricSpikePct: 40,
      electricSpikeMinKwh: 3,
      gasThresholdPpm: 200,
      cooldownMinutes: 30,
      pirTopic: 'smarthome/sensor/pir',
      gasTopic: 'smarthome/sensor/gas',
    },
    adminPin: { enabled: true, daDatPin: true },
  };
}

interface SmartHomeContextValue {
  // Navigation
  currentDest: DestRoute;
  selectedRoomDetailId: string | null;
  chiTietDienOpen: boolean;
  wifiOpen: boolean;
  navigateDest: (route: DestRoute) => void;
  openRoomDetail: (roomId: string) => void;
  openChiTietDien: () => void;
  openWifi: () => void;
  closeSubScreen: () => void;

  // Config & Connection
  config: PublicAppConfig;
  configLoaded: boolean;
  gatewayOnline: boolean;
  mqttStatus: KetNoi;
  cameraStatus: TrangThaiCam;
  nowMs: number;
  toasts: ToastMessage[];
  dismissToast: (id: string) => void;
  showToast: (text: string, tone?: ToastMessage['tone']) => void;

  // Live States
  dien: DienState;
  congtac: CongTacState;
  cuktech: CuktechState;
  server: ServerState;
  wifi: WifiState;
  thoiTiet: ThoiTietUi;
  cameraRecorder: CameraRecorderState;
  dangChupAnh: boolean;
  choCongTacIds: Record<string, boolean>;
  choCongSac: Partial<Record<CongSacId, boolean>>;
  choCaiDatCuktech: Record<number, boolean>;
  dangDoiCuktechCong: Partial<Record<CongSacId, boolean>>;
  dangDoiCuktechCaiDat: boolean;
  dangNoiLaiCuktech: boolean;

  // Demo Room / Home UI States
  rooms: RoomUi[];
  selectedRoomId: string;
  selectedRoom: RoomUi;
  selectRoom: (id: string) => void;
  quickItems: QuickItem[];
  cameras: CameraFeed[];
  selectedCameraIndex: number;
  selectCamera: (index: number) => void;
  chupAnhCamera: (tenLuong: string) => Promise<void>;
  datCanhNguoiCamera: (bat: boolean) => void;
  chaoMatHienTai: FaceGreetEvent | null;
  anPopupChaoMat: () => void;
  docThuChaoMat: (ten: string) => void;
  dieuKhienPtz: (cam: string, direction: string, speed?: number) => Promise<void>;
  goiPtzPreset: (cam: string, preset: string) => Promise<void>;
  datCheDoRiengTuCamera: (bat: boolean) => Promise<void>;

  // Confirm Dialog for sensitive actions (v0.9.5)
  confirmPrompt: ConfirmPromptState | null;
  requireConfirm: (opts: ConfirmPromptState, fn: () => void | Promise<void>) => void;
  acceptConfirm: () => void;
  cancelConfirm: () => void;

  // Quick & Room Controls
  toggleQuickItem: (id: string) => void;
  thaoTacNhanhPhong: (roomId: string, actionId: string) => void;
  toggleRoomLight: (roomId: string) => void;
  toggleRoomFan: (roomId: string) => void;
  toggleRoomAc: (roomId: string) => void;
  stepAcTemp: (roomId: string, delta: number) => void;
  setRoomMode: (roomId: string, mode: RoomMode) => void;
  toggleRoomSchedule: (roomId: string) => void;
  setLightBrightness: (roomId: string, pct: number) => void;
  setFanSpeed: (roomId: string, speed: number) => void;
  toggleRoomTv: (roomId: string) => void;
  setCurtainOpen: (roomId: string, pct: number) => void;
  toggleCurtain: (roomId: string) => void;
  setHoodSpeed: (roomId: string, speed: number) => void;
  toggleStove: (roomId: string) => void;
  stepStoveLevel: (roomId: string, delta: number) => void;
  setGasAlert: (roomId: string, alert: boolean) => void;
  triggerTestGasAlert: (roomId: string) => void;
  dismissGasAlert: (roomId: string) => void;
  setHallView: (roomId: string, view: HallwayView) => void;
  toggleHallFrontLight: (roomId: string) => void;
  setHallFrontBrightness: (roomId: string, pct: number) => void;
  toggleHallSurroundingLight: (roomId: string) => void;
  setHallSurroundingBrightness: (roomId: string, pct: number) => void;
  toggleHallAllLights: (roomId: string) => void;
  toggleHallDoorLock: (roomId: string) => void;
  toggleHallCamera: (roomId: string) => void;
  toggleHallIrMode: (roomId: string) => void;
  toggleHallMotionAuto: (roomId: string) => void;
  triggerHallMotionTest: (roomId: string) => void;
  clearHallMotion: (roomId: string) => void;

  // Real Hardware Controls
  oThietBi: (phong: string, thietBi: string, batMau: boolean) => OThietBi;
  bamCongTac: (phong: string, thietBi: string, guiTuGiong?: boolean) => void;
  datCongSac: (id: CongSacId, bat: boolean) => void;
  datCaiDatCuktech: (piid: number, giaTri: number) => void;
  noiLaiCuktech: () => void;
  guiLenhWifi: (cmd: Record<string, unknown> | string, okMsg?: string) => boolean;
  khoiDongLaiWifi: () => boolean;
  toggleWifiSecurity: (key: string) => void;
  toggleWifiTrusted: (id: string) => void;
  toggleWifiBlocked: (id: string) => void;

  // Services / Settings actions
  taiThoiTiet: () => Promise<void>;
  taiDanhSachCamera: () => Promise<void>;
  luuMqttConfig: (form: {
    host: string;
    cong: number;
    tls: boolean;
    taiKhoan: string;
    matKhau?: string;
    xoaMatKhau?: boolean;
    clientId: string;
  }) => Promise<boolean>;
  luuCameraConfig: (form: {
    host: string;
    cong: number;
    congGhiHinh?: number;
    tls: boolean;
    taiKhoan: string;
    matKhau?: string;
    xoaMatKhau?: boolean;
  }) => Promise<boolean>;
  luuWeatherConfig: (form: WeatherConfig) => Promise<boolean>;
  luuAquaControlUrl: (url: string) => Promise<boolean>;

  // AquaControl Pump System (bom_nuoc)
  bomNuoc: BomNuocState | null;
  luongNuocThang: LuongNuocThang[];
  ketNoiBom: TrangThaiKetNoiBom;
  kenhBom: KenhBomNuoc;
  choBomNuoc: Record<string, boolean>;
  nhatKyBom: SuKienBom[];
  luuBomNuocConfig: (form: BomNuocConfigForm) => Promise<boolean>;
  datCheDoBon: (mode: 'auto' | 'on' | 'off') => Promise<void>;
  datBomTangAp: (bat: boolean) => Promise<void>;
  datDenPhuBom: (bat: boolean) => Promise<void>;
  xoaLoiBom: () => Promise<void>;
  tatCoiBom: () => Promise<void>;
  thuCoiBom: () => Promise<void>;
  luuBaoVeBom: (dry: number, rest: number, run: number, pulses: number, minFlow: number) => Promise<void>;
  ghiMocCanBom: () => Promise<void>;
  ghiMocDayBom: () => Promise<void>;
  datLaiDemLuuLuong: () => Promise<void>;
  xoaLichSuNuocTrenBo: () => Promise<void>;
  xoaNhatKyBomCucBo: () => Promise<void>;

  // Voice Assistant
  voiceConfig: CauHinhGiongNoi;
  voiceUsage: MucDungNgay[];
  phienGiongNoi: PhienGiongNoi;
  voiceSheetOpen: boolean;
  setVoiceSheetOpen: (open: boolean) => void;
  updateVoiceConfig: (c: CauHinhGiongNoi) => void;
  clearVoiceUsage: () => void;
  batTatGiongNoi: () => void;
  tatGiongNoi: () => void;
  ngatLoiGiongNoi: () => void;
  xoaLoiGiongNoi: () => void;

  // Update Check
  updateCheck: UpdateCheckState;
  checkGithubUpdate: () => Promise<void>;

  // Ambient Night Screensaver & Emergency Banner
  screensaverConfig: ScreensaverConfig;
  updateScreensaverConfig: (cfg: ScreensaverConfig) => void;
  screensaverActive: boolean;
  enterScreensaver: () => void;
  wakeScreensaver: () => void;
  emergencyAlert: SuCoKhanCap | null;
  dismissEmergencyAlert: () => void;
  mqttGasAlert: boolean;
  mqttGasPpm: number | null;

  // Telegram Alerts
  telegramStatus: TelegramAlertsStatus | null;
  refreshTelegramStatus: () => Promise<void>;
  luuTelegramConfig: (form: {
    enabled: boolean;
    botToken?: string;
    xoaBotToken?: boolean;
    chatId: string;
    offlineMinutes: number;
    electricSpikePct: number;
    electricSpikeMinKwh: number;
    gasThresholdPpm: number;
    cooldownMinutes: number;
    pirTopic: string;
    gasTopic: string;
  }) => Promise<boolean>;
  testTelegramAlert: () => Promise<void>;

  // Admin 4-digit PIN Lock
  adminLockEnabled: boolean;
  adminUnlocked: boolean;
  pinPrompt: { label: string; error: string | null; busy: boolean } | null;
  requireAdminPin: (actionLabel: string, fn: () => void | Promise<void>) => void;
  submitAdminPin: (pin: string) => Promise<void>;
  cancelAdminPin: () => void;
  lockAdmin: () => Promise<void>;
  updateAdminPin: (form: { currentPin: string; newPin?: string; enabled?: boolean }) => Promise<{ ok: boolean; error?: string }>;

  // Safe Docker Container Control (via Gateway + server-collector :1986, NO MQTT)
  dockerControlStatus: DockerControlStatus;
  dockerBusyName: string | null;
  refreshDockerControlStatus: () => Promise<void>;
  refreshDockerContainers: () => Promise<void>;
  controlDockerContainer: (name: string, action: DockerContainerAction) => Promise<{ ok: boolean; error?: string }>;
}

const SmartHomeContext = createContext<SmartHomeContextValue | null>(null);

export function SmartHomeProvider({ children }: { children: React.ReactNode }) {
  // Navigation
  const [currentDest, setCurrentDest] = useState<DestRoute>('home');
  const [selectedRoomDetailId, setSelectedRoomDetailId] = useState<string | null>(null);
  const [chiTietDienOpen, setChiTietDienOpen] = useState(false);
  const [wifiOpen, setWifiOpen] = useState(false);

  // Config + Gateway state
  const [config, setConfig] = useState<PublicAppConfig>(defaultPublicConfig);
  const [configLoaded, setConfigLoaded] = useState(false);
  const [gatewayOnline, setGatewayOnline] = useState(true);
  const [mqttStatus, setMqttStatus] = useState<KetNoi>({ state: 'chua_cau_hinh' });
  const [cameraStatus, setCameraStatus] = useState<TrangThaiCam>({ state: 'chua_cau_hinh' });
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Telemetry / Hardware states
  const [dien, setDien] = useState<DienState>(createEmptyDienState);
  const [congtac, setCongtac] = useState<CongTacState>(createEmptyCongTacState);
  const [cuktech, setCuktech] = useState<CuktechState>(createEmptyCuktechState);
  const [server, setServer] = useState<ServerState>(() => DemoData.initialServerState());
  const [dockerControlStatus, setDockerControlStatus] = useState<DockerControlStatus>({
    checked: false,
    configured: false,
    enabled: false,
    protectedContainers: ['mosquitto', 'server-collector', 'smarthome-web'],
    allowedContainers: [],
  });
  const [dockerBusyName, setDockerBusyName] = useState<string | null>(null);
  const [wifi, setWifi] = useState<WifiState>(() => DemoData.initialWifiState());
  const [thoiTiet, setThoiTiet] = useState<ThoiTietUi>({ state: 'dang_tai' });
  const [cameraRecorder, setCameraRecorder] = useState<CameraRecorderState>(createEmptyCameraRecorderState);
  const [chaoMatHienTai, setChaoMatHienTai] = useState<FaceGreetEvent | null>(null);
  const chaoMatTimerRef = useRef<number | null>(null);
  const [confirmPrompt, setConfirmPrompt] = useState<ConfirmPromptState | null>(null);
  const pendingConfirmActionRef = useRef<(() => void | Promise<void>) | null>(null);
  const [dangChupAnh, setDangChupAnh] = useState(false);
  const [selectedCameraIndex, setSelectedCameraIndex] = useState(0);

  // Pending commands & deadlines
  const hangChoRef = useRef(new HangChoCongTac());
  const [choCongTacIds, setChoCongTacIds] = useState<Record<string, boolean>>({});
  const cuktechCongExpected = useRef<Partial<Record<CongSacId, { bat: boolean; deadline: number }>>>({});
  const cuktechCaiDatExpected = useRef<Record<number, { giaTri: number; deadline: number }>>({});
  const cuktechNoiLaiDeadline = useRef<number>(0);
  const [choCongSac, setChoCongSac] = useState<Partial<Record<CongSacId, boolean>>>({});
  const [choCaiDatCuktech, setChoCaiDatCuktech] = useState<Record<number, boolean>>({});
  const [dangNoiLaiCuktech, setDangNoiLaiCuktech] = useState(false);
  const hallMotionTimersRef = useRef<Record<string, number>>({});

  const dangDoiCuktechCong = useMemo(() => {
    const out: Partial<Record<CongSacId, boolean>> = {};
    for (const k of Object.keys(choCongSac) as CongSacId[]) out[k] = true;
    return out;
  }, [choCongSac]);
  const dangDoiCuktechCaiDat = Object.keys(choCaiDatCuktech).length > 0;

  // Demo Room / UI states
  const [rooms, setRooms] = useState<RoomUi[]>(() => DemoData.rooms());
  const [selectedRoomId, setSelectedRoomId] = useState<string>('living');
  const [quickItems, setQuickItems] = useState<QuickItem[]>(() => DemoData.quickItems());

  const cameraConfigured = config.camera.host.trim().length > 0 && config.camera.cong >= 1;
  const cameras = useMemo<CameraFeed[]>(() => {
    if (!cameraConfigured || cameraStatus.state === 'chua_cau_hinh') {
      return DemoData.cameras();
    }
    if (cameraStatus.state === 'co') {
      return cameraStatus.ds.map((s, idx) => goiYCameraFeed(s, idx));
    }
    return [];
  }, [cameraConfigured, cameraStatus]);

  useEffect(() => {
    if (cameras.length > 0 && selectedCameraIndex >= cameras.length) {
      setSelectedCameraIndex(0);
    }
  }, [cameras.length, selectedCameraIndex]);

  const selectCamera = useCallback(
    (index: number) => {
      setSelectedCameraIndex(Math.max(0, Math.min(Math.max(0, cameras.length - 1), index)));
    },
    [cameras.length],
  );

  // Voice assistant states
  const [voiceConfig, setVoiceConfig] = useState<CauHinhGiongNoi>(() => loadVoiceConfig());
  const [voiceUsage, setVoiceUsage] = useState<MucDungNgay[]>(() => loadVoiceUsage());
  const [phienGiongNoi, setPhienGiongNoi] = useState<PhienGiongNoi>(createEmptyPhienGiongNoi);
  const [voiceSheetOpen, setVoiceSheetOpen] = useState(false);

  // Update check
  const [updateCheck, setUpdateCheck] = useState<UpdateCheckState>({ status: 'idle' });

  // Screensaver, Gas MQTT, Emergency Alert, Telegram & Admin PIN
  const [screensaverConfig, setScreensaverConfig] = useState<ScreensaverConfig>(() => loadScreensaverConfig());
  const [screensaverActive, setScreensaverActive] = useState(false);
  const [mqttGasAlert, setMqttGasAlert] = useState(false);
  const [mqttGasPpm, setMqttGasPpm] = useState<number | null>(null);
  const [dismissedEmergencyKey, setDismissedEmergencyKey] = useState<string | null>(null);
  const [telegramStatus, setTelegramStatus] = useState<TelegramAlertsStatus | null>(null);
  const [adminUnlocked, setAdminUnlocked] = useState(false);
  const [pinPrompt, setPinPrompt] = useState<{ label: string; error: string | null; busy: boolean } | null>(null);
  const adminTokenRef = useRef<string | null>(null);
  const adminUnlockExpiryRef = useRef<number>(0);
  const pendingAdminActionRef = useRef<(() => void | Promise<void>) | null>(null);
  const lastInteractionRef = useRef<number>(Date.now());
  const screensaverConfigRef = useRef(screensaverConfig);
  screensaverConfigRef.current = screensaverConfig;
  const screensaverActiveRef = useRef(screensaverActive);
  screensaverActiveRef.current = screensaverActive;

  // AquaControl Pump System (bom_nuoc)
  const [bomNuoc, setBomNuoc] = useState<BomNuocState | null>(null);
  const [luongNuocThang, setLuongNuocThang] = useState<LuongNuocThang[]>([]);
  const [ketNoiBom, setKetNoiBom] = useState<TrangThaiKetNoiBom>({ state: 'chua_cau_hinh' });
  const [kenhBom, setKenhBom] = useState<KenhBomNuoc>('KHONG');
  const [wsPendingBom, setWsPendingBom] = useState<Record<string, boolean>>({});
  const [localPendingBom, setLocalPendingBom] = useState<Record<string, boolean>>({});
  const [nhatKyBom, setNhatKyBom] = useState<SuKienBom[]>([]);
  const choBomNuoc = useMemo(
    () => ({ ...wsPendingBom, ...localPendingBom }),
    [wsPendingBom, localPendingBom],
  );

  // Refs synced to latest states
  const roomsRef = useRef(rooms);
  roomsRef.current = rooms;
  const quickItemsRef = useRef(quickItems);
  quickItemsRef.current = quickItems;
  const congtacRef = useRef(congtac);
  congtacRef.current = congtac;
  const dienRef = useRef(dien);
  dienRef.current = dien;
  const serverRef = useRef(server);
  serverRef.current = server;
  const wifiRef = useRef(wifi);
  wifiRef.current = wifi;
  const cuktechRef = useRef(cuktech);
  cuktechRef.current = cuktech;
  const thoiTietRef = useRef(thoiTiet);
  thoiTietRef.current = thoiTiet;
  const cameraRecorderRef = useRef(cameraRecorder);
  cameraRecorderRef.current = cameraRecorder;
  const mqttStatusRef = useRef(mqttStatus);
  mqttStatusRef.current = mqttStatus;
  const configRef = useRef(config);
  configRef.current = config;
  const voiceUsageRef = useRef(voiceUsage);
  voiceUsageRef.current = voiceUsage;
  const bomNuocRef = useRef(bomNuoc);
  bomNuocRef.current = bomNuoc;
  const ketNoiBomRef = useRef(ketNoiBom);
  ketNoiBomRef.current = ketNoiBom;
  const kenhBomRef = useRef(kenhBom);
  kenhBomRef.current = kenhBom;
  const currentDestRef = useRef(currentDest);
  currentDestRef.current = currentDest;
  const bomWsRef = useRef<WebSocket | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const pendingPublishResolvers = useRef(
    new Map<string, (ack: { ok: boolean; reason?: string; error?: string }) => void>(),
  );
  const popHandlingRef = useRef(false);

  // Toast helpers
  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((text: string, tone: ToastMessage['tone'] = 'info') => {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2, 6)}`;
    setToasts((prev) => [...prev.slice(-3), { id, text, tone }]);
    window.setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4500);
  }, []);

  // Navigation + Browser/iOS Swipe-Back History integration
  const pushNavHistory = useCallback((stateObj: Record<string, unknown>) => {
    if (typeof window === 'undefined' || popHandlingRef.current) return;
    try {
      window.history.pushState(stateObj, '');
    } catch {
      // ignore sandboxed history errors
    }
  }, []);

  const navigateDest = useCallback(
    (route: DestRoute) => {
      setCurrentDest(route);
      setSelectedRoomDetailId(null);
      setChiTietDienOpen(false);
      setWifiOpen(false);
      pushNavHistory({ dest: route, room: null, sub: null });
    },
    [pushNavHistory],
  );

  const openRoomDetail = useCallback(
    (roomId: string) => {
      setSelectedRoomId(roomId);
      setSelectedRoomDetailId(roomId);
      setChiTietDienOpen(false);
      setWifiOpen(false);
      pushNavHistory({ dest: currentDestRef.current, room: roomId, sub: null });
    },
    [pushNavHistory],
  );

  const openChiTietDien = useCallback(() => {
    setChiTietDienOpen(true);
    setSelectedRoomDetailId(null);
    setWifiOpen(false);
    pushNavHistory({ dest: currentDestRef.current, room: null, sub: 'chi_tiet_dien' });
  }, [pushNavHistory]);

  const openWifi = useCallback(() => {
    setWifiOpen(true);
    setSelectedRoomDetailId(null);
    setChiTietDienOpen(false);
    pushNavHistory({ dest: currentDestRef.current, room: null, sub: 'wifi' });
  }, [pushNavHistory]);

  const closeSubScreen = useCallback(() => {
    setSelectedRoomDetailId(null);
    setChiTietDienOpen(false);
    setWifiOpen(false);
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    try {
      window.history.replaceState({ dest: 'home', room: null, sub: null }, '');
    } catch {
      // ignore
    }
    const onPopState = (ev: PopStateEvent) => {
      popHandlingRef.current = true;
      try {
        if (screensaverActiveRef.current) {
          setScreensaverActive(false);
          return;
        }
        const st = ev.state as { dest?: DestRoute; room?: string | null; sub?: string | null } | null;
        if (st && st.dest) {
          setCurrentDest(st.dest);
          setSelectedRoomDetailId(st.room ?? null);
          setChiTietDienOpen(st.sub === 'chi_tiet_dien');
          setWifiOpen(st.sub === 'wifi');
        } else {
          setSelectedRoomDetailId(null);
          setChiTietDienOpen(false);
          setWifiOpen(false);
          setCurrentDest('home');
        }
      } finally {
        popHandlingRef.current = false;
      }
    };
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, []);

  // Cleanup hall motion timers on unmount
  useEffect(() => {
    const timers = hallMotionTimersRef.current;
    return () => {
      for (const t of Object.values(timers)) window.clearTimeout(t);
    };
  }, []);

  // MQTT Publish qua Gateway WS
  const publishMqtt = useCallback(
    (topic: string, payload: string): Promise<{ ok: boolean; reason?: string; error?: string }> => {
      const ws = wsRef.current;
      if (!ws || ws.readyState !== WebSocket.OPEN) {
        return Promise.resolve({ ok: false, reason: 'not_connected', error: 'Chưa nối WebSocket gateway' });
      }
      const reqId = LenhCongTac.maLenh();
      return new Promise((resolve) => {
        const timer = window.setTimeout(() => {
          pendingPublishResolvers.current.delete(reqId);
          resolve({ ok: false, reason: 'timeout', error: 'Hết thời gian phản hồi gateway' });
        }, 5000);
        pendingPublishResolvers.current.set(reqId, (ack) => {
          window.clearTimeout(timer);
          resolve(ack);
        });
        ws.send(JSON.stringify({ type: 'publish', reqId, topic, payload }));
      });
    },
    [],
  );

  // Dong bo trang thai cong tac that tu broker vao room UI (khop HomeViewModel.thuTrangThaiThat)
  const syncCongTacVaoRooms = useCallback((ct: CongTacState) => {
    setRooms((prev) =>
      prev.map((r) => {
        let next = r;
        const batDen = trangThaiCongTacCua(ct, r.id, 'den')?.bat;
        if (typeof batDen === 'boolean') {
          const doiSangBat = !next.lightOn && batDen;
          next = {
            ...next,
            lightOn: batDen,
            lightBrightness:
              doiSangBat && (next.lightBrightness <= 25 || next.mode === 'SLEEP')
                ? 100
                : next.lightBrightness,
            mode:
              doiSangBat && (next.mode === 'SLEEP' || next.mode === 'AWAY')
                ? 'CUSTOM'
                : next.mode,
          };
        }
        const batQuat = trangThaiCongTacCua(ct, r.id, 'quat')?.bat;
        if (typeof batQuat === 'boolean') {
          next = { ...next, fanOn: batQuat };
        }
        return next;
      }),
    );
  }, []);

  const syncCuktechPendingStates = useCallback((next: CuktechState) => {
    let congUpdated = false;
    for (const id of Object.keys(cuktechCongExpected.current) as CongSacId[]) {
      const exp = cuktechCongExpected.current[id];
      if (exp && next.caiDat?.congBat[id] === exp.bat) {
        delete cuktechCongExpected.current[id];
        congUpdated = true;
      }
    }
    if (congUpdated) {
      const snap: Partial<Record<CongSacId, boolean>> = {};
      for (const [k, v] of Object.entries(cuktechCongExpected.current) as Array<
        [CongSacId, { bat: boolean; deadline: number }]
      >) {
        if (v) snap[k] = v.bat;
      }
      setChoCongSac(snap);
    }

    let cdUpdated = false;
    for (const rawKey of Object.keys(cuktechCaiDatExpected.current)) {
      const piid = Number(rawKey);
      const exp = cuktechCaiDatExpected.current[piid];
      if (exp && next.caiDat?.tho[piid] === exp.giaTri) {
        delete cuktechCaiDatExpected.current[piid];
        cdUpdated = true;
      }
    }
    if (cdUpdated) {
      const snap: Record<number, boolean> = {};
      for (const rawKey of Object.keys(cuktechCaiDatExpected.current)) {
        snap[Number(rawKey)] = true;
      }
      setChoCaiDatCuktech(snap);
    }
  }, []);

  const danhThucManChoTuSuKien = useCallback(() => {
    lastInteractionRef.current = Date.now();
    if (screensaverActiveRef.current) {
      setScreensaverActive(false);
      setCurrentDest('home');
      setSelectedRoomDetailId(null);
      setChiTietDienOpen(false);
      setWifiOpen(false);
    }
  }, []);

  // Nhan 1 ban tin MQTT (snapshot hoac live)
  const handleMqttMessage = useCallback(
    (topic: string, payload: string, isLive: boolean, retain = false) => {
      const sc = screensaverConfigRef.current;
      const customScGas =
        sc.gasTopic && sc.gasTopic.trim() && sc.gasTopic.trim() !== 'smarthome/sensor/gas'
          ? sc.gasTopic.trim()
          : '';
      const customScPir =
        sc.pirTopic && sc.pirTopic.trim() && sc.pirTopic.trim() !== 'smarthome/sensor/pir'
          ? sc.pirTopic.trim()
          : '';
      const gasTopic =
        customScGas || configRef.current.telegram?.gasTopic || sc.gasTopic || 'smarthome/sensor/gas';
      const pirTopic =
        customScPir || configRef.current.telegram?.pirTopic || sc.pirTopic || 'smarthome/sensor/pir';
      const gasThreshold = configRef.current.telegram?.gasThresholdPpm || 200;
      const lowerTopic = topic.toLowerCase();

      if (
        topic === gasTopic ||
        topic === 'smarthome/sensor/gas' ||
        (lowerTopic.startsWith('smarthome/sensor/') && lowerTopic.includes('gas'))
      ) {
        const res = parseGasSensorPayload(payload, gasThreshold);
        setMqttGasAlert(res.alert);
        setMqttGasPpm(res.ppm);
        return;
      }

      if (
        topic === pirTopic ||
        topic === 'smarthome/sensor/pir' ||
        (lowerTopic.startsWith('smarthome/sensor/') && lowerTopic.includes('pir'))
      ) {
        if (isLive && !retain && sc.pirWakeEnabled && parsePirMotionPayload(payload)) {
          danhThucManChoTuSuKien();
        }
        return;
      }

      if (topic.startsWith('camera/')) {
        if (topic === CameraTopic.FACE_GREET) {
          if (isLive && !retain) {
            const ev = parseFaceGreetEvent(payload);
            if (ev && isFaceGreetFresh(ev)) {
              danhThucManChoTuSuKien();
              setChaoMatHienTai(ev);
              if (chaoMatTimerRef.current !== null) {
                window.clearTimeout(chaoMatTimerRef.current);
              }
              chaoMatTimerRef.current = window.setTimeout(() => {
                setChaoMatHienTai(null);
                chaoMatTimerRef.current = null;
              }, ev.isKnown ? 5000 : 10000);
              if (!ev.silent && ev.greeting) {
                void phatChaoGiongNoi(ev.greeting, {
                  apiPort: cameraRecorderRef.current.apiPort,
                  screensaverConfig: sc,
                });
              }
            }
          }
          return;
        }

        const next = gopCameraRecorder(cameraRecorderRef.current, topic, payload);
        cameraRecorderRef.current = next;
        setCameraRecorder(next);

        if (isLive && !retain && topic === CameraTopic.PERSON && sc.pirWakeEnabled) {
          danhThucManChoTuSuKien();
        }
        if (isLive && !retain && topic === CameraTopic.KET_QUA && next.ketQuaLenh?.message) {
          showToast(next.ketQuaLenh.message, next.ketQuaLenh.ok ? 'success' : 'warning');
        }
        return;
      }

      if (topic.startsWith('dien/')) {
        setDien((cur) => gopDien(cur, topic, payload));
        return;
      }
      if (topic.startsWith('wifi/')) {
        setWifi((cur) => gopWifi(cur, topic, payload));
        return;
      }
      if (topic.startsWith('server/')) {
        setServer((cur) => gopServer(cur, topic, payload));
        return;
      }
      if (topic.startsWith('cuktech/')) {
        const next = gopCuktech(cuktechRef.current, topic, payload, Date.now());
        cuktechRef.current = next;
        setCuktech(next);

        if (isLive) {
          if (topic === CuktechTopic.STATUS || topic === CuktechTopic.SETTINGS) {
            syncCuktechPendingStates(next);
          }
          if (topic === CuktechTopic.STATUS && cuktechNoiLaiDeadline.current > 0 && next.noiDuoc) {
            cuktechNoiLaiDeadline.current = 0;
            setDangNoiLaiCuktech(false);
          }
        }
        return;
      }
      if (topic.startsWith('congtac/')) {
        const next = gopCongTac(congtacRef.current, topic, payload);
        congtacRef.current = next;
        setCongtac(next);
        syncCongTacVaoRooms(next);

        if (isLive && topic.startsWith('congtac/thietbi/')) {
          const ma = topic.slice('congtac/thietbi/'.length);
          const tt = next.trangThai[ma];
          if (tt && hangChoRef.current.nhanTrangThai(ma, tt.traLoiId)) {
            setChoCongTacIds(hangChoRef.current.anhChup());
            if (tt.lyDoLoi) {
              const ten = next.danhSach.find((tb) => tb.ma === ma)?.ten ?? ma;
              showToast(`${ten}: ${tt.lyDoLoi}`, 'warning');
            }
          }
        }
      }
    },
    [danhThucManChoTuSuKien, showToast, syncCongTacVaoRooms, syncCuktechPendingStates],
  );

  // Camera probe
  const taiDanhSachCamera = useCallback(async () => {
    if (!configRef.current.camera.host.trim()) {
      setCameraStatus({ state: 'chua_cau_hinh' });
      return;
    }
    setCameraStatus({ state: 'dang_do' });
    try {
      const resp = await fetch('/api/go2rtc/streams');
      const data = (await resp.json()) as { ok: boolean; streams?: string[]; error?: string };
      if (data.ok) {
        setCameraStatus({ state: 'co', ds: data.streams ?? [] });
      } else if (data.error === 'Chưa khai báo go2rtc') {
        setCameraStatus({ state: 'chua_cau_hinh' });
      } else {
        setCameraStatus({ state: 'loi', moTa: data.error || 'Không kết nối được go2rtc' });
      }
    } catch (err) {
      setCameraStatus({
        state: 'loi',
        moTa: err instanceof Error ? err.message : 'Lỗi kết nối gateway',
      });
    }
  }, []);

  // Weather fetch
  const taiThoiTiet = useCallback(async () => {
    const cfg = configRef.current.weather;
    const res = await layThoiTiet(cfg.lat, cfg.lon, cfg.ten);
    if (res.ok) {
      setThoiTiet({ state: 'co', data: res.data });
    } else {
      setThoiTiet((prev) => (prev.state === 'co' ? prev : { state: 'loi', moTa: res.error }));
    }
  }, []);

  // Initial load: config + weather (every 15m) + camera probe
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const resp = await fetch('/api/config');
        if (resp.ok) {
          const data = (await resp.json()) as PublicAppConfig;
          if (!cancelled) {
            const merged: PublicAppConfig = {
              ...defaultPublicConfig(),
              ...data,
              camera: {
                ...defaultPublicConfig().camera,
                ...data.camera,
                congGhiHinh: data.camera?.congGhiHinh || 1985,
              },
            };
            configRef.current = merged;
            setConfig(merged);
            setConfigLoaded(true);
            setGatewayOnline(true);
          }
        }
      } catch {
        if (!cancelled) setGatewayOnline(false);
      }
      if (!cancelled) {
        void taiThoiTiet();
        void taiDanhSachCamera();
        void fetch('/api/alerts/status')
          .then((r) => (r.ok ? r.json() : null))
          .then((st: TelegramAlertsStatus | null) => {
            if (!cancelled && st) setTelegramStatus(st);
          })
          .catch(() => {});
      }
    })();

    const weatherTimer = window.setInterval(() => {
      void taiThoiTiet();
    }, 15 * 60_000);

    return () => {
      cancelled = true;
      window.clearInterval(weatherTimer);
    };
  }, [taiThoiTiet, taiDanhSachCamera]);

  // WebSocket /ws/mqtt bridge connection with backoff
  useEffect(() => {
    let closedByUnmount = false;
    let reconnectTimer: number | null = null;
    let delayMs = 1500;

    const connectWs = () => {
      if (closedByUnmount) return;
      const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const ws = new WebSocket(`${proto}//${window.location.host}/ws/mqtt`);
      wsRef.current = ws;

      ws.onopen = () => {
        delayMs = 1500;
        setGatewayOnline(true);
      };

      ws.onmessage = (ev) => {
        try {
          const msg = JSON.parse(String(ev.data)) as
            | { type: 'snapshot'; status: KetNoi; messages: Array<[string, string]> }
            | { type: 'status'; status: KetNoi }
            | { type: 'message'; topic: string; payload: string; ts: number; retain?: boolean }
            | { type: 'publish_ack'; reqId: string; ok: boolean; reason?: string; error?: string };

          if (msg.type === 'snapshot') {
            setMqttStatus(msg.status);
            for (const [topic, payload] of msg.messages) {
              handleMqttMessage(topic, payload, false, true);
            }
          } else if (msg.type === 'status') {
            setMqttStatus(msg.status);
          } else if (msg.type === 'message') {
            handleMqttMessage(msg.topic, msg.payload, true, Boolean(msg.retain));
          } else if (msg.type === 'publish_ack') {
            const resolver = pendingPublishResolvers.current.get(msg.reqId);
            if (resolver) {
              pendingPublishResolvers.current.delete(msg.reqId);
              resolver({ ok: msg.ok, reason: msg.reason, error: msg.error });
            }
          }
        } catch {
          // ignore malformed WS frame
        }
      };

      ws.onerror = () => {
        setGatewayOnline(false);
      };

      ws.onclose = () => {
        if (wsRef.current === ws) wsRef.current = null;
        if (!closedByUnmount) {
          setGatewayOnline(false);
          reconnectTimer = window.setTimeout(connectWs, delayMs);
          delayMs = Math.min(delayMs * 1.5, 10_000);
        }
      };
    };

    connectWs();

    return () => {
      closedByUnmount = true;
      if (reconnectTimer !== null) window.clearTimeout(reconnectTimer);
      wsRef.current?.close();
    };
  }, [handleMqttMessage]);

  // WebSocket /ws/bomnuoc bridge connection with adaptive viewing notification
  useEffect(() => {
    let closedByUnmount = false;
    let reconnectTimer: number | null = null;
    let delayMs = 1500;

    const connectBomWs = () => {
      if (closedByUnmount) return;
      const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const ws = new WebSocket(`${proto}//${window.location.host}/ws/bomnuoc`);
      bomWsRef.current = ws;

      ws.onopen = () => {
        delayMs = 1500;
        const active = currentDestRef.current === 'automation' || currentDestRef.current === 'stats';
        ws.send(JSON.stringify({ type: 'viewing', active }));
      };

      ws.onmessage = (ev) => {
        try {
          const msg = JSON.parse(String(ev.data)) as
            | {
                type: 'snapshot';
                state: BomNuocState | null;
                usage: LuongNuocThang[];
                link: TrangThaiKetNoiBom;
                kenh: KenhBomNuoc;
                pending: string[];
                log: SuKienBom[];
              }
            | { type: 'state'; state: BomNuocState | null }
            | { type: 'usage'; usage: LuongNuocThang[] }
            | { type: 'link'; link: TrangThaiKetNoiBom; kenh: KenhBomNuoc }
            | { type: 'pending'; pending: string[] }
            | { type: 'log'; log: SuKienBom[] };

          if (msg.type === 'snapshot') {
            setBomNuoc(msg.state);
            setLuongNuocThang(msg.usage ?? []);
            setKetNoiBom(msg.link);
            setKenhBom(msg.kenh);
            setWsPendingBom(Object.fromEntries((msg.pending ?? []).map((k) => [k, true])));
            setNhatKyBom(msg.log ?? []);
          } else if (msg.type === 'state') {
            setBomNuoc(msg.state);
          } else if (msg.type === 'usage') {
            setLuongNuocThang(msg.usage ?? []);
          } else if (msg.type === 'link') {
            setKetNoiBom(msg.link);
            setKenhBom(msg.kenh);
          } else if (msg.type === 'pending') {
            setWsPendingBom(Object.fromEntries((msg.pending ?? []).map((k) => [k, true])));
          } else if (msg.type === 'log') {
            setNhatKyBom(msg.log ?? []);
          }
        } catch {
          // ignore malformed WS frame
        }
      };

      ws.onclose = () => {
        if (bomWsRef.current === ws) bomWsRef.current = null;
        if (!closedByUnmount) {
          reconnectTimer = window.setTimeout(connectBomWs, delayMs);
          delayMs = Math.min(delayMs * 1.5, 10_000);
        }
      };
    };

    connectBomWs();

    return () => {
      closedByUnmount = true;
      if (reconnectTimer !== null) window.clearTimeout(reconnectTimer);
      bomWsRef.current?.close();
    };
  }, []);

  useEffect(() => {
    const ws = bomWsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      const active = currentDest === 'automation' || currentDest === 'stats';
      ws.send(JSON.stringify({ type: 'viewing', active }));
    }
  }, [currentDest]);

  // Periodic sweep (1s) for switch timeouts, Cuktech deadlines, clock sync
  useEffect(() => {
    const id = window.setInterval(() => {
      const now = Date.now();
      setNowMs(now);

      const expired = hangChoRef.current.quaHan(now);
      if (expired.length > 0) {
        setChoCongTacIds(hangChoRef.current.anhChup());
        for (const ma of expired) {
          const ten = congtacRef.current.danhSach.find((tb) => tb.ma === ma)?.ten ?? ma;
          showToast(`${ten} không phản hồi sau 6 giây. Không rõ lệnh đã chạy chưa.`, 'warning');
        }
      }

      let congChanged = false;
      for (const key of Object.keys(cuktechCongExpected.current) as CongSacId[]) {
        const exp = cuktechCongExpected.current[key];
        if (exp && now >= exp.deadline) {
          delete cuktechCongExpected.current[key];
          congChanged = true;
          showToast(`Cổng ${key === 'USB_A' ? 'USB-A' : key} không phản hồi sau ${GIAY_CHO_CUKTECH}s.`, 'warning');
        }
      }
      if (congChanged) {
        const snap: Partial<Record<CongSacId, boolean>> = {};
        for (const [k, v] of Object.entries(cuktechCongExpected.current) as Array<
          [CongSacId, { bat: boolean; deadline: number }]
        >) {
          if (v) snap[k] = v.bat;
        }
        setChoCongSac(snap);
      }

      let cdChanged = false;
      for (const rawKey of Object.keys(cuktechCaiDatExpected.current)) {
        const piid = Number(rawKey);
        const exp = cuktechCaiDatExpected.current[piid];
        if (exp && now >= exp.deadline) {
          delete cuktechCaiDatExpected.current[piid];
          cdChanged = true;
          showToast('Củ sạc không phản hồi lệnh đổi cài đặt.', 'warning');
        }
      }
      if (cdChanged) {
        const snap: Record<number, boolean> = {};
        for (const rawKey of Object.keys(cuktechCaiDatExpected.current)) {
          snap[Number(rawKey)] = true;
        }
        setChoCaiDatCuktech(snap);
      }

      if (cuktechNoiLaiDeadline.current > 0 && now >= cuktechNoiLaiDeadline.current) {
        cuktechNoiLaiDeadline.current = 0;
        setDangNoiLaiCuktech(false);
        showToast('Không thấy phản hồi nối lại BLE từ máy chủ.', 'warning');
      }
    }, 1000);

    return () => window.clearInterval(id);
  }, [showToast]);

  // Room updaters
  const updateRoom = useCallback((roomId: string, updater: (r: RoomUi) => RoomUi) => {
    setRooms((prev) => prev.map((r) => (r.id === roomId ? updater(r) : r)));
  }, []);

  // Cua duy nhat gui lenh toi cong tac that (hoac doi cuc bo neu la thiet bi mau / mat ket noi khi bam tu man hinh)
  const gatCongTac = useCallback(
    (phong: string, thietBi: string, dao: boolean, bat: boolean, nguon: string): string | null => {
      const tuManHinh = nguon === LenhCongTac.NGUON_MAN_HINH;
      const capNhatCucBo = () => {
        updateRoom(phong, (r) => {
          const hienTai = thietBi === 'den' ? r.lightOn : r.fanOn;
          const moi = dao ? !hienTai : bat;
          const nextMode = tuManHinh && phong !== 'hall' ? 'CUSTOM' : r.mode;
          if (thietBi === 'den') {
            return {
              ...r,
              lightOn: moi,
              lightBrightness: moi && r.lightBrightness <= 25 ? 100 : r.lightBrightness,
              mode: nextMode,
            };
          }
          return { ...r, fanOn: moi, mode: nextMode };
        });
      };

      const ct = congtacRef.current;
      const tb = timCongTac(ct, phong, thietBi);
      if (!tb) {
        capNhatCucBo();
        return null;
      }

      if (mqttStatusRef.current.state !== 'da_noi') {
        const loi = 'Chưa nối broker MQTT nên chưa gửi được lệnh. Kiểm tra Cài đặt.';
        if (tuManHinh) {
          capNhatCucBo();
          showToast(loi, 'error');
        }
        return loi;
      }
      if (!isBridgeSong(ct)) {
        const loi = 'Bộ nối công tắc trên máy chủ chưa chạy nên chưa gửi được lệnh.';
        if (tuManHinh) {
          capNhatCucBo();
          showToast(loi, 'warning');
        }
        return loi;
      }

      const tt = ct.trangThai[tb.ma];
      if (!tt || !tt.online) {
        const loi = `${tb.ten} đang mất kết nối${tt?.lyDoLoi ? ` (${tt.lyDoLoi})` : ''}.`;
        if (tuManHinh) {
          capNhatCucBo();
          showToast(loi, 'warning');
        }
        return loi;
      }
      if (dao && tt.bat === null) {
        const loi = `Chưa biết ${tb.ten} đang bật hay tắt nên không đảo được.`;
        if (tuManHinh) {
          capNhatCucBo();
          showToast(loi, 'warning');
        }
        return loi;
      }
      if (hangChoRef.current.dangCho(tb.ma)) {
        return null;
      }

      const mong = dao ? !tt.bat! : bat;
      const hanhDong = mong ? 'ON' : 'OFF';
      const id = LenhCongTac.maLenh();
      hangChoRef.current.ganh(tb.ma, mong, id, Date.now());
      setChoCongTacIds(hangChoRef.current.anhChup());

      if (tuManHinh) {
        updateRoom(phong, (r) => {
          const nextMode = phong === 'hall' ? r.mode : 'CUSTOM';
          if (thietBi === 'den') {
            return {
              ...r,
              lightBrightness: mong && r.lightBrightness <= 25 ? 100 : r.lightBrightness,
              mode: nextMode,
            };
          }
          return { ...r, mode: nextMode };
        });
      }

      const payload = LenhCongTac.json(phong, thietBi, hanhDong, null, nguon, id);
      void publishMqtt('smarthome/control', payload).then((ack) => {
        if (!ack.ok) {
          hangChoRef.current.go(tb.ma);
          setChoCongTacIds(hangChoRef.current.anhChup());
          if (tuManHinh) {
            showToast(`Không gửi được lệnh: ${ack.error || ack.reason || 'lỗi'}`, 'error');
          }
        }
      });
      return null;
    },
    [publishMqtt, showToast, updateRoom],
  );

  const toggleQuickItem = useCallback((id: string) => {
    setQuickItems((prev) => prev.map((q) => (q.id === id ? { ...q, on: !q.on } : q)));
  }, []);

  const toggleRoomLight = useCallback(
    (id: string) => {
      gatCongTac(id, 'den', true, false, LenhCongTac.NGUON_MAN_HINH);
    },
    [gatCongTac],
  );

  const toggleRoomFan = useCallback(
    (id: string) => {
      gatCongTac(id, 'quat', true, false, LenhCongTac.NGUON_MAN_HINH);
    },
    [gatCongTac],
  );

  const toggleRoomAc = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, acOn: !r.acOn }));
    },
    [updateRoom],
  );

  const stepAcTemp = useCallback(
    (id: string, delta: number) => {
      updateRoom(id, (r) => ({
        ...r,
        acOn: true,
        acTemp: Math.min(30, Math.max(16, r.acTemp + delta)),
      }));
    },
    [updateRoom],
  );

  // Kich ban che do theo tung loai phong khop 100% HomeViewModel.setMode (Android)
  const setRoomMode = useCallback(
    (id: string, mode: RoomMode) => {
      const ct = congtacRef.current;
      const denThatOnline = Boolean(
        timCongTac(ct, id, 'den') &&
          isBridgeSong(ct) &&
          trangThaiCongTacCua(ct, id, 'den')?.online === true,
      );
      const quatThatOnline = Boolean(
        timCongTac(ct, id, 'quat') &&
          isBridgeSong(ct) &&
          trangThaiCongTacCua(ct, id, 'quat')?.online === true,
      );

      updateRoom(id, (r) => {
        if (id === 'living') {
          switch (mode) {
            case 'HOME':
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 100,
                acOn: true,
                acTemp: 24,
                curtainOpen: 100,
                tvOn: false,
              };
            case 'SLEEP':
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 20,
                acOn: true,
                acTemp: 23,
                curtainOpen: 0,
                tvOn: true,
              };
            case 'CUSTOM':
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 55,
                acOn: true,
                acTemp: 25,
                curtainOpen: 50,
                tvOn: false,
              };
            case 'AWAY':
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : false,
                fanOn: quatThatOnline ? r.fanOn : false,
                acOn: false,
                tvOn: false,
                curtainOpen: 0,
              };
          }
        }

        if (id === 'kitchen') {
          switch (mode) {
            case 'HOME': // Nấu ăn
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 100,
                fanOn: quatThatOnline ? r.fanOn : true,
                hoodSpeed: 2,
                stoveOn: true,
                stoveLevel: 5,
              };
            case 'CUSTOM': // Ăn tối
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 55,
                fanOn: quatThatOnline ? r.fanOn : false,
                stoveOn: false,
              };
            case 'SLEEP': // Dọn dẹp
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 100,
                fanOn: quatThatOnline ? r.fanOn : true,
                hoodSpeed: 1,
                stoveOn: false,
              };
            case 'AWAY': // Vắng nhà
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : false,
                fanOn: quatThatOnline ? r.fanOn : false,
                stoveOn: false,
              };
          }
        }

        if (id === 'hall') {
          switch (mode) {
            case 'SLEEP': // Đêm an ninh
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : false,
                hallFrontLightOn: true,
                hallFrontBrightness: 60,
                hallSurroundingLightOn: true,
                hallSurroundingBrightness: 75,
                hallDoorLocked: true,
                hallCameraOn: true,
                hallIrMode: true,
                hallMotionAuto: true,
              };
            case 'HOME': // Về nhà
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 100,
                hallFrontLightOn: true,
                hallFrontBrightness: 100,
                hallSurroundingLightOn: true,
                hallSurroundingBrightness: 80,
                hallDoorLocked: false,
                hallIrMode: false,
              };
            case 'CUSTOM': // Tiếp khách
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : true,
                lightBrightness: 100,
                hallFrontLightOn: true,
                hallFrontBrightness: 100,
                hallSurroundingLightOn: true,
                hallSurroundingBrightness: 100,
                hallDoorLocked: false,
                hallIrMode: false,
              };
            case 'AWAY': // Đi vắng
              return {
                ...r,
                mode,
                lightOn: denThatOnline ? r.lightOn : false,
                hallFrontLightOn: false,
                hallSurroundingLightOn: false,
                hallDoorLocked: true,
                hallCameraOn: true,
                hallIrMode: true,
                hallMotionAuto: true,
              };
          }
        }

        // Phòng ngủ (bed1, bed2, bed3)
        switch (mode) {
          case 'SLEEP':
            return {
              ...r,
              mode,
              lightOn: denThatOnline ? r.lightOn : true,
              lightBrightness: 20,
              fanOn: quatThatOnline ? r.fanOn : true,
              fanSpeed: 1,
              acOn: true,
              acTemp: 26,
            };
          case 'HOME':
            return {
              ...r,
              mode,
              lightOn: denThatOnline ? r.lightOn : true,
              lightBrightness: 100,
              fanOn: quatThatOnline ? r.fanOn : false,
              acOn: true,
              acTemp: 25,
            };
          case 'AWAY':
            return {
              ...r,
              mode,
              lightOn: denThatOnline ? r.lightOn : false,
              fanOn: quatThatOnline ? r.fanOn : false,
              acOn: false,
            };
          case 'CUSTOM':
            return { ...r, mode };
        }
      });
    },
    [updateRoom],
  );

  const toggleRoomSchedule = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, scheduleOn: !r.scheduleOn }));
    },
    [updateRoom],
  );

  const setLightBrightness = useCallback(
    (id: string, pct: number) => {
      const clamped = Math.min(100, Math.max(10, Math.round(pct)));
      const ct = congtacRef.current;
      const tb = timCongTac(ct, id, 'den');
      const tt = tb ? ct.trangThai[tb.ma] : undefined;
      updateRoom(id, (r) => ({
        ...r,
        lightOn: tb && isBridgeSong(ct) && tt?.online ? r.lightOn : true,
        lightBrightness: clamped,
        mode: id === 'hall' ? r.mode : 'CUSTOM',
      }));
      if (tb && isBridgeSong(ct) && tt?.online === true && tt.bat !== true) {
        gatCongTac(id, 'den', false, true, LenhCongTac.NGUON_MAN_HINH);
      }
    },
    [gatCongTac, updateRoom],
  );

  const setFanSpeed = useCallback(
    (id: string, speed: number) => {
      updateRoom(id, (r) => ({
        ...r,
        fanSpeed: Math.min(3, Math.max(1, Math.trunc(speed))),
        fanOn: true,
        mode: id === 'hall' ? r.mode : 'CUSTOM',
      }));
      const ct = congtacRef.current;
      const tb = timCongTac(ct, id, 'quat');
      const tt = tb ? ct.trangThai[tb.ma] : undefined;
      if (tb && isBridgeSong(ct) && tt?.online === true && tt.bat !== true) {
        gatCongTac(id, 'quat', false, true, LenhCongTac.NGUON_MAN_HINH);
      }
    },
    [gatCongTac, updateRoom],
  );

  const toggleRoomTv = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, tvOn: !r.tvOn }));
    },
    [updateRoom],
  );

  const setCurtainOpen = useCallback(
    (id: string, pct: number) => {
      updateRoom(id, (r) => ({ ...r, curtainOpen: Math.min(100, Math.max(0, Math.round(pct))) }));
    },
    [updateRoom],
  );

  const toggleCurtain = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, curtainOpen: r.curtainOpen > 0 ? 0 : 100 }));
    },
    [updateRoom],
  );

  const setHoodSpeed = useCallback(
    (id: string, speed: number) => {
      updateRoom(id, (r) => ({
        ...r,
        hoodSpeed: Math.min(3, Math.max(1, Math.trunc(speed))),
        fanOn: true,
      }));
    },
    [updateRoom],
  );

  const toggleStove = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, stoveOn: !r.stoveOn }));
    },
    [updateRoom],
  );

  const stepStoveLevel = useCallback(
    (id: string, delta: number) => {
      updateRoom(id, (r) => ({
        ...r,
        stoveOn: true,
        stoveLevel: Math.min(9, Math.max(1, r.stoveLevel + delta)),
      }));
    },
    [updateRoom],
  );

  const setGasAlert = useCallback(
    (id: string, alert: boolean) => {
      updateRoom(id, (r) => ({
        ...r,
        gasAlert: alert,
        gasPpm: alert ? 485 : 18,
        fanOn: alert ? true : r.fanOn,
        hoodSpeed: alert ? 3 : r.hoodSpeed,
      }));
    },
    [updateRoom],
  );

  const triggerTestGasAlert = useCallback(
    (id: string) => {
      setGasAlert(id, true);
    },
    [setGasAlert],
  );

  const dismissGasAlert = useCallback(
    (id: string) => {
      setGasAlert(id, false);
    },
    [setGasAlert],
  );

  const setHallView = useCallback(
    (id: string, view: HallwayView) => {
      updateRoom(id, (r) => ({ ...r, hallView: view }));
    },
    [updateRoom],
  );

  const toggleHallFrontLight = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, hallFrontLightOn: !r.hallFrontLightOn }));
    },
    [updateRoom],
  );

  const setHallFrontBrightness = useCallback(
    (id: string, pct: number) => {
      updateRoom(id, (r) => ({
        ...r,
        hallFrontLightOn: true,
        hallFrontBrightness: Math.min(100, Math.max(10, Math.round(pct))),
      }));
    },
    [updateRoom],
  );

  const toggleHallSurroundingLight = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, hallSurroundingLightOn: !r.hallSurroundingLightOn }));
    },
    [updateRoom],
  );

  const setHallSurroundingBrightness = useCallback(
    (id: string, pct: number) => {
      updateRoom(id, (r) => ({
        ...r,
        hallSurroundingLightOn: true,
        hallSurroundingBrightness: Math.min(100, Math.max(10, Math.round(pct))),
      }));
    },
    [updateRoom],
  );

  const toggleHallAllLights = useCallback(
    (id: string) => {
      const room = roomsRef.current.find((r) => r.id === id);
      if (!room) return;
      const anyOn = room.lightOn || room.hallFrontLightOn || room.hallSurroundingLightOn;
      const target = !anyOn;
      gatCongTac(id, 'den', false, target, LenhCongTac.NGUON_MAN_HINH);
      updateRoom(id, (r) => ({
        ...r,
        hallFrontLightOn: target,
        hallSurroundingLightOn: target,
      }));
    },
    [gatCongTac, updateRoom],
  );

  const toggleHallDoorLock = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, hallDoorLocked: !r.hallDoorLocked }));
    },
    [updateRoom],
  );

  const toggleHallCamera = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, hallCameraOn: !r.hallCameraOn }));
    },
    [updateRoom],
  );

  const toggleHallIrMode = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, hallCameraOn: true, hallIrMode: !r.hallIrMode }));
    },
    [updateRoom],
  );

  const toggleHallMotionAuto = useCallback(
    (id: string) => {
      updateRoom(id, (r) => ({ ...r, hallMotionAuto: !r.hallMotionAuto }));
    },
    [updateRoom],
  );

  const clearHallMotion = useCallback(
    (id: string) => {
      if (hallMotionTimersRef.current[id]) {
        window.clearTimeout(hallMotionTimersRef.current[id]);
        delete hallMotionTimersRef.current[id];
      }
      updateRoom(id, (r) => ({ ...r, hallMotionDetected: false }));
    },
    [updateRoom],
  );

  const triggerHallMotionTest = useCallback(
    (id: string) => {
      const cur = roomsRef.current.find((r) => r.id === id);
      if (!cur) return;
      if (cur.hallMotionAuto) {
        gatCongTac(id, 'den', false, true, LenhCongTac.NGUON_MAN_HINH);
      }
      updateRoom(id, (r) => ({
        ...r,
        hallMotionDetected: true,
        lightOn: r.hallMotionAuto ? true : r.lightOn,
        lightBrightness: r.hallMotionAuto ? Math.max(85, r.lightBrightness) : r.lightBrightness,
        hallSurroundingLightOn: r.hallMotionAuto ? true : r.hallSurroundingLightOn,
        hallSurroundingBrightness: r.hallMotionAuto
          ? Math.max(85, r.hallSurroundingBrightness)
          : r.hallSurroundingBrightness,
      }));
      if (hallMotionTimersRef.current[id]) {
        window.clearTimeout(hallMotionTimersRef.current[id]);
      }
      hallMotionTimersRef.current[id] = window.setTimeout(() => {
        delete hallMotionTimersRef.current[id];
        updateRoom(id, (r) => ({ ...r, hallMotionDetected: false }));
      }, 6500);
    },
    [gatCongTac, updateRoom],
  );

  const thaoTacNhanhPhong = useCallback(
    (roomId: string, actionId: string) => {
      switch (actionId) {
        case 'light':
          toggleRoomLight(roomId);
          break;
        case 'fan':
        case 'hood':
          toggleRoomFan(roomId);
          break;
        case 'ac':
          toggleRoomAc(roomId);
          break;
        case 'curtain':
          toggleCurtain(roomId);
          break;
        case 'sleep': {
          const cur = roomsRef.current.find((r) => r.id === roomId)?.mode;
          setRoomMode(roomId, cur === 'SLEEP' ? 'HOME' : 'SLEEP');
          break;
        }
        case 'stove':
          toggleStove(roomId);
          break;
        case 'schedule':
          toggleRoomSchedule(roomId);
          break;
        case 'hall_front':
          toggleHallFrontLight(roomId);
          break;
        case 'hall_lock':
          toggleHallDoorLock(roomId);
          break;
        case 'hall_camera':
          toggleHallCamera(roomId);
          break;
      }
    },
    [
      setRoomMode,
      toggleCurtain,
      toggleHallCamera,
      toggleHallDoorLock,
      toggleHallFrontLight,
      toggleRoomAc,
      toggleRoomFan,
      toggleRoomLight,
      toggleRoomSchedule,
      toggleStove,
    ],
  );

  // Real Hardware Switch helpers
  const oThietBi = useCallback(
    (phong: string, thietBi: string, batMau: boolean): OThietBi => {
      const tb = timCongTac(congtac, phong, thietBi);
      if (!tb) return { kind: 'mau', bat: batMau };
      const tt = congtac.trangThai[tb.ma];
      const cho = hangChoRef.current.cua(tb.ma);
      return buildOThietBiThat({
        ten: tb.ten,
        bridgeSong: isBridgeSong(congtac),
        online: tt?.online ?? false,
        bat: tt?.bat ?? null,
        dangCho: Boolean(cho) || Boolean(choCongTacIds[tb.ma]),
        mong: cho ? cho.mong : null,
        lyDoLoi: tt?.lyDoLoi ?? null,
      });
    },
    [congtac, choCongTacIds],
  );

  const bamCongTac = useCallback(
    (phong: string, thietBi: string, guiTuGiong = false) => {
      gatCongTac(
        phong,
        thietBi,
        true,
        false,
        guiTuGiong ? LenhCongTac.NGUON_GIONG_NOI : LenhCongTac.NGUON_MAN_HINH,
      );
    },
    [gatCongTac],
  );

  // Camera 24/24 actions (v0.9.4)
  const chupAnhCamera = useCallback(
    async (tenLuong: string) => {
      if (dangChupAnh) return;
      setDangChupAnh(true);
      try {
        const res = await CameraPlaybackClient.chupAnhNgay(
          tenLuong,
          cameraRecorderRef.current.apiPort,
        );
        showToast(res.message, res.ok ? 'success' : 'warning');
      } finally {
        setDangChupAnh(false);
      }
    },
    [dangChupAnh, showToast],
  );

  const datCanhNguoiCamera = useCallback(
    (bat: boolean) => {
      if (mqttStatusRef.current.state !== 'da_noi') {
        showToast('Chưa nối broker MQTT nên chưa đổi được cài đặt canh người.', 'warning');
        return;
      }
      const payload = JSON.stringify({
        action: 'SET_DETECT_PERSON',
        id: LenhCongTac.maLenh(),
        value: bat,
      });
      void publishMqtt(CameraTopic.CMD, payload).then((ack) => {
        if (!ack.ok) {
          showToast(`Không gửi được lệnh camera: ${ack.error || ack.reason || 'lỗi'}`, 'error');
        }
      });
    },
    [publishMqtt, showToast],
  );

  const anPopupChaoMat = useCallback(() => {
    if (chaoMatTimerRef.current !== null) {
      window.clearTimeout(chaoMatTimerRef.current);
      chaoMatTimerRef.current = null;
    }
    setChaoMatHienTai(null);
  }, []);

  const docThuChaoMat = useCallback(
    (ten: string) => {
      const clean = ten.trim() || 'Trung';
      const cau = `Chào mừng ${clean} về nhà!`;
      showToast(`Đang đọc thử: "${cau}"`, 'info');
      void phatChaoGiongNoi(cau, {
        apiPort: cameraRecorderRef.current.apiPort,
        screensaverConfig: screensaverConfigRef.current,
        forceTest: true,
      }).then((r) => {
        if (!r.ok) {
          showToast('Trình duyệt chặn tự phát âm thanh hoặc chưa nối được dịch vụ TTS.', 'warning');
        }
      });
    },
    [showToast],
  );

  const dieuKhienPtz = useCallback(
    async (cam: string, direction: string, speed = 0.5) => {
      const res = await CameraPlaybackClient.dieuKhienPtzMove(
        cam,
        direction,
        speed,
        cameraRecorderRef.current.apiPort,
      );
      if (!res.ok) {
        // Fallback MQTT camera/cmd
        const payload = JSON.stringify({
          id: `web-ptz-${Date.now().toString(36)}`,
          action: 'PTZ_MOVE',
          camera: cam,
          direction,
          speed,
        });
        const ack = await publishMqtt(CameraTopic.CMD, payload);
        if (!ack.ok) {
          showToast(res.message, 'warning');
        }
      }
    },
    [publishMqtt, showToast],
  );

  const goiPtzPreset = useCallback(
    async (cam: string, preset: string) => {
      const res = await CameraPlaybackClient.goiPtzPreset(
        cam,
        preset,
        cameraRecorderRef.current.apiPort,
      );
      if (res.ok) {
        showToast(res.message, 'success');
      } else {
        const payload = JSON.stringify({
          id: `web-preset-${Date.now().toString(36)}`,
          action: 'PTZ_PRESET',
          camera: cam,
          preset,
        });
        const ack = await publishMqtt(CameraTopic.CMD, payload);
        showToast(ack.ok ? 'Đã gửi lệnh xoay tới góc lưu sẵn.' : res.message, ack.ok ? 'info' : 'warning');
      }
    },
    [publishMqtt, showToast],
  );

  const datCheDoRiengTuCamera = useCallback(
    async (bat: boolean) => {
      const res = await CameraPlaybackClient.datCheDoRiengTu(
        bat,
        cameraRecorderRef.current.apiPort,
      );
      showToast(res.message, res.ok ? 'success' : 'warning');
    },
    [showToast],
  );

  const requireConfirm = useCallback(
    (opts: ConfirmPromptState, fn: () => void | Promise<void>) => {
      pendingConfirmActionRef.current = fn;
      setConfirmPrompt(opts);
    },
    [],
  );

  const acceptConfirm = useCallback(() => {
    const fn = pendingConfirmActionRef.current;
    pendingConfirmActionRef.current = null;
    setConfirmPrompt(null);
    if (fn) void fn();
  }, []);

  const cancelConfirm = useCallback(() => {
    pendingConfirmActionRef.current = null;
    setConfirmPrompt(null);
  }, []);

  // Cuktech BLE controls
  const datCongSac = useCallback(
    (id: CongSacId, bat: boolean) => {
      if (mqttStatusRef.current.state !== 'da_noi') {
        showToast('Chưa nối được với máy chủ MQTT.', 'error');
        return;
      }
      const meta =
        id === 'C1'
          ? { topic: 'c1', nhan: 'C1' }
          : id === 'C2'
            ? { topic: 'c2', nhan: 'C2' }
            : id === 'C3'
              ? { topic: 'c3', nhan: 'C3' }
              : { topic: 'a', nhan: 'USB-A' };

      cuktechCongExpected.current[id] = { bat, deadline: Date.now() + GIAY_CHO_CUKTECH * 1000 };
      setChoCongSac((prev) => ({ ...prev, [id]: bat }));

      void publishMqtt(
        CuktechTopic.CMD_CONG,
        JSON.stringify({ port: meta.topic, action: bat ? 'on' : 'off' }),
      ).then((ack) => {
        if (!ack.ok) {
          delete cuktechCongExpected.current[id];
          setChoCongSac((prev) => {
            const next = { ...prev };
            delete next[id];
            return next;
          });
          showToast(`Không gửi được lệnh ${meta.nhan}: ${ack.error || ack.reason || 'lỗi'}`, 'error');
        }
      });
    },
    [publishMqtt, showToast],
  );

  const datCaiDatCuktech = useCallback(
    (piid: number, giaTri: number) => {
      if (mqttStatusRef.current.state !== 'da_noi') {
        showToast('Chưa nối được với máy chủ MQTT.', 'error');
        return;
      }
      cuktechCaiDatExpected.current[piid] = {
        giaTri,
        deadline: Date.now() + GIAY_CHO_CUKTECH * 1000,
      };
      setChoCaiDatCuktech((prev) => ({ ...prev, [piid]: true }));

      void publishMqtt(CuktechTopic.CMD_SET, JSON.stringify({ piid, value: giaTri })).then((ack) => {
        if (!ack.ok) {
          delete cuktechCaiDatExpected.current[piid];
          setChoCaiDatCuktech((prev) => {
            const next = { ...prev };
            delete next[piid];
            return next;
          });
          showToast(`Không gửi được cài đặt: ${ack.error || ack.reason || 'lỗi'}`, 'error');
        }
      });
    },
    [publishMqtt, showToast],
  );

  const noiLaiCuktech = useCallback(() => {
    if (mqttStatusRef.current.state !== 'da_noi') {
      showToast('Chưa nối được với máy chủ MQTT.', 'error');
      return;
    }
    cuktechNoiLaiDeadline.current = Date.now() + GIAY_CHO_NOI_LAI * 1000;
    setDangNoiLaiCuktech(true);
    void publishMqtt(CuktechTopic.CMD_BLE, JSON.stringify({ enabled: false })).then((ackOff) => {
      if (!ackOff.ok) {
        cuktechNoiLaiDeadline.current = 0;
        setDangNoiLaiCuktech(false);
        showToast(`Không gửi được lệnh nối lại: ${ackOff.error || ackOff.reason || 'lỗi'}`, 'error');
        return;
      }
      window.setTimeout(() => {
        void publishMqtt(CuktechTopic.CMD_BLE, JSON.stringify({ enabled: true })).then((ackOn) => {
          if (!ackOn.ok) {
            cuktechNoiLaiDeadline.current = 0;
            setDangNoiLaiCuktech(false);
            showToast(`Không bật lại được BLE: ${ackOn.error || ackOn.reason || 'lỗi'}`, 'error');
          }
        });
      }, 2000);
    });
  }, [publishMqtt, showToast]);

  // WiFi actions — wifi_collector.py nhan chuoi tho "reboot" tren topic wifi/cmd
  const guiLenhWifi = useCallback(
    (cmd: Record<string, unknown> | string, okMsg?: string): boolean => {
      if (mqttStatusRef.current.state !== 'da_noi') {
        showToast('Cần nối MQTT để gửi lệnh tới Router.', 'warning');
        return false;
      }
      const rawPayload =
        typeof cmd === 'string'
          ? cmd
          : cmd.action === 'reboot' && Object.keys(cmd).length === 1
            ? 'reboot'
            : JSON.stringify(cmd);
      void publishMqtt(WifiTopic.CMD, rawPayload).then((ack) => {
        if (!ack.ok) showToast(`Lỗi gửi lệnh WiFi: ${ack.error || ack.reason}`, 'error');
        else if (okMsg) showToast(okMsg, 'success');
      });
      return true;
    },
    [publishMqtt, showToast],
  );

  const khoiDongLaiWifi = useCallback((): boolean => {
    return guiLenhWifi('reboot', 'Đã gửi lệnh khởi động lại Router qua MQTT.');
  }, [guiLenhWifi]);

  const toggleWifiSecurity = useCallback(
    (key: string) => {
      setWifi((prev) => ({
        ...prev,
        securitySettings: prev.securitySettings.map((s) =>
          s.key === key ? { ...s, enabled: !s.enabled } : s,
        ),
      }));
      guiLenhWifi({ action: 'toggle_security', key });
    },
    [guiLenhWifi],
  );

  const toggleWifiTrusted = useCallback(
    (id: string) => {
      setWifi((prev) => ({
        ...prev,
        devices: prev.devices.map((d) => (d.id === id ? { ...d, trusted: !d.trusted } : d)),
      }));
      guiLenhWifi({ action: 'toggle_trusted', id });
    },
    [guiLenhWifi],
  );

  const toggleWifiBlocked = useCallback(
    (id: string) => {
      setWifi((prev) => ({
        ...prev,
        devices: prev.devices.map((d) => (d.id === id ? { ...d, blocked: !d.blocked } : d)),
      }));
      guiLenhWifi({ action: 'toggle_blocked', id });
    },
    [guiLenhWifi],
  );

  // Emergency detection + Auto-exit screensaver + Idle timer
  const kitchenRoom = rooms.find((r) => r.id === 'kitchen');
  const rawEmergencyAlert = useMemo(
    () =>
      kiemTraSuCoKhanCap({
        kitchenGasAlert: kitchenRoom?.gasAlert === true,
        mqttGasAlert,
        mqttGasPpm,
        bomNuoc,
        ketNoiBom,
        mqttStatus,
        gatewayOnline,
      }),
    [kitchenRoom?.gasAlert, mqttGasAlert, mqttGasPpm, bomNuoc, ketNoiBom, mqttStatus, gatewayOnline],
  );

  const emergencyAlert =
    rawEmergencyAlert && rawEmergencyAlert.key !== dismissedEmergencyKey ? rawEmergencyAlert : null;

  useEffect(() => {
    if (!rawEmergencyAlert) {
      setDismissedEmergencyKey(null);
    }
  }, [rawEmergencyAlert]);

  useEffect(() => {
    if (emergencyAlert && screensaverActive) {
      setScreensaverActive(false);
    }
  }, [emergencyAlert, screensaverActive]);

  const enterScreensaver = useCallback(() => {
    setScreensaverActive(true);
    pushNavHistory({ dest: currentDestRef.current, screensaver: true });
  }, [pushNavHistory]);

  const wakeScreensaver = useCallback(() => {
    lastInteractionRef.current = Date.now();
    setScreensaverActive(false);
    setCurrentDest('home');
    setSelectedRoomDetailId(null);
    setChiTietDienOpen(false);
    setWifiOpen(false);
  }, []);

  const updateScreensaverConfig = useCallback((cfg: ScreensaverConfig) => {
    setScreensaverConfig(cfg);
    saveScreensaverConfig(cfg);
  }, []);

  const dismissEmergencyAlert = useCallback(() => {
    if (rawEmergencyAlert) {
      setDismissedEmergencyKey(rawEmergencyAlert.key);
    }
  }, [rawEmergencyAlert]);

  useEffect(() => {
    const bump = () => {
      if (!screensaverActiveRef.current) {
        lastInteractionRef.current = Date.now();
      }
    };
    window.addEventListener('pointerdown', bump, { passive: true });
    window.addEventListener('keydown', bump, { passive: true });
    window.addEventListener('touchstart', bump, { passive: true });
    window.addEventListener('wheel', bump, { passive: true });
    return () => {
      window.removeEventListener('pointerdown', bump);
      window.removeEventListener('keydown', bump);
      window.removeEventListener('touchstart', bump);
      window.removeEventListener('wheel', bump);
    };
  }, []);

  useEffect(() => {
    const id = window.setInterval(() => {
      const now = Date.now();
      if (adminTokenRef.current && adminUnlockExpiryRef.current > 0 && now >= adminUnlockExpiryRef.current) {
        adminTokenRef.current = null;
        adminUnlockExpiryRef.current = 0;
        setAdminUnlocked(false);
      }
      const sc = screensaverConfigRef.current;
      if (
        sc.enabled &&
        !screensaverActiveRef.current &&
        !emergencyAlert &&
        sc.idleMinutes > 0 &&
        now - lastInteractionRef.current >= sc.idleMinutes * 60_000
      ) {
        setScreensaverActive(true);
      }
    }, 5000);
    return () => window.clearInterval(id);
  }, [emergencyAlert]);

  // Admin PIN helpers
  const adminLockEnabled = config.adminPin?.enabled !== false;

  const adminHeaders = useCallback((): Record<string, string> => {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (adminTokenRef.current) {
      headers['x-admin-token'] = adminTokenRef.current;
    }
    return headers;
  }, []);

  const handleAdminUnauthorized = useCallback(
    (actionLabel: string, retryFn: () => void | Promise<void>) => {
      adminTokenRef.current = null;
      adminUnlockExpiryRef.current = 0;
      setAdminUnlocked(false);
      pendingAdminActionRef.current = retryFn;
      setPinPrompt({ label: actionLabel, error: 'Phiên kỹ thuật đã hết hạn, vui lòng nhập PIN.', busy: false });
    },
    [],
  );

  const requireAdminPin = useCallback((actionLabel: string, fn: () => void | Promise<void>) => {
    const enabled = configRef.current.adminPin?.enabled !== false;
    const valid =
      !enabled ||
      (Boolean(adminTokenRef.current) && Date.now() < adminUnlockExpiryRef.current);
    if (valid) {
      if (adminTokenRef.current) {
        adminUnlockExpiryRef.current = Date.now() + 5 * 60_000;
      }
      void fn();
      return;
    }
    pendingAdminActionRef.current = fn;
    setPinPrompt({ label: actionLabel, error: null, busy: false });
  }, []);

  const submitAdminPin = useCallback(async (pin: string) => {
    setPinPrompt((prev) => (prev ? { ...prev, busy: true, error: null } : prev));
    try {
      const resp = await fetch('/api/admin/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin }),
      });
      const data = (await resp.json()) as { ok: boolean; token?: string; expiresAt?: number; error?: string };
      if (resp.ok && data.ok && data.token) {
        adminTokenRef.current = data.token;
        adminUnlockExpiryRef.current = data.expiresAt ?? Date.now() + 5 * 60_000;
        setAdminUnlocked(true);
        const pending = pendingAdminActionRef.current;
        pendingAdminActionRef.current = null;
        setPinPrompt(null);
        if (pending) await pending();
        return;
      }
      setPinPrompt((prev) =>
        prev ? { ...prev, busy: false, error: data.error || 'Mã PIN không đúng.' } : prev,
      );
    } catch {
      setPinPrompt((prev) =>
        prev ? { ...prev, busy: false, error: 'Không kết nối được Gateway để kiểm tra PIN.' } : prev,
      );
    }
  }, []);

  const cancelAdminPin = useCallback(() => {
    pendingAdminActionRef.current = null;
    setPinPrompt(null);
  }, []);

  const lockAdmin = useCallback(async () => {
    const token = adminTokenRef.current;
    adminTokenRef.current = null;
    adminUnlockExpiryRef.current = 0;
    setAdminUnlocked(false);
    if (token) {
      void fetch('/api/admin/lock', {
        method: 'POST',
        headers: { 'x-admin-token': token },
      }).catch(() => {});
    }
    showToast('Đã khóa khu vực Kỹ thuật.', 'info');
  }, [showToast]);

  const updateAdminPin = useCallback(
    async (form: { currentPin: string; newPin?: string; enabled?: boolean }) => {
      try {
        const resp = await fetch('/api/admin/pin', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(form),
        });
        const data = (await resp.json()) as { ok: boolean; config?: PublicAppConfig; error?: string };
        if (resp.ok && data.ok && data.config) {
          configRef.current = data.config;
          setConfig(data.config);
          if (form.newPin || form.enabled === false) {
            adminTokenRef.current = null;
            adminUnlockExpiryRef.current = 0;
            setAdminUnlocked(false);
          }
          showToast('Đã cập nhật cấu hình khóa PIN 4 số.', 'success');
          return { ok: true };
        }
        return { ok: false, error: data.error || 'Không cập nhật được mã PIN.' };
      } catch {
        return { ok: false, error: 'Lỗi kết nối Gateway.' };
      }
    },
    [showToast],
  );

  const refreshDockerControlStatus = useCallback(async () => {
    try {
      const resp = await fetch('/api/server/docker/status', { credentials: 'same-origin' });
      const data = (await resp.json()) as {
        ok?: boolean;
        configured?: boolean;
        enabled?: boolean;
        protectedContainers?: string[];
        allowedContainers?: string[];
        error?: string;
      };
      setDockerControlStatus({
        checked: true,
        configured: Boolean(data.configured),
        enabled: Boolean(data.enabled),
        protectedContainers: Array.isArray(data.protectedContainers)
          ? data.protectedContainers
          : ['mosquitto', 'server-collector', 'smarthome-web'],
        allowedContainers: Array.isArray(data.allowedContainers) ? data.allowedContainers : [],
        error: typeof data.error === 'string' ? data.error : undefined,
      });
    } catch {
      setDockerControlStatus((prev) => ({
        ...prev,
        checked: true,
        enabled: false,
        error: 'Không kiểm tra được trạng thái điều khiển Docker từ Gateway.',
      }));
    }
  }, []);

  const refreshDockerContainers = useCallback(async () => {
    try {
      const resp = await fetch('/api/server/docker/containers', { credentials: 'same-origin' });
      const data = (await resp.json()) as { ok?: boolean; containers?: unknown[]; error?: string };
      if (resp.ok && data.ok && Array.isArray(data.containers)) {
        setServer((cur) => ({
          ...cur,
          containers: parseServerContainers(data.containers),
          isRealData: true,
        }));
      }
    } catch {
      // ignore transient refresh failure
    }
  }, []);

  const controlDockerContainer = useCallback(
    async (name: string, action: DockerContainerAction): Promise<{ ok: boolean; error?: string }> => {
      if (isCoreProtectedContainer(name)) {
        const msg = `Container lõi "${name}" nằm trong danh sách bảo vệ cứng, không được phép điều khiển từ xa.`;
        showToast(msg, 'warning');
        return { ok: false, error: msg };
      }
      const actionText = action === 'start' ? 'Bật' : action === 'stop' ? 'Dừng' : 'Khởi động lại';
      setDockerBusyName(name);
      try {
        const resp = await fetch('/api/server/docker/action', {
          method: 'POST',
          credentials: 'same-origin',
          headers: adminHeaders(),
          body: JSON.stringify({ name, action }),
        });
        const data = (await resp.json()) as {
          ok?: boolean;
          requirePin?: boolean;
          message?: string;
          error?: string;
          containers?: unknown[];
        };
        if (resp.status === 401 && data.requirePin) {
          handleAdminUnauthorized(`${actionText} container ${name}`, async () => {
            await controlDockerContainer(name, action);
          });
          return { ok: false, error: data.error || 'Cần nhập PIN kỹ thuật (4 số).' };
        }
        if (resp.ok && data.ok) {
          if (Array.isArray(data.containers)) {
            setServer((cur) => ({
              ...cur,
              containers: parseServerContainers(data.containers),
              isRealData: true,
            }));
          }
          showToast(data.message || `${actionText} container ${name} thành công.`, 'success');
          return { ok: true };
        }
        const errMsg = data.error || data.message || `Không thể ${actionText.toLowerCase()} container ${name}.`;
        showToast(errMsg, 'error');
        return { ok: false, error: errMsg };
      } catch {
        const errMsg = `Không kết nối được Gateway để ${actionText.toLowerCase()} container ${name}.`;
        showToast(errMsg, 'error');
        return { ok: false, error: errMsg };
      } finally {
        setDockerBusyName((cur) => (cur === name ? null : cur));
      }
    },
    [adminHeaders, handleAdminUnauthorized, showToast],
  );

  // Settings API calls
  const luuMqttConfig = useCallback(
    async (form: {
      host: string;
      cong: number;
      tls: boolean;
      taiKhoan: string;
      matKhau?: string;
      xoaMatKhau?: boolean;
      clientId: string;
    }) => {
      try {
        const resp = await fetch('/api/config/mqtt', {
          method: 'PUT',
          headers: adminHeaders(),
          body: JSON.stringify(form),
        });
        if (resp.status === 401) {
          handleAdminUnauthorized('Lưu cấu hình MQTT', () => void luuMqttConfig(form));
          return false;
        }
        const data = (await resp.json()) as { ok: boolean; config: PublicAppConfig; mqttStatus: KetNoi };
        if (data.ok) {
          configRef.current = data.config;
          setConfig(data.config);
          setMqttStatus(data.mqttStatus);
          showToast('Đã lưu cấu hình MQTT & đang kết nối lại.', 'success');
          return true;
        }
      } catch {
        // ignore
      }
      showToast('Không lưu được cấu hình MQTT.', 'error');
      return false;
    },
    [adminHeaders, handleAdminUnauthorized, showToast],
  );

  const luuCameraConfig = useCallback(
    async (form: {
      host: string;
      cong: number;
      congGhiHinh?: number;
      tls: boolean;
      taiKhoan: string;
      matKhau?: string;
      xoaMatKhau?: boolean;
    }) => {
      setCameraStatus(form.host.trim() ? { state: 'dang_do' } : { state: 'chua_cau_hinh' });
      try {
        const resp = await fetch('/api/config/camera', {
          method: 'PUT',
          headers: adminHeaders(),
          body: JSON.stringify(form),
        });
        if (resp.status === 401) {
          handleAdminUnauthorized('Lưu cấu hình Camera go2rtc', () => void luuCameraConfig(form));
          return false;
        }
        const data = (await resp.json()) as {
          ok: boolean;
          config: PublicAppConfig;
          probe: { ok: boolean; streams?: string[]; error?: string };
        };
        if (data.ok) {
          configRef.current = data.config;
          setConfig(data.config);
          if (!data.config.camera.host.trim()) {
            setCameraStatus({ state: 'chua_cau_hinh' });
            showToast('Đã xóa địa chỉ máy chủ Camera.', 'info');
          } else if (data.probe.ok) {
            setCameraStatus({ state: 'co', ds: data.probe.streams ?? [] });
            showToast(`Đã lưu & thấy ${data.probe.streams?.length ?? 0} luồng camera.`, 'success');
          } else {
            setCameraStatus({ state: 'loi', moTa: data.probe.error || 'Chưa kết nối được go2rtc' });
            showToast(`Đã lưu Camera nhưng dò lỗi: ${data.probe.error}`, 'warning');
          }
          return true;
        }
      } catch {
        // ignore
      }
      showToast('Không lưu được cấu hình Camera.', 'error');
      return false;
    },
    [adminHeaders, handleAdminUnauthorized, showToast],
  );

  const luuWeatherConfig = useCallback(
    async (form: WeatherConfig) => {
      try {
        const resp = await fetch('/api/config/weather', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(form),
        });
        const data = (await resp.json()) as { ok: boolean; config: PublicAppConfig };
        if (data.ok) {
          configRef.current = data.config;
          setConfig(data.config);
          setThoiTiet({ state: 'dang_tai' });
          await taiThoiTiet();
          showToast('Đã lưu địa điểm thời tiết.', 'success');
          return true;
        }
      } catch {
        // ignore
      }
      showToast('Không lưu được địa điểm thời tiết.', 'error');
      return false;
    },
    [showToast, taiThoiTiet],
  );

  const luuAquaControlUrl = useCallback(
    async (aquaControlUrl: string) => {
      try {
        const resp = await fetch('/api/config/aquacontrol', {
          method: 'PUT',
          headers: adminHeaders(),
          body: JSON.stringify({ aquaControlUrl }),
        });
        if (resp.status === 401) {
          handleAdminUnauthorized('Lưu địa chỉ AquaControl Web', () => void luuAquaControlUrl(aquaControlUrl));
          return false;
        }
        const data = (await resp.json()) as { ok: boolean; config: PublicAppConfig };
        if (data.ok) {
          configRef.current = data.config;
          setConfig(data.config);
          showToast('Đã lưu địa chỉ AquaControl Web.', 'success');
          return true;
        }
      } catch {
        // ignore
      }
      showToast('Không lưu được địa chỉ AquaControl.', 'error');
      return false;
    },
    [adminHeaders, handleAdminUnauthorized, showToast],
  );

  const luuBomNuocConfig = useCallback(
    async (form: BomNuocConfigForm) => {
      try {
        const resp = await fetch('/api/config/bomnuoc', {
          method: 'PUT',
          headers: adminHeaders(),
          body: JSON.stringify(form),
        });
        if (resp.status === 401) {
          handleAdminUnauthorized('Lưu cấu hình bo bơm nước', () => void luuBomNuocConfig(form));
          return false;
        }
        const data = (await resp.json()) as {
          ok: boolean;
          config: PublicAppConfig;
          link: TrangThaiKetNoiBom;
          kenh: KenhBomNuoc;
        };
        if (data.ok) {
          configRef.current = data.config;
          setConfig(data.config);
          setKetNoiBom(data.link);
          setKenhBom(data.kenh);
          showToast('Đã lưu cấu hình kết nối bo bơm nước.', 'success');
          return true;
        }
      } catch {
        // ignore
      }
      showToast('Không lưu được cấu hình bo bơm nước.', 'error');
      return false;
    },
    [adminHeaders, handleAdminUnauthorized, showToast],
  );

  const refreshTelegramStatus = useCallback(async () => {
    try {
      const resp = await fetch('/api/alerts/status');
      if (resp.ok) {
        const data = (await resp.json()) as TelegramAlertsStatus;
        setTelegramStatus(data);
      }
    } catch {
      // ignore
    }
  }, []);

  const luuTelegramConfig = useCallback(
    async (form: {
      enabled: boolean;
      botToken?: string;
      xoaBotToken?: boolean;
      chatId: string;
      offlineMinutes: number;
      electricSpikePct: number;
      electricSpikeMinKwh: number;
      gasThresholdPpm: number;
      cooldownMinutes: number;
      pirTopic: string;
      gasTopic: string;
    }) => {
      try {
        const resp = await fetch('/api/config/telegram', {
          method: 'PUT',
          headers: adminHeaders(),
          body: JSON.stringify(form),
        });
        if (resp.status === 401) {
          handleAdminUnauthorized('Lưu cấu hình cảnh báo Telegram', () => void luuTelegramConfig(form));
          return false;
        }
        const data = (await resp.json()) as {
          ok: boolean;
          config: PublicAppConfig;
          alertsStatus: TelegramAlertsStatus;
        };
        if (data.ok) {
          configRef.current = data.config;
          setConfig(data.config);
          setTelegramStatus(data.alertsStatus);
          showToast('Đã lưu cấu hình cảnh báo Telegram.', 'success');
          return true;
        }
      } catch {
        // ignore
      }
      showToast('Không lưu được cấu hình Telegram.', 'error');
      return false;
    },
    [adminHeaders, handleAdminUnauthorized, showToast],
  );

  const testTelegramAlert = useCallback(async () => {
    try {
      const resp = await fetch('/api/alerts/telegram/test', {
        method: 'POST',
        headers: adminHeaders(),
      });
      if (resp.status === 401) {
        handleAdminUnauthorized('Gửi thử tin nhắn Telegram', () => void testTelegramAlert());
        return;
      }
      const data = (await resp.json()) as {
        ok: boolean;
        error?: string;
        alertsStatus?: TelegramAlertsStatus;
      };
      if (data.alertsStatus) setTelegramStatus(data.alertsStatus);
      if (data.ok) {
        showToast('Đã gửi tin nhắn thử tới Telegram!', 'success');
      } else {
        showToast(data.error || 'Chưa gửi được tin nhắn Telegram.', 'warning');
      }
    } catch {
      showToast('Không gọi được Gateway để gửi thử Telegram.', 'error');
    }
  }, [adminHeaders, handleAdminUnauthorized, showToast]);

  const guiLenhBom = useCallback(
    async (khoa: string, cmd: BomNuocCmd): Promise<void> => {
      setLocalPendingBom((prev) => ({ ...prev, [khoa]: true }));
      try {
        const resp = await fetch('/api/bomnuoc/cmd', {
          method: 'POST',
          headers: adminHeaders(),
          body: JSON.stringify(cmd),
        });
        if (resp.status === 401) {
          handleAdminUnauthorized('Thao tác kỹ thuật bo bơm', () => guiLenhBom(khoa, cmd));
          return;
        }
        const res = (await resp.json()) as KetQuaLenhBomRes;
        if (res.ok) {
          showToast(res.message, 'success');
        } else if (
          res.status === 'bi_khoa_bao_ve' ||
          res.status === 'chua_xac_nhan' ||
          res.status === 'dang_ban'
        ) {
          showToast(res.message, 'warning');
        } else {
          showToast(res.message || 'Chưa gửi được lệnh bơm.', 'error');
        }
      } catch {
        showToast('Không gọi được gateway để gửi lệnh bơm.', 'error');
      } finally {
        setLocalPendingBom((prev) => {
          const next = { ...prev };
          delete next[khoa];
          return next;
        });
      }
    },
    [adminHeaders, handleAdminUnauthorized, showToast],
  );

  const datCheDoBon = useCallback(
    (mode: 'auto' | 'on' | 'off') => guiLenhBom('tank', { kind: 'tank', mode }),
    [guiLenhBom],
  );

  const datBomTangAp = useCallback(
    (bat: boolean) => guiLenhBom('booster', { kind: 'booster', on: bat }),
    [guiLenhBom],
  );

  const datDenPhuBom = useCallback(
    (bat: boolean) => guiLenhBom('led', { kind: 'led', on: bat }),
    [guiLenhBom],
  );

  const xoaLoiBom = useCallback(() => guiLenhBom('clear', { kind: 'clear' }), [guiLenhBom]);

  const tatCoiBom = useCallback(() => guiLenhBom('coiTat', { kind: 'coiTat' }), [guiLenhBom]);

  const thuCoiBom = useCallback(() => guiLenhBom('coiThu', { kind: 'coiThu' }), [guiLenhBom]);

  const luuBaoVeBom = useCallback(
    (dryCheck: number, minRest: number, maxRun: number, pulses: number, minFlow: number) =>
      guiLenhBom('protection', { kind: 'protection', dryCheck, minRest, maxRun, pulses, minFlow }),
    [guiLenhBom],
  );

  const ghiMocCanBom = useCallback(
    () => guiLenhBom('lvlSetEmpty', { kind: 'lvlSetEmpty' }),
    [guiLenhBom],
  );

  const ghiMocDayBom = useCallback(
    () => guiLenhBom('lvlSetFull', { kind: 'lvlSetFull' }),
    [guiLenhBom],
  );

  const datLaiDemLuuLuong = useCallback(
    () => guiLenhBom('calReset', { kind: 'calReset' }),
    [guiLenhBom],
  );

  const xoaLichSuNuocTrenBo = useCallback(
    () => guiLenhBom('usageClear', { kind: 'usageClear' }),
    [guiLenhBom],
  );

  const xoaNhatKyBomCucBo = useCallback(async () => {
    try {
      const resp = await fetch('/api/bomnuoc/log', {
        method: 'DELETE',
        headers: adminHeaders(),
      });
      if (resp.status === 401) {
        handleAdminUnauthorized('Xóa nhật ký bơm', () => xoaNhatKyBomCucBo());
        return;
      }
      if (resp.ok) {
        setNhatKyBom([]);
        showToast('Đã xoá nhật ký bơm trên gateway.', 'info');
        return;
      }
    } catch {
      // ignore
    }
    showToast('Không xoá được nhật ký bơm.', 'error');
  }, [adminHeaders, handleAdminUnauthorized, showToast]);

  // Voice Actions + TroLyGiongNoi (dong bo day du voi HomeViewModel.kt)
  const hanhDongNha = useMemo<HanhDongNha>(
    () => ({
      maPhong: () => roomsRef.current.map((r) => r.id),
      tenPhong: (ma) => roomsRef.current.find((r) => r.id === ma)?.name ?? null,
      datDen: (phong, bat) => {
        if (timCongTac(congtacRef.current, phong, 'den')) {
          const err = gatCongTac(phong, 'den', false, bat, LenhCongTac.NGUON_GIONG_NOI);
          if (err) throw new LoiLenh(err);
          return;
        }
        updateRoom(phong, (r) => ({ ...r, lightOn: bat }));
      },
      datQuat: (phong, bat) => {
        if (timCongTac(congtacRef.current, phong, 'quat')) {
          const err = gatCongTac(phong, 'quat', false, bat, LenhCongTac.NGUON_GIONG_NOI);
          if (err) throw new LoiLenh(err);
          return;
        }
        updateRoom(phong, (r) => ({ ...r, fanOn: bat }));
      },
      datDieuHoa: (phong, bat) => updateRoom(phong, (r) => ({ ...r, acOn: bat })),
      datLichHen: (phong, bat) => updateRoom(phong, (r) => ({ ...r, scheduleOn: bat })),
      datTv: (phong, bat) => updateRoom(phong, (r) => ({ ...r, tvOn: bat })),
      datRem: (phong, mo) => updateRoom(phong, (r) => ({ ...r, curtainOpen: mo ? 100 : 0 })),
      datBepTu: (phong, bat) => {
        if (phong !== 'kitchen') throw new LoiLenh('Chỉ có bếp từ ở Nhà bếp.');
        updateRoom(phong, (r) => ({ ...r, stoveOn: bat }));
      },
      datNhietDo: (phong, doC) => {
        const capped = Math.min(30, Math.max(16, Math.trunc(doC)));
        updateRoom(phong, (r) => ({ ...r, acTemp: capped, acOn: true }));
        return capped;
      },
      chinhNhietDo: (phong, buoc) => {
        const cur = roomsRef.current.find((r) => r.id === phong)?.acTemp ?? 26;
        const next = Math.min(30, Math.max(16, cur + buoc));
        updateRoom(phong, (r) => ({ ...r, acTemp: next, acOn: true }));
        return next;
      },
      datCheDo: (phong, cheDo) => {
        const mapped = CHE_DO_HOP_LE[cheDo.trim().toUpperCase()];
        if (!mapped) throw new LoiLenh('Chế độ hợp lệ: ngủ, nhà, vắng, tùy chỉnh.');
        setRoomMode(phong, mapped);
      },
      datONhanh: (id, bat) => {
        if (!quickItemsRef.current.some((q) => q.id === id)) {
          throw new LoiLenh('Ô nhanh chỉ có: đèn, quạt, điều hòa, rèm.');
        }
        setQuickItems((prev) => prev.map((q) => (q.id === id ? { ...q, on: bat } : q)));
      },
      dangBat: (phong, thietBi) => {
        if (phong === CongCuNha.NHANH) {
          return quickItemsRef.current.find((q) => q.id === thietBi)?.on ?? null;
        }
        const ct = congtacRef.current;
        if (timCongTac(ct, phong, thietBi)) {
          const tt = trangThaiCongTacCua(ct, phong, thietBi);
          if (!isBridgeSong(ct) || tt?.online !== true) return null;
          return tt.bat ?? null;
        }
        const r = roomsRef.current.find((x) => x.id === phong);
        if (!r) return null;
        switch (thietBi) {
          case 'den': return r.lightOn;
          case 'quat': return r.fanOn;
          case 'dieu_hoa': return r.acOn;
          case 'tivi': return r.tvOn;
          case 'rem': return r.curtainOpen > 0;
          case 'bep_tu': return r.stoveOn;
          case 'lich_hen': return r.scheduleOn;
          default: return null;
        }
      },
      laThat: (phong, thietBi) => Boolean(timCongTac(congtacRef.current, phong, thietBi)),
      thietBiThat: () => {
        const list = [...tenThietBiThat(congtacRef.current)];
        if (daCauHinhBom(configRef.current.bomNuoc)) {
          list.push('Hệ thống bơm nước (bơm lên bồn, bơm tăng áp)');
        }
        return list;
      },
      bomNuocLaThat: () => daCauHinhBom(configRef.current.bomNuoc),
      dieuKhienBom: (thietBi, hanhDong, giaTri) => {
        if (!daCauHinhBom(configRef.current.bomNuoc)) {
          throw new LoiLenh('Chưa cấu hình kết nối bo bơm nước. Vào Tự động hóa → Cấu hình.');
        }
        const s = bomNuocRef.current;
        if (!s) {
          throw new LoiLenh('Chưa nhận được dữ liệu từ bo bơm nước.');
        }
        if (!bomTrucTuyen(ketNoiBomRef.current)) {
          throw new LoiLenh(
            `Bo bơm nước hiện không trực tuyến (${nhanKetNoiBom(ketNoiBomRef.current, kenhBomRef.current)}).`,
          );
        }
        switch (thietBi) {
          case CongCuNha.BOM_CHINH:
          case CongCuNha.CHE_DO: {
            let mode: 'auto' | 'on' | 'off';
            if (hanhDong === CongCuNha.BAT) mode = 'on';
            else if (hanhDong === CongCuNha.TAT) mode = 'off';
            else if (hanhDong === CongCuNha.DAO) mode = bomChinhDangChay(s) ? 'off' : 'on';
            else if (hanhDong === CongCuNha.DAT) {
              const g = giaTri?.trim().toLowerCase() ?? '';
              if (g === 'auto' || g === 'tu_dong' || g === 'tự động') mode = 'auto';
              else if (g === 'on' || g === 'bat' || g === 'bật' || g === 'bat_tay') mode = 'on';
              else if (g === 'off' || g === 'tat' || g === 'tắt' || g === 'tat_tay') mode = 'off';
              else throw new LoiLenh('Bơm lên bồn chỉ nhận chế độ tự động (auto), bật hoặc tắt.');
            } else {
              throw new LoiLenh('Không rõ lệnh cho bơm lên bồn.');
            }
            if (mode === 'on' && dangKhoaBom(s)) {
              throw new LoiLenh('Bơm lên bồn đang khoá bảo vệ sau sự cố. Cần xoá lỗi trước khi bật tay.');
            }
            void datCheDoBon(mode);
            return `Bơm lên bồn: đã gửi lệnh chuyển sang ${nhanCheDoBon(mode)}`;
          }
          case CongCuNha.BOM_TANG_AP: {
            let bat: boolean;
            if (hanhDong === CongCuNha.BAT) bat = true;
            else if (hanhDong === CongCuNha.TAT) bat = false;
            else if (hanhDong === CongCuNha.DAO) bat = s.bstMode !== 'on';
            else throw new LoiLenh('Bơm tăng áp chỉ bật hoặc tắt được.');
            if (bat && (bomApDangKhoa(s) || dangKhoaBom(s))) {
              throw new LoiLenh('Bơm tăng áp đang khoá bảo vệ sau sự cố. Cần xoá lỗi trước khi bật lại.');
            }
            void datBomTangAp(bat);
            return `Bơm tăng áp: đã gửi lệnh ${bat ? 'bật' : 'tắt'}`;
          }
          case CongCuNha.DEN_PHU: {
            if (s.den === null) throw new LoiLenh('Bo bơm này không bật tính năng đèn phụ.');
            let bat: boolean;
            if (hanhDong === CongCuNha.BAT) bat = true;
            else if (hanhDong === CongCuNha.TAT) bat = false;
            else if (hanhDong === CongCuNha.DAO) bat = s.den !== 1;
            else throw new LoiLenh('Đèn phụ chỉ bật hoặc tắt được.');
            void datDenPhuBom(bat);
            return `Đèn phụ hộp bơm: đã gửi lệnh ${bat ? 'bật' : 'tắt'}`;
          }
          case CongCuNha.LOI_BOM:
            void xoaLoiBom();
            return 'Hệ thống bơm: đã gửi lệnh xoá lỗi và mở khoá';
          case CongCuNha.COI_BOM:
            if (hanhDong === CongCuNha.TAT) {
              void tatCoiBom();
              return 'Còi báo động bơm: đã gửi lệnh tắt';
            }
            if (hanhDong === CongCuNha.BAT) {
              void thuCoiBom();
              return 'Còi báo động bơm: đã gửi lệnh kêu thử';
            }
            throw new LoiLenh('Còi bơm chỉ bật thử hoặc tắt được.');
          default:
            throw new LoiLenh('Hệ bơm nước chỉ hỗ trợ bơm chính, bơm tăng áp, đèn phụ, xoá lỗi và còi.');
        }
      },
      moManHinh: (dich) => {
        const raw = dich.trim();
        if (raw.startsWith('phong:')) {
          const id = raw.slice('phong:'.length);
          if (!roomsRef.current.some((r) => r.id === id)) return false;
          openRoomDetail(id);
          return true;
        }
        const target = MAN_HINH_HOP_LE[raw];
        if (!target) return false;
        if (target.route) navigateDest(target.route);
        else if (target.sub === 'chi_tiet_dien') openChiTietDien();
        else if (target.sub === 'wifi') openWifi();
        return true;
      },
      docSoLieu: (muc) => {
        const d = dienRef.current;
        const s = serverRef.current;
        const w = wifiRef.current;
        const ct = congtacRef.current;
        const m = mqttStatusRef.current;
        switch (muc) {
          case 'trang_thai_nha':
            return {
              co_so: true,
              o_nhanh: quickItemsRef.current.map((q) => ({ ma: q.id, ten: q.label, bat: q.on })),
              phong: roomsRef.current.map((r) => {
                const denTb = timCongTac(ct, r.id, 'den');
                const quatTb = timCongTac(ct, r.id, 'quat');
                const denSt = denTb ? ct.trangThai[denTb.ma] : undefined;
                const quatSt = quatTb ? ct.trangThai[quatTb.ma] : undefined;
                const denVal = denTb
                  ? isBridgeSong(ct) && denSt?.online
                    ? denSt.bat
                    : null
                  : r.lightOn;
                const quatVal = quatTb
                  ? isBridgeSong(ct) && quatSt?.online
                    ? quatSt.bat
                    : null
                  : r.fanOn;
                const base: Record<string, unknown> = {
                  ma: r.id,
                  ten: r.name,
                  che_do: tenCheDoPhong(r.id, r.mode),
                  den: denVal,
                  den_that: Boolean(denTb),
                  do_sang_den: r.lightBrightness,
                  quat: quatVal,
                  quat_that: Boolean(quatTb),
                  dieu_hoa: r.acOn,
                  nhiet_do_dat: r.acTemp,
                  lich_hen: r.scheduleOn,
                  gio_hen: r.scheduleTime,
                };
                if (r.id === 'living') {
                  base.tivi = r.tvOn;
                  base.rem_mo = r.curtainOpen;
                } else if (r.id === 'kitchen') {
                  base.cap_hut_mui = r.hoodSpeed;
                  base.bep_tu = r.stoveOn;
                  base.muc_bep_tu = r.stoveLevel;
                  base.canh_bao_gas = r.gasAlert;
                  base.gas_ppm = r.gasPpm;
                } else if (r.id === 'hall') {
                  base.den_truoc_nha = r.hallFrontLightOn;
                  base.den_xung_quanh = r.hallSurroundingLightOn;
                  base.khoa_cua = r.hallDoorLocked;
                  base.camera = r.hallCameraOn;
                }
                return base;
              }),
            };
          case 'dien':
            return {
              co_so: coSoLieuDien(d),
              ket_noi: m.state === 'da_noi' ? 'đã nối' : m.state,
              ly_do: m.state === 'loi' ? m.moTa : d.loi,
              cap_nhat: d.capNhat ?? 'chưa có',
              ngay_gan_nhat: d.ngay,
              hom_nay_kwh: d.ngaySanLuong,
              thang_kwh: d.thangSanLuong,
              tien_thang_vnd: d.thangTien,
              cong_no_dong: d.congNoSoTien,
              chi_so_cong_to: d.congToChiSo,
              tong_7_ngay_kwh: tongKwhDien(d, 7),
              tong_30_ngay_kwh: tongKwhDien(d, 30),
              do_tuoi: doTuoiDien(d),
            };
          case 'server':
            return {
              co_so: s.isRealData,
              ly_do: !s.isRealData ? 'Chưa nhận được gói MQTT server/#, đang hiện số mẫu.' : null,
              hostname: s.info.hostname,
              he_dieu_hanh: s.info.os,
              cpu_phan_tram: s.metric.cpuPercent,
              ram_phan_tram: s.metric.ramPercent,
              ram_dung_gb: s.metric.ramUsedGb,
              ram_tong_gb: s.info.ramTotalGb,
              dia_phan_tram: s.metric.diskPercent,
              dia_dung_gb: s.metric.diskUsedGb,
              dia_tong_gb: s.info.diskTotalGb,
              nhiet_do_c: s.metric.cpuTempC,
              chay_lien_tuc_ngay: s.info.uptimeDays,
              so_container_docker: s.containers.length,
              du_lieu: s.isRealData ? 'thật' : 'mẫu',
            };
          case 'wifi':
            return {
              co_so: w.isRealData,
              ly_do: !w.isRealData ? 'Chưa nhận được gói MQTT wifi/#, đang hiện số mẫu.' : null,
              ssid: w.info.ssid,
              dang_noi: w.info.isConnected,
              tin_hieu_dbm: w.info.signalDbm,
              so_thiet_bi_online: w.devices.filter((x) => x.isOnline && !x.blocked).length,
              download_mbps: w.info.downloadMbps,
              tai_len_mbps: w.info.uploadMbps,
              du_lieu: w.isRealData ? 'thật' : 'mẫu',
            };
          case 'thoi_tiet': {
            const tt = thoiTietRef.current;
            if (tt.state !== 'co') {
              return { co_so: false, ly_do: tt.state === 'loi' ? tt.moTa : 'Đang tải thời tiết' };
            }
            return {
              co_so: true,
              dia_diem: tt.data.diaDiem,
              nhiet_do_c: tt.data.hienTai.nhietDo,
              cam_giac_c: tt.data.hienTai.camGiac,
              do_am: tt.data.hienTai.doAm,
              gio_kmh: tt.data.hienTai.gio,
            };
          }
          case 'lich_am': {
            const now = new Date();
            const dd = now.getDate();
            const mm = now.getMonth() + 1;
            const yy = now.getFullYear();
            const am = AmLich.tuDuongLich(dd, mm, yy);
            const cc = AmLich.canChi(am);
            const suKien = AmLich.suKien(dd, mm, am);
            return {
              co_so: true,
              duong_lich: `${dd}/${mm}/${yy}`,
              ngay_am: `${am.ngay}/${am.thang}${am.nhuan ? ' (nhuận)' : ''}/${am.nam}`,
              can_chi: `Ngày ${cc.ngay}, tháng ${cc.thang}, năm ${cc.nam}`,
              gio_hoang_dao: AmLich.gioHoangDao(cc.chiNgay),
              su_kien: suKien,
            };
          }
          case 'cong_tac':
            return {
              co_so: ct.danhSach.length > 0,
              ly_do:
                ct.danhSach.length === 0
                  ? 'Chưa nhận được danh sách công tắc từ congtac/info.'
                  : !isBridgeSong(ct)
                    ? 'Bộ nối công tắc trên máy chủ đang offline.'
                    : null,
              bridge: ct.bridge ?? 'chưa rõ',
              thiet_bi: ct.danhSach.map((tb) => {
                const st = ct.trangThai[tb.ma];
                const song = isBridgeSong(ct) && Boolean(st?.online);
                return {
                  ten: tb.ten,
                  phong: tb.phong,
                  he: tb.he,
                  online: song,
                  bat: song ? (st?.bat ?? null) : null,
                  luc: st?.luc ?? null,
                  loi: st?.lyDoLoi ?? null,
                };
              }),
            };
          case 'bom_nuoc': {
            if (!daCauHinhBom(configRef.current.bomNuoc)) {
              return { co_so: false, ly_do: 'Chưa cấu hình kết nối bo bơm nước.' };
            }
            const bn = bomNuocRef.current;
            const linkText = nhanKetNoiBom(ketNoiBomRef.current, kenhBomRef.current);
            if (!bn) {
              return { co_so: false, ly_do: `Chưa nhận được gói dữ liệu nào từ bo bơm (${linkText}).` };
            }
            const err = phanTichLoiBomNuoc(bn.fault);
            return {
              co_so: true,
              kenh: kenhBomRef.current,
              trang_thai_ket_noi: linkText,
              bom_chinh_dang_chay: bomChinhDangChay(bn),
              che_do_bon: nhanCheDoBon(bn.tankMode),
              bom_tang_ap_dang_chay: bomApDangChay(bn),
              phao_bon: bonDay(bn) ? 'báo đầy' : 'chưa đầy',
              muc_nuoc_phan_tram: mucNuocPhanTram(bn),
              muc_nuoc_lit: mucNuocLit(bn),
              luu_luong_bom_l_phut: bn.fTank,
              luu_luong_nuoc_may_l_phut: bn.fCity,
              nhiet_do_nuoc_c: nhietDoNuoc(bn),
              nhiet_do_bom_bon_c: nhietDoBomBon(bn),
              nhiet_do_bom_ap_c: nhietDoBomAp(bn),
              nuoc_thang_nay_lit: bn.useT,
              nuoc_may_thang_nay_lit: bn.useC,
              dang_khoa: dangKhoaBom(bn) || bomApDangKhoa(bn),
              loi: err?.noiDung ?? null,
            };
          }
          default:
            return { co_so: false, loi: 'Không rõ mục cần đọc' };
        }
      },
      chupTrangThai: () => {
        const ct = congtacRef.current;
        const formatSwitch = (roomId: string, loai: 'den' | 'quat', fallback: boolean): string => {
          const tb = timCongTac(ct, roomId, loai);
          if (!tb) return fallback ? 'bật' : 'tắt';
          const st = ct.trangThai[tb.ma];
          if (!isBridgeSong(ct) || !st?.online || st.bat === null) {
            return 'không rõ (mất kết nối)';
          }
          return st.bat ? 'bật' : 'tắt';
        };

        const lines = roomsRef.current.map((r) => {
          const modeLabel = tenCheDoPhong(r.id, r.mode);
          const denStr = formatSwitch(r.id, 'den', r.lightOn);
          if (r.id === 'kitchen') {
            const hutStr = formatSwitch(r.id, 'quat', r.fanOn);
            return (
              `- ${r.name} (${r.id}, chế độ ${modeLabel}): đèn=${denStr} (${r.lightBrightness}%), ` +
              `hút mùi=${hutStr} (cấp ${r.hoodSpeed}), bếp từ=${r.stoveOn ? `mức ${r.stoveLevel} (${stoveWatts(r.stoveLevel)}W)` : 'tắt'}, ` +
              `gas=${r.gasPpm} ppm${r.gasAlert ? ' (CẢNH BÁO!)' : ''}`
            );
          }
          if (r.id === 'hall') {
            const viewLabel = HALLWAY_VIEWS.find((v) => v.id === r.hallView)?.label ?? r.hallView;
            return (
              `- ${r.name} (${r.id}, chế độ ${modeLabel}, góc ${viewLabel}): đèn hành lang=${denStr} (${r.lightBrightness}%), ` +
              `đèn trước nhà=${r.hallFrontLightOn ? `${r.hallFrontBrightness}%` : 'tắt'}, ` +
              `đèn xung quanh=${r.hallSurroundingLightOn ? `${r.hallSurroundingBrightness}%` : 'tắt'}, ` +
              `khóa cửa=${r.hallDoorLocked ? 'đã khóa' : 'đang mở'}, ` +
              `camera=${r.hallCameraOn ? (r.hallIrMode ? 'bật IR' : 'bật') : 'tắt'}`
            );
          }
          const quatStr = formatSwitch(r.id, 'quat', r.fanOn);
          const extra =
            r.id === 'living'
              ? `, tivi=${r.tvOn ? 'bật' : 'tắt'}, rèm=${r.curtainOpen}%`
              : ` (tốc độ quạt ${r.fanSpeed})`;
          return (
            `- ${r.name} (${r.id}, chế độ ${modeLabel}): đèn=${denStr} (${r.lightBrightness}%), ` +
            `quạt=${quatStr}${extra}, điều hòa=${r.acOn ? `${r.acTemp}°C` : 'tắt'}`
          );
        });

        if (daCauHinhBom(configRef.current.bomNuoc)) {
          const bn = bomNuocRef.current;
          const link = ketNoiBomRef.current;
          if (!bn || link.state !== 'truc_tiep') {
            lines.push(`- Hệ thống bơm nước: ${nhanKetNoiBom(link, kenhBomRef.current)}`);
          } else {
            const pct = mucNuocPhanTram(bn);
            const muc = pct !== null ? `${Math.trunc(pct)}%` : 'không có cảm biến mức';
            const phao = bonDay(bn) ? 'báo đầy' : 'chưa đầy';
            const err = phanTichLoiBomNuoc(bn.fault);
            lines.push(
              `- Hệ thống bơm nước: bơm chính ${bomChinhDangChay(bn) ? 'đang chạy' : 'tắt'} (${nhanCheDoBon(bn.tankMode)}), ` +
                `bơm tăng áp ${bomApDangChay(bn) ? 'đang chạy' : 'tắt'}, phao ${phao}, mức nước ${muc}` +
                (err ? `, cảnh báo: ${err.noiDung}` : ''),
            );
          }
        }
        return lines.join('\n');
      },
      guiMqtt: (phong, thietBi, hanhDong, giaTri) => {
        const mappedAction =
          hanhDong === 'bat' ? 'ON' : hanhDong === 'tat' ? 'OFF' : hanhDong.toUpperCase();
        if (timCongTac(congtacRef.current, phong, thietBi)) {
          if (mappedAction === 'DAO') {
            bamCongTac(phong, thietBi, true);
          } else if (mappedAction === 'ON' || mappedAction === 'OFF') {
            gatCongTac(phong, thietBi, false, mappedAction === 'ON', LenhCongTac.NGUON_GIONG_NOI);
          }
          return true;
        }
        const payload = LenhCongTac.json(
          phong,
          thietBi,
          mappedAction,
          giaTri,
          LenhCongTac.NGUON_GIONG_NOI,
          LenhCongTac.maLenh(),
        );
        void publishMqtt('smarthome/control', payload);
        return true;
      },
    }),
    [
      updateRoom,
      setRoomMode,
      openRoomDetail,
      navigateDest,
      openChiTietDien,
      openWifi,
      bamCongTac,
      gatCongTac,
      publishMqtt,
      datCheDoBon,
      datBomTangAp,
      datDenPhuBom,
      xoaLoiBom,
      tatCoiBom,
      thuCoiBom,
    ],
  );

  const troLyRef = useRef<TroLyGiongNoi | null>(null);
  if (!troLyRef.current) {
    troLyRef.current = new TroLyGiongNoi({
      nha: hanhDongNha,
      getMucDung: () => voiceUsageRef.current,
      onPhienChange: (p) => setPhienGiongNoi(p),
      onRecordUsage: (giay, tv, tr, tt) => {
        setVoiceUsage((prev) => {
          const next = MucDung.gopPhien(prev, MucDung.homNay(), giay, tv, tr, tt);
          saveVoiceUsage(next);
          return next;
        });
      },
    });
  }

  const updateVoiceConfig = useCallback((c: CauHinhGiongNoi) => {
    const normalized: CauHinhGiongNoi = {
      ...c,
      giayToiDaPhien: Math.min(900, Math.max(60, Math.trunc(c.giayToiDaPhien || 600))),
    };
    setVoiceConfig(normalized);
    saveVoiceConfig(normalized);
  }, []);

  const clearVoiceUsage = useCallback(() => {
    setVoiceUsage([]);
    saveVoiceUsage([]);
    showToast('Đã xóa lịch sử mức dùng giọng nói.', 'info');
  }, [showToast]);

  const batTatGiongNoi = useCallback(() => {
    setVoiceSheetOpen(true);
    troLyRef.current?.batTat(voiceConfig);
  }, [voiceConfig]);

  const tatGiongNoi = useCallback(() => {
    troLyRef.current?.tat();
  }, []);

  const ngatLoiGiongNoi = useCallback(() => {
    troLyRef.current?.ngatLoi();
  }, []);

  const xoaLoiGiongNoi = useCallback(() => {
    troLyRef.current?.xoaLoi();
  }, []);

  const checkGithubUpdate = useCallback(async () => {
    setUpdateCheck({ status: 'checking' });
    try {
      const resp = await fetch('https://api.github.com/repos/trungpq96dev/app_smart_home/releases/latest', {
        headers: { Accept: 'application/vnd.github+json' },
      });
      if (!resp.ok) {
        setUpdateCheck({ status: 'error', message: `GitHub trả mã ${resp.status}` });
        return;
      }
      const raw = (await resp.json()) as {
        tag_name?: string;
        name?: string;
        body?: string;
        html_url?: string;
        published_at?: string;
      };
      const tag = (raw.tag_name || '').replace(/^v/i, '').trim();
      if (!tag) {
        setUpdateCheck({ status: 'error', message: 'Không đọc được phiên bản phát hành' });
        return;
      }
      if (!isNewerVersion(tag, ANDROID_BASE_VERSION)) {
        setUpdateCheck({ status: 'up_to_date', latestTag: raw.tag_name || tag });
      } else {
        setUpdateCheck({
          status: 'available',
          release: {
            tagName: raw.tag_name || tag,
            name: raw.name || raw.tag_name || tag,
            body: raw.body || '',
            htmlUrl: raw.html_url || 'https://github.com/trungpq96dev/app_smart_home/releases',
            publishedAt: raw.published_at || '',
          },
        });
      }
    } catch (err) {
      setUpdateCheck({
        status: 'error',
        message: err instanceof Error ? err.message : 'Không kiểm tra được GitHub',
      });
    }
  }, []);

  const selectedRoom = useMemo(
    () => rooms.find((r) => r.id === selectedRoomId) ?? rooms[0],
    [rooms, selectedRoomId],
  );

  void cuktechTongOat;

  const value: SmartHomeContextValue = {
    currentDest,
    selectedRoomDetailId,
    chiTietDienOpen,
    wifiOpen,
    navigateDest,
    openRoomDetail,
    openChiTietDien,
    openWifi,
    closeSubScreen,
    config,
    configLoaded,
    gatewayOnline,
    mqttStatus,
    cameraStatus,
    nowMs,
    toasts,
    dismissToast,
    showToast,
    dien,
    congtac,
    cuktech,
    server,
    wifi,
    thoiTiet,
    cameraRecorder,
    dangChupAnh,
    choCongTacIds,
    choCongSac,
    choCaiDatCuktech,
    dangDoiCuktechCong,
    dangDoiCuktechCaiDat,
    dangNoiLaiCuktech,
    rooms,
    selectedRoomId,
    selectedRoom,
    selectRoom: setSelectedRoomId,
    quickItems,
    cameras,
    selectedCameraIndex,
    selectCamera,
    chupAnhCamera,
    datCanhNguoiCamera,
    chaoMatHienTai,
    anPopupChaoMat,
    docThuChaoMat,
    dieuKhienPtz,
    goiPtzPreset,
    datCheDoRiengTuCamera,
    confirmPrompt,
    requireConfirm,
    acceptConfirm,
    cancelConfirm,
    toggleQuickItem,
    thaoTacNhanhPhong,
    toggleRoomLight,
    toggleRoomFan,
    toggleRoomAc,
    stepAcTemp,
    setRoomMode,
    toggleRoomSchedule,
    setLightBrightness,
    setFanSpeed,
    toggleRoomTv,
    setCurtainOpen,
    toggleCurtain,
    setHoodSpeed,
    toggleStove,
    stepStoveLevel,
    setGasAlert,
    triggerTestGasAlert,
    dismissGasAlert,
    setHallView,
    toggleHallFrontLight,
    setHallFrontBrightness,
    toggleHallSurroundingLight,
    setHallSurroundingBrightness,
    toggleHallAllLights,
    toggleHallDoorLock,
    toggleHallCamera,
    toggleHallIrMode,
    toggleHallMotionAuto,
    triggerHallMotionTest,
    clearHallMotion,
    oThietBi,
    bamCongTac,
    datCongSac,
    datCaiDatCuktech,
    noiLaiCuktech,
    guiLenhWifi,
    khoiDongLaiWifi,
    toggleWifiSecurity,
    toggleWifiTrusted,
    toggleWifiBlocked,
    taiThoiTiet,
    taiDanhSachCamera,
    luuMqttConfig,
    luuCameraConfig,
    luuWeatherConfig,
    luuAquaControlUrl,
    bomNuoc,
    luongNuocThang,
    ketNoiBom,
    kenhBom,
    choBomNuoc,
    nhatKyBom,
    luuBomNuocConfig,
    datCheDoBon,
    datBomTangAp,
    datDenPhuBom,
    xoaLoiBom,
    tatCoiBom,
    thuCoiBom,
    luuBaoVeBom,
    ghiMocCanBom,
    ghiMocDayBom,
    datLaiDemLuuLuong,
    xoaLichSuNuocTrenBo,
    xoaNhatKyBomCucBo,
    voiceConfig,
    voiceUsage,
    phienGiongNoi,
    voiceSheetOpen,
    setVoiceSheetOpen,
    updateVoiceConfig,
    clearVoiceUsage,
    batTatGiongNoi,
    tatGiongNoi,
    ngatLoiGiongNoi,
    xoaLoiGiongNoi,
    updateCheck,
    checkGithubUpdate,
    screensaverConfig,
    updateScreensaverConfig,
    screensaverActive,
    enterScreensaver,
    wakeScreensaver,
    emergencyAlert,
    dismissEmergencyAlert,
    mqttGasAlert,
    mqttGasPpm,
    telegramStatus,
    refreshTelegramStatus,
    luuTelegramConfig,
    testTelegramAlert,
    adminLockEnabled,
    adminUnlocked,
    pinPrompt,
    requireAdminPin,
    submitAdminPin,
    cancelAdminPin,
    lockAdmin,
    updateAdminPin,
    dockerControlStatus,
    dockerBusyName,
    refreshDockerControlStatus,
    refreshDockerContainers,
    controlDockerContainer,
  };

  return <SmartHomeContext.Provider value={value}>{children}</SmartHomeContext.Provider>;
}

export function useSmartHome(): SmartHomeContextValue {
  const ctx = useContext(SmartHomeContext);
  if (!ctx) throw new Error('useSmartHome must be used inside SmartHomeProvider');
  return ctx;
}
