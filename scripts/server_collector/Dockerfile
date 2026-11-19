# Anh gon chay server_collector.py - doc suc khoe serverubuntu roi publish MQTT.
#
# Ghim phien ban cu the (khong dung latest): thu nay chay khong ai trong, mot
# ban thu vien moi lam hong thi vai ngay sau moi phat hien.
FROM python:3.12.7-slim

WORKDIR /app

# psutil co san wheel manylinux -> khong can trinh bien dich.
RUN pip install --no-cache-dir psutil==6.1.0 paho-mqtt==2.1.0

COPY server_collector.py /app/

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

CMD ["python3", "server_collector.py"]
