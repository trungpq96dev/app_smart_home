import React, { useEffect, useState } from 'react';
import {
  BellRing,
  Check,
  CloudSun,
  Copy,
  Droplets,
  ExternalLink,
  Eye,
  EyeOff,
  Globe,
  Lock,
  LockOpen,
  LogOut,
  Mic,
  Moon,
  RefreshCw,
  Save,
  Send,
  Server,
  Shield,
  Trash2,
  Video,
  Wifi,
} from 'lucide-react';
import {
  BAN_SONG_CONG_LIST,
  GIONG_CO_SAN,
  MucDung,
  MODEL_MAC_DINH,
  type BanSongCong,
} from '../data/giongnoi/types.js';
import { BarChart } from '../components/Charts.js';
import { TrangThaiKetNoiBomPill } from '../components/BomNuocUi.js';
import { useWebAccess } from '../components/WebAccessGate.js';
import {
  ChoiceChip,
  FlatButton,
  SectionHeader,
  SmartCard,
  StatusPill,
  TrangThaiCameraPill,
  TrangThaiKetNoi,
} from '../components/Common.js';
import { DeviceSwitchRow } from '../components/Tiles.js';
import {
  dinhDangThoiGianChay,
  nhanKetNoiBom,
  wifiRssi,
} from '../data/bomnuoc.js';
import {
  ANDROID_BASE_VERSION,
  APP_WEB_VERSION,
  useSmartHome,
} from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

type SettingsTab = 'services' | 'bom_nuoc' | 'voice' | 'security' | 'system';

const WEATHER_PRESETS = [
  { ten: 'Hà Nội', lat: 21.0278, lon: 105.8342 },
  { ten: 'TP.HCM', lat: 10.7769, lon: 106.7009 },
  { ten: 'Đà Nẵng', lat: 16.0544, lon: 108.2022 },
];

export function SettingsScreen() {
  const {
    config,
    gatewayOnline,
    mqttStatus,
    cameraStatus,
    dien,
    nowMs,
    luuMqttConfig,
    luuCameraConfig,
    luuWeatherConfig,
    luuAquaControlUrl,
    bomNuoc,
    ketNoiBom,
    kenhBom,
    luuBomNuocConfig,
    voiceConfig,
    voiceUsage,
    updateVoiceConfig,
    clearVoiceUsage,
    updateCheck,
    checkGithubUpdate,
    screensaverConfig,
    updateScreensaverConfig,
    enterScreensaver,
    telegramStatus,
    luuTelegramConfig,
    testTelegramAlert,
    adminLockEnabled,
    adminUnlocked,
    requireAdminPin,
    lockAdmin,
    updateAdminPin,
    showToast,
  } = useSmartHome();
  const { mode: webAccessMode, logout: webLogout } = useWebAccess();

  const [tab, setTab] = useState<SettingsTab>('services');
  const [tunnelUrl, setTunnelUrl] = useState<string | null>(null);
  const [cockpitTunnelUrl, setCockpitTunnelUrl] = useState<string | null>(null);

  useEffect(() => {
    if (tab !== 'system') return;
    let cancelled = false;
    const loadTunnel = async () => {
      try {
        const r = await fetch('/api/tunnel/status', { credentials: 'same-origin' });
        if (!r.ok || cancelled) return;
        const d = (await r.json()) as { url?: string | null; cockpitUrl?: string | null };
        if (!cancelled) {
          setTunnelUrl(d.url || null);
          setCockpitTunnelUrl(d.cockpitUrl || null);
        }
      } catch {
        // ignore
      }
    };
    void loadTunnel();
    const id = window.setInterval(loadTunnel, 8000);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
  }, [tab]);

  // MQTT form
  const [mqttHost, setMqttHost] = useState(config.mqtt.host);
  const [mqttPort, setMqttPort] = useState(String(config.mqtt.cong));
  const [mqttTls, setMqttTls] = useState(config.mqtt.tls);
  const [mqttUser, setMqttUser] = useState(config.mqtt.taiKhoan);
  const [mqttPass, setMqttPass] = useState('');
  const [mqttClearPass, setMqttClearPass] = useState(false);
  const [mqttClientId, setMqttClientId] = useState(config.mqtt.clientId);
  const [savingMqtt, setSavingMqtt] = useState(false);

  // Camera form
  const [camHost, setCamHost] = useState(config.camera.host);
  const [camPort, setCamPort] = useState(String(config.camera.cong));
  const [camRecorderPort, setCamRecorderPort] = useState(String(config.camera.congGhiHinh || 1985));
  const [camTls, setCamTls] = useState(config.camera.tls);
  const [camUser, setCamUser] = useState(config.camera.taiKhoan);
  const [camPass, setCamPass] = useState('');
  const [camClearPass, setCamClearPass] = useState(false);
  const [savingCam, setSavingCam] = useState(false);

  // Weather form
  const [wTen, setWTen] = useState(config.weather.ten);
  const [wLat, setWLat] = useState(String(config.weather.lat));
  const [wLon, setWLon] = useState(String(config.weather.lon));
  const [wErr, setWErr] = useState<string | null>(null);
  const [savingWeather, setSavingWeather] = useState(false);

  // AquaControl URL form
  const [aquaUrl, setAquaUrl] = useState(config.aquaControlUrl);
  const [savingAqua, setSavingAqua] = useState(false);

  // Bom nuoc form
  const [bomEspHost, setBomEspHost] = useState(config.bomNuoc.espHost);
  const [bomUuTienLan, setBomUuTienLan] = useState(config.bomNuoc.uuTienLan);
  const [bomBrokerHost, setBomBrokerHost] = useState(config.bomNuoc.brokerHost);
  const [bomBrokerPort, setBomBrokerPort] = useState(String(config.bomNuoc.brokerPort || 8883));
  const [bomBrokerTls, setBomBrokerTls] = useState(config.bomNuoc.brokerTls);
  const [bomBrokerUser, setBomBrokerUser] = useState(config.bomNuoc.brokerUser);
  const [bomBrokerPass, setBomBrokerPass] = useState('');
  const [bomClearPass, setBomClearPass] = useState(false);
  const [savingBom, setSavingBom] = useState(false);

  // Voice local form
  const [showApiKey, setShowApiKey] = useState(false);
  const [vKey, setVKey] = useState(voiceConfig.khoaApi);
  const [vModel, setVModel] = useState(voiceConfig.model || MODEL_MAC_DINH);
  const [vVoice, setVVoice] = useState(voiceConfig.giongDoc);
  const [vMqtt, setVMqtt] = useState(voiceConfig.guiMqtt);
  const [vBanSongCong, setVBanSongCong] = useState<BanSongCong>(voiceConfig.banSongCong);
  const [vImLang, setVImLang] = useState(String(voiceConfig.giayImLangTuDong));
  const [vToiDaPhien, setVToiDaPhien] = useState(String(voiceConfig.giayToiDaPhien));
  const [vPhutNgay, setVPhutNgay] = useState(String(voiceConfig.phutMoiNgay));
  const [voiceSavedFlag, setVoiceSavedFlag] = useState(false);

  // Telegram form
  const [tgEnabled, setTgEnabled] = useState(config.telegram.enabled);
  const [tgToken, setTgToken] = useState('');
  const [tgClearToken, setTgClearToken] = useState(false);
  const [tgChatId, setTgChatId] = useState(config.telegram.chatId);
  const [tgOfflineMin, setTgOfflineMin] = useState(String(config.telegram.offlineMinutes));
  const [tgSpikePct, setTgSpikePct] = useState(String(config.telegram.electricSpikePct));
  const [tgGasPpm, setTgGasPpm] = useState(String(config.telegram.gasThresholdPpm));
  const [tgCooldownMin, setTgCooldownMin] = useState(String(config.telegram.cooldownMinutes));
  const [tgPirTopic, setTgPirTopic] = useState(config.telegram.pirTopic);
  const [tgGasTopic, setTgGasTopic] = useState(config.telegram.gasTopic);
  const [savingTg, setSavingTg] = useState(false);
  const [testingTg, setTestingTg] = useState(false);

  // Admin PIN form
  const [pinCur, setPinCur] = useState('');
  const [pinNew, setPinNew] = useState('');
  const [pinConfirm, setPinConfirm] = useState('');
  const [pinMsg, setPinMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const [savingPin, setSavingPin] = useState(false);

  // Sync form defaults once config loads from server
  useEffect(() => {
    setMqttHost(config.mqtt.host);
    setMqttPort(String(config.mqtt.cong));
    setMqttTls(config.mqtt.tls);
    setMqttUser(config.mqtt.taiKhoan);
    setMqttClientId(config.mqtt.clientId);

    setCamHost(config.camera.host);
    setCamPort(String(config.camera.cong));
    setCamRecorderPort(String(config.camera.congGhiHinh || 1985));
    setCamTls(config.camera.tls);
    setCamUser(config.camera.taiKhoan);

    setWTen(config.weather.ten);
    setWLat(String(config.weather.lat));
    setWLon(String(config.weather.lon));

    setAquaUrl(config.aquaControlUrl);

    setBomEspHost(config.bomNuoc.espHost);
    setBomUuTienLan(config.bomNuoc.uuTienLan);
    setBomBrokerHost(config.bomNuoc.brokerHost);
    setBomBrokerPort(String(config.bomNuoc.brokerPort || 8883));
    setBomBrokerTls(config.bomNuoc.brokerTls);
    setBomBrokerUser(config.bomNuoc.brokerUser);

    setTgEnabled(config.telegram.enabled);
    setTgChatId(config.telegram.chatId);
    setTgOfflineMin(String(config.telegram.offlineMinutes || 5));
    setTgSpikePct(String(config.telegram.electricSpikePct || 40));
    setTgGasPpm(String(config.telegram.gasThresholdPpm || 200));
    setTgCooldownMin(String(config.telegram.cooldownMinutes || 30));
    setTgPirTopic(config.telegram.pirTopic || 'smarthome/sensor/pir');
    setTgGasTopic(config.telegram.gasTopic || 'smarthome/sensor/gas');
  }, [config]);

  const inputCls =
    'w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#4F8BFF] font-mono-num';

  const homNayRec = MucDung.cuaNgay(voiceUsage, MucDung.homNay());
  const homNayGiay = homNayRec.giay;
  const bayNgayGiay = MucDung.giayNgayGanDay(voiceUsage, 7);
  const tranGiayNgay = Math.max(0, voiceConfig.phutMoiNgay * 60);
  const quotaPct = tranGiayNgay > 0 ? Math.min(100, (homNayGiay / tranGiayNgay) * 100) : 0;
  const conLaiPhut =
    tranGiayNgay > 0 ? Math.max(0, Math.ceil((tranGiayNgay - homNayGiay) / 60)) : null;
  const quotaColor =
    quotaPct >= 100 ? SH.Red : quotaPct >= 80 ? SH.Amber : SH.Green;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      <div className="shrink-0 flex flex-wrap gap-2">
        <ChoiceChip label="Dịch vụ & Kết nối" selected={tab === 'services'} color={SH.Blue} onClick={() => setTab('services')} />
        <ChoiceChip label="Bơm nước" selected={tab === 'bom_nuoc'} color={SH.Cyan} onClick={() => setTab('bom_nuoc')} />
        <ChoiceChip label="Trợ lý giọng nói" selected={tab === 'voice'} color={SH.Purple} onClick={() => setTab('voice')} />
        <ChoiceChip label="An toàn & Màn chờ" selected={tab === 'security'} color={SH.Amber} onClick={() => setTab('security')} />
        <ChoiceChip label="Thông tin & Hệ thống" selected={tab === 'system'} color={SH.Teal} onClick={() => setTab('system')} />
      </div>

      {/* TAB 1: SERVICES (3 columns across desktop, 0 page scroll) */}
      {tab === 'services' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-3 gap-2.5 items-stretch">
          {/* MQTT Config */}
          <SmartCard className="flex flex-col justify-between gap-2.5 p-3">
            <SectionHeader
              title="Máy chủ MQTT (Gateway Node.js)"
              subtitle="Cấu hình lưu trong gateway, dùng chung mọi trình duyệt"
              action={<TrangThaiKetNoi ketNoi={mqttStatus} dien={dien} nowMs={nowMs} />}
            />

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="sm:col-span-2">
                <label className="block text-xs text-[#8A9BB8] mb-1">Host / IP</label>
                <input type="text" value={mqttHost} onChange={(e) => setMqttHost(e.target.value)} placeholder="192.168.0.x" className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Cổng</label>
                <input type="number" value={mqttPort} onChange={(e) => setMqttPort(e.target.value)} className={inputCls} />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Tài khoản</label>
                <input type="text" value={mqttUser} onChange={(e) => setMqttUser(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Mật khẩu</label>
                <input
                  type="password"
                  value={mqttPass}
                  onChange={(e) => setMqttPass(e.target.value)}
                  placeholder={config.mqtt.coMatKhau ? 'Đã lưu — để trống giữ nguyên' : 'Nhập mật khẩu'}
                  className={inputCls}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-end">
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Client ID</label>
                <input type="text" value={mqttClientId} onChange={(e) => setMqttClientId(e.target.value)} className={inputCls} />
              </div>
              <div className="flex flex-wrap gap-3 pb-1 text-xs text-[#8A9BB8]">
                <label className="inline-flex items-center gap-1.5 cursor-pointer">
                  <input type="checkbox" checked={mqttTls} onChange={(e) => setMqttTls(e.target.checked)} />
                  <span>Bật TLS (8883)</span>
                </label>
                {config.mqtt.coMatKhau && (
                  <label className="inline-flex items-center gap-1.5 cursor-pointer text-[#FCA5A5]">
                    <input type="checkbox" checked={mqttClearPass} onChange={(e) => setMqttClearPass(e.target.checked)} />
                    <span>Xóa mật khẩu cũ</span>
                  </label>
                )}
              </div>
            </div>

            <FlatButton
              color={SH.Blue}
              variant="solid"
              disabled={savingMqtt}
              onClick={() =>
                requireAdminPin('Lưu cấu hình máy chủ MQTT', async () => {
                  setSavingMqtt(true);
                  await luuMqttConfig({
                    host: mqttHost,
                    cong: Number(mqttPort) || 1883,
                    tls: mqttTls,
                    taiKhoan: mqttUser,
                    matKhau: mqttPass || undefined,
                    xoaMatKhau: mqttClearPass,
                    clientId: mqttClientId,
                  });
                  setMqttPass('');
                  setMqttClearPass(false);
                  setSavingMqtt(false);
                })
              }
            >
              <Save size={14} />
              <span>{savingMqtt ? 'Đang lưu…' : 'Lưu & kết nối lại MQTT'}</span>
            </FlatButton>
          </SmartCard>

          {/* go2rtc Camera Config */}
          <SmartCard className="flex flex-col justify-between gap-2.5 p-3">
            <SectionHeader
              title="Camera go2rtc & Đầu ghi 24/24"
              subtitle="Gateway proxy luồng xem & tua lại an toàn, không lộ mật khẩu ra client"
              action={<TrangThaiCameraPill cam={cameraStatus} />}
            />

            {cameraStatus.state === 'loi' && (
              <div className="rounded-xl border border-[#EF4444]/40 bg-[#EF4444]/10 px-3 py-2 text-xs text-[#FCA5A5]">
                {cameraStatus.moTa}
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-4 gap-2.5">
              <div className="sm:col-span-2">
                <label className="block text-xs text-[#8A9BB8] mb-1">Host / IP máy chủ</label>
                <input type="text" value={camHost} onChange={(e) => setCamHost(e.target.value)} placeholder="192.168.0.50" className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Cổng xem</label>
                <input type="number" value={camPort} onChange={(e) => setCamPort(e.target.value)} placeholder="1984" className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Cổng ghi</label>
                <input type="number" value={camRecorderPort} onChange={(e) => setCamRecorderPort(e.target.value)} placeholder="1985" className={inputCls} />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Basic Auth User (nếu có)</label>
                <input type="text" value={camUser} onChange={(e) => setCamUser(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Basic Auth Pass</label>
                <input
                  type="password"
                  value={camPass}
                  onChange={(e) => setCamPass(e.target.value)}
                  placeholder={config.camera.coMatKhau ? 'Đã lưu — để trống giữ nguyên' : 'Không bắt buộc'}
                  className={inputCls}
                />
              </div>
            </div>

            <div className="flex flex-wrap gap-3 text-xs text-[#8A9BB8]">
              <label className="inline-flex items-center gap-1.5 cursor-pointer">
                <input type="checkbox" checked={camTls} onChange={(e) => setCamTls(e.target.checked)} />
                <span>Dùng HTTPS/WSS</span>
              </label>
              {config.camera.coMatKhau && (
                <label className="inline-flex items-center gap-1.5 cursor-pointer text-[#FCA5A5]">
                  <input type="checkbox" checked={camClearPass} onChange={(e) => setCamClearPass(e.target.checked)} />
                  <span>Xóa mật khẩu cũ</span>
                </label>
              )}
            </div>

            <FlatButton
              color={SH.Green}
              variant="solid"
              disabled={savingCam}
              onClick={() =>
                requireAdminPin('Lưu cấu hình Camera go2rtc & Đầu ghi', async () => {
                  setSavingCam(true);
                  await luuCameraConfig({
                    host: camHost,
                    cong: Number(camPort) || 1984,
                    congGhiHinh: Number(camRecorderPort) || 1985,
                    tls: camTls,
                    taiKhoan: camUser,
                    matKhau: camPass || undefined,
                    xoaMatKhau: camClearPass,
                  });
                  setCamPass('');
                  setCamClearPass(false);
                  setSavingCam(false);
                })
              }
            >
              <Video size={14} />
              <span>{savingCam ? 'Đang dò luồng…' : 'Lưu & dò go2rtc'}</span>
            </FlatButton>
          </SmartCard>

          {/* Weather Config */}
          <SmartCard className="flex flex-col justify-between gap-2.5 p-3">
            <SectionHeader
              title="Địa điểm thời tiết (Open-Meteo)"
              action={<CloudSun size={18} className="text-[#F59E0B]" />}
            />
            <div className="flex flex-wrap gap-2">
              {WEATHER_PRESETS.map((p) => (
                <ChoiceChip
                  key={p.ten}
                  label={p.ten}
                  selected={wTen === p.ten}
                  color={SH.Amber}
                  onClick={() => {
                    setWTen(p.ten);
                    setWLat(String(p.lat));
                    setWLon(String(p.lon));
                    setWErr(null);
                  }}
                />
              ))}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Tên hiển thị</label>
                <input type="text" value={wTen} onChange={(e) => setWTen(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Vĩ độ (-90..90)</label>
                <input
                  type="number"
                  step="0.0001"
                  value={wLat}
                  onChange={(e) => {
                    setWLat(e.target.value);
                    setWErr(null);
                  }}
                  className={inputCls}
                />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Kinh độ (-180..180)</label>
                <input
                  type="number"
                  step="0.0001"
                  value={wLon}
                  onChange={(e) => {
                    setWLon(e.target.value);
                    setWErr(null);
                  }}
                  className={inputCls}
                />
              </div>
            </div>
            {wErr && <p className="text-xs text-[#EF4444]">{wErr}</p>}
            <FlatButton
              color={SH.Amber}
              disabled={savingWeather}
              onClick={async () => {
                const latNum = Number.parseFloat(wLat.trim());
                const lonNum = Number.parseFloat(wLon.trim());
                if (!Number.isFinite(latNum) || latNum < -90 || latNum > 90) {
                  setWErr('Vĩ độ phải nằm trong khoảng -90 đến 90.');
                  return;
                }
                if (!Number.isFinite(lonNum) || lonNum < -180 || lonNum > 180) {
                  setWErr('Kinh độ phải nằm trong khoảng -180 đến 180.');
                  return;
                }
                setWErr(null);
                setSavingWeather(true);
                await luuWeatherConfig({
                  ten: wTen.trim() || 'Hà Nội',
                  lat: latNum,
                  lon: lonNum,
                });
                setSavingWeather(false);
              }}
            >
              <Save size={14} />
              <span>{savingWeather ? 'Đang cập nhật…' : 'Lưu địa điểm'}</span>
            </FlatButton>
          </SmartCard>
        </div>
      )}

      {/* TAB 1.5: BOM NUOC AQUACONTROL */}
      {tab === 'bom_nuoc' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5 items-stretch">
          <SmartCard className="lg:col-span-7 flex flex-col justify-between gap-3 p-3.5 lg:overflow-y-auto">
            <SectionHeader
              title="Bo bơm nước ESP32 — AquaControl (Gateway Node.js)"
              subtitle="Tách biệt với Mosquitto số điện. Dùng LAN 1s khi ở nhà và EMQX MQTT 30s khi ở xa."
              action={<TrangThaiKetNoiBomPill ketNoi={ketNoiBom} kenh={kenhBom} />}
            />

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-[#8A9BB8] mb-1">Địa chỉ bo trong mạng LAN</label>
                <input
                  type="text"
                  value={bomEspHost}
                  onChange={(e) => setBomEspHost(e.target.value)}
                  placeholder="vd: 192.168.0.x hoặc bomnuoc.local"
                  className={inputCls}
                />
              </div>

              <label className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 cursor-pointer">
                <div>
                  <div className="font-semibold text-[#E6EDF7]">Ưu tiên kênh LAN (HTTP 1 giây/lần)</div>
                  <div className="text-[11px] text-[#8A9BB8]">Tự chuyển sang MQTT nếu Gateway không gọi được bo qua LAN</div>
                </div>
                <input
                  type="checkbox"
                  checked={bomUuTienLan}
                  onChange={(e) => setBomUuTienLan(e.target.checked)}
                />
              </label>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-2">
                  <label className="block text-[#8A9BB8] mb-1">Địa chỉ broker MQTT (EMQX / Mosquitto)</label>
                  <input
                    type="text"
                    value={bomBrokerHost}
                    onChange={(e) => setBomBrokerHost(e.target.value)}
                    placeholder="vd: abc123.ala.asia-southeast1.emqxsl.com"
                    className={inputCls}
                  />
                </div>
                <div>
                  <label className="block text-[#8A9BB8] mb-1">Cổng</label>
                  <input
                    type="number"
                    value={bomBrokerPort}
                    onChange={(e) => setBomBrokerPort(e.target.value)}
                    placeholder="8883"
                    className={inputCls}
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-[#8A9BB8] mb-1">Tài khoản MQTT bơm</label>
                  <input
                    type="text"
                    value={bomBrokerUser}
                    onChange={(e) => setBomBrokerUser(e.target.value)}
                    className={inputCls}
                  />
                </div>
                <div>
                  <label className="block text-[#8A9BB8] mb-1">Mật khẩu MQTT bơm</label>
                  <input
                    type="password"
                    value={bomBrokerPass}
                    onChange={(e) => setBomBrokerPass(e.target.value)}
                    placeholder={config.bomNuoc.coMatKhau ? 'Đã lưu — để trống giữ nguyên' : 'Nhập mật khẩu'}
                    className={inputCls}
                  />
                </div>
              </div>

              <div className="flex flex-wrap gap-4 text-[#8A9BB8]">
                <label className="inline-flex items-center gap-1.5 cursor-pointer">
                  <input type="checkbox" checked={bomBrokerTls} onChange={(e) => setBomBrokerTls(e.target.checked)} />
                  <span>Bật TLS (ssl:// 8883)</span>
                </label>
                {config.bomNuoc.coMatKhau && (
                  <label className="inline-flex items-center gap-1.5 cursor-pointer text-[#FCA5A5]">
                    <input type="checkbox" checked={bomClearPass} onChange={(e) => setBomClearPass(e.target.checked)} />
                    <span>Xóa mật khẩu cũ</span>
                  </label>
                )}
              </div>
            </div>

            <FlatButton
              color={SH.Cyan}
              variant="solid"
              disabled={savingBom}
              onClick={() =>
                requireAdminPin('Lưu cấu hình kết nối bo bơm nước', async () => {
                  setSavingBom(true);
                  await luuBomNuocConfig({
                    espHost: bomEspHost,
                    uuTienLan: bomUuTienLan,
                    brokerHost: bomBrokerHost,
                    brokerPort: Number(bomBrokerPort) || 8883,
                    brokerTls: bomBrokerTls,
                    brokerUser: bomBrokerUser,
                    brokerPass: bomBrokerPass || undefined,
                    xoaMatKhau: bomClearPass,
                  });
                  setBomBrokerPass('');
                  setBomClearPass(false);
                  setSavingBom(false);
                })
              }
            >
              <Droplets size={14} />
              <span>{savingBom ? 'Đang lưu…' : 'Lưu & kết nối lại bo bơm'}</span>
            </FlatButton>
          </SmartCard>

          <SmartCard className="lg:col-span-5 flex flex-col justify-between gap-3 p-3.5 lg:overflow-y-auto">
            <div className="space-y-2.5 text-xs">
              <SectionHeader title="Trạng thái đường truyền" />
              <div className="space-y-1.5">
                <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                  <span className="text-[#8A9BB8]">Trạng thái</span>
                  <span className="font-semibold text-[#E6EDF7] text-right">{nhanKetNoiBom(ketNoiBom, kenhBom)}</span>
                </div>
                <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                  <span className="text-[#8A9BB8]">Kênh đang dùng</span>
                  <span className="font-mono-num font-bold text-[#E6EDF7]">
                    {kenhBom === 'LAN'
                      ? 'LAN trực tiếp (1s)'
                      : kenhBom === 'MQTT'
                        ? `MQTT (nhịp ${bomNuoc?.hb ?? 30}s)`
                        : 'Chưa kết nối'}
                  </span>
                </div>
                <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                  <span className="text-[#8A9BB8]">Địa chỉ LAN</span>
                  <span className="font-mono-num text-[#E6EDF7]">{config.bomNuoc.espHost || 'Chưa nhập'}</span>
                </div>
                <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                  <span className="text-[#8A9BB8]">Broker MQTT</span>
                  <span className="font-mono-num text-[#E6EDF7]">{config.bomNuoc.brokerHost || 'Chưa nhập'}</span>
                </div>
                <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                  <span className="text-[#8A9BB8]">Mật khẩu MQTT</span>
                  <span className="font-semibold text-[#E6EDF7]">
                    {config.bomNuoc.coMatKhau ? 'Đã lưu trên Gateway' : 'Chưa nhập'}
                  </span>
                </div>
                {bomNuoc?.fw && (
                  <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                    <span className="text-[#8A9BB8]">Firmware bo</span>
                    <span className="font-mono-num text-[#E6EDF7]">{bomNuoc.fw}</span>
                  </div>
                )}
                {bomNuoc?.ip && (
                  <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                    <span className="text-[#8A9BB8]">IP hiện tại</span>
                    <span className="font-mono-num text-[#E6EDF7]">{bomNuoc.ip}</span>
                  </div>
                )}
                {bomNuoc && wifiRssi(bomNuoc) !== null && (
                  <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                    <span className="text-[#8A9BB8]">WiFi RSSI</span>
                    <span className="font-mono-num text-[#E6EDF7]">{wifiRssi(bomNuoc)} dBm</span>
                  </div>
                )}
                {bomNuoc?.up !== null && bomNuoc?.up !== undefined && (
                  <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                    <span className="text-[#8A9BB8]">Thời gian hoạt động</span>
                    <span className="font-mono-num text-[#E6EDF7]">{dinhDangThoiGianChay(bomNuoc.up)}</span>
                  </div>
                )}
              </div>
            </div>

            <div className="space-y-2 border-t border-[#253459] pt-2.5 text-[11px] text-[#8A9BB8]">
              <div>
                <span className="font-semibold text-[#E6EDF7]">Bảo mật Gateway: </span>
                Mật khẩu EMQX chỉ lưu trong <code className="text-[#E6EDF7]">web/data/config.json</code> phía Node.js và không bao giờ gửi xuống trình duyệt.
              </div>
              <div>
                <span className="font-semibold text-[#E6EDF7]">Topic cố định: </span>
                <code className="text-[#E6EDF7]">bomnuoc/state</code>, <code className="text-[#E6EDF7]">bomnuoc/status</code>, <code className="text-[#E6EDF7]">bomnuoc/usage</code> và <code className="text-[#E6EDF7]">bomnuoc/cmd</code>.
              </div>
            </div>
          </SmartCard>
        </div>
      )}

      {/* TAB 2: VOICE ASSISTANT */}
      {tab === 'voice' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
          <SmartCard className="lg:col-span-7 flex flex-col justify-between gap-2.5 p-3 lg:overflow-y-auto">
            <SectionHeader
              title="Cấu hình Gemini Live (trên trình duyệt này)"
              subtitle="Khóa API lưu an toàn trong localStorage, trình duyệt gọi trực tiếp wss://generativelanguage.googleapis.com"
            />

            <div>
              <label className="block text-xs text-[#8A9BB8] mb-1">Gemini API Key</label>
              <div className="flex gap-2">
                <input
                  type={showApiKey ? 'text' : 'password'}
                  value={vKey}
                  onChange={(e) => setVKey(e.target.value)}
                  placeholder="AIza..."
                  className={inputCls}
                />
                <button
                  type="button"
                  onClick={() => setShowApiKey((s) => !s)}
                  className="rounded-xl border border-[#253459] bg-[#0E1627] px-3 text-[#8A9BB8] hover:text-white cursor-pointer"
                >
                  {showApiKey ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Model Gemini Live</label>
                <input type="text" value={vModel} onChange={(e) => setVModel(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Giọng đọc (Voice)</label>
                <select
                  value={vVoice}
                  onChange={(e) => setVVoice(e.target.value)}
                  className={inputCls}
                >
                  {GIONG_CO_SAN.map((g) => (
                    <option key={g} value={g}>
                      {g}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="text-xs text-[#8A9BB8]">Chế độ chống vọng loa (Bán song công)</div>
              <div className="flex flex-wrap gap-2">
                {BAN_SONG_CONG_LIST.map((b) => (
                  <ChoiceChip
                    key={b.id}
                    label={b.nhan}
                    selected={vBanSongCong === b.id}
                    color={SH.Purple}
                    onClick={() => setVBanSongCong(b.id)}
                  />
                ))}
              </div>
            </div>

            <DeviceSwitchRow
              icon={<Wifi size={16} />}
              label="Bắn cả lệnh của thiết bị mẫu ra MQTT"
              subtitle="Công tắc thật luôn gửi qua MQTT; bật nếu muốn thiết bị mô phỏng cũng bắn gói smarthome/control"
              color={SH.Green}
              checked={vMqtt}
              onToggle={() => setVMqtt((m) => !m)}
            />

            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="block text-[11px] text-[#8A9BB8] mb-1">Tự ngắt im lặng (s)</label>
                <input type="number" value={vImLang} onChange={(e) => setVImLang(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-[11px] text-[#8A9BB8] mb-1">Tối đa phiên (60–900s)</label>
                <input type="number" min={60} max={900} value={vToiDaPhien} onChange={(e) => setVToiDaPhien(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-[11px] text-[#8A9BB8] mb-1">Trần mỗi ngày (phút)</label>
                <input type="number" value={vPhutNgay} onChange={(e) => setVPhutNgay(e.target.value)} className={inputCls} />
              </div>
            </div>

            <div className="flex items-center gap-3">
              <FlatButton
                color={SH.Purple}
                variant="solid"
                onClick={() => {
                  requireAdminPin('Lưu khóa API & cấu hình Trợ lý giọng nói', () => {
                    const clampedPhien = Math.min(900, Math.max(60, Number(vToiDaPhien) || 600));
                    setVToiDaPhien(String(clampedPhien));
                    updateVoiceConfig({
                      khoaApi: vKey.trim(),
                      model: vModel.trim() || MODEL_MAC_DINH,
                      giongDoc: vVoice,
                      guiMqtt: vMqtt,
                      banSongCong: vBanSongCong,
                      giayImLangTuDong: Math.max(0, Number(vImLang) || 45),
                      giayToiDaPhien: clampedPhien,
                      phutMoiNgay: Math.max(0, Number(vPhutNgay) || 30),
                    });
                    setVoiceSavedFlag(true);
                    window.setTimeout(() => setVoiceSavedFlag(false), 2500);
                  });
                }}
              >
                <Save size={14} />
                <span>Lưu cài đặt giọng nói</span>
              </FlatButton>
              {voiceSavedFlag && (
                <span className="inline-flex items-center gap-1 text-xs text-[#22C55E]">
                  <Check size={14} /> Đã lưu
                </span>
              )}
            </div>
          </SmartCard>

          <SmartCard className="lg:col-span-5 flex flex-col gap-2.5 p-3">
            <SectionHeader
              title="Hạn mức & Thống kê 30 ngày"
              subtitle={`7 ngày gần đây: ${Math.floor(bayNgayGiay / 60)}p ${bayNgayGiay % 60}s`}
              action={
                <FlatButton
                  color={SH.Red}
                  onClick={() => requireAdminPin('Xóa nhật ký sử dụng giọng nói', clearVoiceUsage)}
                >
                  <Trash2 size={13} />
                  <span>Xóa nhật ký</span>
                </FlatButton>
              }
            />

            {/* Daily Quota Bar */}
            <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-[#8A9BB8]">Hạn mức hôm nay ({voiceConfig.phutMoiNgay} phút)</span>
                <span className="font-semibold" style={{ color: quotaColor }}>
                  {tranGiayNgay <= 0
                    ? 'Không giới hạn'
                    : homNayGiay >= tranGiayNgay
                      ? 'Đã chạm trần'
                      : `Còn ${conLaiPhut} phút`}
                </span>
              </div>
              <div className="h-2 w-full rounded-full bg-[#1C2946] overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{ width: `${quotaPct}%`, backgroundColor: quotaColor }}
                />
              </div>
              <div className="grid grid-cols-3 gap-2 pt-1 text-center">
                <div className="rounded-lg bg-[#16213A] px-2 py-1.5">
                  <div className="text-[10px] text-[#8A9BB8]">Hôm nay</div>
                  <div className="font-mono-num text-xs font-bold text-[#E6EDF7]">
                    {Math.floor(homNayGiay / 60)}p {homNayGiay % 60}s
                  </div>
                </div>
                <div className="rounded-lg bg-[#16213A] px-2 py-1.5">
                  <div className="text-[10px] text-[#8A9BB8]">Số phiên</div>
                  <div className="font-mono-num text-xs font-bold text-[#A78BFA]">
                    {homNayRec.soPhien}
                  </div>
                </div>
                <div className="rounded-lg bg-[#16213A] px-2 py-1.5">
                  <div className="text-[10px] text-[#8A9BB8]">Token hôm nay</div>
                  <div className="font-mono-num text-xs font-bold text-[#22D3EE]">
                    {homNayRec.tokenTong.toLocaleString('vi-VN')}
                  </div>
                </div>
              </div>
            </div>
            {voiceUsage.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#253459] py-8 text-center text-xs text-[#8A9BB8]">
                Chưa có phiên trò chuyện giọng nói nào trên trình duyệt này.
              </div>
            ) : (
              <BarChart
                values={voiceUsage.map((x) => Math.max(0.1, x.giay / 60))}
                labels={voiceUsage.map((x) => x.ngay.slice(5))}
                color={SH.Purple}
                unit="p"
              />
            )}
          </SmartCard>
        </div>
      )}

      {/* TAB 2.5: AN TOAN, MAN HINH CHO, TELEGRAM & KHOA PIN */}
      {tab === 'security' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-3 gap-2.5 items-stretch">
          {/* Card 1: Telegram Emergency Alerts */}
          <SmartCard className="flex flex-col justify-between gap-2.5 p-3 lg:overflow-y-auto">
            <div className="space-y-2.5">
              <SectionHeader
                title="Cảnh báo Telegram (Gateway)"
                subtitle="Báo khẩn bơm khóa, rò gas, mất mạng >5p, điện tăng vọt"
                action={
                  <StatusPill
                    label={telegramStatus?.enabled ? 'Đang bật' : 'Đang tắt'}
                    color={telegramStatus?.enabled ? SH.Green : SH.TextDim}
                  />
                }
              />

              <label className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs cursor-pointer">
                <span className="font-semibold text-[#E6EDF7]">Bật đẩy cảnh báo Telegram 24/7</span>
                <input
                  type="checkbox"
                  checked={tgEnabled}
                  onChange={(e) => setTgEnabled(e.target.checked)}
                />
              </label>

              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Bot Token (lưu trên Gateway)</label>
                <input
                  type="password"
                  value={tgToken}
                  onChange={(e) => setTgToken(e.target.value)}
                  placeholder={config.telegram.coBotToken ? 'Đã lưu — để trống giữ nguyên' : '123456:ABC-DEF...'}
                  className={inputCls}
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs text-[#8A9BB8] mb-1">Chat ID</label>
                  <input
                    type="text"
                    value={tgChatId}
                    onChange={(e) => setTgChatId(e.target.value)}
                    placeholder="-100... hoặc ID cá nhân"
                    className={inputCls}
                  />
                </div>
                <div>
                  <label className="block text-xs text-[#8A9BB8] mb-1">Rớt mạng (phút)</label>
                  <input
                    type="number"
                    min={1}
                    max={120}
                    value={tgOfflineMin}
                    onChange={(e) => setTgOfflineMin(e.target.value)}
                    className={inputCls}
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="block text-[11px] text-[#8A9BB8] mb-1">Điện vọt (%)</label>
                  <input
                    type="number"
                    min={10}
                    max={500}
                    value={tgSpikePct}
                    onChange={(e) => setTgSpikePct(e.target.value)}
                    className={inputCls}
                  />
                </div>
                <div>
                  <label className="block text-[11px] text-[#8A9BB8] mb-1">Gas (ppm)</label>
                  <input
                    type="number"
                    min={50}
                    max={5000}
                    value={tgGasPpm}
                    onChange={(e) => setTgGasPpm(e.target.value)}
                    className={inputCls}
                  />
                </div>
                <div>
                  <label className="block text-[11px] text-[#8A9BB8] mb-1">Chống lặp (p)</label>
                  <input
                    type="number"
                    min={1}
                    max={1440}
                    value={tgCooldownMin}
                    onChange={(e) => setTgCooldownMin(e.target.value)}
                    className={inputCls}
                  />
                </div>
              </div>

              {config.telegram.coBotToken && (
                <label className="inline-flex items-center gap-1.5 text-xs text-[#FCA5A5] cursor-pointer">
                  <input
                    type="checkbox"
                    checked={tgClearToken}
                    onChange={(e) => setTgClearToken(e.target.checked)}
                  />
                  <span>Xóa Bot Token đã lưu</span>
                </label>
              )}

              {telegramStatus && telegramStatus.history.length > 0 && (
                <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-2 space-y-1 max-h-24 overflow-y-auto">
                  <div className="text-[10px] font-semibold text-[#8A9BB8] uppercase">Tin nhắn gần đây</div>
                  {telegramStatus.history.slice(0, 4).map((h) => (
                    <div key={h.id} className="flex items-center justify-between gap-2 text-[11px]">
                      <span className="truncate text-[#E6EDF7]">{h.title}</span>
                      <span className={h.ok ? 'text-[#22C55E] shrink-0' : 'text-[#EF4444] shrink-0'}>
                        {h.ok ? 'Đã gửi' : 'Lỗi'}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="flex flex-wrap gap-2">
              <FlatButton
                color={SH.Blue}
                variant="solid"
                disabled={savingTg}
                className="flex-1"
                onClick={() =>
                  requireAdminPin('Lưu cấu hình cảnh báo Telegram', async () => {
                    setSavingTg(true);
                    await luuTelegramConfig({
                      enabled: tgEnabled,
                      botToken: tgToken || undefined,
                      xoaBotToken: tgClearToken,
                      chatId: tgChatId,
                      offlineMinutes: Math.max(1, Number(tgOfflineMin) || 5),
                      electricSpikePct: Math.max(10, Number(tgSpikePct) || 40),
                      electricSpikeMinKwh: config.telegram.electricSpikeMinKwh || 3,
                      gasThresholdPpm: Math.max(50, Number(tgGasPpm) || 200),
                      cooldownMinutes: Math.max(1, Number(tgCooldownMin) || 30),
                      pirTopic: tgPirTopic.trim() || 'smarthome/sensor/pir',
                      gasTopic: tgGasTopic.trim() || 'smarthome/sensor/gas',
                    });
                    setTgToken('');
                    setTgClearToken(false);
                    setSavingTg(false);
                  })
                }
              >
                <BellRing size={14} />
                <span>{savingTg ? 'Đang lưu…' : 'Lưu Telegram'}</span>
              </FlatButton>
              <FlatButton
                color={SH.Teal}
                disabled={testingTg}
                onClick={() =>
                  requireAdminPin('Gửi thử tin nhắn Telegram', async () => {
                    setTestingTg(true);
                    await testTelegramAlert();
                    setTestingTg(false);
                  })
                }
              >
                <Send size={14} />
                <span>{testingTg ? 'Đang gửi…' : 'Gửi thử'}</span>
              </FlatButton>
            </div>
          </SmartCard>

          {/* Card 2: Ambient Night Screensaver */}
          <SmartCard className="flex flex-col justify-between gap-2.5 p-3 lg:overflow-y-auto">
            <div className="space-y-2.5">
              <SectionHeader
                title="Màn hình chờ ban đêm"
                subtitle="Nền đen #000000 · Pixel Shift · Giảm sáng sau 22:00"
                action={<Moon size={18} className="text-[#9E7238]" />}
              />

              <label className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs cursor-pointer">
                <span className="font-semibold text-[#E6EDF7]">Tự động vào màn hình chờ khi rỗi</span>
                <input
                  type="checkbox"
                  checked={screensaverConfig.enabled}
                  onChange={(e) =>
                    updateScreensaverConfig({ ...screensaverConfig, enabled: e.target.checked })
                  }
                />
              </label>

              <div>
                <div className="text-xs text-[#8A9BB8] mb-1.5">Thời gian không chạm để vào màn chờ</div>
                <div className="flex flex-wrap gap-1.5">
                  {[1, 3, 5, 10].map((m) => (
                    <ChoiceChip
                      key={m}
                      label={`${m} phút`}
                      selected={screensaverConfig.idleMinutes === m}
                      color={SH.Amber}
                      onClick={() =>
                        updateScreensaverConfig({ ...screensaverConfig, idleMinutes: m })
                      }
                    />
                  ))}
                </div>
              </div>

              <label className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs cursor-pointer">
                <div>
                  <div className="font-semibold text-[#E6EDF7]">Giảm sáng ban đêm (22:00 – 06:00)</div>
                  <div className="text-[11px] text-[#8A9BB8]">
                    Phủ lớp đen mờ {Math.round(screensaverConfig.webDimOpacity * 100)}% chống chói
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={screensaverConfig.nightDimEnabled}
                  onChange={(e) =>
                    updateScreensaverConfig({
                      ...screensaverConfig,
                      nightDimEnabled: e.target.checked,
                    })
                  }
                />
              </label>

              <div>
                <div className="flex justify-between text-xs text-[#8A9BB8] mb-1">
                  <span>Độ mờ lớp phủ đêm (Web)</span>
                  <span className="font-mono-num text-[#E6EDF7]">
                    {Math.round(screensaverConfig.webDimOpacity * 100)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={15}
                  max={70}
                  step={5}
                  value={Math.round(screensaverConfig.webDimOpacity * 100)}
                  onChange={(e) =>
                    updateScreensaverConfig({
                      ...screensaverConfig,
                      webDimOpacity: Number(e.target.value) / 100,
                    })
                  }
                  className="w-full sh-slider"
                />
              </div>

              <label className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs cursor-pointer">
                <div>
                  <div className="font-semibold text-[#E6EDF7]">Đánh thức khi PIR phát hiện người</div>
                  <div className="text-[11px] text-[#8A9BB8] font-mono-num">
                    {screensaverConfig.pirTopic}
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={screensaverConfig.pirWakeEnabled}
                  onChange={(e) =>
                    updateScreensaverConfig({
                      ...screensaverConfig,
                      pirWakeEnabled: e.target.checked,
                    })
                  }
                />
              </label>

              <div>
                <label className="block text-[11px] text-[#8A9BB8] mb-1">Topic MQTT cảm biến PIR / Gas</label>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    value={tgPirTopic}
                    onChange={(e) => {
                      setTgPirTopic(e.target.value);
                      updateScreensaverConfig({ ...screensaverConfig, pirTopic: e.target.value });
                    }}
                    placeholder="smarthome/sensor/pir"
                    className={inputCls}
                  />
                  <input
                    type="text"
                    value={tgGasTopic}
                    onChange={(e) => {
                      setTgGasTopic(e.target.value);
                      updateScreensaverConfig({ ...screensaverConfig, gasTopic: e.target.value });
                    }}
                    placeholder="smarthome/sensor/gas"
                    className={inputCls}
                  />
                </div>
              </div>
            </div>

            <FlatButton color={SH.Amber} variant="solid" onClick={enterScreensaver}>
              <Moon size={14} />
              <span>Bật màn hình chờ ngay</span>
            </FlatButton>
          </SmartCard>

          {/* Card 3: 4-digit Admin PIN Lock */}
          <SmartCard className="flex flex-col justify-between gap-2.5 p-3 lg:overflow-y-auto">
            <div className="space-y-2.5">
              <SectionHeader
                title="Khóa PIN 4 số Kỹ thuật"
                subtitle="Chặn bấm nhầm cấu hình MQTT, WiFi, hiệu chuẩn bơm & xóa log"
                action={
                  <StatusPill
                    label={!adminLockEnabled ? 'Đang tắt' : adminUnlocked ? 'Đã mở' : 'Đang khóa'}
                    color={!adminLockEnabled ? SH.TextDim : adminUnlocked ? SH.Green : SH.Amber}
                  />
                }
              />

              <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-2.5 text-[11px] text-[#94A3B8] space-y-1">
                <div className="font-semibold text-[#E6EDF7] flex items-center gap-1.5">
                  <Shield size={13} className="text-[#F59E0B]" />
                  <span>Các thao tác được bảo vệ:</span>
                </div>
                <p>Hiệu chuẩn mức nước & ngưỡng bảo vệ bơm, Cấu hình bo bơm, Reboot Router / Đổi mật khẩu WiFi, Xóa nhật ký, Đổi MQTT / Camera / API Key / Telegram.</p>
              </div>

              {adminLockEnabled && adminUnlocked && (
                <FlatButton color={SH.Amber} onClick={() => void lockAdmin()} className="w-full">
                  <Lock size={14} />
                  <span>Khóa khu vực kỹ thuật ngay</span>
                </FlatButton>
              )}

              <div className="border-t border-[#253459] pt-2 space-y-2">
                <div className="text-xs font-semibold text-[#E6EDF7]">Đổi mã PIN (4 chữ số)</div>
                <input
                  type="password"
                  inputMode="numeric"
                  maxLength={4}
                  value={pinCur}
                  onChange={(e) => setPinCur(e.target.value.replace(/\D/g, '').slice(0, 4))}
                  placeholder="PIN hiện tại (mặc định 1234)"
                  className={inputCls}
                />
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="password"
                    inputMode="numeric"
                    maxLength={4}
                    value={pinNew}
                    onChange={(e) => setPinNew(e.target.value.replace(/\D/g, '').slice(0, 4))}
                    placeholder="PIN mới (4 số)"
                    className={inputCls}
                  />
                  <input
                    type="password"
                    inputMode="numeric"
                    maxLength={4}
                    value={pinConfirm}
                    onChange={(e) => setPinConfirm(e.target.value.replace(/\D/g, '').slice(0, 4))}
                    placeholder="Nhập lại PIN mới"
                    className={inputCls}
                  />
                </div>
                {pinMsg && (
                  <div
                    className={`rounded-xl px-2.5 py-1.5 text-[11px] ${
                      pinMsg.ok
                        ? 'bg-[#22C55E]/15 text-[#86EFAC] border border-[#22C55E]/40'
                        : 'bg-[#EF4444]/15 text-[#FCA5A5] border border-[#EF4444]/40'
                    }`}
                  >
                    {pinMsg.text}
                  </div>
                )}
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <FlatButton
                color={SH.Blue}
                variant="solid"
                disabled={savingPin}
                onClick={async () => {
                  if (pinNew.length !== 4 || pinNew !== pinConfirm) {
                    setPinMsg({ ok: false, text: 'PIN mới phải đúng 4 chữ số và khớp nhau.' });
                    return;
                  }
                  setSavingPin(true);
                  const res = await updateAdminPin({ currentPin: pinCur, newPin: pinNew });
                  setSavingPin(false);
                  if (res.ok) {
                    setPinCur('');
                    setPinNew('');
                    setPinConfirm('');
                    setPinMsg({ ok: true, text: 'Đã đổi mã PIN thành công.' });
                  } else {
                    setPinMsg({ ok: false, text: res.error || 'Không đổi được mã PIN.' });
                  }
                }}
              >
                <LockOpen size={14} />
                <span>{savingPin ? 'Đang lưu…' : 'Cập nhật mã PIN mới'}</span>
              </FlatButton>

              <FlatButton
                color={adminLockEnabled ? SH.Red : SH.Green}
                onClick={async () => {
                  if (pinCur.length !== 4) {
                    setPinMsg({ ok: false, text: 'Nhập mã PIN hiện tại ở trên để bật/tắt khóa.' });
                    return;
                  }
                  setSavingPin(true);
                  const res = await updateAdminPin({
                    currentPin: pinCur,
                    enabled: !adminLockEnabled,
                  });
                  setSavingPin(false);
                  if (res.ok) {
                    setPinCur('');
                    setPinMsg({
                      ok: true,
                      text: !adminLockEnabled ? 'Đã bật khóa PIN kỹ thuật.' : 'Đã tạm tắt khóa PIN.',
                    });
                  } else {
                    setPinMsg({ ok: false, text: res.error || 'Sai mã PIN hiện tại.' });
                  }
                }}
              >
                <Lock size={14} />
                <span>{adminLockEnabled ? 'Tạm tắt khóa PIN (cần PIN)' : 'Bật lại khóa PIN'}</span>
              </FlatButton>
            </div>
          </SmartCard>
        </div>
      )}

      {/* TAB 3: SYSTEM & UPDATE CHECK */}
      {tab === 'system' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-2 gap-2.5 items-stretch">
          <SmartCard className="space-y-3">
            <SectionHeader title="Phiên bản & Gateway" action={<Server size={17} className="text-[#14B8A6]" />} />
            <div className="space-y-2 text-xs">
              <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                <span className="text-[#8A9BB8]">Phiên bản Web</span>
                <span className="font-mono-num font-bold text-[#E6EDF7]">{APP_WEB_VERSION}</span>
              </div>
              <div className="flex justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                <span className="text-[#8A9BB8]">Đồng bộ mã Android gốc</span>
                <span className="font-mono-num font-bold text-[#E6EDF7]">v{ANDROID_BASE_VERSION}</span>
              </div>
              <div className="flex justify-between items-center rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2">
                <span className="text-[#8A9BB8]">Trạng thái Node.js Gateway</span>
                <StatusPill label={gatewayOnline ? 'Sẵn sàng' : 'Mất kết nối'} color={gatewayOnline ? SH.Green : SH.Red} />
              </div>
            </div>

            {/* Link 4G Cloudflare Quick Tunnel */}
            <div className="pt-2 space-y-2 border-t border-[#253459]">
              <div className="flex items-center justify-between gap-2">
                <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#E6EDF7]">
                  <Globe size={14} className="text-[#22C55E]" />
                  <span>Đường dẫn 4G từ xa (Cloudflare HTTPS)</span>
                </span>
                {webAccessMode === 'cookie' && (
                  <button
                    type="button"
                    onClick={() => void webLogout()}
                    className="inline-flex items-center gap-1 rounded-lg border border-[#EF4444]/40 bg-[#EF4444]/12 px-2 py-1 text-[11px] font-semibold text-[#FCA5A5] hover:brightness-110 cursor-pointer"
                  >
                    <LogOut size={12} />
                    <span>Đăng xuất</span>
                  </button>
                )}
              </div>
              {tunnelUrl ? (
                <div className="space-y-2">
                  <div>
                    <div className="text-[11px] text-[#8A9BB8] mb-1">1. Web Nhà thông minh (4G):</div>
                    <div className="flex items-center gap-2">
                      <input type="text" readOnly value={tunnelUrl} className={inputCls} />
                      <FlatButton
                        color={SH.Green}
                        onClick={() => {
                          void navigator.clipboard?.writeText(tunnelUrl);
                          showToast('Đã sao chép link 4G SmartHome!', 'success');
                        }}
                      >
                        <Copy size={13} />
                        <span>Chép</span>
                      </FlatButton>
                    </div>
                  </div>
                  {cockpitTunnelUrl && (
                    <div>
                      <div className="text-[11px] text-[#8A9BB8] mb-1">2. Quản trị Server Ubuntu (:9090 Cockpit):</div>
                      <div className="flex items-center gap-2">
                        <input type="text" readOnly value={cockpitTunnelUrl} className={inputCls} />
                        <FlatButton
                          color={SH.Purple}
                          onClick={() => {
                            void navigator.clipboard?.writeText(cockpitTunnelUrl);
                            showToast('Đã sao chép link 4G Cockpit :9090!', 'success');
                          }}
                        >
                          <Copy size={13} />
                          <span>Chép</span>
                        </FlatButton>
                        <FlatButton
                          color={SH.Blue}
                          onClick={() => window.open(cockpitTunnelUrl, '_blank', 'noopener,noreferrer')}
                        >
                          <ExternalLink size={13} />
                        </FlatButton>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="rounded-xl border border-dashed border-[#253459] bg-[#0E1627] px-3 py-2 text-[11px] text-[#8A9BB8]">
                  Chưa phát hiện đường hầm cloudflared đang chạy.
                </div>
              )}
            </div>

            <div className="pt-2 space-y-2 border-t border-[#253459]">
              <label className="block text-xs font-semibold text-[#E6EDF7]">
                Địa chỉ Web AquaControl (nếu có)
              </label>
              <div className="flex gap-2">
                <input
                  type="url"
                  value={aquaUrl}
                  onChange={(e) => setAquaUrl(e.target.value)}
                  placeholder="http://192.168.0.50:8080"
                  className={inputCls}
                />
                <FlatButton
                  color={SH.Teal}
                  disabled={savingAqua}
                  onClick={() =>
                    requireAdminPin('Lưu địa chỉ AquaControl Web', async () => {
                      setSavingAqua(true);
                      await luuAquaControlUrl(aquaUrl.trim());
                      setSavingAqua(false);
                    })
                  }
                >
                  <Save size={14} />
                  <span>Lưu</span>
                </FlatButton>
              </div>
            </div>
          </SmartCard>

          <SmartCard className="space-y-3">
            <SectionHeader
              title="Kiểm tra bản phát hành GitHub"
              subtitle="trungpq96dev/app_smart_home · Chỉ kiểm tra phiên bản, không tải APK trên trình duyệt"
            />
            <FlatButton color={SH.Blue} onClick={checkGithubUpdate} disabled={updateCheck.status === 'checking'}>
              <RefreshCw size={14} className={updateCheck.status === 'checking' ? 'animate-spin' : ''} />
              <span>{updateCheck.status === 'checking' ? 'Đang kiểm tra…' : 'Kiểm tra cập nhật'}</span>
            </FlatButton>

            {updateCheck.status === 'up_to_date' && (
              <div className="rounded-xl border border-[#22C55E]/40 bg-[#22C55E]/10 p-3 text-xs text-[#86EFAC]">
                Đang ở phiên bản mới nhất ({updateCheck.latestTag}).
              </div>
            )}

            {updateCheck.status === 'available' && (
              <div className="rounded-xl border border-[#4F8BFF]/40 bg-[#4F8BFF]/10 p-3 space-y-2 text-xs">
                <div className="font-bold text-[#E6EDF7]">
                  Có bản phát hành mới: {updateCheck.release.name} ({updateCheck.release.tagName})
                </div>
                {updateCheck.release.body && (
                  <pre className="whitespace-pre-wrap text-[11px] text-[#8A9BB8] font-sans max-h-36 overflow-y-auto">
                    {updateCheck.release.body}
                  </pre>
                )}
                <a
                  href={updateCheck.release.htmlUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 text-[#4F8BFF] font-semibold hover:underline"
                >
                  <span>Xem trên GitHub Releases</span>
                  <ExternalLink size={13} />
                </a>
              </div>
            )}

            {updateCheck.status === 'error' && (
              <div className="rounded-xl border border-[#EF4444]/40 bg-[#EF4444]/10 p-3 text-xs text-[#FCA5A5]">
                {updateCheck.message}
              </div>
            )}
          </SmartCard>
        </div>
      )}
      <Mic className="hidden" size={1} />
    </div>
  );
}

