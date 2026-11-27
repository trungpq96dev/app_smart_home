# SmartHome Web (v0.9.4-web · Chuẩn PWA iPhone & Desktop)

Phiên bản chạy trên trình duyệt web và **PWA trên iPhone / iPad (iOS Safari)** của ứng dụng **SmartHome Android v0.9.4**, xây dựng bằng **React + TypeScript + Tailwind CSS + Node.js Gateway** trong thư mục `web/` (không đụng tới mã nguồn Android tại `app/`).

## Kiến trúc & Bảo mật

- **Node.js Gateway (`server/`)**:
  - Giữ kết nối TCP/TLS đến **Mosquitto MQTT Broker** (`dien/#`, `congtac/#`, `cuktech/#`, `server/#`, `wifi/#`, `camera/#`) và phát lại qua WebSocket nội bộ `/ws/mqtt`. Áp dụng allowlist nghiêm ngặt khi publish từ web (`smarthome/control`, `wifi/cmd`, `camera/cmd`, `cuktech/charger/port|ble|set` — chặn ghi vào `dien/*`).
  - Giữ kết nối kép **AquaControl Bơm nước (`bomNuocBridge.ts`)**: ưu tiên HTTP LAN tuần tự hoá bằng mutex (nhịp thích ứng 1s/5s qua `/api/state`, `/api/usage`, `/api/cmd`), tự động rơi về **EMQX/MQTT riêng** (`bomnuoc/state`, `bomnuoc/status`, `bomnuoc/usage`, `bomnuoc/cmd`), `CommandBus` chờ bo xác nhận 8 giây, ghi nhật ký bền vững tối đa 100 sự kiện tại `web/data/bom_nuoc_nhat_ky.json`, phát realtime qua `/ws/bomnuoc` và nhận lệnh qua `POST /api/bomnuoc/cmd`.
  - Proxy HTTP & WebSocket tới **go2rtc** (`/go2rtc/*` và `/api/go2rtc/streams`, cổng `1984`) và **Đầu ghi Camera 24/24** (`/api/camera/recorder/*`, cổng `1985` — hỗ trợ tua video `HTTP Range 206 Partial Content`, kéo đoạn ghi trên Google Drive 5TB qua `rclone`, ảnh chụp nhanh & thumbnail sự kiện người), tự gắn Basic Auth phía server và không bao giờ trả `producers[].url` (chứa mật khẩu camera Tuya) hay mật khẩu MQTT về trình duyệt.
  - Lưu cấu hình dùng chung tại `web/data/config.json` (đã gitignore).
- **Web Client & iPhone PWA (`client/`)**:
  - Đầy đủ 12 màn hình đồng bộ với bản Android `v0.9.4`: Trang chủ (`RoomPager` vuốt ngang, đồng hồ Âm lịch Can-Chi, dải WiFi gia đình), Thiết bị (lưới 3×2 ngữ cảnh phòng), Chi tiết phòng (kèm 4 sân khấu 2.5D HTML5 Canvas phối cảnh 1 điểm tụ: Phòng ngủ, Phòng khách, Nhà bếp đảo đá/bếp từ/khói, Hành lang biệt thự 2 tầng 3 góc camera + IR + Radar PIR), Camera (**Trực tiếp WebRTC/MSE + Xem lại 24/24 Timeline Imou/EZVIZ + Sự kiện phát hiện người**), Cảm biến & Sạc CUKTECH 10 Ultra BLE (6 cài đặt + hẹn giờ cổng sạc + dải phân bổ công suất), Tự động hóa (**AquaControl 4 tab**), Server (CPU/RAM/Disk/Nhiệt độ + RAPL + Pin/UPS), Thống kê (**Điện EVN thật + Nước thật** + đồng hồ bán nguyệt 180°), Chi tiết điện (lọc khoảng ngày), Quản trị WiFi 5 tab, Tiện ích Lịch Vạn Niên 42 ô, Cài đặt 5 tab.
  - **Tối ưu cho iPhone (Safari & Add to Home Screen)**: `manifest.webmanifest`, `apple-mobile-web-app-capable`, safe-area tai thỏ / Dynamic Island / thanh Home, chống tự động phóng to khi chạm ô nhập liệu, thanh điều hướng đáy cho điện thoại, mở khóa WebAudio cho Trợ lý Gemini Live trên iOS và giữ sáng màn hình đêm bằng Screen Wake Lock API.
  - **Nguyên tắc "Không bịa số"**: Tuyệt đối không thay số giả hoặc hiển thị công tắc ở trạng thái TẮT khi MQTT điện / công tắc / Cuktech / camera go2rtc / bo bơm nước chưa nối được hoặc thiếu cảm biến — luôn hiện `—` và ghi rõ nguyên nhân.

## Cài lên Màn hình chính iPhone (Như ứng dụng thật)

1. Kết nối iPhone vào mạng WiFi gia đình (hoặc Tailscale về `serverubuntu`).
2. Mở **Safari** truy cập `http://192.168.0.50:3080` (cổng `3080` để tránh đụng Grafana ở cổng `3000`).
3. Bấm nút **Chia sẻ (Share)** ở thanh dưới Safari → chọn **Thêm vào Màn hình chính (Add to Home Screen)** → **Thêm**.

## Chạy chế độ phát triển (Development)

```bash
cd web
npm install
npm run dev
```

- Gateway chạy tại `http://127.0.0.1:3000`
- Vite Dev Server chạy tại `http://127.0.0.1:5173` (tự proxy `/api`, `/ws`, `/go2rtc` về port 3000)

## Kiểm thử & Build Production

```bash
cd web
npm test
npm run typecheck
npm run build
npm start
```

## Triển khai bằng Docker

```bash
cd web
cp cau_hinh.mau.env .env   # điền IP/mật khẩu nội bộ (tùy chọn)
docker compose up -d --build
```
