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

Hai bộ test, chạy trên JVM bằng Robolectric, **không cần máy tính bảng**:

- `ShellSmokeTest` — 5 test: app mở được, điều hướng được, đổi tab thống kê được,
  các màn chưa làm vẫn mở được. Mục đích là bắt lỗi chết app lúc chạy (thiếu icon,
  chia cho 0, vẽ cung âm...) — những thứ không lộ ra lúc biên dịch.
- `ScreenshotTest` — vẽ từng màn ra file PNG ở `app\build\screenshots\`, đúng
  **1340 × 800 px** (qualifier `w1006dp-h601dp-land-tvdpi` = 213 dpi — đo thật bằng
  `adb shell wm density`, không phải đoán).
  Dùng để soi tràn bố cục, chữ bị cắt, thẻ chồng nhau mà không cần cắm máy.

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

APK ở `app\build\outputs\apk\release\app-release.apk` — **939 KB**, nhỏ hơn bản debug
(16 MB) khoảng 17 lần nhờ R8 rút gọn code và `isShrinkResources`.

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

---

## Cập nhật app về sau

APK release mới phải ký đúng khoá cũ thì mới cài đè được:

```powershell
C:\Android\platform-tools\adb.exe install -r D:\SmartHomeApp\app\build\outputs\apk\release\app-release.apk
```

Nhớ tăng `versionCode` trong `app\build.gradle.kts` mỗi lần phát hành bản mới.
