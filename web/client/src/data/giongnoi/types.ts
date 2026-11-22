export type BanSongCong = 'TU_DONG' | 'LUON_BAT' | 'LUON_TAT';

export const BAN_SONG_CONG_LIST: Array<{ id: BanSongCong; nhan: string }> = [
  { id: 'TU_DONG', nhan: 'Tự động' },
  { id: 'LUON_BAT', nhan: 'Luôn bật' },
  { id: 'LUON_TAT', nhan: 'Luôn tắt' },
];

export interface CauHinhGiongNoi {
  khoaApi: string;
  model: string;
  giongDoc: string;
  guiMqtt: boolean;
  banSongCong: BanSongCong;
  giayImLangTuDong: number;
  giayToiDaPhien: number;
  phutMoiNgay: number;
}

export const MODEL_MAC_DINH = 'gemini-3.8-live';
export const GIONG_CO_SAN = ['Aoede', 'Kore', 'Puck', 'Charon', 'Fenrir', 'Leda', 'Orus', 'Zephyr'];
export const GIONG_MAC_DINH = 'Aoede';

const GOC_WS =
  'wss://generativelanguage.googleapis.com/ws/' +
  'google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent';

export function defaultCauHinhGiongNoi(): CauHinhGiongNoi {
  return {
    khoaApi: '',
    model: MODEL_MAC_DINH,
    giongDoc: GIONG_MAC_DINH,
    guiMqtt: false,
    banSongCong: 'TU_DONG',
    giayImLangTuDong: 45,
    giayToiDaPhien: 600,
    phutMoiNgay: 30,
  };
}

export function coKhoaGiongNoi(c: CauHinhGiongNoi): boolean {
  return c.khoaApi.trim().length > 0;
}

export function duongWsGemini(c: CauHinhGiongNoi): string {
  return `${GOC_WS}?key=${c.khoaApi.trim()}`;
}

export function choThayTrongLoiWs(): string {
  return `${GOC_WS}?key=***`;
}

export interface DongThoai {
  cuaMinh: boolean;
  cau: string;
}

export interface ViecDaLam {
  cau: string;
  thanhCong: boolean;
  chuaCoThietBiThat: boolean;
}

export type TrangThaiGiongNoi =
  | { state: 'tat' }
  | { state: 'dang_noi' }
  | { state: 'dang_nghe' }
  | { state: 'dang_tra_loi' }
  | { state: 'loi'; cau: string };

export function dangChayTrangThai(t: TrangThaiGiongNoi): boolean {
  return t.state === 'dang_noi' || t.state === 'dang_nghe' || t.state === 'dang_tra_loi';
}

export interface PhienGiongNoi {
  trangThai: TrangThaiGiongNoi;
  thoai: DongThoai[];
  daLam: ViecDaLam[];
  giayDaChay: number;
  banSongCong: boolean;
}

export const TOI_DA_DONG_THOAI = 8;

export function createEmptyPhienGiongNoi(): PhienGiongNoi {
  return {
    trangThai: { state: 'tat' },
    thoai: [],
    daLam: [],
    giayDaChay: 0,
    banSongCong: false,
  };
}

export interface MucDungNgay {
  ngay: string;
  giay: number;
  soPhien: number;
  tokenVao: number;
  tokenRa: number;
  tokenTong: number;
}

export const MucDung = {
  SO_NGAY_GIU: 30,

  gopPhien(
    cu: MucDungNgay[],
    homNay: string,
    giay: number,
    tokenVao: number,
    tokenRa: number,
    tokenTong: number,
  ): MucDungNgay[] {
    const ds = cu.map((x) => ({ ...x }));
    const idx = ds.findIndex((x) => x.ngay === homNay);
    if (idx >= 0) {
      const n = ds[idx];
      ds[idx] = {
        ...n,
        giay: n.giay + giay,
        soPhien: n.soPhien + 1,
        tokenVao: n.tokenVao + tokenVao,
        tokenRa: n.tokenRa + tokenRa,
        tokenTong: n.tokenTong + tokenTong,
      };
    } else {
      ds.push({ ngay: homNay, giay, soPhien: 1, tokenVao, tokenRa, tokenTong });
    }
    return ds.sort((a, b) => a.ngay.localeCompare(b.ngay)).slice(-MucDung.SO_NGAY_GIU);
  },

  cuaNgay(ds: MucDungNgay[], ngay: string): MucDungNgay {
    return ds.find((x) => x.ngay === ngay) ?? {
      ngay,
      giay: 0,
      soPhien: 0,
      tokenVao: 0,
      tokenRa: 0,
      tokenTong: 0,
    };
  },

  giayNgayGanDay(ds: MucDungNgay[], soNgay: number): number {
    if (soNgay <= 0) return 0;
    return ds.slice(-soNgay).reduce((acc, x) => acc + x.giay, 0);
  },

  homNay(): string {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  sangJson(ds: MucDungNgay[]): string {
    return JSON.stringify(
      ds.map((n) => ({
        ngay: n.ngay,
        giay: n.giay,
        phien: n.soPhien,
        tv: n.tokenVao,
        tr: n.tokenRa,
        tt: n.tokenTong,
      })),
    );
  },

  tuJson(s: string | null | undefined): MucDungNgay[] {
    if (!s || !s.trim()) return [];
    try {
      const m = JSON.parse(s);
      if (!Array.isArray(m)) return [];
      const out: MucDungNgay[] = [];
      for (const item of m) {
        if (!item || typeof item !== 'object') continue;
        const o = item as Record<string, unknown>;
        const ngay = typeof o.ngay === 'string' ? o.ngay.trim() : '';
        if (!ngay) continue;
        out.push({
          ngay,
          giay: Number(o.giay ?? 0) || 0,
          soPhien: Number(o.phien ?? 0) || 0,
          tokenVao: Number(o.tv ?? 0) || 0,
          tokenRa: Number(o.tr ?? 0) || 0,
          tokenTong: Number(o.tt ?? 0) || 0,
        });
      }
      return out.sort((a, b) => a.ngay.localeCompare(b.ngay));
    } catch {
      return [];
    }
  },
};

const STORAGE_VOICE_CONFIG = 'smarthome_voice_config_v1';
const STORAGE_VOICE_USAGE = 'smarthome_voice_usage_v1';

export function loadVoiceConfig(): CauHinhGiongNoi {
  const def = defaultCauHinhGiongNoi();
  try {
    const raw = localStorage.getItem(STORAGE_VOICE_CONFIG);
    if (!raw) return def;
    const p = JSON.parse(raw) as Partial<CauHinhGiongNoi>;
    const ban: BanSongCong =
      p.banSongCong === 'LUON_BAT' || p.banSongCong === 'LUON_TAT' || p.banSongCong === 'TU_DONG'
        ? p.banSongCong
        : def.banSongCong;
    return {
      khoaApi: typeof p.khoaApi === 'string' ? p.khoaApi : def.khoaApi,
      model: typeof p.model === 'string' && p.model.trim() ? p.model : def.model,
      giongDoc: typeof p.giongDoc === 'string' && p.giongDoc.trim() ? p.giongDoc : def.giongDoc,
      guiMqtt: typeof p.guiMqtt === 'boolean' ? p.guiMqtt : def.guiMqtt,
      banSongCong: ban,
      giayImLangTuDong: typeof p.giayImLangTuDong === 'number' ? p.giayImLangTuDong : def.giayImLangTuDong,
      giayToiDaPhien: typeof p.giayToiDaPhien === 'number' ? p.giayToiDaPhien : def.giayToiDaPhien,
      phutMoiNgay: typeof p.phutMoiNgay === 'number' ? p.phutMoiNgay : def.phutMoiNgay,
    };
  } catch {
    return def;
  }
}

export function saveVoiceConfig(c: CauHinhGiongNoi): void {
  try {
    localStorage.setItem(
      STORAGE_VOICE_CONFIG,
      JSON.stringify({
        ...c,
        khoaApi: c.khoaApi.trim(),
        model: c.model.trim(),
        giongDoc: c.giongDoc.trim(),
      }),
    );
  } catch {
    // ignore storage quota
  }
}

export function loadVoiceUsage(): MucDungNgay[] {
  try {
    return MucDung.tuJson(localStorage.getItem(STORAGE_VOICE_USAGE));
  } catch {
    return [];
  }
}

export function saveVoiceUsage(ds: MucDungNgay[]): void {
  try {
    localStorage.setItem(STORAGE_VOICE_USAGE, MucDung.sangJson(ds));
  } catch {
    // ignore storage quota
  }
}

