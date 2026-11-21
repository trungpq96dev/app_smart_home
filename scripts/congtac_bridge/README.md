# congtac_bridge — 2 công tắc Wi-Fi phòng ngủ 1 ↔ MQTT

Chạy trong Docker **trên serverubuntu**. Nghe lệnh ở `smarthome/control` (topic app đã dựng sẵn
cho trợ lý giọng nói), nói chuyện thẳng với công tắc trong LAN, rồi publish trạng thái thật lên
`congtac/#`. App chỉ đọc nhánh `congtac/` và chỉ ghi `smarthome/control`.

Kế hoạch đầy đủ: `docs/KE-HOACH-CONGTAC.md`. Số liệu đo thiết bị: `scripts/congtac_g0/README.md`.

| Thiết bị | Hệ | Giao thức | Cần gì |
|---|---|---|---|
| Đèn Phòng ngủ 1 | Smart Life / Tuya | TCP 6668, protocol 3.3 | `device_id` + **`local_key`** |
| Quạt Phòng ngủ 1 | CozyLife | TCP 5555, JSON + CRLF | không cần khoá nào |

---

## Topic

| Topic | Retained | Nội dung |
|---|---|---|
| `congtac/info` | ✔ | danh sách thiết bị bridge đang quản |
| `congtac/thietbi/<id>` | ✔ | trạng thái một công tắc |
| `congtac/trangthai` | ✔ | `online` / `offline` — **LWT** |
| `congtac/capnhat` | ✔ | mốc thời gian mỗi vòng poll |
| `congtac/cmd/result` | ✘ | kết quả một lệnh vừa thực thi |
| `smarthome/control` | ✘ | **bridge nghe** — lệnh từ app |

`<id>` = `<phòng>_<thiết bị>` → `bed1_den`, `bed1_quat`. Khớp đúng chuỗi mà
`HomeViewModel.kt:474` đang sinh ra, nên không cần bảng ánh xạ ở đâu cả.

```json
// congtac/thietbi/bed1_quat
{"id":"bed1_quat","phong":"bed1","loai":"quat","online":true,"is_on":false,
 "nguon":"thiet_bi","doc_luc":"2026-11-21 00:40:32","doi_luc":"2026-11-21 00:40:32",
 "tra_loi_id":null,"ly_do_loi":null}
```

* `nguon` ∈ `khoi_dong` | `lenh` | `thiet_bi`. `thiet_bi` nghĩa là phát hiện qua poll — tức có
  người bấm công tắc cơ hoặc bật từ app Smart Life.
* `tra_loi_id` chép lại `id` của lệnh vừa thực thi. App dùng nó để gỡ cờ "đang chờ" — **so chuỗi
  chứ không so mốc thời gian**, vì đồng hồ server và đồng hồ tablet không bao giờ khớp.
* **`online:false` thì `is_on` luôn là `null`**, không giữ giá trị cũ, không đặt `false`. Xem mục
  *Không bịa trạng thái* bên dưới.

### Lệnh nhận được

| `action` | Xử lý |
|---|---|
| `ON` / `OFF` | đặt → **đọc lại từ thiết bị** → publish trạng thái thật |
| `DAT` + `value` | `on/off/true/false/1/0/bat/tat`; không hiểu → `ket_qua:"loi"` |
| `DAO` | **đọc trạng thái thật trước rồi mới đảo**; đọc hỏng → `loi`, không làm gì cả |
| `TANG` / `GIAM` | `ket_qua:"bo_qua"` — công tắc chỉ có bật/tắt |
| thiết bị không phải của mình | **im lặng**, không publish gì |

`smarthome/control` là bus dùng chung. Mai mốt ESP32 sở hữu `kitchen_den`; nếu bridge bắn "không
biết thiết bị" cho mọi lệnh không phải của mình thì log và `cmd/result` đầy báo động giả, và app
không phân biệt được "không ai nhận" với "có người nhận nhưng không phải tôi".

---

## Cài trên serverubuntu

```
scp -r scripts/congtac_bridge serverubuntu@192.168.0.50:~/
ssh serverubuntu@192.168.0.50
cd ~/congtac_bridge
cp cau_hinh.mau.env .env
nano .env            # điền MQTT_PASS và DEN_LOCAL_KEY
chmod 600 .env
docker compose up -d --build
docker compose logs -f
```

Kiểm broker đã có số thật:

```
mosquitto_sub -h 192.168.0.50 -u iot -P '<pass>' -t 'congtac/#' -v
```

Thử một lệnh, bằng **đúng payload app đang bắn**:

```
mosquitto_pub -h 192.168.0.50 -u iot -P '<pass>' -t 'smarthome/control' -q 1 \
  -m '{"device":"bed1_quat","room":"bed1","action":"ON","source":"test","id":"t1"}'
```

### Khác `server_collector` ở phần Docker

Những đặc quyền của `server_collector` là để đọc phần cứng host; bridge này không cần gì cả.

| `server_collector` | `congtac_bridge` |
|---|---|
| `pid: host`, `network_mode: host` | **không** |
| mount `/`, `/sys`, `docker.sock` | **không mount gì** |
| `user: "0:0"` | user thường (uid 10001) |
| `MQTT_BROKER=127.0.0.1` | `MQTT_BROKER=192.168.0.50` |

> Trong container **không** dùng network host thì `127.0.0.1` là chính container, không phải
> broker. Để `127.0.0.1` ở đây là triệu chứng "connection refused" lúc 2 giờ sáng.

Bridge chỉ **mở kết nối TCP ra**, nên bridge network là đủ. Chỉ cần host network nếu muốn bridge
tự dò IP bằng quảng bá UDP — **đừng làm**, đặt IP tĩnh trên router thì đơn giản và chẩn đoán được
hơn nhiều.

---

## Tự tìm thiết bị khi DHCP đổi IP

**Không phải đặt IP tĩnh trên router.** Bridge nhận diện thiết bị bằng **mã cố định**, IP chỉ
là gợi ý để thử trước cho nhanh:

| Hệ | Mã cố định | Lấy ở đâu |
|---|---|---|
| Tuya | `DEN_DEVICE_ID` (gwId) | gói quảng bá UDP 6667 — không cần tài khoản cloud |
| CozyLife | `QUAT_DID` (did) | trả lời `cmd 0` |

Mất dấu thiết bị thì bridge quét `MANG` ở cổng đặc trưng của hệ đó, hỏi từng máy và **đối
chiếu mã** rồi mới nhận, sau đó tự cập nhật IP và publish lại `congtac/info`.

> **Thấy cổng mở KHÔNG đủ để kết luận.** Cổng 5555 cũng là cổng ADB của Android; cổng 6668 thì
> camera Tuya trong nhà cũng mở. Nên phải xác minh bằng mã: CozyLife so `did`, Tuya thì thử
> giải mã bằng `device_id` + `local_key` (khoá chỉ mở được đúng một thiết bị). Không khai mã
> thì bridge **từ chối nhận**, thà chịu offline còn hơn bật nhầm đồ của người khác.

`CHO_QUET=120` chặn quét liên tục: thiết bị mất điện thật thì không nên quét lại mỗi vòng poll.
Để `MANG` rỗng là tắt hẳn tính năng — lúc đó IP trong `.env` phải luôn đúng.

Đo thật 21/11: `.env` đặt `QUAT_IP=192.168.0.177` (không tồn tại) → bridge quét và tìm đúng
quạt ở `.103` trong **1,4 giây**.

> `docker compose restart` **không đọc lại `.env`** — nó dùng lại container cũ với biến môi
> trường cũ. Sửa `.env` xong phải `docker compose up -d --force-recreate`.

---

## Chưa có `local_key` thì vẫn chạy được quạt

Đặt `DEN_BAT=0` trong `.env`. Bridge bỏ qua hẳn đèn, `congtac/info` chỉ liệt kê quạt, và app tự
thu lại — vì app **không hard-code** tên thiết bị nào, nó dựng danh sách từ `congtac/info`.

Lấy được khoá rồi thì sửa `.env` + `docker compose up -d`, **không sửa một dòng Kotlin nào**.

---

## Không bịa trạng thái

Đọc không được thì `is_on` là `null`, **không phải** `false`.

Một cái công tắc gạt được, nằm ở thế TẮT, trong khi app không hề biết đèn đang sáng hay không, là
lời nói dối tốn ít pixel nhất trong cả dự án. App nhận `null` thì vẽ `—` và nói rõ lý do, giống
cách `ServerMetric.cpuTempC: Float?` đang làm với nhiệt độ.

Hệ quả: `DAO` **phải đọc trạng thái thật trước rồi mới đảo**. Cache cũ 10 giây mà trong 10 giây
đó có người bấm công tắc cơ thì đảo ngược ý người dùng — triệu chứng *"bấm bật thì nó tắt"*,
rất khó chịu và rất khó tái hiện.

---

## Số đo thật — 21/11/2026

Chạy trên serverubuntu, quạt CozyLife `192.168.0.103`, `QUAT_DPID=1` (đã xác minh bằng cách
bật tắt thật — quạt quay rồi dừng):

| Lệnh | Độ trễ tới `congtac/cmd/result` |
|---|---|
| `ON` | **96 ms** |
| `OFF` | **458 ms** |

Tài nguyên: RAM **13,2 / 128 MB**, CPU **0,12 %**, `restarts 0`.

Hạn chờ 6 giây của app rộng hơn số đo xấu nhất 13 lần — không cần siết.

> Lấy `MQTT_PASS` ở **`~/server_collector/.env`**, KHÔNG phải `~/homeserver/.env`: file kia
> đặt tên biến là `MQTT_PASSWORD`, chép nhầm là ra biến rỗng và bridge không nối được broker.

> Server này **không có `mosquitto_sub`**. Muốn soi topic thì chạy tạm bằng chính image của
> bridge (nó có sẵn paho), truyền tài khoản qua `--env-file` để mật khẩu không lọt vào danh
> sách tiến trình.

---

## Triệu chứng → nguyên nhân

| Thấy gì | Gần như chắc là |
|---|---|
| Quạt **im lặng tuyệt đối**, không lỗi, chỉ timeout | `sn` không phải chuỗi 13 chữ số mili-giây, hoặc thiếu `"pv":0`. Đây là cái bẫy đắt nhất của giao thức CozyLife |
| Đèn: `Unexpected Payload` / giải mã ra rác | **`local_key` đã đổi** — xảy ra mỗi lần ghép lại thiết bị (xoá khỏi Smart Life rồi add lại, hoặc reset công tắc). Chạy lại `python -m tinytuya wizard` |
| Đèn: `No response` sau đúng `POLL_TIMEOUT` giây | mất điện, sai IP, **hoặc sai `DEN_VERSION`** — ba thứ này cho triệu chứng giống hệt nhau |
| Đèn: `Connection refused` lúc đang mở app Smart Life | trúng bẫy "một khe Tuya" — xem mục dưới |
| Bridge reconnect quay vòng, nhìn như mất sóng | trùng `MQTT_CLIENT_ID` với collector khác |
| Quạt câm sau vài chục lệnh, rút điện mới sống | hết connection slot vì có chỗ nào quên `close()` |
| Mọi mốc thời gian lệch 7 tiếng | quên `GIO_VN` — image slim mặc định UTC |

### Tuya chỉ cho MỘT kết nối local

Bridge giữ socket persistent tới `6668`. Hệ quả: **app Smart Life trên điện thoại sẽ chuyển sang
điều khiển qua mây** — vẫn chạy, chỉ chậm hơn. Đó là đánh đổi có chủ ý: tablet treo tường mới là
client chính. Ghi ở đây để sau này không ai tưởng là lỗi.

Thiết bị reboot thì socket chết lặng và lỗi mãi không tự khỏi, nên driver tự mở lại sau **2 lần
đọc lỗi liên tiếp**.

---

## Nhịp và ngưỡng

| Biến | Mặc định | Vì sao |
|---|---|---|
| `POLL_INTERVAL` | 10 s | dưới 5 thì Tuya dễ lỗi và CozyLife ăn slot; trên 15 thì bấm công tắc cơ xong nhìn tablet thấy sai tới 15 giây — đúng thứ làm người dùng mất tin vào màn hình |
| `POLL_TIMEOUT` | 3 s | hai thiết bị chết cùng lúc thì mỗi vòng dài thêm `2 × POLL_TIMEOUT`. Đừng để 5–10 |
| `SO_LAN_LOI_OFFLINE` | 3 | ≈30 s. Một vòng lỗi lẻ chỉ là Wi-Fi chớp; tuyên bố offline ngay thì màn hình nhấp nháy suốt ngày |
| `LAM_MOI_RETAIN` | 60 s | publish lại dù không đổi, giữ retained tươi mà broker vẫn sạch |

Bridge **chỉ publish khi trạng thái đổi**, khi vừa thực thi một lệnh, hoặc khi đến hạn làm mới —
không publish mỗi vòng poll.

---

## Vì sao không dùng `asyncio`

Tài liệu gốc đề xuất `asyncio`. Bài toán ở đây là **tuần tự hoá** (Tuya một kết nối, CozyLife ít
slot), không phải song song: chỉ 2 thiết bị, dưới 1 truy vấn/giây. `tinytuya` vốn đồng bộ nên
asyncio chỉ dẫn tới `run_in_executor` — vẫn là thread pool, cộng thêm một lớp khái niệm để gỡ rối.
`paho` đã có luồng mạng riêng. Và hai collector hiện có đều là `while True` đồng bộ, nên người đọc
sau chỉ phải học một kiểu.

Thay vào đó: một `queue.Queue` và **một luồng duy nhất được chạm thiết bị**. Callback MQTT chỉ
parse rồi bỏ vào hàng đợi — gọi driver trong callback là block luồng mạng của Paho, rớt keepalive,
broker đá client, reconnect quay vòng.

Xét lại khi vượt ~15 thiết bị.

---

## Test

Chạy được không cần broker lẫn phần cứng:

```
python scripts/congtac_bridge/test_driver.py
```

Fixture là gói **thật** bắt từ công tắc CozyLife `192.168.0.103` ngày 21/11/2026, không phải gói
tự nghĩ ra.

---

## Bảo mật

`.env` chứa `MQTT_PASS` và `DEN_LOCAL_KEY` → `chmod 600`, đã nằm trong `.gitignore`. Repo này
công khai trên GitHub; chỉ commit `cau_hinh.mau.env`.

`python -m tinytuya wizard` sinh ra `devices.json` / `snapshot.json` / `tuya-raw.json` — chúng
chứa `local_key` của **mọi** thiết bị Tuya trong tài khoản, kể cả camera. Chạy wizard ở thư mục
tạm, đừng chạy trong repo.

> **Đây là lần đầu tiên MQTT điều khiển được vật thể trong nhà.** Mosquitto hiện chưa có
> `acl_file` (`CAU-HINH.txt` dòng 35): bất cứ tài khoản nào đăng nhập được cũng đọc ghi được mọi
> topic. Trước nay mất mật khẩu `iot` là lộ số liệu; từ nay là **bật tắt được đèn phòng ngủ**, và
> mật khẩu đó nằm trong máy tính bảng treo tường. Siết ACL thì: `iot` chỉ đọc; bridge có tài khoản
> riêng được ghi `congtac/#` và đọc `smarthome/control`; tablet được ghi `smarthome/control` và
> đọc bốn gốc số liệu.
