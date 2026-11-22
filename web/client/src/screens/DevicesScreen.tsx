import React from 'react';
import { AirVent, ChevronRight, Fan, Lightbulb, Power } from 'lucide-react';
import { isBridgeSong } from '../data/congtac.js';
import { ROOM_MODE_LABELS } from '../data/models.js';
import {
  DemoBadge,
  IconBadge,
  RoomIcon,
  SectionHeader,
  SmartCard,
  StatusPill,
} from '../components/Common.js';
import { DeviceSwitchRow } from '../components/Tiles.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

export function DevicesScreen() {
  const {
    rooms,
    congtac,
    choCongTacIds,
    oThietBi,
    bamCongTac,
    toggleRoomLight,
    toggleRoomFan,
    toggleRoomAc,
    openRoomDetail,
  } = useSmartHome();

  const bridgeOk = isBridgeSong(congtac);

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Real Hardware Switches Section */}
      {congtac.danhSach.length > 0 && (
        <SmartCard className="shrink-0 space-y-2 p-2.5">
          <SectionHeader
            title={`Công tắc thật ngoài đời (${congtac.danhSach.length})`}
            subtitle="Điều khiển trực tiếp qua MQTT → Python Switch Bridge"
            badge={
              <StatusPill
                label={bridgeOk ? 'Bộ nối đang chạy' : 'Bộ nối chưa chạy'}
                color={bridgeOk ? SH.Green : SH.Amber}
              />
            }
          />
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
            {congtac.danhSach.map((tb) => {
              const tt = congtac.trangThai[tb.ma];
              const cho = oThietBi(tb.phong, tb.thietBi, false);
              const pending = cho.kind === 'that' ? cho.dangCho : Boolean(choCongTacIds[tb.ma]);
              const online = tt?.online ?? false;
              const bat = cho.kind === 'that' && cho.dangCho && cho.mong !== null ? cho.mong : (tt?.bat ?? null);
              return (
                <DeviceSwitchRow
                  key={tb.ma}
                  icon={<Power size={16} />}
                  label={tb.ten}
                  subtitle={`Phòng: ${tb.phong} · ${tb.he}${tt?.lyDoLoi ? ` · ${tt.lyDoLoi}` : ''}`}
                  color={SH.Green}
                  checked={bat === true}
                  disabled={pending}
                  pending={pending}
                  badge={<StatusPill label={online ? 'Online' : 'Offline'} color={online ? SH.Green : SH.Red} dot={false} />}
                  onToggle={() => bamCongTac(tb.phong, tb.thietBi)}
                />
              );
            })}
          </div>
        </SmartCard>
      )}

      {/* Rooms Grid fills remaining height */}
      <div className="flex-1 min-h-0 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 lg:grid-rows-2 gap-2.5">
        {rooms.map((r) => {
          const den = oThietBi(r.id, 'den', r.lightOn);
          const quat = oThietBi(r.id, 'quat', r.fanOn);
          const dieuHoa = oThietBi(r.id, 'dieu_hoa', r.acOn);
          const countOn = [
            den.kind === 'that' ? den.bat === true : r.lightOn,
            quat.kind === 'that' ? quat.bat === true : r.fanOn,
            dieuHoa.kind === 'that' ? dieuHoa.bat === true : r.acOn,
            r.tvOn,
            r.stoveOn,
          ].filter(Boolean).length;

          return (
            <SmartCard key={r.id} className="p-3 flex flex-col justify-between gap-2 min-h-0">
              <div className="flex items-center justify-between gap-2 shrink-0">
                <div className="flex items-center gap-2 min-w-0">
                  <IconBadge color={r.accent} size={34}>
                    <RoomIcon icon={r.icon} size={17} />
                  </IconBadge>
                  <div className="min-w-0">
                    <div className="text-sm font-bold text-[#E6EDF7] truncate">{r.name}</div>
                    <div className="flex items-center gap-2">
                      <span className="text-[11px] font-semibold" style={{ color: r.accent }}>
                        {ROOM_MODE_LABELS[r.mode]}
                      </span>
                      <span className="text-[11px] text-[#8A9BB8]">· {countOn} thiết bị bật</span>
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => openRoomDetail(r.id)}
                  className="inline-flex items-center gap-1 rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-1 text-xs font-semibold text-[#4F8BFF] hover:bg-[#1D2B4C] cursor-pointer shrink-0"
                >
                  <span>Chi tiết</span>
                  <ChevronRight size={13} />
                </button>
              </div>

              <div className="space-y-1.5 flex-1 flex flex-col justify-center">
                <DeviceSwitchRow
                  icon={<Lightbulb size={16} />}
                  label={den.kind === 'that' ? den.ten : 'Đèn chiếu sáng'}
                  subtitle={den.kind === 'that' ? (den.ghiChu || 'Công tắc thật') : `Sáng ${r.lightBrightness}%`}
                  color={SH.Amber}
                  checked={den.kind === 'that' ? (den.dangCho && den.mong !== null ? den.mong : den.bat === true) : r.lightOn}
                  disabled={den.kind === 'that' ? den.dangCho : false}
                  pending={den.kind === 'that' ? den.dangCho : false}
                  badge={den.kind === 'that' ? <StatusPill label="Thật" color={SH.Green} dot={false} /> : <DemoBadge />}
                  onToggle={() => (den.kind === 'that' ? bamCongTac(r.id, 'den') : toggleRoomLight(r.id))}
                />
                <DeviceSwitchRow
                  icon={<Fan size={16} />}
                  label={quat.kind === 'that' ? quat.ten : r.id === 'kitchen' ? 'Máy hút mùi' : 'Quạt'}
                  subtitle={quat.kind === 'that' ? (quat.ghiChu || 'Công tắc thật') : r.id === 'kitchen' ? `Mức ${r.hoodSpeed}/3` : `Tốc độ ${r.fanSpeed}/3`}
                  color={SH.Blue}
                  checked={quat.kind === 'that' ? (quat.dangCho && quat.mong !== null ? quat.mong : quat.bat === true) : r.fanOn}
                  disabled={quat.kind === 'that' ? quat.dangCho : false}
                  pending={quat.kind === 'that' ? quat.dangCho : false}
                  badge={quat.kind === 'that' ? <StatusPill label="Thật" color={SH.Green} dot={false} /> : <DemoBadge />}
                  onToggle={() => (quat.kind === 'that' ? bamCongTac(r.id, 'quat') : toggleRoomFan(r.id))}
                />
                <DeviceSwitchRow
                  icon={<AirVent size={16} />}
                  label={dieuHoa.kind === 'that' ? dieuHoa.ten : 'Điều hòa'}
                  subtitle={dieuHoa.kind === 'that' ? (dieuHoa.ghiChu || 'Công tắc thật') : `${r.acTemp}°C`}
                  color={SH.Purple}
                  checked={dieuHoa.kind === 'that' ? (dieuHoa.dangCho && dieuHoa.mong !== null ? dieuHoa.mong : dieuHoa.bat === true) : r.acOn}
                  disabled={dieuHoa.kind === 'that' ? dieuHoa.dangCho : false}
                  pending={dieuHoa.kind === 'that' ? dieuHoa.dangCho : false}
                  badge={dieuHoa.kind === 'that' ? <StatusPill label="Thật" color={SH.Green} dot={false} /> : <DemoBadge />}
                  onToggle={() => (dieuHoa.kind === 'that' ? bamCongTac(r.id, 'dieu_hoa') : toggleRoomAc(r.id))}
                />
              </div>
            </SmartCard>
          );
        })}
      </div>
    </div>
  );
}

