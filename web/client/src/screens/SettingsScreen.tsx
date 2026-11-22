import React, { useEffect, useState } from 'react';
import {
  Check,
  CloudSun,
  Droplets,
  ExternalLink,
  Eye,
  EyeOff,
  Mic,
  RefreshCw,
  Save,
  Server,
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

type SettingsTab = 'services' | 'bom_nuoc' | 'voice' | 'system';

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
  } = useSmartHome();

  const [tab, setTab] = useState<SettingsTab>('services');

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
  const [camTls, setCamTls] = useState(config.camera.tls);
  const [camUser, setCamUser] = useState(config.camera.taiKhoan);
  const [camPass, setCamPass] = useState('');
  const [camClearPass, setCamClearPass] = useState(false);
  const [savingCam, setSavingCam] = useState(false);

  // Weather form
  const [wTen, setWTen] = useState(config.weather.ten);
  const [wLat, setWLat] = useState(String(config.weather.lat));
  const [wLon, setWLon] = useState(String(config.weather.lon));
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

  // Sync form defaults once config loads from server
  useEffect(() => {
    setMqttHost(config.mqtt.host);
    setMqttPort(String(config.mqtt.cong));
    setMqttTls(config.mqtt.tls);
    setMqttUser(config.mqtt.taiKhoan);
    setMqttClientId(config.mqtt.clientId);

    setCamHost(config.camera.host);
    setCamPort(String(config.camera.cong));
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
  }, [config]);

  const inputCls =
    'w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#4F8BFF] font-mono-num';

  const homNayGiay = MucDung.cuaNgay(voiceUsage, MucDung.homNay()).giay;
  const bayNgayGiay = MucDung.giayNgayGanDay(voiceUsage, 7);

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      <div className="shrink-0 flex flex-wrap gap-2">
        <ChoiceChip label="Dịch vụ & Kết nối" selected={tab === 'services'} color={SH.Blue} onClick={() => setTab('services')} />
        <ChoiceChip label="Bơm nước" selected={tab === 'bom_nuoc'} color={SH.Cyan} onClick={() => setTab('bom_nuoc')} />
        <ChoiceChip label="Trợ lý giọng nói" selected={tab === 'voice'} color={SH.Purple} onClick={() => setTab('voice')} />
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
              onClick={async () => {
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
              }}
            >
              <Save size={14} />
              <span>{savingMqtt ? 'Đang lưu…' : 'Lưu & kết nối lại MQTT'}</span>
            </FlatButton>
          </SmartCard>

          {/* go2rtc Camera Config */}
          <SmartCard className="space-y-3">
            <SectionHeader
              title="Máy chủ Camera go2rtc"
              subtitle="Gateway proxy luồng camera an toàn, không lộ mật khẩu ra client"
              action={<TrangThaiCameraPill cam={cameraStatus} />}
            />

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="sm:col-span-2">
                <label className="block text-xs text-[#8A9BB8] mb-1">Host / IP go2rtc</label>
                <input type="text" value={camHost} onChange={(e) => setCamHost(e.target.value)} placeholder="192.168.0.x" className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Cổng</label>
                <input type="number" value={camPort} onChange={(e) => setCamPort(e.target.value)} className={inputCls} />
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
              onClick={async () => {
                setSavingCam(true);
                await luuCameraConfig({
                  host: camHost,
                  cong: Number(camPort) || 1984,
                  tls: camTls,
                  taiKhoan: camUser,
                  matKhau: camPass || undefined,
                  xoaMatKhau: camClearPass,
                });
                setCamPass('');
                setCamClearPass(false);
                setSavingCam(false);
              }}
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
                <label className="block text-xs text-[#8A9BB8] mb-1">Vĩ độ (Lat)</label>
                <input type="number" step="0.0001" value={wLat} onChange={(e) => setWLat(e.target.value)} className={inputCls} />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Kinh độ (Lon)</label>
                <input type="number" step="0.0001" value={wLon} onChange={(e) => setWLon(e.target.value)} className={inputCls} />
              </div>
            </div>
            <FlatButton
              color={SH.Amber}
              disabled={savingWeather}
              onClick={async () => {
                setSavingWeather(true);
                await luuWeatherConfig({
                  ten: wTen.trim() || 'Hà Nội',
                  lat: Number(wLat) || 21.0278,
                  lon: Number(wLon) || 105.8342,
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
              onClick={async () => {
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
              }}
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
              label="Cho phép trợ lý gửi lệnh MQTT thật"
              subtitle="Bật để giọng nói điều khiển công tắc thật ngoài đời"
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
                <label className="block text-[11px] text-[#8A9BB8] mb-1">Tối đa 1 phiên (s)</label>
                <input type="number" value={vToiDaPhien} onChange={(e) => setVToiDaPhien(e.target.value)} className={inputCls} />
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
                  updateVoiceConfig({
                    khoaApi: vKey.trim(),
                    model: vModel.trim() || MODEL_MAC_DINH,
                    giongDoc: vVoice,
                    guiMqtt: vMqtt,
                    banSongCong: vBanSongCong,
                    giayImLangTuDong: Math.max(0, Number(vImLang) || 45),
                    giayToiDaPhien: Math.max(30, Number(vToiDaPhien) || 600),
                    phutMoiNgay: Math.max(0, Number(vPhutNgay) || 30),
                  });
                  setVoiceSavedFlag(true);
                  window.setTimeout(() => setVoiceSavedFlag(false), 2500);
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
              title="Thống kê sử dụng 30 ngày"
              subtitle={`Hôm nay: ${Math.floor(homNayGiay / 60)}p ${homNayGiay % 60}s · 7 ngày: ${Math.floor(bayNgayGiay / 60)}p`}
              action={
                <FlatButton color={SH.Red} onClick={clearVoiceUsage}>
                  <Trash2 size={13} />
                  <span>Xóa nhật ký</span>
                </FlatButton>
              }
            />
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
                  onClick={async () => {
                    setSavingAqua(true);
                    await luuAquaControlUrl(aquaUrl.trim());
                    setSavingAqua(false);
                  }}
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

