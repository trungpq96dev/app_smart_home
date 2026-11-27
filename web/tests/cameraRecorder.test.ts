import { describe, expect, it } from 'vitest';
import {
  CameraTopic,
  camDangGhi,
  congTruNgayVn,
  createEmptyCameraRecorderState,
  dinhDangGioVn,
  doanTiepTheo,
  gopCameraRecorder,
  mocNhaySuKien,
  suKienNguoiTiep,
  suKienNguoiTruoc,
  timViTriPhat,
  type DongThoiGianNgay,
} from '../client/src/data/cameraRecorder.js';

describe('CameraRecorder MQTT state & Timeline Imou/EZVIZ logic (v0.9.4)', () => {
  it('gop trang thai MQTT camera/# chinh xac va khong bia so khi khoi tao', () => {
    let st = createEmptyCameraRecorderState();
    expect(st.online).toBeNull();
    expect(camDangGhi(st, 'cua_chinh')).toBe(false);

    st = gopCameraRecorder(st, CameraTopic.TRANG_THAI, 'online');
    expect(st.online).toBe(true);

    st = gopCameraRecorder(
      st,
      CameraTopic.STATE,
      JSON.stringify({
        recording_enabled: true,
        detect_person_enabled: true,
        telegram_enabled: true,
        drive_enabled: true,
        api_port: 1985,
        cache_used_gb: 3.45,
        cache_max_gb: 10.0,
        pending_uploads: 1,
        uploaded_today: 48,
        uploaded_today_mb: 512.3,
        person_today: 5,
        cameras: {
          cua_chinh: { recording: true, last_segment_ts: 1764050000, error: null },
          san_sau: { recording: false, last_segment_ts: null, error: 'RTSP timeout' },
        },
        cap_nhat: '2026-11-26 21:00:00',
      }),
    );

    expect(st.apiPort).toBe(1985);
    expect(st.cacheUsedGb).toBe(3.45);
    expect(st.uploadedToday).toBe(48);
    expect(st.personToday).toBe(5);
    expect(camDangGhi(st, 'cua_chinh')).toBe(true);
    expect(camDangGhi(st, 'san_sau')).toBe(false);

    st = gopCameraRecorder(
      st,
      CameraTopic.PERSON,
      JSON.stringify({
        event_id: 18,
        camera: 'cua_chinh',
        start_ts: 1764051234,
        thoi_gian: '2026-11-26 13:53:54',
        person_count: 2,
        confidence: 0.92,
      }),
    );
    expect(st.nguoiGanNhat?.eventId).toBe(18);
    expect(st.nguoiGanNhat?.soNguoi).toBe(2);

    st = gopCameraRecorder(st, CameraTopic.TRANG_THAI, 'offline');
    expect(st.online).toBe(false);
    expect(camDangGhi(st, 'cua_chinh')).toBe(false);
  });

  it('timViTriPhat nhay qua khoang trong giua 2 doan video va dinh dang gio UTC+7', () => {
    // 2026-11-25 00:00:00 UTC+7 = 1764003600
    const dayStart = 1764003600;
    expect(dinhDangGioVn(dayStart)).toBe('00:00:00');
    expect(dinhDangGioVn(dayStart + 13 * 3600 + 5 * 60 + 9)).toBe('13:05:09');
    expect(congTruNgayVn('2026-11-26', -1)).toBe('2026-11-25');

    const tl: DongThoiGianNgay = {
      camera: 'cua_chinh',
      ngay: '2026-11-25',
      dayStartTs: dayStart,
      dayEndTs: dayStart + 86400,
      segments: [
        { id: 1, startTs: dayStart + 100, endTs: dayStart + 700, durationSec: 600, sizeBytes: 1000, tier: 'drive' },
        { id: 2, startTs: dayStart + 710, endTs: dayStart + 1310, durationSec: 600, sizeBytes: 1000, tier: 'hot' },
      ],
      events: [
        { id: 10, kind: 'person', startTs: dayStart + 250, endTs: dayStart + 262, maxConf: 0.88, personCount: 1, hasThumb: true },
        { id: 11, kind: 'snapshot', startTs: dayStart + 500, endTs: dayStart + 500, maxConf: 1, personCount: 0, hasThumb: true },
        { id: 12, kind: 'person', startTs: dayStart + 900, endTs: dayStart + 915, maxConf: 0.94, personCount: 2, hasThumb: true },
      ],
    };

    // Nam trong doan 1
    const inSeg1 = timViTriPhat(tl, dayStart + 220);
    expect(inSeg1?.segment.id).toBe(1);
    expect(inSeg1?.offsetMs).toBe(120_000);

    // Roi vao khoang nghi 10 giay (700..710) -> nhay sang dau doan 2
    const inGap = timViTriPhat(tl, dayStart + 705);
    expect(inGap?.segment.id).toBe(2);
    expect(inGap?.offsetMs).toBe(0);
    expect(inGap?.targetEpochSec).toBe(dayStart + 710);

    // Qua cuoi doan cuoi -> lui ve cuoi doan 2 tru 2 giay
    const pastEnd = timViTriPhat(tl, dayStart + 5000);
    expect(pastEnd?.segment.id).toBe(2);
    expect(pastEnd?.targetEpochSec).toBe(dayStart + 1308);

    // Chuyen doan tiep va tim su kien nguoi (bo qua snapshot)
    expect(doanTiepTheo(tl, 1)?.id).toBe(2);
    expect(doanTiepTheo(tl, 2)).toBeNull();
    expect(suKienNguoiTiep(tl, dayStart + 260)?.id).toBe(12);
    expect(suKienNguoiTruoc(tl, dayStart + 900)?.id).toBe(10);
    expect(mocNhaySuKien(tl, tl.events[0])).toBe(dayStart + 247);
  });
});
