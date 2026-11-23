import React, { useState } from 'react';
import { AlertTriangle, X } from 'lucide-react';
import { dangChayTrangThai } from '../data/giongnoi/types.js';
import { DEST_ITEMS } from '../data/models.js';
import { tinhCanhBaoSidebar } from '../data/sidebarStatus.js';
import { AutomationScreen } from '../screens/AutomationScreen.js';
import { CamBienScreen } from '../screens/CamBienScreen.js';
import { CameraScreen } from '../screens/CameraScreen.js';
import { ChiTietDienScreen } from '../screens/ChiTietDienScreen.js';
import { DevicesScreen } from '../screens/DevicesScreen.js';
import { HomeScreen } from '../screens/HomeScreen.js';
import { RoomDetailScreen } from '../screens/RoomDetailScreen.js';
import { ServerScreen } from '../screens/ServerScreen.js';
import { SettingsScreen } from '../screens/SettingsScreen.js';
import { StatsScreen } from '../screens/StatsScreen.js';
import { TienIchScreen } from '../screens/TienIchScreen.js';
import { WifiScreen } from '../screens/WifiScreen.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';
import { AdminPinModal } from './AdminPinModal.js';
import { AmbientScreensaver } from './AmbientScreensaver.js';
import { BangGiongNoi } from './BangGiongNoi.js';
import { FlatButton, ToastHost, TopBar } from './Common.js';
import { SideNav } from './SideNav.js';

export function AppShell() {
  const {
    currentDest,
    selectedRoomDetailId,
    chiTietDienOpen,
    wifiOpen,
    navigateDest,
    closeSubScreen,
    rooms,
    config,
    gatewayOnline,
    mqttStatus,
    cameraStatus,
    dien,
    thoiTiet,
    server,
    bomNuoc,
    ketNoiBom,
    nowMs,
    toasts,
    dismissToast,
    phienGiongNoi,
    voiceSheetOpen,
    setVoiceSheetOpen,
    batTatGiongNoi,
    tatGiongNoi,
    ngatLoiGiongNoi,
    screensaverConfig,
    screensaverActive,
    enterScreensaver,
    wakeScreensaver,
    emergencyAlert,
    dismissEmergencyAlert,
    adminLockEnabled,
    adminUnlocked,
    pinPrompt,
    submitAdminPin,
    cancelAdminPin,
    lockAdmin,
  } = useSmartHome();

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const voiceRunning = dangChayTrangThai(phienGiongNoi.trangThai);
  const canhBaoMuc = tinhCanhBaoSidebar({
    bomNuoc,
    ketNoiBom,
    mqttStatus,
    gatewayOnline,
    server,
  });

  const handleBamTroLy = () => {
    if (phienGiongNoi.trangThai.state === 'dang_tra_loi') {
      ngatLoiGiongNoi();
    } else if (voiceRunning) {
      tatGiongNoi();
    } else {
      batTatGiongNoi();
    }
  };

  const activeRoom = selectedRoomDetailId
    ? rooms.find((r) => r.id === selectedRoomDetailId)
    : undefined;

  const topTitle = chiTietDienOpen
    ? 'Chi tiết điện EVN'
    : wifiOpen
      ? 'Quản trị WiFi & Router'
      : activeRoom
        ? activeRoom.name
        : (DEST_ITEMS.find((d) => d.route === currentDest)?.title ?? 'SmartHome');

  const topSubtitle = chiTietDienOpen
    ? 'Sản lượng từng ngày & công tơ trực tiếp'
    : wifiOpen
      ? 'Lưu lượng, thiết bị kết nối & bảo mật mạng'
      : activeRoom
        ? 'Điều khiển chi tiết & mô phỏng 2.5D trực quan'
        : undefined;

  const hasBack = chiTietDienOpen || wifiOpen || Boolean(activeRoom);

  const renderMainContent = () => {
    if (chiTietDienOpen) return <ChiTietDienScreen />;
    if (wifiOpen) return <WifiScreen />;
    if (activeRoom) return <RoomDetailScreen roomId={activeRoom.id} />;

    switch (currentDest) {
      case 'home': return <HomeScreen />;
      case 'devices': return <DevicesScreen />;
      case 'camera': return <CameraScreen />;
      case 'sensors': return <CamBienScreen />;
      case 'automation': return <AutomationScreen />;
      case 'settings': return <SettingsScreen />;
      case 'server': return <ServerScreen />;
      case 'stats': return <StatsScreen />;
      case 'utilities': return <TienIchScreen />;
    }
  };

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-[#0B1220] text-[#E6EDF7]">
      <SideNav
        current={currentDest}
        onSelect={navigateDest}
        mobileOpen={mobileMenuOpen}
        onCloseMobile={() => setMobileMenuOpen(false)}
        canhBaoMuc={canhBaoMuc}
        phienGiongNoi={phienGiongNoi}
        onBamTroLy={handleBamTroLy}
      />

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <TopBar
          title={topTitle}
          subtitle={topSubtitle}
          onBack={hasBack ? closeSubScreen : undefined}
          onMenuOpen={!hasBack ? () => setMobileMenuOpen(true) : undefined}
          onOpenSettings={() => navigateDest('settings')}
          mqttStatus={mqttStatus}
          cameraStatus={cameraStatus}
          dien={dien}
          nowMs={nowMs}
          voiceActive={voiceRunning}
          onVoiceToggle={() => {
            if (!voiceSheetOpen) setVoiceSheetOpen(true);
            else batTatGiongNoi();
          }}
          onOpenScreensaver={enterScreensaver}
          adminUnlocked={adminUnlocked}
          adminLockEnabled={adminLockEnabled}
          onLockAdmin={() => void lockAdmin()}
        />

        <main className="flex-1 min-h-0 overflow-y-auto lg:overflow-hidden p-3 flex flex-col gap-2.5">
          {emergencyAlert && (
            <div
              className="shrink-0 flex flex-wrap items-center justify-between gap-2.5 rounded-2xl px-3.5 py-2.5 animate-pulse"
              style={{
                backgroundColor: tint(SH.Red, 0.18),
                border: `1.5px solid ${SH.Red}`,
              }}
            >
              <div className="flex items-center gap-2.5 min-w-0 flex-1">
                <AlertTriangle size={18} className="text-[#EF4444] shrink-0" />
                <div className="min-w-0">
                  <div className="text-xs font-bold text-[#FEE2E2] truncate">{emergencyAlert.title}</div>
                  <div className="text-[11px] text-[#FCA5A5] truncate">{emergencyAlert.detail}</div>
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <FlatButton
                  color={SH.Red}
                  variant="solid"
                  onClick={() => navigateDest(emergencyAlert.dest)}
                >
                  <span>Mở kiểm tra</span>
                </FlatButton>
                <button
                  type="button"
                  onClick={dismissEmergencyAlert}
                  title="Ẩn tạm cảnh báo này"
                  className="rounded-xl p-1.5 text-[#FCA5A5] hover:text-white hover:bg-[#EF4444]/20 cursor-pointer"
                >
                  <X size={15} />
                </button>
              </div>
            </div>
          )}

          <div className="flex-1 min-h-0 w-full">{renderMainContent()}</div>
        </main>
      </div>

      <BangGiongNoi />
      <ToastHost toasts={toasts} onDismiss={dismissToast} />

      {screensaverActive && (
        <AmbientScreensaver
          nowMs={nowMs}
          thoiTiet={thoiTiet}
          dien={dien}
          bomNuoc={bomNuoc}
          ketNoiBom={ketNoiBom}
          bomConfig={config.bomNuoc}
          config={screensaverConfig}
          onWake={wakeScreensaver}
        />
      )}

      <AdminPinModal
        open={Boolean(pinPrompt)}
        actionLabel={pinPrompt?.label ?? ''}
        error={pinPrompt?.error ?? null}
        busy={pinPrompt?.busy ?? false}
        onSubmit={(pin) => void submitAdminPin(pin)}
        onClose={cancelAdminPin}
      />
    </div>
  );
}
