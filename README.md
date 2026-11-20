# SmartHome Android — khung giao diện

App nhà thông minh cho **Samsung Galaxy Tab A7 Lite** nằm ngang, Kotlin + Jetpack Compose.

Ba thứ đã chạy bằng số thật: **số điện** (EVN → MQTT), **hình camera**
(camera Tuya → go2rtc) và **lịch âm** (tính tại chỗ, không cần mạng). Phần còn lại —
đèn, quạt, rèm, cảm biến — vẫn là khung giao diện chạy trên số liệu mẫu: bấm được,
gạt được, đổi được, nhưng chưa nối tới thiết bị nào.

Build: xem [BUILD.md](BUILD.md). Ảnh chụp từng màn: [docs/screenshots](docs/screenshots).

---

## Cái gì đã có

| Màn | Trạng thái |
|---|---|
| Trang chủ | Đủ: header (kèm đồng hồ + ngày âm lịch, bấm vào mở lịch vạn niên) + 4 ô số liệu + thẻ phòng vuốt ngang + 4 ô thao tác nhanh + dải trạng thái |
| Điều khiển phòng | Đủ: 2 công tắc + điều hòa −/+ + 4 chế độ + lịch hẹn |
| Camera an ninh | **Hình thật từ camera Tuya** qua go2rtc. Chưa khai báo go2rtc thì vẫn là ô giữ chỗ |
| Thống kê & Báo cáo | Đủ: 5 tab. **Tab Điện năng dùng số thật từ MQTT**, 4 tab kia là dữ liệu mẫu |
| Thiết bị | Đủ: lưới thẻ thiết bị gom từ các phòng, bấm mở đúng phòng đó |
| Tự động hóa | Đủ: thẻ hệ thống bơm AquaControl (mở thẳng app `vn.aquacontrol`) + các kịch bản tự động |
| WiFi gia đình | 5 tab. **Dùng số thật từ `wifi/#`** khi `wifi_collector` đang chạy; tab Bảo mật và Nâng cao còn nhiều phần là dữ liệu mẫu |
| Server | **Số thật từ `server/#`**: CPU/RAM/ổ đĩa/nhiệt độ/Docker của `serverubuntu`. Chỉ xem, không điều khiển |
| Cài đặt | Tab *Dịch vụ & Kết nối* (ba cột: broker MQTT, go2rtc, Open-Meteo) và tab *Cập nhật hệ thống* (OTA qua GitHub) |
| Tiện ích · Lịch vạn niên | **Đủ và dùng số thật**: lịch âm/dương, Can Chi, ngày lễ, giờ hoàng đạo. Tính tại chỗ nên không cần mạng |
| Cảm biến | Màn trống, nói rõ "sẽ làm ở giai đoạn sau" |
| Trợ lý giọng nói | Nút mic nổi trên mọi màn. **Nói chuyện thật với Gemini Live**, điều khiển thiết bị và chuyển màn bằng giọng nói, trả lời câu hỏi bằng số thật |

### Thao tác thật sự hoạt động

Trạng thái nằm ở `HomeViewModel` chứ không nằm trong từng màn, nên **sống qua lúc chuyển
màn và xoay máy**: bật đèn ở màn chi tiết phòng rồi quay về Trang chủ sẽ thấy thẻ phòng đó
đổi thành "Đèn: Bật".

| Thao tác | Kết quả |
|---|---|
| Bấm menu trái | Đổi màn |
| Vuốt thẻ phòng | Đổi phòng, chấm chỉ số chạy theo |
| Bấm "Điều khiển" trên thẻ phòng | Mở màn chi tiết đúng phòng đó |
| Gạt Đèn phòng / Quạt trần | Đổi trạng thái, icon đổi màu theo |
| Bấm − / + Điều hòa | 16–30 °C, kẹp hai đầu |
| Chọn Ngủ / Nhà / Vắng nhà / Tùy chỉnh | Chip đổi trạng thái chọn |
| Bấm 4 ô thao tác nhanh | Bật/tắt, vòng tròn icon sáng/tối |
| Chọn camera bên trái | Đổi luồng đang xem; luồng cũ bị cắt ngay |
| Đổi tab Điện năng → Gas | Biểu đồ, số liệu, đơn vị, màu đổi theo |
| Đổi "7 ngày" → 14 / 30 ngày | Đổi số cột biểu đồ |
| Bấm một ngày trong lịch | Thẻ bên phải đổi theo: âm lịch, Can Chi, lễ, giờ hoàng đạo |
| Bấm ◀ / ▶ ở lịch | Đổi tháng; bấm ngày mờ của tháng khác thì lịch nhảy luôn sang tháng đó |
| Nút Back của máy | Thoát màn con trước, chưa thoát app ngay |

---

## Điện năng: số thật từ EVN qua MQTT

Tab **Điện năng** ở màn Thống kê không còn dùng dữ liệu mẫu. Nó đọc thẳng từ broker theo
hợp đồng topic của dự án `dien_evn` (xem tài liệu bàn giao `bangiao_env_cskh.html`).

Đường đi: EVN CSKH → script `dien_evn` chạy Docker trên `serverubuntu` → Mosquitto →
app subscribe `dien/#`. Mọi topic đều **retained** nên vừa kết nối là có số ngay.

> **Broker là Mosquitto, không phải EMQX**, và chỉ có cổng **1883 không mã hoá** — 8883
> đóng. Tài liệu bàn giao `dien_evn` viết EMQX/8883/TLS; đó là môi trường dự kiến, không
> phải môi trường thật. Đo tận nơi ngày 18/11/2026. Nên ở màn Cài đặt phải **tắt TLS** và
> để cổng **1883**. Mật khẩu MQTT đi qua mạng dưới dạng chữ thường — chấp nhận được trong
> mạng nhà, đừng mở broker ra Internet.

| Ô trên màn hình | Lấy từ |
|---|---|
| "Điện năng tiêu thụ … kWh" | cộng `kwh` trong `dien/lichsu` theo khoảng đang chọn |
| Biểu đồ cột | các phần tử cuối của `dien/lichsu`, nhãn thứ tính từ ngày thật |
| Chênh % | so n ngày cuối với n ngày liền trước |
| "Sản lượng ngày gần nhất" | `dien/ngay/sanluong` + `dien/ngay/ngay` |
| "Chi phí tháng này đến giờ" | `dien/thang/tien` + `dien/thang/sanluong` |
| "Trạng thái" | `dien/trangthai`, `dien/loi` và **tuổi** của `dien/capnhat` |
| Chỉ số công tơ · Công nợ | `dien/congto/chiso`, `dien/congno/sotien` |

Khai báo broker ở màn **Cài đặt**. Mật khẩu lưu trong DataStore trên máy và đã loại khỏi
sao lưu đám mây — mã nguồn không nhúng sẵn mật khẩu nào vì nó nằm trên GitHub.

### Ba chỗ đã sửa theo tài liệu bàn giao

1. **Bỏ tile "Công suất hiện tại … kW".** EVN chốt theo ngày, không có số tức thời. Thay
   bằng "Sản lượng ngày gần nhất".
2. **"Chi phí ước tính" → "Chi phí tháng này đến giờ"**, lấy `dien/thang/tien` (luỹ kế cả
   kỳ) thay vì suy từ khoảng đang chọn. Giá điện bậc thang luỹ tiến nên cộng tiền 7 ngày
   rồi nhân ra cả tháng là sai rất xa: tài liệu dẫn ví dụ 48,7 kWh → 126.000đ trong khi
   mức dùng đó cả tháng (~209 kWh) tốn **~480.650đ**.
3. **Không bịa số.** Mất kết nối hoặc chưa có retained thì hiện `--` và nói rõ lý do,
   tuyệt đối không lấy số mẫu bù vào. Bốn tab còn lại vẫn là dữ liệu mẫu và vẫn đeo chip
   "DỮ LIỆU MẪU"; tab Điện năng thì không đeo, thay bằng trạng thái kết nối.

### App không ghi gì vào `dien/`

Số điện do script trên `serverubuntu` quyết định. App chỉ subscribe, không publish — ghi
vào là đè lên số thật của đường ống.

### Một bẫy được bắt sẵn

Tài liệu bàn giao cảnh báo: thiếu ACL `dien/#` thì "script publish được nhưng bên đọc
không nhận gì, mà lỗi lại không rõ ràng". Broker từ chối subscribe bằng cách trả granted
QoS `0x80` chứ không ném lỗi. `MqttDien.kt` kiểm đúng chỗ đó và báo thẳng
*"Broker từ chối cho đọc dien/#"* thay vì để người dùng đi sửa địa chỉ broker oan.

Riêng Mosquitto ở nhà thì **chưa có `acl_file` nào** — bất cứ tài khoản nào đăng nhập được
cũng đọc ghi được mọi topic, kể cả topic điều khiển bơm và đèn. App và script đang dùng
chung tài khoản `iot` sẵn có. Cái bẫy trên vì vậy chưa bao giờ bắn ở đây, nhưng đổi lại:
**mật khẩu nằm trên máy tính bảng treo tường cũng chính là mật khẩu mở được mọi thứ**.
Siết lại thì tạo tài khoản riêng cho từng thiết bị rồi thêm `acl_file` — không phải sửa
dòng code nào.

---

## Camera: hình thật từ camera Tuya

Màn **Camera an ninh** phát hình thật. Camera Tuya không nói chuyện trực tiếp với app —
đường đi là:

```
Camera Tuya  →  go2rtc (serverubuntu, Docker, đăng nhập tài khoản Tuya)  →  WebRTC  →  máy tính bảng
```

go2rtc chạy trên `serverubuntu` (`192.168.0.50:1984`) chứ không phải trên máy Windows nữa —
lý do ở mục *Vì sao chuyển go2rtc sang server* bên dưới.

Khai báo địa chỉ máy chạy go2rtc ở màn **Cài đặt**, cột phải. App hỏi `/api/streams` để biết
go2rtc đang giữ những luồng nào rồi dựng danh sách camera bên trái từ đó — không phải gõ tay
tên từng luồng.

### Vì sao là WebView chứ không phải ExoPlayer

go2rtc phát WebRTC. ExoPlayer không nói WebRTC, phải lui về RTSP — trễ hơn và kéo thêm vài MB
thư viện. Trang `stream.html` sẵn có của go2rtc thì **tự chọn đường truyền** (WebRTC trước,
không được thì MSE), nên app không phải đoán, và go2rtc nâng cấp thì app được hưởng theo mà
không cần build lại. WebView có sẵn trong Android, không thêm phụ thuộc nào.

Toàn bộ nằm ở `ui/components/LuongCamera.kt` — một file, đổi cách phát thì sửa đúng chỗ đó.

### Vì sao chuyển go2rtc sang server

Lúc đầu go2rtc chạy trên máy tính Windows, và xem bị giật rõ trong khi app Tuya trên điện
thoại thì mượt. Đo ra:

| | |
|---|---|
| Băng thông | 810 kbit/s — không nghẽn |
| Gói mất | 0 |
| Jitter | **104 ms** |
| Bộ đệm phải giữ | **376 ms** |

Chỗ quyết định: con số đó đo **qua loopback**, tức chặng go2rtc → người xem không đi qua
mạng nào cả. Vậy mà jitter vẫn 104 ms — nghĩa là nó sinh ra ở chặng **camera → go2rtc**,
nơi cả camera lẫn máy Windows đều chạy Wi-Fi.

App Tuya nối **thẳng** camera → điện thoại: một chặng, đệm một lần. Đường cũ của app này đi
camera → AP → máy Windows → AP → tablet: hình bay qua không khí **bốn lần** và bị đệm hai
lần. `serverubuntu` nối dây (`enp4s0`), nên còn **hai lần**. Chuyển xong thì đỡ giật rõ.

> Không sửa được bằng cách cho app nối thẳng camera. Quét cổng camera `192.168.0.104`: 554
> (RTSP), 80, 443, 8080 đều **đóng**; chỉ 6668 mở, mà đó là cổng điều khiển Tuya chứ không
> phải video. Muốn lấy hình bắt buộc phải qua đúng quy trình Tuya (đăng nhập cloud → MQTT →
> WebRTC P2P), tức là viết lại toàn bộ giao thức đó bằng Kotlin **và** nhét mật khẩu Tuya vào
> máy tính bảng. Không đáng.

### Lần nối đầu chậm, và app tự thử lại

Nối bình thường mất **4–12 giây**: go2rtc phải đăng nhập máy chủ Tuya rồi mới mở được đường
P2P. Trong lúc đó trang `stream.html` của go2rtc in ra dòng đỏ của nó —
`streams: tuya: mqtt: disconnect` — rồi **nằm im, không tự thử lại** khi người xem nối bằng
websocket.

App che kín trang đó bằng lớp *"Đang nối camera..."* của riêng mình, và tự nạp lại khi thấy
trang báo lỗi. Dòng lỗi thô của go2rtc không bao giờ lọt ra màn hình.

> **Thử lại phải thưa, không được dày.** Mỗi lần nạp lại là một lần đăng nhập lại lên máy chủ
> Tuya. Đo ngày 18/11/2026: thử lại 6 lần liên tiếp thì Tuya chặn tạm và **mọi** lần nối sau
> đó đều hỏng; để yên 4 phút rồi thử đúng một lần thì lên hình trong **4,3 giây**. Nên giãn
> cách tăng dần 4s → 15s → 40s, tối đa 3 lần.

### Rời màn là cắt luồng

Đây là phần dễ làm sai nhất và tốn kém nhất nếu sai: không cắt thì camera gửi hình về máy
tính bảng suốt ngày, tốn pin và băng thông, còn go2rtc vẫn đếm máy tính bảng là một người
đang xem. Nên khi khung hình bị gỡ khỏi màn, WebView được nạp `about:blank` rồi huỷ hẳn.

Đo trên máy thật (SM-T225): đang ở màn Camera thì go2rtc đếm **1** người xem, về Trang chủ
còn **0**, quay lại thì lên **1**. Đo lại sau khi chuyển sang server: vẫn đúng như vậy.

### App không bao giờ đăng nhập Tuya

Tài khoản Tuya chỉ nằm trong `go2rtc.yaml` trên máy chạy go2rtc. App chỉ nói chuyện với
go2rtc.

### Chỗ hở ở API go2rtc — đã bịt ngày 17/11/2026

`/api/streams` của go2rtc trả về cả URL nguồn, mà trong URL đó có email và **mật khẩu tài
khoản Tuya** ở dạng chữ thường. go2rtc mặc định không đặt mật khẩu cho API và lắng nghe trên
mọi địa chỉ, nên trước đó **ai vào được Wi-Fi cũng đọc được**.

Đã đặt `api: username/password` trong `go2rtc.yaml`. Giờ gọi từ địa chỉ mạng mà không có tài
khoản thì nhận **401**. App giữ tài khoản đó trong DataStore và đưa cho WebView khi go2rtc
hỏi, nên hình vẫn chạy bình thường.

App vẫn cố ý **chỉ lấy khoá** của JSON và không bao giờ đụng tới phần url — mật khẩu Tuya
không có lý do gì để đi vào app.

### Âm thanh

Camera có micro và go2rtc phát kèm tiếng, nên khung hình **có tiếng**. Muốn tắt thì thêm
`&media=video` vào URL trong `CauHinhCamera.urlTrangPhat()`.

---

## Trợ lý giọng nói: nói chuyện thật, không có mẫu câu nào

Nút mic ở góc dưới bên phải, nổi trên mọi màn. Bấm một cái là mở phiên, nói
chuyện qua lại tự nhiên, bấm lại hoặc im lặng 45 giây thì tự đóng.

**Không có bảng từ khóa nào trong mã nguồn.** Hiểu câu nói là việc của
[Gemini Live API](https://aistudio.google.com/docs/live-api); app chỉ khai báo
ba hàm cho nó gọi. Nên *"nóng quá, hạ điều hòa phòng ngủ 1 xuống 24 độ giúp
anh"* chạy đúng mà không ai phải viết câu đó ở đâu cả.

```
Mic 16kHz ──► AudioRecord ──base64──┐
                                    ├──► WebSocket WSS ──► Gemini Live
Loa 24kHz ◄── AudioTrack ◄──base64──┘            │
                                                  │ toolCall
                                                  ▼
                                             CongCuNha
                                    ┌─────────────┼──────────────┐
                                    ▼             ▼              ▼
                            HomeViewModel   đổi màn hình    smarthome/control
```

Máy tính bảng nối **thẳng** tới Gemini, không qua `serverubuntu`. Bớt một chặng
mạng nên đáp nhanh hơn, không phải nuôi thêm một container, và server tắt thì
trợ lý vẫn sống.

### Ba hàm khai cho Gemini

Ít hàm thì model gọi đúng hơn, nên gộp còn ba thay vì mỗi việc một hàm:

| Hàm | Làm gì |
|---|---|
| `dieu_khien_thiet_bi` | Đèn, quạt, điều hòa, nhiệt độ, chế độ phòng, lịch hẹn, 4 ô nhanh. Nhận cả `tat_ca` |
| `mo_man_hinh` | Chuyển màn, kể cả mở thẳng một phòng (`phong:living`) |
| `doc_so_lieu` | Điện EVN, server, WiFi, thời tiết, lịch âm, trạng thái cả nhà |

`system_instruction` khai sẵn sơ đồ nhà và một ảnh chụp trạng thái lúc mở
phiên, nên *"đèn phòng khách đang bật không"* trả lời được ngay mà không tốn
một vòng gọi hàm.

### Bật là bật, không phải đảo

Nút trên màn hình thì **đảo** là đúng — người ta nhìn thấy công tắc rồi mới
bấm. Giọng nói thì không: nói *"bật đèn phòng khách"* lúc đèn đang bật mà lại
đảo thành tắt là làm ngược hẳn ý người nói. Nên `HomeViewModel` có thêm một bộ
hàm `datDen`/`datQuat`/`datDieuHoa` đặt thẳng trạng thái, tách khỏi các hàm
`toggle*` mà giao diện dùng. Có test riêng cho đúng chỗ này.

### Khử vọng — chỗ dễ hỏng nhất

Loa phát tiếng Gemini lọt vào mic thì Gemini nghe chính nó và **tự nói chuyện
với mình**, không dừng lại được. Hai lớp chặn:

1. `AudioRecord` dùng nguồn `VOICE_COMMUNICATION` (không phải `MIC`) và
   `AudioTrack` dùng `USAGE_VOICE_COMMUNICATION`. Phải là **cả hai đầu** thì
   Android mới ghép được cặp tín hiệu cho chip DSP khử vọng phần cứng.
2. Máy không có `AcousticEchoCanceler` thì app tự chuyển sang **bán song
   công**: ngừng gửi mic trong lúc loa còn tiếng. Mất khả năng ngắt lời giữa
   chừng, nhưng không bao giờ vào vòng lặp. Ép bật/tắt được ở màn Cài đặt.

Lúc khóa mic thì **vẫn đọc hết đệm của `AudioRecord`**, chỉ không gửi đi.
Không đọc thì đệm đầy, và lúc mở khóa sẽ bắn đi một cục tiếng của quá khứ.

### Tiền và thời gian

Gemini Live tính **theo phút nói chuyện**. Máy này treo tường, một lần quên tắt
mic là tính tiền cả ngày. Nên phiên luôn tự đóng khi im lặng (mặc định 45 giây)
và có trần cứng (mặc định 10 phút, Live API tự cắt ở 15 phút). Bảng thoại có
đồng hồ đếm. Đừng tắt hai thứ đó.

### Khóa API để ở đâu

Trong DataStore của máy tính bảng, gõ ở màn **Cài đặt → Trợ lý giọng nói** —
đúng chỗ của mật khẩu MQTT và tài khoản go2rtc. **Không nhúng vào mã nguồn**:
mã nguồn ở GitHub, và bản APK release được phát hành công khai ở GitHub
Releases cho cơ chế OTA, nên ai tải APK về cũng moi được thứ build vào trong đó.

### Ô "Model" là ô nhập tay — cố ý

Google đổi tên model Live khá thường xuyên. Tra ngày 20/11/2026 thì tài liệu
chính thức gọi `gemini-3.8-live` là model mặc định, trong khi các hướng dẫn
trôi nổi vẫn chép `gemini-2.5-flash-native-audio-preview-*` và
`gemini-3.1-flash-live-preview`. Để ô nhập tay thì đổi model về sau là gõ lại
một ô, không phải build lại APK và cài lại máy.

### Đèn quạt vẫn chưa nối thiết bị thật

Nói *"bật đèn phòng khách"* sẽ gạt công tắc trên màn hình và Gemini xác nhận
bằng giọng — nhưng **không có bóng đèn nào sáng**. Theo nguyên tắc *"không bịa
số"* của dự án, bảng thoại ghi một dòng xám *"chỉ đổi trên màn hình — chưa nối
thiết bị thật"* dưới mỗi lệnh, và `system_instruction` dặn Gemini nói thật nếu
bị hỏi thẳng. Bật tuỳ chọn **Bắn lệnh ra MQTT** thì mỗi lệnh còn đi kèm một
JSON vào `smarthome/control` — đường dựng sẵn cho ESP32, hiện chưa ai nghe.

---

## Lịch âm: tính tại chỗ, không cần mạng

Màn **Tiện ích** là màn duy nhất chạy số thật mà **không nối đi đâu cả**. Không MQTT,
không HTTP, không khoá API — mất mạng vẫn đúng, và vì vậy cũng không bao giờ phải hiện
`--` như các màn kia.

Thuật toán là bản mã nguồn mở của **Hồ Ngọc Đức**, port sang Kotlin trong
`data/lunar/AmLich.kt`. Chép nguyên công thức, **không "làm gọn" hệ số nào**: mọi hằng số
trong phần tính trăng mới và kinh độ mặt trời đều là hệ số thiên văn, đổi một chữ số là
lệch ngày Tết.

> **Múi giờ cố định +7.** Âm lịch Việt Nam chốt ngày theo giờ Hà Nội, Trung Quốc dùng +8
> nên có năm hai bên lệch nhau đúng một ngày. Đừng đổi tham số đó thành giờ máy.

### Test đối chiếu bằng ngày Tết thật

Đây là chỗ dễ tự lừa mình nhất: cho chạy một lần, chép kết quả ra làm mốc, rồi test nào
cũng xanh — nhưng chỉ chứng minh được code **không đổi**, không chứng minh được code
**đúng**. Nên mốc so sánh trong `AmLichTest.kt` là những thứ tra được trên tờ lịch giấy:

- Mùng 1 Tết các năm 2023 → 2027 (Quý Mão, Giáp Thìn, Ất Tỵ, Bính Ngọ, Đinh Mùi)
- Năm 2025 **phải có tháng 6 nhuận** — quét cả 365 ngày để tìm
- Năm 2026 **không được có** tháng nhuận nào
- Ngày 30 Tết vẫn thuộc năm cũ
- Can Chi ngày xoay đúng vòng 60, tháng âm chỉ dài 29 hoặc 30 ngày

### Ba chỗ cố ý làm khác bản thiết kế

1. **Bỏ cột menu con** (Thời tiết / Điện lực EVN / Không khí AQI). Hai mục đầu đã có thật
   ở Trang chủ và màn Thống kê, mục thứ ba chưa có nguồn. Ba thẻ bấm vào không ra gì sẽ
   ăn 150dp bề ngang của màn chỉ rộng 818dp.
2. **Lễ âm lịch không tính trong tháng nhuận** — mùng 5/5 nhuận không phải Tết Đoan Ngọ.
3. **Đếm ngày tới Rằm / Mùng 1 bằng cách duyệt thật từng ngày**, không lấy `15 − ngày`.
   Tháng âm lúc 29 lúc 30 ngày, trừ thẳng là lệch một ngày ở cuối tháng thiếu.

---

## Cái gì CHƯA có — và chỗ để cắm vào sau

| Chưa có | Cắm vào đâu |
|---|---|
| Dữ liệu thật cho đèn/quạt/rèm/cảm biến | `data/DemoData.kt` — **toàn bộ số liệu mẫu còn lại nằm đúng một file này** |
| Ảnh phòng | `ui/components/ImagePlaceholder.kt` — đổi thân hàm thành `Image(...)`. Mọi chỗ gọi đều đi qua đây |
| Ảnh tĩnh / quay video ở màn Camera | Ảnh tĩnh cần `ffmpeg` trên máy chạy go2rtc (`/api/frame.jpeg` trả 500 khi thiếu). Hình trực tiếp thì không cần |
| Điều khiển thiết bị qua MQTT | `vm/HomeViewModel.kt` — các hàm toggle đổi thành gửi lệnh rồi chờ thiết bị xác nhận. Lớp MQTT đã có sẵn ở `data/dien/MqttDien.kt` để dùng lại; `guiLenhDieuKhien()` và topic `smarthome/control` đã dựng sẵn cho giọng nói |
| Từ khóa đánh thức ("Này trợ lý") | Chưa có. Gemini Live không làm wake-word, phải thêm thư viện riêng (Porcupine/Vosk) — stream 24/7 thì hóa đơn và pin đều không chịu nổi |
| Màn Cảm biến | `ui/screens/PlaceholderScreen.kt` — màn trống duy nhất còn lại |
| Thời tiết / Điện lực / AQI trong màn Tiện ích | Bản thiết kế có cột menu con 4 mục, hiện chỉ làm mục Lịch. Thêm cột đó vào `TienIchScreen.kt` khi có nội dung thật |
| Chế độ sáng, font Inter, ký release | Xem BUILD.md |

---

## Vì sao có chip "DỮ LIỆU MẪU"

Mọi con số trên màn hình (26.5 °C, 68 %, 1.25 kW, 48.7 kWh, 126.000 đ…) đều do người viết
đặt ra cho giao diện nhìn được, **không phải số đọc từ thiết bị**.

Tài liệu `Design Specification` của dự án AquaControl có quy tắc *"Không có số liệu giả"*.
Khung giao diện thì bắt buộc phải có số mẫu mới nhìn ra được bố cục, nên cách xử lý là:
gom hết vào một file, và **nói thẳng trên màn hình** rằng đó là số mẫu.

Tắt chip: đặt `DemoData.IS_DEMO = false`.

Ba nút ở màn Camera (Chụp ảnh / Quay video / Toàn màn hình) cũng theo nguyên tắc đó — bấm
vào thì báo *"Chưa nối camera thật"* chứ không giả vờ đã chụp được.

---

## Cấu trúc

```
data/
  Models.kt     RoomUi, QuickItem, CameraFeed, StatSeries, RoomMode, StatKind, StatRange
  DemoData.kt   TOÀN BỘ số liệu mẫu
  dien/         CauHinhMqtt, DienState, MqttDien — số điện thật qua MQTT
  camera/       CauHinhCamera, Go2rtc — hình camera thật qua go2rtc
  lunar/        AmLich — đổi âm lịch, Can Chi, giờ hoàng đạo. Kotlin thuần,
                không dính Compose/Android nên test chạy thẳng trên JVM
  giongnoi/     CauHinhGiongNoi, GeminiLive (WebSocket), AmThanh (mic + loa),
                CongCuNha (ba hàm khai cho Gemini), TrangThaiGiongNoi
vm/
  HomeViewModel.kt   State + các hàm toggle + bản cài HanhDongNha cho giọng nói
  TroLyGiongNoi.kt   Ghép WebSocket + mic + loa + công cụ thành một phiên
ui/
  theme/        Color, Dimens (ngân sách 1006×601dp), Type, Theme
  components/   SmartCard, StatTile, QuickActionTile, DeviceSwitchRow, TempStepperRow,
                ChoiceChip, ImagePlaceholder, BarChart, ArcGauge, PagerDots…
  shell/        Nav, SideNav, TopBar, AppShell, Clock
  screens/      Home, RoomDetail, Camera, Stats, ChiTietDien, Devices, Automation,
                Server, Wifi, Settings, TienIch, Placeholder
```

### Hai quyết định đáng nói

**Không dùng Navigation-Compose.** App chỉ có vài màn phẳng; một biến `current` trong
`AppShell` là đủ, bớt được một thư viện và toàn bộ phần khai báo route.

**Không dùng thư viện biểu đồ.** Biểu đồ cột và đồng hồ cung vẽ bằng `Canvas` thuần
(`ui/components/Charts.kt`), đủ dùng ở kích thước này và không kéo thêm phụ thuộc nào.

Phụ thuộc cố ý rút gọn: không Room, không Retrofit, không KSP, không thư viện biểu đồ,
không thư viện JSON — `HttpURLConnection` và `org.json` có sẵn trong Android là đủ cho
Open-Meteo, go2rtc và OTA.

Hai thứ phải thêm vì không có đường vòng: **Paho MQTT** (số điện thật) và **OkHttp**
(WebSocket cho Gemini Live — Android không có client WebSocket nào sẵn, và
`java.net.http.WebSocket` chỉ có từ API 34 trong khi minSdk là 26).

---

## Ràng buộc kích thước — điểm dễ làm hỏng nhất

Số đo thật trên máy (SM-T225, Android 13), không phải ước lượng:

```
adb shell wm size      ->  Physical size: 800x1340
adb shell wm density   ->  Physical density: 213      (tvdpi)
```

213 dpi → density 1,33125 → vùng vẽ **1006 × 601 dp**.

> Dự án AquaControl ghi 893 × 533 dp vì đoán density là 1.5. **Con số đó sai với máy này.**
> Đừng chép lại.

App chạy kiosk (ẩn cả thanh trạng thái lẫn thanh điều hướng) nên được dùng trọn 1006 × 601 dp.
Để nguyên hai thanh thì chỉ còn 1340 × 780 px và có một dải đen ở cạnh trên — đo được bằng
`dumpsys window displays`, `app=1340x780`. Không muốn kiosk: xoá lời gọi `hideSystemBars()`
trong `MainActivity`.

**Chiều cao vẫn là phía eo.** Đây là chỗ hay làm hỏng layout chép từ ảnh thiết kế, nên sidebar
và thanh tiêu đề cố định, phần còn lại chia theo `weight`, và các dải cố định (header 64dp,
hàng ô số liệu 88dp, dải trạng thái 40dp) được tính sao cho tổng không vượt ngân sách.
Xem chú thích trong `ui/theme/Dimens.kt`.

Sửa bố cục xong thì chạy `build.bat testDebugUnitTest` rồi xem lại ảnh trong
`app\build\screenshots\` — rẻ hơn nhiều so với cài lại lên máy tính bảng mỗi lần.
