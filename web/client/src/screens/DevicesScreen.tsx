import React, { useMemo } from 'react';
import {
  AirVent,
  Fan,
  Flame,
  Lightbulb,
  Lock,
  Unlock,
  Video,
} from 'lucide-react';
import {
  isBridgeSong,
  laCongTacThat,
  trangThaiCongTacCua,
  type CongTacState,
} from '../data/congtac.js';
import { tenCheDoPhong, type RoomUi } from '../data/models.js';
import {
  Dot,
  IconBadge,
  RoomIcon,
  StatusPill,
} from '../components/Common.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

/**
 * Thứ tự hiển thị 6 mục phòng lớn trên lưới 3x2 (khớp 1:1 DevicesScreen.kt trên Tablet).
 * Hàng 1: Phòng ngủ 1, 2, 3. Hàng 2: Nhà bếp, Phòng khách, Hành lang.
 */
export const THU_TU_PHONG = ['bed1', 'bed2', 'bed3', 'kitchen', 'living', 'hall'] as const;

export type DeviceTileIconKey =
  | 'lightbulb'
  | 'air'
  | 'ac'
  | 'flame'
  | 'lock'
  | 'unlock'
  | 'videocam';

export interface TrangThaiMucThietBi {
  ten: string;
  icon: DeviceTileIconKey;
  mau: string;
  bat: boolean;
  chuTrangThai: string;
  that: boolean;
  roRang: boolean;
}

export interface TomTatPhongUi {
  room: RoomUi;
  mucThietBi: TrangThaiMucThietBi[];
  coCongTacThat: boolean;
  soDangBat: number;
}

function mucCongTac(
  roomId: string,
  loai: string,
  ten: string,
  icon: DeviceTileIconKey,
  mau: string,
  giaTriRoom: boolean,
  ct: CongTacState,
): TrangThaiMucThietBi {
  const that = laCongTacThat(ct, roomId, loai);
  const tt = trangThaiCongTacCua(ct, roomId, loai);
  const roRang = !that || (isBridgeSong(ct) && tt?.online === true && tt.bat !== null);
  const giaTriHienThi = that ? tt?.bat === true : giaTriRoom;
  const bat = giaTriHienThi && roRang;
  const chu = !roRang ? '—' : bat ? 'Bật' : 'Tắt';
  return {
    ten,
    icon,
    mau,
    bat,
    chuTrangThai: chu,
    that,
    roRang,
  };
}

export function tomTatPhong(room: RoomUi, ct: CongTacState): TomTatPhongUi {
  let mucThietBi: TrangThaiMucThietBi[];

  if (room.id === 'kitchen') {
    const den = mucCongTac(room.id, 'den', 'Đèn', 'lightbulb', SH.Amber, room.lightOn, ct);
    const hutMuiGoc = mucCongTac(room.id, 'quat', 'Hút mùi', 'air', SH.Blue, room.fanOn, ct);
    const hutMui: TrangThaiMucThietBi =
      hutMuiGoc.roRang && hutMuiGoc.bat
        ? { ...hutMuiGoc, chuTrangThai: `Cấp ${room.hoodSpeed}` }
        : hutMuiGoc;
    const bepTu: TrangThaiMucThietBi = {
      ten: 'Bếp từ',
      icon: 'flame',
      mau: SH.Orange,
      bat: room.stoveOn,
      chuTrangThai: room.stoveOn ? `Mức ${room.stoveLevel}` : 'Tắt',
      that: false,
      roRang: true,
    };
    mucThietBi = [den, hutMui, bepTu];
  } else if (room.id === 'hall') {
    const denGoc = mucCongTac(room.id, 'den', 'Đèn', 'lightbulb', SH.Amber, room.lightOn, ct);
    let den: TrangThaiMucThietBi;
    if (!denGoc.roRang) {
      den = denGoc;
    } else {
      const soDenBat = [
        denGoc.bat,
        room.hallFrontLightOn,
        room.hallSurroundingLightOn,
      ].filter(Boolean).length;
      den = {
        ...denGoc,
        bat: soDenBat > 0,
        chuTrangThai: soDenBat > 0 ? `${soDenBat}/3 bật` : 'Tắt',
      };
    }
    const khoa: TrangThaiMucThietBi = {
      ten: 'Khóa cửa',
      icon: room.hallDoorLocked ? 'lock' : 'unlock',
      mau: room.hallDoorLocked ? SH.Green : SH.Red,
      bat: room.hallDoorLocked,
      chuTrangThai: room.hallDoorLocked ? 'Đã khóa' : 'Đang mở',
      that: false,
      roRang: true,
    };
    const cam: TrangThaiMucThietBi = {
      ten: 'Camera',
      icon: 'videocam',
      mau: room.hallCameraOn ? SH.Blue : SH.TextOff,
      bat: room.hallCameraOn,
      chuTrangThai: !room.hallCameraOn ? 'Tắt' : room.hallIrMode ? 'IR' : 'Bật',
      that: false,
      roRang: true,
    };
    mucThietBi = [den, khoa, cam];
  } else {
    const den = mucCongTac(room.id, 'den', 'Đèn', 'lightbulb', SH.Amber, room.lightOn, ct);
    const quat = mucCongTac(room.id, 'quat', 'Quạt', 'air', SH.Blue, room.fanOn, ct);
    const dhGoc = mucCongTac(room.id, 'dieu_hoa', 'Điều hòa', 'ac', SH.Purple, room.acOn, ct);
    const dieuHoa: TrangThaiMucThietBi = dhGoc.that
      ? dhGoc.roRang && dhGoc.bat
        ? { ...dhGoc, chuTrangThai: `${room.acTemp}°C` }
        : dhGoc
      : {
          ten: 'Điều hòa',
          icon: 'ac',
          mau: SH.Purple,
          bat: room.acOn,
          chuTrangThai: room.acOn ? `${room.acTemp}°C` : 'Tắt',
          that: false,
          roRang: true,
        };
    mucThietBi = [den, quat, dieuHoa];
  }

  return {
    room,
    mucThietBi,
    coCongTacThat: mucThietBi.some((m) => m.that),
    soDangBat: mucThietBi.filter((m) => m.bat).length,
  };
}

export function tomTatDanhSachPhong(rooms: RoomUi[], ct: CongTacState): {
  dsPhong: TomTatPhongUi[];
  tongDangBat: number;
  tongThietBi: number;
} {
  const rank = (id: string) => {
    const idx = (THU_TU_PHONG as readonly string[]).indexOf(id);
    return idx >= 0 ? idx : 99;
  };
  const dsPhong = [...rooms]
    .sort((a, b) => rank(a.id) - rank(b.id))
    .map((r) => tomTatPhong(r, ct));
  const tongDangBat = dsPhong.reduce((sum, p) => sum + p.soDangBat, 0);
  const tongThietBi = dsPhong.length * 3;
  return { dsPhong, tongDangBat, tongThietBi };
}

function DeviceTileIcon({ icon, size = 22 }: { icon: DeviceTileIconKey; size?: number }) {
  switch (icon) {
    case 'lightbulb':
      return <Lightbulb size={size} />;
    case 'air':
      return <Fan size={size} />;
    case 'ac':
      return <AirVent size={size} />;
    case 'flame':
      return <Flame size={size} />;
    case 'lock':
      return <Lock size={size} />;
    case 'unlock':
      return <Unlock size={size} />;
    case 'videocam':
      return <Video size={size} />;
  }
}

function OThietBiNho({ muc }: { muc: TrangThaiMucThietBi }) {
  const nen = muc.bat ? tint(muc.mau, 0.12) : SH.Card2;
  const vien = muc.bat ? tint(muc.mau, 0.35) : SH.Border;
  const chuColor = !muc.roRang ? SH.TextDim : muc.bat ? muc.mau : SH.TextOff;

  return (
    <div
      className="flex flex-col items-center justify-center rounded-[10px] px-1.5 py-2.5 text-center min-w-0 h-full transition-colors"
      style={{
        backgroundColor: nen,
        border: `1px solid ${vien}`,
      }}
    >
      <span
        className="shrink-0"
        style={{ color: muc.bat ? muc.mau : SH.TextOff }}
      >
        <DeviceTileIcon icon={muc.icon} size={22} />
      </span>
      <span className="mt-1.5 text-[11.5px] font-medium text-[#94A3B8] truncate max-w-full">
        {muc.ten}
      </span>
      <span
        className="mt-0.5 text-[13px] font-semibold truncate max-w-full font-mono-num"
        style={{ color: chuColor }}
      >
        {muc.chuTrangThai}
      </span>
    </div>
  );
}

function ThePhong({
  tomTat,
  onClick,
}: {
  tomTat: TomTatPhongUi;
  onClick: () => void;
}) {
  const { room, mucThietBi, coCongTacThat, soDangBat } = tomTat;
  const coThietBiBat = soDangBat > 0;
  const vienMau = coThietBiBat ? tint(room.accent, 0.55) : SH.Border;
  const chipMau = coCongTacThat ? SH.Green : SH.Amber;

  const tenCheDo = tenCheDoPhong(room.id, room.mode);
  const moTa =
    room.scheduleOn && room.id !== 'kitchen'
      ? `Chế độ: ${tenCheDo} • ${room.scheduleTime}`
      : `Chế độ: ${tenCheDo}`;

  return (
    <button
      type="button"
      onClick={onClick}
      className="group w-full h-full text-left rounded-[14px] p-3.5 flex flex-col justify-between gap-3 transition-all hover:bg-[#1A2744] active:scale-[0.99] cursor-pointer min-h-[210px] lg:min-h-0"
      style={{
        backgroundColor: SH.Card,
        border: `1px solid ${vienMau}`,
      }}
    >
      {/* Đầu thẻ: IconBadge + tên phòng & chế độ + chip THẬT/MẪU */}
      <div className="flex items-center gap-2.5 w-full min-w-0 shrink-0">
        <IconBadge
          color={coThietBiBat ? room.accent : SH.TextOff}
          size={44}
          filled={coThietBiBat}
        >
          <RoomIcon icon={room.icon} size={23} />
        </IconBadge>

        <div className="flex-1 min-w-0">
          <div className="text-[15px] sm:text-base font-bold text-[#F1F5F9] truncate">
            {room.name}
          </div>
          <div className="text-xs text-[#94A3B8] truncate mt-0.5">{moTa}</div>
        </div>

        <span
          className="shrink-0 rounded-[5px] px-1.5 py-0.5 text-[11px] font-semibold tracking-wide"
          style={{
            color: chipMau,
            backgroundColor: tint(chipMau, 0.14),
          }}
        >
          {coCongTacThat ? 'THẬT' : 'MẪU'}
        </span>
      </div>

      {/* Giữa thẻ: 3 ô tóm tắt Đèn / Quạt / Điều hòa */}
      <div className="grid grid-cols-3 gap-2 w-full flex-1 min-h-0">
        {mucThietBi.map((muc) => (
          <OThietBiNho key={muc.ten} muc={muc} />
        ))}
      </div>

      {/* Chân thẻ: tổng đang bật + gợi ý bấm để chỉnh */}
      <div className="flex items-center justify-between gap-2 w-full shrink-0">
        <div className="flex items-center gap-1.5 min-w-0">
          <Dot color={coThietBiBat ? SH.Green : SH.TextOff} size={8} />
          <span
            className="text-xs font-medium truncate"
            style={{ color: coThietBiBat ? SH.Green : SH.TextOff }}
          >
            {coThietBiBat ? `${soDangBat}/3 đang bật` : 'Tất cả đang tắt'}
          </span>
        </div>
        <span
          className="text-xs font-semibold shrink-0 transition-transform group-hover:translate-x-0.5"
          style={{ color: room.accent }}
        >
          Vào chỉnh →
        </span>
      </div>
    </button>
  );
}

export function DevicesScreen() {
  const { rooms, congtac, openRoomDetail } = useSmartHome();

  const { dsPhong, tongDangBat, tongThietBi } = useMemo(
    () => tomTatDanhSachPhong(rooms, congtac),
    [rooms, congtac],
  );

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Tóm tắt trên màn điện thoại nhỏ (trên Tablet/Desktop đã nằm ngay trên TopBar như bản Android) */}
      <div className="sm:hidden shrink-0 flex flex-wrap items-center gap-2">
        <StatusPill label={`${dsPhong.length} phòng`} color={SH.Blue} dot={false} />
        <StatusPill
          label={`${tongDangBat}/${tongThietBi} thiết bị đang bật`}
          color={tongDangBat > 0 ? SH.Green : SH.TextOff}
        />
      </div>

      {/* Lưới 3x2 — 6 mục phòng lớn khớp 1:1 với DevicesScreen.kt trên Tablet */}
      <div className="flex-1 min-h-0 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 lg:grid-rows-2 gap-2.5 sm:gap-3">
        {dsPhong.map((tomTat) => (
          <ThePhong
            key={tomTat.room.id}
            tomTat={tomTat}
            onClick={() => openRoomDetail(tomTat.room.id)}
          />
        ))}
      </div>
    </div>
  );
}
