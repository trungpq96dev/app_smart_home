# SmartHome Web

Phiên bản chạy trên trình duyệt web của ứng dụng **SmartHome Android**, xây dựng bằng **React + TypeScript + Tailwind CSS + Node.js Gateway** trong thư mục `web/` (không đụng tới mã nguồn Android tại `app/`).

## Kiến trúc & Bảo mật

- **Node.js Gateway (`server/`)**:
  - Giữ kết nối TCP/TLS đến **Mosquitto MQTT Broker** (`dien/#`, `congtac/#`, `cuktech/#`, `server/#`, `wifi/#`) và phát lại qua WebSocket nội bộ `/ws/mqtt`. Áp dụng allowlist nghiêm ngặt khi publish từ web (`smarthome/control`, `wifi/cmd`, `cuktech/charger/port|ble|set` — chặn ghi vào `dien/*`).
  - Giữ kết nối kép **AquaControl Bơm nước (`bomNuocBridge.ts`)**: ưu tiên HTTP LAN tuần tự hoá bằng mutex (nhịp thích ứng 1s/5s qua `/api/state`, `/api/usage`, `/api/cmd`), tự động rơi về **EMQX/MQTT riêng** (`bomnuoc/state`, `bomnuoc/status`, `bomnuoc/usage`, `bomnuoc/cmd`), `CommandBus` chờ bo xác nhận 8 giây, ghi nhật ký bền vững tối đa 100 sự kiện tại `web/data/bom_nuoc_nhat_ky.json`, phát realtime qua `/ws/bomnuoc` và nhận lệnh qua `POST /api/bomnuoc/cmd`.
  - Proxy HTTP & WebSocket tới **go2rtc** (`/go2rtc/*` và `/api/go2rtc/streams`), tự gắn Basic Auth phía server và không bao giờ trả `producers[].url` (chứa mật khẩu camera Tuya) hay mật khẩu MQTT về trình duyệt.
  - Lưu cấu hình dùng chung tại `web/data/config.json` (đã gitignore).
- **Web Client (`client/`)**:
  - Đầy đủ 12 màn hình: Trang chủ, Thiết bị, Chi tiết phòng (kèm 4 sân khấu 2.5D HTML5 Canvas chuyển động: Phòng khách, Phòng ngủ, Nhà bếp gas/bếp từ, Hành lang IR/PIR), Camera, Cảm biến & Sạc Cuktech BLE, Tự động hóa (**tích hợp đầy đủ AquaControl 4 tab**: Bơm nước & Sơ đồ SVG, Bảo vệ & Hiệu chuẩn, Nhật ký & Biểu đồ 12 tháng, Kịch bản), Server, Thống kê (**Điện EVN thật + Nước thật** + các tab mẫu), Chi tiết điện, Quản trị WiFi 5 tab, Tiện ích Lịch Âm Hồ Ngọc Đức, Cài đặt 4 tab (thêm tab **Bơm nước**).
  - **Trợ lý giọng nói Gemini Live**: API Key và lịch sử sử dụng 30 ngày được lưu trong `localStorage` của trình duyệt; Web Audio thu 16kHz PCM và phát 24kHz PCM trực tiếp với `generativelanguage.googleapis.com`; hỗ trợ đọc & điều khiển `bom_nuoc`.
  - **Nguyên tắc "Không bịa số"**: Tuyệt đối không thay số giả khi MQTT điện / công tắc / Cuktech / camera go2rtc / bo bơm nước chưa nối được hoặc thiếu cảm biến (`lvl = -1`, `temp = -127`, `rssi = 0`).

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

Mở trình duyệt tại `http://localhost:3000` (cấu hình MQTT / Camera / Thời tiết trực tiếp tại mục **Cài đặt**).

## Triển khai bằng Docker

```bash
cd web
cp cau_hinh.mau.env .env   # điền IP/mật khẩu nội bộ (tùy chọn)
docker compose up -d --build
```

