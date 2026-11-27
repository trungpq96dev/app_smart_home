/**
 * Hợp đồng topic MQTT & HTTP Playback API của dịch vụ `scripts/camera_recorder/` trên serverubuntu.
 * Port 1:1 từ `CameraRecorderModels.kt` & `CameraPlaybackApi.kt` (Android v0.9.4).
 */

export const CameraTopic = {
  GOC: 'camera/#',
  TRANG_THAI: 'camera/trangthai',
  STATE: 'camera/state',
  PERSON: 'camera/person',
  CMD: 'camera/cmd',
  KET_QUA: 'camera/cmd/result',
} as const;

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

export function camDangGhi(rec: CameraRecorderState, tenLuong?: string | null): boolean {
  if (!isRecorderSong(rec) || rec.recordingEnabled !== true) return false;
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
      return {
        ...cur,
        online: cur.online ?? true,
        recordingEnabled: typeof o.recording_enabled === 'boolean' ? o.recording_enabled : true,
        detectPersonEnabled: typeof o.detect_person_enabled === 'boolean' ? o.detect_person_enabled : true,
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
};
