import React from 'react';
import {
  Cloud,
  CloudFog,
  CloudLightning,
  CloudRain,
  CloudSnow,
  CloudSun,
  Droplets,
  Moon,
  RefreshCw,
  Sun,
  Wind,
} from 'lucide-react';
import type { ThoiTietUi } from '../data/thoitiet.js';
import { SH, tint } from '../theme/colors.js';
import { FlatButton, SmartCard } from './Common.js';

function wmoDescription(code: number, banNgay: boolean): { text: string; color: string; Icon: React.ComponentType<{ size?: number }> } {
  if (code === 0) {
    return {
      text: banNgay ? 'Trời quang' : 'Đêm quang mây',
      color: banNgay ? SH.Amber : SH.Purple,
      Icon: banNgay ? Sun : Moon,
    };
  }
  if (code === 1 || code === 2) {
    return { text: 'Ít mây', color: SH.Amber, Icon: CloudSun };
  }
  if (code === 3) {
    return { text: 'Nhiều mây', color: SH.TextSecondary, Icon: Cloud };
  }
  if (code === 45 || code === 48) {
    return { text: 'Sương mù', color: SH.TextSecondary, Icon: CloudFog };
  }
  if (code >= 51 && code <= 67) {
    return { text: code >= 61 ? 'Có mưa' : 'Mưa phùn', color: SH.Cyan, Icon: CloudRain };
  }
  if (code >= 71 && code <= 77) {
    return { text: 'Tuyết rơi', color: SH.Cyan, Icon: CloudSnow };
  }
  if (code >= 80 && code <= 82) {
    return { text: 'Mưa rào', color: SH.Blue, Icon: CloudRain };
  }
  if (code >= 95) {
    return { text: 'Có giông', color: SH.Purple, Icon: CloudLightning };
  }
  return { text: 'Thay đổi', color: SH.TextSecondary, Icon: CloudSun };
}

export function WeatherCard({
  thoiTiet,
  onRetry,
}: {
  thoiTiet: ThoiTietUi;
  onRetry: () => void;
}) {
  if (thoiTiet.state === 'dang_tai') {
    return (
      <SmartCard>
        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm font-bold text-[#E6EDF7]">Thời tiết ngoài trời</div>
            <div className="text-xs text-[#8A9BB8] mt-1">Đang tải từ Open-Meteo…</div>
          </div>
          <RefreshCw size={18} className="animate-spin text-[#4F8BFF]" />
        </div>
      </SmartCard>
    );
  }

  if (thoiTiet.state === 'loi') {
    return (
      <SmartCard>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="text-sm font-bold text-[#E6EDF7]">Thời tiết ngoài trời</div>
            <p className="text-xs text-[#F59E0B] mt-1">{thoiTiet.moTa}</p>
          </div>
          <FlatButton onClick={onRetry} color={SH.Blue} className="shrink-0">
            <RefreshCw size={14} />
            <span>Thử lại</span>
          </FlatButton>
        </div>
      </SmartCard>
    );
  }

  const { diaDiem, hienTai, cacNgay, gioCapNhat } = thoiTiet.data;
  const info = wmoDescription(hienTai.maWmo, hienTai.banNgay);
  const MainIcon = info.Icon;

  return (
    <SmartCard className="p-3">
      <div className="flex items-start justify-between gap-2.5">
        <div className="flex items-center gap-2.5 min-w-0">
          <div
            className="flex h-10 w-10 items-center justify-center rounded-xl shrink-0"
            style={{
              backgroundColor: tint(info.color, 0.16),
              border: `1px solid ${tint(info.color, 0.35)}`,
              color: info.color,
            }}
          >
            <MainIcon size={20} />
          </div>
          <div className="min-w-0">
            <div className="text-sm font-bold text-[#E6EDF7]">{diaDiem}</div>
            <div className="text-[11px] text-[#8A9BB8] truncate">
              {info.text} · Cảm giác {hienTai.camGiac}°C · {gioCapNhat}
            </div>
          </div>
        </div>
        <div className="text-right shrink-0">
          <div className="font-mono-num text-xl font-bold text-[#E6EDF7] leading-none">{hienTai.nhietDo}°C</div>
          <button
            type="button"
            onClick={onRetry}
            className="text-[11px] text-[#4F8BFF] hover:underline mt-1 cursor-pointer"
          >
            Làm mới
          </button>
        </div>
      </div>

      <div className="mt-2 flex items-center gap-4 text-[11px] text-[#8A9BB8]">
        <span className="inline-flex items-center gap-1">
          <Droplets size={13} className="text-[#22D3EE]" />
          <span>Ẩm {hienTai.doAm}%</span>
        </span>
        <span className="inline-flex items-center gap-1">
          <Wind size={13} className="text-[#4F8BFF]" />
          <span>Gió {hienTai.gio} km/h</span>
        </span>
      </div>

      {cacNgay.length > 0 && (
        <div className="mt-2.5 pt-2 border-t border-[#253459] grid grid-cols-4 gap-1.5">
          {cacNgay.map((ngay, idx) => {
            const dInfo = wmoDescription(ngay.maWmo, true);
            const DayIcon = dInfo.Icon;
            return (
              <div
                key={idx}
                className="flex flex-col items-center rounded-xl bg-[#0E1627] border border-[#253459] py-1.5 px-1 text-center"
              >
                <span className="text-[10px] font-semibold text-[#8A9BB8]">{ngay.thu}</span>
                <span className="my-1" style={{ color: dInfo.color }}>
                  <DayIcon size={15} />
                </span>
                <span className="font-mono-num text-[10px] font-bold text-[#E6EDF7]">
                  {ngay.caoNhat}° <span className="text-[#5F6F8C] font-normal">/ {ngay.thapNhat}°</span>
                </span>
              </div>
            );
          })}
        </div>
      )}
    </SmartCard>
  );
}

