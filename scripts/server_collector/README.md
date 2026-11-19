# server_collector — đọc sức khỏe serverubuntu → MQTT

Chạy trong Docker **trên chính serverubuntu**. Đọc CPU/RAM/ổ đĩa/nhiệt độ/mạng
của host bằng `psutil`, liệt kê container qua Docker socket (chỉ đọc), rồi publish
lên Mosquitto. App SmartHome (mục **Server** trong sidebar) chỉ subscribe.

## Topic (app chỉ đọc, không ghi)

| Topic            | Nội dung |
|------------------|----------|
| `server/info`    | hostname, os, kernel, CPU, RAM tổng, disk tổng, uptime (≈60s/lần) |
| `server/metrics` | cpu%, ram, disk, nhiệt độ, load, mạng Rx/Tx (mỗi `POLL_INTERVAL`) |
| `server/docker`  | danh sách container: name, image, state, status (≈60s/lần) |
| `server/capnhat` | mốc thời gian cập nhật |

Không bịa số: trường host không cấp (vd nhiệt độ khi không có cảm biến) publish
`null` → app hiển thị "—".

## Cài trên serverubuntu

```
scp -r scripts/server_collector serverubuntu@192.168.0.50:~/
ssh serverubuntu@192.168.0.50
cd ~/server_collector
cp cau_hinh.mau.env .env
nano .env            # điền MQTT_PASS (tài khoản iot, lấy từ ~/homeserver/.env)
chmod 600 .env
docker compose up -d --build
docker compose logs -f
```

Kiểm tra broker đã có số thật:

```
mosquitto_sub -h 192.168.0.50 -u iot -P <pass> -t 'server/#' -v
```

Đối chiếu: `free -h` (RAM), `df -h` (ổ đĩa), `uptime`, `docker ps`.

## Ghi chú

- Chạy `user: 0:0` (root) để đọc `/sys` (nhiệt độ) và Docker socket. Mọi mount
  đều **read-only** và `no-new-privileges`.
- Không muốn lộ Docker socket? Bỏ dòng mount `docker.sock` trong
  `docker-compose.yml` — thẻ Docker trên app sẽ trống, phần còn lại vẫn chạy.
- Read-only: collector **chỉ publish**, không nhận lệnh điều khiển nào.
