FROM python:3.11-slim
WORKDIR /app
RUN pip install requests --no-cache-dir
COPY ingest.py .
CMD ["python", "ingest.py"]
