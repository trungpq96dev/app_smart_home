import React from 'react';
import {
  BatteryCharging,
  Bolt,
  Power,
  Radio,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Wifi,
} from 'lucide-react';
import {
  CONG_SAC_LIST,
  cuktechCoMauThuan,
  cuktechCongBiTat,
  cuktechDanhSachCong,
  cuktechLaSoCu,
  cuktechMatKetNoiThat,
  cuktechSoCongDangChay,
  cuktechSongTotNhat,
  cuktechTongOat,
  CuktechPiid,
  KICH_BAN_LIST,
  MUC_SONG_NHAN,
  mucSongCua,
  nhanDbmSong,
  nhanGiaoThucCong,
  songLaSoCu,
} from '../data/cuktech.js';
import { isBridgeSong } from '../data/congtac.js';
import {
  ChoiceChip,
  DemoBadge,
  EmptyState,
  FlatButton,
  IconBadge,
  SectionHeader,
  SmartCard,
  StatusPill,
} from '../components/Common.js';
import { DeviceSwitchRow } from '../components/Tiles.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

export function CamBienScreen() {
  const {
    cuktech,
    congtac,
    dien,
    nowMs,
    mqttStatus,
    rooms,
    choCongTacIds,
    dangDoiCuktechCong,
    dangDoiCuktechCaiDat,
    dangNoiLaiCuktech,
    datCongSac,
    datCaiDatCuktech,
    noiLaiCuktech,
    bamCongTac,
    openChiTietDien,
  } = useSmartHome();

  const tongW = cuktechTongOat(cuktech);
  const soDangChay = cuktechSoCongDangChay(cuktech);
  const danhSachCong = cuktechDanhSachCong(cuktech);
  const songBest = cuktechSongTotNhat(cuktech);
  const songList = Object.values(cuktech.song);
  const soCu = cuktechLaSoCu(cuktech, nowMs);
  const matThat = cuktechMatKetNoiThat(cuktech, nowMs);
  const mauThuan = cuktechCoMauThuan(cuktech, nowMs);

  const kitchen = rooms.find((r) => r.id === 'kitchen');
  const hall = rooms.find((r) => r.id === 'hall');

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Cuktech Smart Charger (MQTT + BLE) */}
      <SmartCard className="flex-1 min-h-0 flex flex-col gap-2.5 p-3">
        <SectionHeader
          title="Ổ cắm sạc nhanh Cuktech (BLE → MQTT)"
          subtitle={
            cuktech.coSoThat
              ? `${cuktech.model || 'Cuktech'} · FW ${cuktech.firmware || '—'} · Tổng ${tongW.toFixed(1)}W (${soDangChay} cổng đang sạc)`
              : mqttStatus.state === 'da_noi'
                ? 'Chưa nhận được gói cuktech/charger/status từ MQTT broker'
                : 'Chưa nối MQTT nên chưa đọc được sạc Cuktech'
          }
          action={
            <div className="flex items-center gap-2">
              {cuktech.coSoThat && (
                <StatusPill
                  label={
                    cuktech.noiDuoc
                      ? soCu
                        ? 'Đã nối · số >60s'
                        : 'BLE đang nối'
                      : matThat
                        ? 'Mất kết nối BLE'
                        : mauThuan
                          ? 'Đang chờ BLE đồng bộ'
                          : 'Chưa nối BLE'
                  }
                  color={cuktech.noiDuoc && !soCu ? SH.Green : cuktech.noiDuoc ? SH.Amber : SH.Red}
                />
              )}
              <FlatButton
                color={SH.Blue}
                disabled={dangNoiLaiCuktech || mqttStatus.state !== 'da_noi'}
                onClick={noiLaiCuktech}
              >
                <RefreshCw size={14} className={dangNoiLaiCuktech ? 'animate-spin' : ''} />
                <span>{dangNoiLaiCuktech ? 'Đang nối lại…' : 'Nối lại BLE'}</span>
              </FlatButton>
            </div>
          }
        />

        {!cuktech.coSoThat ? (
          <div className="flex-1 min-h-0 flex items-center justify-center">
            <EmptyState
              icon={<BatteryCharging size={28} />}
              title="Chưa có dữ liệu từ Cuktech Charger"
              description="Ứng dụng không bịa số W/V/A. Đảm bảo script Bluetooth Cuktech đang publish lên topic cuktech/charger/status."
            />
          </div>
        ) : (
          <div className="flex-1 min-h-0 overflow-y-auto space-y-2.5 pr-0.5">
            {/* 4 Charging Ports */}
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
              {CONG_SAC_LIST.map((meta, idx) => {
                const c = danhSachCong[idx];
                const biTat = cuktechCongBiTat(cuktech, meta.id);
                const dangBat = biTat !== null ? !biTat : (c.choPhep ?? c.dangChay);
                const pending = Boolean(dangDoiCuktechCong[meta.id]);
                const color = c.dangChay ? SH.Green : biTat ? SH.Red : SH.Blue;

                return (
                  <div
                    key={meta.id}
                    className="rounded-xl border p-3.5 space-y-2.5"
                    style={{
                      backgroundColor: c.dangChay ? tint(SH.Green, 0.08) : SH.Surface,
                      borderColor: c.dangChay ? tint(SH.Green, 0.35) : SH.Border,
                    }}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-[#E6EDF7]">{meta.nhan}</span>
                      <StatusPill label={nhanGiaoThucCong(c)} color={color} dot={c.dangChay} />
                    </div>
                    <div className="font-mono-num">
                      <div className="text-xl font-bold text-[#E6EDF7]">{c.oat.toFixed(1)} W</div>
                      <div className="text-[11px] text-[#8A9BB8]">
                        {c.volt.toFixed(2)}V · {c.ampe.toFixed(2)}A
                      </div>
                    </div>
                    <FlatButton
                      color={dangBat ? SH.Green : SH.TextSecondary}
                      disabled={pending}
                      className="w-full"
                      onClick={() => datCongSac(meta.id, !dangBat)}
                    >
                      <Power size={13} />
                      <span>{pending ? 'Đang gửi…' : dangBat ? 'Đang cho phép (Bấm tắt)' : 'Đang tắt (Bấm bật)'}</span>
                    </FlatButton>
                  </div>
                );
              })}
            </div>

            {/* Settings & BLE RSSI */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {/* Settings */}
              <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3.5 space-y-3">
                <div className="text-xs font-bold text-[#E6EDF7]">Cấu hình phân bổ & màn hình Cuktech</div>
                <div>
                  <div className="text-[11px] text-[#8A9BB8] mb-1.5">Kịch bản phân bổ công suất</div>
                  <div className="flex flex-wrap gap-2">
                    {KICH_BAN_LIST.map((kb) => (
                      <ChoiceChip
                        key={kb.ma}
                        label={kb.nhan}
                        selected={cuktech.caiDat?.kichBan?.ma === kb.ma}
                        disabled={dangDoiCuktechCaiDat}
                        color={SH.Blue}
                        onClick={() => datCaiDatCuktech(CuktechPiid.KICH_BAN, kb.ma)}
                      />
                    ))}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] text-[#8A9BB8] mb-1.5">Thời gian sáng màn hình</div>
                  <div className="flex flex-wrap gap-2">
                    {CuktechPiid.SANG_MAN_CHON.map(([ma, nhan]) => (
                      <ChoiceChip
                        key={ma}
                        label={nhan}
                        selected={cuktech.caiDat?.thoiGianSangMan === nhan}
                        disabled={dangDoiCuktechCaiDat}
                        color={SH.Purple}
                        onClick={() => datCaiDatCuktech(CuktechPiid.SANG_MAN, ma)}
                      />
                    ))}
                  </div>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1">
                  <FlatButton
                    color={cuktech.caiDat?.sacNhoGiotUsbA ? SH.Green : SH.TextSecondary}
                    disabled={dangDoiCuktechCaiDat}
                    onClick={() => datCaiDatCuktech(CuktechPiid.SAC_NHO_GIOT_A, cuktech.caiDat?.sacNhoGiotUsbA ? 0 : 1)}
                  >
                    Nhỏ giọt USB-A: {cuktech.caiDat?.sacNhoGiotUsbA ? 'Bật' : 'Tắt'}
                  </FlatButton>
                  <FlatButton
                    color={cuktech.caiDat?.tuTatManNhanRoi ? SH.Green : SH.TextSecondary}
                    disabled={dangDoiCuktechCaiDat}
                    onClick={() => datCaiDatCuktech(CuktechPiid.TU_TAT_MAN, cuktech.caiDat?.tuTatManNhanRoi ? 0 : 1)}
                  >
                    Tắt màn rỗi: {cuktech.caiDat?.tuTatManNhanRoi ? 'Bật' : 'Tắt'}
                  </FlatButton>
                  <FlatButton
                    color={cuktech.caiDat?.khoaXoayMan ? SH.Green : SH.TextSecondary}
                    disabled={dangDoiCuktechCaiDat}
                    onClick={() => datCaiDatCuktech(CuktechPiid.KHOA_XOAY, cuktech.caiDat?.khoaXoayMan ? 0 : 1)}
                  >
                    Khóa xoay: {cuktech.caiDat?.khoaXoayMan ? 'Bật' : 'Tắt'}
                  </FlatButton>
                </div>
              </div>

              {/* BLE RSSI Receivers */}
              <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3.5 space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-[#E6EDF7]">Độ mạnh sóng BLE từ các trạm thu</span>
                  {songBest && (
                    <StatusPill
                      label={`Tốt nhất: ${nhanDbmSong(songBest)}`}
                      color={mucSongCua(songBest) === 'TOT' ? SH.Green : SH.Amber}
                    />
                  )}
                </div>
                {songList.length === 0 ? (
                  <div className="text-xs text-[#8A9BB8] py-4 text-center">Chưa có trạm thu BLE báo RSSI về topic cuktech/rssi/#.</div>
                ) : (
                  <div className="space-y-2">
                    {songList.map((s) => {
                      const muc = mucSongCua(s);
                      const stale = songLaSoCu(s);
                      const col = muc === 'TOT' ? SH.Green : muc === 'KHA' ? SH.Blue : SH.Amber;
                      return (
                        <div
                          key={s.nguonId}
                          className="flex items-center justify-between rounded-xl border border-[#253459] bg-[#16213A] px-3 py-2 text-xs"
                        >
                          <div className="flex items-center gap-2">
                            <Wifi size={14} style={{ color: col }} />
                            <div>
                              <div className="font-semibold text-[#E6EDF7]">{s.nguonTen || s.nguonId}</div>
                              <div className="text-[11px] text-[#8A9BB8]">
                                {MUC_SONG_NHAN[muc]}
                                {s.tuoiGiay !== null ? ` · ${s.tuoiGiay}s trước` : ''}
                                {stale ? ' (Số cũ)' : ''}
                              </div>
                            </div>
                          </div>
                          <span className="font-mono-num font-bold" style={{ color: col }}>
                            {nhanDbmSong(s)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </SmartCard>

      {/* Electricity & Switch Sensors + Safety Simulation */}
      <div className="shrink-0 grid grid-cols-1 lg:grid-cols-2 gap-2.5">
        <SmartCard onClick={openChiTietDien} className="space-y-3">
          <SectionHeader
            title="Công tơ & Trạng thái EVN"
            subtitle={dien.capNhat ? `Cập nhật: ${dien.capNhat}` : 'Chưa nhận dữ liệu điện'}
          />
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-xl bg-[#0E1627] border border-[#253459] p-3">
              <div className="text-xs text-[#8A9BB8]">Chỉ số công tơ</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-1">
                {dien.congToChiSo !== null ? `${dien.congToChiSo.toFixed(1)} kWh` : '—'}
              </div>
            </div>
            <div className="rounded-xl bg-[#0E1627] border border-[#253459] p-3">
              <div className="text-xs text-[#8A9BB8]">Dư nợ EVN</div>
              <div className="font-mono-num text-lg font-bold text-[#F59E0B] mt-1">
                {dien.congNoSoTien !== null ? `${Math.round(dien.congNoSoTien).toLocaleString('vi-VN')} đ` : '—'}
              </div>
            </div>
          </div>
          {congtac.danhSach.length > 0 && (
            <div className="space-y-2 pt-2">
              <div className="text-xs font-semibold text-[#8A9BB8]">
                Công tắc thật ({isBridgeSong(congtac) ? 'Bridge đang sống' : 'Bridge ngoại tuyến'})
              </div>
              {congtac.danhSach.map((tb) => {
                const tt = congtac.trangThai[tb.ma];
                const pending = Boolean(choCongTacIds[tb.ma]);
                return (
                  <DeviceSwitchRow
                    key={tb.ma}
                    icon={<Bolt size={15} />}
                    label={tb.ten}
                    subtitle={
                      pending
                        ? 'Đang gửi lệnh…'
                        : !isBridgeSong(congtac)
                          ? 'Bộ nối chưa chạy trên máy chủ'
                          : !tt?.online
                            ? tt?.lyDoLoi || 'Mất kết nối'
                            : tt?.luc
                              ? `Đọc lúc ${tt.luc}`
                              : 'Chưa đọc được trạng thái'
                    }
                    color={SH.Green}
                    checked={tt?.bat === true}
                    pending={pending}
                    disabled={pending}
                    onToggle={() => bamCongTac(tb.phong, tb.thietBi)}
                  />
                );
              })}
            </div>
          )}
        </SmartCard>

        <SmartCard className="space-y-3">
          <SectionHeader
            title="Cảm biến an toàn môi trường"
            subtitle="Mô phỏng cho khu vực chưa gắn cảm biến MQTT"
            badge={<DemoBadge />}
          />
          <div className="space-y-2.5">
            <div className="flex items-center justify-between rounded-xl border border-[#253459] bg-[#0E1627] p-3">
              <div className="flex items-center gap-2.5">
                <IconBadge color={kitchen?.gasAlert ? SH.Red : SH.Green} size={34}>
                  {kitchen?.gasAlert ? <ShieldAlert size={17} /> : <ShieldCheck size={17} />}
                </IconBadge>
                <div>
                  <div className="text-xs font-bold text-[#E6EDF7]">Cảm biến Gas Nhà bếp</div>
                  <div className="text-[11px] text-[#8A9BB8]">Nồng độ {kitchen?.gasPpm ?? 18} ppm</div>
                </div>
              </div>
              <StatusPill label={kitchen?.gasAlert ? 'Rò rỉ Gas!' : 'Bình thường'} color={kitchen?.gasAlert ? SH.Red : SH.Green} />
            </div>

            <div className="flex items-center justify-between rounded-xl border border-[#253459] bg-[#0E1627] p-3">
              <div className="flex items-center gap-2.5">
                <IconBadge color={hall?.hallMotionDetected ? SH.Amber : SH.Blue} size={34}>
                  <Radio size={17} />
                </IconBadge>
                <div>
                  <div className="text-xs font-bold text-[#E6EDF7]">Cảm biến chuyển động PIR Hành lang</div>
                  <div className="text-[11px] text-[#8A9BB8]">
                    Tự bật đèn: {hall?.hallMotionAuto ? 'Bật' : 'Tắt'}
                  </div>
                </div>
              </div>
              <StatusPill
                label={hall?.hallMotionDetected ? 'Có người qua lại' : 'Yên tĩnh'}
                color={hall?.hallMotionDetected ? SH.Amber : SH.Green}
              />
            </div>
          </div>
        </SmartCard>
      </div>
    </div>
  );
}

