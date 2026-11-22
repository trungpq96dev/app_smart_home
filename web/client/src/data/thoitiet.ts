export interface ThoiTietHienTai {
  nhietDo: number;
  camGiac: number;
  doAm: number;
  gio: number;
  maWmo: number;
  banNgay: boolean;
}

export interface NgayThoiTiet {
  thu: string;
  maWmo: number;
  caoNhat: number;
  thapNhat: number;
}

export interface ThoiTietData {
  diaDiem: string;
  hienTai: ThoiTietHienTai;
  cacNgay: NgayThoiTiet[];
  gioCapNhat: string;
}

export type ThoiTietUi =
  | { state: 'dang_tai' }
  | { state: 'co'; data: ThoiTietData }
  | { state: 'loi'; moTa: string };

export const OpenMeteo = {
  LAT: 21.0278,
  LON: 105.8342,
  TEN_DIA_DIEM: 'Hà Nội',
  SO_NGAY: 4,
};

function two(v: number): string {
  return v < 10 ? `0${v}` : String(v);
}

function nhanThu(ngayIso: string, chiSo: number): string {
  if (chiSo === 0) return 'Hôm nay';
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ngayIso);
  if (!m) return ngayIso;
  const dt = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  const dow = dt.getDay(); // 0=CN, 1=T2..6=T7
  switch (dow) {
    case 1: return 'Th 2';
    case 2: return 'Th 3';
    case 3: return 'Th 4';
    case 4: return 'Th 5';
    case 5: return 'Th 6';
    case 6: return 'Th 7';
    default: return 'CN';
  }
}

function gioCapNhat(gioIso: string): string {
  const m = /T(\d{2}):(\d{2})/.exec(gioIso);
  if (m) return `${m[1]}:${m[2]}`;
  const now = new Date();
  return `${two(now.getHours())}:${two(now.getMinutes())}`;
}

export function dienGiaiLoiThoiTiet(err: unknown): string {
  const e = err as { name?: string; message?: string } | undefined;
  const msg = e?.message || '';
  if (e?.name === 'TimeoutError' || e?.name === 'AbortError') {
    return 'Open-Meteo không trả lời kịp. Mạng chậm hoặc đang chập chờn, sẽ tự thử lại sau.';
  }
  if (msg.toLowerCase().includes('failed to fetch') || msg.toLowerCase().includes('networkerror')) {
    return 'Không vào được Open-Meteo. Thiết bị chưa có mạng, hoặc đang ở Wi-Fi không ra được Internet.';
  }
  return msg || 'Lỗi không rõ';
}

export async function layThoiTiet(
  lat: number = OpenMeteo.LAT,
  lon: number = OpenMeteo.LON,
  tenDiaDiem: string = OpenMeteo.TEN_DIA_DIEM,
): Promise<{ ok: true; data: ThoiTietData } | { ok: false; error: string }> {
  const url =
    `https://api.open-meteo.com/v1/forecast` +
    `?latitude=${lat}` +
    `&longitude=${lon}` +
    `&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m` +
    `&daily=weather_code,temperature_2m_max,temperature_2m_min` +
    `&timezone=auto` +
    `&forecast_days=${OpenMeteo.SO_NGAY}`;

  try {
    const resp = await fetch(url, { signal: AbortSignal.timeout(8000) });
    if (resp.status !== 200) {
      return { ok: false, error: `Open-Meteo trả mã ${resp.status}.` };
    }
    const goc = (await resp.json()) as {
      current: {
        temperature_2m: number;
        apparent_temperature: number;
        relative_humidity_2m: number;
        wind_speed_10m: number;
        weather_code: number;
        is_day?: number;
        time?: string;
      };
      daily: {
        time: string[];
        weather_code: number[];
        temperature_2m_max: number[];
        temperature_2m_min: number[];
      };
    };

    const cur = goc.current;
    const hienTai: ThoiTietHienTai = {
      nhietDo: Math.round(cur.temperature_2m),
      camGiac: Math.round(cur.apparent_temperature),
      doAm: Math.trunc(cur.relative_humidity_2m),
      gio: Math.round(cur.wind_speed_10m),
      maWmo: Math.trunc(cur.weather_code),
      banNgay: (cur.is_day ?? 1) === 1,
    };

    const cacNgay: NgayThoiTiet[] = goc.daily.time.map((ngayIso, i) => ({
      thu: nhanThu(ngayIso, i),
      maWmo: Math.trunc(goc.daily.weather_code[i]),
      caoNhat: Math.round(goc.daily.temperature_2m_max[i]),
      thapNhat: Math.round(goc.daily.temperature_2m_min[i]),
    }));

    return {
      ok: true,
      data: {
        diaDiem: tenDiaDiem,
        hienTai,
        cacNgay,
        gioCapNhat: gioCapNhat(cur.time || ''),
      },
    };
  } catch (err) {
    return { ok: false, error: dienGiaiLoiThoiTiet(err) };
  }
}

