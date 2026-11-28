import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Camera,
  Check,
  Loader2,
  RefreshCw,
  Timer,
  Trash2,
  Upload,
  UserPlus,
  Users,
  Volume2,
} from 'lucide-react';
import {
  CameraPlaybackClient,
  dinhDangGioVn,
  type AnhNguoiLa,
  type CameraRecorderState,
  type NguoiQuenMat,
} from '../data/cameraRecorder.js';
import { SH } from '../theme/colors.js';

/** Nén & chuẩn hóa chiều ảnh (trình duyệt hiện đại tự xoay theo EXIF khi vẽ canvas) về <= 960px JPEG base64. */
async function compressImageFileToBase64(file: File): Promise<string> {
  const objUrl = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error('Không đọc được tệp ảnh.'));
      el.src = objUrl;
    });
    const maxDim = Math.max(img.naturalWidth || img.width, img.naturalHeight || img.height);
    const scale = maxDim > 960 ? 960 / maxDim : 1;
    const w = Math.max(1, Math.round((img.naturalWidth || img.width) * scale));
    const h = Math.max(1, Math.round((img.naturalHeight || img.height) * scale));
    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Trình duyệt không hỗ trợ xử lý ảnh.');
    ctx.drawImage(img, 0, 0, w, h);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.88);
    const comma = dataUrl.indexOf(',');
    return comma >= 0 ? dataUrl.slice(comma + 1) : dataUrl;
  } finally {
    URL.revokeObjectURL(objUrl);
  }
}

export function KhungQuanLyKhuonMat({
  luongThat,
  cameraRecorder,
  lanTai,
  onDocThuChao,
  onNotice,
}: {
  luongThat: string | null;
  cameraRecorder: CameraRecorderState;
  lanTai: number;
  onDocThuChao: (ten: string) => void;
  onNotice: (msg: string, tone?: 'info' | 'success' | 'warning' | 'error') => void;
}) {
  const apiPort = cameraRecorder.apiPort;
  const [tenNguoi, setTenNguoi] = useState('');
  const [dsNguoiQuen, setDsNguoiQuen] = useState<NguoiQuenMat[]>([]);
  const [dsNguoiLa, setDsNguoiLa] = useState<AnhNguoiLa[]>([]);
  const [dangTai, setDangTai] = useState(false);
  const [dangXuLy, setDangXuLy] = useState(false);
  const [demNguoc, setDemNguoc] = useState(0);
  const [ketQua, setKetQua] = useState<{ ok: boolean; msg: string } | null>(null);
  const [choXoaTen, setChoXoaTen] = useState<string | null>(null);
  const [nhipTai, setNhipTai] = useState(0);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const taiLaiDanhSach = useCallback(() => setNhipTai((n) => n + 1), []);

  useEffect(() => {
    let cancelled = false;
    setDangTai(true);
    Promise.all([
      CameraPlaybackClient.taiDanhSachNguoiQuen(apiPort),
      CameraPlaybackClient.taiDanhSachNguoiLa(apiPort),
    ])
      .then(([people, unknown]) => {
        if (cancelled) return;
        setDsNguoiQuen(people);
        setDsNguoiLa(unknown);
      })
      .finally(() => {
        if (!cancelled) setDangTai(false);
      });
    return () => {
      cancelled = true;
    };
  }, [apiPort, lanTai, nhipTai]);

  const chupTuCameraAnNinh = async (coDemNguoc: boolean) => {
    const ten = tenNguoi.trim();
    if (!ten) {
      setKetQua({ ok: false, msg: 'Hãy nhập tên người quen (ví dụ: Trung, Mẹ...) trước khi chụp.' });
      return;
    }
    if (!luongThat) {
      setKetQua({ ok: false, msg: 'Chưa chọn luồng camera an ninh nào.' });
      return;
    }
    if (dangXuLy || demNguoc > 0) return;

    setKetQua(null);
    if (coDemNguoc) {
      for (let s = 3; s >= 1; s--) {
        setDemNguoc(s);
        await new Promise((r) => setTimeout(r, 1000));
      }
      setDemNguoc(0);
    }

    setDangXuLy(true);
    try {
      const res = await CameraPlaybackClient.themMatTuCamera(ten, luongThat, apiPort);
      setKetQua({ ok: res.ok, msg: res.message });
      onNotice(res.message, res.ok ? 'success' : 'warning');
      if (res.ok) taiLaiDanhSach();
    } finally {
      setDangXuLy(false);
    }
  };

  const handleUploadFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const ten = tenNguoi.trim();
    if (!ten) {
      setKetQua({ ok: false, msg: 'Hãy nhập tên người quen trước khi chọn/chụp ảnh.' });
      return;
    }
    setDangXuLy(true);
    setKetQua(null);
    try {
      const b64 = await compressImageFileToBase64(file);
      const res = await CameraPlaybackClient.themMatTuAnhBase64(ten, b64, apiPort);
      setKetQua({ ok: res.ok, msg: res.message });
      onNotice(res.message, res.ok ? 'success' : 'warning');
      if (res.ok) taiLaiDanhSach();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Không xử lý được ảnh.';
      setKetQua({ ok: false, msg });
      onNotice(msg, 'error');
    } finally {
      setDangXuLy(false);
    }
  };

  const ganTenNguoiLa = async (item: AnhNguoiLa) => {
    const ten = tenNguoi.trim();
    if (!ten) {
      setKetQua({ ok: false, msg: 'Nhập tên ở khung bên trái rồi bấm "Gán tên" trên ảnh người lạ.' });
      return;
    }
    setDangXuLy(true);
    try {
      const res = await CameraPlaybackClient.chuyenNguoiLaThanhQuen(item.filename, ten, apiPort);
      setKetQua({ ok: res.ok, msg: res.message });
      onNotice(res.message, res.ok ? 'success' : 'warning');
      if (res.ok) taiLaiDanhSach();
    } finally {
      setDangXuLy(false);
    }
  };

  const xoaNguoi = async (name: string) => {
    setDangXuLy(true);
    try {
      const res = await CameraPlaybackClient.xoaNguoiQuen(name, apiPort);
      setChoXoaTen(null);
      setKetQua({ ok: res.ok, msg: res.message });
      onNotice(res.message, res.ok ? 'success' : 'warning');
      if (res.ok) taiLaiDanhSach();
    } finally {
      setDangXuLy(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-2.5 lg:h-full lg:min-h-0">
      {/* Cột trái: Form đăng ký khuôn mặt */}
      <div className="lg:col-span-5 flex flex-col gap-3 rounded-2xl border border-[#253459] bg-[#131D33] p-3.5 lg:h-full lg:overflow-y-auto">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <UserPlus size={17} className="text-[#4F8BFF]" />
            <h3 className="text-sm font-bold text-[#E6EDF7]">Thêm khuôn mặt người quen</h3>
          </div>
          <button
            type="button"
            onClick={() => onDocThuChao(tenNguoi.trim() || dsNguoiQuen[0]?.name || 'Trung')}
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#22C55E]/50 bg-[#22C55E]/15 px-2.5 py-1 text-[11px] font-bold text-[#86EFAC] hover:bg-[#22C55E]/25 cursor-pointer"
          >
            <Volume2 size={13} />
            <span>Nghe thử giọng</span>
          </button>
        </div>

        <div className="space-y-1.5">
          <label className="block text-[11px] font-semibold text-[#8A9BB8]">
            Tên người quen (câu chào: &ldquo;Chào mừng [Tên] về nhà!&rdquo;)
          </label>
          <input
            type="text"
            value={tenNguoi}
            onChange={(e) => setTenNguoi(e.target.value)}
            placeholder="Ví dụ: Trung, Mẹ, Bố..."
            className="w-full rounded-xl border border-[#253459] bg-[#0B1220] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#4F8BFF]"
          />
          {dsNguoiQuen.length > 0 && (
            <div className="flex flex-wrap gap-1 pt-0.5">
              {dsNguoiQuen.map((p) => (
                <button
                  key={p.name}
                  type="button"
                  onClick={() => setTenNguoi(p.name)}
                  className={`rounded-lg border px-2 py-0.5 text-[10.5px] font-semibold cursor-pointer transition ${
                    tenNguoi.trim() === p.name
                      ? 'border-[#4F8BFF] bg-[#4F8BFF]/20 text-[#93C5FD]'
                      : 'border-[#253459] bg-[#0E1627] text-[#8A9BB8] hover:text-[#E6EDF7]'
                  }`}
                >
                  + {p.name} ({p.count})
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Cách 1: Chụp từ Camera an ninh */}
        <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3 space-y-2">
          <div className="text-xs font-bold text-[#E6EDF7]">
            Cách 1 · Chụp từ Camera an ninh ({luongThat || 'chưa chọn'})
          </div>
          <p className="text-[11px] text-[#8A9BB8] leading-snug">
            Đứng nhìn thẳng vào camera ở cự ly 1–3m. Nên chụp 3–5 góc khác nhau để nhận diện chính xác hơn.
          </p>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              disabled={dangXuLy || demNguoc > 0 || !luongThat}
              onClick={() => void chupTuCameraAnNinh(false)}
              className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-[#4F8BFF] px-3 py-2 text-xs font-bold text-white hover:bg-[#3B7BFF] disabled:opacity-50 cursor-pointer"
            >
              {dangXuLy ? <Loader2 size={14} className="animate-spin" /> : <Camera size={14} />}
              <span>Chụp ngay</span>
            </button>
            <button
              type="button"
              disabled={dangXuLy || demNguoc > 0 || !luongThat}
              onClick={() => void chupTuCameraAnNinh(true)}
              className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-[#4F8BFF]/50 bg-[#4F8BFF]/15 px-3 py-2 text-xs font-bold text-[#93C5FD] hover:bg-[#4F8BFF]/25 disabled:opacity-50 cursor-pointer"
            >
              <Timer size={14} />
              <span>{demNguoc > 0 ? `Chụp sau ${demNguoc}s…` : 'Đếm ngược 3s'}</span>
            </button>
          </div>
        </div>

        {/* Cách 2: Chụp / chọn ảnh từ điện thoại hoặc máy tính */}
        <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3 space-y-2">
          <div className="text-xs font-bold text-[#E6EDF7]">
            Cách 2 · Chụp / tải ảnh từ điện thoại hoặc máy tính
          </div>
          <p className="text-[11px] text-[#8A9BB8] leading-snug">
            Tự động chuẩn hóa chiều ảnh &amp; nén JPEG gửi trực tiếp tới AI nhận diện trên serverubuntu.
          </p>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            capture="user"
            className="hidden"
            onChange={(e) => void handleUploadFile(e)}
          />
          <button
            type="button"
            disabled={dangXuLy}
            onClick={() => {
              if (!tenNguoi.trim()) {
                setKetQua({ ok: false, msg: 'Hãy nhập tên người quen trước khi chụp/chọn ảnh.' });
                return;
              }
              fileInputRef.current?.click();
            }}
            className="w-full inline-flex items-center justify-center gap-1.5 rounded-xl border border-[#14B8A6]/50 bg-[#14B8A6]/15 px-3 py-2 text-xs font-bold text-[#5EEAD4] hover:bg-[#14B8A6]/25 disabled:opacity-50 cursor-pointer"
          >
            {dangXuLy ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />}
            <span>Chụp / chọn ảnh khuôn mặt</span>
          </button>
        </div>

        {ketQua && (
          <div
            className={`rounded-xl border px-3 py-2 text-xs font-medium ${
              ketQua.ok
                ? 'border-[#22C55E]/40 bg-[#22C55E]/15 text-[#86EFAC]'
                : 'border-[#EF4444]/40 bg-[#EF4444]/15 text-[#FCA5A5]'
            }`}
          >
            {ketQua.msg}
          </div>
        )}
      </div>

      {/* Cột phải: Danh sách người quen + Ảnh người lạ gần đây */}
      <div className="lg:col-span-7 flex flex-col gap-2.5 lg:h-full lg:min-h-0">
        {/* Danh sách người quen */}
        <div className="flex flex-col gap-2 rounded-2xl border border-[#253459] bg-[#131D33] p-3.5 lg:flex-1 lg:min-h-0">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Users size={16} className="text-[#22C55E]" />
              <h3 className="text-xs font-bold text-[#E6EDF7]">
                Người quen đã đăng ký ({dsNguoiQuen.length})
              </h3>
            </div>
            <button
              type="button"
              onClick={taiLaiDanhSach}
              className="inline-flex items-center gap-1 rounded-lg border border-[#253459] bg-[#0E1627] px-2 py-1 text-[11px] text-[#8A9BB8] hover:text-[#E6EDF7] cursor-pointer"
            >
              <RefreshCw size={12} className={dangTai ? 'animate-spin' : ''} />
              <span>Làm mới</span>
            </button>
          </div>

          <div className="flex-1 min-h-0 overflow-y-auto space-y-2 pr-0.5">
            {dsNguoiQuen.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#253459] bg-[#0E1627]/60 py-6 px-3 text-center text-xs text-[#8A9BB8]">
                {dangTai ? 'Đang tải danh sách người quen…' : 'Chưa có khuôn mặt nào được đăng ký.'}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {dsNguoiQuen.map((p) => {
                  const thumb = p.sampleUrl ? CameraPlaybackClient.urlAnhMat(p.sampleUrl, apiPort) : '';
                  const confirming = choXoaTen === p.name;
                  return (
                    <div
                      key={p.name}
                      className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] p-2"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="h-11 w-11 shrink-0 overflow-hidden rounded-xl border border-[#253459] bg-[#0B1220] flex items-center justify-center">
                          {thumb ? (
                            <img src={thumb} alt={p.name} className="h-full w-full object-cover" />
                          ) : (
                            <Users size={18} className="text-[#5F6F8C]" />
                          )}
                        </div>
                        <div className="min-w-0">
                          <div className="text-xs font-bold text-[#E6EDF7] truncate">{p.name}</div>
                          <div className="text-[11px] text-[#8A9BB8]">{p.count} ảnh mẫu</div>
                        </div>
                      </div>

                      {confirming ? (
                        <div className="flex items-center gap-1 shrink-0">
                          <button
                            type="button"
                            disabled={dangXuLy}
                            onClick={() => void xoaNguoi(p.name)}
                            className="rounded-lg bg-[#EF4444] px-2 py-1 text-[10.5px] font-bold text-white cursor-pointer"
                          >
                            Xóa
                          </button>
                          <button
                            type="button"
                            onClick={() => setChoXoaTen(null)}
                            className="rounded-lg border border-[#253459] px-2 py-1 text-[10.5px] text-[#8A9BB8] cursor-pointer"
                          >
                            Hủy
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setChoXoaTen(p.name)}
                          title={`Xóa khuôn mặt ${p.name}`}
                          className="rounded-lg border border-[#EF4444]/30 bg-[#EF4444]/10 p-1.5 text-[#FCA5A5] hover:bg-[#EF4444]/20 cursor-pointer"
                        >
                          <Trash2 size={13} />
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Cách 3: Gán tên từ ảnh người lạ gần đây */}
        <div className="flex flex-col gap-2 rounded-2xl border border-[#253459] bg-[#131D33] p-3.5 lg:flex-1 lg:min-h-0">
          <div className="flex items-center justify-between gap-2">
            <h3 className="text-xs font-bold text-[#E6EDF7]">
              Cách 3 · Ảnh người lạ gần đây ({dsNguoiLa.length})
            </h3>
            <span className="text-[10.5px] text-[#8A9BB8]">
              Nhập tên bên trái rồi bấm &ldquo;Gán tên&rdquo;
            </span>
          </div>

          <div className="flex-1 min-h-0 overflow-y-auto pr-0.5">
            {dsNguoiLa.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#253459] bg-[#0E1627]/60 py-6 px-3 text-center text-xs text-[#8A9BB8]">
                Chưa có ảnh người lạ nào gần đây.
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {dsNguoiLa.map((item) => {
                  const imgUrl = CameraPlaybackClient.urlAnhMat(item.url, apiPort);
                  return (
                    <div
                      key={item.filename}
                      className="rounded-xl border border-[#253459] bg-[#0E1627] overflow-hidden flex flex-col"
                    >
                      <div className="aspect-[4/3] w-full bg-[#0B1220] overflow-hidden">
                        <img
                          src={imgUrl}
                          alt={item.filename}
                          className="h-full w-full object-cover"
                          loading="lazy"
                        />
                      </div>
                      <div className="p-2 flex items-center justify-between gap-1">
                        <span className="text-[10.5px] font-mono-num text-[#8A9BB8]">
                          {item.ts > 0 ? dinhDangGioVn(item.ts) : '—'}
                        </span>
                        <button
                          type="button"
                          disabled={dangXuLy}
                          onClick={() => void ganTenNguoiLa(item)}
                          className="inline-flex items-center gap-1 rounded-lg border px-2 py-0.5 text-[10.5px] font-bold cursor-pointer"
                          style={{
                            borderColor: `${SH.Green}66`,
                            backgroundColor: `${SH.Green}22`,
                            color: '#86EFAC',
                          }}
                        >
                          <Check size={11} />
                          <span>Gán tên</span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
