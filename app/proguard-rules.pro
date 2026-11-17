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
