/**
 * Hợp đồng topic MQTT & HTTP Playback API của dịch vụ `scripts/camera_recorder/` trên serverubuntu.
 * Port 1:1 từ `CameraRecorderModels.kt` & `CameraPlaybackApi.kt` (Android v0.9.4).
 */

export const CameraTopic = {
  GOC: 'camera/#',
  TRANG_THAI: 'camera/trangthai',
  STATE: 'camera/state',
  PERSON: 'camera/person',
  FACE_GREET: 'camera/face/greet',
  CMD: 'camera/cmd',
  KET_QUA: 'camera/cmd/result',
} as const;

export interface PtzPreset {
  id: string;
  label: string;
  token: string;
}

export const PTZ_PRESETS_MAC_DINH: PtzPreset[] = [
  { id: 'cua_chinh', label: 'Cửa chính', token: '1' },
  { id: 'ban_an', label: 'Bàn ăn', token: '2' },
  { id: 'cau_thang', label: 'Cầu thang', token: '3' },
  { id: 'toan_canh', label: 'Toàn cảnh', token: '4' },
];

export interface NguoiQuenMat {
  name: string;
  count: number;
  sampleUrl: string | null;
  updatedTs: number;
}

export interface AnhNguoiLa {
  filename: string;
  url: string;
  ts: number;
}

export interface FaceGreetEvent {
  name: string;
  isKnown: boolean;
  greeting: string;
  camera: string;
  confidence: number;
  snapshotUrl: string;
  ts: number;
  mode: string;
  silent: boolean;
}

export function parseFaceGreetEvent(payload: string): FaceGreetEvent | null {
  const txt = payload.trim();
  if (!txt) return null;
  try {
    const o = JSON.parse(txt) as Record<string, unknown>;
    if (!o || typeof o !== 'object') return null;
    const name = typeof o.name === 'string' ? o.name.trim() : '';
    if (!name) return null;
    const isKnown =
      typeof o.is_known === 'boolean' ? o.is_known : name.toLowerCase() !== 'unknown';
    const rawGreet = typeof o.greeting === 'string' ? o.greeting.trim() : '';
    const greeting = rawGreet || (isKnown ? `Chào mừng ${name} về nhà!` : 'Có khách ở cửa');
    return {
      name,
      isKnown,
      greeting,
      camera: typeof o.camera === 'string' ? o.camera : '',
      confidence: typeof o.confidence === 'number' && Number.isFinite(o.confidence) ? o.confidence : 0,
      snapshotUrl: typeof o.snapshot_url === 'string' ? o.snapshot_url : '',
      ts: typeof o.ts === 'number' && Number.isFinite(o.ts) ? Math.trunc(o.ts) : 0,
      mode: typeof o.mode === 'string' && o.mode.trim() ? o.mode.trim() : 'home',
      silent: Boolean(o.silent),
    };
  } catch {
    return null;
  }
}

export function isFaceGreetFresh(ev: FaceGreetEvent, nowEpochSec = Math.floor(Date.now() / 1000), maxAgeSec = 30): boolean {
  return ev.ts > 0 && Math.abs(nowEpochSec - ev.ts) < maxAgeSec;
}

const LECH_VN_GIAY = 7 * 3600;

function pad2(n: number): string {
  return String(Math.trunc(n)).padStart(2, '0');
}

/** Định dạng giây Unix sang `HH:mm:ss` theo múi giờ Việt Nam (`Asia/Ho_Chi_Minh`, UTC+7). */
export function dinhDangGioVn(epochSec: number): string {
  const d = new Date((Math.trunc(epochSec) + LECH_VN_GIAY) * 1000);
  return `${pad2(d.getUTCHours())}:${pad2(d.getUTCMinutes())}:${pad2(d.getUTCSeconds())}`;
}

/** Ngày hôm nay theo múi giờ Việt Nam dạng `YYYY-MM-DD`. */
export function homNayVnStr(nowMs = Date.now()): string {
  const d = new Date(nowMs + LECH_VN_GIAY * 1000);
  return `${d.getUTCFullYear()}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())}`;
}

/** Cộng/trừ ngày từ chuỗi `YYYY-MM-DD`, trả về `YYYY-MM-DD`. */
export function congTruNgayVn(ngayStr: string, soNgay: number): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ngayStr.trim());
  if (!m) return ngayStr;
  const d = new Date(Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]) + soNgay));
  return `${d.getUTCFullYear()}-${pad2(d.getUTCMonth() + 1)}-${pad2(d.getUTCDate())}`;
}

export interface TrangThaiGhiTungCam {
  ten: string;
  dangGhi: boolean;
  lastSegmentTs: number | null;
  loi: string | null;
}

export interface SuKienNguoiGanNhat {
  eventId: number;
  camera: string;
  startTs: number;
  thoiGian: string;
  soNguoi: number;
  doTinCay: number;
}

export interface KetQuaLenhCam {
  traLoiId: string | null;
  action: string;
  ok: boolean;
  message: string;
}

export interface CameraRecorderState {
  online: boolean | null;
  recordingEnabled: boolean | null;
  detectPersonEnabled: boolean | null;
  faceRecognitionEnabled: boolean | null;
  privacyMode: boolean;
  homeMode: string;
  ptzCameras: string[];
  indoorCameras: string[];
  telegramEnabled: boolean;
  driveEnabled: boolean;
  apiPort: number | null;
  cacheUsedGb: number;
  cacheMaxGb: number;
  pendingUploads: number;
  uploadedToday: number;
  uploadedTodayMb: number;
  personToday: number;
  driveError: string | null;
  cameras: Record<string, TrangThaiGhiTungCam>;
  capNhat: string | null;
  traLoiId: string | null;
  nguoiGanNhat: SuKienNguoiGanNhat | null;
  ketQuaLenh: KetQuaLenhCam | null;
}

export function createEmptyCameraRecorderState(): CameraRecorderState {
  return {
    online: null,
    recordingEnabled: null,
    detectPersonEnabled: null,
    faceRecognitionEnabled: null,
    privacyMode: false,
    homeMode: 'home',
    ptzCameras: [],
    indoorCameras: [],
    telegramEnabled: false,
    driveEnabled: false,
    apiPort: null,
    cacheUsedGb: 0,
    cacheMaxGb: 10,
    pendingUploads: 0,
    uploadedToday: 0,
    uploadedTodayMb: 0,
    personToday: 0,
    driveError: null,
    cameras: {},
    capNhat: null,
    traLoiId: null,
    nguoiGanNhat: null,
    ketQuaLenh: null,
  };
}

export function isRecorderSong(rec: CameraRecorderState): boolean {
  return rec.online === true;
}

export function camBiAnRiengTu(rec: CameraRecorderState, tenLuong?: string | null): boolean {
  if (!rec.privacyMode) return false;
  if (rec.indoorCameras.length === 0) return true;
  if (!tenLuong) return true;
  const goc = tenLuong.replace(/_sd$/i, '');
  return rec.indoorCameras.includes(goc);
}

export function camDangGhi(rec: CameraRecorderState, tenLuong?: string | null): boolean {
  if (!isRecorderSong(rec) || rec.recordingEnabled !== true) return false;
  if (camBiAnRiengTu(rec, tenLuong)) return false;
  if (!tenLuong) return true;
  return rec.cameras[tenLuong]?.dangGhi === true;
}

function cleanStrOrNull(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const t = v.trim();
  return t && t !== 'null' ? t : null;
}

export function gopCameraRecorder(
  cur: CameraRecorderState,
  topic: string,
  payload: string,
): CameraRecorderState {
  const txt = payload.trim();
  if (!txt) return cur;

  if (topic === CameraTopic.TRANG_THAI) {
    return {
      ...cur,
      online: txt.toLowerCase() === 'online',
    };
  }

  try {
    const o = JSON.parse(txt) as Record<string, unknown>;
    if (!o || typeof o !== 'object') return cur;

    if (topic === CameraTopic.STATE) {
      const camsMap: Record<string, TrangThaiGhiTungCam> = {};
      const rawCams = o.cameras;
      if (rawCams && typeof rawCams === 'object') {
        for (const [k, rawItem] of Object.entries(rawCams as Record<string, unknown>)) {
          if (!rawItem || typeof rawItem !== 'object') continue;
          const item = rawItem as Record<string, unknown>;
          camsMap[k] = {
            ten: k,
            dangGhi: Boolean(item.recording),
            lastSegmentTs:
              typeof item.last_segment_ts === 'number' && Number.isFinite(item.last_segment_ts)
                ? Math.trunc(item.last_segment_ts)
                : null,
            loi: cleanStrOrNull(item.error),
          };
        }
      }
      const rawPort = typeof o.api_port === 'number' ? Math.trunc(o.api_port) : 0;
      const ptzList = Array.isArray(o.ptz_cameras)
        ? o.ptz_cameras.filter((x): x is string => typeof x === 'string' && x.trim().length > 0).map((x) => x.trim())
        : null;
      const indoorList = Array.isArray(o.indoor_cameras)
        ? o.indoor_cameras.filter((x): x is string => typeof x === 'string' && x.trim().length > 0).map((x) => x.trim())
        : null;
      return {
        ...cur,
        online: cur.online ?? true,
        recordingEnabled: typeof o.recording_enabled === 'boolean' ? o.recording_enabled : true,
        detectPersonEnabled: typeof o.detect_person_enabled === 'boolean' ? o.detect_person_enabled : true,
        faceRecognitionEnabled:
          typeof o.face_recognition_enabled === 'boolean'
            ? o.face_recognition_enabled
            : cur.faceRecognitionEnabled,
        privacyMode: typeof o.privacy_mode === 'boolean' ? o.privacy_mode : cur.privacyMode,
        homeMode: typeof o.home_mode === 'string' && o.home_mode.trim() ? o.home_mode.trim() : cur.homeMode,
        ptzCameras: ptzList !== null ? ptzList : cur.ptzCameras,
        indoorCameras: indoorList !== null ? indoorList : cur.indoorCameras,
        telegramEnabled: Boolean(o.telegram_enabled),
        driveEnabled: Boolean(o.drive_enabled),
        apiPort: rawPort >= 1 && rawPort <= 65535 ? rawPort : null,
        cacheUsedGb: typeof o.cache_used_gb === 'number' && Number.isFinite(o.cache_used_gb) ? o.cache_used_gb : 0,
        cacheMaxGb: typeof o.cache_max_gb === 'number' && Number.isFinite(o.cache_max_gb) ? o.cache_max_gb : 10,
        pendingUploads: typeof o.pending_uploads === 'number' ? Math.trunc(o.pending_uploads) : 0,
        uploadedToday: typeof o.uploaded_today === 'number' ? Math.trunc(o.uploaded_today) : 0,
        uploadedTodayMb: typeof o.uploaded_today_mb === 'number' ? o.uploaded_today_mb : 0,
        personToday: typeof o.person_today === 'number' ? Math.trunc(o.person_today) : 0,
        driveError: cleanStrOrNull(o.drive_error),
        cameras: camsMap,
        capNhat: cleanStrOrNull(o.cap_nhat),
        traLoiId: cleanStrOrNull(o.tra_loi_id),
      };
    }

    if (topic === CameraTopic.PERSON) {
      return {
        ...cur,
        nguoiGanNhat: {
          eventId: typeof o.event_id === 'number' ? Math.trunc(o.event_id) : 0,
          camera: typeof o.camera === 'string' ? o.camera : '',
          startTs: typeof o.start_ts === 'number' ? Math.trunc(o.start_ts) : 0,
          thoiGian: typeof o.thoi_gian === 'string' ? o.thoi_gian : '',
          soNguoi: typeof o.person_count === 'number' ? Math.max(1, Math.trunc(o.person_count)) : 1,
          doTinCay: typeof o.confidence === 'number' ? o.confidence : 0,
        },
      };
    }

    if (topic === CameraTopic.KET_QUA) {
      return {
        ...cur,
        ketQuaLenh: {
          traLoiId: cleanStrOrNull(o.tra_loi_id),
          action: typeof o.action === 'string' ? o.action : '',
          ok: Boolean(o.ok),
          message: typeof o.message === 'string' ? o.message : '',
        },
      };
    }

    return cur;
  } catch {
    return cur;
  }
}

// ------------------------------------------------ Mô hình Xem lại Timeline

export interface NgayCoVideo {
  ngay: string;
  soDoan: number;
  tongGiay: number;
  soSuKienNguoi: number;
}

export interface DoanVideo {
  id: number;
  startTs: number;
  endTs: number;
  durationSec: number;
  sizeBytes: number;
  /** `'hot'` = nằm sẵn trên ổ serverubuntu (<0.5s); `'drive'` = kéo từ Google Drive 5TB. */
  tier: string;
}

export function isDoanCucBo(seg: DoanVideo): boolean {
  return seg.tier === 'hot';
}

export interface SuKienCamera {
  id: number;
  kind: string;
  startTs: number;
  endTs: number;
  maxConf: number;
  personCount: number;
  hasThumb: boolean;
}

export function isSuKienNguoi(ev: SuKienCamera): boolean {
  return ev.kind === 'person';
}

export function thoiLuongSuKienGiay(ev: SuKienCamera): number {
  return Math.max(1, Math.trunc(ev.endTs - ev.startTs));
}

export interface ViTriPhat {
  segment: DoanVideo;
  offsetMs: number;
  targetEpochSec: number;
}

export interface DongThoiGianNgay {
  camera: string;
  ngay: string;
  dayStartTs: number;
  dayEndTs: number;
  segments: DoanVideo[];
  events: SuKienCamera[];
}

export function locSuKienNguoi(tl: DongThoiGianNgay | null | undefined): SuKienCamera[] {
  if (!tl) return [];
  return tl.events.filter(isSuKienNguoi);
}

/**
 * Tìm đoạn video chứa mốc `epochSec`; nếu rơi vào khoảng nghỉ ngắn giữa 2 file
 * thì nhảy tới đầu đoạn tiếp theo gần nhất (hoặc cuối đoạn trước nếu ở cuối ngày).
 */
export function timViTriPhat(tl: DongThoiGianNgay, epochSec: number): ViTriPhat | null {
  if (tl.segments.length === 0) return null;
  const trung = tl.segments.find((s) => epochSec >= s.startTs && epochSec < s.endTs);
  if (trung) {
    const offsetMs = Math.max(0, (epochSec - trung.startTs) * 1000);
    return { segment: trung, offsetMs, targetEpochSec: epochSec };
  }
  const keTiep = tl.segments.find((s) => s.startTs >= epochSec);
  if (keTiep) {
    return { segment: keTiep, offsetMs: 0, targetEpochSec: keTiep.startTs };
  }
  const cuoi = tl.segments[tl.segments.length - 1];
  const offCuoi = Math.max(0, (cuoi.endTs - cuoi.startTs - 2) * 1000);
  return {
    segment: cuoi,
    offsetMs: offCuoi,
    targetEpochSec: Math.max(cuoi.startTs, cuoi.endTs - 2),
  };
}

/** Mục tiêu nhảy khi người dùng bấm vào 1 sự kiện có người: lùi trước 3 giây để thấy lúc bước vào. */
export function mocNhaySuKien(tl: DongThoiGianNgay, ev: SuKienCamera): number {
  return Math.max(tl.dayStartTs, ev.startTs - 3);
}

export function doanTiepTheo(tl: DongThoiGianNgay, sauSegId: number): DoanVideo | null {
  const idx = tl.segments.findIndex((s) => s.id === sauSegId);
  return idx >= 0 && idx + 1 < tl.segments.length ? tl.segments[idx + 1] : null;
}

export function suKienNguoiTruoc(tl: DongThoiGianNgay, hienTaiTs: number): SuKienCamera | null {
  const ds = locSuKienNguoi(tl);
  for (let i = ds.length - 1; i >= 0; i--) {
    if (ds[i].startTs < hienTaiTs - 4) return ds[i];
  }
  return null;
}

export function suKienNguoiTiep(tl: DongThoiGianNgay, hienTaiTs: number): SuKienCamera | null {
  const ds = locSuKienNguoi(tl);
  return ds.find((ev) => ev.startTs > hienTaiTs + 2) ?? null;
}

// ------------------------------------------------ HTTP Client qua Web Gateway

function portParam(apiPort?: number | null): string {
  return typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535 ? `port=${apiPort}` : '';
}

export const CameraPlaybackClient = {
  urlVideoSegment(segId: number, apiPort?: number | null): string {
    const q = portParam(apiPort);
    return `/api/camera/recorder/video/${segId}.mp4${q ? `?${q}` : ''}`;
  },

  urlThumbnail(eventId: number, apiPort?: number | null): string {
    const q = portParam(apiPort);
    return `/api/camera/recorder/thumb/${eventId}.jpg${q ? `?${q}` : ''}`;
  },

  async danhSachNgay(cam: string, apiPort?: number | null): Promise<NgayCoVideo[]> {
    const qs = new URLSearchParams({ cam });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) {
      qs.set('port', String(apiPort));
    }
    try {
      const resp = await fetch(`/api/camera/recorder/days?${qs.toString()}`);
      if (!resp.ok) return [];
      const json = (await resp.json()) as { days?: Array<Record<string, unknown>> };
      if (!Array.isArray(json.days)) return [];
      return json.days
        .map((d) => ({
          ngay: typeof d.date === 'string' ? d.date : '',
          soDoan: typeof d.segment_count === 'number' ? Math.trunc(d.segment_count) : 0,
          tongGiay: typeof d.recorded_sec === 'number' ? Math.trunc(d.recorded_sec) : 0,
          soSuKienNguoi: typeof d.person_events === 'number' ? Math.trunc(d.person_events) : 0,
        }))
        .filter((d) => Boolean(d.ngay));
    } catch {
      return [];
    }
  },

  async taiDongThoiGian(
    cam: string,
    ngay: string,
    apiPort?: number | null,
  ): Promise<{ ok: true; data: DongThoiGianNgay } | { ok: false; error: string }> {
    const qs = new URLSearchParams({ cam, date: ngay });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) {
      qs.set('port', String(apiPort));
    }
    try {
      const resp = await fetch(`/api/camera/recorder/timeline?${qs.toString()}`);
      const json = (await resp.json()) as Record<string, unknown>;
      if (!resp.ok || json.error) {
        return {
          ok: false,
          error:
            typeof json.error === 'string' && json.error
              ? json.error
              : `Đầu ghi trả mã ${resp.status}.`,
        };
      }
      const rawSegs = Array.isArray(json.segments) ? json.segments : [];
      const rawEvents = Array.isArray(json.events) ? json.events : [];
      const segments: DoanVideo[] = rawSegs
        .map((item) => {
          const s = item as Record<string, unknown>;
          return {
            id: typeof s.id === 'number' ? Math.trunc(s.id) : 0,
            startTs: typeof s.start_ts === 'number' ? Math.trunc(s.start_ts) : 0,
            endTs: typeof s.end_ts === 'number' ? Math.trunc(s.end_ts) : 0,
            durationSec: typeof s.duration_sec === 'number' ? s.duration_sec : 0,
            sizeBytes: typeof s.size_bytes === 'number' ? Math.trunc(s.size_bytes) : 0,
            tier: typeof s.tier === 'string' ? s.tier : 'hot',
          };
        })
        .filter((s) => s.id > 0 && s.endTs > s.startTs);

      const events: SuKienCamera[] = rawEvents
        .map((item) => {
          const e = item as Record<string, unknown>;
          const startTs = typeof e.start_ts === 'number' ? Math.trunc(e.start_ts) : 0;
          const endTs = typeof e.end_ts === 'number' ? Math.trunc(e.end_ts) : startTs;
          return {
            id: typeof e.id === 'number' ? Math.trunc(e.id) : 0,
            kind: typeof e.kind === 'string' ? e.kind : 'person',
            startTs,
            endTs: Math.max(startTs, endTs),
            maxConf: typeof e.max_conf === 'number' ? e.max_conf : 0,
            personCount: typeof e.person_count === 'number' ? Math.max(1, Math.trunc(e.person_count)) : 1,
            hasThumb: Boolean(e.has_thumb),
          };
        })
        .filter((e) => e.id > 0 && e.startTs > 0);

      const dayStartTs = typeof json.day_start_ts === 'number' ? Math.trunc(json.day_start_ts) : 0;
      const dayEndTs =
        typeof json.day_end_ts === 'number' ? Math.trunc(json.day_end_ts) : dayStartTs + 86400;

      return {
        ok: true,
        data: {
          camera: typeof json.camera === 'string' ? json.camera : cam,
          ngay: typeof json.date === 'string' ? json.date : ngay,
          dayStartTs,
          dayEndTs,
          segments,
          events,
        },
      };
    } catch (err) {
      return {
        ok: false,
        error: err instanceof Error ? err.message : 'Không kết nối được dịch vụ đầu ghi camera.',
      };
    }
  },

  async chupAnhNgay(
    cam: string,
    apiPort?: number | null,
  ): Promise<{ ok: boolean; eventId?: number; message: string }> {
    const qs = new URLSearchParams({ cam });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) {
      qs.set('port', String(apiPort));
    }
    try {
      const resp = await fetch(`/api/camera/recorder/snapshot?${qs.toString()}`, {
        method: 'POST',
      });
      const json = (await resp.json()) as Record<string, unknown>;
      if (resp.ok && json.ok) {
        return {
          ok: true,
          eventId: typeof json.event_id === 'number' ? Math.trunc(json.event_id) : undefined,
          message:
            typeof json.message === 'string' && json.message
              ? json.message
              : `Đã chụp ảnh "${cam}" (lưu Drive & Telegram).`,
        };
      }
      return {
        ok: false,
        message:
          typeof json.message === 'string' && json.message
            ? json.message
            : typeof json.error === 'string' && json.error
              ? json.error
              : `Không chụp được ảnh (mã ${resp.status}).`,
      };
    } catch (err) {
      return {
        ok: false,
        message: err instanceof Error ? err.message : 'Không gọi được đầu ghi để chụp ảnh.',
      };
    }
  },

  urlAnhMat(rawUrl: string, apiPort?: number | null): string {
    const s = rawUrl.trim();
    if (!s) return '';
    if (s.startsWith('http://') || s.startsWith('https://')) return s;
    const sub = s.startsWith('/api/') ? s.slice('/api'.length) : s.startsWith('/') ? s : `/${s}`;
    const portSuffix =
      typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535
        ? `${sub.includes('?') ? '&' : '?'}port=${apiPort}`
        : '';
    return `/api/camera/recorder${sub}${portSuffix}`;
  },

  urlTts(text: string, apiPort?: number | null): string {
    const qs = new URLSearchParams({ text });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) {
      qs.set('port', String(apiPort));
    }
    return `/api/camera/recorder/tts?${qs.toString()}`;
  },

  async taiDanhSachNguoiQuen(apiPort?: number | null): Promise<NguoiQuenMat[]> {
    const qs = new URLSearchParams();
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    const q = qs.toString();
    try {
      const resp = await fetch(`/api/camera/recorder/face/people${q ? `?${q}` : ''}`, {
        credentials: 'same-origin',
      });
      if (!resp.ok) return [];
      const json = (await resp.json()) as { people?: Array<Record<string, unknown>> };
      if (!Array.isArray(json.people)) return [];
      return json.people
        .map((o) => ({
          name: typeof o.name === 'string' ? o.name.trim() : '',
          count: typeof o.count === 'number' ? Math.trunc(o.count) : 0,
          sampleUrl: typeof o.sample_url === 'string' && o.sample_url.trim() ? o.sample_url.trim() : null,
          updatedTs: typeof o.updated_ts === 'number' ? Math.trunc(o.updated_ts) : 0,
        }))
        .filter((x) => Boolean(x.name));
    } catch {
      return [];
    }
  },

  async taiDanhSachNguoiLa(apiPort?: number | null): Promise<AnhNguoiLa[]> {
    const qs = new URLSearchParams();
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    const q = qs.toString();
    try {
      const resp = await fetch(`/api/camera/recorder/face/unknown${q ? `?${q}` : ''}`, {
        credentials: 'same-origin',
      });
      if (!resp.ok) return [];
      const json = (await resp.json()) as { items?: Array<Record<string, unknown>> };
      if (!Array.isArray(json.items)) return [];
      return json.items
        .map((o) => ({
          filename: typeof o.filename === 'string' ? o.filename.trim() : '',
          url: typeof o.url === 'string' ? o.url.trim() : '',
          ts: typeof o.ts === 'number' ? Math.trunc(o.ts) : 0,
        }))
        .filter((x) => Boolean(x.filename && x.url));
    } catch {
      return [];
    }
  },

  async themMatTuCamera(
    name: string,
    camera: string,
    apiPort?: number | null,
  ): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams();
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    const q = qs.toString();
    try {
      const resp = await fetch(`/api/camera/recorder/face/enroll/camera${q ? `?${q}` : ''}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name.trim(), camera: camera.trim() }),
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && json.ok);
      const message =
        typeof json.message === 'string' && json.message
          ? json.message
          : typeof json.error === 'string' && json.error
            ? json.error
            : ok
              ? 'Đã thêm ảnh khuôn mặt từ camera.'
              : `Không thêm được ảnh (mã ${resp.status}).`;
      return { ok, message };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi kết nối đầu ghi camera.' };
    }
  },

  async themMatTuAnhBase64(
    name: string,
    imageBase64: string,
    apiPort?: number | null,
  ): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams();
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    const q = qs.toString();
    try {
      const resp = await fetch(`/api/camera/recorder/face/enroll/upload${q ? `?${q}` : ''}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name.trim(), image_base64: imageBase64 }),
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && json.ok);
      const message =
        typeof json.message === 'string' && json.message
          ? json.message
          : typeof json.error === 'string' && json.error
            ? json.error
            : ok
              ? 'Đã lưu ảnh khuôn mặt.'
              : `Không lưu được ảnh (mã ${resp.status}).`;
      return { ok, message };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi tải ảnh lên đầu ghi.' };
    }
  },

  async chuyenNguoiLaThanhQuen(
    filename: string,
    name: string,
    apiPort?: number | null,
  ): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams();
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    const q = qs.toString();
    try {
      const resp = await fetch(`/api/camera/recorder/face/promote${q ? `?${q}` : ''}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ filename, name: name.trim() }),
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && json.ok);
      const message =
        typeof json.message === 'string' && json.message
          ? json.message
          : typeof json.error === 'string' && json.error
            ? json.error
            : ok
              ? 'Đã gán tên người quen.'
              : 'Không gán được tên.';
      return { ok, message };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi kết nối đầu ghi.' };
    }
  },

  async xoaNguoiQuen(
    name: string,
    apiPort?: number | null,
  ): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams();
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    const q = qs.toString();
    try {
      const resp = await fetch(`/api/camera/recorder/face/delete${q ? `?${q}` : ''}`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name.trim() }),
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && json.ok);
      const message =
        typeof json.message === 'string' && json.message
          ? json.message
          : typeof json.error === 'string' && json.error
            ? json.error
            : ok
              ? 'Đã xóa người quen.'
              : 'Không xóa được người quen.';
      return { ok, message };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi kết nối đầu ghi.' };
    }
  },

  async taiPtzPresets(cam: string, apiPort?: number | null): Promise<PtzPreset[]> {
    const qs = new URLSearchParams({ cam });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    try {
      const resp = await fetch(`/api/camera/recorder/ptz/presets?${qs.toString()}`, {
        credentials: 'same-origin',
      });
      if (!resp.ok) return PTZ_PRESETS_MAC_DINH;
      const json = (await resp.json()) as { presets?: Array<Record<string, unknown>> };
      if (!Array.isArray(json.presets) || json.presets.length === 0) return PTZ_PRESETS_MAC_DINH;
      const list = json.presets
        .map((o) => {
          const id =
            (typeof o.id === 'string' && o.id.trim()) ||
            (typeof o.token === 'string' && o.token.trim()) ||
            '';
          return {
            id,
            label: (typeof o.label === 'string' && o.label.trim()) || id,
            token: (typeof o.token === 'string' && o.token.trim()) || id,
          };
        })
        .filter((x) => Boolean(x.id));
      return list.length > 0 ? list : PTZ_PRESETS_MAC_DINH;
    } catch {
      return PTZ_PRESETS_MAC_DINH;
    }
  },

  async dieuKhienPtzMove(
    cam: string,
    direction: string,
    speed = 0.5,
    apiPort?: number | null,
  ): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams({ cam, dir: direction, speed: String(speed) });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    try {
      const resp = await fetch(`/api/camera/recorder/ptz/move?${qs.toString()}`, {
        method: 'POST',
        credentials: 'same-origin',
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && json.ok);
      return {
        ok,
        message:
          typeof json.message === 'string' && json.message
            ? json.message
            : typeof json.error === 'string' && json.error
              ? json.error
              : ok
                ? 'Đã xoay camera.'
                : 'Không xoay được camera.',
      };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi gọi PTZ.' };
    }
  },

  async goiPtzPreset(
    cam: string,
    preset: string,
    apiPort?: number | null,
  ): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams({ cam, preset });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    try {
      const resp = await fetch(`/api/camera/recorder/ptz/preset?${qs.toString()}`, {
        method: 'POST',
        credentials: 'same-origin',
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && json.ok);
      return {
        ok,
        message:
          typeof json.message === 'string' && json.message
            ? json.message
            : typeof json.error === 'string' && json.error
              ? json.error
              : ok
                ? 'Đã xoay tới góc lưu sẵn.'
                : 'Không gọi được góc lưu sẵn.',
      };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi gọi preset PTZ.' };
    }
  },

  async datCheDoRiengTu(bat: boolean, apiPort?: number | null): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams({ enabled: bat ? '1' : '0' });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    try {
      const resp = await fetch(`/api/camera/recorder/privacy?${qs.toString()}`, {
        method: 'POST',
        credentials: 'same-origin',
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && (json.ok !== false));
      return {
        ok,
        message:
          typeof json.message === 'string' && json.message
            ? json.message
            : bat
              ? 'Đã bật Chế độ Riêng tư.'
              : 'Đã tắt Chế độ Riêng tư.',
      };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi đổi Chế độ Riêng tư.' };
    }
  },

  async datCheDoNha(mode: string, apiPort?: number | null): Promise<{ ok: boolean; message: string }> {
    const qs = new URLSearchParams({ mode });
    if (typeof apiPort === 'number' && apiPort >= 1 && apiPort <= 65535) qs.set('port', String(apiPort));
    try {
      const resp = await fetch(`/api/camera/recorder/home_mode?${qs.toString()}`, {
        method: 'POST',
        credentials: 'same-origin',
      });
      const json = (await resp.json()) as Record<string, unknown>;
      const ok = Boolean(resp.ok && (json.ok !== false));
      return {
        ok,
        message: typeof json.message === 'string' && json.message ? json.message : 'Đã đổi chế độ camera.',
      };
    } catch (err) {
      return { ok: false, message: err instanceof Error ? err.message : 'Lỗi đổi chế độ camera.' };
    }
  },
};
