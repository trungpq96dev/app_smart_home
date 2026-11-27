# beszel — Giám sát sức khỏe & lịch sử tài nguyên `serverubuntu`

Chạy song song và độc lập với `server_collector` (collector kia đẩy MQTT `server/#` cho
máy tính bảng treo tường, còn **Beszel** mở dashboard web lưu lịch sử biểu đồ CPU, RAM,
ổ đĩa, nhiệt độ, mạng, thống kê từng Docker container, cảnh báo và S.M.A.R.T ổ SSD).

## Kiến trúc trên `serverubuntu`

- **Beszel Hub** (`henrygd/beszel:latest`): mở giao diện web tại `http://192.168.0.50:8090`.
- **Beszel Agent** (`henrygd/beszel-agent:alpine`): chạy `network_mode: host`, giao tiếp nội bộ
  với Hub qua Unix socket `/beszel_socket/beszel.sock` (không cần mở thêm cổng TCP ra ngoài).

## Cài đặt / Cập nhật trên `serverubuntu`

```bash
scp -r scripts/beszel serverubuntu@192.168.0.50:~/
ssh serverubuntu@192.168.0.50
cd ~/beszel
cp cau_hinh.mau.env .env
chmod 600 .env

# Khởi chạy Hub trước để sinh khóa SSH tại beszel_data/id_ed25519
docker compose up -d beszel

# Trích khóa công khai của Hub vào .env rồi bật cả cụm
docker run --rm --entrypoint /bin/cat -v "$PWD/beszel_data:/d:ro" henrygd/beszel-agent:alpine /d/id_ed25519 > .tmp_key
chmod 600 .tmp_key && PUB_KEY=$(ssh-keygen -y -f .tmp_key) && rm -f .tmp_key
sed -i "s|^KEY=.*|KEY=\"${PUB_KEY}\"|" .env
docker compose up -d
```

## Khởi tạo lần đầu trên trình duyệt

1. Mở `http://192.168.0.50:8090` và tạo tài khoản quản trị (email + mật khẩu).
2. Bấm **Add System** → chọn tab **Docker** / điền:
   - **Name**: `serverubuntu`
   - **Host / IP**: `/beszel_socket/beszel.sock`
3. Bấm **Add system** — số liệu CPU, RAM, SSD, nhiệt độ, mạng, Docker và S.M.A.R.T sẽ lên ngay.

## Ghi chú bảo mật & phần cứng

- `docker.sock` được mount `:ro` (chỉ đọc) để thống kê CPU/RAM từng container.
- `/dev/sda` + `SYS_RAWIO` dùng để `smartctl` đọc sức khỏe S.M.A.R.T của SSD SATA (`Colorful SL300`).
  Nếu không cần S.M.A.R.T, có thể bỏ mục `devices` và `cap_add` trong `docker-compose.yml`.
- Không mở cổng `8090` trực tiếp ra Internet nếu chưa đứng sau reverse proxy HTTPS.
