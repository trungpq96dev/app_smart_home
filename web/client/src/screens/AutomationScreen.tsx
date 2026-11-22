import React from 'react';
import { Droplets, ExternalLink, Moon, Radio, Settings, ShieldAlert, Sun, Workflow } from 'lucide-react';
import { DemoBadge, FlatButton, IconBadge, SectionHeader, SmartCard, StatusPill } from '../components/Common.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

export function AutomationScreen() {
  const { config, navigateDest, showToast } = useSmartHome();
  const hasAquaUrl = config.aquaControlUrl.trim().length > 0;

  const openAquaControl = () => {
    if (!hasAquaUrl) {
      showToast('Chưa cấu hình địa chỉ web AquaControl. Vào Cài đặt → Hệ thống để thêm.', 'warning');
      return;
    }
    window.open(config.aquaControlUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="lg:h-full lg:overflow-hidden grid grid-cols-1 lg:grid-cols-12 gap-3 items-stretch">
      {/* Left: AquaControl Web launcher card */}
      <SmartCard className="lg:col-span-5 flex flex-col justify-between gap-3 p-4">
        <div className="space-y-3">
          <div className="flex items-center gap-3">
            <IconBadge color={SH.Teal} size={44}>
              <Droplets size={22} />
            </IconBadge>
            <div>
              <h2 className="text-base font-bold text-[#E6EDF7]">AquaControl · Tưới & Bơm tự động</h2>
              <p className="text-xs text-[#8A9BB8]">Mở bảng điều khiển AquaControl Web</p>
            </div>
          </div>

          <p className="text-xs text-[#8A9BB8] leading-relaxed">
            Trên bản Web, nút này thay cho mở ứng dụng Android <code className="text-[#E6EDF7]">vn.aquacontrol</code> bằng cách mở trực tiếp giao diện web cục bộ của bộ điều khiển bơm/tưới.
          </p>

          <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3 text-xs">
            <div className="text-[#8A9BB8]">Địa chỉ đã cấu hình:</div>
            <div className="font-mono-num text-[#E6EDF7] mt-1 break-all">
              {hasAquaUrl ? config.aquaControlUrl : 'Chưa cấu hình (vào Cài đặt → Thông tin & Hệ thống)'}
            </div>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <FlatButton color={SH.Teal} variant="solid" onClick={openAquaControl}>
            <ExternalLink size={14} />
            <span>Mở AquaControl</span>
          </FlatButton>
          <FlatButton color={SH.TextSecondary} onClick={() => navigateDest('settings')}>
            <Settings size={14} />
            <span>Cài đặt URL</span>
          </FlatButton>
        </div>
      </SmartCard>

      {/* Right: 4 Smart Home Scenarios */}
      <div className="lg:col-span-7 flex flex-col min-h-0">
        <SmartCard className="flex-1 min-h-0 flex flex-col gap-3 p-4">
          <SectionHeader
            title="Kịch bản tự động hóa"
            subtitle="Các quy tắc vận hành thông minh theo ngữ cảnh phòng"
            badge={<DemoBadge />}
          />
          <div className="flex-1 min-h-0 grid grid-cols-1 sm:grid-cols-2 gap-3">
            {[
              {
                title: 'Đi ngủ 23:00',
                desc: 'Tắt đèn phòng khách, bật quạt nhẹ, đặt điều hòa 27°C',
                color: SH.Purple,
                icon: <Moon size={18} />,
                active: true,
              },
              {
                title: 'Chào buổi sáng',
                desc: 'Mở rèm phòng khách 100%, tắt chế độ hồng ngoại hành lang',
                color: SH.Amber,
                icon: <Sun size={18} />,
                active: true,
              },
              {
                title: 'An toàn rò rỉ Gas',
                desc: 'Tự bật máy hút mùi bếp mức 3 và hú cảnh báo toàn nhà',
                color: SH.Red,
                icon: <ShieldAlert size={18} />,
                active: true,
              },
              {
                title: 'Cảm biến PIR hành lang',
                desc: 'Tự bật đèn đầu hè & sân quanh vào ban đêm (18:00 - 05:30)',
                color: SH.Cyan,
                icon: <Radio size={18} />,
                active: true,
              },
            ].map((sc) => (
              <div
                key={sc.title}
                className="rounded-xl border border-[#253459] bg-[#0E1627] p-3.5 space-y-2"
              >
                <div className="flex items-center justify-between gap-2">
                  <IconBadge color={sc.color} size={34}>
                    {sc.icon}
                  </IconBadge>
                  <StatusPill label={sc.active ? 'Đang bật' : 'Tạm tắt'} color={SH.Green} />
                </div>
                <div className="text-xs font-bold text-[#E6EDF7]">{sc.title}</div>
                <p className="text-[11px] text-[#8A9BB8] leading-relaxed">{sc.desc}</p>
              </div>
            ))}
          </div>
        </SmartCard>
      </div>
      <Workflow className="hidden" size={1} />
    </div>
  );
}

