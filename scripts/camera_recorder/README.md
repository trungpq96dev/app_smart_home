# Hướng dẫn cài đặt Đầu ghi Camera 24/24 + Google Drive 5TB + AI Phát hiện người

Dịch vụ `camera_recorder` chạy hoàn toàn tự động trên `serverubuntu` (`192.168.0.50`):
- **Ghi 24/24** không cần chạm vào máy tính bảng (cắt đoạn 10 phút, chống hỏng file khi mất điện, giãn cách thông minh tránh Tuya khóa 4 phút).
- **Hot Cache 10 GB** trên ổ cứng `serverubuntu` để xem lại các ngày gần nhất tức thì (< 0,5 giây), đồng thời đẩy nền toàn bộ lên **Google Drive 5TB cá nhân (`@gmail.com`)** qua `rclone`.
- **AI phát hiện người 2 tầng nhẹ trên CPU** (lọc chuyển động -> YOLOv8n ONNX) + bắn ảnh khoanh người ngay về **Telegram**.
- **HTTP Playback API (`:1985`) & MQTT (`camera/#`)** phục vụ giao diện **Xem lại Timeline + nhảy tới mốc có người** kiểu Imou/EZVIZ trên tablet.

---

## Bước 1: Kiểm tra cổng RTSP trên `go2rtc.yaml`

Trên `serverubuntu`, mở file `go2rtc.yaml` và đảm bảo có bật nhánh `rtsp:` (mặc định cổng `8554`):

```yaml
rtsp:
  listen: ":8554"
```

Nếu vừa sửa `go2rtc.yaml`, khởi động lại container `go2rtc`.

---

## Bước 2: Cấp quyền Google Drive 5TB cá nhân (`@gmail.com`) cho `rclone`

> Với tài khoản Gmail cá nhân, **phải dùng OAuth2 Desktop** (như hướng dẫn dưới), không dùng Service Account vì Service Account không có hạn mức ổ đĩa riêng trên Gmail cá nhân.

1. *(Khuyên dùng để đạt tốc độ tối đa, không bị dùng chung hạn mức API của rclone)*: Vào Google Cloud Console -> tạo **OAuth Client ID** loại **Desktop app** (bật **Google Drive API**) để lấy `client_id` và `client_secret`.
2. Trên **máy Windows có trình duyệt**, tải `rclone.exe` rồi chạy:
   ```powershell
   rclone config
   ```
   - `n` (New remote) -> đặt tên đúng là **`gdrive`**
   - Storage: chọn **`drive`** (Google Drive)
   - Điền `client_id` và `client_secret` (hoặc nhấn Enter dùng mặc định)
   - Scope: chọn **`1`** (`drive` — toàn quyền đọc/ghi/xóa file cũ do đầu ghi tạo)
   - `Use auto config?` -> chọn **`y`**, trình duyệt mở ra -> đăng nhập tài khoản Gmail 5TB và bấm **Allow**.
   - `Configure this as a Shared Drive?` -> chọn **`n`** (vì là Gmail cá nhân).
3. Lấy đường dẫn file cấu hình vừa sinh trên Windows:
   ```powershell
   rclone config file
   ```
   Copy file `rclone.conf` đó sang thư mục `~/camera_recorder/rclone.conf` trên `serverubuntu`.

---

## Bước 3: Lấy Bot Token & Chat ID của Telegram

1. Mở Telegram tìm **`@BotFather`** -> gửi `/newbot` -> nhận **`TG_BOT_TOKEN`**.
2. Tạo 1 nhóm riêng (hoặc nhắn tin trực tiếp với bot vừa tạo, bấm **Start**).
3. Mở trình duyệt truy cập `https://api.telegram.org/bot<TG_BOT_TOKEN>/getUpdates` sau khi nhắn 1 câu vào nhóm/bot để lấy số **`id`** trong `"chat":{"id": ...}` điền vào **`TG_CHAT_ID`**.

---

## Bước 4: Đẩy lên `serverubuntu` và khởi chạy

Từ máy Windows (PowerShell tại thư mục gốc dự án):

```powershell
scp -r scripts/camera_recorder serverubuntu@192.168.0.50:~/
scp <duong_dan_tới>\rclone.conf serverubuntu@192.168.0.50:~/camera_recorder/rclone.conf
```

SSH vào `serverubuntu`:

```bash
ssh serverubuntu@192.168.0.50
cd ~/camera_recorder
mkdir -p data
sudo chown -R 10002:10002 data rclone.conf
chmod 600 rclone.conf

cp cau_hinh.mau.env .env
nano .env
# Điền: GO2RTC_USER, GO2RTC_PASS, TG_BOT_TOKEN, TG_CHAT_ID, MQTT_PASS (lấy từ ~/server_collector/.env)
chmod 600 .env

docker compose up -d --build
docker compose logs -f
```

Kiểm tra nhanh trên `serverubuntu`:
```bash
curl -u <user>:<pass> http://127.0.0.1:1985/api/health
mosquitto_sub -h 127.0.0.1 -u iot -P <pass> -t "camera/#" -v
```

Trên máy tính bảng **SmartHomeApp**: vào **Cài đặt -> Dịch vụ & Kết nối -> cột Camera**, cổng ghi hình mặc định đã là **`1985`** (dùng chung địa chỉ `192.168.0.50` và tài khoản `go2rtc`). Mở màn **Camera -> Xem lại** là thấy ngay thanh dòng thời gian và danh sách sự kiện có người!
