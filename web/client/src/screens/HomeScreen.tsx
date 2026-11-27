import React, { useEffect, useRef, useState } from 'react';
import {
  Activity,
  AirVent,
  Blinds,
  Bolt,
  ChevronLeft,
  ChevronRight,
  Cpu,
  DoorClosed,
  DoorOpen,
  Droplets,
  Fan,
  Flame,
  Lightbulb,
  Moon,
  ShieldAlert,
  ShieldCheck,
  Sliders,
  Thermometer,
  Timer,
  Video,
  Wifi,
} from 'lucide-react';
import { AmLich } from '../data/amLich.js';
import { isBridgeSong, oThietBiBatHienThi, oThietBiRoRang } from '../data/congtac.js';
import { DemoData } from '../data/demoData.js';
import { coSoLieuDien } from '../data/dien.js';
import { HALLWAY_VIEWS, stoveWatts, tenCheDoPhong, type RoomUi } from '../data/models.js';
import {
  DemoBadge,
  IconBadge,
  RoomIcon,
  SmartCard,
  StatusPill,
  WifiDevIcon,
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
const WIFI_BADGE_COLORS = [SH.Blue, SH.Teal, SH.Purple, SH.Green, SH.Amber, SH.Cyan, SH.Orange];

function loiChaoTheoGio(hour: number): string {
  if (hour < 11) return 'Chào buổi sáng!';
  if (hour < 13) return 'Chào buổi trưa!';
  if (hour < 18) return 'Chào buổi chiều!';
  return 'Chào buổi tối!';
}

function formatClockParts(ms: number) {
  const d = new Date(ms);
  const h = d.getHours();
  const hh = String(h).padStart(2, '0');
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
    greeting: loiChaoTheoGio(h),
    time: `${hh}:${mm}`,
    sec: ss,
    solar: `${thu}, ${String(dd).padStart(2, '0')}/${String(mon).padStart(2, '0')}/${yy}`,
    lunar: `Âm ${am.ngay}/${am.thang}${am.nhuan ? ' (nhuận)' : ''} · Năm ${cc.nam}`,
    le,
  };
}

function RoomStatusChip({
  label,
  active,
  danger = false,
}: {
  label: string;
  active: boolean;
  danger?: boolean;
}) {
  return (
    <span
      className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10.5px] font-semibold backdrop-blur-md whitespace-nowrap"
      style={{
        backgroundColor: danger
          ? 'rgba(185, 28, 28, 0.78)'
          : active
            ? 'rgba(15, 23, 42, 0.78)'
            : 'rgba(15, 23, 42, 0.58)',
        border: `1px solid ${
          danger
            ? 'rgba(252, 165, 165, 0.65)'
            : active
              ? 'rgba(96, 165, 250, 0.45)'
              : 'rgba(148, 163, 184, 0.22)'
        }`,
        color: danger ? '#FEE2E2' : active ? '#F8FAFC' : '#94A3B8',
      }}
    >
      <span
        className="h-1.5 w-1.5 rounded-full"
        style={{
          backgroundColor: danger ? SH.Red : active ? SH.Green : '#64748B',
        }}
      />
      <span>{label}</span>
    </span>
  );
}

export function HomeScreen() {
  const {
    config,
    rooms,
    selectedRoom,
    selectRoom,
    dien,
    congtac,
    mqttStatus,
    gatewayOnline,
    thoiTiet,
    taiThoiTiet,
    wifi,
    oThietBi,
    bamCongTac,
    toggleRoomLight,
    toggleRoomFan,
    toggleRoomAc,
    toggleCurtain,
    toggleRoomSchedule,
    setRoomMode,
    toggleStove,
    toggleHallFrontLight,
    toggleHallDoorLock,
    toggleHallCamera,
    openRoomDetail,
    openChiTietDien,
    openWifi,
    navigateDest,
  } = useSmartHome();

  const [clockMs, setClockMs] = useState(() => Date.now());
  const swipeStartRef = useRef<{ x: number; y: number } | null>(null);
  const didSwipeRef = useRef(false);

  useEffect(() => {
    const id = window.setInterval(() => setClockMs(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);

  const clk = formatClockParts(clockMs);
  const kitchenRoom = rooms.find((r) => r.id === 'kitchen');
  const gasDanger = kitchenRoom?.gasAlert === true;

  const curRoomIdx = Math.max(
    0,
    rooms.findIndex((x) => x.id === selectedRoom.id),
  );

  const stepRoomPager = (delta: number) => {
    if (rooms.length === 0) return;
    const nextIdx = (curRoomIdx + delta + rooms.length) % rooms.length;
    selectRoom(rooms[nextIdx].id);
  };

  // Optimistic real-switch state projected onto 2.5D room stage
  const r = selectedRoom;
  const denObj = oThietBi(r.id, 'den', r.lightOn);
  const quatObj = oThietBi(r.id, 'quat', r.fanOn);
  const dhObj = oThietBi(r.id, 'dieu_hoa', r.acOn);
  const hasRealInRoom =
    denObj.kind === 'that' ||
    quatObj.kind === 'that' ||
    (r.id !== 'kitchen' && r.id !== 'hall' && dhObj.kind === 'that');

  const stageRoom: RoomUi = {
    ...r,
    lightOn: denObj.kind === 'that' ? oThietBiBatHienThi(denObj, r.lightOn) : r.lightOn,
    fanOn: quatObj.kind === 'that' ? oThietBiBatHienThi(quatObj, r.fanOn) : r.fanOn,
    acOn: dhObj.kind === 'that' ? oThietBiBatHienThi(dhObj, r.acOn) : r.acOn,
  };

  const stageDanger =
    (r.id === 'kitchen' && r.gasAlert) || (r.id === 'hall' && r.hallMotionDetected);
  const hallViewLabel =
    HALLWAY_VIEWS.find((v) => v.id === r.hallView)?.label ?? 'Đầu hè';
  const hallLightsOnCount =
    (stageRoom.lightOn ? 1 : 0) +
    (r.hallFrontLightOn ? 1 : 0) +
    (r.hallSurroundingLightOn ? 1 : 0);

  const renderStage = () => {
    if (stageRoom.id === 'living') return <LivingRoomStage room={stageRoom} fill compact />;
    if (stageRoom.id === 'kitchen') return <KitchenStage room={stageRoom} fill compact />;
    if (stageRoom.id === 'hall') return <HallwayStage room={stageRoom} fill compact />;
    return <BedroomStage room={stageRoom} fill compact />;
  };

  const renderStatusChips = () => {
    if (r.id === 'living') {
      const thirdChip = r.tvOn
        ? { label: 'TV Bật', active: true }
        : stageRoom.fanOn
          ? { label: 'Quạt Bật', active: true }
          : { label: `Rèm ${r.curtainOpen}%`, active: r.curtainOpen > 0 };
      return (
        <>
          <RoomStatusChip
            label={stageRoom.lightOn ? `Đèn ${r.lightBrightness}%` : 'Đèn tắt'}
            active={stageRoom.lightOn}
          />
          <RoomStatusChip
            label={stageRoom.acOn ? `ĐH ${r.acTemp}°C` : 'ĐH tắt'}
            active={stageRoom.acOn}
          />
          <RoomStatusChip label={thirdChip.label} active={thirdChip.active} />
        </>
      );
    }
    if (r.id === 'kitchen') {
      return (
        <>
          <RoomStatusChip
            label={stageRoom.lightOn ? `Đèn ${r.lightBrightness}%` : 'Đèn tắt'}
            active={stageRoom.lightOn}
          />
          <RoomStatusChip
            label={stageRoom.fanOn ? `Hút C${r.hoodSpeed}` : 'Hút tắt'}
            active={stageRoom.fanOn}
          />
          <RoomStatusChip
            label={r.stoveOn ? `Bếp P${r.stoveLevel}` : 'Bếp tắt'}
            active={r.stoveOn}
          />
        </>
      );
    }
    if (r.id === 'hall') {
      return (
        <>
          <RoomStatusChip label={`Đèn ${hallLightsOnCount}/3`} active={hallLightsOnCount > 0} />
          <RoomStatusChip
            label={r.hallDoorLocked ? 'Đã khóa' : 'Đang mở'}
            active={r.hallDoorLocked}
            danger={!r.hallDoorLocked}
          />
          <RoomStatusChip
            label={!r.hallCameraOn ? 'Cam tắt' : r.hallIrMode ? 'Cam IR' : 'Cam'}
            active={r.hallCameraOn}
          />
        </>
      );
    }
    return (
      <>
        <RoomStatusChip
          label={stageRoom.lightOn ? `Đèn ${r.lightBrightness}%` : 'Đèn tắt'}
          active={stageRoom.lightOn}
        />
        <RoomStatusChip
          label={stageRoom.fanOn ? `Quạt Cấp ${r.fanSpeed}` : 'Quạt tắt'}
          active={stageRoom.fanOn}
        />
        <RoomStatusChip
          label={stageRoom.acOn ? `ĐH ${r.acTemp}°C` : 'ĐH tắt'}
          active={stageRoom.acOn}
        />
      </>
    );
  };

  // Room-aware 4 contextual quick action cards (1:1 with Android HomeScreen.kt)
  const renderRoomQuickActions = () => {
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
        const ro = oThietBiRoRang(o);
        const dangBatHien = oThietBiBatHienThi(o, demoBat);
        return (
          <QuickActionTile
            key={`${r.id}-${thietBiKey}`}
            label={o.ten}
            icon={icon}
            color={color}
            on={dangBatHien}
            unknownState={!ro}
            subtitleWarning={!ro}
            pending={o.dangCho}
            disabled={o.dangCho}
            subtitle={o.ghiChu || demoSub}
            badge={
              <StatusPill
                label="THẬT"
                color={o.online && o.bridgeSong ? SH.Green : SH.Amber}
                dot={false}
              />
            }
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
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          {renderSwitchTile(
            'den',
            'Đèn bếp',
            <Lightbulb size={18} />,
            SH.Amber,
            r.lightOn,
            () => toggleRoomLight(r.id),
            `Sáng ${r.lightBrightness}%`,
          )}
          {renderSwitchTile(
            'quat',
            'Hút mùi',
            <Fan size={18} />,
            SH.Green,
            r.fanOn,
            () => toggleRoomFan(r.id),
            `Cấp ${r.hoodSpeed}/3`,
          )}
          <QuickActionTile
            label="Bếp từ"
            icon={<Flame size={18} />}
            color={SH.Orange}
            on={r.stoveOn}
            subtitle={r.stoveOn ? `Mức P${r.stoveLevel} · ${stoveWatts(r.stoveLevel)}W` : 'Đang tắt'}
            badge={<DemoBadge />}
            onClick={() => toggleStove(r.id)}
          />
          <QuickActionTile
            label="Hẹn giờ"
            icon={<Timer size={18} />}
            color={SH.Teal}
            on={r.scheduleOn}
            statusText={r.scheduleOn ? '15 phút' : 'TẮT'}
            subtitle={r.scheduleOn ? 'Tự tắt hút mùi sau 15 phút' : 'Tắt hẹn giờ'}
            badge={<DemoBadge />}
            onClick={() => toggleRoomSchedule(r.id)}
          />
        </div>
      );
    }

    if (r.id === 'hall') {
      return (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          {renderSwitchTile(
            'den',
            'Đèn sảnh',
            <Lightbulb size={18} />,
            SH.Amber,
            r.lightOn,
            () => toggleRoomLight(r.id),
            `Sáng ${r.lightBrightness}%`,
          )}
          <QuickActionTile
            label="Đèn trước"
            icon={<Lightbulb size={18} />}
            color={SH.Cyan}
            on={r.hallFrontLightOn}
            subtitle={`Sáng ${r.hallFrontBrightness}%`}
            badge={<DemoBadge />}
            onClick={() => toggleHallFrontLight(r.id)}
          />
          <QuickActionTile
            label="Khóa cửa"
            icon={r.hallDoorLocked ? <DoorClosed size={18} /> : <DoorOpen size={18} />}
            color={r.hallDoorLocked ? SH.Green : SH.Red}
            on={r.hallDoorLocked}
            statusText={r.hallDoorLocked ? 'ĐÃ KHÓA' : 'ĐANG MỞ'}
            subtitle={r.hallDoorLocked ? 'Đã chốt an toàn' : 'Cửa đang mở khóa!'}
            subtitleWarning={!r.hallDoorLocked}
            badge={<DemoBadge />}
            onClick={() => toggleHallDoorLock(r.id)}
          />
          <QuickActionTile
            label="Camera"
            icon={<Video size={18} />}
            color={SH.Blue}
            on={r.hallCameraOn}
            subtitle={!r.hallCameraOn ? 'Tắt' : r.hallIrMode ? 'Hồng ngoại IR' : 'Chế độ màu'}
            badge={<DemoBadge />}
            onClick={() => toggleHallCamera(r.id)}
          />
        </div>
      );
    }

    // Living or Bedroom
    return (
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        {renderSwitchTile(
          'den',
          'Đèn phòng',
          <Lightbulb size={18} />,
          SH.Amber,
          r.lightOn,
          () => toggleRoomLight(r.id),
          `Độ sáng ${r.lightBrightness}%`,
        )}
        {renderSwitchTile(
          'quat',
          'Quạt trần',
          <Fan size={18} />,
          SH.Blue,
          r.fanOn,
          () => toggleRoomFan(r.id),
          r.id === 'living'
            ? r.fanOn
              ? 'Đang quay'
              : 'Đang tắt'
            : `Tốc độ ${r.fanSpeed}/3`,
        )}
        {renderSwitchTile(
          'dieu_hoa',
          'Điều hòa',
          <AirVent size={18} />,
          SH.Purple,
          r.acOn,
          () => toggleRoomAc(r.id),
          `${r.acTemp}°C`,
        )}
        {r.id === 'living' ? (
          <QuickActionTile
            label="Rèm cửa"
            icon={<Blinds size={18} />}
            color={SH.Teal}
            on={r.curtainOpen > 0}
            statusText={r.curtainOpen > 0 ? `${r.curtainOpen}%` : 'ĐÓNG'}
            subtitle={r.curtainOpen > 0 ? `Đang mở ${r.curtainOpen}%` : 'Đang đóng kín'}
            badge={<DemoBadge />}
            onClick={() => toggleCurtain(r.id)}
          />
        ) : (
          <QuickActionTile
            label="Chế độ ngủ"
            icon={<Moon size={18} />}
            color={SH.Purple}
            on={r.mode === 'SLEEP'}
            subtitle={r.mode === 'SLEEP' ? 'Đèn dịu · ĐH 27°C' : 'Bấm để chuyển sang Ngủ'}
            badge={<DemoBadge />}
            onClick={() => setRoomMode(r.id, r.mode === 'SLEEP' ? 'HOME' : 'SLEEP')}
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
  const wifiPreview = wifi.devices.slice(0, 7);
  const wifiExtraCount = Math.max(0, wifi.devices.length - wifiPreview.length);

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Greeting & Live Clock Banner + 4 Summary StatTiles */}
      <div className="shrink-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
        <SmartCard className="lg:col-span-4 flex flex-col justify-between p-2.5">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="text-[11px] font-medium text-[#8A9BB8]">Nhà thông minh của bạn</span>
                <StatusPill
                  label={gatewayOnline ? 'Trực tuyến' : 'Mất Gateway'}
                  color={gatewayOnline ? SH.Green : SH.Red}
                />
              </div>
              <div className="text-sm font-bold text-[#E6EDF7] mt-0.5">{clk.greeting}</div>
              <div className="mt-1 flex items-center gap-1.5 flex-wrap">
                {realCount === 0 && <DemoBadge label="DỮ LIỆU MẪU" />}
                <span className="text-[11px] text-[#8A9BB8] truncate">
                  {realCount > 0
                    ? `${realOnline}/${realCount} công tắc thật · Bridge ${isBridgeSong(congtac) ? 'sống' : 'tắt'}`
                    : DemoData.systemStatus}
                </span>
              </div>
            </div>

            {/* DongHoLich Clickable Pill */}
            <button
              type="button"
              onClick={() => navigateDest('utilities')}
              className="rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-1.5 text-right hover:border-[#4F8BFF]/50 transition-colors cursor-pointer shrink-0"
              title="Mở Lịch Vạn Niên"
            >
              <div className="flex items-baseline justify-end gap-0.5 font-mono-num">
                <span className="text-lg xl:text-xl font-bold tracking-tight text-[#E6EDF7] leading-none">
                  {clk.time}
                </span>
                <span className="text-[11px] font-semibold text-[#4F8BFF]">:{clk.sec}</span>
              </div>
              <div className="text-[10.5px] text-[#CBD5E1] mt-1 font-medium">{clk.solar}</div>
              <div className="text-[10px] text-[#F59E0B] mt-0.5">{clk.lunar}</div>
              {clk.le && <div className="text-[9.5px] text-[#34D399] mt-0.5">{clk.le}</div>}
            </button>
          </div>
        </SmartCard>

        {/* 4 Summary StatTiles */}
        <div className="lg:col-span-8 grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          <StatTile
            label="Nhiệt độ trong nhà"
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
            label="Tổng quan An toàn"
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
        {/* Left: Full-bleed 2.5D RoomPager + Contextual Quick Actions + WiFi Strip */}
        <div className="lg:col-span-8 flex flex-col min-h-0 gap-2.5">
          <SmartCard className="flex-1 min-h-0 flex flex-col gap-2 p-2.5">
            {/* Room Selector Tabs */}
            <div className="shrink-0 flex items-center gap-1.5 overflow-x-auto pb-0.5">
              {rooms.map((rm) => {
                const active = rm.id === r.id;
                return (
                  <button
                    key={rm.id}
                    type="button"
                    onClick={() => selectRoom(rm.id)}
                    className="inline-flex items-center gap-1.5 rounded-xl px-2.5 py-1 text-[11px] font-semibold whitespace-nowrap transition-all cursor-pointer shrink-0"
                    style={{
                      backgroundColor: active ? tint(rm.accent, 0.2) : SH.Surface,
                      border: `1px solid ${active ? rm.accent : SH.Border}`,
                      color: active ? '#FFFFFF' : SH.TextSecondary,
                    }}
                  >
                    <span style={{ color: rm.accent }}>
                      <RoomIcon icon={rm.icon} size={13} />
                    </span>
                    <span>{rm.name}</span>
                  </button>
                );
              })}
            </div>

            {/* Full-bleed 2.5D RoomPager Viewport */}
            <div
              role="button"
              tabIndex={0}
              onClick={() => {
                if (didSwipeRef.current) {
                  didSwipeRef.current = false;
                  return;
                }
                openRoomDetail(r.id);
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') openRoomDetail(r.id);
                if (e.key === 'ArrowLeft') stepRoomPager(-1);
                if (e.key === 'ArrowRight') stepRoomPager(1);
              }}
              onTouchStart={(e) => {
                const t = e.touches[0];
                if (t) swipeStartRef.current = { x: t.clientX, y: t.clientY };
              }}
              onTouchEnd={(e) => {
                const st = swipeStartRef.current;
                swipeStartRef.current = null;
                const t = e.changedTouches[0];
                if (!st || !t) return;
                const dx = t.clientX - st.x;
                const dy = t.clientY - st.y;
                if (Math.abs(dx) > 42 && Math.abs(dx) > Math.abs(dy) * 1.2) {
                  didSwipeRef.current = true;
                  stepRoomPager(dx < 0 ? 1 : -1);
                }
              }}
              className="relative flex-1 min-h-[210px] rounded-2xl overflow-hidden flex flex-col cursor-pointer select-none transition-colors"
              style={{
                border: stageDanger ? `2px solid ${SH.Red}` : `1px solid ${SH.Border}`,
                boxShadow: stageDanger ? `0 0 24px -4px ${tint(SH.Red, 0.5)}` : undefined,
              }}
            >
              {renderStage()}

              {/* Top Gradient Overlay */}
              <div
                className="pointer-events-none absolute inset-x-0 top-0 flex items-start justify-between gap-2 px-3 pt-2.5 pb-6"
                style={{
                  background:
                    'linear-gradient(to bottom, rgba(8,13,24,0.82) 0%, rgba(8,13,24,0.38) 62%, rgba(8,13,24,0) 100%)',
                }}
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="text-sm font-bold text-white drop-shadow">{r.name}</span>
                    {hasRealInRoom ? (
                      <StatusPill label="THẬT" color={SH.Green} dot={false} />
                    ) : (
                      <DemoBadge />
                    )}
                  </div>
                  <div className="text-[11px] text-[#CBD5E1] truncate">
                    Chế độ: {tenCheDoPhong(r.id, r.mode)}
                    {r.id === 'hall' ? ` • ${hallViewLabel}` : ''}
                  </div>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  {r.id === 'kitchen' && r.gasAlert && (
                    <StatusPill label="RÒ RỈ GAS" color={SH.Red} pulse />
                  )}
                  {r.id === 'hall' && r.hallMotionDetected && (
                    <StatusPill label="CHUYỂN ĐỘNG" color={SH.Red} pulse />
                  )}
                </div>
              </div>

              {/* Left / Right Pager Buttons */}
              <button
                type="button"
                aria-label="Phòng trước"
                onClick={(e) => {
                  e.stopPropagation();
                  stepRoomPager(-1);
                }}
                className="absolute left-2 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded-full border border-white/15 bg-[#0B1220]/70 text-white hover:bg-[#16213A] backdrop-blur-sm cursor-pointer"
              >
                <ChevronLeft size={16} />
              </button>
              <button
                type="button"
                aria-label="Phòng tiếp theo"
                onClick={(e) => {
                  e.stopPropagation();
                  stepRoomPager(1);
                }}
                className="absolute right-2 top-1/2 -translate-y-1/2 flex h-8 w-8 items-center justify-center rounded-full border border-white/15 bg-[#0B1220]/70 text-white hover:bg-[#16213A] backdrop-blur-sm cursor-pointer"
              >
                <ChevronRight size={16} />
              </button>

              {/* Bottom Gradient Overlay: 3 Status Chips + PagerDots + Điều khiển */}
              <div
                className="pointer-events-none absolute inset-x-0 bottom-0 flex flex-wrap items-end justify-between gap-2 px-3 pb-2.5 pt-7"
                style={{
                  background:
                    'linear-gradient(to top, rgba(8,13,24,0.88) 0%, rgba(8,13,24,0.45) 62%, rgba(8,13,24,0) 100%)',
                }}
              >
                <div className="flex flex-wrap items-center gap-1.5">{renderStatusChips()}</div>

                <div className="pointer-events-auto flex items-center gap-2.5 ml-auto">
                  {/* PagerDots */}
                  <div className="hidden sm:flex items-center gap-1">
                    {rooms.map((rm, idx) => (
                      <button
                        key={rm.id}
                        type="button"
                        aria-label={`Chuyển tới ${rm.name}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          selectRoom(rm.id);
                        }}
                        className="h-1.5 rounded-full transition-all cursor-pointer"
                        style={{
                          width: idx === curRoomIdx ? 16 : 6,
                          backgroundColor: idx === curRoomIdx ? rm.accent : 'rgba(255,255,255,0.32)',
                        }}
                      />
                    ))}
                  </div>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      openRoomDetail(r.id);
                    }}
                    className="inline-flex items-center gap-1 rounded-xl bg-[#4F8BFF] px-2.5 py-1 text-[11px] font-semibold text-white shadow hover:brightness-110 cursor-pointer"
                  >
                    <Sliders size={12} />
                    <span>Điều khiển</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Room-specific 4 quick action cards */}
            <div className="shrink-0">{renderRoomQuickActions()}</div>
          </SmartCard>

          {/* Bottom Strip: WiFi gia đình + Thiết bị đã kết nối (matching HomeScreen.kt) */}
          <div className="shrink-0 grid grid-cols-1 md:grid-cols-12 gap-2.5">
            <SmartCard className="md:col-span-5 flex flex-col justify-between gap-2 p-2.5">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <IconBadge color={wifi.info.isConnected ? SH.Teal : SH.Red} size={30}>
                    <Wifi size={15} />
                  </IconBadge>
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs font-bold text-[#E6EDF7]">WiFi gia đình</span>
                      {!wifi.isRealData && <DemoBadge />}
                    </div>
                    <div className="text-[11px] text-[#8A9BB8] truncate">
                      {wifi.info.ssid} · Bảo mật {wifi.info.security}
                    </div>
                  </div>
                </div>
                <StatusPill
                  label={wifi.info.isConnected ? 'Đã kết nối' : 'Mất kết nối'}
                  color={wifi.info.isConnected ? SH.Green : SH.Red}
                />
              </div>

              <div className="flex items-center justify-between gap-2 pt-1 border-t border-[#253459] text-[11px]">
                <span className="text-[#8A9BB8] truncate">
                  {wifi.info.signalLabel} · Tốc độ <b className="font-mono-num text-[#E6EDF7]">{wifi.info.speedMbps} Mbps</b>
                </span>
                <button
                  type="button"
                  onClick={openWifi}
                  className="font-semibold text-[#14B8A6] hover:underline shrink-0 cursor-pointer"
                >
                  Cài đặt WiFi →
                </button>
              </div>
            </SmartCard>

            <SmartCard className="md:col-span-7 flex flex-col justify-between gap-2 p-2.5">
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-bold text-[#E6EDF7]">
                  Thiết bị đã kết nối ({wifi.devices.length})
                </span>
                <button
                  type="button"
                  onClick={openWifi}
                  className="text-[11px] font-semibold text-[#4F8BFF] hover:underline cursor-pointer"
                >
                  Xem tất cả →
                </button>
              </div>

              <div className="flex items-center gap-2 overflow-x-auto pb-0.5">
                {wifiPreview.map((dev, idx) => {
                  const col = WIFI_BADGE_COLORS[idx % WIFI_BADGE_COLORS.length];
                  return (
                    <button
                      key={dev.id}
                      type="button"
                      onClick={openWifi}
                      className="flex flex-col items-center gap-1 min-w-[56px] max-w-[66px] shrink-0 cursor-pointer group"
                      title={`${dev.name} (${dev.ip})`}
                    >
                      <div
                        className="flex h-8 w-8 items-center justify-center rounded-xl transition-transform group-hover:scale-105"
                        style={{
                          backgroundColor: tint(col, 0.16),
                          border: `1px solid ${tint(col, 0.36)}`,
                          color: col,
                        }}
                      >
                        <WifiDevIcon icon={dev.icon} size={15} />
                      </div>
                      <span className="w-full text-[10px] text-[#94A3B8] truncate text-center">
                        {dev.name}
                      </span>
                    </button>
                  );
                })}
                {wifiExtraCount > 0 && (
                  <button
                    type="button"
                    onClick={openWifi}
                    className="flex flex-col items-center gap-1 min-w-[54px] shrink-0 cursor-pointer"
                  >
                    <div className="flex h-8 w-8 items-center justify-center rounded-xl border border-[#253459] bg-[#0E1627] font-mono-num text-[11px] font-bold text-[#CBD5E1]">
                      +{wifiExtraCount}
                    </div>
                    <span className="text-[10px] text-[#8A9BB8]">Khác</span>
                  </button>
                )}
              </div>
            </SmartCard>
          </div>
        </div>

        {/* Right: Weather, Power & Shortcuts */}
        <div className="lg:col-span-4 flex flex-col min-h-0 gap-2.5 justify-between">
          <WeatherCard
            thoiTiet={thoiTiet}
            onRetry={taiThoiTiet}
            diaDiemFallback={config.weather.ten || 'Thời tiết ngoài trời'}
          />

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
                <span>Camera 24/24</span>
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
