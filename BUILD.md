# SmartHome Android — build

Khung giao diện app nhà thông minh, Kotlin + Jetpack Compose, tối ưu cho
**Samsung Galaxy Tab A7 Lite** (8.7", 1340×800, nằm ngang).

---

## Build

```bat
build.bat
```

APK nằm ở `app\build\outputs\apk\debug\app-debug.apk`.

`build.bat` đã tự đặt `JAVA_HOME` và `GRADLE_USER_HOME`. Truyền tham số khác cũng được:
`build.bat testDebugUnitTest`, `build.bat clean`.

### Vì sao phải dùng `build.bat` chứ không gọi thẳng `gradlew.bat`

Ổ C của máy này **đã gần đầy** (có lúc còn 20 MB). Cache của Gradle chiếm ~1,5 GB và mặc định
nằm ở `C:\Users\Admin\.gradle`. `build.bat` trỏ `GRADLE_USER_HOME` sang `D:\gradle-home`
nên build không ăn thêm chỗ nào trên C.

Gọi thẳng `gradlew.bat` sẽ quay về cache trên C và **build có thể chết giữa chừng** với lỗi
khó hiểu `DexArchiveMergerException` — gốc rễ thật sự là *"There is not enough space on the disk"*.

Muốn chạy tay thì đặt biến trước:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"; $env:GRADLE_USER_HOME="D:\gradle-home"; .\gradlew.bat assembleDebug
```

---

## Máy này đã cài sẵn

| Thành phần | Phiên bản | Ở đâu |
|---|---|---|
| Eclipse Temurin JDK | 17.0.21 | `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot` |
| Android SDK | platform 35, build-tools 35.0.0, platform-tools | `C:\Android` |
| Gradle | 8.9 (qua wrapper) | cache ở `D:\gradle-home` |

`local.properties` đã có, trỏ `sdk.dir=C:/Android`.

> **Dùng dấu `/`, đừng dùng `\`.** File này là Java properties nên `\` là ký tự escape:
> viết `C\:\Android` thì `\A` bị nuốt mất dấu `\`, đường dẫn thành `C:Android` và build
> chết với lỗi *"The filename, directory name, or volume label syntax is incorrect"*.

---

## Cài lên máy tính bảng

Bật **Tuỳ chọn nhà phát triển → Gỡ lỗi USB** trên Tab A7 Lite rồi cắm cáp:

```powershell
C:\Android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

Hoặc chép thẳng file APK sang máy tính bảng rồi mở để cài.

APK này cài song song được với `AquaControl` — khác package (`vn.smarthome` so với
`vn.aquacontrol`), không ghi đè lên nhau.

---

## Kiểm thử

```bat
build.bat testDebugUnitTest
```

Các bộ test chạy trên JVM bằng Robolectric, **không cần máy tính bảng**:

- `ShellSmokeTest` — app mở được, điều hướng được, đổi tab thống kê được, các màn
  chưa làm vẫn mở được, nút mic hiện trên mọi màn và bấm khi chưa có khóa Gemini
  thì đẩy sang màn Cài đặt chứ không chết. Mục đích là bắt lỗi chết app lúc chạy
  (thiếu icon, chia cho 0, vẽ cung âm...) — những thứ không lộ ra lúc biên dịch.
- `ScreenshotTest` — vẽ từng màn ra file PNG ở `app\build\screenshots\`, đúng
  **1340 × 800 px** (qualifier `w1006dp-h601dp-land-tvdpi` = 213 dpi — đo thật bằng
  `adb shell wm density`, không phải đoán).
  Dùng để soi tràn bố cục, chữ bị cắt, thẻ chồng nhau mà không cần cắm máy.
- `CongCuNhaTest` — phần đọc lệnh của Gemini. Gemini là hộp đen: nó gọi hàm với
  tham số thiếu, sai chính tả, hoặc tên phòng không có thật. Mọi trường hợp đó phải
  trả về một câu tiếng Việt để nó hỏi lại người dùng, **không được ném ngoại lệ làm
  đứt cả phiên nói chuyện**. Có test riêng cho chỗ "bật đèn lúc đèn đang bật thì vẫn
  là bật, không được đảo thành tắt".
- `GeminiLiveTest` — hình dạng gói `setup` gửi cho Live API. Sai một tên trường ở
  đây thì trên máy thật chỉ thấy "nối được mà trợ lý câm như hến" — rất khó lần ra.
  Có một test khẳng định **khóa API không lọt vào gói setup** (khóa đi bằng tham số
  truy vấn của URL; nhét vào gói là một đường cho nó lọt vào log).
- `AmLichTest`, `DienStateTest`, `CauHinhCameraTest` — xem README.

Chạy xong một lượt ngày 20/11/2026: **96 test, không cái nào hỏng**.

Bản chụp gần nhất: `docs\screenshots\` (render trên máy tính) và `docs\device\`
(chụp từ chính Tab A7 Lite qua `adb exec-out screencap`).

> Robolectric đặt `sdk = 34` vì bản 4.13 chưa hỗ trợ API 35, dù app vẫn compile với 35.

Ảnh render **không thay thế việc cài lên máy thật**: font hệ thống, inset thanh điều hướng
và tỉ lệ màn hình thật vẫn có thể lệch đôi chút.

---

## Bản phát hành

```bat
build.bat assembleRelease
```

APK ở `app\build\outputs\apk\release\app-release.apk` — **2,00 MB** (2.099.968 byte),
nhỏ hơn bản debug (17,4 MB) khoảng **8,7 lần** nhờ R8 rút gọn code và `isShrinkResources`.

### Trợ lý giọng nói làm APK nặng thêm bao nhiêu

Đo bằng chính các file APK do CI phát hành, nên cùng một điều kiện build:

| Tag | Byte | | So với bản trước |
|---|---:|---|---|
| v0.3.0 | 1.894.469 | 1,81 MB | |
| v0.4.0 | 1.910.853 | 1,82 MB | +16 KB |
| v0.4.2 | 1.910.853 | 1,82 MB | +0 |
| **v0.5.0** | **2.099.968** | **2,00 MB** | **+185 KB** ← OkHttp + mã giọng nói |

Tức là **+185 KB**, đúng tầm ước lượng 300–500 KB lúc lên kế hoạch.

> **Đừng lấy con số "939 KB" ở các bản ghi cũ làm mốc.** Bản ghi ngày 17/11/2026 của
> tài liệu này viết APK release là 939 KB, nhưng APK CI phát hành đã là **1,81 MB ngay
> từ v0.3.0** — tức con số cũ đo ở một trạng thái khác và không so sánh được. Ngày
> 20/11/2026 người viết đã một lần dùng nhầm nó làm mốc rồi kết luận sai rằng trợ lý
> giọng nói "làm APK phình hơn gấp đôi".
>
> Muốn biết một tính năng làm nặng thêm bao nhiêu thì **so hai file APK do CI phát
> hành**, đừng so với số chép trong tài liệu. Lấy cỡ thật bằng:
>
> ```
> curl -s https://api.github.com/repos/trungpq96dev/app_smart_home/releases | grep '"size"'
> ```

Nếu sau này phải ép cỡ xuống thì OkHttp là chỗ đầu tiên để nhìn: riêng danh sách
public-suffix nó luôn gói kèm đã 40 KB, mà app không dùng cookie nên không bao giờ
đụng tới.

### Khoá ký

Đã có sẵn, không phải làm gì thêm:

| File | Vai trò |
|---|---|
| `smarthome-release.jks` | khoá ký, RSA 2048, hạn 10.000 ngày |
| `keystore.properties` | đường dẫn + mật khẩu (sinh ngẫu nhiên 28 ký tự lúc tạo) |

**Cả hai file đều nằm trong `.gitignore`. Đừng đưa lên git, đừng gửi cho ai.**

> **Mất hai file này là mất khả năng cập nhật app.** Android chỉ cho cài đè khi APK mới ký
> bằng đúng khoá cũ. Mất khoá thì phải gỡ app rồi cài lại từ đầu.

Đã sao lưu ngày 17/11/2026 vào `C:\Users\Admin\OneDrive\Backup\SmartHome-Keystore\`
(kèm file `DOC-TRUOC-KHI-DUNG.md` hướng dẫn khôi phục). Thư mục đó đồng bộ lên cloud
Microsoft, **gồm cả mật khẩu** — nên bật xác thực hai lớp cho tài khoản Microsoft và
đừng chia sẻ thư mục `Backup` cho ai.

Vân tay chứng chỉ để đối chiếu đúng khoá:

```
SHA-256: 53:02:C9:04:43:93:52:37:12:65:FE:6F:46:68:BE:69:31:31:E7:6C:29:00:C2:C8:1D:2B:69:60:8D:13:9D:E5
```

Máy nào không có `keystore.properties` thì release vẫn build được, ký bằng khoá debug —
để dự án không chết build trên máy khác.

### Lần đầu cài bản release đè lên bản debug cũ

Bản debug cài trước ngày 17/11/2026 mang đúng package `vn.smarthome` nhưng ký bằng khoá
debug, nên cài đè bản release sẽ lỗi `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Gỡ trước một lần:

```powershell
C:\Android\platform-tools\adb.exe uninstall vn.smarthome
```

```powershell
C:\Android\platform-tools\adb.exe install D:\SmartHomeApp\app\build\outputs\apk\release\app-release.apk
```

Từ nay bản debug mang package `vn.smarthome.debug` (`applicationIdSuffix`), nên **hai bản
cài song song được** và không đá nhau nữa.

---

## Gỡ rối

| Hiện tượng | Nguyên nhân |
|---|---|
| `DexArchiveMergerException` | Thường là **hết dung lượng ổ C**, không phải lỗi code. Dùng `build.bat` |
| `SDK location not found` | thiếu `local.properties` |
| `The filename, directory name, or volume label syntax is incorrect` | `sdk.dir` viết dấu `\` đơn — dùng `C:/Android` |
| `Unsupported class file major version` | đang chạy JDK khác 17 — kiểm tra `JAVA_HOME` |
| `Minimum supported Gradle version is 8.9` | AGP 8.7.2 cần Gradle tối thiểu 8.9, đừng hạ phiên bản |
| Test treo không bao giờ xong | quên `mainClock.autoAdvance = false`. Đồng hồ sidebar có vòng lặp `while(true) delay(...)`, để test tự chạy đồng hồ thì `waitForIdle` chờ mãi |

---

## Bản release đã kiểm chứng trên máy thật

Chạy trên Samsung SM-T225 (Android 13), ngày 17/11/2026:

| Hạng mục | Kết quả |
|---|---|
| Cài đặt | `Success` (sau khi gỡ bản debug cũ cùng package) |
| Chữ ký | `apksigner verify` → **Verifies**, APK Signature Scheme v2 |
| Khởi động nguội lần đầu | 2,0 giây (còn dexopt) |
| Khởi động nguội các lần sau | **0,51 / 0,56 / 0,61 giây** |
| So với bản debug | debug mất **11,0 giây** — nhanh hơn khoảng 20 lần |
| Crash | không có (`logcat -b crash` rỗng) |
| Lỗi do R8 cắt nhầm | không có `ClassNotFoundException` / `NoSuchMethodError` / `VerifyError` |

Đã mở đủ 4 màn và thao tác thật: gạt công tắc "Quạt trần" ở màn chi tiết phòng, đổi màn
qua menu trái, mở Thống kê. Biểu đồ Canvas, đồng hồ cung, danh sách 5 tab (duyệt bằng
`enum entries`) và đồng hồ chạy giây ở khung camera đều sống sau khi R8 rút gọn.

Ảnh chụp từ máy: `docs\device\release\`.

### R8 và Paho — bẫy chỉ lộ ra ở bản release trên máy thật

Câu "không cần thêm luật ProGuard nào" ở bản ghi ngày 17/11 **đã sai** từ khi thêm MQTT.

Paho tìm lớp ghi log bằng **tên chuỗi** (`Class.forName` + `ResourceBundle`), nên R8 đổi
tên lớp là nó không thấy nữa và app chết ngay lúc bấm "Lưu và kết nối lại":

```
java.util.MissingResourceException: Error locating the logging class
```

Đã gặp thật trên SM-T225 ngày 17/11/2026. Đáng chú ý là **mọi thứ khác đều xanh**: build
sạch, 26 test Robolectric qua, cả 4 màn vẽ đúng — vì test chạy trên lớp *chưa* rút gọn và
không màn nào mở kết nối MQTT thật. Chỉ bản release chạy trên máy thật, và chỉ khi thực sự
bấm kết nối, mới lộ.

Luật trong `app\proguard-rules.pro` giữ nguyên tên mọi thứ trong Paho. Sau khi thêm: cài
lại, bấm kết nối tới một host không tồn tại, app không chết và báo đúng *"Không tìm thấy
máy chủ broker"*.

> Rút ra: thêm thư viện nào dùng reflection thì **phải thử bản release trên máy thật**,
> đừng tin bản debug và test.

### OkHttp qua được R8 (20/11/2026)

Thêm OkHttp cho trợ lý giọng nói. Khác Paho: OkHttp **mang sẵn luật R8 trong chính
gói aar** nên không phải giữ lớp nào bằng tay. `proguard-rules.pro` chỉ thêm mấy
dòng `-dontwarn` cho các lớp chỉ dùng trên JVM máy chủ (Conscrypt, BouncyCastle,
OpenJSSE, Animal Sniffer) — không có trên Android và không bao giờ chạy ở đây.

`assembleRelease` chạy sạch ở cả hai nơi: trên máy này (APK ký đúng khóa cũ, vân tay
khớp bản ghi ở trên) và trên CI — tag `v0.5.0` build `success`, phát hành
`SmartHome-v0.5.0.apk`. Hai file chênh nhau 4 byte, tức là cùng một thứ.

> **Vẫn còn nợ phần quan trọng nhất**: cài bản release lên SM-T225 rồi **thật sự
> bấm nút mic**. Đúng bài học của Paho — build sạch và test xanh không chứng minh
> được gì về thư viện mạng; nó chỉ lộ nguyên hình khi thực sự mở kết nối, ở bản
> release, trên máy thật. Tính tới lúc ghi dòng này, chưa ai bấm nút mic trên
> máy thật lần nào.

### Gemini Live đóng kết nối ngay khi bấm mic (v0.5.0 — đã sửa ở v0.5.1)

Bấm nút mic là hỏng ngay:

```
Invalid JSON payload received. Unknown name "speechConfig" at 'setup': Cannot find field.
```

Gói `setup` đặt `responseModalities` và `speechConfig` ở **cấp ngoài**. Đúng ra chúng
phải nằm trong `generationConfig`. Server trả mã đóng 1007 và app hiện nguyên câu đó.

**Vì sao mắc**: trang *hướng dẫn* của Google viết ví dụ
`{"setup": {"model": ..., "responseModalities": ["AUDIO"]}}` — nhưng đó là hình dạng
cấu hình của **SDK**, không phải hình dạng thật trên đường dây. Trang
[API reference](https://aistudio.google.com/api-reference/live) mới là chỗ đúng:
`BidiGenerateContentSetup` có đúng 12 trường và không có hai cái kia.

> **Bài học đắt hơn bản thân lỗi**: `GeminiLiveTest` lúc đó CÓ chạy và CÓ xanh — nó
> khẳng định `setup.responseModalities` tồn tại. Nhưng mã nguồn và test đều viết ra từ
> **cùng một giả định sai**, nên test chỉ chốt được *hình dạng*, không chốt được *hình
> dạng đúng*. Test không bao giờ thay được việc đối chiếu tài liệu tham chiếu.
>
> Test bây giờ có `setup_chi_duoc_chua_dung_cac_truong_server_nhan` — liệt kê trắng 12
> trường hợp lệ và bắt lỗi mọi tên lạ. Thêm trường mới vào `setup` mà quên cập nhật
> danh sách đó thì test đỏ ngay, không phải đợi bấm mic trên máy thật mới biết.

### Build release chết vì khóa file — không phải lỗi code

Gặp ngày 20/11/2026:

```
Execution failed for task ':app:minifyReleaseWithR8'
> classes.dex: The process cannot access the file because it is being used by another process
```

Nguyên nhân: **hai Gradle daemon cùng build một thư mục `app\build`**. Phân biệt
bằng command line của tiến trình java — daemon nào dùng `D:\gradle-home\caches` là
của `build.bat`, daemon nào dùng `C:\Users\Admin\.gradle\caches` là của ai đó gọi
thẳng `gradlew.bat`.

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Select-Object ProcessId, CreationDate, CommandLine
```

Dừng daemon dùng cache trên C (nhẹ tay hơn kill, và chỉ dừng daemon trong registry
của C chứ không đụng daemon của `build.bat`):

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"; Remove-Item Env:\GRADLE_USER_HOME -ErrorAction SilentlyContinue; .\gradlew.bat --stop
```

> Đừng kill bừa tiến trình java — có thể đang là build dở của một phiên làm việc
> khác trên cùng thư mục này.

---

## Camera qua go2rtc — đã kiểm trên máy thật

Chạy trên Samsung SM-T225 (Android 13), ngày 17/11/2026, với camera Tuya thật:

| Hạng mục | Kết quả |
|---|---|
| Máy tính bảng dò ra go2rtc | **1 luồng** (`cam1`), chip xanh ở góc phải màn Cài đặt |
| Hình lên trong tab Camera | có — go2rtc đếm **1.751.310 byte** đã gửi về máy tính bảng |
| Độ phân giải | **1920 × 1080**, WebRTC |
| Đang ở màn Camera | go2rtc đếm **1** người xem |
| Về Trang chủ | **0** người xem — luồng bị cắt |
| Quay lại màn Camera | lên lại **1** |
| Crash | không có (`logcat -b crash` rỗng) |
| Khung hình | ~61 fps, không giật |

Máy tính bảng `192.168.0.108`, máy chạy go2rtc `192.168.0.105`, cùng Wi-Fi.

### Ba cái bẫy đã gặp

**`usesCleartextTraffic` phải bật.** go2rtc trong mạng nhà chạy `http://192.168.x.x:1984`.
Từ Android 9, HTTP thường bị chặn mặc định và **không báo gì** — chỉ thấy khung đen tuyệt đối.
Khi go2rtc ra sau `tailscale serve` (https) thì bỏ dòng đó trong manifest đi được.

**`width=100%` phải viết `100%25` trong URL.** Dấu `%` trong query là ký tự mở đầu mã hoá,
để nguyên thì trình duyệt đọc `%w` thành rác và khung hình tụt về 320px giữa màn đen.
Có test riêng cho chỗ này (`CauHinhCameraTest`).

**`AndroidView.update` đừng so với `view.url`.** Ngay sau `factory`, `loadUrl` chưa chạy xong
nên `view.url` còn `null` — so kiểu đó là nạp trang lần thứ hai, thành **hai kết nối WebRTC**
cùng lúc trong vài giây, tốn gấp đôi băng thông. Đo được bằng `/api/streams`: 2 consumer,
cả hai đều đang nhận byte. Sửa bằng cách ghi url vào `view.tag` rồi so với tag.

### Đặt mật khẩu cho API go2rtc

Đã làm ngày 17/11/2026. Bản `go2rtc.yaml` cũ nằm ở
`C:\go2rtc\go2rtc.yaml.truoc-khi-dat-mat-khau`.

```yaml
api:
  username: tablet
  password: <28 ký tự ngẫu nhiên — nằm trong chính go2rtc.yaml>
```

> **go2rtc bỏ qua đăng nhập cho `127.0.0.1`.** Thử bằng `curl http://127.0.0.1:1984/...` sẽ
> thấy **200** và tưởng mật khẩu không ăn. Phải thử qua địa chỉ mạng
> (`http://192.168.0.105:1984/...`) mới thấy **401**. Mất mười phút ở chỗ này.

Bật mật khẩu **không làm hỏng luồng WebRTC**: WebView bị hỏi `401`, app trả tài khoản qua
`onReceivedHttpAuthRequest`, rồi Chromium dùng lại tài khoản đã nhớ cho cả lần bắt tay
WebSocket `/api/ws`. Đã đo lại sau khi bật: vẫn 1 consumer khi xem, 0 khi rời màn.

### go2rtc chạy ở đâu — `serverubuntu`, không phải máy Windows

Chuyển ngày 18/11/2026 vì xem bị giật. Lý do và số đo: xem README, mục *Vì sao chuyển go2rtc
sang server*. Tóm tắt: máy Windows nối Wi-Fi nên hình bay qua không khí bốn lần; server nối
dây nên còn hai.

| Mục | Giá trị |
|---|---|
| Máy | `serverubuntu`, `192.168.0.50`, nối dây `enp4s0` 100 Mbps |
| Cách chạy | Docker, `alexxit/go2rtc:latest`, `--network host --restart unless-stopped` |
| Cấu hình | `~/go2rtc/go2rtc.yaml`, quyền `600` |
| Tự chạy lại | theo Docker, không cần ai đăng nhập |

```bash
ssh serverubuntu@192.168.0.50 'docker logs --tail 30 go2rtc'
```

```bash
ssh serverubuntu@192.168.0.50 'docker restart go2rtc'
```

Dựng lại từ đầu nếu cần:

```bash
docker run -d --name go2rtc --network host --restart unless-stopped -v /home/serverubuntu/go2rtc:/config alexxit/go2rtc:latest
```

> `--network host` là bắt buộc. Chạy kiểu bridge thì go2rtc chỉ thấy địa chỉ trong container,
> và các ứng viên ICE nó quảng cáo sẽ không ai nối tới được.

### Hai cái bẫy của nguồn Tuya

**`tuya: mqtt: disconnect` KHÔNG phải lỗi chí mạng.** Nó xuất hiện gần như mỗi lần nối đầu;
go2rtc thử lại và lên hình sau 4-12 giây. Ngày 18/11/2026 người viết đã tưởng đây là lỗi
thật và đi đo IPv6, DNS, đồng hồ, Tailscale, IP công cộng mất cả tiếng — trong khi chỉ cần
đợi thêm mười giây. Dấu hiệu nhận ra: `curl /api/frame.jpeg` vẫn trả **HTTP 200**.

**Thử lại dày thì Tuya chặn tạm.** Đo được: nạp lại 6 lần liên tiếp → mọi lần nối sau đó đều
hỏng trong nhiều phút; để yên 4 phút rồi thử một lần → lên hình trong **4,3 giây**. Vì vậy
`LuongCamera.kt` giãn cách 4s → 15s → 40s và chỉ thử 3 lần. Đừng rút ngắn.

Một chi tiết nữa trong log: `dial cons=1 prod=0` nghĩa là có người xem mà nguồn chưa nối —
với người xem nối bằng websocket thì go2rtc **không tự thử lại**, phải nạp lại trang. Còn
`websocket: close 1001 (going away)` là do chính app nạp lại; nạp lại quá sớm sẽ giết đúng
lần thử sắp thành công.

### Bản trên máy Windows đã nghỉ

Scheduled Task `go2rtc` đã gỡ, tiến trình đã tắt. Thư mục `C:\go2rtc` vẫn còn nguyên để làm
đường lùi.

> **Hai file trong đó chứa mật khẩu tài khoản Tuya ở dạng chữ thường**: `go2rtc.yaml` và
> `go2rtc.yaml.truoc-khi-dat-mat-khau`. Giờ server mới là nơi chạy thật, nên hai file này là
> bản sao thừa của một bí mật. Xoá đi thì gọn, nhưng mất đường lùi — tự quyết.

### Gỡ rối camera

| Hiện tượng | Nguyên nhân |
|---|---|
| Chip "Chưa khai báo go2rtc" | chưa điền địa chỉ ở màn Cài đặt |
| "Không tìm thấy máy chạy go2rtc" | sai địa chỉ, hoặc máy tính bảng ở Wi-Fi khác |
| "Máy chạy go2rtc không trả lời" | container `go2rtc` trên server đã tắt, hoặc sai địa chỉ |
| "go2rtc đòi tài khoản" | go2rtc có đặt `api: username/password` — điền vào màn Cài đặt |
| Dò ra luồng nhưng khung đen | thường là cổng 8555 (WebRTC) chưa mở, trong khi 1984 đã mở |
| Mọi thứ đang chạy bỗng mất hình | `serverubuntu` tắt, hoặc container `go2rtc` chết |
| Nối mãi không lên, thử nhiều lần càng hỏng | Tuya chặn tạm vì đăng nhập quá dày — để yên vài phút rồi thử lại **một** lần |
| Nút *Chụp ảnh* không ra hình | `/api/frame.jpeg` cần `ffmpeg` trên máy chạy go2rtc |

> **Tường lửa Windows**: luật tên `go2rtc` hiện chỉ áp cho profile **Public**, và mạng Wi-Fi
> đang dùng cũng đang là Public nên khớp. Đổi mạng đó sang Private thì luật không áp nữa và
> khung hình đen — nhớ chỗ này. Kiểm bằng `Get-NetConnectionProfile`.

---

## Cập nhật app về sau

APK release mới phải ký đúng khoá cũ thì mới cài đè được:

```powershell
C:\Android\platform-tools\adb.exe install -r D:\SmartHomeApp\app\build\outputs\apk\release\app-release.apk
```

Nhớ tăng `versionCode` trong `app\build.gradle.kts` mỗi lần phát hành bản mới.
