import React, { useState } from 'react';
import {
  AlertTriangle,
  BatteryCharging,
  Bolt,
  Clock,
  Info,
  Power,
  Radio,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Wifi,
  X,
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
  cuktechTyLePhanBo,
  CuktechPiid,
  KICH_BAN_LIST,
  MUC_SONG_NHAN,
  mucSongCua,
  nhanDbmSong,
  nhanGiaoThucCong,
  songLaSoCu,
  type CongSacId,
  type MucSongBle,
} from '../data/cuktech.js';
import { isBridgeSong, oThietBiBatHienThi, oThietBiRoRang } from '../data/congtac.js';
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

const PORT_COLORS: Record<CongSacId, string> = {
  C1: SH.Blue,
  C2: SH.Green,
  C3: SH.Purple,
  USB_A: SH.Amber,
};

function mauMucSong(muc: MucSongBle): string {
  switch (muc) {
    case 'TOT':
      return SH.Green;
    case 'KHA':
      return SH.Blue;
    case 'YEU':
      return SH.Amber;
    case 'RAT_YEU':
      return SH.Red;
    case 'KHONG_RO':
      return SH.TextDim;
  }
}

function dinhDangHenGio(phut: number | undefined): string {
  if (phut === undefined) return '—';
  if (phut <= 0) return 'Tắt hẹn';
  if (phut >= 60 && phut % 60 === 0) return `${phut / 60} giờ`;
  return `${phut} phút`;
}

export function CamBienScreen() {
  const {
    cuktech,
    congtac,
    dien,
    nowMs,
    mqttStatus,
    rooms,
    choCongSac,
    choCaiDatCuktech,
    dangNoiLaiCuktech,
    datCongSac,
    datCaiDatCuktech,
    noiLaiCuktech,
    oThietBi,
    bamCongTac,
    openChiTietDien,
    mqttGasAlert,
    mqttGasPpm,
  } = useSmartHome();

  const [henGioPort, setHenGioPort] = useState<CongSacId | null>(null);

  const tongW = cuktechTongOat(cuktech);
  const soDangChay = cuktechSoCongDangChay(cuktech);
  const danhSachCong = cuktechDanhSachCong(cuktech);
  const tyLePhanBo = cuktechTyLePhanBo(cuktech);
  const songBest = cuktechSongTotNhat(cuktech);
  const songList = Object.values(cuktech.song);
  const soCu = cuktechLaSoCu(cuktech, nowMs);
  const matThat = cuktechMatKetNoiThat(cuktech, nowMs);
  const mauThuan = cuktechCoMauThuan(cuktech, nowMs);

  const kitchen = rooms.find((r) => r.id === 'kitchen');
  const hall = rooms.find((r) => r.id === 'hall');

  const activeTimers = CONG_SAC_LIST.filter((m) => (cuktech.caiDat?.henGioPhut[m.id] ?? 0) > 0);
  const anyTimerPending = CONG_SAC_LIST.some((m) => Boolean(choCaiDatCuktech[CuktechPiid.henGio(m.id)]));

  const sacNhoGiotVal = cuktech.caiDat?.sacNhoGiotUsbA ?? null;
  const tuTatManVal = cuktech.caiDat?.tuTatManNhanRoi ?? null;
  const khoaXoayVal = cuktech.caiDat?.khoaXoayMan ?? null;

  const gasIsReal = mqttGasPpm !== null || mqttGasAlert;
  const gasDanger = mqttGasAlert || kitchen?.gasAlert === true;
  const gasPpmText = mqttGasPpm !== null ? `${mqttGasPpm} ppm` : `${kitchen?.gasPpm ?? 18} ppm`;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Cuktech Smart Charger (MQTT + BLE) */}
      <SmartCard className="flex-1 min-h-0 flex flex-col gap-2.5 p-3">
        <SectionHeader
          title="Trạm sạc CUKTECH 10 Ultra (BLE → MQTT)"
          subtitle={
            cuktech.coSoThat
              ? `${cuktech.model || 'CUKTECH 10 Ultra'} · FW ${cuktech.firmware || '—'} · Tổng ${tongW.toFixed(1)}W (${soDangChay} cổng đang sạc)`
              : mqttStatus.state === 'da_noi'
                ? 'Chưa nhận được gói cuktech/charger/status từ MQTT broker'
                : 'Chưa nối MQTT nên chưa đọc được sạc Cuktech'
          }
          action={
            <div className="flex items-center gap-2 flex-wrap justify-end">
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
                          ? 'TRẠNG THÁI CŨ'
                          : 'Chưa nối BLE'
                  }
                  color={cuktech.noiDuoc && !soCu ? SH.Green : matThat ? SH.Red : SH.Amber}
                />
              )}
              <FlatButton
                color={SH.Blue}
                disabled={dangNoiLaiCuktech || mqttStatus.state !== 'da_noi'}
                onClick={noiLaiCuktech}
                title="Ghép nối BLE lại với Cuktech (~40 giây)"
              >
                <RefreshCw size={14} className={dangNoiLaiCuktech ? 'animate-spin' : ''} />
                <span>{dangNoiLaiCuktech ? 'Đang nối lại (~40s)…' : 'Nối lại BLE'}</span>
              </FlatButton>
            </div>
          }
        />

        {!cuktech.coSoThat ? (
          <div className="flex-1 min-h-0 flex items-center justify-center">
            <EmptyState
              icon={<BatteryCharging size={28} />}
              title="Chưa có dữ liệu từ CUKTECH 10 Ultra"
              description="Ứng dụng không bịa số W/V/A. Đảm bảo script Bluetooth Cuktech đang publish lên topic cuktech/charger/status."
            />
          </div>
        ) : (
          <div className="flex-1 min-h-0 overflow-y-auto space-y-2.5 pr-0.5">
            {/* BangCanhBao Diagnostics */}
            {!cuktech.xacThuc && (
              <div className="flex items-start gap-2.5 rounded-xl border border-[#F59E0B]/45 bg-[#F59E0B]/10 px-3 py-2 text-xs text-[#FDE68A]">
                <AlertTriangle size={15} className="text-[#F59E0B] shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold">CHƯA XÁC THỰC:</span> Trạm thu BLE chưa hoàn tất khóa xác thực với củ sạc, một số cài đặt có thể chưa điều khiển được.
                </div>
              </div>
            )}
            {matThat && (
              <div className="flex items-start gap-2.5 rounded-xl border border-[#EF4444]/45 bg-[#EF4444]/10 px-3 py-2 text-xs text-[#FECACA]">
                <ShieldAlert size={15} className="text-[#EF4444] shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold">MẤT KẾT NỐI BLE:</span> Quá 60 giây chưa nhận được gói mới từ Cuktech. Bấm <b>Nối lại BLE</b> (quá trình bắt tay lại mất khoảng 40 giây).
                </div>
              </div>
            )}
            {mauThuan && !matThat && (
              <div className="flex items-start gap-2.5 rounded-xl border border-[#F59E0B]/45 bg-[#F59E0B]/10 px-3 py-2 text-xs text-[#FDE68A]">
                <Info size={15} className="text-[#F59E0B] shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold">TRẠNG THÁI CŨ:</span> Phiên BLE vừa báo ngắt tạm thời, đang hiển thị số đo cuối cùng trong lúc trạm thu tự nối lại (~40 giây).
                </div>
              </div>
            )}

            {/* DaiPhanBo Power Distribution Bar */}
            <div className="rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2.5 space-y-1.5">
              <div className="flex items-center justify-between gap-2 text-xs">
                <span className="font-semibold text-[#E6EDF7]">Phân bổ công suất tức thời</span>
                <span className="font-mono-num font-bold text-[#4F8BFF]">{tongW.toFixed(1)} W</span>
              </div>
              <div className="h-2.5 w-full rounded-full bg-[#1C2946] overflow-hidden flex">
                {tongW > 0 ? (
                  CONG_SAC_LIST.map((m) => {
                    const pct = tyLePhanBo[m.id] ?? 0;
                    if (pct <= 0) return null;
                    return (
                      <div
                        key={m.id}
                        className="h-full transition-all duration-300"
                        style={{
                          width: `${pct}%`,
                          backgroundColor: PORT_COLORS[m.id],
                        }}
                        title={`${m.nhan}: ${pct.toFixed(0)}%`}
                      />
                    );
                  })
                ) : (
                  <div className="h-full w-full bg-[#253459]/40" />
                )}
              </div>
              <div className="flex flex-wrap items-center gap-3 text-[11px] text-[#8A9BB8]">
                {tongW > 0 ? (
                  CONG_SAC_LIST.map((m) => {
                    const pct = tyLePhanBo[m.id] ?? 0;
                    if (pct <= 0) return null;
                    return (
                      <span key={m.id} className="inline-flex items-center gap-1.5 font-mono-num">
                        <span
                          className="h-2 w-2 rounded-full inline-block"
                          style={{ backgroundColor: PORT_COLORS[m.id] }}
                        />
                        <span className="text-[#E6EDF7] font-semibold">{m.nhan}</span>
                        <span>{Math.round(pct)}%</span>
                      </span>
                    );
                  })
                ) : (
                  <span>Chưa có cổng nào đang cấp tải (0.0 W)</span>
                )}
              </div>
            </div>

            {/* 4 Charging Ports */}
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
              {CONG_SAC_LIST.map((meta, idx) => {
                const c = danhSachCong[idx];
                const biTat = cuktechCongBiTat(cuktech, meta.id);
                const choMong = choCongSac[meta.id];
                const pending = choMong !== undefined;
                const roCongBat = biTat !== null || pending;
                const dangBat = pending ? choMong : biTat === false;
                const henPhut = cuktech.caiDat?.henGioPhut[meta.id];
                const color = c.dangChay ? SH.Green : biTat === true ? SH.Red : PORT_COLORS[meta.id];

                return (
                  <div
                    key={meta.id}
                    className="rounded-xl border p-3.5 flex flex-col justify-between gap-2.5"
                    style={{
                      backgroundColor: c.dangChay ? tint(SH.Green, 0.08) : SH.Surface,
                      borderColor: c.dangChay ? tint(SH.Green, 0.35) : SH.Border,
                    }}
                  >
                    <div className="space-y-2">
                      <div className="flex items-center justify-between gap-1.5">
                        <span className="text-xs font-bold text-[#E6EDF7]">{meta.nhan}</span>
                        <div className="flex items-center gap-1">
                          {henPhut !== undefined && henPhut > 0 && (
                            <button
                              type="button"
                              onClick={() => setHenGioPort(meta.id)}
                              className="inline-flex items-center gap-1 rounded-full border border-[#F59E0B]/40 bg-[#F59E0B]/15 px-2 py-0.5 text-[10px] font-semibold text-[#FCD34D] cursor-pointer"
                              title="Đổi hẹn tắt cổng này"
                            >
                              <Clock size={10} />
                              <span>{dinhDangHenGio(henPhut)}</span>
                            </button>
                          )}
                          <StatusPill label={nhanGiaoThucCong(c)} color={color} dot={c.dangChay} />
                        </div>
                      </div>
                      <div className="font-mono-num">
                        <div className="text-xl font-bold text-[#E6EDF7]">{c.oat.toFixed(1)} W</div>
                        <div className="text-[11px] text-[#8A9BB8]">
                          {c.volt.toFixed(2)}V · {c.ampe.toFixed(2)}A
                        </div>
                      </div>
                    </div>

                    {roCongBat ? (
                      <FlatButton
                        color={dangBat ? SH.Green : SH.TextSecondary}
                        disabled={pending || mqttStatus.state !== 'da_noi'}
                        className="w-full"
                        onClick={() => datCongSac(meta.id, !dangBat)}
                      >
                        <Power size={13} />
                        <span>
                          {choMong === true
                            ? 'Đang bật…'
                            : choMong === false
                              ? 'Đang tắt…'
                              : dangBat
                                ? 'Đang bật (Bấm tắt)'
                                : 'Đã tắt cổng (Bấm bật)'}
                        </span>
                      </FlatButton>
                    ) : (
                      <div className="rounded-xl border border-dashed border-[#253459] bg-[#0E1627] px-2.5 py-2 text-center text-[11px] text-[#8A9BB8]">
                        — Chưa rõ bật/tắt cổng
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Settings & BLE RSSI */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
              {/* All 6 Settings */}
              <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3.5 space-y-3">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-bold text-[#E6EDF7]">6 cài đặt phần cứng CUKTECH</span>
                  {!cuktech.caiDat && (
                    <span className="text-[11px] text-[#8A9BB8]">Chờ gói settings…</span>
                  )}
                </div>

                <div>
                  <div className="flex items-center justify-between text-[11px] text-[#8A9BB8] mb-1.5">
                    <span>1. Kịch bản phân bổ công suất</span>
                    <span className="font-semibold text-[#E6EDF7]">
                      {choCaiDatCuktech[CuktechPiid.KICH_BAN]
                        ? 'Đang lưu…'
                        : cuktech.caiDat?.kichBan?.nhan ?? '—'}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {KICH_BAN_LIST.map((kb) => (
                      <ChoiceChip
                        key={kb.ma}
                        label={kb.nhan}
                        selected={cuktech.caiDat?.kichBan?.ma === kb.ma}
                        disabled={Boolean(choCaiDatCuktech[CuktechPiid.KICH_BAN]) || mqttStatus.state !== 'da_noi'}
                        color={SH.Blue}
                        onClick={() => datCaiDatCuktech(CuktechPiid.KICH_BAN, kb.ma)}
                      />
                    ))}
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between text-[11px] text-[#8A9BB8] mb-1.5">
                    <span>2. Thời gian sáng màn hình</span>
                    <span className="font-semibold text-[#E6EDF7]">
                      {choCaiDatCuktech[CuktechPiid.SANG_MAN]
                        ? 'Đang lưu…'
                        : cuktech.caiDat?.thoiGianSangMan ?? '—'}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {CuktechPiid.SANG_MAN_CHON.map(([ma, nhan]) => (
                      <ChoiceChip
                        key={ma}
                        label={nhan}
                        selected={cuktech.caiDat?.thoiGianSangMan === nhan}
                        disabled={Boolean(choCaiDatCuktech[CuktechPiid.SANG_MAN]) || mqttStatus.state !== 'da_noi'}
                        color={SH.Purple}
                        onClick={() => datCaiDatCuktech(CuktechPiid.SANG_MAN, ma)}
                      />
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                  <FlatButton
                    color={sacNhoGiotVal === true ? SH.Green : SH.TextSecondary}
                    disabled={
                      sacNhoGiotVal === null ||
                      Boolean(choCaiDatCuktech[CuktechPiid.SAC_NHO_GIOT_A]) ||
                      mqttStatus.state !== 'da_noi'
                    }
                    onClick={() => {
                      if (sacNhoGiotVal !== null) {
                        datCaiDatCuktech(CuktechPiid.SAC_NHO_GIOT_A, sacNhoGiotVal ? 0 : 1);
                      }
                    }}
                  >
                    <span>
                      Nhỏ giọt USB-A:{' '}
                      {choCaiDatCuktech[CuktechPiid.SAC_NHO_GIOT_A]
                        ? 'Đang gửi…'
                        : sacNhoGiotVal === null
                          ? '—'
                          : sacNhoGiotVal
                            ? 'Bật'
                            : 'Tắt'}
                    </span>
                  </FlatButton>

                  <FlatButton
                    color={tuTatManVal === true ? SH.Green : SH.TextSecondary}
                    disabled={
                      tuTatManVal === null ||
                      Boolean(choCaiDatCuktech[CuktechPiid.TU_TAT_MAN]) ||
                      mqttStatus.state !== 'da_noi'
                    }
                    onClick={() => {
                      if (tuTatManVal !== null) {
                        datCaiDatCuktech(CuktechPiid.TU_TAT_MAN, tuTatManVal ? 0 : 1);
                      }
                    }}
                  >
                    <span>
                      Tắt màn rỗi:{' '}
                      {choCaiDatCuktech[CuktechPiid.TU_TAT_MAN]
                        ? 'Đang gửi…'
                        : tuTatManVal === null
                          ? '—'
                          : tuTatManVal
                            ? 'Bật'
                            : 'Tắt'}
                    </span>
                  </FlatButton>

                  <FlatButton
                    color={khoaXoayVal === true ? SH.Green : SH.TextSecondary}
                    disabled={
                      khoaXoayVal === null ||
                      Boolean(choCaiDatCuktech[CuktechPiid.KHOA_XOAY]) ||
                      mqttStatus.state !== 'da_noi'
                    }
                    onClick={() => {
                      if (khoaXoayVal !== null) {
                        datCaiDatCuktech(CuktechPiid.KHOA_XOAY, khoaXoayVal ? 0 : 1);
                      }
                    }}
                  >
                    <span>
                      Khóa xoay:{' '}
                      {choCaiDatCuktech[CuktechPiid.KHOA_XOAY]
                        ? 'Đang gửi…'
                        : khoaXoayVal === null
                          ? '—'
                          : khoaXoayVal
                            ? 'Bật'
                            : 'Tắt'}
                    </span>
                  </FlatButton>

                  <FlatButton
                    color={activeTimers.length > 0 ? SH.Amber : SH.Cyan}
                    disabled={mqttStatus.state !== 'da_noi'}
                    onClick={() => setHenGioPort('C1')}
                  >
                    <Clock size={13} />
                    <span>
                      Hẹn tắt:{' '}
                      {anyTimerPending
                        ? 'Đang gửi…'
                        : activeTimers.length > 0
                          ? `${activeTimers.length} cổng đang hẹn`
                          : cuktech.caiDat
                            ? 'Đang tắt'
                            : '—'}
                    </span>
                  </FlatButton>
                </div>
              </div>

              {/* BLE RSSI Receivers */}
              <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3.5 space-y-2.5">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-bold text-[#E6EDF7]">Độ mạnh sóng BLE từ các trạm thu</span>
                  {songBest && (
                    <StatusPill
                      label={`Tốt nhất: ${nhanDbmSong(songBest)}`}
                      color={mauMucSong(mucSongCua(songBest))}
                    />
                  )}
                </div>
                {songList.length === 0 ? (
                  <div className="text-xs text-[#8A9BB8] py-4 text-center">
                    Chưa có trạm thu BLE báo RSSI về topic cuktech/rssi/#.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {songList.map((s) => {
                      const muc = mucSongCua(s);
                      const stale = songLaSoCu(s);
                      const col = mauMucSong(muc);
                      return (
                        <div
                          key={s.nguonId}
                          className="flex items-center justify-between rounded-xl border border-[#253459] bg-[#16213A] px-3 py-2 text-xs"
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <Wifi size={14} className="shrink-0" style={{ color: col }} />
                            <div className="min-w-0">
                              <div className="font-semibold text-[#E6EDF7] truncate">
                                {s.nguonTen || s.nguonId}
                                {s.dangNoi && (
                                  <span className="ml-1.5 text-[10px] font-bold text-[#22C55E]">· Đang giữ BLE</span>
                                )}
                              </div>
                              <div className="text-[11px] text-[#8A9BB8]">
                                {MUC_SONG_NHAN[muc]}
                                {s.tuoiGiay !== null ? ` · ${s.tuoiGiay}s trước` : ''}
                                {stale ? ' (Số cũ >5p)' : ''}
                              </div>
                            </div>
                          </div>
                          <span className="font-mono-num font-bold shrink-0 ml-2" style={{ color: col }}>
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

      {/* Electricity & Switch Sensors + Safety */}
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
            <div className="space-y-2 pt-1" onClick={(e) => e.stopPropagation()}>
              <div className="text-xs font-semibold text-[#8A9BB8]">
                Công tắc thật ({isBridgeSong(congtac) ? 'Bridge đang sống' : 'Bridge ngoại tuyến'})
              </div>
              {congtac.danhSach.map((tb) => {
                const o = oThietBi(tb.phong, tb.thietBi, false);
                const roRang = oThietBiRoRang(o);
                const bat = oThietBiBatHienThi(o, false);
                const pending = o.kind === 'that' ? o.dangCho : false;
                const tt = congtac.trangThai[tb.ma];
                return (
                  <DeviceSwitchRow
                    key={tb.ma}
                    icon={<Bolt size={15} />}
                    label={tb.ten}
                    subtitle={
                      o.kind === 'that' && o.ghiChu
                        ? o.ghiChu
                        : tt?.luc
                          ? `Đọc lúc ${tt.luc}`
                          : 'Công tắc thật · MQTT'
                    }
                    subtitleWarning={!roRang}
                    unknownState={!roRang}
                    color={SH.Green}
                    checked={bat}
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
            subtitle={gasIsReal ? 'Nhận trực tiếp từ cảm biến MQTT' : 'Mô phỏng khi chưa có gói MQTT cảm biến'}
            badge={gasIsReal ? <StatusPill label="MQTT Thật" color={SH.Green} /> : <DemoBadge />}
          />
          <div className="space-y-2.5">
            <div className="flex items-center justify-between rounded-xl border border-[#253459] bg-[#0E1627] p-3">
              <div className="flex items-center gap-2.5">
                <IconBadge color={gasDanger ? SH.Red : SH.Green} size={34}>
                  {gasDanger ? <ShieldAlert size={17} /> : <ShieldCheck size={17} />}
                </IconBadge>
                <div>
                  <div className="text-xs font-bold text-[#E6EDF7]">Cảm biến Gas Nhà bếp</div>
                  <div className="text-[11px] text-[#8A9BB8]">Nồng độ {gasPpmText}</div>
                </div>
              </div>
              <StatusPill label={gasDanger ? 'Rò rỉ Gas!' : 'Bình thường'} color={gasDanger ? SH.Red : SH.Green} />
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

      {/* HopThoaiHenGio Modal */}
      {henGioPort !== null && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm"
          onClick={() => setHenGioPort(null)}
        >
          <div
            className="w-full max-w-md rounded-2xl border border-[#253459] bg-[#16213A] p-4 shadow-2xl space-y-3.5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Clock size={17} className="text-[#F59E0B]" />
                <h3 className="text-sm font-bold text-[#E6EDF7]">Hẹn giờ tự tắt cổng sạc CUKTECH</h3>
              </div>
              <button
                type="button"
                onClick={() => setHenGioPort(null)}
                className="rounded-lg p-1 text-[#8A9BB8] hover:bg-[#253459] hover:text-white cursor-pointer"
              >
                <X size={16} />
              </button>
            </div>

            <div>
              <div className="text-[11px] text-[#8A9BB8] mb-1.5">Chọn cổng sạc</div>
              <div className="grid grid-cols-4 gap-2">
                {CONG_SAC_LIST.map((m) => {
                  const curMin = cuktech.caiDat?.henGioPhut[m.id];
                  const selected = henGioPort === m.id;
                  return (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => setHenGioPort(m.id)}
                      className="rounded-xl border px-2 py-2 text-center transition-colors cursor-pointer"
                      style={{
                        backgroundColor: selected ? tint(PORT_COLORS[m.id], 0.2) : '#0E1627',
                        borderColor: selected ? PORT_COLORS[m.id] : '#253459',
                      }}
                    >
                      <div className="text-xs font-bold text-[#E6EDF7]">{m.nhan}</div>
                      <div className="text-[10px] text-[#8A9BB8] mt-0.5">{dinhDangHenGio(curMin)}</div>
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <div className="text-[11px] text-[#8A9BB8] mb-1.5">
                Thời gian tự tắt cho cổng <b className="text-[#E6EDF7]">{CONG_SAC_LIST.find((x) => x.id === henGioPort)?.nhan}</b>
              </div>
              <div className="grid grid-cols-3 sm:grid-cols-5 gap-2">
                {CuktechPiid.HEN_GIO_CHON.map(([phut, nhan]) => {
                  const piid = CuktechPiid.henGio(henGioPort);
                  const curVal = cuktech.caiDat?.henGioPhut[henGioPort];
                  const isSel = curVal === phut;
                  const busy = Boolean(choCaiDatCuktech[piid]);
                  return (
                    <ChoiceChip
                      key={phut}
                      label={nhan}
                      selected={isSel}
                      disabled={busy || mqttStatus.state !== 'da_noi'}
                      color={phut === 0 ? SH.TextSecondary : SH.Amber}
                      onClick={() => datCaiDatCuktech(piid, phut)}
                    />
                  );
                })}
              </div>
            </div>

            <div className="flex justify-end pt-1">
              <FlatButton color={SH.Blue} onClick={() => setHenGioPort(null)}>
                Đóng
              </FlatButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
