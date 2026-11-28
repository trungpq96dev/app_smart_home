import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Bell,
  BellOff,
  Camera,
  ChevronLeft,
  ChevronRight,
  Cloud,
  Compass,
  Eye,
  EyeOff,
  FastForward,
  Footprints,
  HardDrive,
  History,
  Pause,
  Play,
  RefreshCw,
  Rewind,
  Settings,
  SkipBack,
  SkipForward,
  Square,
  Users,
  VideoOff,
} from 'lucide-react';
import {
  CamIcon,
  Dot,
  FlatButton,
  ImagePlaceholder,
  RoundIconButton,
  StatusPill,
  TrangThaiCameraPill,
} from '../components/Common.js';
import { KhungQuanLyKhuonMat } from '../components/KhungQuanLyKhuonMat.js';
import { LuongCamera } from '../components/LuongCamera.js';
import {
  MUC_ZOOM_TIMELINE_LIST,
  ThanhThoiGianCamera,
  type MucZoomTimelineInfo,
} from '../components/ThanhThoiGianCamera.js';
import { TrinhPhatXemLai } from '../components/TrinhPhatXemLai.js';
import {
  CameraPlaybackClient,
  PTZ_PRESETS_MAC_DINH,
  camBiAnRiengTu,
  camDangGhi,
  congTruNgayVn,
  dinhDangGioVn,
  doanTiepTheo,
  homNayVnStr,
  isDoanCucBo,
  isRecorderSong,
  locSuKienNguoi,
  mocNhaySuKien,
  suKienNguoiTiep,
  suKienNguoiTruoc,
  thoiLuongSuKienGiay,
  timViTriPhat,
  type CameraRecorderState,
  type DoanVideo,
  type DongThoiGianNgay,
  type NgayCoVideo,
  type PtzPreset,
  type SuKienCamera,
} from '../data/cameraRecorder.js';
import type { CameraFeed } from '../data/models.js';
import { useSmartHome, type TrangThaiCam } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

type CheDoManCam = 'TRUC_TIEP' | 'XEM_LAI' | 'KHUON_MAT';

const CHUA_NOI = 'Chưa khai báo go2rtc — vào màn Cài đặt điền địa chỉ máy chạy go2rtc.';

function formatCameraStamp(nowMs: number): string {
  const d = new Date(nowMs);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export function CameraScreen() {
  const {
    cameras,
    selectedCameraIndex,
    selectCamera,
    cameraStatus,
    cameraRecorder,
    dangChupAnh,
    chupAnhCamera,
    datCanhNguoiCamera,
    docThuChaoMat,
    dieuKhienPtz,
    goiPtzPreset,
    datCheDoRiengTuCamera,
    config,
    nowMs,
    taiDanhSachCamera,
    navigateDest,
    showToast,
  } = useSmartHome();

  const [cheDo, setCheDo] = useState<CheDoManCam>('TRUC_TIEP');
  const [lanTai, setLanTai] = useState(0);

  const cam = cameras[selectedCameraIndex] ?? cameras[0] ?? null;
  const cameraConfigured = config.camera.host.trim().length > 0 && config.camera.cong >= 1;
  const luongThat = cameraConfigured && cam?.luong ? cam.luong : null;
  const stamp = formatCameraStamp(nowMs);

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Thanh công cụ trên cùng: chuyển nhanh Trực tiếp | Xem lại + Trạng thái đầu ghi 24/24 */}
      <div className="shrink-0 flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-[#253459] bg-[#131D33] px-3 py-2">
        <div className="flex items-center gap-2.5 flex-wrap">
          <div className="inline-flex rounded-xl bg-[#0B1220] p-0.5 border border-[#1E2C4A]">
            <button
              type="button"
              onClick={() => setCheDo('TRUC_TIEP')}
              className={`rounded-lg px-3 py-1.5 text-xs transition-colors cursor-pointer ${
                cheDo === 'TRUC_TIEP'
                  ? 'bg-[#4F8BFF] text-white font-bold'
                  : 'text-[#8A9BB8] hover:text-[#E6EDF7]'
              }`}
            >
              Trực tiếp
            </button>
            <button
              type="button"
              onClick={() => setCheDo('XEM_LAI')}
              className={`rounded-lg px-3 py-1.5 text-xs transition-colors cursor-pointer ${
                cheDo === 'XEM_LAI'
                  ? 'bg-[#4F8BFF] text-white font-bold'
                  : 'text-[#8A9BB8] hover:text-[#E6EDF7]'
              }`}
            >
              Xem lại
            </button>
            <button
              type="button"
              onClick={() => setCheDo('KHUON_MAT')}
              className={`inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs transition-colors cursor-pointer ${
                cheDo === 'KHUON_MAT'
                  ? 'bg-[#4F8BFF] text-white font-bold'
                  : 'text-[#8A9BB8] hover:text-[#E6EDF7]'
              }`}
            >
              <Users size={12} />
              <span>Khuôn mặt</span>
            </button>
          </div>

          <Rec247StatusPill
            rec={cameraRecorder}
            luongDangChon={luongThat}
            trangThaiCam={cameraStatus}
          />
        </div>

        <div className="flex items-center gap-1.5">
          <RoundIconButton
            title="Dò lại luồng & làm mới"
            onClick={() => {
              void taiDanhSachCamera();
              setLanTai((n) => n + 1);
            }}
          >
            <RefreshCw size={15} className={cameraStatus.state === 'dang_do' ? 'animate-spin' : ''} />
          </RoundIconButton>
          <RoundIconButton
            title="Cài đặt go2rtc & cổng đầu ghi"
            onClick={() => navigateDest('settings')}
          >
            <Settings size={15} />
          </RoundIconButton>
        </div>
      </div>

      {/* Bố cục chính: Cột trái danh sách camera + thống kê đầu ghi | Vùng chính bên phải */}
      <div className="flex flex-col lg:flex-row gap-2.5 lg:flex-1 lg:min-h-0">
        {/* Cột trái trên Tablet/PC — Thanh cuộn ngang gọn trên iPhone dọc */}
        <div className="flex flex-col gap-2 lg:w-[172px] lg:shrink-0 lg:h-full">
          {cameras.length === 0 ? (
            <div className="rounded-xl border border-[#253459] bg-[#131D33] p-3">
              <VideoOff size={18} className="text-[#5F6F8C] mb-1.5" />
              <div className="text-xs font-semibold text-[#8A9BB8]">
                {cameraStatus.state === 'dang_do' ? 'Đang hỏi go2rtc…' : 'Không có camera'}
              </div>
            </div>
          ) : (
            <div className="flex lg:flex-col gap-2 overflow-x-auto lg:overflow-y-auto pb-0.5 lg:pb-0">
              {cameras.map((c, idx) => (
                <CameraRow
                  key={c.id}
                  feed={c}
                  selected={idx === selectedCameraIndex}
                  dangGhi={camDangGhi(cameraRecorder, c.luong)}
                  onClick={() => selectCamera(idx)}
                />
              ))}
            </div>
          )}

          <div className="hidden lg:block lg:flex-1" />

          <TheThongKeDauGhi rec={cameraRecorder} />
        </div>

        {/* Vùng chính bên phải */}
        <div className="flex-1 min-w-0 lg:min-h-0 lg:h-full">
          {cheDo === 'TRUC_TIEP' ? (
            <KhungCheDoTrucTiep
              cam={cam}
              luongThat={luongThat}
              cameraHost={config.camera.host}
              trangThaiCam={cameraStatus}
              cameraRecorder={cameraRecorder}
              stamp={stamp}
              lanTai={lanTai}
              dangChupAnh={dangChupAnh}
              onTaiLai={() => setLanTai((n) => n + 1)}
              onChupAnh={() => {
                if (luongThat) void chupAnhCamera(luongThat);
                else showToast(CHUA_NOI, 'warning');
              }}
              onDaoCanhNguoi={() => {
                const hienTai = cameraRecorder.detectPersonEnabled ?? true;
                datCanhNguoiCamera(!hienTai);
              }}
              onMoXemLai={() => setCheDo('XEM_LAI')}
              onDaoRiengTu={() => void datCheDoRiengTuCamera(!cameraRecorder.privacyMode)}
              onPtzMove={(dir) => {
                if (luongThat) void dieuKhienPtz(luongThat, dir, 0.5);
              }}
              onPtzPreset={(preset) => {
                if (luongThat) void goiPtzPreset(luongThat, preset);
              }}
              onNotice={(m) => showToast(m, 'warning')}
            />
          ) : cheDo === 'XEM_LAI' ? (
            <KhungCheDoXemLai
              tenCamHienThi={cam?.name ?? 'Camera'}
              luongThat={luongThat}
              cameraRecorder={cameraRecorder}
              lanTai={lanTai}
              onNotice={(m) => showToast(m, 'info')}
            />
          ) : (
            <KhungQuanLyKhuonMat
              luongThat={luongThat}
              cameraRecorder={cameraRecorder}
              lanTai={lanTai}
              onDocThuChao={docThuChaoMat}
              onNotice={(m, tone) => showToast(m, tone ?? 'info')}
            />
          )}
        </div>
      </div>
    </div>
  );
}

// ============================================================ CHẾ ĐỘ TRỰC TIẾP

function KhungCheDoTrucTiep({
  cam,
  luongThat,
  cameraHost,
  trangThaiCam,
  cameraRecorder,
  stamp,
  lanTai,
  dangChupAnh,
  onTaiLai,
  onChupAnh,
  onDaoCanhNguoi,
  onMoXemLai,
  onDaoRiengTu,
  onPtzMove,
  onPtzPreset,
  onNotice,
}: {
  cam: CameraFeed | null;
  luongThat: string | null;
  cameraHost: string;
  trangThaiCam: TrangThaiCam;
  cameraRecorder: CameraRecorderState;
  stamp: string;
  lanTai: number;
  dangChupAnh: boolean;
  onTaiLai: () => void;
  onChupAnh: () => void;
  onDaoCanhNguoi: () => void;
  onMoXemLai: () => void;
  onDaoRiengTu: () => void;
  onPtzMove: (dir: string) => void;
  onPtzPreset: (preset: string) => void;
  onNotice: (msg: string) => void;
}) {
  const [moPtz, setMoPtz] = useState(false);
  const [presets, setPresets] = useState<PtzPreset[]>(PTZ_PRESETS_MAC_DINH);
  const camHoTroPtz = Boolean(
    luongThat &&
      (cameraRecorder.ptzCameras.includes(luongThat) ||
        cameraRecorder.ptzCameras.includes(luongThat.replace(/_sd$/i, ''))),
  );
  const dangAnRiengTu = camBiAnRiengTu(cameraRecorder, luongThat);

  useEffect(() => {
    let cancelled = false;
    if (!luongThat || (!moPtz && !camHoTroPtz)) return;
    void CameraPlaybackClient.taiPtzPresets(luongThat, cameraRecorder.apiPort).then((list) => {
      if (!cancelled) setPresets(list);
    });
    return () => {
      cancelled = true;
    };
  }, [luongThat, moPtz, camHoTroPtz, cameraRecorder.apiPort]);

  const dangCanhNguoi = cameraRecorder.detectPersonEnabled;
  const nhanCanhNguoi =
    dangCanhNguoi === true
      ? 'Canh người: Bật'
      : dangCanhNguoi === false
        ? 'Canh người: Tắt'
        : 'Canh người';

  const suKienGanNhat =
    luongThat && cameraRecorder.nguoiGanNhat?.camera === luongThat
      ? `Người gần nhất: ${cameraRecorder.nguoiGanNhat.thoiGian.slice(-8)}`
      : null;

  const phuText = luongThat
    ? `go2rtc · ${cameraHost}${camDangGhi(cameraRecorder, luongThat) ? ' · Đang ghi 24/24 lên Drive' : ''}`
    : 'chưa nối luồng hình';

  return (
    <div className="flex flex-col gap-2.5 lg:h-full">
      <div className="relative w-full aspect-video lg:aspect-auto lg:flex-1 lg:min-h-0 rounded-2xl overflow-hidden">
        {dangAnRiengTu ? (
          <div className="flex h-full min-h-[220px] w-full flex-col items-center justify-center rounded-2xl border border-[#253459] bg-[#0B1220] p-6 text-center gap-2.5">
            <EyeOff size={34} className="text-[#A855F7]" />
            <div className="text-sm font-bold text-[#E6EDF7]">Chế độ Riêng tư đang bật</div>
            <p className="text-xs text-[#8A9BB8] max-w-sm">
              Camera trong nhà đã tạm ẩn hình ảnh và tạm dừng ghi hình. Bấm nút bên dưới để mở lại.
            </p>
            <FlatButton onClick={onDaoRiengTu} color={SH.Purple}>
              <Eye size={14} />
              <span>Tắt Chế độ Riêng tư</span>
            </FlatButton>
          </div>
        ) : luongThat ? (
          <>
            <LuongCamera
              key={`${luongThat}-${lanTai}`}
              tenLuong={luongThat}
              kichHoat
              fill
              onLoi={onNotice}
            />
            <PhuDeCamera
              ten={cam?.name ?? luongThat}
              phu={phuText}
              stamp={stamp}
              trucTiep
              suKienGanNhat={suKienGanNhat}
            />
          </>
        ) : cam ? (
          <div className="relative h-full w-full">
            <ImagePlaceholder
              fill
              title={cam.name}
              subtitle="Khai báo địa chỉ go2rtc ở màn Cài đặt để xem luồng hình trực tiếp."
              color={cam.accent}
              icon={<CamIcon icon={cam.icon} size={24} />}
            />
            <PhuDeCamera ten={cam.name} phu={phuText} stamp={stamp} trucTiep={false} />
          </div>
        ) : (
          <div className="flex h-full min-h-[220px] w-full flex-col items-center justify-center rounded-2xl border border-[#253459] bg-[#131D33] p-6 text-center">
            <VideoOff size={32} className="text-[#5F6F8C] mb-2.5" />
            <div className="text-xs text-[#8A9BB8] max-w-md">
              {trangThaiCam.state === 'dang_do'
                ? 'Đang hỏi go2rtc…'
                : trangThaiCam.state === 'loi'
                  ? trangThaiCam.moTa
                  : 'Chưa có luồng camera nào'}
            </div>
          </div>
        )}
      </div>

      {/* Khung điều khiển xoay Camera 360° (PTZ ONVIF) */}
      {(moPtz || camHoTroPtz) && luongThat && (
        <div className="shrink-0 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#131D33] px-3 py-2">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-[#93C5FD] mr-1">
              <Compass size={13} />
              <span>Xoay 360°</span>
            </span>
            <button
              type="button"
              onClick={() => onPtzMove('left')}
              title="Xoay trái"
              className="rounded-lg border border-[#253459] bg-[#0E1627] p-1.5 text-[#E6EDF7] hover:border-[#4F8BFF] cursor-pointer"
            >
              <ArrowLeft size={14} />
            </button>
            <button
              type="button"
              onClick={() => onPtzMove('up')}
              title="Xoay lên"
              className="rounded-lg border border-[#253459] bg-[#0E1627] p-1.5 text-[#E6EDF7] hover:border-[#4F8BFF] cursor-pointer"
            >
              <ArrowUp size={14} />
            </button>
            <button
              type="button"
              onClick={() => onPtzMove('stop')}
              title="Dừng xoay"
              className="rounded-lg border border-[#EF4444]/40 bg-[#EF4444]/15 p-1.5 text-[#FCA5A5] hover:bg-[#EF4444]/25 cursor-pointer"
            >
              <Square size={12} />
            </button>
            <button
              type="button"
              onClick={() => onPtzMove('down')}
              title="Xoay xuống"
              className="rounded-lg border border-[#253459] bg-[#0E1627] p-1.5 text-[#E6EDF7] hover:border-[#4F8BFF] cursor-pointer"
            >
              <ArrowDown size={14} />
            </button>
            <button
              type="button"
              onClick={() => onPtzMove('right')}
              title="Xoay phải"
              className="rounded-lg border border-[#253459] bg-[#0E1627] p-1.5 text-[#E6EDF7] hover:border-[#4F8BFF] cursor-pointer"
            >
              <ArrowRight size={14} />
            </button>
          </div>
          <div className="flex items-center gap-1.5 flex-wrap">
            {presets.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => onPtzPreset(p.id)}
                className="rounded-lg border border-[#4F8BFF]/40 bg-[#4F8BFF]/15 px-2.5 py-1 text-[11px] font-semibold text-[#93C5FD] hover:bg-[#4F8BFF]/25 cursor-pointer"
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Nút thao tác dưới khung trực tiếp */}
      <div className="shrink-0 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
        <FlatButton onClick={onChupAnh} disabled={dangChupAnh} color={SH.Blue} className="w-full">
          <Camera size={14} />
          <span>{dangChupAnh ? 'Đang chụp…' : 'Chụp ảnh'}</span>
        </FlatButton>
        <FlatButton
          onClick={onDaoCanhNguoi}
          color={dangCanhNguoi === false ? SH.TextSecondary : SH.Orange}
          className="w-full"
        >
          {dangCanhNguoi === false ? <BellOff size={14} /> : <Bell size={14} />}
          <span>{nhanCanhNguoi}</span>
        </FlatButton>
        <FlatButton
          onClick={onDaoRiengTu}
          color={cameraRecorder.privacyMode ? SH.Purple : SH.TextSecondary}
          className="w-full"
        >
          {cameraRecorder.privacyMode ? <EyeOff size={14} /> : <Eye size={14} />}
          <span>{cameraRecorder.privacyMode ? 'Riêng tư: Bật' : 'Riêng tư'}</span>
        </FlatButton>
        <FlatButton
          onClick={() => setMoPtz((v) => !v)}
          color={moPtz || camHoTroPtz ? SH.Cyan : SH.TextSecondary}
          className="w-full"
        >
          <Compass size={14} />
          <span>Xoay 360°</span>
        </FlatButton>
        <FlatButton onClick={onMoXemLai} color={SH.Purple} className="w-full">
          <History size={14} />
          <span>Xem lại 24/24</span>
        </FlatButton>
        <FlatButton onClick={onTaiLai} color={SH.TextSecondary} className="w-full">
          <RefreshCw size={14} />
          <span>Tải lại hình</span>
        </FlatButton>
      </div>
    </div>
  );
}

// ============================================================ CHẾ ĐỘ XEM LẠI (IMOU / EZVIZ)

function KhungCheDoXemLai({
  tenCamHienThi,
  luongThat,
  cameraRecorder,
  lanTai,
  onNotice,
}: {
  tenCamHienThi: string;
  luongThat: string | null;
  cameraRecorder: CameraRecorderState;
  lanTai: number;
  onNotice: (msg: string) => void;
}) {
  const congApi = cameraRecorder.apiPort;
  const homNay = useMemo(() => homNayVnStr(), []);

  const [ngayChon, setNgayChon] = useState(homNay);
  const [dsNgay, setDsNgay] = useState<NgayCoVideo[]>([]);
  const [timeline, setTimeline] = useState<DongThoiGianNgay | null>(null);
  const [dangTaiTl, setDangTaiTl] = useState(false);
  const [loiTl, setLoiTl] = useState<string | null>(null);
  const [nhipTaiLai, setNhipTaiLai] = useState(0);

  const [doanDangPhat, setDoanDangPhat] = useState<DoanVideo | null>(null);
  const [seekOffsetMs, setSeekOffsetMs] = useState(0);
  const [seekVersion, setSeekVersion] = useState(0);
  const [currentTs, setCurrentTs] = useState(0);

  const [dangPhat, setDangPhat] = useState(true);
  const [tocDo, setTocDo] = useState<1 | 2 | 4>(1);
  const [zoom, setZoom] = useState<MucZoomTimelineInfo>(MUC_ZOOM_TIMELINE_LIST[1]); // mặc định 6h
  const [chiTuaNguoi, setChiTuaNguoi] = useState(false);

  const doanDangPhatRef = useRef(doanDangPhat);
  doanDangPhatRef.current = doanDangPhat;

  const nhayToiMoc = (tl: DongThoiGianNgay, dichTs: number) => {
    const viTri = timViTriPhat(tl, dichTs);
    if (!viTri) {
      onNotice('Chưa có đoạn ghi hình nào tại mốc này.');
      return;
    }
    setCurrentTs(viTri.targetEpochSec);
    setSeekOffsetMs(viTri.offsetMs);
    if (doanDangPhatRef.current?.id !== viTri.segment.id) {
      setDoanDangPhat(viTri.segment);
    }
    setSeekVersion((v) => v + 1);
    setDangPhat(true);
  };

  useEffect(() => {
    setDoanDangPhat(null);
    doanDangPhatRef.current = null;
  }, [luongThat, ngayChon]);

  useEffect(() => {
    let cancelled = false;
    if (!luongThat) {
      setTimeline(null);
      setDoanDangPhat(null);
      setLoiTl(CHUA_NOI);
      return;
    }

    setDangTaiTl(true);
    setLoiTl(null);

    (async () => {
      const days = await CameraPlaybackClient.danhSachNgay(luongThat, congApi);
      if (!cancelled) setDsNgay(days);

      const res = await CameraPlaybackClient.taiDongThoiGian(luongThat, ngayChon, congApi);
      if (cancelled) return;
      if (res.ok) {
        const tl = res.data;
        setTimeline(tl);
        if (tl.segments.length > 0) {
          const skNguoi = locSuKienNguoi(tl);
          const suKienCuoi = skNguoi[skNguoi.length - 1];
          const segCuoi = tl.segments[tl.segments.length - 1];
          const mocMoDau = suKienCuoi
            ? mocNhaySuKien(tl, suKienCuoi)
            : Math.max(segCuoi.startTs, segCuoi.endTs - 30);
          if (!doanDangPhatRef.current) {
            nhayToiMoc(tl, mocMoDau);
          }
        } else {
          setDoanDangPhat(null);
          setCurrentTs(tl.dayStartTs + 12 * 3600);
        }
      } else {
        setTimeline(null);
        setDoanDangPhat(null);
        setLoiTl(res.error);
      }
      setDangTaiTl(false);
    })();

    return () => {
      cancelled = true;
    };
  }, [luongThat, ngayChon, lanTai, nhipTaiLai, congApi]);

  const suKienNguoi = useMemo(() => locSuKienNguoi(timeline), [timeline]);
  const coNguoiHomDo = dsNgay.find((d) => d.ngay === ngayChon)?.soSuKienNguoi ?? 0;
  const coDuLieu = Boolean(timeline && timeline.segments.length > 0);
  const dayStart = timeline?.dayStartTs ?? 0;
  const dayEnd = timeline?.dayEndTs ?? 86_400;

  return (
    <div className="flex flex-col lg:flex-row gap-2.5 lg:h-full lg:min-h-0">
      {/* Cột giữa: Chọn ngày + Khung phát MP4 + Nút tua + Thước Timeline 24/24 */}
      <div className="flex flex-col gap-1.5 flex-1 min-w-0 lg:h-full lg:min-h-0">
        {/* 1. Thanh chọn ngày & mức zoom Timeline */}
        <div className="shrink-0 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-1.5">
            <NutDieuKhienNho
              title="Ngày trước"
              onClick={() => setNgayChon((d) => congTruNgayVn(d, -1))}
            >
              <ChevronLeft size={15} />
            </NutDieuKhienNho>

            <button
              type="button"
              onClick={() => setNgayChon(homNay)}
              title="Bấm để nhảy nhanh về hôm nay"
              className="inline-flex items-center gap-1.5 rounded-lg border border-[#253459] bg-[#131D33] px-2.5 py-1 text-xs font-semibold text-[#E6EDF7] hover:bg-[#192642] cursor-pointer"
            >
              <span>{ngayChon === homNay ? `Hôm nay (${ngayChon})` : ngayChon}</span>
              {coNguoiHomDo > 0 && (
                <span className="inline-flex items-center gap-1 text-[11px] font-bold text-[#F97316]">
                  <Dot color={SH.Orange} size={6} />
                  <span>{coNguoiHomDo}</span>
                </span>
              )}
            </button>

            <NutDieuKhienNho
              title="Ngày sau"
              disabled={ngayChon >= homNay}
              onClick={() => {
                const next = congTruNgayVn(ngayChon, 1);
                if (next <= homNay) setNgayChon(next);
              }}
            >
              <ChevronRight size={15} />
            </NutDieuKhienNho>

            <NutDieuKhienNho
              title="Làm mới dòng thời gian"
              onClick={() => setNhipTaiLai((n) => n + 1)}
            >
              <RefreshCw size={14} className={dangTaiTl ? 'animate-spin' : ''} />
            </NutDieuKhienNho>
          </div>

          <div className="inline-flex rounded-lg border border-[#253459] bg-[#131D33] p-0.5">
            {MUC_ZOOM_TIMELINE_LIST.map((muc) => {
              const chon = muc.id === zoom.id;
              return (
                <button
                  key={muc.id}
                  type="button"
                  onClick={() => setZoom(muc)}
                  className={`rounded-md px-2.5 py-1 text-[11px] transition-colors cursor-pointer ${
                    chon
                      ? 'bg-[#4F8BFF] text-white font-bold'
                      : 'text-[#8A9BB8] hover:text-[#E6EDF7]'
                  }`}
                >
                  {muc.nhan}
                </button>
              );
            })}
          </div>
        </div>

        {/* 2. Khung phát video MP4 */}
        <div className="relative w-full aspect-video lg:aspect-auto lg:flex-1 lg:min-h-0 rounded-2xl border border-[#253459] bg-[#131D33] overflow-hidden">
          {dangTaiTl && !timeline ? (
            <div className="flex h-full w-full items-center justify-center">
              <RefreshCw size={26} className="animate-spin text-[#4F8BFF]" />
            </div>
          ) : loiTl ? (
            <KhungThongBaoXemLai tieuDe="Chưa tải được lịch sử ghi hình" chiTiet={loiTl} />
          ) : !timeline || timeline.segments.length === 0 || !doanDangPhat ? (
            <KhungThongBaoXemLai
              tieuDe={`Chưa có video ngày ${ngayChon}`}
              chiTiet="Đầu ghi trên serverubuntu tự lưu 24/24 theo từng đoạn 10 phút — đoạn đầu tiên sẽ xuất hiện ngay khi chốt xong."
            />
          ) : (
            <>
              <TrinhPhatXemLai
                url={CameraPlaybackClient.urlVideoSegment(doanDangPhat.id, congApi)}
                seekOffsetMs={seekOffsetMs}
                seekVersion={seekVersion}
                dangPhat={dangPhat}
                tocDo={tocDo}
                laTuDrive={!isDoanCucBo(doanDangPhat)}
                onTienTrinh={(posMs) => {
                  const mocMoi = doanDangPhat.startTs + Math.floor(posMs / 1000);
                  setCurrentTs(mocMoi);
                  if (chiTuaNguoi) {
                    const dangTrongSuKien = suKienNguoi.some(
                      (ev) => mocMoi >= ev.startTs - 4 && mocMoi <= ev.endTs + 3,
                    );
                    if (!dangTrongSuKien) {
                      const ke = suKienNguoiTiep(timeline, mocMoi);
                      if (ke) nhayToiMoc(timeline, mocNhaySuKien(timeline, ke));
                    }
                  }
                }}
                onHetDoan={() => {
                  const tiep = doanTiepTheo(timeline, doanDangPhat.id);
                  if (tiep) {
                    setSeekOffsetMs(0);
                    setCurrentTs(tiep.startTs);
                    setDoanDangPhat(tiep);
                    setSeekVersion((v) => v + 1);
                  } else {
                    setDangPhat(false);
                  }
                }}
                onLoi={onNotice}
              />

              <div className="pointer-events-none absolute top-2 left-2 flex flex-wrap items-center gap-1.5">
                <StatusPill label={`Xem lại · ${dinhDangGioVn(currentTs)}`} color={SH.Amber} />
                <StatusPill
                  label={isDoanCucBo(doanDangPhat) ? 'Bộ đệm 10GB' : 'Google Drive 5TB'}
                  color={isDoanCucBo(doanDangPhat) ? SH.Green : SH.Blue}
                  dot={false}
                />
              </div>

              <div className="pointer-events-none absolute bottom-2 left-2 rounded-md bg-[#0B1220]/75 px-2 py-0.5 text-[11px] text-[#8A9BB8]">
                {tenCamHienThi} · {ngayChon}
              </div>
            </>
          )}
        </div>

        {/* 3. Hàng nút điều khiển tua video + nhảy sự kiện người + tốc độ 1x/2x/4x */}
        <div className="shrink-0 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-1.5">
            <NutDieuKhienNho
              title="Sự kiện người trước"
              disabled={!coDuLieu}
              onClick={() => {
                if (!timeline) return;
                const ev = suKienNguoiTruoc(timeline, currentTs);
                if (ev) nhayToiMoc(timeline, mocNhaySuKien(timeline, ev));
                else onNotice('Không còn sự kiện có người nào trước mốc này trong ngày.');
              }}
            >
              <SkipBack size={15} />
            </NutDieuKhienNho>

            <NutDieuKhienNho
              title="Lùi 10 giây"
              disabled={!coDuLieu}
              onClick={() => timeline && nhayToiMoc(timeline, currentTs - 10)}
            >
              <Rewind size={15} />
            </NutDieuKhienNho>

            <NutDieuKhienNho
              title={dangPhat ? 'Tạm dừng' : 'Phát'}
              disabled={!coDuLieu}
              noiBat={coDuLieu}
              onClick={() => setDangPhat((p) => !p)}
            >
              {dangPhat ? <Pause size={15} /> : <Play size={15} />}
            </NutDieuKhienNho>

            <NutDieuKhienNho
              title="Tiến 10 giây"
              disabled={!coDuLieu}
              onClick={() => timeline && nhayToiMoc(timeline, currentTs + 10)}
            >
              <FastForward size={15} />
            </NutDieuKhienNho>

            <NutDieuKhienNho
              title="Sự kiện người tiếp"
              disabled={!coDuLieu}
              onClick={() => {
                if (!timeline) return;
                const ev = suKienNguoiTiep(timeline, currentTs);
                if (ev) nhayToiMoc(timeline, mocNhaySuKien(timeline, ev));
                else onNotice('Đã ở sự kiện có người cuối cùng trong ngày.');
              }}
            >
              <SkipForward size={15} />
            </NutDieuKhienNho>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              disabled={!coDuLieu}
              onClick={() => setTocDo((s) => (s === 1 ? 2 : s === 2 ? 4 : 1))}
              className="rounded-lg border border-[#253459] bg-[#131D33] px-2.5 py-1 text-xs font-bold disabled:opacity-40 cursor-pointer"
              style={{ color: tocDo > 1 ? SH.Amber : '#E6EDF7' }}
            >
              {tocDo}x
            </button>

            <button
              type="button"
              onClick={() => setChiTuaNguoi((v) => !v)}
              className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-[11px] font-semibold transition-colors cursor-pointer"
              style={{
                backgroundColor: chiTuaNguoi ? tint(SH.Orange, 0.18) : '#131D33',
                border: `1px solid ${chiTuaNguoi ? SH.Orange : '#253459'}`,
                color: chiTuaNguoi ? SH.Orange : '#8A9BB8',
              }}
            >
              <Footprints size={13} />
              <span>Chỉ mốc người</span>
            </button>
          </div>
        </div>

        {/* 4. Thanh thước dòng thời gian 24/24 kéo mượt bằng ngón tay */}
        <ThanhThoiGianCamera
          dayStartTs={dayStart}
          dayEndTs={dayEnd}
          currentTs={currentTs > 0 ? currentTs : dayStart + 43_200}
          zoom={zoom}
          segments={timeline?.segments ?? []}
          events={timeline?.events ?? []}
          onChonMoc={(moc) => timeline && nhayToiMoc(timeline, moc)}
          className="shrink-0"
        />
      </div>

      {/* Cột phải: Danh sách sự kiện "Có người" kèm thumbnail */}
      <CotSuKienNguoi
        suKienNguoi={suKienNguoi}
        currentTs={currentTs}
        congApi={congApi}
        onChonSuKien={(ev) => timeline && nhayToiMoc(timeline, mocNhaySuKien(timeline, ev))}
      />
    </div>
  );
}

// ============================================================ THÀNH PHẦN PHỤ

function CotSuKienNguoi({
  suKienNguoi,
  currentTs,
  congApi,
  onChonSuKien,
}: {
  suKienNguoi: SuKienCamera[];
  currentTs: number;
  congApi: number | null;
  onChonSuKien: (ev: SuKienCamera) => void;
}) {
  const daoNguoc = useMemo(() => [...suKienNguoi].reverse(), [suKienNguoi]);

  return (
    <div className="flex flex-col rounded-2xl border border-[#253459] bg-[#131D33] p-2.5 lg:w-[224px] lg:shrink-0 lg:h-full max-h-[260px] lg:max-h-none">
      <div className="flex items-center justify-between gap-2 mb-2 shrink-0">
        <div className="flex items-center gap-1.5">
          <Footprints size={15} className="text-[#F97316]" />
          <span className="text-xs font-bold text-[#E6EDF7]">Có người</span>
        </div>
        <span className="text-[11px] font-semibold text-[#F97316]">{suKienNguoi.length} mốc</span>
      </div>

      {daoNguoc.length === 0 ? (
        <div className="flex-1 flex items-center justify-center py-6 px-2 text-center text-[11px] text-[#5F6F8C]">
          Chưa ghi nhận người nào trong ngày này.
        </div>
      ) : (
        <div className="flex-1 min-h-0 overflow-y-auto flex flex-col gap-1.5 pr-0.5">
          {daoNguoc.map((ev) => {
            const dangXem = currentTs >= ev.startTs - 4 && currentTs <= ev.endTs + 2;
            return (
              <TheSuKienNguoi
                key={ev.id}
                ev={ev}
                dangXem={dangXem}
                congApi={congApi}
                onClick={() => onChonSuKien(ev)}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}

function TheSuKienNguoi({
  ev,
  dangXem,
  congApi,
  onClick,
}: {
  ev: SuKienCamera;
  dangXem: boolean;
  congApi: number | null;
  onClick: () => void;
}) {
  const [thumbLoi, setThumbLoi] = useState(false);

  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-2 rounded-xl p-1.5 text-left transition-colors cursor-pointer w-full"
      style={{
        backgroundColor: dangXem ? tint(SH.Orange, 0.16) : '#192642',
        border: `1px solid ${dangXem ? SH.Orange : '#253459'}`,
      }}
    >
      <div className="h-[40px] w-[64px] shrink-0 overflow-hidden rounded-md bg-[#0B1220] flex items-center justify-center">
        {ev.hasThumb && !thumbLoi ? (
          <img
            src={CameraPlaybackClient.urlThumbnail(ev.id, congApi)}
            alt="Ảnh phát hiện người"
            loading="lazy"
            onError={() => setThumbLoi(true)}
            className="h-full w-full object-cover"
          />
        ) : (
          <Footprints size={16} className="text-[#F97316]" />
        )}
      </div>

      <div className="min-w-0 flex-1">
        <div
          className="text-xs font-bold font-mono truncate"
          style={{ color: dangXem ? SH.Orange : '#E6EDF7' }}
        >
          {dinhDangGioVn(ev.startTs)}
        </div>
        <div className="text-[11px] text-[#8A9BB8] truncate">
          {ev.personCount} người · {thoiLuongSuKienGiay(ev)}s · {Math.round(ev.maxConf * 100)}%
        </div>
      </div>
    </button>
  );
}

function TheThongKeDauGhi({ rec }: { rec: CameraRecorderState }) {
  const dangSong = isRecorderSong(rec);
  return (
    <div className="rounded-xl border border-[#253459] bg-[#131D33] p-2.5 flex flex-col gap-1">
      <div className="flex items-center gap-1.5">
        {rec.driveEnabled ? (
          <Cloud size={14} style={{ color: dangSong ? SH.Green : '#5F6F8C' }} />
        ) : (
          <HardDrive size={14} style={{ color: dangSong ? SH.Green : '#5F6F8C' }} />
        )}
        <span className="text-xs font-bold text-[#E6EDF7] truncate">Đầu ghi 24/24</span>
      </div>

      {rec.online === null ? (
        <div className="text-[11px] text-[#5F6F8C]">Chưa nhận MQTT camera/#</div>
      ) : !dangSong ? (
        <div className="text-[11px] text-[#EF4444]">Đầu ghi đang tắt (offline)</div>
      ) : (
        <div className="flex flex-wrap lg:flex-col gap-x-3 gap-y-0.5 text-[11px] text-[#8A9BB8]">
          <span>
            Đệm: {rec.cacheUsedGb.toFixed(1)}/{rec.cacheMaxGb.toFixed(0)} GB
          </span>
          <span>Drive hôm nay: {rec.uploadedToday} đoạn</span>
          <span style={{ color: rec.personToday > 0 ? SH.Orange : '#8A9BB8' }}>
            Người hôm nay: {rec.personToday} lần
          </span>
        </div>
      )}
    </div>
  );
}

function Rec247StatusPill({
  rec,
  luongDangChon,
  trangThaiCam,
}: {
  rec: CameraRecorderState;
  luongDangChon: string | null;
  trangThaiCam: TrangThaiCam;
}) {
  if (camDangGhi(rec, luongDangChon)) {
    return <StatusPill label="REC 24/24 · Drive" color={SH.Green} pulse />;
  }
  if (rec.online === false) {
    return <StatusPill label="Đầu ghi tắt" color={SH.Red} />;
  }
  return <TrangThaiCameraPill cam={trangThaiCam} />;
}

function CameraRow({
  feed,
  selected,
  dangGhi,
  onClick,
}: {
  feed: CameraFeed;
  selected: boolean;
  dangGhi: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-2 rounded-xl p-2 text-left transition-colors shrink-0 min-w-[144px] lg:min-w-0 lg:w-full cursor-pointer"
      style={{
        backgroundColor: selected ? tint(SH.Blue, 0.15) : '#131D33',
        border: `1px solid ${selected ? '#4F8BFF' : '#253459'}`,
      }}
    >
      <div
        className="flex h-8 w-10 shrink-0 items-center justify-center rounded-lg"
        style={{
          backgroundColor: tint(feed.accent, 0.16),
          border: `1px solid ${tint(feed.accent, 0.35)}`,
          color: feed.accent,
        }}
      >
        <CamIcon icon={feed.icon} size={16} />
      </div>
      <div className="min-w-0 flex-1">
        <div
          className="text-xs font-semibold truncate"
          style={{ color: selected ? '#E6EDF7' : '#8A9BB8' }}
        >
          {feed.name}
        </div>
        {dangGhi && <div className="text-[10px] font-semibold text-[#22C55E] truncate">● 24/24</div>}
      </div>
    </button>
  );
}

function PhuDeCamera({
  ten,
  phu,
  stamp,
  trucTiep,
  suKienGanNhat,
}: {
  ten: string;
  phu: string;
  stamp: string;
  trucTiep: boolean;
  suKienGanNhat?: string | null;
}) {
  return (
    <>
      {trucTiep && (
        <div className="pointer-events-none absolute top-2.5 left-2.5 inline-flex items-center gap-1.5 rounded-md bg-[#0B1220]/75 px-2 py-1 text-xs font-semibold text-[#E6EDF7]">
          <Dot color={SH.Red} pulse size={7} />
          <span>Trực tiếp</span>
        </div>
      )}
      <div className="pointer-events-none absolute top-2.5 right-2.5 rounded-md bg-[#0B1220]/75 px-2 py-1 text-[11px] font-mono font-semibold text-[#E6EDF7]">
        {stamp}
      </div>
      {suKienGanNhat && (
        <div className="pointer-events-none absolute bottom-2.5 right-2.5 rounded-md bg-[#0B1220]/80 px-2 py-1 text-[11px] font-semibold text-[#F97316]">
          {suKienGanNhat}
        </div>
      )}
      <div className="pointer-events-none absolute bottom-2.5 left-2.5 max-w-[70%] rounded-md bg-[#0B1220]/65 px-2 py-1">
        <div className="text-xs font-bold text-[#E6EDF7] truncate">{ten}</div>
        <div className="text-[10px] text-[#8A9BB8] truncate">{phu}</div>
      </div>
    </>
  );
}

function KhungThongBaoXemLai({ tieuDe, chiTiet }: { tieuDe: string; chiTiet: string }) {
  return (
    <div className="flex h-full min-h-[210px] w-full flex-col items-center justify-center px-6 text-center">
      <History size={28} className="text-[#5F6F8C] mb-2" />
      <div className="text-xs font-bold text-[#8A9BB8]">{tieuDe}</div>
      <div className="text-[11px] text-[#5F6F8C] mt-1 max-w-md">{chiTiet}</div>
    </div>
  );
}

function NutDieuKhienNho({
  children,
  title,
  disabled = false,
  noiBat = false,
  onClick,
}: {
  children: React.ReactNode;
  title: string;
  disabled?: boolean;
  noiBat?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      title={title}
      disabled={disabled}
      onClick={onClick}
      className="inline-flex h-[30px] w-[30px] items-center justify-center rounded-lg transition-all active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
      style={{
        backgroundColor: noiBat ? SH.Blue : '#131D33',
        border: `1px solid ${noiBat ? SH.Blue : '#253459'}`,
        color: noiBat ? '#ffffff' : '#E6EDF7',
      }}
    >
      {children}
    </button>
  );
}
