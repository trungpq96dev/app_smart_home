import React, { useState } from 'react';
import { dangChayTrangThai } from '../data/giongnoi/types.js';
import { DEST_ITEMS } from '../data/models.js';
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
import { BangGiongNoi } from './BangGiongNoi.js';
import { ToastHost, TopBar } from './Common.js';
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
    gatewayOnline,
    mqttStatus,
    cameraStatus,
    dien,
    nowMs,
    toasts,
    dismissToast,
    phienGiongNoi,
    voiceSheetOpen,
    setVoiceSheetOpen,
    batTatGiongNoi,
  } = useSmartHome();

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const voiceRunning = dangChayTrangThai(phienGiongNoi.trangThai);

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
        gatewayOnline={gatewayOnline}
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
        />

        <main className="flex-1 min-h-0 overflow-y-auto lg:overflow-hidden p-3">
          <div className="h-full w-full">{renderMainContent()}</div>
        </main>
      </div>

      <BangGiongNoi />
      <ToastHost toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
}

