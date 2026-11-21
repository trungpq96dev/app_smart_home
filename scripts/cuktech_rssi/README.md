# cuktech_rssi — đo sức sóng BLE của củ sạc

Một việc duy nhất: đọc RSSI của củ sạc CUKTECH từ BlueZ rồi publish lên
`cuktech/rssi/<nguồn>` để app hiện ra. Không điều khiển gì, không ghi gì
xuống thiết bị.

## Vì sao cần nó

Ngày 21/11/2026 củ sạc rớt kết nối 37 lần trong 2 giờ (log
`cuktech-ble-server`: 53 phiên đóng với `reason=link_loss`). Đo ra thì RSSI
tại serverubuntu là **−81 đến −96 dBm**, trong khi cùng củ sạc đó đo từ máy
Windows cạnh nó là **−43 dBm**. Tức là lỗi nằm ở sóng, không ở phần mềm.

Nhưng lúc đó phải SSH vào server mới biết. Script này đưa con số đó lên màn
hình tablet, để lần sau nhìn là biết ngay nên đổ lỗi cho sóng hay cho mã.

## Lấy số ở đâu

`busctl get-property org.bluez /org/bluez/hci0/dev_<MAC> org.bluez.Device1 RSSI`

**Không tự quét BLE.** `cuktech-ble-server` đã quét lại mỗi lần nó nối lại
(vài chục giây một lần khi sóng yếu), nên thuộc tính RSSI trên D-Bus tự tươi.
Bật thêm một phiên discovery nữa là tranh sóng của chính cái dongle đang cố
giữ kết nối.

Hệ quả phải biết: khi kết nối đang **ổn định lâu**, không ai quét, nên số sẽ
cũ dần. Vì vậy gói tin có `tuoi_giay` = bao lâu rồi chưa thấy giá trị mới.

## Thang đo

| dBm | Xếp hạng | Thực tế |
|---|---|---|
| ≥ −65 | tốt | giữ kết nối liên tục |
| −66 … −80 | khá | thỉnh thoảng rớt |
| −81 … −90 | yếu | rớt liên tục |
| < −90 | rất yếu | gần như không nối nổi |

## Gói tin

Topic `cuktech/rssi/server`, **retained**:

```json
{"rssi": -81, "xep_hang": "yeu", "tuoi_giay": 12, "dang_noi": false,
 "luc": "2026-11-21T08:30:00+07:00", "mac": "3C:CD:73:2B:58:8A",
 "nguon": "serverubuntu (dongle USB)", "adapter": "hci0"}
```

Payload rỗng (LWT) = script chết. App phải hiểu đó là "không biết", chứ không
phải "sóng bằng 0".

## Cài trên serverubuntu

Chạy thẳng, **không Docker**: nó cần D-Bus của BlueZ trên host, mà cho một
container thấy D-Bus hệ thống thì phải mở quyền rộng hơn nhiều so với giá trị
của con số này.

```bash
scp -r scripts/cuktech_rssi serverubuntu@192.168.0.50:~/
ssh serverubuntu@192.168.0.50
cd ~/cuktech_rssi
python3 -m venv .venv && .venv/bin/pip install paho-mqtt
cp cau_hinh.mau.env .env && nano .env && chmod 600 .env
.venv/bin/python rssi_mqtt.py        # chạy thử, Ctrl-C để dừng
```

Chạy nền sau mỗi lần khởi động máy — `crontab -e` (không cần sudo):

```
@reboot cd $HOME/cuktech_rssi && ./.venv/bin/python rssi_mqtt.py >> /tmp/cuktech_rssi.log 2>&1
```

## Đo ở nhiều chỗ

Đặt `NGUON_ID` khác nhau cho mỗi điểm đo thì app hiện song song. Firmware
ESP32-C3 trong `firmware/esp32c3_ble_proxy/` publish lên cùng dạng topic với
`NGUON_ID=esp`, nên nhìn hai dòng cạnh nhau là biết dời trạm có ăn thua không.
