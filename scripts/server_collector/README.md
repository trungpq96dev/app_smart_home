# server_collector — đọc sức khỏe serverubuntu → MQTT + Điều khiển Docker an toàn

Chạy trong Docker **trên chính serverubuntu**. Đọc CPU/RAM/ổ đĩa/nhiệt độ/mạng
của host bằng `psutil`, đo **CPU% và RAM (MB) của từng Docker container**, rồi publish
lên Mosquitto. App/Web SmartHome (mục **Server**) chỉ subscribe để hiển thị.

## Topic MQTT (chỉ đọc — tuyệt đối không ghi lệnh Docker qua MQTT)

| Topic            | Nội dung |
|------------------|----------|
| `server/info`    | hostname, os, kernel, CPU, RAM tổng, disk tổng, uptime (≈60s/lần) |
| `server/metrics` | cpu%, ram, disk, nhiệt độ, load, mạng Rx/Tx (mỗi `POLL_INTERVAL`) |
| `server/docker`  | danh sách container: `name`, `image`, `state`, `status`, `cpu_percent`, `mem_mb`, `mem_limit_mb`, `mem_percent`, `protected`, `controllable` (≈15s/lần) |
| `server/capnhat` | mốc thời gian cập nhật |

Không bịa số: trường host không cấp (vd nhiệt độ khi không có cảm biến, hoặc container đang dừng) publish `null` → app hiển thị "—".

## Điều khiển Start / Stop / Restart an toàn (HTTP API `:1986`)

Vì Mosquitto chưa bật `acl_file`, `server_collector` **tuyệt đối không subscribe MQTT** để nhận lệnh Docker.
Khi muốn bật nút Start / Stop / Restart từng container từ SmartHome Web (đã khóa thêm PIN 4 số Admin):

1. Tạo token bí mật ngẫu nhiên (≥ 16 ký tự):
   ```bash
   openssl rand -hex 24
   ```
2. Trong `~/server_collector/.env`:
   ```env
   DOCKER_CONTROL_ENABLED=true
   DOCKER_CONTROL_PORT=1986
   DOCKER_CONTROL_TOKEN=<token-vua-tao>
   DOCKER_ALLOWED_CONTAINERS=camera-recorder,beszel,beszel-agent,smarthome-tunnel,smarthome-cockpit-tunnel,wifi-collector,dien-evn
   ```
3. Trong `~/web/.env` (SmartHome Web Gateway):
   ```env
   DOCKER_CONTROL_HOST=192.168.0.50
   DOCKER_CONTROL_PORT=1986
   DOCKER_CONTROL_TOKEN=<cung-token-tren>
   ```
4. Khởi động lại cả 2 dịch vụ:
   ```bash
   cd ~/server_collector && docker compose up -d --build
   cd ~/web && docker compose up -d --build
   ```

### Các lớp bảo vệ cứng
- **Fail-closed allowlist**: Chỉ container liệt kê rõ trong `DOCKER_ALLOWED_CONTAINERS` mới được điều khiển.
- **Bảo vệ cứng container lõi**: `mosquitto`, `server-collector`, `smarthome-web` (và chính container đang chạy) bị chặn cứng trong mã nguồn, không thể ghi đè qua `.env`.
- **Chỉ cho 3 lệnh**: `start`, `stop`, `restart` (cấm `rm`, `exec`, `create`, path traversal).
- **Xác thực nhiều lớp**: SmartHome Web khóa mật khẩu từ xa (4G) + PIN Admin 4 số → gọi `:1986` bằng `Authorization: Bearer <token>` (chống brute-force, khóa IP 60s nếu sai 5 lần).

## Cài trên serverubuntu

```bash
scp -r scripts/server_collector serverubuntu@192.168.0.50:~/
ssh serverubuntu@192.168.0.50
cd ~/server_collector
cp cau_hinh.mau.env .env
nano .env            # điền MQTT_PASS (+ DOCKER_CONTROL_* nếu muốn bật điều khiển)
chmod 600 .env
docker compose up -d --build
docker compose logs -f
```
