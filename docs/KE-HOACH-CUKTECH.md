# Kế hoạch: đưa củ sạc CUKTECH 10 Ultra lên màn hình SmartHome

Soạn 20/11/2026 từ tài liệu `k_ho_ch_t_ch_h_p_cuktech_10_ultra.md`, đã đối chiếu với mã nguồn
thật của app. Mọi khẳng định về mã nguồn trong file này đều kèm đường dẫn và số dòng để kiểm lại.

---

## 1. Tóm tắt

| | |
|---|---|
| Mục tiêu | Hiện công suất/điện áp/dòng từng cổng của củ sạc CUKTECH 10 Ultra lên tablet |
| Đường đi chốt | Củ sạc → BLE GATT → `cuktech-ble-server` (Docker trên serverubuntu) → Mosquitto `cuktech/charger/#` → `MqttDien.kt` → màn Cảm biến |
| Khác tài liệu gốc | Bỏ FastAPI/REST/WebSocket; bỏ ESP32 + OpenMQTTGateway; bỏ `xiaomi-ble`; widget là Compose |
| Việc không phải làm nữa | Không tự viết collector, không tự dịch ngược giao thức — đã có dự án MIT làm xong (mục 1c) |
| Việc làm trước | Lấy token Xiaomi, và kiểm serverubuntu có Bluetooth trong tầm sóng không (mục 1c) |

---

## 1b. Kết quả đo Giai đoạn 0 — 21/11/2026

Đo từ máy Windows bằng adapter Bluetooth sẵn có, không cần ESP32 cũng không cần ra server.
Công cụ và số liệu thô: [`scripts/cuktech_g0/`](../scripts/cuktech_g0/README.md).

| Phép đo | Kết quả |
|---|---|
| Quét 20 giây | 30 thiết bị BLE, đúng **một** con phát `FE95`: `3C:CD:73:2B:58:8A`, tên `njcuk.fitting.ad1204`, RSSI −59 |
| Bám theo 90 giây | 48 gói **giống hệt nhau từng byte**, frame counter không tăng |
| Đọc khung | 11 byte = frame control + product id `0x660E` + counter + MAC. Cờ "object" **tắt** → không có chỗ nào cho số liệu |
| Nối GATT (chỉ đọc) | Nối được, không cần ghép đôi. Firmware `2.1.2_0073`. Có service riêng **`AF00`**: `af07` (write) + `af08` (notify) |
| Nghe `af08` 45 giây | **0 gói** — thiết bị không tự đẩy số liệu |

**Kết luận tạm:** nhánh MiBeacon của tài liệu gốc **không đi được** — quảng bá không chở số
liệu nên có bindkey cũng không có gì để `xiaomi-ble` giải. Số liệu nằm sau một phiên GATT đã
xác thực — mục 1c cho biết chính xác ở đâu, và ai đã làm xong việc đó.

**Ba thứ chưa kiểm được, cần tay người** (hướng dẫn kèm lệnh ở README của `scripts/cuktech_g0/`):

1. Con `3C:CD:73:2B:58:8A` có **đúng là củ sạc** không — rút điện rồi quét lại là biết.
2. Lúc **đang sạc** quảng bá có khác không — các phép đo trên làm lúc không rõ có tải hay không.
3. Bindkey — cần thêm củ sạc vào Mi Home vùng Trung Quốc.

Chưa ghi một byte nào vào thiết bị: toàn bộ phép đo trên đều chỉ đọc.

---

## 1c. Đã có người làm xong — 21/11/2026

Dự án **[kairui1108/cuktech-ble-server](https://github.com/kairui1108/cuktech-ble-server)**,
giấy phép MIT, Python, có test và golden fixture, có Docker và systemd. Chuyên trị đúng
CUKTECH 10 Ultra.

**Nó khớp với số tôi đo được đến từng characteristic:**

| Dự án đó dùng | Máy này đo thấy trên `3C:CD:73:2B:58:8A` |
|---|---|
| `UUID_FE95` service | có |
| `CHAR_AUTH_CTRL = 0x0010` | có |
| `CHAR_AUTH_DATA = 0x0019` | có |
| `CHAR_CMD_SEND = 0x001a` | có |
| `CHAR_CMD_RECV = 0x001b` (notify) | có |
| `CHAR_DEVICE_INFO = 0x001c` | có |
| `CHAR_FW_VERSION = 0x0004` | có, đọc ra `2.1.2_0073` |

Một dự án anh em (`Dream-Architect1026/cuktech-ble-server`) ghi model đã test là
`njcuk.fitting.ad1204` firmware `2.1.2_0073` — **đúng tên và đúng firmware máy này bắt được**.
Vậy là trả lời xong câu (a) của mục 1b: `3C:CD:73:2B:58:8A` chính là củ sạc, không cần rút
điện ra thử nữa.

**Và nó giải thích vì sao nghe 45 giây được 0 gói:** kênh số liệu là `0x001b` của service
`FE95`, không phải `af08` của `AF00` như tôi đoán lúc đầu. Muốn nó nói thì phải qua bắt tay
MiOT (HKDF + HMAC + AES-CCM) bằng **token**, rồi gửi lệnh MiOT `SIID=2, PIID 1–20` vào
`0x001a`. Đúng như anh nghi: số liệu bị khoá sau một phiên xác thực.

**Nó publish thẳng MQTT, retained, có LWT** — tức ăn khớp luôn với kiến trúc ở mục 2:

```
cuktech/charger/port/c1 | c2 | c3 | a     JSON từng cổng, retained
cuktech/charger/settings                   retained
cuktech/charger/status                     trạng thái kết nối, retained + LWT
cuktech/charger/set                        (nó subscribe - app KHÔNG ghi vào)
cuktech/charger/port                       (nó subscribe - app KHÔNG ghi vào)
```

**Điều kiện chạy:** Linux + BlueZ 5.66+ (khuyến nghị 5.71) + adapter Bluetooth, hoặc Docker
trên Linux. Windows **chưa được kiểm chứng**. Cấu hình cần `mac`, `token` (32 ký tự hex),
`ble_key` (tuỳ chọn). Nhịp làm mới mặc định **10 giây**.

**Hai thứ phải kiểm trước khi chốt:**

1. **serverubuntu có Bluetooth không, và có trong tầm củ sạc không?** Tôi không có khoá SSH
   nên chưa kiểm được. Không có thì cắm USB Bluetooth 5.0 (~50k), vẫn phải đủ gần.
   Xa quá thì có [`tsunglung/esphome-cuktech-ble`](https://github.com/tsunglung/esphome-cuktech-ble)
   làm cầu ESP32 — nhưng đó là việc riêng, không phải OpenMQTTGateway như tài liệu gốc.
2. **Củ sạc có cho hai kết nối cùng lúc không?** Nếu không, server và iPhone sẽ tranh nhau:
   anh mở app CUKTECH là màn hình tablet đứng số. Phép thử ở README của `scripts/cuktech_g0/`.

---

## 1d. Soát mã trước khi chạy — 21/11/2026

Đọc tại chỗ, không chạy. Bản soát: `kairui1108/cuktech-ble-server` @ `31752b2`, tag mới nhất
**v2.0.1**, 63 file Python.

### Sạch

| Soát | Kết quả |
|---|---|
| `eval` / `exec` / `os.system` / `subprocess` / `pickle.loads` | **không có dòng nào** ngoài test |
| Mọi URL trong mã Python | chỉ `account.xiaomi.com`, `*.api.io.mi.com`, `api.bemfa.com`, `localhost` |
| Thống kê / telemetry / tự cập nhật | **không có** |
| Bemfa (mây IoT Trung Quốc, cho Xiaoai/Xiaodu) | `enabled: false` mặc định, không chạy nếu không bật |
| Đăng nhập Xiaomi | `/api/xiaomi/login` là **quét QR**, chỉ nhận tham số vùng. Mật khẩu Xiaomi không bao giờ phải gõ |
| CORS | chỉ cho `http://localhost:<port>` và `http://127.0.0.1:<port>`, **không** phải `*` |
| systemd unit | user thường + `NoNewPrivileges` + `ProtectSystem=strict` + `ProtectHome=read-only` |

### Lỗ phải bịt: HTTP 8199 không có xác thực

`ha_server.py` có sáu middleware (size limit, CORS, gzip, cache, timeout, device) — **không
cái nào là xác thực**. Trong khi đó có **14 endpoint POST**, gồm `/api/port` (bật/tắt cổng
sạc), `/api/config`, `/api/xiaomi/beaconkey` (trả thẳng `ble_key` trong JSON), `/api/xiaomi/*`.

Và nó **nghe trên mọi địa chỉ**: `ha_server.py:3883` viết cứng `web.run_app(app, host="0.0.0.0", ...)`.
Khoá `server.host` trong `config.yaml` có được đọc (`config.py:387`) nhưng **không được dùng** —
đặt `127.0.0.1` vào config cũng vô ích.

Nghĩa là: **ai vào được Wi-Fi nhà cũng bật tắt được cổng sạc và đọc được khoá BLE.** Đúng loại
lỗ đã dính một lần với API của go2rtc (README, mục *Chỗ hở ở API go2rtc*).

### Cách bịt — vá một dòng

Sửa `ha_server.py:3883` thành `host=s.config.server.host`, rồi đặt `server.host: 127.0.0.1`
trong `config.yaml`. Một dòng, làm cho chính khoá cấu hình của dự án chạy đúng như nó hứa —
đáng gửi ngược lên upstream.

Lúc cần mở trang cấu hình (quét QR lấy token) thì đi qua SSH tunnel:
`ssh -L 8199:127.0.0.1:8199 serverubuntu@192.168.0.50` rồi mở `http://localhost:8199`.
Tunnel còn khớp luôn với danh sách CORS của dự án.

> Không khuyến nghị bật `ufw` cho việc này: máy đang chạy SSH, Mosquitto, go2rtc, Docker —
> bật tường lửa không cẩn thận là tự khoá mình ra ngoài. Vá một dòng an toàn hơn.

### Lưu ý về nguồn

Repo này là **bản sao tự động**: HEAD là commit của `github-actions[bot]` với nội dung
"Sync from upstream (v2.0.1)", đồng bộ từ thư mục `ble_server/` của
`kairui1108/cuktech-ble-ha` (cùng tác giả). Nên **ghim theo tag `v2.0.1`**, đừng bám `main` —
bot có thể đẩy bản mới bất cứ lúc nào mà không ai soát.

---

## 2. Bốn chỗ sửa so với tài liệu gốc

### 2.1 Số liệu đi bằng MQTT, không dựng FastAPI

Tài liệu đề xuất một service FastAPI ở cổng 8085 cho tablet gọi `GET /api/cuktech/status` và
`ws://.../ws/cuktech`.

App *có* dùng HTTP ở vài chỗ — `data/camera/Go2rtc.kt`, `data/ota/OtaManager.kt`,
`data/thoitiet/OpenMeteo.kt`, `data/giongnoi/GeminiLive.kt` — nhưng đó là những việc MQTT không
làm được: trang phát video, kiểm bản cập nhật trên GitHub, API thời tiết, phiên thoại Gemini.
**Toàn bộ số liệu đo đạc theo thời gian thì đi đúng một cửa**: [MqttDien.kt](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt)
— một client Paho, subscribe ba gốc `dien/#`, `wifi/#`, `server/#` ([dòng 146](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt#L146)),
mỗi gốc một `data class *State` kèm hàm `gop(cur, topic, payload)` (`DienState`, `WifiState`,
[`ServerState.gop` dòng 107](../app/src/main/java/vn/smarthome/data/server/ServerModels.kt#L107)).

`cuktech-ble-server` có sẵn **cả hai**: REST + SSE ở cổng 8199, và MQTT retained. Chọn MQTT.
Cho củ sạc đi đường REST/SSE nghĩa là nó thành nguồn số liệu **duy nhất** không nằm trên
broker: thêm một ô địa chỉ nữa ở màn Cài đặt, thêm một trạng thái mất kết nối nữa phải vẽ, thêm
một cơ chế tự nối lại nữa phải nuôi — trong khi `isAutomaticReconnect` của Paho
([dòng 95](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt#L95)) và topic retained đã làm
sẵn. Đi bằng MQTT thì phần app gọn còn một file mới + vài dòng sửa (mục 5.3).

> Kéo theo: cơ chế "WebSocket đứt 5 giây thì quay sang polling REST mỗi 3 giây" của tài liệu gốc
> không cần nữa.

### 2.2 Giao diện là Compose, không phải React/Vue/HTML5

App là Kotlin + Jetpack Compose thuần, chạy ngang trên Galaxy Tab A7 Lite. Chỗ đặt: mục
**Cảm biến** trong sidebar (`Dest.SENSORS`, [Nav.kt:25](../app/src/main/java/vn/smarthome/ui/shell/Nav.kt#L25))
— đây là mục duy nhất còn rơi vào `PlaceholderScreen`
([AppShell.kt:252](../app/src/main/java/vn/smarthome/ui/shell/AppShell.kt#L252)).

> Lưu ý khi làm: cờ `built` trong `Dest` **không điều khiển gì cả** — không chỗ nào đọc nó
> (`Dest.SETTINGS` cũng để `built = false` mà màn Cài đặt vẫn chạy). Việc thật là thêm nhánh
> `dest == Dest.SENSORS ->` vào `AppShell.kt`; sửa cờ chỉ cho khỏi sai nghĩa.

### 2.3 Không bịa số, và không mượn số mẫu

`ServerState` và `WifiState` khởi tạo bằng `DemoData` rồi lật cờ `isRealData` khi có số thật
([ServerModels.kt:94-105](../app/src/main/java/vn/smarthome/data/server/ServerModels.kt#L94)) —
vì hai màn đó có phần dữ liệu mẫu đeo chip "DỮ LIỆU MẪU".

`CuktechState` **không làm vậy**: khởi tạo rỗng, chưa có gói BLE nào thì màn hình nói "chưa có số"
chứ không vẽ một củ sạc tưởng tượng đang ra 85,5 W. Đây là luật đã áp cho tab Điện năng (README,
mục "Không bịa số") và cho nhiệt độ server (`cpuTempC: Float?` → hiện `—`).

### 2.4 Giai đoạn 0 là cổng chặn

Cả tài liệu đứng trên một giả định chưa ai kiểm: *CUKTECH 10 Ultra phát số từng cổng qua quảng bá
BLE MiBeacon mã hoá, và `xiaomi-ble` giải được*. Khả năng sai cao (mục 6.1). Sai thì decoder,
collector, Dockerfile, systemd, màn hình — vứt hết. Nên qua Giai đoạn 0 mới làm tiếp.

---

## 3. Kiến trúc

```
[ CUKTECH 10 Ultra  3C:CD:73:2B:58:8A ]
         | BLE GATT, service FE95, sau bắt tay MiOT bằng token
         v
[ cuktech-ble-server  - Docker tren serverubuntu, MIT ]
         |
         | publish cuktech/charger/#   (retained, co LWT)
         v
[ serverubuntu 192.168.0.50 : Mosquitto 1883 ]
         |
         v  subscribe cuktech/#  (QoS 1)
[ App SmartHome - MqttDien.kt ]
         |
         v
[ Màn Cảm biến + ô tóm tắt Trang chủ ]
```

Ba chặng, không còn ESP32, không còn collector tự viết. Phần duy nhất ta viết là `data class`
và màn hình Compose.

Broker: Mosquitto `192.168.0.50:1883`, **không TLS**, tài khoản `iot` — theo `CAU-HINH.txt` dòng
14, 24–25.

---

## 4. Hợp đồng topic — ĐO THẬT 21/11/2026

Đây là JSON **thật đọc từ broker**, không phải chép từ README. Cổng C1 đang cắm tải lúc đo.

```
cuktech/charger/port/c1
{"voltage": 20.1, "current": 2.3, "power": 46.2, "active": true,
 "protocol": "PD", "status_raw": 1, "_raw_code": "0x03", "_proto_num": 7, "enabled": true}

cuktech/charger/port/c2 | c3 | a        (cổng trống)
{"voltage": 0.0, "current": 0.0, "power": 0.0, "active": false, "protocol": "idle"}

cuktech/charger/status
{"connected": true, "authenticated": true,
 "device_model": "njcuk.fitting.ad1204_HS6621C", "firmware_version": "2.1.2_0073",
 "ports": {"1": {...}, "2": {...}, "3": {...}, "4": {...}},
 "settings": {...}}

cuktech/charger/settings
{"5": 1, "6": 2, "8": 0, ..., "16": 15, "17": 120391469, "18": 522, "21": 50532111}
```

### Bảy điều chốt được từ gói thật

1. **Tên trường là `voltage` / `current` / `power`**, không phải `voltage_v` / `power_w` như
   tài liệu gốc đặt. Đơn vị không nằm trong tên.
2. **`active`** (bool) mới là thứ quyết định cổng sáng hay mờ, dùng nó thay vì so `power > 0.5`.
3. **`protocol`** là chuỗi: `"PD"`, `"idle"`, `"5V"`. Cổng trống ghi `"idle"`, không phải `null`.
4. **Cổng trống chỉ có 5 trường** — không có `enabled`, `status_raw`, `_raw_code`. Khai
   `data class` phải để mấy trường đó nullable/có mặc định, không thì parse cổng trống là nổ.
5. **`cuktech/charger/status` chở luôn cả 4 cổng**, khoá là `"1".."4"` (1→c1, 2→c2, 3→c3,
   4→USB-A). Nên app có thể chỉ subscribe một topic này là đủ, khỏi ghép 4 topic rời.
6. **KHÔNG có nhiệt độ, KHÔNG có quạt, KHÔNG có tổng công suất.** `fan_speed_rpm` và
   `temperature_c` của tài liệu gốc không tồn tại. Tổng phải tự cộng từ 4 cổng và nói rõ
   trên màn hình là số cộng lại.
7. **`settings` là bảng PIID** — ban đầu tưởng chưa giải nghĩa, nhưng
   `src/cuktech_ble/protocol.py` của dự án có chú thích đầy đủ. Đối chiếu gói thật với ảnh
   chụp app CUKTECH ngày 21/11/2026, **tám giá trị khớp hết**:

   | PIID | Nghĩa | Gói thật | App điện thoại |
   |---|---|---|---|
   | 5 | Kịch bản (1=AI, 2=Kết hợp, 3=Đơn, 4=Cân bằng) | `1` | AI |
   | 6 | Thời gian sáng màn (1=5′, 2=10′, 3=30′, 4=luôn sáng, 5=1′) | `2` | 10 phút |
   | 8–12 | Hẹn tắt: bật/tắt + số phút từng cổng | `0` | "Chưa cài đặt" |
   | 13 | Ngôn ngữ (0=EN, 1=CN) | `0` | English |
   | 15 | Sạc nhỏ giọt USB-A | `0` | tắt |
   | 16 | Mặt nạ bit cổng (bit0=C1…bit3=A) | `15` = `0b1111` | cả 4 cổng bật |
   | 19 | Tự tắt màn khi nhàn rỗi | `1` | bật |
   | 20 | Khoá xoay màn | `0` | tắt |

   PIID 17, 18, 21 thì chính dự án cũng ghi "未知" (chưa rõ) — ba cái đó mới là chưa giải được.

> Trường có gạch dưới (`_raw_code`, `_proto_num`) là nội bộ của dự án, có thể đổi giữa các
> bản. **Đừng khai vào `data class`.**

### Một chỗ lệch cần nhớ

`/api/health` báo `"ble": false` trong khi `cuktech/charger/status` báo
`"connected": true, "authenticated": true`. App đọc MQTT nên không ảnh hưởng — nhưng đừng
dùng `/api/health` để chẩn đoán, nó không khớp.

---

## 5. Các giai đoạn

### Giai đoạn 0 — Xác minh thiết bị · ĐÃ XONG 21/11/2026

> Giữ lại nguyên văn để biết đã hỏi gì và vì sao. Kết quả ở mục 1b và 1c. Câu 1–2 (Mi Home,
> bindkey) nay thay bằng: lấy **token** qua trang `config.html` của `cuktech-ble-server`,
> không cần `xiaomi-cloud-tokens-extractor`, và `ble_key` là tuỳ chọn.

Chưa viết code dự án. Mọi thứ ở thư mục tạm. Trả lời năm câu, có bằng chứng:

1. **Củ sạc thêm được vào Mi Home không?** Nếu nó chỉ vào được app CUKTECH riêng thì
   `xiaomi-cloud-tokens-extractor` không có gì để lấy và cả nhánh MiBeacon sụp.
2. **Lấy được bindkey không?** `xiaomi-cloud-tokens-extractor` (Docker), vùng Trung Quốc.
   Cần ghi lại: MAC + chuỗi hex 32 ký tự.
3. **Nó quảng bá cái gì?** Ngồi cạnh củ sạc:
   `nRF Connect` trên điện thoại (xem có service data `0xFE95` không, payload dài bao nhiêu byte),
   hoặc trên serverubuntu `sudo btmon` / `bluetoothctl scan on`.
4. **Payload có đổi theo tải không?** Cắm rồi rút tải ở cổng thứ nhất, so hai payload.
   **Đây là câu quan trọng nhất của cả dự án.**
5. **Đếm đúng phần cứng:** củ sạc thật có mấy cổng, tên gì, có quạt không, có hiện nhiệt độ không.
   Tài liệu gốc ghi 4 cổng (C1/C2/C3/USB-A) + `fan_speed_rpm`; chưa ai xác nhận.

Rồi giải mã thử: vài gói thô + bindkey + script nháp ~20 dòng dùng `xiaomi-ble`, xem nó trả về
sensor nào. **Giữ lại các gói hex bắt được** — chúng thành fixture cho unit test ở Giai đoạn 2.

**Quyết định cuối giai đoạn:**

| Kết quả | Hướng đi |
|---|---|
| Có `0xFE95`, giải ra số đổi theo tải | Giai đoạn 1, đúng kế hoạch này |
| Có BLE nhưng giao thức riêng | Phương án B (mục 6.2) — cân nhắc có đáng không |
| Chỉ quảng bá tên, không có số | Phương án C (mục 6.3) |
| Không có BLE / không ghép được | Dừng, báo lại |

> **Đang ở đâu (21/11/2026):** câu 3 và 4 đã đo xong, kết quả rơi vào hàng thứ ba — chỉ quảng
> bá nhận dạng, không có số. Nhưng còn kênh GATT `AF00` chưa khai thác, nên chưa chốt vội:
> làm nốt ba phép đo ở mục 1b rồi mới chọn giữa B và C.

### Giai đoạn 1 — Cho serverubuntu thấy củ sạc · ĐÃ XONG 21/11/2026

| Đo | Kết quả |
|---|---|
| Bluetooth sẵn có trên server | **không có**. `RT5390R` là Wi-Fi thuần, `lsusb` không có BT |
| Dongle USB đã cắm | Baseus, nhưng ruột là **Barrot 8041a02** giả danh CSR (`0a12:0001`). Kernel báo bản vá lỗi nhận **áp không thành công** |
| BlueZ | **5.85** (cần ≥ 5.66) |
| `btmgmt info` | có `le` trong `current settings` |
| Thấy củ sạc | có, **RSSI −76** với củ sạc kê sát server |
| `bluetoothctl connect` | **thất bại** — nhưng là lỗi giả: nó cố nối *profile*, mà củ sạc không có profile nào |
| Nối GATT bằng `bleak` | **thành công, giữ 30 giây không rớt** |

**Kết luận: dongle dùng được, không cần mua con khác.** Phép thử đúng là `bleak`, không phải
`bluetoothctl connect` — thiết bị chỉ-GATT luôn làm `bluetoothctl connect` báo hỏng.

−76 là yếu so với khoảng cách (máy Windows với adapter Intel đo −43 ở xa hơn), nên **giữ củ
sạc gần server**, và nếu sau này hay rớt thì đổi dongle **RTL8761B** (`lsusb` ra `0bda:8771`)
chứ không phải dời máy.

### Giai đoạn 2 — Dựng `cuktech-ble-server` trên serverubuntu · nửa buổi

**Không viết collector nữa.** Giai đoạn này chỉ còn cài và cấu hình.

**Chạy native systemd, không chạy Docker.** `docker/docker-compose.yml` của dự án đòi
`privileged: true` + `network_mode: host` + socket D-Bus — tức mã của người lạ chạy với toàn
quyền trên máy. Bản systemd của chính dự án (`systemd/install-service.sh`) chạy bằng **user
thường**, chỉ thêm nhóm `bluetooth`, kèm `NoNewPrivileges`, `ProtectSystem=strict`,
`ProtectHome=read-only`, `PrivateTmp`. Ít quyền hơn hẳn, mà đã chứng minh là đủ: `bleak` nối
được GATT bằng đúng user thường ở Giai đoạn 1.

Các bước:

1. `git clone`, rồi **ghim theo tag** (`git tag` xem có gì, `git checkout <tag>`). Đây là dự
   án cá nhân, đừng bám `main`.
2. `python3 -m venv .venv` + `pip install -r requirements.txt`.
   Server đang chạy **Python 3.14** — nếu có gói nào không build được thì lùi về Python 3.12
   (`apt install python3.12-venv`), đừng vá lung tung.
3. `cp config.yaml.example config.yaml`, điền:
   `ble.mac: 3C:CD:73:2B:58:8A`; `mqtt.enabled: true`, `host: 127.0.0.1`, `port: 1883`,
   `username: iot`, `password` lấy từ `~/homeserver/.env`.
4. Chạy tay một lần để lấy **token**: mở `http://192.168.0.50:8199/config.html`, quét QR bằng
   Mi Home, nó tự điền. Không cần `xiaomi-cloud-tokens-extractor`.
5. Chạy được rồi mới `bash systemd/install-service.sh` để thành dịch vụ.
6. `chmod 600 config.yaml` — trong đó có token và mật khẩu MQTT. **Không vào git.**

> **Fork hay không.** Hiện có vài bản: `kairui1108/cuktech-ble-server` (bản gốc),
> `Arimayuki03/cuktech-ble-server` (fork đang được bảo trì), `kairui1108/cuktech-ble-ha`
> (bản tích hợp Home Assistant). Giao thức gốc ghi nguồn là `zhyzhaogit/cuktech-ble-controller`.

Nghiệm thu: `mosquitto_sub -h 192.168.0.50 -u iot -P <pass> -t 'cuktech/#' -v` ra JSON từng
cổng; cắm tải vào C1 thì số đổi; dừng dịch vụ thì `cuktech/charger/status` chuyển offline.

**Chép lại JSON thật của một cổng vào đây** trước khi sang Giai đoạn 3 — `data class` khai
theo gói thật, không theo README của dự án.

### Trạng thái dựng — 21/11/2026

Đã chạy trên serverubuntu (`~/cuktech-ble-server`, tag `v2.0.1`):

| Bước | Trạng thái |
|---|---|
| Clone + ghim tag `v2.0.1` | xong |
| Vá `ha_server.py:3883` → `host=s.config.server.host` | xong, `git diff` đúng 1 dòng |
| venv + 7 gói trên Python 3.14 | xong, import được hết |
| `config.yaml` (mac, mqtt iot, `server.host: 127.0.0.1`, `chmod 600`) | xong |
| Chạy thử | `LISTEN 127.0.0.1:8199` — gọi từ `192.168.0.50:8199` trả `000` |
| `/api/health` | `ble: false, mqtt: true, bemfa: false` |
| Topic đầu tiên trên broker | `cuktech/charger/status` → `device_model: njcuk.fitting.ad1204_HS6621C`, `firmware_version: 2.1.2_0073` |

Tức dongle Baseus **nối BLE và đọc được device info thật**. `connected: false` chỉ vì chưa
có token.

**Còn lại, cần tay người:**

1. **Token** — mở tunnel rồi quét QR bằng Mi Home (xem README `scripts/cuktech_g0/`).
2. **`bash systemd/install-service.sh`** — cần `sudo`, mà `sudo` trên server hỏi mật khẩu.
   Hiện tiến trình đang chạy bằng `nohup`, **reboot là mất**.
3. `sudo apt install mosquitto-clients` nếu muốn dùng `mosquitto_sub` (hiện chưa có).

### Giai đoạn 3 — Phía app · XONG 21/11/2026

| File | Việc |
|---|---|
| `data/cuktech/CuktechModels.kt` *(mới)* | `CuktechTopic`, `CongSacId`, `CongSac`, `CuktechState.gop()`. Khởi tạo **rỗng**, không `DemoData` |
| `data/dien/MqttDien.kt` | `_cuktech` StateFlow, nhánh `cuktech/`, thêm `CuktechTopic.GOC` vào `GOC_CAN_DOC` |
| `vm/HomeViewModel.kt` | phơi `cuktech` |
| `ui/screens/CamBienScreen.kt` *(mới)* | thẻ tổng + 4 thẻ cổng + băng cảnh báo mất kết nối |
| `ui/shell/AppShell.kt` | nhánh `dest == Dest.SENSORS` |
| `ui/shell/Nav.kt` | `SENSORS(..., built = true)` |
| `app/src/test/.../CuktechStateTest.kt` *(mới)* | 8 test, fixture là **gói thật** bắt từ broker |
| `app/src/test/.../ShellSmokeTest.kt` | test cũ khẳng định Cảm biến là placeholder — thay bằng 2 test màn thật |

**Lỗi `grantedQos` ở Giai đoạn 3 cũ: phiên khác đã sửa rồi.** `MqttDien` giờ duyệt cả mảng,
đối chiếu theo chỉ số với `GOC_CAN_DOC` và nêu đúng tên gốc bị từ chối. Không phải làm lại.

Kết quả: `:app:compileDebugKotlin` sạch, `:app:testDebugUnitTest` **161 test xanh**.

**Chưa làm (có chủ đích):**

* Ô tóm tắt củ sạc ở Trang chủ.
* Khai `"cu_sac"` cho trợ lý giọng nói trong `CongCuNha.kt`.
* Chạy thử trên máy thật SM-T225 — mới chỉ build và test, **chưa ai nhìn thấy màn này chạy**.

### Giai đoạn 4 — Nghiệm thu và tài liệu · nửa buổi

1. `mosquitto_sub -t 'cuktech/#' -v` khớp hợp đồng mục 4.
2. Cắm tải vào C1 → số trên tablet đổi. Đo độ trễ thật và ghi vào README (dự kiến ~10 giây,
   xem mục 6.4).
3. Rút điện củ sạc → app chuyển "mất kết nối" nhờ `cuktech/charger/status`, không treo số cũ.
4. `docker compose stop` server → app hiện offline nhờ LWT.
5. Mở app lúc chưa có gói nào → hiện "chưa có số", không hiện số mẫu.
6. **Mở app CUKTECH trên iPhone trong lúc tablet đang hiện số** → xem server có bị đá ra
   không (mục 6.2). Ghi lại kết quả dù tốt hay xấu.
7. Thử trên máy thật SM-T225, cả theme sáng và tối.
8. Cập nhật tài liệu: `README.md` (bảng "Cái gì đã có", dòng Cảm biến; thêm một mục như mục
   Điện năng/Camera), `CAU-HINH.txt` (bảng topic + lệnh `mosquitto_sub` + cách cài server).
9. Nâng `versionCode`/`versionName` như các lần trước (`chore: Nang phien ban len ...`).

---

## 6. Rủi ro

### 6.1 Rủi ro lớn nhất — ĐÃ ĐO: số liệu không nằm trong gói quảng bá

> **Đo ngày 21/11/2026 (mục 1b): đúng như lo ngại.** Khung FE95 dài 11 byte, cờ "object" tắt,
> 48 gói liên tiếp giống hệt nhau. Không có số liệu nào trong quảng bá. Phần phân tích dưới
> đây giữ lại vì nó giải thích vì sao, và vì sao `xiaomi-ble` sẽ im lặng chứ không báo lỗi.

Khung service data MiBeacon rất ngắn và thường chở **một** phép đo mỗi khung. Chở 4 cổng ×
(V, A, W, giao thức) + nhiệt độ + quạt trong một khung quảng bá là bất thường. Nhiều khả năng
những số đó chỉ lấy được khi **kết nối GATT** rồi đăng ký notify — mà OpenMQTTGateway thì chủ yếu
*nghe quảng bá*, không kết nối GATT cho thiết bị lạ.

Thêm nữa: `xiaomi-ble` chỉ hiểu các object ID nó đã biết. Gặp ID riêng của CUKTECH thì nó trả về
**rỗng chứ không báo lỗi** — rất dễ tưởng mình cấu hình sai trong khi thiết bị vốn không nói.

Đây chính là lý do có Giai đoạn 0.

### 6.2 Tranh kết nối với điện thoại

Phương án B cũ (tự dịch ngược giao thức) **không còn cần** — mục 1c đã có dự án làm xong.
Rủi ro còn lại ở chỗ khác: củ sạc có thể chỉ cho **một** kết nối BLE tại một thời điểm.

Nếu đúng vậy thì server và app CUKTECH trên iPhone sẽ tranh nhau: anh mở app xem số là
tablet đứng, đóng app thì server phải bắt tay lại (dự án ghi "năm bước bắt tay", 30–60 giây).
Chưa tài liệu nào của dự án nói về chuyện này, nên phải tự đo — bước 6 của Giai đoạn 4.

Nếu tranh thật thì chọn một trong hai: để server giữ kết nối và bỏ thói quen mở app trên
điện thoại, hoặc quay về mục 6.3.

### 6.3 Phương án C — đo bằng thiết bị khác

Cắm củ sạc sau một ổ cắm đo điện (Tuya/Shelly/ZigBee). Mất số từng cổng, còn tổng công suất —
rẻ, chắc chắn chạy, và vẫn vào đúng đường MQTT này: chỉ `cuktech/ports` rỗng, phần còn lại
không đổi một dòng.

### 6.4 Độ trễ

`settings_refresh_interval` mặc định **10 giây**, cộng thêm thời gian bắt tay lại sau mỗi lần
mất kết nối (30–60 giây). Chỉ tiêu "< 1,5 giây" của tài liệu gốc **bỏ hẳn**, không phải vì
code sai mà vì thiết bị và dự án trung gian làm việc theo nhịp đó. Muốn nhanh hơn thì phải
sửa dự án đó — không đáng cho một màn hình treo tường.

### 6.5 Số cổng và quạt

Topic của dự án cho biết đúng **bốn** cổng: `c1`, `c2`, `c3`, `a`. Trường `fan_speed_rpm` của
tài liệu gốc thì không thấy đâu — nhiều khả năng không có. Khai `data class` theo **gói JSON
thật đọc từ broker**, không theo tài liệu gốc cũng không theo README của dự án.

### 6.6 Bảo mật

Mosquitto ở nhà chưa có `acl_file`: ai đăng nhập được là đọc ghi được mọi topic
(`CAU-HINH.txt` dòng 35–36). Thêm `cuktech/#` không làm xấu hơn. Nhưng tới ngày siết ACL thì
nhớ cấp quyền cho cả gốc mới — và lúc đó chỗ sửa `grantedQos` ở Giai đoạn 3 mới là thứ cho
biết vì sao màn hình trống.

Hai thứ phải giữ kín: **token** của củ sạc (có nó là điều khiển được củ sạc) và mật khẩu MQTT.
Không commit, không dán vào chat.

Và một thứ nữa: `cuktech-ble-server` là **mã của người lạ chạy trong nhà**, có quyền Bluetooth
và quyền ghi lên broker. Đọc qua `docker-compose.yml` của nó trước khi chạy, ghim theo tag,
đừng để nó tự cập nhật.

---

## 7. Thứ tự và điểm dừng

```
G0 xác minh ──► G1 server thấy củ sạc ──► G2 dựng cuktech-ble-server ──► G3 app ──► G4 nghiệm thu
   (xong)          (BlueZ? tầm sóng?)        (token + config + Docker)
                          │                           │
                          └─► xa quá: cầu ESP32       └─► chưa thấy JSON thì chưa viết Kotlin
```

Giai đoạn 0 **đã xong** (mục 1b, 1c): thiết bị xác định rồi, đường đi xác định rồi.

Điểm dừng còn lại: **thấy JSON thật trên broker mới viết mã Kotlin**, và khai `data class`
theo gói thật chứ không theo tài liệu. Làm ngược lại thì vẽ xong giao diện mới biết tên trường
không khớp.
