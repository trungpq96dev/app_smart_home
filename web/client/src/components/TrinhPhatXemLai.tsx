import React, { useEffect, useRef, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { SH } from '../theme/colors.js';

/**
 * Khung phát video MP4 xem lại 24/24 (port 1:1 từ `TrinhPhatXemLai.kt`):
 *  - Dùng HTML5 `<video playsInline>` qua proxy HTTP Range 206 của Web Gateway -> tua giữa đoạn 10 phút mượt ngay,
 *    chạy tốt cả trên Safari iPhone lẫn máy tính bảng/PC.
 *  - Hỗ trợ đổi tốc độ phát `1x / 2x / 4x`.
 *  - Hiện rõ lớp thông báo khi đang kéo đoạn video cũ từ Google Drive 5TB về máy chủ.
 */
export function TrinhPhatXemLai({
  url,
  seekOffsetMs,
  seekVersion,
  dangPhat,
  tocDo,
  laTuDrive,
  onTienTrinh,
  onHetDoan,
  onLoi,
  className = '',
}: {
  url: string;
  seekOffsetMs: number;
  seekVersion: number;
  dangPhat: boolean;
  tocDo: number;
  laTuDrive: boolean;
  onTienTrinh: (posMs: number) => void;
  onHetDoan: () => void;
  onLoi: (msg: string) => void;
  className?: string;
}) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [daSanSang, setDaSanSang] = useState(false);
  const [dangNap, setDangNap] = useState(true);
  const pendingSeekSecRef = useRef(Math.max(0, seekOffsetMs / 1000));

  const onTienTrinhRef = useRef(onTienTrinh);
  onTienTrinhRef.current = onTienTrinh;
  const onHetDoanRef = useRef(onHetDoan);
  onHetDoanRef.current = onHetDoan;
  const onLoiRef = useRef(onLoi);
  onLoiRef.current = onLoi;

  useEffect(() => {
    setDaSanSang(false);
    setDangNap(true);
    pendingSeekSecRef.current = Math.max(0, seekOffsetMs / 1000);
  }, [url]);

  // Tua khi người dùng thả tay trên thanh Timeline hoặc bấm sự kiện người (kể cả cùng segment)
  useEffect(() => {
    const targetSec = Math.max(0, seekOffsetMs / 1000);
    pendingSeekSecRef.current = targetSec;
    const v = videoRef.current;
    if (v && daSanSang) {
      try {
        if (Math.abs(v.currentTime - targetSec) > 0.35) {
          v.currentTime = targetSec;
        }
      } catch {
        // ignore seek before ready
      }
    }
  }, [seekVersion, seekOffsetMs, daSanSang]);

  // Đổi trạng thái Play/Pause hoặc tốc độ 1x/2x/4x
  useEffect(() => {
    const v = videoRef.current;
    if (!v || !daSanSang) return;
    try {
      v.playbackRate = Math.min(4, Math.max(0.5, tocDo));
      if (dangPhat) {
        if (v.paused) {
          void v.play().catch(() => {});
        }
      } else if (!v.paused) {
        v.pause();
      }
    } catch {
      // ignore playbackRate glitch
    }
  }, [dangPhat, tocDo, daSanSang]);

  // Cập nhật kim đồng hồ trên thanh Timeline mỗi 300ms khi đang phát
  useEffect(() => {
    if (!daSanSang || !dangPhat) return;
    const timer = window.setInterval(() => {
      const v = videoRef.current;
      if (v && !v.paused && !v.seeking && Number.isFinite(v.currentTime)) {
        onTienTrinhRef.current(Math.round(v.currentTime * 1000));
      }
    }, 300);
    return () => window.clearInterval(timer);
  }, [daSanSang, dangPhat]);

  // Tạm dừng khi ẩn tab / tắt màn hình điện thoại
  useEffect(() => {
    const onVis = () => {
      if (document.hidden) {
        videoRef.current?.pause();
      }
    };
    document.addEventListener('visibilitychange', onVis);
    return () => document.removeEventListener('visibilitychange', onVis);
  }, []);

  return (
    <div className={`relative h-full w-full bg-black overflow-hidden ${className}`}>
      <video
        ref={videoRef}
        src={url}
        playsInline
        preload="auto"
        className="h-full w-full object-contain bg-black"
        onLoadedMetadata={(e) => {
          const v = e.currentTarget;
          setDaSanSang(true);
          const targetSec = pendingSeekSecRef.current;
          if (targetSec > 0.5) {
            try {
              v.currentTime = targetSec;
            } catch {
              // ignore
            }
          }
          v.playbackRate = Math.min(4, Math.max(0.5, tocDo));
          if (dangPhat) {
            void v.play().catch(() => {});
          }
        }}
        onCanPlay={() => setDangNap(false)}
        onPlaying={() => setDangNap(false)}
        onWaiting={() => setDangNap(true)}
        onEnded={() => onHetDoanRef.current()}
        onError={() => {
          setDangNap(false);
          onLoiRef.current('Không phát được đoạn video này (kiểm tra kết nối đầu ghi hoặc Google Drive).');
        }}
      />

      {dangNap && (
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center bg-[#0B1220]/75 px-6 text-center">
          <RefreshCw
            size={26}
            className="animate-spin mb-2"
            style={{ color: laTuDrive ? SH.Blue : SH.Green }}
          />
          <div className="text-xs font-semibold text-[#E6EDF7]">
            {laTuDrive
              ? 'Đang kéo đoạn video từ Google Drive 5TB…'
              : 'Đang nạp đoạn ghi hình…'}
          </div>
        </div>
      )}
    </div>
  );
}
