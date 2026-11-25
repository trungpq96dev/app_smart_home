# Đầu ghi NVR 24/24 + AI phát hiện người + Đồng bộ Google Drive 5TB cho SmartHomeApp.
# Ghim phiên bản cố định theo đúng chuẩn của repo để chạy ổn định dài ngày.
FROM python:3.12.7-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
        ffmpeg \
        rclone \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
        paho-mqtt==2.1.0 \
        numpy==2.1.3 \
        Pillow==11.0.0 \
        onnxruntime==1.20.1

RUN useradd --create-home --uid 10002 recorder \
    && mkdir -p /data /config \
    && chown -R recorder:recorder /data /config

WORKDIR /app
COPY cau_hinh.py db.py storage.py telegram_notifier.py recorder.py detector.py api.py mqtt_bridge.py camera_recorder.py /app/

USER recorder

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

EXPOSE 1985

CMD ["python3", "camera_recorder.py"]
