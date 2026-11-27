import React from 'react';
import { AlertCircle, Check, Mic, MicOff, Settings, Sparkles, Volume2, X } from 'lucide-react';
import { coKhoaGiongNoi, dangChayTrangThai } from '../data/giongnoi/types.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';
import { FlatButton, StatusPill } from './Common.js';

export function BangGiongNoi() {
  const {
    phienGiongNoi,
    voiceConfig,
    voiceSheetOpen,
    setVoiceSheetOpen,
    batTatGiongNoi,
    tatGiongNoi,
    ngatLoiGiongNoi,
    xoaLoiGiongNoi,
    navigateDest,
  } = useSmartHome();

  if (!voiceSheetOpen) return null;

  const { trangThai, thoai, daLam, giayDaChay, banSongCong } = phienGiongNoi;
  const dangChay = dangChayTrangThai(trangThai);
  const dangTraLoi = trangThai.state === 'dang_tra_loi';
  const coKhoa = coKhoaGiongNoi(voiceConfig);

  const phut = String(Math.floor(giayDaChay / 60)).padStart(2, '0');
  const giay = String(giayDaChay % 60).padStart(2, '0');

  const statusMeta = (() => {
    switch (trangThai.state) {
      case 'dang_noi': return { label: 'Đang kết nối Gemini Live…', color: SH.Amber };
      case 'dang_nghe': return { label: 'Đang lắng nghe…', color: SH.Green };
      case 'dang_tra_loi': return { label: 'Trợ lý đang trả lời…', color: SH.Blue };
      case 'loi': return { label: 'Lỗi kết nối', color: SH.Red };
      default: return { label: 'Sẵn sàng', color: SH.TextSecondary };
    }
  })();

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/65 p-0 sm:p-4 backdrop-blur-xs">
      <div className="w-full sm:max-w-lg rounded-t-3xl sm:rounded-2xl border border-[#253459] bg-[#0E1627] shadow-2xl overflow-hidden flex flex-col max-h-[85dvh]">
        {/* Header */}
        <div className="flex items-center justify-between gap-3 px-4 py-3.5 border-b border-[#253459] bg-[#16213A]">
          <div className="flex items-center gap-2.5">
            <div
              className="flex h-9 w-9 items-center justify-center rounded-xl"
              style={{
                backgroundColor: tint(statusMeta.color, 0.16),
                border: `1px solid ${tint(statusMeta.color, 0.35)}`,
                color: statusMeta.color,
              }}
            >
              <Sparkles size={18} />
            </div>
            <div>
              <div className="text-sm font-bold text-[#E6EDF7]">Trợ lý giọng nói Gemini</div>
              <div className="flex items-center gap-2 mt-0.5">
                <StatusPill label={statusMeta.label} color={statusMeta.color} pulse={dangChay} />
                {dangChay && (
                  <span className="font-mono-num text-[11px] text-[#8A9BB8]">
                    {phut}:{giay}
                  </span>
                )}
                {banSongCong && dangChay && (
                  <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-[#253459] text-[#8A9BB8]">
                    Bán song công
                  </span>
                )}
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setVoiceSheetOpen(false)}
            className="text-[#8A9BB8] hover:text-white p-1.5 rounded-lg hover:bg-[#253459] cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {!coKhoa && (
            <div className="rounded-xl border border-[#F59E0B]/40 bg-[#F59E0B]/10 p-3 text-xs text-[#FCD34D]">
              Chưa nhập Gemini API Key. API Key chỉ lưu cục bộ trong trình duyệt của bạn.
              <div className="mt-2">
                <FlatButton
                  color={SH.Amber}
                  onClick={() => {
                    setVoiceSheetOpen(false);
                    navigateDest('settings');
                  }}
                >
                  <Settings size={14} />
                  <span>Mở Cài đặt → Trợ lý giọng nói</span>
                </FlatButton>
              </div>
            </div>
          )}

          {trangThai.state === 'loi' && (
            <div className="rounded-xl border border-[#EF4444]/40 bg-[#EF4444]/10 p-3 text-xs text-[#FCA5A5] flex items-start justify-between gap-2">
              <span>{trangThai.cau}</span>
              <button type="button" onClick={xoaLoiGiongNoi} className="underline shrink-0 cursor-pointer">
                Đóng lỗi
              </button>
            </div>
          )}

          {/* Action log */}
          {daLam.length > 0 && (
            <div className="space-y-1.5">
              <div className="text-[11px] font-semibold uppercase tracking-wider text-[#5F6F8C]">
                Lệnh đã thực hiện
              </div>
              {daLam.map((item, idx) => (
                <div
                  key={idx}
                  className="flex items-start gap-2 rounded-xl border border-[#253459] bg-[#16213A] px-3 py-2 text-xs"
                >
                  <span
                    className="mt-0.5 shrink-0"
                    style={{ color: item.thanhCong ? SH.Green : SH.Red }}
                  >
                    {item.thanhCong ? <Check size={14} /> : <AlertCircle size={14} />}
                  </span>
                  <div className="flex-1">
                    <div className="text-[#E6EDF7]">{item.cau}</div>
                    {item.chuaCoThietBiThat && (
                      <div className="text-[10px] text-[#F59E0B] mt-0.5">
                        Thiết bị mô phỏng trên giao diện (chưa nối công tắc vật lý)
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Conversation transcript */}
          <div className="space-y-2">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-[#5F6F8C]">
              Hội thoại
            </div>
            {thoai.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#253459] p-4 text-center text-xs text-[#8A9BB8]">
                Bấm nút Mic bên dưới và nói bằng tiếng Việt tự nhiên:
                <div className="mt-2 space-y-1 text-[#E6EDF7]/80">
                  <div>“Bật đèn phòng khách”</div>
                  <div>“Giảm điều hòa phòng ngủ 1 xuống 25 độ”</div>
                  <div>“Tháng này hết bao nhiêu tiền điện?”</div>
                  <div>“Mở màn hình Camera”</div>
                </div>
              </div>
            ) : (
              thoai.map((d, i) => (
                <div key={i} className={`flex ${d.cuaMinh ? 'justify-end' : 'justify-start'}`}>
                  <div
                    className="max-w-[85%] rounded-2xl px-3.5 py-2 text-xs leading-relaxed"
                    style={{
                      backgroundColor: d.cuaMinh ? tint(SH.Blue, 0.22) : '#16213A',
                      border: `1px solid ${d.cuaMinh ? tint(SH.Blue, 0.4) : SH.Border}`,
                      color: SH.TextPrimary,
                    }}
                  >
                    <div className="text-[10px] font-semibold text-[#8A9BB8] mb-0.5">
                      {d.cuaMinh ? 'Bạn' : 'Gemini'}
                    </div>
                    {d.cau}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Footer controls */}
        <div className="flex items-center justify-between gap-3 px-4 pt-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] border-t border-[#253459] bg-[#16213A]">
          <div className="text-[11px] text-[#8A9BB8] flex items-center gap-1.5 truncate">
            <Volume2 size={14} className="shrink-0 text-[#4F8BFF]" />
            <span>Giọng {voiceConfig.giongDoc} · Trần {voiceConfig.phutMoiNgay} phút/ngày</span>
          </div>

          <div className="flex items-center gap-2">
            {dangTraLoi && (
              <FlatButton onClick={ngatLoiGiongNoi} color={SH.Purple} variant="soft">
                <Mic size={15} />
                <span>Ngắt lời</span>
              </FlatButton>
            )}
            {dangChay ? (
              <FlatButton onClick={tatGiongNoi} color={SH.Red} variant="solid">
                <MicOff size={15} />
                <span>Dừng nói</span>
              </FlatButton>
            ) : (
              <FlatButton onClick={batTatGiongNoi} color={SH.Green} variant="solid">
                <Mic size={15} />
                <span>Bắt đầu nói</span>
              </FlatButton>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

