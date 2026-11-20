# Cau noi 2 cong tac Wi-Fi phong ngu 1 <-> Mosquitto.
#
# Ghim phien ban cu the (khong dung latest): thu nay chay khong ai trong,
# mot ban thu vien moi lam hong thi vai ngay sau moi phat hien.
FROM python:3.12.7-slim

# tinytuya >= 1.13 moi ho tro protocol 3.4/3.5 (bat tay phien). Den o nha
# la 3.3 nen khong bat buoc, nhung dat san de doi thiet bi khong phai sua.
RUN pip install --no-cache-dir paho-mqtt==2.1.0 tinytuya==1.21.1

RUN useradd --create-home --uid 10001 congtac

WORKDIR /app
COPY congtac_bridge.py driver.py /app/

# KHAC server_collector: khong can root. Nhung dac quyen o kia la de doc
# phan cung host (/sys, docker.sock); bridge nay chi mo ket noi TCP ra.
USER congtac

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

CMD ["python3", "congtac_bridge.py"]
