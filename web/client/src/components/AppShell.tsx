import React, { useMemo, useState } from 'react';
import { AlertTriangle, Home, Menu, X } from 'lucide-react';
import { dangChayTrangThai } from '../data/giongnoi/types.js';
import { DEST_ITEMS, type DestRoute } from '../data/models.js';
import { tinhCanhBaoSidebar } from '../data/sidebarStatus.js';
import { AutomationScreen } from '../screens/AutomationScreen.js';
import { CamBienScreen } from '../screens/CamBienScreen.js';
import { CameraScreen } from '../screens/CameraScreen.js';
import { ChiTietDienScreen } from '../screens/ChiTietDienScreen.js';
import { DevicesScreen, tomTatDanhSachPhong } from '../screens/DevicesScreen.js';
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
import { DestIcon, FlatButton, RoundIconButton, StatusPill, ToastHost, TopBar } from './Common.js';
import { ConfirmDialog } from './ConfirmDialog.js';
import { PopupChaoKhuonMat } from './PopupChaoKhuonMat.js';
import { SideNav } from './SideNav.js';

const MOBILE_PRIMARY_TABS: Array<{ route: DestRoute; label: string }> = [
  { route: 'home', label: 'Trang chủ' },
  { route: 'devices', label: 'Thiết bị' },
  { route: 'camera', label: 'Camera' },
  { route: 'automation', label: 'Tự động' },
];

const EXTRA_ROUTES: DestRoute[] = ['utilities', 'sensors', 'stats', 'server', 'settings'];

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
    congtac,
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
    cameraRecorder,
    chaoMatHienTai,
    anPopupChaoMat,
    confirmPrompt,
    acceptConfirm,
    cancelConfirm,
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

  const { dsPhong: devicesSummaryRooms, tongDangBat, tongThietBi } = useMemo(
    () => tomTatDanhSachPhong(rooms, congtac),
    [rooms, congtac],
  );

  const topTitle = chiTietDienOpen
    ? 'Chi tiết điện EVN'
    : wifiOpen
      ? 'Quản trị WiFi & Router'
      : activeRoom
        ? `Điều khiển ${activeRoom.name.toLowerCase()}`
        : (DEST_ITEMS.find((d) => d.route === currentDest)?.title ?? 'SmartHome');

  const topSubtitle = chiTietDienOpen
    ? 'Sản lượng từng ngày & công tơ trực tiếp'
    : wifiOpen
      ? 'Lưu lượng, thiết bị kết nối & bảo mật mạng'
      : undefined;

  const hasBack = chiTietDienOpen || wifiOpen || Boolean(activeRoom);

  const topBarExtra = activeRoom ? (
    <RoundIconButton
      onClick={() => navigateDest('home')}
      color={SH.Blue}
      size={32}
      title="Về Trang chủ"
    >
      <Home size={16} />
    </RoundIconButton>
  ) : !hasBack && currentDest === 'devices' ? (
    <div className="hidden sm:flex items-center gap-1.5">
      <StatusPill label={`${devicesSummaryRooms.length} phòng`} color={SH.Blue} dot={false} />
      <StatusPill
        label={`${tongDangBat}/${tongThietBi} thiết bị đang bật`}
        color={tongDangBat > 0 ? SH.Green : SH.TextOff}
      />
    </div>
  ) : undefined;

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

  const extraAlertColor = EXTRA_ROUTES.map((r) => canhBaoMuc[r]).find(Boolean);
  const isExtraActive = !hasBack && EXTRA_ROUTES.includes(currentDest);

  return (
    <div className="flex h-dvh min-h-screen w-full overflow-hidden bg-[#0B1220] text-[#E6EDF7]">
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
            else handleBamTroLy();
          }}
          onOpenScreensaver={enterScreensaver}
          adminUnlocked={adminUnlocked}
          adminLockEnabled={adminLockEnabled}
          onLockAdmin={() => void lockAdmin()}
          extra={topBarExtra}
        />

        <main className="flex-1 min-h-0 overflow-y-auto lg:overflow-hidden p-2.5 sm:p-3 pl-[max(0.625rem,env(safe-area-inset-left))] pr-[max(0.625rem,env(safe-area-inset-right))] flex flex-col gap-2.5">
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

          <div className="w-full lg:flex-1 lg:min-h-0">{renderMainContent()}</div>
        </main>

        {/* Native-feel iPhone / Mobile Bottom Navigation Bar */}
        <nav
          aria-label="Điều hướng nhanh trên điện thoại"
          className="md:hidden shrink-0 border-t border-[#253459] bg-[#0E1627]/95 backdrop-blur-md pb-[env(safe-area-inset-bottom)] pl-[env(safe-area-inset-left)] pr-[env(safe-area-inset-right)]"
        >
          <div className="grid grid-cols-5 px-1 pt-1.5 pb-1">
            {MOBILE_PRIMARY_TABS.map((item) => {
              const active = !hasBack && currentDest === item.route;
              const badge = canhBaoMuc[item.route];
              return (
                <button
                  key={item.route}
                  type="button"
                  onClick={() => navigateDest(item.route)}
                  className="relative flex flex-col items-center justify-center gap-0.5 py-1 rounded-xl transition-colors cursor-pointer"
                  style={{ color: active ? SH.Blue : SH.TextSecondary }}
                >
                  <span className="relative">
                    <DestIcon route={item.route} size={19} />
                    {badge && (
                      <span
                        className="absolute -top-0.5 -right-1.5 h-2 w-2 rounded-full"
                        style={{ backgroundColor: badge }}
                      />
                    )}
                  </span>
                  <span className="text-[10px] font-semibold leading-tight truncate max-w-full px-0.5">
                    {item.label}
                  </span>
                </button>
              );
            })}

            <button
              type="button"
              onClick={() => setMobileMenuOpen(true)}
              className="relative flex flex-col items-center justify-center gap-0.5 py-1 rounded-xl transition-colors cursor-pointer"
              style={{ color: isExtraActive || mobileMenuOpen ? SH.Blue : SH.TextSecondary }}
            >
              <span className="relative">
                <Menu size={19} />
                {extraAlertColor && (
                  <span
                    className="absolute -top-0.5 -right-1.5 h-2 w-2 rounded-full"
                    style={{ backgroundColor: extraAlertColor }}
                  />
                )}
              </span>
              <span className="text-[10px] font-semibold leading-tight">Thêm</span>
            </button>
          </div>
        </nav>
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

      <ConfirmDialog
        prompt={confirmPrompt}
        onConfirm={acceptConfirm}
        onCancel={cancelConfirm}
      />

      {chaoMatHienTai && !screensaverActive && (
        <PopupChaoKhuonMat
          ev={chaoMatHienTai}
          apiPort={cameraRecorder.apiPort}
          onMoCamera={() => {
            anPopupChaoMat();
            navigateDest('camera');
          }}
          onDong={anPopupChaoMat}
        />
      )}
    </div>
  );
}
