FROM python:3.13-slim
WORKDIR /app
ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1
COPY data/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY data/ ./data/
EXPOSE 1880
CMD ["python","data/simulador_planta.py","--host","0.0.0.0","--porta","1880"]
