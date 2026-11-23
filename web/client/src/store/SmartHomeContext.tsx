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
  cuktechDanhSachCong,
  cuktechTongOat,
  gopCuktech,
  type CongSacId,
  type CuktechState,
} from '../data/cuktech.js';
import { DemoData } from '../data/demoData.js';
import {
  coSoLieuDien,
  createEmptyDienState,
  DienTopic,
  doTuoiDien,
  gopDien,
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
  type CameraFeed,
  type DestRoute,
  type HallwayView,
  type QuickItem,
  type RoomMode,
  type RoomUi,
} from '../data/models.js';
import { gopServer, ServerTopic, type ServerState } from '../data/server.js';
import { layThoiTiet, OpenMeteo, type ThoiTietUi } from '../data/thoitiet.js';
import { gopWifi, WifiTopic, type WifiState } from '../data/wifi.js';

export type TrangThaiCam =
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
  tls: boolean;
  taiKhoan: string;
  coMatKhau: boolean;
}

export interface WeatherConfig {
  ten: string;
  lat: number;
  lon: number;
}

export interface PublicAppConfig {
  mqtt: PublicMqttConfig;
  camera: PublicCameraConfig;
  weather: WeatherConfig;
  aquaControlUrl: string;
  bomNuoc: PublicBomNuocConfig;
}

export interface ToastMessage {
  id: string;
  text: string;
  tone: 'info' | 'success' | 'warning' | 'error';
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

export const APP_WEB_VERSION = '0.9.2-web';
export const ANDROID_BASE_VERSION = '0.9.2';

const GIAY_CHO_CUKTECH = 25;
const GIAY_CHO_NOI_LAI = 90;

const CHE_DO_HOP_LE: Record<string, RoomMode> = {
  SLEEP: 'SLEEP',
  NGU: 'SLEEP',
  NGỦ: 'SLEEP',
  HOME: 'HOME',
  NHA: 'HOME',
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

const TEN_THIET_BI_TUAN: Record<string, string> = {
  den: 'đèn',
  quat: 'quạt',
  dieu_hoa: 'điều hòa',
  tivi: 'tivi',
  rem: 'rèm',
  bep_tu: 'bếp từ',
};

function defaultPublicConfig(): PublicAppConfig {
  return {
    mqtt: { host: '', cong: 1883, tls: false, taiKhoan: '', clientId: '', coMatKhau: false },
    camera: { host: '', cong: 1984, tls: false, taiKhoan: '', coMatKhau: false },
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
  choCongTacIds: Record<string, boolean>;
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

  // Quick & Room Controls
  toggleQuickItem: (id: string) => void;
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
  setHoodSpeed: (roomId: string, speed: number) => void;
  toggleStove: (roomId: string) => void;
  stepStoveLevel: (roomId: string, delta: number) => void;
  triggerTestGasAlert: (roomId: string) => void;
  dismissGasAlert: (roomId: string) => void;
  setHallView: (roomId: string, view: HallwayView) => void;
  toggleHallFrontLight: (roomId: string) => void;
  setHallFrontBrightness: (roomId: string, pct: number) => void;
  toggleHallSurroundingLight: (roomId: string) => void;
  setHallSurroundingBrightness: (roomId: string, pct: number) => void;
  toggleHallDoorLock: (roomId: string) => void;
  toggleHallCamera: (roomId: string) => void;
  toggleHallIrMode: (roomId: string) => void;
  toggleHallMotionAuto: (roomId: string) => void;
  triggerHallMotionTest: (roomId: string) => void;

  // Real Hardware Controls
  oThietBi: (phong: string, thietBi: string, batMau: boolean) => OThietBi;
  bamCongTac: (phong: string, thietBi: string, guiTuGiong?: boolean) => void;
  datCongSac: (id: CongSacId, bat: boolean) => void;
  datCaiDatCuktech: (piid: number, giaTri: number) => void;
  noiLaiCuktech: () => void;
  guiLenhWifi: (cmd: Record<string, unknown>, okMsg?: string) => boolean;
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
  const [cameraStatus, setCameraStatus] = useState<TrangThaiCam>({ state: 'dang_do' });
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Telemetry / Hardware states
  const [dien, setDien] = useState<DienState>(createEmptyDienState);
  const [congtac, setCongtac] = useState<CongTacState>(createEmptyCongTacState);
  const [cuktech, setCuktech] = useState<CuktechState>(createEmptyCuktechState);
  const [server, setServer] = useState<ServerState>(() => DemoData.initialServerState());
  const [wifi, setWifi] = useState<WifiState>(() => DemoData.initialWifiState());
  const [thoiTiet, setThoiTiet] = useState<ThoiTietUi>({ state: 'dang_tai' });

  // Pending commands & deadlines
  const hangChoRef = useRef(new HangChoCongTac());
  const [choCongTacIds, setChoCongTacIds] = useState<Record<string, boolean>>({});
  const cuktechCongDeadlines = useRef<Partial<Record<CongSacId, number>>>({});
  const cuktechCaiDatDeadline = useRef<number>(0);
  const cuktechNoiLaiDeadline = useRef<number>(0);
  const [dangDoiCuktechCong, setDangDoiCuktechCong] = useState<Partial<Record<CongSacId, boolean>>>({});
  const [dangDoiCuktechCaiDat, setDangDoiCuktechCaiDat] = useState(false);
  const [dangNoiLaiCuktech, setDangNoiLaiCuktech] = useState(false);

  // Demo Room / UI states
  const [rooms, setRooms] = useState<RoomUi[]>(() => DemoData.rooms());
  const [selectedRoomId, setSelectedRoomId] = useState<string>('living');
  const [quickItems, setQuickItems] = useState<QuickItem[]>(() => DemoData.quickItems());
  const cameras = useMemo(() => DemoData.cameras(), []);

  // Voice assistant states
  const [voiceConfig, setVoiceConfig] = useState<CauHinhGiongNoi>(() => loadVoiceConfig());
  const [voiceUsage, setVoiceUsage] = useState<MucDungNgay[]>(() => loadVoiceUsage());
  const [phienGiongNoi, setPhienGiongNoi] = useState<PhienGiongNoi>(createEmptyPhienGiongNoi);
  const [voiceSheetOpen, setVoiceSheetOpen] = useState(false);

  // Update check
  const [updateCheck, setUpdateCheck] = useState<UpdateCheckState>({ status: 'idle' });

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

  // Refs synced to latest states (tranh stale closure cho WebSocket, timer & Gemini Live tool calls)
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

  // Navigation helpers
  const navigateDest = useCallback((route: DestRoute) => {
    setCurrentDest(route);
    setSelectedRoomDetailId(null);
    setChiTietDienOpen(false);
    setWifiOpen(false);
  }, []);

  const openRoomDetail = useCallback((roomId: string) => {
    setSelectedRoomId(roomId);
    setSelectedRoomDetailId(roomId);
    setChiTietDienOpen(false);
    setWifiOpen(false);
  }, []);

  const openChiTietDien = useCallback(() => {
    setChiTietDienOpen(true);
    setSelectedRoomDetailId(null);
    setWifiOpen(false);
  }, []);

  const openWifi = useCallback(() => {
    setWifiOpen(true);
    setSelectedRoomDetailId(null);
    setChiTietDienOpen(false);
  }, []);

  const closeSubScreen = useCallback(() => {
    setSelectedRoomDetailId(null);
    setChiTietDienOpen(false);
    setWifiOpen(false);
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

  // Nhan 1 ban tin MQTT (snapshot hoac live)
  const handleMqttMessage = useCallback(
    (topic: string, payload: string, isLive: boolean) => {
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
          if (topic === CuktechTopic.STATUS) {
            for (const c of cuktechDanhSachCong(next)) {
              const dl = cuktechCongDeadlines.current[c.id];
              if (dl && next.noiDuoc) {
                delete cuktechCongDeadlines.current[c.id];
              }
            }
            setDangDoiCuktechCong(() => {
              const out: Partial<Record<CongSacId, boolean>> = {};
              for (const k of Object.keys(cuktechCongDeadlines.current) as CongSacId[]) out[k] = true;
              return out;
            });
            if (cuktechNoiLaiDeadline.current > 0 && next.noiDuoc) {
              cuktechNoiLaiDeadline.current = 0;
              setDangNoiLaiCuktech(false);
            }
          }
          if (topic === CuktechTopic.SETTINGS && cuktechCaiDatDeadline.current > 0) {
            cuktechCaiDatDeadline.current = 0;
            setDangDoiCuktechCaiDat(false);
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
    [showToast, syncCongTacVaoRooms],
  );

  // Camera probe
  const taiDanhSachCamera = useCallback(async () => {
    setCameraStatus({ state: 'dang_do' });
    try {
      const resp = await fetch('/api/go2rtc/streams');
      const data = (await resp.json()) as { ok: boolean; streams?: string[]; error?: string };
      if (data.ok) {
        setCameraStatus({ state: 'co', ds: data.streams ?? [] });
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
            configRef.current = data;
            setConfig(data);
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
            | { type: 'message'; topic: string; payload: string; ts: number }
            | { type: 'publish_ack'; reqId: string; ok: boolean; reason?: string; error?: string };

          if (msg.type === 'snapshot') {
            setMqttStatus(msg.status);
            for (const [topic, payload] of msg.messages) {
              handleMqttMessage(topic, payload, false);
            }
          } else if (msg.type === 'status') {
            setMqttStatus(msg.status);
          } else if (msg.type === 'message') {
            handleMqttMessage(msg.topic, msg.payload, true);
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
        showToast('Công tắc không trả lời (hết 6s). Kiểm tra điện / Wi-Fi công tắc.', 'warning');
      }

      let congChanged = false;
      for (const key of Object.keys(cuktechCongDeadlines.current) as CongSacId[]) {
        const dl = cuktechCongDeadlines.current[key];
        if (dl && now >= dl) {
          delete cuktechCongDeadlines.current[key];
          congChanged = true;
          showToast('Sạc không phản hồi lệnh bật/tắt cổng. Kiểm tra kết nối BLE.', 'warning');
        }
      }
      if (congChanged) {
        setDangDoiCuktechCong(() => {
          const next: Partial<Record<CongSacId, boolean>> = {};
          for (const k of Object.keys(cuktechCongDeadlines.current) as CongSacId[]) next[k] = true;
          return next;
        });
      }

      if (cuktechCaiDatDeadline.current > 0 && now >= cuktechCaiDatDeadline.current) {
        cuktechCaiDatDeadline.current = 0;
        setDangDoiCuktechCaiDat(false);
        showToast('Sạc không phản hồi lệnh đổi cài đặt.', 'warning');
      }

      if (cuktechNoiLaiDeadline.current > 0 && now >= cuktechNoiLaiDeadline.current) {
        cuktechNoiLaiDeadline.current = 0;
        setDangNoiLaiCuktech(false);
        showToast('Không thấy phản hồi nối lại từ máy chủ.', 'warning');
      }
    }, 1000);

    return () => window.clearInterval(id);
  }, [showToast]);

  // Room updaters
  const updateRoom = useCallback((roomId: string, updater: (r: RoomUi) => RoomUi) => {
    setRooms((prev) => prev.map((r) => (r.id === roomId ? updater(r) : r)));
  }, []);

  // Cua duy nhat gui lenh toi cong tac that (hoac doi cuc bo neu la thiet bi mau)
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
        if (tuManHinh) showToast(loi, 'error');
        return loi;
      }
      if (!isBridgeSong(ct)) {
        const loi = 'Bộ nối công tắc trên máy chủ chưa chạy nên chưa gửi được lệnh.';
        if (tuManHinh) showToast(loi, 'warning');
        return loi;
      }

      const tt = ct.trangThai[tb.ma];
      if (!tt || !tt.online) {
        const loi = `${tb.ten} đang mất kết nối${tt?.lyDoLoi ? ` (${tt.lyDoLoi})` : ''}.`;
        if (tuManHinh) showToast(loi, 'warning');
        return loi;
      }
      if (dao && tt.bat === null) {
        const loi = `Chưa biết ${tb.ten} đang bật hay tắt nên không đảo được.`;
        if (tuManHinh) showToast(loi, 'warning');
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

  const toggleRoomLight = useCallback((id: string) => {
    gatCongTac(id, 'den', true, false, LenhCongTac.NGUON_MAN_HINH);
  }, [gatCongTac]);

  const toggleRoomFan = useCallback((id: string) => {
    gatCongTac(id, 'quat', true, false, LenhCongTac.NGUON_MAN_HINH);
  }, [gatCongTac]);

  const toggleRoomAc = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, acOn: !r.acOn }));
  }, [updateRoom]);

  const stepAcTemp = useCallback((id: string, delta: number) => {
    updateRoom(id, (r) => ({ ...r, acTemp: Math.min(30, Math.max(16, r.acTemp + delta)) }));
  }, [updateRoom]);

  const setRoomMode = useCallback((id: string, mode: RoomMode) => {
    const ct = congtacRef.current;
    const denThat = Boolean(
      timCongTac(ct, id, 'den') &&
      isBridgeSong(ct) &&
      trangThaiCongTacCua(ct, id, 'den')?.online === true,
    );
    const quatThat = Boolean(
      timCongTac(ct, id, 'quat') &&
      isBridgeSong(ct) &&
      trangThaiCongTacCua(ct, id, 'quat')?.online === true,
    );
    updateRoom(id, (r) => {
      switch (mode) {
        case 'SLEEP':
          return {
            ...r,
            mode,
            lightOn: denThat ? r.lightOn : false,
            fanOn: quatThat ? r.fanOn : true,
            acOn: true,
            acTemp: 27,
            tvOn: false,
            curtainOpen: 0,
            stoveOn: false,
            hoodSpeed: 1,
            hallFrontLightOn: false,
            hallSurroundingLightOn: false,
            hallDoorLocked: true,
            hallIrMode: true,
          };
        case 'AWAY':
          return {
            ...r,
            mode,
            lightOn: denThat ? r.lightOn : false,
            fanOn: quatThat ? r.fanOn : false,
            acOn: false,
            tvOn: false,
            stoveOn: false,
            hoodSpeed: 1,
            hallFrontLightOn: false,
            hallSurroundingLightOn: true,
            hallDoorLocked: true,
            hallCameraOn: true,
            hallMotionAuto: true,
          };
        case 'HOME':
          return {
            ...r,
            mode,
            lightOn: denThat ? r.lightOn : true,
            acOn: true,
            acTemp: 25,
            curtainOpen: 100,
            hallFrontLightOn: true,
            hallSurroundingLightOn: true,
            hallDoorLocked: true,
            hallIrMode: false,
          };
        default:
          return { ...r, mode };
      }
    });
  }, [updateRoom]);

  const toggleRoomSchedule = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, scheduleOn: !r.scheduleOn }));
  }, [updateRoom]);

  const setLightBrightness = useCallback((id: string, pct: number) => {
    updateRoom(id, (r) => ({ ...r, lightBrightness: Math.min(100, Math.max(10, Math.round(pct))) }));
  }, [updateRoom]);

  const setFanSpeed = useCallback((id: string, speed: number) => {
    updateRoom(id, (r) => ({ ...r, fanSpeed: Math.min(3, Math.max(1, Math.trunc(speed))), fanOn: true }));
    const ct = congtacRef.current;
    const tb = timCongTac(ct, id, 'quat');
    const tt = tb ? ct.trangThai[tb.ma] : undefined;
    if (tb && isBridgeSong(ct) && tt?.online === true && tt.bat !== true) {
      gatCongTac(id, 'quat', false, true, LenhCongTac.NGUON_MAN_HINH);
    }
  }, [gatCongTac, updateRoom]);

  const toggleRoomTv = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, tvOn: !r.tvOn }));
  }, [updateRoom]);

  const setCurtainOpen = useCallback((id: string, pct: number) => {
    updateRoom(id, (r) => ({ ...r, curtainOpen: Math.min(100, Math.max(0, Math.round(pct))) }));
  }, [updateRoom]);

  const setHoodSpeed = useCallback((id: string, speed: number) => {
    updateRoom(id, (r) => ({ ...r, hoodSpeed: Math.min(3, Math.max(1, Math.trunc(speed))), fanOn: true }));
  }, [updateRoom]);

  const toggleStove = useCallback((id: string) => {
    updateRoom(id, (r) => {
      const nextOn = !r.stoveOn;
      return nextOn && !r.fanOn
        ? { ...r, stoveOn: true, fanOn: true, hoodSpeed: 2 }
        : { ...r, stoveOn: nextOn };
    });
  }, [updateRoom]);

  const stepStoveLevel = useCallback((id: string, delta: number) => {
    updateRoom(id, (r) => ({ ...r, stoveLevel: Math.min(9, Math.max(1, r.stoveLevel + delta)) }));
  }, [updateRoom]);

  const triggerTestGasAlert = useCallback((id: string) => {
    updateRoom(id, (r) => {
      const next = !r.gasAlert;
      return { ...r, gasAlert: next, gasPpm: next ? 360 : 18, fanOn: next ? true : r.fanOn, hoodSpeed: next ? 3 : r.hoodSpeed };
    });
  }, [updateRoom]);

  const dismissGasAlert = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, gasAlert: false, gasPpm: 18 }));
  }, [updateRoom]);

  const setHallView = useCallback((id: string, view: HallwayView) => {
    updateRoom(id, (r) => ({ ...r, hallView: view }));
  }, [updateRoom]);

  const toggleHallFrontLight = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, hallFrontLightOn: !r.hallFrontLightOn }));
  }, [updateRoom]);

  const setHallFrontBrightness = useCallback((id: string, pct: number) => {
    updateRoom(id, (r) => ({ ...r, hallFrontBrightness: Math.min(100, Math.max(10, Math.round(pct))) }));
  }, [updateRoom]);

  const toggleHallSurroundingLight = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, hallSurroundingLightOn: !r.hallSurroundingLightOn }));
  }, [updateRoom]);

  const setHallSurroundingBrightness = useCallback((id: string, pct: number) => {
    updateRoom(id, (r) => ({ ...r, hallSurroundingBrightness: Math.min(100, Math.max(10, Math.round(pct))) }));
  }, [updateRoom]);

  const toggleHallDoorLock = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, hallDoorLocked: !r.hallDoorLocked }));
  }, [updateRoom]);

  const toggleHallCamera = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, hallCameraOn: !r.hallCameraOn }));
  }, [updateRoom]);

  const toggleHallIrMode = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, hallIrMode: !r.hallIrMode }));
  }, [updateRoom]);

  const toggleHallMotionAuto = useCallback((id: string) => {
    updateRoom(id, (r) => ({ ...r, hallMotionAuto: !r.hallMotionAuto }));
  }, [updateRoom]);

  const triggerHallMotionTest = useCallback((id: string) => {
    updateRoom(id, (r) => {
      const next = !r.hallMotionDetected;
      return {
        ...r,
        hallMotionDetected: next,
        hallFrontLightOn: next && r.hallMotionAuto ? true : r.hallFrontLightOn,
        hallSurroundingLightOn: next && r.hallMotionAuto ? true : r.hallSurroundingLightOn,
      };
    });
  }, [updateRoom]);

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

      cuktechCongDeadlines.current[id] = Date.now() + GIAY_CHO_CUKTECH * 1000;
      setDangDoiCuktechCong((prev) => ({ ...prev, [id]: true }));

      void publishMqtt(
        CuktechTopic.CMD_CONG,
        JSON.stringify({ port: meta.topic, action: bat ? 'on' : 'off' }),
      ).then((ack) => {
        if (!ack.ok) {
          delete cuktechCongDeadlines.current[id];
          setDangDoiCuktechCong((prev) => {
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
      cuktechCaiDatDeadline.current = Date.now() + GIAY_CHO_CUKTECH * 1000;
      setDangDoiCuktechCaiDat(true);
      void publishMqtt(CuktechTopic.CMD_SET, JSON.stringify({ piid, value: giaTri })).then((ack) => {
        if (!ack.ok) {
          cuktechCaiDatDeadline.current = 0;
          setDangDoiCuktechCaiDat(false);
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

  // WiFi actions
  const guiLenhWifi = useCallback(
    (cmd: Record<string, unknown>, okMsg?: string): boolean => {
      if (mqttStatusRef.current.state !== 'da_noi') {
        showToast('Cần nối MQTT để gửi lệnh tới Router.', 'warning');
        return false;
      }
      void publishMqtt(WifiTopic.CMD, JSON.stringify(cmd)).then((ack) => {
        if (!ack.ok) showToast(`Lỗi gửi lệnh WiFi: ${ack.error || ack.reason}`, 'error');
        else if (okMsg) showToast(okMsg, 'success');
      });
      return true;
    },
    [publishMqtt, showToast],
  );

  const toggleWifiSecurity = useCallback(
    (key: string) => {
      setWifi((prev) => ({
        ...prev,
        securitySettings: prev.securitySettings.map((s) => (s.key === key ? { ...s, enabled: !s.enabled } : s)),
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
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(form),
        });
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
    [showToast],
  );

  const luuCameraConfig = useCallback(
    async (form: {
      host: string;
      cong: number;
      tls: boolean;
      taiKhoan: string;
      matKhau?: string;
      xoaMatKhau?: boolean;
    }) => {
      setCameraStatus({ state: 'dang_do' });
      try {
        const resp = await fetch('/api/config/camera', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(form),
        });
        const data = (await resp.json()) as {
          ok: boolean;
          config: PublicAppConfig;
          probe: { ok: boolean; streams?: string[]; error?: string };
        };
        if (data.ok) {
          configRef.current = data.config;
          setConfig(data.config);
          if (data.probe.ok) {
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
    [showToast],
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
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ aquaControlUrl }),
        });
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
    [showToast],
  );

  const luuBomNuocConfig = useCallback(
    async (form: BomNuocConfigForm) => {
      try {
        const resp = await fetch('/api/config/bomnuoc', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(form),
        });
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
    [showToast],
  );

  const guiLenhBom = useCallback(
    async (khoa: string, cmd: BomNuocCmd): Promise<void> => {
      setLocalPendingBom((prev) => ({ ...prev, [khoa]: true }));
      try {
        const resp = await fetch('/api/bomnuoc/cmd', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(cmd),
        });
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
    [showToast],
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

  const xoaLoiBom = useCallback(
    () => guiLenhBom('clear', { kind: 'clear' }),
    [guiLenhBom],
  );

  const tatCoiBom = useCallback(
    () => guiLenhBom('coiTat', { kind: 'coiTat' }),
    [guiLenhBom],
  );

  const thuCoiBom = useCallback(
    () => guiLenhBom('coiThu', { kind: 'coiThu' }),
    [guiLenhBom],
  );

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
      const resp = await fetch('/api/bomnuoc/log', { method: 'DELETE' });
      if (resp.ok) {
        setNhatKyBom([]);
        showToast('Đã xoá nhật ký bơm trên gateway.', 'info');
        return;
      }
    } catch {
      // ignore
    }
    showToast('Không xoá được nhật ký bơm.', 'error');
  }, [showToast]);

  // Voice Actions + TroLyGiongNoi
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
        const ct = congtacRef.current;
        if (timCongTac(ct, phong, thietBi)) {
          return trangThaiCongTacCua(ct, phong, thietBi)?.bat ?? null;
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
          case CongCuNha.BOM_CHINH: {
            let mode: 'auto' | 'on' | 'off';
            if (hanhDong === CongCuNha.BAT) mode = 'on';
            else if (hanhDong === CongCuNha.TAT) mode = 'off';
            else if (hanhDong === CongCuNha.DAO) mode = bomChinhDangChay(s) ? 'off' : 'on';
            else if (hanhDong === CongCuNha.DAT) {
              const g = giaTri?.trim().toLowerCase() ?? '';
              if (g === 'auto' || g === 'tu_dong' || g === 'tự động') mode = 'auto';
              else if (g === 'on' || g === 'bat' || g === 'bật') mode = 'on';
              else if (g === 'off' || g === 'tat' || g === 'tắt') mode = 'off';
              else throw new LoiLenh('Bơm lên bồn chỉ nhận chế độ tự động (auto), bật hoặc tắt.');
            } else {
              throw new LoiLenh('Không rõ lệnh cho bơm lên bồn.');
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
              phong: roomsRef.current.map((r) => ({
                ma: r.id,
                ten: r.name,
                che_do: r.mode,
                den: r.lightOn,
                quat: r.fanOn,
                dieu_hoa: r.acOn,
                nhiet_do_dat: r.acTemp,
              })),
            };
          case 'dien':
            return {
              co_so: coSoLieuDien(d),
              ket_noi: m.state === 'da_noi' ? 'đã nối' : m.state,
              cap_nhat: d.capNhat ?? 'chưa có',
              hom_nay_kwh: d.ngaySanLuong,
              thang_kwh: d.thangSanLuong,
              tien_thang_vnd: d.thangTien,
              chi_so_cong_to: d.congToChiSo,
              do_tuoi: doTuoiDien(d),
            };
          case 'server':
            return {
              co_so: s.isRealData,
              hostname: s.info.hostname,
              cpu_phan_tram: s.metric.cpuPercent,
              ram_phan_tram: s.metric.ramPercent,
              nhiet_do_c: s.metric.cpuTempC,
              du_lieu: s.isRealData ? 'thật' : 'mẫu',
            };
          case 'wifi':
            return {
              co_so: w.isRealData,
              ssid: w.info.ssid,
              so_thiet_bi_online: w.devices.filter((x) => x.isOnline).length,
              download_mbps: w.info.downloadMbps,
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
            const am = AmLich.tuDuongLich(now.getDate(), now.getMonth() + 1, now.getFullYear());
            const cc = AmLich.canChi(am);
            const suKien = AmLich.suKien(now.getDate(), now.getMonth() + 1, am);
            return {
              co_so: true,
              ngay_am: `${am.ngay}/${am.thang}${am.nhuan ? ' (nhuận)' : ''}/${am.nam}`,
              can_chi: `Ngày ${cc.ngay}, tháng ${cc.thang}, năm ${cc.nam}`,
              su_kien: suKien,
            };
          }
          case 'cong_tac':
            return {
              co_so: ct.danhSach.length > 0,
              bridge: ct.bridge ?? 'chưa rõ',
              thiet_bi: ct.danhSach.map((tb) => {
                const st = ct.trangThai[tb.ma];
                return { ten: tb.ten, phong: tb.phong, online: st?.online ?? false, bat: st?.bat ?? null };
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
        const lines = roomsRef.current.map(
          (r) => `${r.name} (${r.id}): đèn=${r.lightOn ? 'bật' : 'tắt'}, quạt=${r.fanOn ? 'bật' : 'tắt'}, điều hòa=${r.acOn ? `${r.acTemp}°C` : 'tắt'}`,
        );
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
    [updateRoom, setRoomMode, openRoomDetail, navigateDest, openChiTietDien, openWifi, bamCongTac, gatCongTac, publishMqtt, datCheDoBon, datBomTangAp, datDenPhuBom, xoaLoiBom, tatCoiBom, thuCoiBom],
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
    setVoiceConfig(c);
    saveVoiceConfig(c);
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
      if (tag === ANDROID_BASE_VERSION) {
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

  // Also add helper for voice text logging? No, unused TEN_THIET_BI_TUAN warning avoidance
  void TEN_THIET_BI_TUAN;
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
    choCongTacIds,
    dangDoiCuktechCong,
    dangDoiCuktechCaiDat,
    dangNoiLaiCuktech,
    rooms,
    selectedRoomId,
    selectedRoom,
    selectRoom: setSelectedRoomId,
    quickItems,
    cameras,
    toggleQuickItem,
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
    setHoodSpeed,
    toggleStove,
    stepStoveLevel,
    triggerTestGasAlert,
    dismissGasAlert,
    setHallView,
    toggleHallFrontLight,
    setHallFrontBrightness,
    toggleHallSurroundingLight,
    setHallSurroundingBrightness,
    toggleHallDoorLock,
    toggleHallCamera,
    toggleHallIrMode,
    toggleHallMotionAuto,
    triggerHallMotionTest,
    oThietBi,
    bamCongTac,
    datCongSac,
    datCaiDatCuktech,
    noiLaiCuktech,
    guiLenhWifi,
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
  };

  return <SmartHomeContext.Provider value={value}>{children}</SmartHomeContext.Provider>;
}

export function useSmartHome(): SmartHomeContextValue {
  const ctx = useContext(SmartHomeContext);
  if (!ctx) throw new Error('useSmartHome must be used inside SmartHomeProvider');
  return ctx;
}

