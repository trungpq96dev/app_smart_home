import React, { useEffect, useRef, useState } from 'react';
import { RefreshCw, VideoOff } from 'lucide-react';
import { SH } from '../theme/colors.js';
import { FlatButton } from './Common.js';

type TrangThaiLuong =
  | { state: 'cho' }
  | { state: 'chay' }
  | { state: 'loi'; moTa: string };

const CHO_THU_LAI = [4000, 15000, 40000];
const POLL_MS = 1500;
const INITIAL_TIMEOUT_MS = 25000;

export function LuongCamera({
  tenLuong,
  kichHoat,
  aspect = '16 / 9',
  fill = false,
  className = '',
}: {
  tenLuong: string;
  kichHoat: boolean;
  aspect?: string;
  fill?: boolean;
  className?: string;
}) {
  const [lanThu, setLanThu] = useState(0);
  const [trangThai, setTrangThai] = useState<TrangThaiLuong>({ state: 'cho' });
  const iframeRef = useRef<HTMLIFrameElement | null>(null);
  const retryTimerRef = useRef<number | null>(null);

  const srcUrl =
    kichHoat && tenLuong
      ? `/go2rtc/stream.html?src=${encodeURIComponent(tenLuong)}&width=100%25&t=${lanThu}`
      : 'about:blank';

  useEffect(() => {
    setTrangThai({ state: 'cho' });
    setLanThu(0);
  }, [tenLuong, kichHoat]);

  useEffect(() => {
    if (!kichHoat || !tenLuong) return;
    let disposed = false;
    const startMs = Date.now();
    let seenChay = false;

    const scheduleRetry = (moTa: string) => {
      if (disposed) return;
      setTrangThai({ state: 'loi', moTa });
      if (retryTimerRef.current === null) {
        const delay = CHO_THU_LAI[Math.min(lanThu, CHO_THU_LAI.length - 1)];
        retryTimerRef.current = window.setTimeout(() => {
          retryTimerRef.current = null;
          if (!disposed) {
            setTrangThai({ state: 'cho' });
            setLanThu((n) => n + 1);
          }
        }, delay);
      }
    };

    const pollId = window.setInterval(() => {
      if (disposed) return;
      const frame = iframeRef.current;
      try {
        const doc = frame?.contentDocument;
        if (doc) {
          const videoStream = doc.querySelector('video-stream');
          const video =
            videoStream?.shadowRoot?.querySelector('video') ||
            doc.querySelector('video');
          if (video && video.videoWidth > 0 && video.currentTime > 0 && !video.paused) {
            seenChay = true;
            setTrangThai({ state: 'chay' });
            return;
          }
          const bodyText = (doc.body?.innerText || '').toLowerCase();
          if (
            bodyText.includes('disconnect') ||
            bodyText.includes('error') ||
            bodyText.includes('streams:') ||
            bodyText.includes('404')
          ) {
            scheduleRetry('go2rtc báo lỗi hoặc chưa có luồng');
            return;
          }
        }
      } catch {
        // Ignore cross-frame inspection glitch
      }

      if (!seenChay && Date.now() - startMs > INITIAL_TIMEOUT_MS) {
        scheduleRetry('Quá 25s chưa thấy hình từ go2rtc');
      }
    }, POLL_MS);

    return () => {
      disposed = true;
      window.clearInterval(pollId);
      if (retryTimerRef.current !== null) {
        window.clearTimeout(retryTimerRef.current);
        retryTimerRef.current = null;
      }
      const frame = iframeRef.current;
      try {
        const doc = frame?.contentDocument;
        doc?.querySelectorAll('video, audio').forEach((m) => {
          const media = m as HTMLMediaElement;
          media.pause();
          media.removeAttribute('src');
          media.load();
        });
        doc?.querySelector('video-stream')?.remove();
      } catch {
        // ignore
      }
    };
  }, [kichHoat, tenLuong, lanThu]);

  return (
    <div
      className={`relative w-full overflow-hidden rounded-xl border border-[#253459] bg-[#0B1220] ${
        fill ? 'flex-1 min-h-0 h-full' : ''
      } ${className}`}
      style={fill ? undefined : { aspectRatio: aspect }}
    >
      {kichHoat && (
        <iframe
          ref={iframeRef}
          src={srcUrl}
          title={`Camera ${tenLuong}`}
          className="h-full w-full border-0"
          allow="autoplay; fullscreen"
        />
      )}

      {trangThai.state === 'cho' && (
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center bg-[#0B1220]/75 text-center p-4">
          <RefreshCw size={22} className="animate-spin text-[#4F8BFF] mb-2" />
          <div className="text-xs font-semibold text-[#E6EDF7]">Đang kết nối luồng {tenLuong}…</div>
        </div>
      )}

      {trangThai.state === 'loi' && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-[#0B1220]/90 text-center p-4">
          <VideoOff size={24} className="text-[#EF4444] mb-2" />
          <div className="text-xs font-semibold text-[#E6EDF7]">{trangThai.moTa}</div>
          <div className="mt-3">
            <FlatButton
              color={SH.Blue}
              onClick={() => {
                setTrangThai({ state: 'cho' });
                setLanThu((n) => n + 1);
              }}
            >
              <RefreshCw size={13} />
              <span>Thử lại ngay</span>
            </FlatButton>
          </div>
        </div>
      )}
    </div>
  );
}

