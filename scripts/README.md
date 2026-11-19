# Hướng dẫn cài đặt SmartHome WiFi Collector trên serverubuntu

Script `wifi_collector.py` định kỳ đọc thông tin từ Router TP-Link TL-WR840N (192.168.0.1) và bắn lên Mosquitto MQTT trên serverubuntu để app máy tính bảng hiển thị thông tin thật.

---

## 1. Copy thư mục scripts sang serverubuntu

Từ máy Windows, mở PowerShell chạy lệnh:
```bash
ssh serverubuntu@192.168.0.50 "mkdir -p ~/wifi"
scp scripts/wifi_collector.py serverubuntu@192.168.0.50:~/wifi/
scp scripts/smarthome-wifi.service serverubuntu@192.168.0.50:~/wifi/
```

## 2. Cài đặt thư viện Python trên serverubuntu

Đăng nhập SSH vào serverubuntu:
```bash
ssh serverubuntu@192.168.0.50
```

Cài đặt paho-mqtt:
```bash
sudo apt update
sudo apt install -y python3 python3-pip
pip3 install paho-mqtt
```

## 3. Chạy thử nghiệm script

Chạy thử trực tiếp:
```bash
cd ~/wifi
python3 wifi_collector.py
```
*(Nếu mật khẩu admin của router khác mặc định, sửa trong file `wifi_collector.py` hoặc truyền biến môi trường `ROUTER_PASS="mat_khau_cua_ban"`)*

Khi thấy log in ra:
```
[INFO] Đã kết nối Mosquitto tại 127.0.0.1:1883
[INFO] Cập nhật thành công: SSID=..., Thiết bị=...
```
tức là đã kết nối thành công!

## 4. Cài đặt chạy tự động vĩnh viễn (systemd service)

```bash
sudo cp ~/wifi/smarthome-wifi.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable smarthome-wifi
sudo systemctl start smarthome-wifi
```

Kiểm tra trạng thái dịch vụ:
```bash
sudo systemctl status smarthome-wifi
```

Xem log theo thời gian thực:
```bash
journalctl -u smarthome-wifi -f
```
