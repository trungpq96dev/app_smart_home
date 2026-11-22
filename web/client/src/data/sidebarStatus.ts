import {
  coLoiBom,
  dangKhoaBom,
  bomApDangKhoa,
  phanTichLoiBomNuoc,
  type BomNuocState,
  type TrangThaiKetNoiBom,
} from './bomnuoc.js';
import type { KetNoi } from './dien.js';
import type { DestRoute } from './models.js';
import type { ServerState } from './server.js';
import { SH } from '../theme/colors.js';

/**
 * Tính chấm trạng thái đỏ / vàng cho các mục trong Sidebar (Tự động hóa & Server).
 * Chỉ hiện chấm khi có sự cố hoặc cảnh báo thật; bình thường / chưa cấu hình không hiện.
 */
export function tinhCanhBaoSidebar(params: {
  bomNuoc: BomNuocState | null;
  ketNoiBom: TrangThaiKetNoiBom;
  mqttStatus: KetNoi;
  gatewayOnline: boolean;
  server: ServerState;
}): Partial<Record<DestRoute, string>> {
  const { bomNuoc, ketNoiBom, mqttStatus, gatewayOnline, server } = params;
  const out: Partial<Record<DestRoute, string>> = {};

  const bomDo =
    (bomNuoc !== null &&
      (dangKhoaBom(bomNuoc) ||
        bomApDangKhoa(bomNuoc) ||
        phanTichLoiBomNuoc(bomNuoc.fault)?.mucDo === 'NGUY_HIEM')) ||
    ketNoiBom.state === 'bo_offline' ||
    ketNoiBom.state === 'mat_broker' ||
    ketNoiBom.state === 'sai_tai_khoan' ||
    ketNoiBom.state === 'loi';

  const bomVang =
    !bomDo &&
    ((bomNuoc !== null && coLoiBom(bomNuoc)) ||
      ketNoiBom.state === 'so_cu' ||
      ketNoiBom.state === 'chi_co_broker');

  if (bomDo) out.automation = SH.Red;
  else if (bomVang) out.automation = SH.Amber;

  const cpuTemp = server.metric.cpuTempC;
  const serverDo =
    !gatewayOnline ||
    mqttStatus.state === 'loi' ||
    (server.isRealData && cpuTemp !== null && cpuTemp >= 80);

  const serverVang =
    !serverDo &&
    server.isRealData &&
    ((cpuTemp !== null && cpuTemp >= 75) ||
      server.containers.some((c) => c.state !== 'running'));

  if (serverDo) out.server = SH.Red;
  else if (serverVang) out.server = SH.Amber;

  return out;
}

