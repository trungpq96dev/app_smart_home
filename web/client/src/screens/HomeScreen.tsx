import React, { useEffect, useState } from 'react';
import {
  Activity,
  AirVent,
  Bolt,
  Calendar,
  ChevronRight,
  Cpu,
  DoorClosed,
  DoorOpen,
  Droplets,
  Fan,
  Flame,
  Lightbulb,
  ShieldAlert,
  ShieldCheck,
  Sliders,
  Thermometer,
  Tv,
  Video,
  Wifi,
} from 'lucide-react';
import { AmLich } from '../data/amLich.js';
import { isBridgeSong } from '../data/congtac.js';
import { DemoData } from '../data/demoData.js';
import { coSoLieuDien } from '../data/dien.js';
import { ROOM_MODE_LABELS } from '../data/models.js';
import {
  DemoBadge,
  IconBadge,
  QuickIcon,
  RoomIcon,
  SectionHeader,
  SmartCard,
  StatusPill,
} from '../components/Common.js';
import { QuickActionTile, StatTile } from '../components/Tiles.js';
import { WeatherCard } from '../components/WeatherCard.js';
import { LivingRoomStage } from '../components/stages/LivingRoomStage.js';
import { BedroomStage } from '../components/stages/BedroomStage.js';
import { KitchenStage } from '../components/stages/KitchenStage.js';
import { HallwayStage } from '../components/stages/HallwayStage.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

const THU_VN = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

function formatClockParts(ms: number) {
  const d = new Date(ms);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  const ss = String(d.getSeconds()).padStart(2, '0');
  const thu = THU_VN[d.getDay()];
  const dd = d.getDate();
  const mon = d.getMonth() + 1;
  const yy = d.getFullYear();
  const am = AmLich.tuDuongLich(dd, mon, yy);
  const cc = AmLich.canChi(am);
  const le = AmLich.suKien(dd, mon, am);
  return {
    time: `${hh}:${mm}`,
    sec: ss,
    solar: `${thu}, ${dd}/${mon}/${yy}`,
    lunar: `${am.ngay}/${am.thang}${am.nhuan ? ' nhuận' : ''} ÂL · ${cc.ngay}`,
    le,
  };
}

export function HomeScreen() {
  const {
    rooms,
    selectedRoom,
    selectRoom,
    quickItems,
    toggleQuickItem,
    dien,
    congtac,
    mqttStatus,
    thoiTiet,
    taiThoiTiet,
    wifi,
    oThietBi,
    bamCongTac,
    toggleRoomLight,
    toggleRoomFan,
    toggleRoomAc,
    toggleRoomTv,
    toggleRoomSchedule,
    toggleStove,
    toggleHallFrontLight,
    toggleHallSurroundingLight,
    toggleHallDoorLock,
    toggleHallCamera,
    openRoomDetail,
    openChiTietDien,
    openWifi,
    navigateDest,
  } = useSmartHome();

  const [clockMs, setClockMs] = useState(() => Date.now());
  useEffect(() => {
    const id = window.setInterval(() => setClockMs(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);

  const clk = formatClockParts(clockMs);
  const kitchenRoom = rooms.find((r) => r.id === 'kitchen');
  const gasDanger = kitchenRoom?.gasAlert === true;

  // Render stage for selected room (auto-scales cleanly inside flex container)
  const renderStage = () => {
    if (selectedRoom.id === 'living') return <LivingRoomStage room={selectedRoom} fill />;
    if (selectedRoom.id === 'kitchen') return <KitchenStage room={selectedRoom} fill />;
    if (selectedRoom.id === 'hall') return <HallwayStage room={selectedRoom} fill />;
    return <BedroomStage room={selectedRoom} fill />;
  };

  // Room-aware 4 quick action cards
  const renderRoomQuickActions = () => {
    const r = selectedRoom;
    const renderSwitchTile = (
      thietBiKey: string,
      label: string,
      icon: React.ReactNode,
      color: string,
      demoBat: boolean,
      onDemoToggle: () => void,
      demoSub: string,
    ) => {
      const o = oThietBi(r.id, thietBiKey, demoBat);
      if (o.kind === 'that') {
        const dangBatHien = o.dangCho && o.mong !== null ? o.mong : o.bat === true;
        return (
          <QuickActionTile
            key={`${r.id}-${thietBiKey}`}
            label={o.ten}
            icon={icon}
            color={color}
            on={dangBatHien}
            pending={o.dangCho}
            disabled={o.dangCho}
            subtitle={o.ghiChu || 'Công tắc thật · MQTT'}
            badge={<StatusPill label="Thật" color={o.online ? SH.Green : SH.Red} dot={false} />}
            onClick={() => bamCongTac(r.id, thietBiKey)}
          />
        );
      }
      return (
        <QuickActionTile
          key={`${r.id}-${thietBiKey}`}
          label={label}
          icon={icon}
          color={color}
          on={demoBat}
          subtitle={demoSub}
          badge={<DemoBadge />}
          onClick={onDemoToggle}
        />
      );
    };

    if (r.id === 'kitchen') {
      return (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {renderSwitchTile(
            'den',
            'Đèn bếp',
            <Lightbulb size={19} />,
            SH.Amber,
            r.lightOn,
            () => toggleRoomLight(r.id),
            `Sáng ${r.lightBrightness}%`,
          )}
          {renderSwitchTile(
            'quat',
            'Hút mùi',
            <Fan size={19} />,
            SH.Green,
            r.fanOn,
            () => toggleRoomFan(r.id),
            `Mức ${r.hoodSpeed}/3`,
          )}
          {renderSwitchTile(
            'dieu_hoa',
            'Điều hòa',
            <AirVent size={19} />,
            SH.Purple,
            r.acOn,
            () => toggleRoomAc(r.id),
            `${r.acTemp}°C`,
          )}
          <QuickActionTile
            label="Bếp từ"
            icon={<Flame size={19} />}
            color={SH.Red}
            on={r.stoveOn}
            subtitle={r.stoveOn ? `Mức P${r.stoveLevel}` : 'Đang tắt'}
            badge={<DemoBadge />}
            onClick={() => toggleStove(r.id)}
          />
        </div>
      );
    }

    if (r.id === 'hall') {
      return (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <QuickActionTile
            label="Đèn đầu hè"
            icon={<Lightbulb size={19} />}
            color={SH.Amber}
            on={r.hallFrontLightOn}
            subtitle={`Sáng ${r.hallFrontBrightness}%`}
            badge={<DemoBadge />}
            onClick={() => toggleHallFrontLight(r.id)}
          />
          <QuickActionTile
            label="Đèn xung quanh"
            icon={<Lightbulb size={19} />}
            color={SH.Cyan}
            on={r.hallSurroundingLightOn}
            subtitle={`Sáng ${r.hallSurroundingBrightness}%`}
            badge={<DemoBadge />}
            onClick={() => toggleHallSurroundingLight(r.id)}
          />
          <QuickActionTile
            label="Khóa cửa chính"
            icon={r.hallDoorLocked ? <DoorClosed size={19} /> : <DoorOpen size={19} />}
            color={r.hallDoorLocked ? SH.Green : SH.Amber}
            on={r.hallDoorLocked}
            subtitle={r.hallDoorLocked ? 'Đang khóa' : 'Đang mở'}
            badge={<DemoBadge />}
            onClick={() => toggleHallDoorLock(r.id)}
          />
          <QuickActionTile
            label="Camera giám sát"
            icon={<Video size={19} />}
            color={SH.Blue}
            on={r.hallCameraOn}
            subtitle={r.hallIrMode ? 'Hồng ngoại IR' : 'Chế độ màu'}
            badge={<DemoBadge />}
            onClick={() => toggleHallCamera(r.id)}
          />
        </div>
      );
    }

    // Living or Bedroom
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {renderSwitchTile(
          'den',
          'Đèn phòng',
          <Lightbulb size={19} />,
          SH.Amber,
          r.lightOn,
          () => toggleRoomLight(r.id),
          `Độ sáng ${r.lightBrightness}%`,
        )}
        {renderSwitchTile(
          'quat',
          'Quạt',
          <Fan size={19} />,
          SH.Blue,
          r.fanOn,
          () => toggleRoomFan(r.id),
          `Tốc độ ${r.fanSpeed}/3`,
        )}
        {renderSwitchTile(
          'dieu_hoa',
          'Điều hòa',
          <AirVent size={19} />,
          SH.Purple,
          r.acOn,
          () => toggleRoomAc(r.id),
          `${r.acTemp}°C`,
        )}
        {r.id === 'living' ? (
          <QuickActionTile
            label="Smart TV"
            icon={<Tv size={19} />}
            color={SH.Cyan}
            on={r.tvOn}
            subtitle={r.tvOn ? 'Đang phát 4K HDR' : 'Chế độ chờ'}
            badge={<DemoBadge />}
            onClick={() => toggleRoomTv(r.id)}
          />
        ) : (
          <QuickActionTile
            label="Hẹn giờ ngủ"
            icon={<Calendar size={19} />}
            color={SH.Teal}
            on={r.scheduleOn}
            subtitle={`${r.scheduleLabel} ${r.scheduleTime}`}
            badge={<DemoBadge />}
            onClick={() => toggleRoomSchedule(r.id)}
          />
        )}
      </div>
    );
  };

  const coDien = coSoLieuDien(dien);
  const dienHomNayText = coDien && dien.ngaySanLuong !== null ? dien.ngaySanLuong.toFixed(2) : '—';
  const dienSubText =
    coDien && dien.ngay
      ? `Ngày ${dien.ngay}`
      : mqttStatus.state === 'da_noi'
        ? 'Chờ số từ broker'
        : 'Chưa nối MQTT';

  const realCount = congtac.danhSach.length;
  const realOnline = congtac.danhSach.filter((tb) => congtac.trangThai[tb.ma]?.online).length;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Greeting & Live Clock Banner + 4 Summary StatTiles */}
      <div className="shrink-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
        <SmartCard className="lg:col-span-4 flex flex-col justify-between p-2.5">
          <div className="flex items-start justify-between gap-2">
            <div>
              <div className="text-[11px] font-semibold text-[#8A9BB8]">Bảng điều khiển ngôi nhà</div>
              <div className="mt-0.5 flex items-baseline gap-1 font-mono-num">
                <span className="text-2xl xl:text-3xl font-bold tracking-tight text-[#E6EDF7] leading-none">
                  {clk.time}
                </span>
                <span className="text-xs font-semibold text-[#4F8BFF]">:{clk.sec}</span>
              </div>
              <div className="text-[11px] text-[#E6EDF7] mt-1 font-medium">{clk.solar}</div>
            </div>
            <button
              type="button"
              onClick={() => navigateDest('utilities')}
              className="rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-1.5 text-right hover:border-[#4F8BFF]/50 transition-colors cursor-pointer shrink-0"
            >
              <div className="text-[10px] font-semibold text-[#4F8BFF]">Lịch Âm</div>
              <div className="text-[11px] font-medium text-[#E6EDF7] mt-0.5">{clk.lunar}</div>
              {clk.le && <div className="text-[10px] text-[#F59E0B] mt-0.5">{clk.le}</div>}
            </button>
          </div>
          <div className="mt-2 pt-1.5 border-t border-[#253459] flex items-center justify-between gap-2 text-[11px]">
            <span className="text-[#8A9BB8] truncate">
              {realCount > 0
                ? `${realOnline}/${realCount} công tắc thật · Bridge ${isBridgeSong(congtac) ? 'sống' : 'tắt'}`
                : DemoData.systemStatus}
            </span>
            <button
              type="button"
              onClick={() => navigateDest('devices')}
              className="text-[#4F8BFF] font-semibold hover:underline cursor-pointer shrink-0"
            >
              Tất cả thiết bị →
            </button>
          </div>
        </SmartCard>

        {/* 4 Summary StatTiles */}
        <div className="lg:col-span-8 grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          <StatTile
            label="Nhiệt độ nhà"
            value={DemoData.indoorTemp}
            unit="°C"
            color={SH.Red}
            icon={<Thermometer size={15} />}
            subtitle="Phòng khách"
            badge={<DemoBadge />}
            onClick={() => navigateDest('stats')}
          />
          <StatTile
            label="Độ ẩm"
            value={DemoData.humidity}
            unit="%"
            color={SH.Cyan}
            icon={<Droplets size={15} />}
            subtitle="Mức dễ chịu"
            badge={<DemoBadge />}
            onClick={() => navigateDest('stats')}
          />
          <StatTile
            label="Điện gần nhất"
            value={dienHomNayText}
            unit={dienHomNayText !== '—' ? 'kWh' : undefined}
            color={SH.Blue}
            icon={<Bolt size={15} />}
            subtitle={dienSubText}
            onClick={openChiTietDien}
          />
          <StatTile
            label="An toàn nhà"
            value={gasDanger ? 'CẢNH BÁO' : DemoData.safetyLabel}
            color={gasDanger ? SH.Red : SH.Green}
            icon={gasDanger ? <ShieldAlert size={15} /> : <ShieldCheck size={15} />}
            subtitle={gasDanger ? `Gas ${kitchenRoom?.gasPpm} ppm!` : 'Không rò rỉ · Cửa khóa'}
            badge={!gasDanger ? <DemoBadge /> : undefined}
            onClick={() => openRoomDetail(gasDanger ? 'kitchen' : 'hall')}
          />
        </div>
      </div>

      {/* Main Content Split */}
      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
        {/* Left: 2.5D Interactive Room Showcase + Room Quick Controls */}
        <div className="lg:col-span-8 flex flex-col min-h-0 gap-2.5">
          <SmartCard className="flex-1 min-h-0 flex flex-col gap-2 p-3">
            <div className="shrink-0 flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <span className="text-xs font-bold text-[#E6EDF7] whitespace-nowrap">Không gian 2.5D</span>
                <span className="text-[11px] text-[#8A9BB8] truncate">
                  {selectedRoom.name} · {ROOM_MODE_LABELS[selectedRoom.mode]}
                </span>
              </div>
              <button
                type="button"
                onClick={() => openRoomDetail(selectedRoom.id)}
                className="inline-flex items-center gap-1 rounded-xl bg-[#4F8BFF]/15 border border-[#4F8BFF]/40 px-2.5 py-1 text-[11px] font-semibold text-[#4F8BFF] hover:bg-[#4F8BFF]/25 cursor-pointer shrink-0"
              >
                <Sliders size={13} />
                <span>Điều khiển chi tiết</span>
              </button>
            </div>

            {/* Room selector tabs */}
            <div className="shrink-0 flex items-center gap-1.5 overflow-x-auto pb-0.5">
              {rooms.map((r) => {
                const active = r.id === selectedRoom.id;
                return (
                  <button
                    key={r.id}
                    type="button"
                    onClick={() => selectRoom(r.id)}
                    className="inline-flex items-center gap-1.5 rounded-xl px-2.5 py-1 text-[11px] font-semibold whitespace-nowrap transition-all cursor-pointer shrink-0"
                    style={{
                      backgroundColor: active ? tint(r.accent, 0.2) : SH.Surface,
                      border: `1px solid ${active ? r.accent : SH.Border}`,
                      color: active ? '#FFFFFF' : SH.TextSecondary,
                    }}
                  >
                    <span style={{ color: r.accent }}>
                      <RoomIcon icon={r.icon} size={13} />
                    </span>
                    <span>{r.name}</span>
                  </button>
                );
              })}
            </div>

            {/* Auto-scaling 2.5D Room Stage */}
            <div className="flex-1 min-h-[200px] flex flex-col">
              {renderStage()}
            </div>

            {/* Room-specific 4 quick action cards */}
            <div className="shrink-0">{renderRoomQuickActions()}</div>
          </SmartCard>

          {/* Compact Single-Row Global House Quick Bar */}
          <SmartCard className="shrink-0 flex flex-wrap items-center justify-between gap-2 px-3 py-2">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-[#E6EDF7]">Bật / Tắt nhanh toàn nhà</span>
              <DemoBadge />
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 flex-1 sm:max-w-xl">
              {quickItems.map((q) => (
                <button
                  key={q.id}
                  type="button"
                  onClick={() => toggleQuickItem(q.id)}
                  className="flex items-center justify-between gap-2 rounded-xl px-2.5 py-1.5 text-xs font-semibold transition-all active:scale-95 cursor-pointer"
                  style={{
                    backgroundColor: q.on ? tint(q.color, 0.16) : SH.Surface,
                    border: `1px solid ${q.on ? tint(q.color, 0.45) : SH.Border}`,
                    color: q.on ? '#FFFFFF' : SH.TextSecondary,
                  }}
                >
                  <span className="inline-flex items-center gap-1.5 min-w-0">
                    <span style={{ color: q.on ? q.color : SH.TextDim }}>
                      <QuickIcon icon={q.icon} size={14} />
                    </span>
                    <span className="truncate">{q.label}</span>
                  </span>
                  <span
                    className="text-[10px] font-bold shrink-0"
                    style={{ color: q.on ? q.color : SH.TextDim }}
                  >
                    {q.on ? 'BẬT' : 'TẮT'}
                  </span>
                </button>
              ))}
            </div>
          </SmartCard>
        </div>

        {/* Right: Weather, Power, WiFi & Shortcuts */}
        <div className="lg:col-span-4 flex flex-col min-h-0 gap-2.5 justify-between">
          <WeatherCard thoiTiet={thoiTiet} onRetry={taiThoiTiet} />

          {/* Electricity Snapshot Card */}
          <SmartCard onClick={openChiTietDien} className="space-y-2 p-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 min-w-0">
                <IconBadge color={SH.Blue} size={32}>
                  <Bolt size={16} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="text-xs font-bold text-[#E6EDF7] truncate">Điện EVN · Trực tiếp</div>
                  <div className="text-[11px] text-[#8A9BB8] truncate">
                    {coDien ? `Cập nhật ${dien.capNhat || 'gần đây'}` : 'Bấm để xem chi tiết & cấu hình'}
                  </div>
                </div>
              </div>
              <ChevronRight size={16} className="text-[#8A9BB8] shrink-0" />
            </div>

            {coDien ? (
              <div className="grid grid-cols-2 gap-2">
                <div className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5">
                  <div className="text-[10px] text-[#8A9BB8]">Tháng này</div>
                  <div className="font-mono-num text-xs font-bold text-[#E6EDF7]">
                    {dien.thangSanLuong !== null ? `${dien.thangSanLuong.toFixed(1)} kWh` : '—'}
                  </div>
                </div>
                <div className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5">
                  <div className="text-[10px] text-[#8A9BB8]">Tiền điện ước</div>
                  <div className="font-mono-num text-xs font-bold text-[#22C55E]">
                    {dien.thangTien !== null
                      ? `${Math.round(dien.thangTien).toLocaleString('vi-VN')} đ`
                      : '—'}
                  </div>
                </div>
              </div>
            ) : (
              <div className="rounded-xl bg-[#0E1627] border border-dashed border-[#253459] px-2.5 py-1.5 text-[11px] text-[#8A9BB8]">
                Chưa có số liệu điện từ MQTT. Bấm vào pill góc trên để cài đặt.
              </div>
            )}
          </SmartCard>

          {/* WiFi Router Snapshot Card */}
          <SmartCard onClick={openWifi} className="p-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 min-w-0">
                <IconBadge color={SH.Teal} size={32}>
                  <Wifi size={16} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-bold text-[#E6EDF7] truncate">{wifi.info.ssid}</span>
                    {!wifi.isRealData && <DemoBadge />}
                  </div>
                  <div className="text-[11px] text-[#8A9BB8] truncate">
                    {wifi.devices.filter((d) => d.isOnline).length} thiết bị online · ↓{wifi.info.downloadMbps.toFixed(1)} Mbps
                  </div>
                </div>
              </div>
              <ChevronRight size={16} className="text-[#8A9BB8] shrink-0" />
            </div>
          </SmartCard>

          {/* Quick Navigation Grid */}
          <SmartCard className="p-3 space-y-2">
            <div className="text-xs font-bold text-[#E6EDF7]">Truy cập nhanh</div>
            <div className="grid grid-cols-2 gap-1.5">
              <button
                type="button"
                onClick={() => navigateDest('camera')}
                className="flex items-center gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-2 text-xs font-semibold text-[#E6EDF7] hover:border-[#4F8BFF]/50 cursor-pointer"
              >
                <Video size={14} className="text-[#4F8BFF]" />
                <span>Camera</span>
              </button>
              <button
                type="button"
                onClick={() => navigateDest('sensors')}
                className="flex items-center gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-2 text-xs font-semibold text-[#E6EDF7] hover:border-[#4F8BFF]/50 cursor-pointer"
              >
                <Activity size={14} className="text-[#22C55E]" />
                <span>Cảm biến & Sạc</span>
              </button>
              <button
                type="button"
                onClick={() => navigateDest('server')}
                className="flex items-center gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-2 text-xs font-semibold text-[#E6EDF7] hover:border-[#4F8BFF]/50 cursor-pointer"
              >
                <Cpu size={14} className="text-[#A78BFA]" />
                <span>Máy chủ</span>
              </button>
              <button
                type="button"
                onClick={() => navigateDest('settings')}
                className="flex items-center gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-2 text-xs font-semibold text-[#E6EDF7] hover:border-[#4F8BFF]/50 cursor-pointer"
              >
                <Sliders size={14} className="text-[#F59E0B]" />
                <span>Cài đặt</span>
              </button>
            </div>
          </SmartCard>
        </div>
      </div>
    </div>
  );
}

