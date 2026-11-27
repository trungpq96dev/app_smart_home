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
  Thermometer,
  Wind,
} from 'lucide-react';
import type { ThoiTietUi } from '../data/thoitiet.js';
import { SH, tint } from '../theme/colors.js';
import { RoundIconButton, SmartCard } from './Common.js';

function wmoDescription(
  code: number,
  banNgay: boolean,
): { text: string; color: string; Icon: React.ComponentType<{ size?: number }> } {
  if (code === 0) {
    return {
      text: banNgay ? 'Trời quang' : 'Đêm quang mây',
      color: banNgay ? SH.Amber : SH.Purple,
      Icon: banNgay ? Sun : Moon,
    };
  }
  if (code === 1) {
    return {
      text: banNgay ? 'Nắng nhẹ' : 'Ít mây',
      color: banNgay ? SH.Amber : SH.Purple,
      Icon: banNgay ? CloudSun : Moon,
    };
  }
  if (code === 2) {
    return { text: 'Có mây', color: SH.Amber, Icon: CloudSun };
  }
  if (code === 3) {
    return { text: 'Nhiều mây', color: SH.TextSecondary, Icon: Cloud };
  }
  if (code === 45 || code === 48) {
    return { text: 'Sương mù', color: SH.TextSecondary, Icon: CloudFog };
  }
  if (code === 56 || code === 57) {
    return { text: 'Mưa phùn lạnh', color: SH.Cyan, Icon: CloudSnow };
  }
  if (code >= 51 && code <= 55) {
    return { text: 'Mưa phùn', color: SH.Cyan, Icon: CloudRain };
  }
  if (code === 66 || code === 67) {
    return { text: 'Mưa lạnh', color: SH.Cyan, Icon: CloudSnow };
  }
  if (code >= 61 && code <= 65) {
    return { text: 'Có mưa', color: SH.Cyan, Icon: CloudRain };
  }
  if (code === 77) {
    return { text: 'Hạt tuyết', color: SH.Cyan, Icon: CloudSnow };
  }
  if (code >= 71 && code <= 75) {
    return { text: 'Tuyết rơi', color: SH.Cyan, Icon: CloudSnow };
  }
  if (code >= 80 && code <= 82) {
    return { text: 'Mưa rào', color: SH.Blue, Icon: CloudRain };
  }
  if (code === 85 || code === 86) {
    return { text: 'Mưa tuyết', color: SH.Cyan, Icon: CloudSnow };
  }
  if (code === 96 || code === 99) {
    return { text: 'Dông, mưa đá', color: SH.Red, Icon: CloudLightning };
  }
  if (code >= 95) {
    return { text: 'Dông', color: SH.Purple, Icon: CloudLightning };
  }
  return { text: 'Thay đổi', color: SH.TextSecondary, Icon: CloudSun };
}

function MiniWeatherStat({
  icon,
  label,
  value,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  color: string;
}) {
  return (
    <div className="flex items-center gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5 min-w-0">
      <span className="shrink-0" style={{ color }}>
        {icon}
      </span>
      <div className="min-w-0">
        <div className="text-[10px] text-[#8A9BB8] leading-tight truncate">{label}</div>
        <div className="font-mono-num text-[11px] font-bold text-[#E6EDF7] truncate">{value}</div>
      </div>
    </div>
  );
}

export function WeatherCard({
  thoiTiet,
  onRetry,
  diaDiemFallback = 'Thời tiết ngoài trời',
}: {
  thoiTiet: ThoiTietUi;
  onRetry: () => void;
  diaDiemFallback?: string;
}) {
  if (thoiTiet.state === 'dang_tai') {
    return (
      <SmartCard className="p-3">
        <div className="flex items-center justify-between gap-2.5">
          <div className="flex items-center gap-2.5 min-w-0">
            <div
              className="flex h-10 w-10 items-center justify-center rounded-xl shrink-0"
              style={{
                backgroundColor: tint(SH.Blue, 0.14),
                border: `1px solid ${tint(SH.Blue, 0.3)}`,
                color: SH.Blue,
              }}
            >
              <CloudSun size={20} />
            </div>
            <div className="min-w-0">
              <div className="text-sm font-bold text-[#E6EDF7] truncate">{diaDiemFallback}</div>
              <div className="text-xs text-[#8A9BB8] mt-0.5 truncate">Đang tải từ Open-Meteo…</div>
            </div>
          </div>
          <RoundIconButton onClick={onRetry} disabled title="Đang tải thời tiết" size={32} color={SH.Blue}>
            <RefreshCw size={15} className="animate-spin" />
          </RoundIconButton>
        </div>
      </SmartCard>
    );
  }

  if (thoiTiet.state === 'loi') {
    return (
      <SmartCard className="p-3">
        <div className="flex items-center justify-between gap-2.5">
          <div className="min-w-0">
            <div className="text-sm font-bold text-[#E6EDF7] truncate">{diaDiemFallback}</div>
            <p className="text-xs text-[#F59E0B] mt-0.5 line-clamp-2">{thoiTiet.moTa}</p>
          </div>
          <RoundIconButton onClick={onRetry} title="Thử tải lại thời tiết" size={32} color={SH.Blue}>
            <RefreshCw size={15} />
          </RoundIconButton>
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
            <div className="text-sm font-bold text-[#E6EDF7] truncate">{diaDiem}</div>
            <div className="text-[11px] text-[#8A9BB8] truncate">
              {info.text} · Cập nhật {gioCapNhat}
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <div className="font-mono-num text-xl font-bold text-[#E6EDF7] leading-none">
            {hienTai.nhietDo}°C
          </div>
          <RoundIconButton onClick={onRetry} title="Làm mới thời tiết" size={30} color={SH.Blue}>
            <RefreshCw size={13} />
          </RoundIconButton>
        </div>
      </div>

      <div className="mt-2.5 grid grid-cols-3 gap-1.5">
        <MiniWeatherStat
          icon={<Thermometer size={14} />}
          label="Cảm giác"
          value={`${hienTai.camGiac}°`}
          color={SH.Red}
        />
        <MiniWeatherStat
          icon={<Droplets size={14} />}
          label="Độ ẩm"
          value={`${hienTai.doAm}%`}
          color={SH.Cyan}
        />
        <MiniWeatherStat
          icon={<Wind size={14} />}
          label="Gió"
          value={`${hienTai.gio} km/h`}
          color={SH.Blue}
        />
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
