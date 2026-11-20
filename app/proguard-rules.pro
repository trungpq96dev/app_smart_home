# ---------------------------------------------------------------------------
#  Eclipse Paho MQTT
# ---------------------------------------------------------------------------
#  Paho tim lop ghi log bang TEN CHUOI (Class.forName + ResourceBundle), nen
#  khi R8 doi ten lop thi no khong tim thay nua va nem:
#
#      java.util.MissingResourceException: Error locating the logging class
#
#  Loi nay CHI xuat hien o ban release va CHI khi that su mo ket noi MQTT -
#  build sach, test Robolectric xanh, man hinh ve dung het. Da gap that tren
#  SM-T225 ngay 17/11/2026 luc bam "Luu va ket noi lai".
#
#  Vi vay: giu nguyen ten moi thu trong Paho.
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-dontwarn org.eclipse.paho.**

# ---------------------------------------------------------------------------
#  OkHttp / Okio - WebSocket toi Gemini Live
# ---------------------------------------------------------------------------
#  OkHttp mang san luat R8 trong chinh goi aar nen khong phai giu lop nao.
#  Hai dong duoi chi de dap canh bao ve nhung lop CHI dung tren JVM may chu
#  (Conscrypt, BouncyCastle, OpenJSSE, Animal Sniffer) - khong co tren Android
#  va khong bao gio chay o day.
#
#  Bai hoc tu Paho o tren van con nguyen: thu vien nao dung reflection thi
#  PHAI thu ban release tren may that, dung tin ban debug va test.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
