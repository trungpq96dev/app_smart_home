import React from 'react';
import {
  AirVent,
  AlertTriangle,
  Blinds,
  Calendar,
  Eye,
  Fan,
  Flame,
  Lightbulb,
  Lock,
  Radio,
  ShieldAlert,
  Tv,
  Unlock,
  Video,
} from 'lucide-react';
import { HALLWAY_VIEWS, ROOM_MODE_LABELS, ROOM_MODES, stoveWatts } from '../data/models.js';
import {
  ChoiceChip,
  DemoBadge,
  FlatButton,
  RoomIcon,
  SectionHeader,
  SmartCard,
  StatusPill,
} from '../components/Common.js';
import {
  CompactSlider,
  DeviceSwitchRow,
  TempStepperRow,
} from '../components/Tiles.js';
import { BedroomStage } from '../components/stages/BedroomStage.js';
import { HallwayStage } from '../components/stages/HallwayStage.js';
import { KitchenStage } from '../components/stages/KitchenStage.js';
import { LivingRoomStage } from '../components/stages/LivingRoomStage.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

export function RoomDetailScreen({ roomId }: { roomId: string }) {
  const {
    rooms,
    openRoomDetail,
    oThietBi,
    bamCongTac,
    toggleRoomLight,
    toggleRoomFan,
    toggleRoomAc,
    stepAcTemp,
    setRoomMode,
    toggleRoomSchedule,
    setLightBrightness,
    setFanSpeed,
    toggleRoomTv,
    setCurtainOpen,
    setHoodSpeed,
    toggleStove,
    stepStoveLevel,
    triggerTestGasAlert,
    dismissGasAlert,
    setHallView,
    toggleHallFrontLight,
    setHallFrontBrightness,
    toggleHallSurroundingLight,
    setHallSurroundingBrightness,
    toggleHallDoorLock,
    toggleHallCamera,
    toggleHallIrMode,
    toggleHallMotionAuto,
    triggerHallMotionTest,
  } = useSmartHome();

  const room = rooms.find((r) => r.id === roomId) ?? rooms[0];

  const renderRealOrDemoSwitch = (
    thietBiKey: string,
    demoLabel: string,
    icon: React.ReactNode,
    color: string,
    demoChecked: boolean,
    demoSub: string,
    onDemoToggle: () => void,
  ) => {
    const o = oThietBi(room.id, thietBiKey, demoChecked);
    if (o.kind === 'that') {
      return (
        <DeviceSwitchRow
          icon={icon}
          label={o.ten}
          subtitle={o.ghiChu || 'Công tắc thật qua MQTT'}
          color={color}
          checked={o.dangCho && o.mong !== null ? o.mong : o.bat === true}
          disabled={o.dangCho}
          pending={o.dangCho}
          badge={<StatusPill label="Công tắc thật" color={o.online ? SH.Green : SH.Red} dot={false} />}
          onToggle={() => bamCongTac(room.id, thietBiKey)}
        />
      );
    }
    return (
      <DeviceSwitchRow
        icon={icon}
        label={demoLabel}
        subtitle={demoSub}
        color={color}
        checked={demoChecked}
        badge={<DemoBadge />}
        onToggle={onDemoToggle}
      />
    );
  };

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Top Bar: Room switch pills (left) + Room modes (right) */}
      <div className="shrink-0 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 overflow-x-auto pb-0.5">
          {rooms.map((r) => {
            const active = r.id === room.id;
            return (
              <button
                key={r.id}
                type="button"
                onClick={() => openRoomDetail(r.id)}
                className="inline-flex items-center gap-1.5 rounded-xl px-2.5 py-1 text-xs font-semibold whitespace-nowrap cursor-pointer shrink-0"
                style={{
                  backgroundColor: active ? tint(r.accent, 0.2) : SH.Card,
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

        <div className="flex items-center gap-1.5 flex-wrap">
          {ROOM_MODES.map((m) => (
            <ChoiceChip
              key={m}
              label={ROOM_MODE_LABELS[m]}
              selected={room.mode === m}
              color={room.accent}
              onClick={() => setRoomMode(room.id, m)}
            />
          ))}
        </div>
      </div>

      {/* Main 2-column layout: 2.5D Stage + Controls */}
      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
        {/* Left: Stage + Alert Banner + Schedule */}
        <div className="lg:col-span-7 flex flex-col min-h-0 gap-2.5">
          <div className="flex-1 min-h-[240px] flex flex-col">
            {room.id === 'living' ? (
              <LivingRoomStage room={room} fill />
            ) : room.id === 'kitchen' ? (
              <KitchenStage room={room} fill />
            ) : room.id === 'hall' ? (
              <HallwayStage room={room} fill />
            ) : (
              <BedroomStage room={room} fill />
            )}
          </div>

          {/* Gas alert banner if Kitchen in alert */}
          {room.id === 'kitchen' && room.gasAlert && (
            <SmartCard glowColor={SH.Red} className="shrink-0 bg-[#EF4444]/10 border-[#EF4444] p-2.5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2 text-[#FCA5A5]">
                  <ShieldAlert size={18} className="text-[#EF4444] shrink-0" />
                  <div>
                    <div className="text-xs font-bold text-[#FECACA]">
                      CẢNH BÁO RÒ RỈ GAS ({room.gasPpm} ppm)
                    </div>
                    <div className="text-[11px]">Máy hút mùi đã tự bật mức cao nhất.</div>
                  </div>
                </div>
                <FlatButton color={SH.Red} variant="solid" onClick={() => dismissGasAlert(room.id)}>
                  Đã xử lý / Tắt còi
                </FlatButton>
              </div>
            </SmartCard>
          )}

          {/* Hallway Motion Banner */}
          {room.id === 'hall' && room.hallMotionDetected && (
            <SmartCard glowColor={SH.Amber} className="shrink-0 bg-[#F59E0B]/10 border-[#F59E0B] p-2.5">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 text-xs text-[#FCD34D]">
                  <Radio size={16} className="animate-pulse text-[#F59E0B]" />
                  <span>Cảm biến PIR phát hiện có chuyển động ở hành lang.</span>
                </div>
                <FlatButton color={SH.Amber} onClick={() => triggerHallMotionTest(room.id)}>
                  Xóa cảnh báo
                </FlatButton>
              </div>
            </SmartCard>
          )}

          {/* Compact Schedule Row */}
          <SmartCard className="shrink-0 p-2.5 space-y-1.5">
            <SectionHeader title="Lịch tự động" badge={<DemoBadge />} />
            <DeviceSwitchRow
              icon={<Calendar size={15} />}
              label={room.scheduleLabel}
              subtitle={room.scheduleTime}
              color={room.accent}
              checked={room.scheduleOn}
              onToggle={() => toggleRoomSchedule(room.id)}
            />
          </SmartCard>
        </div>

        {/* Right: Detailed device controls */}
        <div className="lg:col-span-5 flex flex-col min-h-0 gap-2.5 lg:overflow-y-auto pr-0.5">
          {room.id === 'hall' ? (
            <>
              <SmartCard className="space-y-3">
                <SectionHeader title="Góc quan sát Camera" badge={<DemoBadge />} />
                <div className="flex flex-wrap gap-2">
                  {HALLWAY_VIEWS.map((v) => (
                    <ChoiceChip
                      key={v.id}
                      label={v.label}
                      selected={room.hallView === v.id}
                      color={SH.Cyan}
                      onClick={() => setHallView(room.id, v.id)}
                    />
                  ))}
                </div>
              </SmartCard>

              <SmartCard className="space-y-3">
                <SectionHeader title="Chiếu sáng hành lang" badge={<DemoBadge />} />
                <DeviceSwitchRow
                  icon={<Lightbulb size={16} />}
                  label="Đèn phía trước (đầu hè)"
                  subtitle={`Công suất ${room.hallFrontBrightness}%`}
                  color={SH.Amber}
                  checked={room.hallFrontLightOn}
                  onToggle={() => toggleHallFrontLight(room.id)}
                />
                <CompactSlider
                  label="Độ sáng đèn đầu hè"
                  value={room.hallFrontBrightness}
                  min={10}
                  max={100}
                  step={5}
                  color={SH.Amber}
                  disabled={!room.hallFrontLightOn}
                  onChange={(v) => setHallFrontBrightness(room.id, v)}
                />

                <DeviceSwitchRow
                  icon={<Lightbulb size={16} />}
                  label="Đèn chiếu sáng xung quanh"
                  subtitle={`Công suất ${room.hallSurroundingBrightness}%`}
                  color={SH.Cyan}
                  checked={room.hallSurroundingLightOn}
                  onToggle={() => toggleHallSurroundingLight(room.id)}
                />
                <CompactSlider
                  label="Độ sáng đèn xung quanh"
                  value={room.hallSurroundingBrightness}
                  min={10}
                  max={100}
                  step={5}
                  color={SH.Cyan}
                  disabled={!room.hallSurroundingLightOn}
                  onChange={(v) => setHallSurroundingBrightness(room.id, v)}
                />
              </SmartCard>

              <SmartCard className="space-y-3">
                <SectionHeader title="An ninh & Giám sát" badge={<DemoBadge />} />
                <DeviceSwitchRow
                  icon={room.hallDoorLocked ? <Lock size={16} /> : <Unlock size={16} />}
                  label="Khóa cửa thông minh"
                  subtitle={room.hallDoorLocked ? 'Đã chốt an toàn' : 'Đang mở khóa'}
                  color={SH.Green}
                  checked={room.hallDoorLocked}
                  onToggle={() => toggleHallDoorLock(room.id)}
                />
                <DeviceSwitchRow
                  icon={<Video size={16} />}
                  label="Camera góc rộng hành lang"
                  subtitle={room.hallCameraOn ? 'Đang ghi hình' : 'Đã tắt'}
                  color={SH.Blue}
                  checked={room.hallCameraOn}
                  onToggle={() => toggleHallCamera(room.id)}
                />
                <DeviceSwitchRow
                  icon={<Eye size={16} />}
                  label="Chế độ hồng ngoại ban đêm (IR)"
                  subtitle={room.hallIrMode ? 'Bộ lọc hồng ngoại đang bật' : 'Hình ảnh màu chuẩn'}
                  color={SH.Green}
                  checked={room.hallIrMode}
                  onToggle={() => toggleHallIrMode(room.id)}
                />
                <DeviceSwitchRow
                  icon={<Radio size={16} />}
                  label="Tự bật đèn khi phát hiện chuyển động"
                  subtitle="Cảm biến PIR ngoài sân"
                  color={SH.Amber}
                  checked={room.hallMotionAuto}
                  onToggle={() => toggleHallMotionAuto(room.id)}
                />
                <FlatButton color={SH.Amber} className="w-full" onClick={() => triggerHallMotionTest(room.id)}>
                  <Radio size={14} />
                  <span>{room.hallMotionDetected ? 'Tắt mô phỏng chuyển động' : 'Thử kích hoạt chuyển động PIR'}</span>
                </FlatButton>
              </SmartCard>
            </>
          ) : room.id === 'kitchen' ? (
            <>
              <SmartCard className="space-y-3">
                <SectionHeader title="Chiếu sáng & Hút mùi" />
                {renderRealOrDemoSwitch(
                  'den',
                  'Đèn nhà bếp',
                  <Lightbulb size={16} />,
                  SH.Amber,
                  room.lightOn,
                  `Độ sáng ${room.lightBrightness}%`,
                  () => toggleRoomLight(room.id),
                )}
                <CompactSlider
                  label="Độ sáng đèn bếp"
                  value={room.lightBrightness}
                  min={10}
                  max={100}
                  step={5}
                  color={SH.Amber}
                  disabled={!room.lightOn}
                  onChange={(v) => setLightBrightness(room.id, v)}
                />

                {renderRealOrDemoSwitch(
                  'quat',
                  'Máy hút mùi',
                  <Fan size={16} />,
                  SH.Green,
                  room.fanOn,
                  `Mức gió ${room.hoodSpeed}/3`,
                  () => toggleRoomFan(room.id),
                )}
                <div className="flex items-center justify-between gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                  <span className="text-xs text-[#8A9BB8]">Tốc độ hút mùi</span>
                  <div className="flex items-center gap-1.5">
                    {[1, 2, 3].map((spd) => (
                      <ChoiceChip
                        key={spd}
                        label={`Mức ${spd}`}
                        selected={room.hoodSpeed === spd}
                        color={SH.Green}
                        onClick={() => setHoodSpeed(room.id, spd)}
                      />
                    ))}
                  </div>
                </div>
              </SmartCard>

              <SmartCard className="space-y-3">
                <SectionHeader title="Bếp từ & Điều hòa" badge={<DemoBadge />} />
                <DeviceSwitchRow
                  icon={<Flame size={16} />}
                  label="Bếp từ đôi"
                  subtitle={room.stoveOn ? `Đang nấu mức P${room.stoveLevel} · ${stoveWatts(room.stoveLevel)}W` : 'Đang tắt'}
                  color={SH.Red}
                  checked={room.stoveOn}
                  onToggle={() => toggleStove(room.id)}
                />
                <TempStepperRow
                  label={`Công suất bếp (${stoveWatts(room.stoveLevel)}W)`}
                  valueText={`P${room.stoveLevel}`}
                  minusDisabled={room.stoveLevel <= 1}
                  plusDisabled={room.stoveLevel >= 9}
                  color={SH.Red}
                  onMinus={() => stepStoveLevel(room.id, -1)}
                  onPlus={() => stepStoveLevel(room.id, 1)}
                />
                {renderRealOrDemoSwitch(
                  'dieu_hoa',
                  'Điều hòa nhà bếp',
                  <AirVent size={16} />,
                  SH.Purple,
                  room.acOn,
                  `Đặt ${room.acTemp}°C`,
                  () => toggleRoomAc(room.id),
                )}
                <TempStepperRow
                  label="Nhiệt độ đặt"
                  valueText={`${room.acTemp}°C`}
                  minusDisabled={room.acTemp <= 16}
                  plusDisabled={room.acTemp >= 30}
                  onMinus={() => stepAcTemp(room.id, -1)}
                  onPlus={() => stepAcTemp(room.id, 1)}
                />
              </SmartCard>

              <SmartCard className="space-y-2.5">
                <SectionHeader title="Cảm biến rò rỉ Gas" badge={<DemoBadge />} />
                <div className="flex items-center justify-between text-xs">
                  <span className="text-[#8A9BB8]">Nồng độ đo được:</span>
                  <span className="font-mono-num font-bold" style={{ color: room.gasAlert ? SH.Red : SH.Green }}>
                    {room.gasPpm} ppm ({room.gasAlert ? 'NGUY HIỂM' : 'An toàn'})
                  </span>
                </div>
                <FlatButton color={SH.Red} className="w-full" onClick={() => triggerTestGasAlert(room.id)}>
                  <AlertTriangle size={14} />
                  <span>{room.gasAlert ? 'Tắt mô phỏng rò Gas' : 'Thử kích hoạt cảnh báo rò Gas'}</span>
                </FlatButton>
              </SmartCard>
            </>
          ) : (
            // Living room & Bedrooms
            <>
              <SmartCard className="space-y-3">
                <SectionHeader title="Đèn & Quạt" />
                {renderRealOrDemoSwitch(
                  'den',
                  'Đèn chiếu sáng chính',
                  <Lightbulb size={16} />,
                  SH.Amber,
                  room.lightOn,
                  `Độ sáng ${room.lightBrightness}%`,
                  () => toggleRoomLight(room.id),
                )}
                <CompactSlider
                  label="Độ sáng đèn"
                  value={room.lightBrightness}
                  min={10}
                  max={100}
                  step={5}
                  color={SH.Amber}
                  disabled={!room.lightOn}
                  onChange={(v) => setLightBrightness(room.id, v)}
                />

                {renderRealOrDemoSwitch(
                  'quat',
                  room.id === 'living' ? 'Quạt cây thông minh' : 'Quạt trần 5 cánh',
                  <Fan size={16} />,
                  SH.Blue,
                  room.fanOn,
                  `Tốc độ ${room.fanSpeed}/3`,
                  () => toggleRoomFan(room.id),
                )}
                <div className="flex items-center justify-between gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                  <span className="text-xs text-[#8A9BB8]">Cấp tốc độ quạt</span>
                  <div className="flex items-center gap-1.5">
                    {[1, 2, 3].map((spd) => (
                      <ChoiceChip
                        key={spd}
                        label={`Mức ${spd}`}
                        selected={room.fanSpeed === spd}
                        color={SH.Blue}
                        onClick={() => setFanSpeed(room.id, spd)}
                      />
                    ))}
                  </div>
                </div>
              </SmartCard>

              <SmartCard className="space-y-3">
                <SectionHeader title="Điều hòa & Tiện nghi" />
                {renderRealOrDemoSwitch(
                  'dieu_hoa',
                  'Điều hòa không khí',
                  <AirVent size={16} />,
                  SH.Purple,
                  room.acOn,
                  `Nhiệt độ đặt ${room.acTemp}°C`,
                  () => toggleRoomAc(room.id),
                )}
                <TempStepperRow
                  label="Nhiệt độ điều hòa"
                  valueText={`${room.acTemp}°C`}
                  minusDisabled={room.acTemp <= 16}
                  plusDisabled={room.acTemp >= 30}
                  onMinus={() => stepAcTemp(room.id, -1)}
                  onPlus={() => stepAcTemp(room.id, 1)}
                />

                {room.id === 'living' && (
                  <>
                    <DeviceSwitchRow
                      icon={<Tv size={16} />}
                      label="Smart TV 4K HDR"
                      subtitle={room.tvOn ? 'Đang phát' : 'Chế độ chờ'}
                      color={SH.Cyan}
                      checked={room.tvOn}
                      badge={<DemoBadge />}
                      onToggle={() => toggleRoomTv(room.id)}
                    />
                    <div className="space-y-2">
                      <CompactSlider
                        label="Độ mở rèm cửa sổ"
                        value={room.curtainOpen}
                        min={0}
                        max={100}
                        step={5}
                        color={SH.Teal}
                        onChange={(v) => setCurtainOpen(room.id, v)}
                      />
                      <div className="flex items-center gap-2">
                        <FlatButton color={SH.Teal} className="flex-1" onClick={() => setCurtainOpen(room.id, 100)}>
                          <Blinds size={14} />
                          <span>Mở hết (100%)</span>
                        </FlatButton>
                        <FlatButton color={SH.TextSecondary} className="flex-1" onClick={() => setCurtainOpen(room.id, 0)}>
                          <span>Đóng kín (0%)</span>
                        </FlatButton>
                      </div>
                    </div>
                  </>
                )}
              </SmartCard>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

