import React, { useEffect, useRef, useState } from 'react';
import { RefreshCw, VideoOff } from 'lucide-react';
import { SH } from '../theme/colors.js';
import { FlatButton } from './Common.js';

type TrangThaiLuong =
  | { state: 'cho' }
  | { state: 'chay' }
  | { state: 'loi'; moTa: string };

// Thu lai thua: 4s -> 15s -> 40s, toi da 3 lan de tranh Tuya chan tam 4 phut (khop LuongCamera.kt)
const CHO_THU_LAI = [4000, 15000, 40000];
const MAX_THU_LAI = 3;
const POLL_MS = 1500;
const INITIAL_TIMEOUT_MS = 25000;

export function LuongCamera({
  tenLuong,
  kichHoat,
  aspect = '16 / 9',
  fill = false,
  className = '',
  onLoi,
}: {
  tenLuong: string;
  kichHoat: boolean;
  aspect?: string;
  fill?: boolean;
  className?: string;
  onLoi?: (msg: string) => void;
}) {
  const [lanThu, setLanThu] = useState(0);
  const [pageVisible, setPageVisible] = useState(() =>
    typeof document === 'undefined' ? true : !document.hidden,
  );
  const [trangThai, setTrangThai] = useState<TrangThaiLuong>({ state: 'cho' });
  const iframeRef = useRef<HTMLIFrameElement | null>(null);
  const retryTimerRef = useRef<number | null>(null);
  const onLoiRef = useRef(onLoi);
  onLoiRef.current = onLoi;

  // Cat han luong WebRTC khi tat man hinh / an tab de khong ton pin va khong vong tieng camera
  useEffect(() => {
    const onVisChange = () => {
      setPageVisible(!document.hidden);
    };
    document.addEventListener('visibilitychange', onVisChange);
    return () => document.removeEventListener('visibilitychange', onVisChange);
  }, []);

  const dangMo = kichHoat && Boolean(tenLuong) && pageVisible;
  const srcUrl = dangMo
    ? `/go2rtc/stream.html?src=${encodeURIComponent(tenLuong)}&width=100%25&t=${lanThu}`
    : 'about:blank';

  useEffect(() => {
    setTrangThai({ state: 'cho' });
    setLanThu(0);
  }, [tenLuong, kichHoat]);

  useEffect(() => {
    if (!dangMo) return;
    let disposed = false;
    const startMs = Date.now();
    let seenChay = false;
    let lastVideoTime = -1;
    let stuckTicks = 0;

    const scheduleRetry = (moTa: string) => {
      if (disposed) return;
      if (lanThu >= MAX_THU_LAI) {
        const msg = `${moTa} — đã thử ${MAX_THU_LAI} lần. Máy chủ Tuya có thể đang chặn tạm, chờ ít phút rồi bấm Thử lại.`;
        setTrangThai({ state: 'loi', moTa: msg });
        onLoiRef.current?.(msg);
        return;
      }
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
            if (Math.abs(video.currentTime - lastVideoTime) < 0.001) {
              stuckTicks++;
              if (stuckTicks >= 8) {
                stuckTicks = 0;
                scheduleRetry('Luồng hình đứng yên, đang tự nối lại');
                return;
              }
            } else {
              lastVideoTime = video.currentTime;
              stuckTicks = 0;
            }
            seenChay = true;
            setTrangThai({ state: 'chay' });
            return;
          }

          const bodyText = (
            (doc.body?.innerText || '') +
            ' ' +
            (videoStream?.textContent || '') +
            ' ' +
            (videoStream?.shadowRoot?.textContent || '')
          ).toLowerCase();
          if (
            bodyText.includes('disconnect') ||
            bodyText.includes('error') ||
            bodyText.includes('streams:') ||
            bodyText.includes('404')
          ) {
            scheduleRetry('go2rtc đang đăng nhập lại hoặc chưa mở được luồng');
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
        const shadowMedia = doc
          ?.querySelector('video-stream')
          ?.shadowRoot?.querySelectorAll('video, audio');
        shadowMedia?.forEach((m) => {
          const media = m as HTMLMediaElement;
          media.pause();
          media.removeAttribute('src');
          media.load();
        });
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
  }, [dangMo, tenLuong, lanThu]);

  return (
    <div
      className={`relative w-full overflow-hidden rounded-2xl border border-[#253459] bg-[#0B1220] ${
        fill ? 'aspect-video lg:aspect-auto lg:flex-1 lg:min-h-0 lg:h-full' : ''
      } ${className}`}
      style={fill ? undefined : { aspectRatio: aspect }}
    >
      {dangMo && (
        <iframe
          ref={iframeRef}
          src={srcUrl}
          title={`Camera ${tenLuong}`}
          className="h-full w-full border-0"
          allow="autoplay; fullscreen"
        />
      )}

      {trangThai.state === 'cho' && (
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center bg-[#0B1220]/80 text-center p-4">
          <RefreshCw size={22} className="animate-spin text-[#4F8BFF] mb-2" />
          <div className="text-xs font-semibold text-[#E6EDF7]">Đang nối camera ({tenLuong})…</div>
          <div className="text-[11px] text-[#8A9BB8] mt-1">
            Lần nối đầu mất 4–12 giây để go2rtc mở luồng WebRTC
          </div>
        </div>
      )}

      {trangThai.state === 'loi' && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-[#0B1220]/92 text-center p-4">
          <VideoOff size={24} className="text-[#EF4444] mb-2" />
          <div className="text-xs font-semibold text-[#E6EDF7] max-w-md">{trangThai.moTa}</div>
          <div className="mt-3">
            <FlatButton
              color={SH.Blue}
              onClick={() => {
                if (retryTimerRef.current !== null) {
                  window.clearTimeout(retryTimerRef.current);
                  retryTimerRef.current = null;
                }
                setTrangThai({ state: 'cho' });
                setLanThu(0);
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
