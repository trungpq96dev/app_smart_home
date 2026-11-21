# Kế hoạch: nối 2 công tắc Wi-Fi phòng ngủ 1 (Smart Life + CozyLife)

Soạn 21/11/2026 từ `k_ho_ch_t_ch_h_p_c_ng_t_c_smart_life_cozylife.md`, đã đối chiếu với mã nguồn
thật **và đã đo trực tiếp trên hai thiết bị**. Mọi khẳng định về mã nguồn đều kèm đường dẫn;
mọi khẳng định về thiết bị đều kèm gói tin bắt được.

---

## 0. Đang ở đâu — 21/11/2026

| Giai đoạn | Trạng thái |
|---|---|
| G0 — xác minh | **Xong.** Tìm ra cả hai thiết bị, xác nhận cả hai giao thức, lấy được `device_id` + protocol của đèn. **G0-c đã chốt: `dpid 1` đúng là quạt** (21/11, bật tắt thật, người nghe xác nhận). G0-b (IP tĩnh) đã bỏ: bridge tự dò theo mã thiết bị |
| G1 — `local_key` | **Chưa bắt đầu.** Cần tài khoản `iot.tuya.com` — việc của người |
| G2 — bridge | **ĐANG CHẠY THẬT** trên serverubuntu từ 21/11, chỉ với quạt (`DEN_BAT=0`) |
| G3 — app Kotlin | **Xong**, đã phát hành v0.7.0 và cài lên tablet |
| G4 — nghiệm thu | **Quạt: xong.** Đèn: chờ G1 |

### Số đo thật — 21/11/2026

Đo bằng cách publish vào `smarthome/control` rồi bấm giờ tới lúc `congtac/cmd/result` về:

| Lệnh | Độ trễ | Kết quả |
|---|---|---|
| `ON` | **96 ms** | `ok`, `is_on=true`, `tra_loi_id=bat1` |
| `OFF` | **458 ms** | `ok`, `is_on=false`, `tra_loi_id=tat1` |

Hạn chờ 6 giây của hàng đợi trong app (`HangChoCongTac.GIAY_CHO_MAC_DINH`) rộng hơn số đo
xấu nhất **13 lần** — giữ nguyên, không cần siết.

Ba phép thử đường lệnh không chạm thiết bị cũng đã qua: `TANG` → `bo_qua`; thiết bị lạ
(`kitchen_den`) → **im lặng**, không publish gì; JSON hỏng → `WARNING` rồi chạy tiếp.
Bridge: RAM 13,2/128 MB, CPU 0,12%, `restarts 0`.

Đã viết:

* [`scripts/congtac_g0/`](../scripts/congtac_g0/README.md) — 3 script dò, **chỉ đọc**, đã chạy
  thật trên hai thiết bị.
* [`scripts/congtac_bridge/`](../scripts/congtac_bridge/README.md) — bridge đầy đủ:
  `driver.py` (Tuya + CozyLife), `congtac_bridge.py`, Docker, `cau_hinh.mau.env`, README.
  `test_driver.py`: **25 test xanh**, chạy không cần broker lẫn phần cứng.
* `scripts/.gitignore` — chặn thêm `devices.json` / `snapshot.json` / `tuya-raw.json`
  (các file `tinytuya wizard` sinh ra, chứa `local_key` của **mọi** thiết bị Tuya trong tài khoản).

> **Mật khẩu MQTT KHÔNG nằm ở `~/homeserver/.env`** như tài liệu cũ ghi — file đó đặt tên biến
> là `MQTT_PASSWORD`, còn collector và bridge đều đọc `MQTT_PASS`. Chép nhầm là ra biến rỗng và
> bridge không nối được broker. Chỗ đúng: `~/server_collector/.env`.

Phần bridge đã tự kiểm được những gì không cần broker: đọc thật từ quạt (`online=True, is_on=False`),
thiết bị chết → `online=False, is_on=None` kèm lý do, `dao()` ném chứ không đoán, dpid sai → báo rõ,
ngưỡng 3 vòng lỗi mới tuyên bố offline, và chỉ publish khi trạng thái đổi.

**Chưa gửi một lệnh bật/tắt nào tới thiết bị nào** — toàn bộ phép đo tới giờ đều chỉ đọc.

---

## 1. Bối cảnh

App SmartHome đang chạy thật trên tablet treo tường, nhưng **đèn, quạt, rèm vẫn là khung giao
diện**: gạt công tắc thì icon đổi màu và chấm hết, không bóng đèn nào sáng. README ghi thẳng điều
đó, và trợ lý giọng nói còn in một dòng xám *"chỉ đổi trên màn hình — chưa nối thiết bị thật"*
dưới mỗi lệnh.

Việc lần này là nối **đúng hai thứ** thành thật: **đèn** (hệ Smart Life / Tuya) và **quạt**
(hệ CozyLife) trong **Phòng ngủ 1** — mã phòng `bed1` trong app. Mọi thiết bị còn lại vẫn là số mẫu.

Kết quả mong muốn: gạt công tắc Đèn/Quạt phòng ngủ 1 trên tablet thì đồ vật ngoài đời bật tắt
theo; bấm công tắc cơ trên tường thì màn hình đổi theo; thiết bị mất điện thì màn hình nói
*"mất kết nối"* chứ không vờ là đã tắt; và nói *"bật đèn phòng ngủ một"* thì đèn sáng thật.

Tài liệu gốc đề xuất dựng một backend FastAPI + REST mới. **Không làm vậy** — lý do ở mục 3.1.

---

## 2. Kết quả đo Giai đoạn 0 — 21/11/2026

Đo từ máy Windows (`192.168.0.105`), chỉ đọc. **Chưa ghi một byte điều khiển nào vào thiết bị
nào.** Dò toàn dải `192.168.0.2-254` ở hai cổng 6668 và 5555; cả mạng chỉ có đúng hai máy trả lời.

| | Đèn — Tuya | Quạt — CozyLife |
|---|---|---|
| IP | **192.168.0.100** | **192.168.0.103** |
| MAC | `84:7a:b6:1f:95:fb` | `00:50:c2:ec:bf:27` |
| Cổng mở | 6668 (và chỉ 6668) | 5555 |
| Bằng chứng | quảng bá UDP 6667, magic `000055aa`, lệnh `0x13` | trả lời `cmd 0` và `cmd 2` |

**Đèn** — giải khung quảng bá bằng khoá công khai (`MD5("yGAdlopoPVldABfn")`, hằng số của giao
thức, **không** phải bí mật của nhà):

```json
{"ip":"192.168.0.100","gwId":"eb74553feb16595327lzam","active":2,"ablilty":0,
 "encrypt":true,"productKey":"noxmjxuwck4yvbyi","version":"3.3","token":true}
```

Tức là **`device_id` lấy được rồi, và protocol là 3.3** — bản dễ nhất, không phải bắt tay phiên
như 3.4/3.5. Còn thiếu đúng một thứ: `local_key`.

**Quạt** — `cmd 0` (hỏi lý lịch) và `cmd 2` (hỏi trạng thái):

```json
{"cmd":0,"pv":0,"sn":"...","msg":{"attr":[0],"did":"727700990050c2ecbf27","dtp":"02",
 "pid":"osP4vT","mac":"0050c2ecbf27","ip":"192.168.0.103","rssi":-53,
 "sv":"1.0.0","hv":"0.0.1"},"res":0}

{"cmd":2,"pv":0,"sn":"...","msg":{"attr":[1,2],"data":{"1":0,"2":0}},"res":0}
```

Hỏi ba lần liên tiếp, mỗi lần mở rồi **đóng hẳn socket**: lần nào cũng trả lời. Cách đóng socket
sau mỗi lệnh mà tài liệu gốc dặn là đúng.

### 2b. Bốn chỗ tài liệu gốc ghi SAI — đo ra mới biết

1. **`sn` bắt buộc là chuỗi 13 chữ số mili-giây.** Tài liệu gốc viết `"sn": 1700000000` — số
   nguyên 10 chữ số. Gửi như thế thiết bị **im lặng tuyệt đối**: không trả lời, không báo lỗi,
   chỉ timeout. Đây là cái bẫy đắt nhất của cả tài liệu, vì triệu chứng của nó giống hệt "sai IP"
   và "thiết bị hỏng". Phải là `str(int(time.time()*1000))`.
2. **Thiếu `"pv":0` cũng im lặng y như vậy.** Không phải trường tuỳ chọn.
3. **`res` nằm ở cấp ngoài cùng, không nằm trong `msg`.** `res:0` = ok, `res:1` = lỗi
   (đo được: gửi `"msg":{}` thì trả `res:1`). Tài liệu gốc không nhắc tới `res`.
4. **Thiết bị khai hai kênh `attr:[1,2]`**, không phải một. Công tắc trên tường chỉ có một nút,
   nên kênh 2 là thứ khác (hẹn giờ / chế độ). Dùng dpid `"1"`, nhưng phải xác minh một lần
   (mục 5, G0-c) chứ đừng tin suy đoán này.

### 2c. Ba thứ chưa kiểm được, cần tay người

1. **`192.168.0.100` có đúng là công tắc đèn không?** Camera Tuya ở `192.168.0.104`
   (`CAU-HINH.txt` mục 1) **hiện đang tắt** — ping không trả lời, không mở cổng nào. Nên không
   loại trừ được khả năng `.100` chính là camera đã đổi IP. Phép thử dứt điểm rẻ: **rút điện công
   tắc đèn, quét lại**; `.100` biến mất thì đúng là nó.
2. **Kênh nào của CozyLife là quạt** — dpid `1` hay `2`. Bật tắt bằng tay rồi hỏi lại là biết
   (chỉ đọc, không cần ghi).
3. **`local_key` của đèn** — mục 5, G1.

---

## 3. Bốn chỗ sửa so với tài liệu gốc

### 3.1 Đi bằng MQTT, không dựng FastAPI

Tài liệu đề xuất `backend/` FastAPI + REST `/api/switches`. Nhà này đã có sẵn **một cửa số liệu
duy nhất**: Mosquitto trên `serverubuntu` `192.168.0.50:1883`, và
[`MqttDien.kt`](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt) — một client Paho subscribe
ba gốc `dien/#`, `wifi/#`, `server/#`, mỗi gốc một `data class *State` kèm `gop(cur, topic, payload)`.

Cho công tắc đi đường REST nghĩa là: thêm một ô địa chỉ nữa ở màn Cài đặt, thêm một trạng thái
mất kết nối nữa phải vẽ, thêm một cơ chế tự nối lại nữa phải nuôi, và app phải **tự poll** thay vì
được đẩy — trong khi `isAutomaticReconnect` của Paho và topic retained đã làm sẵn hết.

Thêm nữa, **topic lệnh đã dựng sẵn và chưa ai nghe**:
`DieuKhienTopic.CMD = "smarthome/control"` + `guiLenhDieuKhien()`
([MqttDien.kt:28, 191](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt#L191)), payload
`{"device":"bed1_den","room":"bed1","action":"ON","source":"voice"}`
([HomeViewModel.kt:474](../app/src/main/java/vn/smarthome/vm/HomeViewModel.kt#L474)).
Dùng lại nó thì **đường lệnh của giọng nói chạy được mà không sửa dòng nào**.

Đây cũng là đúng khuôn mà `docs/KE-HOACH-CUKTECH.md` mục 2.1 đã chốt cho mọi thiết bị mới.

### 3.2 Driver chạy trên server, không chạy trong app

Tài liệu gốc viết driver bằng Python — giữ nguyên ý đó, đặt ở `scripts/congtac_bridge/` theo khuôn
`scripts/server_collector/` đang chạy. **Không** viết lại giao thức Tuya bằng Kotlin: 3.3 cần
AES-ECB + bắt tay, tương đương viết lại `tinytuya`, và đổi lại `local_key` sẽ nằm trong máy tính
bảng treo tường thay vì trong `.env` trên server.

### 3.3 Không bịa trạng thái — `is_on` phải nullable

Chỗ này là nơi nguyên tắc *"không bịa số"* của dự án sống hoặc chết. **Thiết bị offline thì
`is_on` BẮT BUỘC là `null`**, không giữ giá trị cũ, không đặt `false`.

Một cái công tắc gạt được, nằm ở thế TẮT, trong khi app không hề biết đèn đang sáng hay không, là
lời nói dối tốn ít pixel nhất trong app này. Màn hình phải vẽ `—` giống cách
`ServerMetric.cpuTempC: Float?` đang làm.

Kéo theo: `DAO` (đảo) **phải đọc trạng thái thật trước rồi mới đảo**, không được dựa vào cache.
Cache cũ 10 giây mà trong 10 giây đó có người bấm công tắc cơ thì đảo ngược ý người dùng —
triệu chứng *"bấm bật thì nó tắt"*, rất khó chịu và rất khó tái hiện.

### 3.4 Xác nhận bằng mã lệnh, không bằng đồng hồ

Lệnh mang thêm một trường `id` ngẫu nhiên; bridge chép lại vào trạng thái dưới khoá `tra_loi_id`.
App gỡ cờ "đang chờ" khi thấy `tra_loi_id` khớp.

Vì sao không so mốc thời gian: mốc trong payload do **server** sinh, còn mốc "lúc tôi gửi" do
**tablet** sinh. Hai đồng hồ đó không bao giờ khớp, và lệch vài giây là cơ chế chống-nhảy-ngược
hỏng theo cách rất khó tìm. So chuỗi `id` thì không cần đồng hồ nào.

---

## 4. Kiến trúc và hợp đồng topic

```
[ Công tắc Tuya 192.168.0.100 ]      [ Công tắc CozyLife 192.168.0.103 ]
      TCP 6668, proto 3.3                    TCP 5555, JSON + \r\n
            |                                        |
            +------------------+---------------------+
                               v
              [ congtac_bridge — Docker, Python, serverubuntu ]
                               |
              publish congtac/#  |  subscribe smarthome/control
                               v
              [ Mosquitto 192.168.0.50:1883 ]
                               ^
              subscribe congtac/#  |  publish smarthome/control
                               |
                     [ App — MqttDien.kt ]
                               v
          [ RoomDetail · Devices · Home · Trợ lý giọng nói ]
```

### Bảng topic

| Topic | Retained | Ai ghi | Nội dung |
|---|---|---|---|
| `congtac/info` | ✔ | bridge | danh sách thiết bị bridge đang quản |
| `congtac/thietbi/<id>` | ✔ | bridge | trạng thái một công tắc |
| `congtac/trangthai` | ✔ | bridge | `online` / `offline` — **LWT** |
| `congtac/capnhat` | ✔ | bridge | `YYYY-MM-DD HH:MM:SS` |
| `congtac/cmd/result` | ✘ | bridge | kết quả một lệnh vừa thực thi |
| `smarthome/control` | ✘ | **app** | lệnh — topic đã có sẵn, giữ nguyên |

`<id>` = `<phong>_<thietbi>` → `bed1_den`, `bed1_quat`. Cố ý khớp đúng chuỗi
`HomeViewModel.kt:474` đang sinh ra, nên không cần bảng ánh xạ ở bất kỳ đâu.

> **Vì sao trạng thái từng máy là `congtac/thietbi/<id>` chứ không phải `congtac/trangthai/<id>`:**
> để `congtac/trangthai` (LWT, payload là chuỗi thường `"online"`) không đụng tiền tố với một
> nhánh JSON. Trùng tiền tố thì `gop()` phải nhớ "so khớp chính xác trước, tiền tố sau", và cái
> ngày ai đó sửa nhầm thứ tự thì LWT bị đem đi `JSONObject(...)` rồi nuốt lỗi lặng lẽ.

### Payload

`congtac/info`:

```json
{"bridge":"congtac_bridge 1.0","cap_nhat":"2026-11-21 20:14:03",
 "thiet_bi":[
   {"id":"bed1_den","ten":"Đèn Phòng ngủ 1","phong":"bed1","loai":"den",
    "driver":"tuya","ip":"192.168.0.100","nang_luc":["ON","OFF","DAO","DAT"]},
   {"id":"bed1_quat","ten":"Quạt Phòng ngủ 1","phong":"bed1","loai":"quat",
    "driver":"cozylife","ip":"192.168.0.103","nang_luc":["ON","OFF","DAO","DAT"]}]}
```

`congtac/thietbi/bed1_den`:

```json
{"id":"bed1_den","phong":"bed1","loai":"den","online":true,"is_on":true,
 "nguon":"thiet_bi","doc_luc":"2026-11-21 20:14:03","doi_luc":"2026-11-21 20:13:51",
 "tra_loi_id":"a1b2c3","ly_do_loi":null}
```

`smarthome/control` (app ghi — thêm `id`, mở rộng `source`):

```json
{"device":"bed1_den","room":"bed1","action":"ON","source":"man_hinh","id":"a1b2c3"}
```

### Luật

* **Retained hết** nhánh trạng thái. App dùng `isCleanSession = true`
  ([MqttDien.kt:94](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt#L94)) nên không retained
  thì mở app ra là trắng cho tới vòng poll kế.
* **`online:false` ⇒ `is_on:null`.** Không ngoại lệ (mục 3.3).
* **`nguon`** ∈ `khoi_dong` | `lenh` | `thiet_bi` — `thiet_bi` nghĩa là phát hiện qua poll, tức
  có người bấm công tắc cơ hoặc bật từ app Smart Life.
* **Danh sách thiết bị do `congtac/info` quyết định**, app không hard-code `bed1_den` ở đâu cả.
  Đây là thứ làm cho việc thêm công tắc thứ ba sau này không phải build lại APK.
* **`congtac/trangthai = offline` đè lên mọi thứ**: các topic retained lúc đó chỉ là ảnh chụp lúc
  bridge còn sống, app phải coi là không đáng tin.
* **Thiết bị lạ thì im lặng.** `smarthome/control` là bus dùng chung; bridge gặp `device` không
  phải của mình thì `logger.debug` rồi bỏ qua, **không** publish gì lên `cmd/result`. Mai mốt
  ESP32 sở hữu `kitchen_den` thì log không đầy báo động giả.
* **`TANG`/`GIAM`** → `ket_qua:"bo_qua"`, lý do *"công tắc chỉ có bật/tắt"*. Tuyệt đối không coi
  `TANG` là `ON`.

---

## 5. Các giai đoạn

### G0 — Xác minh nốt · CỔNG CHẶN · 1 buổi

Phần lớn đã xong (mục 2). Còn ba việc, đều cần tay người, đều chỉ đọc. Script tạm đặt ở
`scripts/congtac_g0/` theo đúng kiểu `scripts/cuktech_g0/` (xong thì phần dùng được chuyển sang
`congtac_bridge/driver.py`, gói thô thành fixture test, rồi xoá thư mục).

* **G0-a — `.100` là công tắc hay là camera?** Rút điện công tắc đèn → quét lại cổng 6668 toàn
  dải. `.100` biến mất = đúng nó. Vẫn còn = `.100` là camera, và công tắc đèn chưa tìm thấy.
* ~~**G0-b — đặt IP tĩnh.**~~ **KHÔNG CẦN NỮA** (21/11): bridge tự tìm thiết bị theo mã cố
  định (`gwId` / `did`), DHCP đổi IP bao nhiêu lần cũng được. Xem
  [`scripts/congtac_bridge/README.md`](../scripts/congtac_bridge/README.md) mục *Tự tìm thiết
  bị*. Phần dưới giữ lại phòng khi muốn tắt tự dò (`MANG=`) và quay về IP cố định.
* **G0-b (cũ) — đặt IP tĩnh.** `192.168.0.1` → DHCP → Address Reservation → gán MAC
  `84:7a:b6:1f:95:fb` và `00:50:c2:ec:bf:27` vào hai IP **ngoài dải cấp động** (xem DHCP Settings,
  thường `.100–.199` — tức là hai IP hiện tại đều đang nằm trong dải động và **sẽ đổi**). Rút
  điện hai công tắc 5 giây cho xin lease mới. TL-WR840N mất hết reservation khi reset về mặc
  định — ghi vào `CAU-HINH.txt`.
* **G0-c — kênh nào là quạt.** Chạy vòng hỏi `cmd 2` mỗi 2 giây, trong lúc đó bật tắt quạt bằng
  nút trên tường. Kênh nào đổi giữa `0` và `255` là kênh đó. Ghi lại giá trị BẬT thật (`255` là
  thường thấy, nhưng đừng tin sẵn).

**Điểm dừng:** G0-a hỏng (không tìm ra công tắc Tuya) thì dừng nhánh đèn và báo lại.

### G1 — Lấy `local_key` của đèn · 1 buổi, có thể tắc

`device_id` đã có (`eb74553feb16595327lzam`) và protocol đã biết (3.3). Chỉ còn `local_key`, và
nó bắt buộc đi qua tài khoản cloud một lần.

1. Đèn phải đã nằm trong app Smart Life. **Ghi lại tài khoản đó đăng ký ở vùng nào** — quyết định
   bước 3.
2. `iot.tuya.com` → Cloud → Development → **Create Cloud Project**.
3. **Chọn Data Center — bẫy số một.** Tài khoản đăng ký ở Việt Nam thường rơi vào **Central
   Europe**; tài khoản cũ hoặc dùng email có khi ở **Western America**. Chọn sai thì wizard chạy
   trót lọt nhưng **danh sách thiết bị rỗng**, không một thông báo lỗi nào. Thử theo thứ tự
   `eu` → `us` → `cn`; tạo thêm project ở DC khác không mất gì.
4. Service API: bật `IoT Core`, `Authorization Token Management`, `Smart Home Scene Linkage`.
   Gói **Trial**, hạn ~1 tháng.
5. Devices → Link App Account → Add App Account → hiện QR → Smart Life → *Tôi* → quét.
   Tab Devices phải thấy đèn; không thấy = sai DC, quay lại bước 3.
6. Trong venv ở thư mục scratchpad (**đừng chạy trong `D:\SmartHomeApp`**):
   `pip install tinytuya` rồi `python -m tinytuya wizard`.
   Nó sinh `devices.json`, `tuya-raw.json`, `snapshot.json`.
7. Chép **đúng hai giá trị** `id` và `key` sang `.env` của bridge. Không chép file.

> **`devices.json` chứa `local_key` của MỌI thiết bị Tuya trong tài khoản**, kể cả camera. Repo
> này công khai trên GitHub. Thêm `devices.json`, `snapshot.json`, `tuya-raw.json` vào
> `scripts/.gitignore` **trước khi chạy wizard**, không phải sau.

**Hai thứ phải ghi vào README vì vài tháng sau không ai nhớ:**
`local_key` **đổi mỗi lần ghép lại thiết bị** (xoá khỏi Smart Life rồi add lại, hoặc reset công
tắc) — triệu chứng là nối được TCP nhưng giải mã ra rác hoặc `status()` trả `None`. Và **tài khoản
Trial hết hạn không làm đèn ngừng chạy**: cloud chỉ cần lúc *lấy* khoá, bridge chạy hoàn toàn local.

**Nếu tắc quá nửa buổi:** bridge có cờ `DEN_BAT=0` trong `.env` — tắt driver đèn, `congtac/info`
chỉ còn quạt, app tự thu lại (vì không hard-code). Lên quạt trước, cắm đèn vào sau bằng cách sửa
`.env` + `docker compose up -d`, **không sửa một dòng Kotlin nào**.

### G2 — `scripts/congtac_bridge/` · 1–1,5 buổi

```
scripts/congtac_bridge/
├── Dockerfile
├── docker-compose.yml
├── cau_hinh.mau.env       # commit; KHÔNG có mật khẩu/khoá
├── .gitignore
├── README.md              # tiếng Việt: bảng topic + lệnh cài + triệu chứng lỗi
├── congtac_bridge.py      # MQTT, hàng đợi lệnh, vòng poll
├── driver.py              # Driver (ABC) + TuyaDriver + CozyLifeDriver
└── test_driver.py         # parse payload CozyLife từ gói thật bắt ở G0
```

Tách `driver.py` ra để phần dễ sai nhất test được mà không cần broker lẫn phần cứng — đúng lý lẽ
`KE-HOACH-CUKTECH.md` đã dùng cho `giai_ma.py`.

**Khác `server_collector` ở phần Docker** — những đặc quyền kia là để đọc phần cứng host, bridge
không cần gì cả:

| `server_collector` | `congtac_bridge` |
|---|---|
| `pid: host`, `network_mode: host` | **không** |
| mount `/`, `/sys`, `docker.sock` | **không mount gì** |
| `user: "0:0"` | user thường (`useradd -r -u 10001`) |
| `MQTT_BROKER=127.0.0.1` | `MQTT_BROKER=192.168.0.50` |

> Bẫy đã ghi sẵn ở `CAU-HINH.txt`: container **không** dùng network host thì `127.0.0.1` là chính
> container, không phải broker. Bridge chỉ **mở kết nối TCP ra**, nên bridge network là đủ.

Giữ nguyên từ khuôn: `FROM python:3.12.7-slim` (ghim, không `latest`),
`pip install --no-cache-dir paho-mqtt==2.1.0 tinytuya==<ghim lúc cài, ≥1.13>`,
`restart: unless-stopped`, `mem_limit: 128m`, `cpus: 0.5`, `no-new-privileges:true`,
log `max-size: 2m / max-file: 3`, `PYTHONUNBUFFERED=1`.

**Cấu trúc code:**

```python
class KetQuaDoc(NamedTuple):
    online: bool
    is_on: bool | None          # None khi KHONG doc duoc - khong doan
    ly_do_loi: str | None       # cau tieng Viet doc duoc

class Driver(ABC):
    def doc(self) -> KetQuaDoc
    def dat(self, bat: bool) -> KetQuaDoc   # dat XONG doc lai -> tra trang thai THAT
    def dao(self) -> KetQuaDoc              # doc() truoc; doc hong thi nem, khong doan
    def mo_ta(self) -> dict                 # mot phan tu cua congtac/info
```

Vòng chính — **một luồng duy nhất được chạm thiết bị**:

```
while True:
    han = monotonic() + POLL_INTERVAL
    while monotonic() < han:
        lenh = hang_doi.get(timeout=0.2)   # Empty -> continue
        xu_ly_lenh(cl, lenh)               # thuc thi + publish state NGAY
    for d in drivers:
        publish_neu_doi(cl, d, d.doc(), "thiet_bi")
    cl.publish(f"{GOC}/capnhat", now, retain=True, qos=1)
```

**Không dùng `asyncio`**, dù tài liệu gốc đề xuất. Bài toán ở đây là *tuần tự hoá* (Tuya một kết
nối, CozyLife ít slot), không phải *song song*; `tinytuya` vốn đồng bộ nên asyncio chỉ dẫn tới
`run_in_executor`, tức vẫn là thread pool cộng thêm một lớp khái niệm; `paho` đã có luồng mạng
riêng; và hai collector hiện có đều là `while True` đồng bộ — người đọc sau chỉ phải học một kiểu.
Xét lại khi vượt ~15 thiết bị.

**Bảy bẫy kỹ thuật phải xử lý:**

1. **Không bao giờ gọi driver trong callback Paho.** Callback chạy trên luồng mạng của Paho;
   block ở đó là rớt keepalive → broker đá client → reconnect quay vòng, triệu chứng giống hệt
   mất sóng Wi-Fi. `on_message` chỉ parse rồi `hang_doi.put()`.
2. **Tuya chỉ cho MỘT kết nối local** (TCP 6668). Dùng persistent socket
   (`set_socketPersistent(True)`, `set_socketTimeout(3)`, `set_socketRetryLimit(1)`) và **tự mở
   lại sau 2 lần đọc lỗi liên tiếp** — thiết bị reboot thì socket chết lặng. Hệ quả phải ghi vào
   README: **app Smart Life trên điện thoại sẽ chuyển sang điều khiển qua mây** (vẫn chạy, chậm
   hơn). Không ghi thì sau này có người tưởng là lỗi.
3. **CozyLife: `close()` trong `finally`, mọi nhánh.** Slot hữu hạn (~4, dọn chậm); quên đóng thì
   sau vài chục lệnh quạt câm tới khi rút điện. Đây là lỗi dễ mắc nhất của driver này.
4. **`sn` là chuỗi 13 chữ số, tăng dần mỗi lệnh; `pv:0` không được thiếu** (mục 2b). Đọc trả lời
   thì khớp đúng `sn` chứ đừng lấy gói đầu tiên vớ được — một số firmware tự đẩy gói report.
5. **`POLL_INTERVAL=10`, `POLL_TIMEOUT=3`.** Dưới 5 s thì Tuya bắt đầu dễ lỗi và CozyLife ăn slot;
   trên 15 s thì bấm công tắc cơ xong nhìn tablet thấy sai tới 15 giây — đúng thứ làm người dùng
   mất tin vào màn hình. Timeout để 3 chứ đừng 5–10: hai thiết bị chết cùng lúc thì mỗi vòng dài
   thêm `2 × POLL_TIMEOUT`.
6. **Mất điện: 3 vòng lỗi liên tiếp mới coi là offline** (≈30 s). Trước ngưỡng đó giữ trạng thái
   cũ nhưng không cập nhật `doc_luc`. Cắm lại thì tự hồi, không cần restart container.
7. **Múi giờ.** Image slim mặc định UTC — dùng `timezone(timedelta(hours=7))` như hai script kia.
   Và `MQTT_CLIENT_ID` phải khác mọi collector khác, trùng là broker đá cả hai
   ([MqttDien.kt:67](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt#L67) đã cảnh báo).

**Điểm dừng bắt buộc: thấy JSON đúng trên `congtac/#` bằng `mosquitto_sub` rồi mới động vào Kotlin.**

### G3 — Phía app · 1,5 buổi

| File | Việc |
|---|---|
| `data/congtac/CongTacModels.kt` *(mới)* | `CongTacTopic`, `CongTac`, `TrangThaiCongTac`, `CongTacState` + `gop()`, `LenhCongTac`, `OThietBi`. Khởi tạo **rỗng**, không `DemoData` |
| `data/dien/MqttDien.kt` | `_congTac` StateFlow; gốc thứ 4; nhánh `congtac/` trong `messageArrived`; **sửa lỗi `grantedQos`**; `guiLenh()` trả `KetQuaGui` thay vì `Boolean` |
| `vm/HomeViewModel.kt` | thu trạng thái từ broker vào `_state.rooms`; `gatCongTac`/`datCongTac`; hàng chờ + hết giờ; `thongBao`; `jsonCongTac` |
| `data/giongnoi/CongCuNha.kt` | `HanhDongNha.laThat`/`thietBiThat`; cờ `that` trong kết quả; chặn `DAO` khi không rõ; `"cong_tac"` vào `MUC_HOP_LE` |
| `vm/TroLyGiongNoi.kt` | `ghiViecDaLam` đọc cờ `that` thay `IS_DEMO`; `loiDan()` liệt kê thật/mẫu |
| `ui/components/Tiles.kt` | `DeviceSwitchRow` nhận thêm `o: OThietBi? = null` |
| `ui/screens/RoomDetailScreen.kt`, `DevicesScreen.kt`, `HomeScreen.kt` | nhãn "MẪU" theo từng thẻ thay vì theo màn |
| `data/DemoData.kt` | chỉ sửa **comment** của `IS_DEMO` — nay nó chỉ còn nói về số liệu tổng quan |

Không thêm thư viện, không thêm ô cấu hình ở màn Cài đặt, không đụng `AndroidManifest`.

**a) Lỗi sẵn có phải sửa cùng lúc.**
[`MqttDien.kt:151`](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt#L151) chỉ kiểm
`cap[0] == 0x80`, mà `cap[0]` là `dien/#`. Broker từ chối `wifi/#` hay `server/#` thì app **im
lặng** — màn hình trống mà không ai nói vì sao. Thêm gốc thứ tư làm chỗ hở rộng thêm. Sửa: dựng
một danh sách `GOC_CAN_DOC` có tên, sinh `qoss` từ nó để không bao giờ lệch độ dài, duyệt cả mảng
`grantedQos`, nêu đúng tên gốc bị từ chối, và bắt luôn ca `cap.size != GOC_CAN_DOC.size`.
Broker nhà chưa có `acl_file` nên lỗi này chưa bao giờ bắn — nhưng nó sẽ bắn đúng vào ngày siết ACL.

**b) Chỗ khó nhất — "nửa thật nửa mẫu".**
`DemoData.IS_DEMO` hiện là một cờ toàn cục dùng ở 8 chỗ, trong đó
[`TroLyGiongNoi.kt:355`](../app/src/main/java/vn/smarthome/vm/TroLyGiongNoi.kt#L355) gắn cảnh báo
*"chưa nối thiết bị thật"* cho **mọi** lệnh. Sau thay đổi này chỉ `bed1/den` và `bed1/quat` là thật.

Nguồn sự thật duy nhất: **`CongTacState.laThat(phong, thietBi)`**, đọc từ `congtac/info`.
Không thêm trường vào `RoomUi` — `RoomUi` do `DemoData.rooms()` dựng ra, nhét cờ vào đó là
hard-code danh sách thiết bị thật trong chính file dữ liệu mẫu; và "thật/mẫu" là thuộc tính của
*từng thiết bị*, không phải của phòng, nên thêm vào `RoomUi` là mở đầu một danh sách cờ chỉ dài ra.

Bảng hiển thị:

| Tình huống | Màn hình vẽ |
|---|---|
| Không có trong `congtac/info` | như hiện nay + chip nhỏ **MẪU** trên thẻ |
| Thật, `congtac/trangthai != online` | **—** · "Bridge chưa chạy", ẩn Switch |
| Thật, bridge ok, `online:false` | **—** · "Mất kết nối lúc …", ẩn Switch |
| Thật, online, `is_on:null` | **—** · "Chưa đọc được công tắc", ẩn Switch |
| Thật, online, có giá trị | Switch bình thường |
| Thật, đang chờ xác nhận | Switch ở vị trí mong muốn, mờ, "Đang gửi…" |

**c) Luồng gạt công tắc.** Một chiều duy nhất: `congtac/thietbi/<id>` → `_state.rooms`. Không màn
nào phải đổi cách đọc trạng thái; chúng chỉ nhận thêm `OThietBi` để biết vẽ `—` hay vẽ Switch.
`toggleLight`/`toggleFan` đi qua `gatCongTac`, và nó dừng lại **trước khi publish** ở bốn ca:
không phải thiết bị thật (→ đổi state như cũ, hết), bridge không `online`, thiết bị `offline`, và
`DAO` khi `is_on == null` (→ *"Chưa biết đang bật hay tắt, không đảo được"*). Gửi xong thì vào
hàng chờ kèm `id`; gỡ khi thấy `tra_loi_id` khớp; quá **6 giây** không thấy thì gỡ và báo
*"không phản hồi"*. Chốt lại con số 6 bằng số đo thật ở G4 — **sửa chỉ tiêu theo số đo, không sửa
số đo theo chỉ tiêu**.

**d) Giọng nói.** Không thêm tool thứ tư — `dieu_khien_thiet_bi` đã đủ, và
[`CongCuNha.kt:301`](../app/src/main/java/vn/smarthome/data/giongnoi/CongCuNha.kt#L301) ghi rõ
*"model gọi đúng hơn khi ít công cụ"*. Nhưng thêm `"cong_tac"` vào `MUC_HOP_LE`, vì
`trang_thai_nha` trộn thật lẫn mẫu nên câu *"đèn phòng ngủ 1 có sáng thật không"* đang được trả
lời bằng số mẫu.

Ba chỗ nữa: `dieuKhien()` hiện gọi `if (guiMqtt) nha.guiMqtt(...)` **sau khi** đã đổi state —
với thiết bị thật thì `datDen` đã publish rồi, bắn thêm là hai lệnh cho một câu nói, công tắc nháy
hai lần; `chupTrangThai()` đi thẳng vào `system_instruction` nên `is_on == null` phải in
*"không rõ"* chứ không in "tắt"; và câu xác nhận nên là *"đã gửi lệnh bật"* chứ không phải *"đã
bật"* — lúc tool trả về, lệnh mới rời broker.

Cờ `CauHinhGiongNoi.guiMqtt` **giữ mặc định `false` nhưng đổi nghĩa**: lệnh vào thiết bị thật luôn
được gửi bất kể cờ (nếu không thì trợ lý im lặng không bật được đèn và không ai hiểu tại sao);
cờ chỉ còn chi phối thiết bị **mẫu**. Đổi nhãn ở màn Cài đặt thành *"Gửi cả lệnh của thiết bị mẫu
ra MQTT (để gỡ rối)"*, và sửa dòng chữ
[`SettingsScreen.kt:549`](../app/src/main/java/vn/smarthome/ui/screens/SettingsScreen.kt#L549) đang
nói *"chưa thiết bị nào nghe topic này"* — từ nay có rồi.

**e) Test** (Robolectric, `@Config(sdk = [34])`, vì `org.json` là thư viện Android):
`CongTacStateTest` — `info` chuẩn → `laThat` đúng; phần tử hỏng thì bỏ riêng chứ không vứt cả
mảng; **`congtac/trangthai = "ok"` không bị đem đi parse JSON**; thiếu khoá `is_on` → `null` chứ
không `false`; `"is_on": null` tường minh → `null`; payload rỗng → giữ nguyên `cur`.
`CongCuNhaTest` bổ sung: bật đèn `bed1` → `that = true`; bật đèn `living` → `that = false`;
`guiMqtt=true` + thiết bị thật → **không** gửi hai lần; `DAO` khi `dangBat == null` + thiết bị
thật → lỗi, nhưng thiết bị mẫu thì vẫn bật như cũ (không hồi quy).
Hàng chờ tách thành lớp thuần `HangChoCongTac` để test được không cần dựng `HomeViewModel`.

### G4 — Nghiệm thu và tài liệu · nửa buổi

Xem mục 6. Xong thì cập nhật `README.md` (bảng "Cái gì đã có" + một mục như mục Điện năng),
`CAU-HINH.txt` (bảng topic, IP tĩnh, lệnh cài bridge, ghi chú reservation của TL-WR840N),
`scripts/congtac_bridge/README.md`, và nâng `versionCode`/`versionName`.

Soạn `docs/KE-HOACH-CONGTAC.md` từ chính file này, theo khuôn `docs/KE-HOACH-CUKTECH.md` — **bỏ
`local_key` ra ngoài**; `device_id` thì giữ được vì một mình nó không điều khiển được gì, nhưng
repo công khai nên cân nhắc để cả nó trong `.env`.

---

## 6. Nghiệm thu

Đặt sẵn `PASS='<mật khẩu iot>'`. Mở `mosquitto_sub` ở một cửa sổ **trước khi** `mosquitto_pub` ở
cửa sổ khác.

```bash
# Toan nhanh, chay nen suot buoi nghiem thu
mosquitto_sub -h 192.168.0.50 -u iot -P "$PASS" -t 'congtac/#' -v

# Danh sach thiet bi - phai khop .env
mosquitto_sub -h 192.168.0.50 -u iot -P "$PASS" -t 'congtac/info' -C 1

# Bat den, bang DUNG payload app dang ban
mosquitto_pub -h 192.168.0.50 -u iot -P "$PASS" -t 'smarthome/control' -q 1 \
  -m '{"device":"bed1_den","room":"bed1","action":"ON","source":"test","id":"t1"}'

# Dao - phai doc truoc roi moi dao
mosquitto_pub ... -m '{"device":"bed1_den","room":"bed1","action":"DAO","source":"test","id":"t2"}'

# Action khong ho tro -> ket_qua "bo_qua", va quat KHONG duoc bat
mosquitto_pub ... -m '{"device":"bed1_quat","room":"bed1","action":"TANG","source":"test","id":"t3"}'

# Thiet bi la -> IM LANG, congtac/cmd/result khong co gi
mosquitto_pub ... -m '{"device":"kitchen_den","room":"kitchen","action":"ON","source":"test","id":"t4"}'

# JSON hong -> log warning, bridge khong chet
mosquitto_pub ... -m 'khong phai json'

# LWT
ssh serverubuntu@192.168.0.50 'cd ~/congtac_bridge && docker compose stop'
mosquitto_sub -h 192.168.0.50 -u iot -P "$PASS" -t 'congtac/trangthai' -C 1   # phai ra offline
```

**Đối chiếu với thực tế — phần quan trọng nhất, và phải bấm giờ:**

| Việc làm bằng tay | Kỳ vọng | Ghi gì vào README |
|---|---|---|
| Bấm công tắc cơ trên tường | ≤ `POLL_INTERVAL + 1` s, `is_on` đổi, `nguon:"thiet_bi"` | trễ thật đo được, cả hai thiết bị |
| Bật từ app Smart Life cùng Wi-Fi | như trên; **đồng thời xem log bridge** — báo lỗi kết nối suốt lúc Smart Life đang mở là đã trúng bẫy "một khe Tuya" | có xung đột hay không |
| Rút điện công tắc quạt | sau ~30 s: `online:false`, `is_on:null`, `ly_do_loi` có nội dung. Cắm lại tự hồi | thời gian phát hiện thật |
| `docker compose stop` | `congtac/trangthai = offline` ngay (LWT) | — |
| Mở app lúc bridge chưa chạy | "mất kết nối" — **không** vẽ công tắc đang bật | — |
| Gạt trên tablet | UI lật ngay, xác nhận < 1 s, không nhảy ngược | trễ xác nhận thật → chốt lại hạn 6 s |
| Rút điện thiết bị rồi gạt | sau 6 s quay lại + báo không phản hồi | — |
| Nói *"bật đèn phòng ngủ một"* | đèn sáng thật, **không** còn dòng xám "chưa nối thiết bị thật" | — |
| Nói *"bật đèn phòng khách"* | vẫn còn dòng xám đó | — |

Thử trên máy thật SM-T225, cả theme sáng và tối.

---

## 7. Rủi ro, xếp theo mức nghiêm trọng

1. **Không lấy được `local_key`** (G1). Khả năng cao, chặn toàn bộ nhánh đèn. Lối ra: `DEN_BAT=0`,
   lên quạt trước.
2. **Lần đầu tiên MQTT điều khiển được vật thể trong nhà, mà Mosquitto chưa có `acl_file`.**
   Trước nay mất mật khẩu `iot` là lộ số liệu; từ nay là **bật tắt được đèn phòng ngủ**, và mật
   khẩu đó nằm trong máy tính bảng treo tường. Đây là đổi về chất, không phải về lượng. Ghi ngay
   vào kế hoạch siết ACL: `iot` chỉ đọc; bridge có tài khoản riêng được ghi `congtac/#` và đọc
   `smarthome/control`; tablet được ghi `smarthome/control` và đọc bốn gốc. Và đó đúng là ngày mà
   chỗ sửa `grantedQos` ở G3 mới cho biết vì sao màn hình trống.
3. **`.100` hoá ra là camera** (G0-a chưa làm) — thì công tắc đèn chưa hề được tìm thấy và cả
   nhánh Tuya phải dò lại.
4. **Tuya một kết nối local** — bridge và Smart Life giành khe. Triệu chứng là lỗi ngắt quãng
   không tái hiện được, rất dễ đổ oan cho Wi-Fi. Log phải phân biệt "từ chối kết nối" với "timeout".
5. **`local_key` đổi sau khi ghép lại thiết bị** — gãy im lặng sau vài tháng, lúc không ai nhớ.
   README phải có mục "triệu chứng → nguyên nhân → chạy lại wizard".
6. **UI nhảy ngược hoặc nói dối** — optimistic mà không reconcile, hoặc `DAO` dựa vào cache.
7. **Chặn luồng MQTT** — gọi `tinytuya` trong callback Paho.
8. **IP đổi** sau mất điện cả nhà: hai IP hiện tại đều nằm trong dải DHCP động. G0-b phải làm.
9. **CozyLife hết connection slot** vì quên `close()`.
10. **Lộ bí mật**: `local_key`, `MQTT_PASS`, và đặc biệt `devices.json` do wizard sinh ra.

---

## 8. Thứ tự và điểm dừng

```
G0 xac minh ─► G1 local_key ─► G2 bridge ─► G3 app ─► G4 nghiem thu
     │              │                │
     │              └─► tac: DEN_BAT=0, len quat truoc
     └─► .100 la camera: dung nhanh den, bao lai
                                     └─► chua thay JSON dung thi chua dong vao Kotlin
```

Hai điểm dừng bắt buộc: **hết G0 mới chốt kiến trúc**, và **thấy JSON đúng trên `congtac/#` bằng
`mosquitto_sub` rồi mới viết dòng Kotlin đầu tiên**. Làm ngược lại thì vẽ xong giao diện mới biết
không có số để đổ vào.

---

## Các file chính

**Mới** — `scripts/congtac_g0/` (tạm), `scripts/congtac_bridge/` (7 file),
`app/src/main/java/vn/smarthome/data/congtac/CongTacModels.kt`,
`app/src/test/java/vn/smarthome/CongTacStateTest.kt`, `docs/KE-HOACH-CONGTAC.md`

**Sửa** — `app/src/main/java/vn/smarthome/data/dien/MqttDien.kt` ·
`vm/HomeViewModel.kt` · `data/giongnoi/CongCuNha.kt` · `vm/TroLyGiongNoi.kt` ·
`ui/components/Tiles.kt` · `ui/screens/{RoomDetail,Devices,Home,Settings}Screen.kt` ·
`data/DemoData.kt` (chỉ comment) · `app/src/test/java/vn/smarthome/CongCuNhaTest.kt` ·
`scripts/.gitignore` · `README.md` · `CAU-HINH.txt` · `app/build.gradle.kts` (version)
