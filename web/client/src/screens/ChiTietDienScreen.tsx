import React, { useState } from 'react';
import { Bolt } from 'lucide-react';
import { coSoLieuDien, doTuoiDien } from '../data/dien.js';
import { BarChart } from '../components/Charts.js';
import { EmptyState, SectionHeader, SmartCard, StatusPill, TrangThaiKetNoi } from '../components/Common.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

export function ChiTietDienScreen() {
  const { dien, mqttStatus, nowMs } = useSmartHome();
  const [selectedIdx, setSelectedIdx] = useState<number | null>(null);

  const hasData = coSoLieuDien(dien);
  const tuoi = doTuoiDien(dien, new Date(nowMs));
  const labels = dien.lichSu.map((d) => {
    const m = /^\d{4}-(\d{2})-(\d{2})$/.exec(d.ngay);
    return m ? `${m[2]}/${m[1]}` : d.ngay;
  });
  const values = dien.lichSu.map((d) => d.kwh);

  const activeItem =
    selectedIdx !== null && dien.lichSu[selectedIdx]
      ? dien.lichSu[selectedIdx]
      : dien.lichSu[dien.lichSu.length - 1] ?? null;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      <SmartCard className="shrink-0 flex flex-wrap items-center justify-between gap-2 p-2.5">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h2 className="text-sm font-bold text-[#E6EDF7]">Chi tiết sản lượng & hóa đơn EVN</h2>
            <TrangThaiKetNoi ketNoi={mqttStatus} dien={dien} nowMs={nowMs} />
          </div>
          <p className="text-[11px] text-[#8A9BB8] mt-0.5">
            {dien.capNhat
              ? `Đồng bộ lúc ${dien.capNhat} (${tuoi === 'MOI' ? 'Mới' : tuoi === 'HOI_CU' ? '8-24h' : 'Cũ >24h'})`
              : 'Chưa có mốc đồng bộ từ máy chủ EVN'}
          </p>
        </div>
        {dien.loi && <StatusPill label={`Lỗi EVN: ${dien.loi}`} color={SH.Red} />}
      </SmartCard>

      {!hasData ? (
        <div className="flex-1 min-h-0 flex items-center justify-center">
          <EmptyState
            icon={<Bolt size={28} />}
            title="Chưa nhận được số liệu điện"
            description="Kiểm tra kết nối MQTT và container dien-evn trên máy chủ gia đình."
          />
        </div>
      ) : (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5">
          <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Hôm nay / Gần nhất</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">
                {dien.ngaySanLuong !== null ? `${dien.ngaySanLuong.toFixed(2)} kWh` : '—'}
              </div>
              <div className="text-[11px] text-[#5F6F8C]">{dien.ngay || '—'}</div>
            </SmartCard>

            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Sản lượng tháng</div>
              <div className="font-mono-num text-lg font-bold text-[#4F8BFF] mt-0.5">
                {dien.thangSanLuong !== null ? `${dien.thangSanLuong.toFixed(1)} kWh` : '—'}
              </div>
              <div className="text-[11px] text-[#22C55E]">
                {dien.thangTien !== null ? `${Math.round(dien.thangTien).toLocaleString('vi-VN')} đ` : 'Chưa tính tiền'}
              </div>
            </SmartCard>

            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Chỉ số công tơ</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">
                {dien.congToChiSo !== null ? `${dien.congToChiSo.toFixed(1)}` : '—'}
              </div>
              <div className="text-[11px] text-[#5F6F8C]">kWh tích lũy</div>
            </SmartCard>

            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Công nợ EVN</div>
              <div className="font-mono-num text-lg font-bold text-[#F59E0B] mt-0.5">
                {dien.congNoSoTien !== null
                  ? `${Math.round(dien.congNoSoTien).toLocaleString('vi-VN')} đ`
                  : '—'}
              </div>
              <div className="text-[11px] text-[#5F6F8C]">Số dư phải thanh toán</div>
            </SmartCard>
          </div>

          {dien.lichSu.length > 0 && (
            <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
              <SmartCard className="lg:col-span-8 flex flex-col justify-between gap-2 p-3 min-h-0">
                <SectionHeader
                  title={`Biểu đồ từng ngày (${dien.lichSu.length} ngày)`}
                  subtitle={
                    activeItem
                      ? `Đang chọn ${activeItem.ngay}: ${activeItem.kwh.toFixed(2)} kWh`
                      : 'Bấm vào cột hoặc dòng để xem'
                  }
                />
                <div className="flex-1 min-h-0 flex items-center">
                  <BarChart
                    values={values}
                    labels={labels}
                    color={SH.Blue}
                    unit="kWh"
                    highlightIndex={selectedIdx}
                    onSelectIndex={(idx) => setSelectedIdx(idx)}
                  />
                </div>
              </SmartCard>

              <SmartCard className="lg:col-span-4 flex flex-col min-h-0 p-3">
                <div className="text-xs font-bold text-[#E6EDF7] pb-2 border-b border-[#253459] shrink-0">
                  Bảng sản lượng từng ngày
                </div>
                <div className="flex-1 min-h-0 overflow-y-auto pt-1 divide-y divide-[#253459]/70">
                  {[...dien.lichSu].reverse().map((row, revIdx) => {
                    const origIdx = dien.lichSu.length - 1 - revIdx;
                    const active = selectedIdx === origIdx;
                    return (
                      <button
                        key={row.ngay}
                        type="button"
                        onClick={() => setSelectedIdx(origIdx)}
                        className={`w-full flex items-center justify-between px-2.5 py-1.5 text-xs rounded-lg transition-colors cursor-pointer ${
                          active ? 'bg-[#4F8BFF]/15 text-white' : 'hover:bg-[#0E1627] text-[#E6EDF7]'
                        }`}
                      >
                        <span className="font-mono-num">{row.ngay}</span>
                        <span className="font-mono-num font-bold text-[#4F8BFF]">{row.kwh.toFixed(2)} kWh</span>
                      </button>
                    );
                  })}
                </div>
              </SmartCard>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

