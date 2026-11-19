# Anh gon chay wifi_collector.py - cao router TP-Link roi publish MQTT.
#
# Ghim phien ban cu the (khong dung latest): thu nay chay khong ai trong,
# mot ban thu vien moi lam hong thi vai ngay sau moi phat hien.
FROM python:3.12.7-slim

RUN useradd --create-home --uid 10002 wifi

WORKDIR /app

# Script chi dung thu vien chuan (urllib, base64, json, re) + paho-mqtt.
# Khong can requests.
RUN pip install --no-cache-dir paho-mqtt==2.1.0

COPY wifi_collector.py /app/

USER wifi

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

CMD ["python3", "wifi_collector.py"]
