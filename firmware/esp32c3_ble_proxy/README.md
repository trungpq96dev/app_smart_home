# Trạm trung chuyển BLE cho củ sạc CUKTECH (ESP32-C3)

Cho máy chủ **mượn radio Bluetooth** của một con ESP32-C3 đặt cạnh củ sạc, qua
Wi-Fi. Máy chủ vẫn chạy nguyên `cuktech-ble-server` như cũ — nó không biết là
radio đã dời chỗ.

## Vì sao

Đo ngày 21/11/2026:

| Chỗ đo | RSSI | Hệ quả |
|---|---|---|
| serverubuntu (dongle USB) | **−81 … −96 dBm** | 37 lần nối lại trong 2 giờ, 53 phiên đóng với `reason=link_loss` |
| máy Windows cạnh củ sạc | **−43 dBm** | giữ kết nối bình thường |

Phần mềm không sai — máy chủ đứng quá xa. Dời cái radio lại gần là chữa đúng
chỗ, còn mọi thứ khác giữ nguyên.

## Nó KHÔNG làm gì

Trạm **không hiểu** giao thức của củ sạc. Nó chuyển tiếp HCI — đúng thứ ngôn
ngữ mà BlueZ nói với một con dongle USB. Nhờ vậy toàn bộ bắt tay MiOT (HKDF +
HMAC + AES-CCM), đọc PIID, bật tắt cổng… vẫn nằm trong `cuktech-ble-server`,
không phải chép lại một dòng nào sang C.

```
BlueZ (hci0)  <->  btattach -P h4  <->  pty  <->  hci_tcp_pty.py
                                                        |  TCP qua Wi-Fi
                                                  ESP32-C3  <->  VHCI  <->  radio BLE
                                                                               |
                                                                          củ sạc CUKTECH
```

## Hai chế độ

| Khi nào | Trạm làm gì |
|---|---|
| Chưa ai nối vào cổng TCP | Tự quét BLE, publish RSSI lên `cuktech/rssi/esp` |
| Có máy chủ nối vào | Nhường hẳn radio cho máy chủ, chỉ soi các gói quảng bá đi qua để vẫn báo được RSSI |

Chế độ 1 là để **thử chỗ đặt trước khi đấu nối**: cắm điện, mở app, nhìn con
số. Chỗ nào ra ≥ −65 dBm thì đặt ở đó.

## Phần cứng

- Một **ESP32-C3 mini** (ESP32-C3 SuperMini, hoặc bất kỳ board C3 nào có Wi-Fi).
- Nguồn USB 5V cắm liên tục. Không cần nối dây gì vào máy chủ — tất cả đi qua
  Wi-Fi.
- Đặt trong **tầm nhìn thẳng** tới củ sạc, cách 1–3 m là lý tưởng. Tránh để
  sau tủ sắt hoặc sát bộ nguồn.

> ESP32-C3 chỉ có Wi-Fi 2.4 GHz, và Wi-Fi với BLE dùng **chung một ăng-ten**.
> `sdkconfig.defaults` đã bật phần chia thời gian (`ESP_COEX_SW_COEXIST_ENABLE`)
> và tắt chế độ ngủ của Wi-Fi — đừng tắt hai thứ đó.

## Nạp firmware

Cần [ESP-IDF v5.1 trở lên](https://docs.espressif.com/projects/esp-idf/en/stable/esp32c3/get-started/).

```powershell
cd D:\SmartHomeApp\firmware\esp32c3_ble_proxy
copy main\cau_hinh.mau.h main\cau_hinh.h
notepad main\cau_hinh.h
```

Điền SSID, mật khẩu Wi-Fi, địa chỉ MQTT và mật khẩu MQTT. File `cau_hinh.h`
nằm trong `.gitignore` — đừng commit nó.

```powershell
idf.py set-target esp32c3
idf.py build
idf.py -p COM5 flash monitor
```

Thấy dòng này là xong:

```
tram_ble: co IP 192.168.0.123
tram_ble: dang nghe HCI tren cong 9000
tram_ble: dang tu quet de do suc song
tram_ble: tram san sang
```

Ghi lại cái IP đó.

## Bước 1 — thử chỗ đặt (chưa đụng gì tới máy chủ)

Cắm trạm ở chỗ định đặt rồi mở màn **Cảm biến** trong app. Khi củ sạc mất kết
nối, thẻ cảnh báo sẽ hiện hai dòng:

```
serverubuntu (dongle USB): −94 dBm · Rất yếu
ESP32-C3 (cạnh củ sạc): −48 dBm · Tốt
```

Chênh nhau như vậy nghĩa là dời radio sẽ ăn thua. Còn nếu cả hai đều yếu thì
chỗ đặt chưa đúng — dời trạm rồi xem lại, chưa cần làm bước 2.

## Bước 2 — cho máy chủ mượn radio

Trên serverubuntu:

```bash
scp -r firmware/esp32c3_ble_proxy/server serverubuntu@192.168.0.50:~/esp_ble/
ssh serverubuntu@192.168.0.50
python3 ~/esp_ble/hci_tcp_pty.py 192.168.0.123 --lien-ket /tmp/ttyESPBT
```

Nó in ra đường dẫn pty rồi đứng đó. Mở một cửa sổ khác:

```bash
sudo btattach -B /tmp/ttyESPBT -P h4
hciconfig -a        # phải thấy thêm một adapter nữa
```

**Rút con dongle USB cũ ra.** `cuktech-ble-server` không có chỗ chọn adapter —
nó dùng cái mặc định của BlueZ. Rút dongle cũ thì trạm thành adapter duy nhất
và server tự dùng nó, không phải sửa một dòng cấu hình nào.

Rồi khởi động lại dịch vụ:

```bash
sudo systemctl restart cuktech-ble-server
```

Xem log: `tail -f /proc/$(pgrep -f ha_server.py)/root/tmp/cuktech_server.log`

### Cho nó tự chạy sau khi khởi động máy

`hci_tcp_pty.py` không cần quyền root, `btattach` thì cần. Cách gọn nhất là
hai unit systemd — xem `server/systemd/` nếu bạn muốn dựng; còn tạm thời thì
`crontab -e` cho cái thứ nhất và `/etc/rc.local` cho cái thứ hai cũng đủ.

## Khi nào thì KHÔNG nên dùng cách này

- **Wi-Fi nhà yếu ở chỗ đặt trạm.** HCI chịu được độ trễ vài chục ms, nhưng
  Wi-Fi chập chờn thì BlueZ coi là controller chết và reset adapter. Đổi chỗ
  hoặc thêm một điểm phát Wi-Fi trước.
- **Chỉ cần chữa nhanh.** Dời hẳn máy chủ lại gần, hoặc nối dongle USB qua dây
  nối dài kéo về phía củ sạc, rẻ hơn và không thêm mắt xích nào.
- **Dongle cũ mới là thủ phạm.** Con đang dùng là CSR clone (`0a12:0001`,
  hãng khai `Barrot Technology`) — loại nổi tiếng chập chờn với BLE trên Linux.
  Thay bằng RTL8761B (TP-Link UB500) hết vài trăm nghìn, thử trước cũng đáng.

## Giới hạn đã biết

- **Một host một lúc.** Trạm từ chối client thứ hai. Hai host cùng điều khiển
  một controller là hỏng ngay.
- **Trạm mất điện = mất adapter.** `hci_tcp_pty.py` tự nối lại, nhưng BlueZ có
  thể cần `btattach` chạy lại. Đây là cái giá của việc kéo HCI qua mạng.
- **Chưa đo trên thiết bị thật.** Toàn bộ phần firmware và `hci_tcp_pty.py`
  trong thư mục này **chưa được chạy thử trên phần cứng** — chưa có con
  ESP32-C3 nào trong tay lúc viết. Mã được viết theo đúng tài liệu VHCI của
  ESP-IDF và khuôn gói HCI H4, nhưng lần nạp đầu tiên nên theo dõi
  `idf.py monitor` và log của BlueZ (`sudo btmon`) để bắt lỗi sớm.
