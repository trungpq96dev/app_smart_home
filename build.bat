@echo off
REM ============================================================
REM  Build APK SmartHome.
REM
REM  GRADLE_USER_HOME tro sang o D CO CHU DICH: o C tren may nay
REM  da gan het dung luong, ma cache cua Gradle chiem ~1.5 GB.
REM  Dung thu muc nay thi build khong dung them cho nao tren C.
REM ============================================================
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot"
set "GRADLE_USER_HOME=D:\gradle-home"

if "%~1"=="" (
    call "%~dp0gradlew.bat" assembleDebug
) else (
    call "%~dp0gradlew.bat" %*
)
