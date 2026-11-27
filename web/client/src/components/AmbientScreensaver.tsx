import React, { useEffect, useState } from 'react';
import { Cloud, CloudFog, CloudLightning, CloudRain, CloudSnow, CloudSun, Sun } from 'lucide-react';
import { AmLich } from '../data/amLich.js';
import type { BomNuocState, PublicBomNuocConfig, TrangThaiKetNoiBom } from '../data/bomnuoc.js';
import type { DienState } from '../data/dien.js';
import { laKhungGioDem, nhanBomChoManCho, type ScreensaverConfig } from '../data/screensaver.js';
import type { ThoiTietUi } from '../data/thoitiet.js';
import { SH } from '../theme/colors.js';

const THU_VN_FULL = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

function NightCloudMoonIcon() {
  return (
    <svg width="72" height="58" viewBox="0 0 72 58" fill="none" aria-hidden="true">
      <path
        d="M44.5 9.5C38.6 10.6 34 15.8 34 22.1C34 29.2 39.8 35 46.9 35C51.6 35 55.7 32.5 58 28.7C56.2 29.3 54.3 29.6 52.3 29.6C43.8 29.6 36.9 22.7 36.9 14.2C36.9 12.5 37.2 10.9 37.7 9.4C40 9.1 42.3 9.1 44.5 9.5Z"
        fill="#7693C4"
      />
      <path
        d="M46 6.5C35.8 6.5 27.5 14.8 27.5 25C27.5 35.2 35.8 43.5 46 43.5C53.4 43.5 59.8 39.1 62.7 32.8C60.7 33.6 58.5 34 56.2 34C46.3 34 38.3 26 38.3 16.1C38.3 12.6 39.3 9.3 41.1 6.6C42.7 6.5 44.3 6.5 46 6.5Z"
        fill="#7693C4"
        opacity="0.92"
      />
      <path
        d="M19 46.5H40.5C44.1 46.5 47 43.7 47 40.2C47 36.9 44.5 34.3 41.3 34C40.1 29.8 36.3 26.8 31.8 26.8C26.4 26.8 22 31.1 21.9 36.5C17.9 37 14.8 40.3 14.8 44.3C14.8 45.6 16.7 46.5 19 46.5Z"
        fill="#536B96"
        stroke="#000000"
        strokeWidth="2.5"
      />
    </svg>
  );
}

function AmbientWeatherBadge({ maWmo, banNgay }: { maWmo: number; banNgay: boolean }) {
  if (!banNgay && (maWmo === 0 || maWmo === 1 || maWmo === 2)) {
    return <NightCloudMoonIcon />;
  }
  const color = '#7693C4';
  if (maWmo === 0) return <Sun size={56} color="#B89B5E" strokeWidth={1.6} />;
  if (maWmo === 1 || maWmo === 2) return <CloudSun size={56} color="#B89B5E" strokeWidth={1.6} />;
  if (maWmo === 3) return <Cloud size={56} color={color} strokeWidth={1.6} />;
  if (maWmo === 45 || maWmo === 48) return <CloudFog size={56} color={color} strokeWidth={1.6} />;
  if ((maWmo >= 51 && maWmo <= 67) || (maWmo >= 80 && maWmo <= 82)) {
    return <CloudRain size={56} color={color} strokeWidth={1.6} />;
  }
  if (maWmo >= 71 && maWmo <= 86) return <CloudSnow size={56} color={color} strokeWidth={1.6} />;
  if (maWmo >= 95) return <CloudLightning size={56} color="#9C84C9" strokeWidth={1.6} />;
  return <CloudSun size={56} color={color} strokeWidth={1.6} />;
}

function wmoShortLabel(maWmo: number, banNgay: boolean): string {
  if (maWmo === 0) return banNgay ? 'Trời quang' : 'Quang mây';
  if (maWmo === 1 || maWmo === 2) return 'Ít mây';
  if (maWmo === 3) return 'Nhiều mây';
  if (maWmo === 45 || maWmo === 48) return 'Sương mù';
  if (maWmo >= 51 && maWmo <= 57) return 'Mưa phùn';
  if (maWmo >= 61 && maWmo <= 67) return 'Có mưa';
  if (maWmo >= 71 && maWmo <= 86) return 'Mưa tuyết';
  if (maWmo >= 80 && maWmo <= 82) return 'Mưa rào';
  if (maWmo >= 95) return 'Có giông';
  return 'Thay đổi';
}

export function AmbientScreensaver({
  nowMs,
  thoiTiet,
  dien,
  bomNuoc,
  ketNoiBom,
  bomConfig,
  config,
  onWake,
}: {
  nowMs: number;
  thoiTiet: ThoiTietUi;
  dien: DienState;
  bomNuoc: BomNuocState | null;
  ketNoiBom: TrangThaiKetNoiBom;
  bomConfig: PublicBomNuocConfig;
  config: ScreensaverConfig;
  onWake: () => void;
}) {
  // Pixel shift moi 90 giay (±10px X/Y) de chong luu anh OLED/LCD
  const [shift, setShift] = useState({ x: 0, y: 0 });

  useEffect(() => {
    const id = window.setInterval(() => {
      const x = Math.floor(Math.random() * 21) - 10;
      const y = Math.floor(Math.random() * 21) - 10;
      setShift({ x, y });
    }, 90_000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      e.preventDefault();
      onWake();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onWake]);

  // Giữ màn hình iPhone/Tablet không tự khóa khi đang bật đồng hồ đêm (Screen Wake Lock API)
  useEffect(() => {
    let sentinel: { release: () => Promise<void> } | null = null;
    let cancelled = false;
    const nav = navigator as Navigator & {
      wakeLock?: { request: (type: 'screen') => Promise<{ release: () => Promise<void> }> };
    };

    const acquire = async () => {
      if (cancelled || !nav.wakeLock || document.visibilityState !== 'visible') return;
      try {
        sentinel = await nav.wakeLock.request('screen');
      } catch {
        // Ignore if unsupported or battery saver blocks Wake Lock
      }
    };

    void acquire();
    const onVis = () => {
      if (document.visibilityState === 'visible') void acquire();
    };
    document.addEventListener('visibilitychange', onVis);

    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVis);
      if (sentinel) {
        void sentinel.release().catch(() => {});
      }
    };
  }, []);

  const dt = new Date(nowMs);
  const hh = String(dt.getHours()).padStart(2, '0');
  const mm = String(dt.getMinutes()).padStart(2, '0');
  const thu = THU_VN_FULL[dt.getDay()];
  const dd = String(dt.getDate()).padStart(2, '0');
  const mon = String(dt.getMonth() + 1).padStart(2, '0');
  const yyyy = dt.getFullYear();

  const am = AmLich.tuDuongLich(dt.getDate(), dt.getMonth() + 1, yyyy);
  const cc = AmLich.canChi(am);
  const amDay = String(am.ngay).padStart(2, '0');
  const amMon = String(am.thang).padStart(2, '0');

  const weatherOk = thoiTiet.state === 'co' ? thoiTiet.data : null;
  const tempDisplay = weatherOk ? `${weatherOk.hienTai.nhietDo}°C` : '--°C';
  const locDisplay = weatherOk
    ? `${weatherOk.diaDiem} · ${wmoShortLabel(weatherOk.hienTai.maWmo, weatherOk.hienTai.banNgay)}`
    : thoiTiet.state === 'dang_tai'
      ? 'Đang tải thời tiết…'
      : 'Chưa có thời tiết';

  const dienHomNay = dien.ngaySanLuong !== null ? `${dien.ngaySanLuong.toFixed(1)} kWh` : '-- kWh';
  const bomNhan = nhanBomChoManCho(bomNuoc, ketNoiBom, bomConfig);

  const isNightDim =
    config.nightDimEnabled &&
    laKhungGioDem(dt.getHours(), config.nightStartHour, config.nightEndHour);

  return (
    <div
      role="button"
      tabIndex={0}
      aria-label="Màn hình chờ ban đêm, chạm để đánh thức"
      onPointerDown={onWake}
      className="fixed inset-0 z-50 bg-[#000000] select-none flex flex-col items-center justify-between pt-[max(2rem,env(safe-area-inset-top))] pb-[max(2rem,env(safe-area-inset-bottom))] pl-[max(1.5rem,env(safe-area-inset-left))] pr-[max(1.5rem,env(safe-area-inset-right))] cursor-pointer overflow-hidden"
    >
      <div className="h-4 shrink-0" />

      {/* Khoi trung tam co Pixel Shift */}
      <div
        className="flex flex-col sm:flex-row items-center justify-center gap-8 sm:gap-14 max-w-4xl w-full"
        style={{
          transform: `translate3d(${shift.x}px, ${shift.y}px, 0)`,
          transition: 'transform 1.8s ease-in-out',
        }}
      >
        {/* Ben trai: Gio lon + Duong lich + Am lich */}
        <div className="flex flex-col items-center text-center">
          <div className="font-light tracking-tight text-[#E8E3D5] tabular-nums leading-none text-[84px] sm:text-[124px] md:text-[148px]">
            {hh}:{mm}
          </div>
          <div className="mt-3 text-lg sm:text-2xl text-[#8E949E] font-normal">
            {thu}, {dd}/{mon}/{yyyy}
          </div>
          <div className="mt-1.5 text-base sm:text-xl text-[#9E7238] font-normal">
            Âm {amDay}/{amMon}{am.nhuan ? ' Nhuận' : ''} · Năm {cc.nam}
          </div>
        </div>

        {/* Duong ke doc ngan cach */}
        <div className="hidden sm:block w-px h-48 bg-[#262A30] shrink-0" />

        {/* Ben phai: Thoi tiet ngoai troi */}
        <div className="flex flex-col items-center text-center">
          <div className="flex items-center gap-4">
            {weatherOk ? (
              <AmbientWeatherBadge
                maWmo={weatherOk.hienTai.maWmo}
                banNgay={weatherOk.hienTai.banNgay}
              />
            ) : (
              <NightCloudMoonIcon />
            )}
            <div className="font-light text-[#DCD7C9] tabular-nums leading-none text-5xl sm:text-7xl">
              {tempDisplay}
            </div>
          </div>
          <div className="mt-3.5 text-lg sm:text-2xl text-[#8E949E] font-normal">
            {locDisplay}
          </div>
        </div>
      </div>

      {/* Thanh trang thai gon phia duoi + goi y cham */}
      <div
        className="flex flex-col items-center gap-3.5 w-full"
        style={{
          transform: `translate3d(${Math.round(shift.x * 0.5)}px, ${Math.round(shift.y * 0.5)}px, 0)`,
          transition: 'transform 1.8s ease-in-out',
        }}
      >
        <div className="flex flex-wrap items-center justify-center gap-3 opacity-85">
          <div className="inline-flex items-center gap-2 rounded-full bg-[#14171C] border border-[#1E232B] px-4 py-1.5 text-xs sm:text-sm text-[#A3ABB7]">
            <span
              className="inline-block h-2 w-2 rounded-full"
              style={{ backgroundColor: SH.Green }}
            />
            <span>Hệ thống an toàn</span>
          </div>
          <div className="inline-flex items-center rounded-full bg-[#14171C] border border-[#1E232B] px-4 py-1.5 text-xs sm:text-sm text-[#A3ABB7] tabular-nums">
            Điện hôm nay: {dienHomNay}
          </div>
          <div className="inline-flex items-center rounded-full bg-[#14171C] border border-[#1E232B] px-4 py-1.5 text-xs sm:text-sm text-[#A3ABB7]">
            Bơm: {bomNhan}
          </div>
        </div>
        <div className="text-[11px] sm:text-xs text-[#4A5058]">
          Chạm màn hình để đánh thức
        </div>
      </div>

      {/* Lop phu giam do sang tu dong sau 22:00 */}
      {isNightDim && (
        <div
          aria-hidden="true"
          className="pointer-events-none fixed inset-0 bg-black transition-opacity duration-700"
          style={{ opacity: config.webDimOpacity }}
        />
      )}
    </div>
  );
}

