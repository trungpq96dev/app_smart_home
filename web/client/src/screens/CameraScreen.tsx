import React, { useMemo, useState } from 'react';
import { Maximize2, Minimize2, RefreshCw, Settings, Video } from 'lucide-react';
import type { CameraFeed } from '../data/models.js';
import {
  CamIcon,
  FlatButton,
  ImagePlaceholder,
  SectionHeader,
  SmartCard,
  StatusPill,
  TrangThaiCameraPill,
} from '../components/Common.js';
import { LuongCamera } from '../components/LuongCamera.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

function chuanHoaTen(s: string): string {
  return s
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
}

const TU_KHOA_CAM: Record<string, string[]> = {
  front: ['cua', 'chinh', 'truoc', 'front', 'door', 'gate', 'cong'],
  yard: ['san', 'vuon', 'yard', 'garden', 'dauhe', 'ngoai'],
  kitchen: ['bep', 'kitchen'],
  hall: ['hanh', 'lang', 'hall', 'stairs', 'cau', 'thang'],
};

export function CameraScreen() {
  const { cameras, cameraStatus, config, taiDanhSachCamera, navigateDest } = useSmartHome();
  const [phongToId, setPhongToId] = useState<string | null>(null);

  const danhSach = useMemo(() => {
    const luongs = cameraStatus.state === 'co' ? cameraStatus.ds : [];
    const daGhep = new Set<string>();

    const ghepCard = cameras.map((cam): { feed: CameraFeed; matchedStream: string | null } => {
      const targetNorm = chuanHoaTen(cam.name);
      const keys = TU_KHOA_CAM[cam.id] || [];
      let found: string | null = null;

      for (const s of luongs) {
        if (daGhep.has(s)) continue;
        const sn = chuanHoaTen(s);
        if (sn === targetNorm || sn.includes(targetNorm) || keys.some((k) => sn.includes(k))) {
          found = s;
          daGhep.add(s);
          break;
        }
      }
      return { feed: cam, matchedStream: found };
    });

    // Luong go2rtc khong khop o co dinh -> them thanh card rieng
    const extra: Array<{ feed: CameraFeed; matchedStream: string }> = luongs
      .filter((s) => !daGhep.has(s))
      .map((s) => ({
        feed: {
          id: `extra-${s}`,
          name: s,
          icon: 'videocam',
          accent: SH.Blue,
          luong: s,
        },
        matchedStream: s,
      }));

    return [...ghepCard, ...extra];
  }, [cameras, cameraStatus]);

  const spotItem = phongToId ? danhSach.find((x) => x.feed.id === phongToId) : null;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Header status card */}
      <SmartCard className="shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5">
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <h2 className="text-sm font-bold text-[#E6EDF7]">Camera an ninh (go2rtc proxy)</h2>
            <TrangThaiCameraPill cam={cameraStatus} />
          </div>
          <p className="text-[11px] text-[#8A9BB8] mt-0.5 truncate">
            {cameraStatus.state === 'co'
              ? `Máy chủ ${config.camera.host}:${config.camera.cong} trả về ${cameraStatus.ds.length} luồng: ${cameraStatus.ds.join(', ') || 'trống'}`
              : cameraStatus.state === 'loi'
                ? cameraStatus.moTa
                : 'Đang kiểm tra kết nối đến máy chủ go2rtc…'}
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <FlatButton onClick={taiDanhSachCamera} color={SH.Blue}>
            <RefreshCw size={13} className={cameraStatus.state === 'dang_do' ? 'animate-spin' : ''} />
            <span>Dò lại go2rtc</span>
          </FlatButton>
          <FlatButton onClick={() => navigateDest('settings')} color={SH.TextSecondary}>
            <Settings size={13} />
            <span>Cài đặt</span>
          </FlatButton>
        </div>
      </SmartCard>

      {/* Expanded single stream OR full-fit camera grid */}
      {spotItem ? (
        <SmartCard glowColor={spotItem.feed.accent} className="flex-1 min-h-0 flex flex-col gap-2 p-3">
          <SectionHeader
            title={`Đang phóng to: ${spotItem.feed.name}`}
            subtitle={spotItem.matchedStream ? `Luồng ${spotItem.matchedStream}` : 'Chưa có luồng thật'}
            action={
              <FlatButton color={SH.TextSecondary} onClick={() => setPhongToId(null)}>
                <Minimize2 size={14} />
                <span>Thu nhỏ</span>
              </FlatButton>
            }
          />
          {spotItem.matchedStream ? (
            <LuongCamera tenLuong={spotItem.matchedStream} kichHoat fill />
          ) : (
            <ImagePlaceholder
              fill
              title={spotItem.feed.name}
              subtitle="Không bịa video mẫu khi luồng chưa khớp tên trên go2rtc."
              color={spotItem.feed.accent}
            />
          )}
        </SmartCard>
      ) : (
        <div
          className={`flex-1 min-h-0 grid grid-cols-1 md:grid-cols-2 ${
            danhSach.length > 4 ? 'lg:grid-cols-3 lg:grid-rows-2' : 'lg:grid-rows-2'
          } gap-2.5`}
        >
          {danhSach.map(({ feed, matchedStream }) => (
            <SmartCard key={feed.id} className="flex flex-col min-h-0 gap-2 p-3">
              <div className="shrink-0 flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <span style={{ color: feed.accent }}>
                    <CamIcon icon={feed.icon} size={17} />
                  </span>
                  <div className="min-w-0">
                    <div className="text-xs font-bold text-[#E6EDF7] truncate">{feed.name}</div>
                    <div className="text-[11px] text-[#8A9BB8] truncate">
                      {matchedStream ? `Luồng: ${matchedStream}` : 'Chưa có luồng khớp'}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-1.5 shrink-0">
                  <StatusPill
                    label={matchedStream ? 'Trực tiếp' : 'Chưa nối'}
                    color={matchedStream ? SH.Green : SH.TextDim}
                  />
                  {matchedStream && (
                    <button
                      type="button"
                      onClick={() => setPhongToId(feed.id)}
                      className="rounded-lg border border-[#253459] bg-[#0E1627] p-1 text-[#8A9BB8] hover:text-white cursor-pointer"
                      title="Phóng to"
                    >
                      <Maximize2 size={13} />
                    </button>
                  )}
                </div>
              </div>

              {matchedStream ? (
                <LuongCamera tenLuong={matchedStream} kichHoat fill />
              ) : (
                <ImagePlaceholder
                  fill
                  title={`${feed.name} — Chưa có hình`}
                  subtitle={
                    cameraStatus.state === 'co'
                      ? 'Tên luồng go2rtc không khớp tự động. Kiểm tra tên luồng trong go2rtc.yaml.'
                      : 'go2rtc chưa kết nối được hoặc chưa cấu hình IP.'
                  }
                  color={feed.accent}
                />
              )}
            </SmartCard>
          ))}
        </div>
      )}
    </div>
  );
  void Video;
}

