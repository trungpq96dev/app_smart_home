# Trạm CUKTECH trên ESP32-C3

ESP32-C3 đặt cạnh củ sạc, **tự nối BLE tới nó**, tự bắt tay MiOT, rồi đẩy số
lên MQTT. Máy chủ Ubuntu không còn dính gì tới Bluetooth nữa — nó chỉ là một
thuê bao MQTT như mọi thứ khác trong nhà.

```
ESP32-C3  ──BLE──  củ sạc CUKTECH 10 Ultra
   │
   └──Wi-Fi/MQTT──  Mosquitto trên serverubuntu  ──  app trên tablet
```

## Vì sao làm thế này

Đo ngày 21/11/2026:

| Chỗ đo | RSSI | Hệ quả |
|---|---|---|
| serverubuntu (dongle USB) | −81 … −96 dBm | 37 lần nối lại trong 2 giờ, 53 phiên `link_loss` |
| máy đặt cạnh củ sạc | −43 dBm | giữ kết nối bình thường |

Máy chủ sau này còn để trên cao, tức là càng xa. Đặt radio cạnh củ sạc là chữa
đúng chỗ. Còn đoạn Wi-Fi từ trạm về máy chủ thì mất gói bao nhiêu cũng chỉ trễ
số vài giây — nó không làm đứt phiên BLE, vì phiên BLE không còn đi qua đó nữa.

Đây là điểm khác hẳn so với cách cho máy chủ **mượn radio** (HCI qua TCP) mà
thư mục này từng chứa: cách đó kéo cả luồng HCI — vốn nhạy với độ trễ từng mili
giây — qua Wi-Fi, nên Wi-Fi chập chờn là BlueZ coi như controller chết. Bản cũ
vẫn nằm trong lịch sử git (commit `2e78d95`) nếu cần xem lại.

## Nó làm được gì

Giữ **nguyên hợp đồng topic** mà `cuktech-ble-server` đang publish, nên app
không phải sửa một dòng nào:

| Topic | Nội dung |
|---|---|
| `cuktech/charger/status` | retained — `connected`, `authenticated`, `ports`, `settings`, `firmware_version` |
| `cuktech/charger/port/<c1\|c2\|c3\|a>` | retained — điện áp, dòng, công suất, giao thức, `enabled` |
| `cuktech/charger/settings` | retained — bảng PIID thô |
| `cuktech/rssi/esp` | retained — sức sóng đo **tại trạm** |
| `cuktech/charger/port` | ← app ghi `{"port":"c1","action":"on"}` |
| `cuktech/charger/set` | ← app ghi `{"piid":5,"value":2}` |
| `cuktech/charger/ble` | ← app ghi `{"enabled":false}` rồi `true` = nối lại |

Một chỗ **thiếu so với bản chạy trên máy chủ**: không có `device_model`. Đọc
được tên model phải đi qua kênh device-info mà firmware này chưa dùng tới, và
bịa một chuỗi vào đó thì còn tệ hơn để trống — app đã xử lý đúng trường hợp
thiếu (nó chỉ không hiện dòng model).

## Nó KHÔNG làm

Lịch sử sạc, biểu đồ, giới hạn dung lượng, ngắt khi đầy — những thứ
`cuktech-ble-server` làm bằng SQLite. Trạm này chỉ là mắt và tay: đọc số, đẩy
lên MQTT, nhận lệnh. Muốn giữ phần lịch sử thì giữ máy chủ nhưng cho nó đọc
MQTT thay vì đọc BLE — đó là việc khác, chưa làm.

## Phần cứng

- Một **ESP32-C3 mini** (ESP32-C3 SuperMini hoặc board C3 bất kỳ có Wi-Fi).
- Nguồn USB 5 V cắm liên tục.
- Đặt trong tầm nhìn thẳng tới củ sạc, 1–3 m. Mục tiêu RSSI tốt hơn −65 dBm;
  app hiện con số này nên cứ cắm rồi nhìn màn hình mà dời.

ESP32-C3 chỉ có Wi-Fi 2.4 GHz, và Wi-Fi với BLE dùng **chung một ăng-ten**.
`sdkconfig.defaults` đã bật phần chia thời gian (`ESP_COEX_SW_COEXIST_ENABLE`)
và tắt chế độ ngủ — đừng tắt hai thứ đó.

## Nạp firmware

Cần [ESP-IDF v5.1 trở lên](https://docs.espressif.com/projects/esp-idf/en/stable/esp32c3/get-started/).
Bản đã dựng được ở đây là **v5.5.6**, cài sẵn trên máy này ở `C:\Espressif`:

```powershell
. 'C:\Espressif\Initialize-Idf.ps1' -IdfId esp-idf-c8b597983fb75feed08f99e44d31e20e
```

(`IdfId` đọc ở `idfSelectedId` trong `C:\Espressif\esp_idf.json` — máy khác thì
số khác.)

```powershell
cd D:\SmartHomeApp\firmware\esp32c3_cuktech
copy main\cau_hinh.mau.h main\cau_hinh.h
notepad main\cau_hinh.h
```

Phải điền:

- `CH_WIFI_SSID` / `CH_WIFI_PASS`
- `CH_MQTT_URI` / `CH_MQTT_PASS`
- **`CH_TOKEN_HEX`** — 24 ký tự hex, lấy ở `ble.token` trong
  `~/cuktech-ble-server/config.yaml` trên serverubuntu:

  ```bash
  ssh serverubuntu@192.168.0.50 \
    "python3 -c \"import yaml;print(yaml.safe_load(open('/home/serverubuntu/cuktech-ble-server/config.yaml'))['ble']['token'])\""
  ```

  Đây là **bí mật thật** — có nó là điều khiển được củ sạc. `cau_hinh.h` đã nằm
  trong `.gitignore`; đừng dán nó vào chat hay commit.

  Không phải `ble_key` (32 ký tự) — cái đó để giải MiBeacon, không dùng cho bắt
  tay này.

```powershell
idf.py set-target esp32c3
idf.py build
idf.py -p COM5 flash monitor
```

## Chuyển từ máy chủ sang trạm

**Phải tắt `cuktech-ble-server` trước.** Hai lý do, lý do thứ hai mới là lý do
thật: hai bên cùng ghi một topic retained thì màn hình nhảy qua lại; và củ sạc
chỉ chấp nhận **một kết nối BLE một lúc** — hai bên tranh nhau thì không bên nào
nối được.

```bash
ssh serverubuntu@192.168.0.50
sudo systemctl stop cuktech-ble-server
sudo systemctl disable cuktech-ble-server

# collector RSSI của máy chủ cũng nên dừng — không còn gì để đo
pkill -f "cuktech_rssi/[.]venv"
crontab -e      # xoá dòng @reboot ... rssi_mqtt.py
```

Rồi cắm điện trạm. Log sẽ chạy:

```
tram: co IP 192.168.0.x
ble: da noi, conn=0
ble: MTU = 517
ble: da noi va bat notify du bon kenh
cuktech: [1/5] khoi tao thiet bi (0xa4)...
cuktech: [4/5] nhan phan hoi cua thiet bi...
cuktech: da dan khoa phien
cuktech: HMAC thiet bi dung
cuktech: [+] xac thuc thanh cong (0x21)
tram: doc lai cai dat: 15/15 PIID
```

Muốn quay lại máy chủ: rút điện trạm, `sudo systemctl enable --now
cuktech-ble-server`.

## Giao thức: lấy ở đâu ra

Toàn bộ phần bắt tay và khung lệnh chép từng bước từ
`src/cuktech_ble/controller.py` và `protocol.py` của
[`cuktech-ble-server`](https://github.com/kairui1108/cuktech-ble-server) (MIT) —
bản đang chạy thật trên serverubuntu, không phải đoán từ tài liệu.

| Việc | File ở đây | Nguồn |
|---|---|---|
| Bắt tay MiOT 5 bước + vòng thách thức | `cuktech.c` | `controller.py:_try_authenticate` |
| HKDF / HMAC / AES-CCM | `mi_crypto.c` | `controller.py:_encrypt` `decrypt` |
| Khung TLV, giải số cổng | `miot.c` | `_build_miot_tlv`, `state_protocol_v2.py` |
| UUID, PIID | `miot.h` | `protocol.py` |

Có mấy bước trông vô nghĩa mà **vẫn phải giữ**: gửi lại 240 byte `0xf2`, hai lần
keepalive ở vòng hai, gửi lại chính gói response của thiết bị. Dự án gốc lấy
chúng từ bản ghi Frida của app Mi Home thật, và thiết bị từ chối đăng nhập nếu
thiếu.

## Những chỗ dễ hỏng nhất

1. **MTU.** `CONFIG_BT_NIMBLE_ATT_PREFERRED_MTU=517` trong `sdkconfig.defaults`.
   Khung trao đổi khoá dài tới ~240 byte, mà MTU 23 mặc định chỉ chở 20 byte.
   Thiếu dòng này thì bắt tay chết ở bước 1 mà không rõ vì sao.
2. **CCCD phải ghi có trả lời.** Theo chuẩn GATT, CCCD chỉ nhận Write Request.
   Ghi kiểu "bắn rồi quên" thì thiết bị âm thầm bỏ qua, notify không bao giờ
   bật, và triệu chứng lại giống hệt sóng yếu.
3. **Token sai.** Năm bước bắt tay chạy trọn, chỉ riêng bước so HMAC lệch. Log
   nói thẳng `HMAC thiet bi KHONG khop` — thấy dòng đó thì đừng đi đo sóng.
4. **Bộ đếm 16 bit.** Thiết bị chỉ gửi 16 bit thấp của bộ đếm chống phát lại.
   Quá 65535 khung mà không tự đếm phần cao thì giải mã hỏng hàng loạt sau vài
   giờ chạy, trong khi kết nối vẫn tốt. `mi_crypto.c` bắt lúc nó quay vòng.

## Đã biên dịch — chưa chạy trên phần cứng

Ngày 22/11/2026, dựng bằng ESP-IDF v5.5.6 trên máy này: **build sạch, không
một cảnh báo nào** (`-Werror=all` đang bật). Ba chỗ phải sửa so với bản viết
hôm 21/11, đều lộ ra ngay khi trình dịch chạm vào:

| Chỗ | Vì sao hỏng |
|---|---|
| `ble_uuid_is16()` | Hàm này **không tồn tại** trong NimBLE. Thay bằng so trực tiếp `chr->uuid.u.type == BLE_UUID_TYPE_16` rồi mới gọi `ble_uuid_u16()`. |
| `ble_svc_gap_init()` / `..._device_name_set()` | Service của **GATT server**, chỉ được biên dịch khi bật vai trò peripheral — mà `sdkconfig.defaults` tắt peripheral. Bỏ hẳn: trạm không quảng bá thì đặt tên cho ai đọc. |
| Phân vùng `factory` 1 MB | Ảnh ra `0x10fa00` (~1.06 MB), tràn 0xfa00. Thêm [`partitions.csv`](partitions.csv) cho app 1.5 MB — vẫn vừa flash 2 MB. |

Kích thước cuối: `0x10fa00` byte, còn **29 %** chỗ trống trong phân vùng.

Biên dịch được **không có nghĩa là giao thức đúng**. Trình dịch không biết gì
về trình tự bắt tay MiOT; phần đó vẫn chỉ mới được đối chiếu logic với bản
Python đang chạy, chưa có con ESP32-C3 nào xác nhận. Lần nạp đầu nên:

```powershell
idf.py -p COM5 flash monitor
```

rồi đọc log theo đúng thứ tự năm bước ở trên. Bước nào dừng thì so với mục
"Những chỗ dễ hỏng nhất".
