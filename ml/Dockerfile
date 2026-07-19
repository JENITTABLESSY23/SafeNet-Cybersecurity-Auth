FROM python:3.11-slim
WORKDIR /app

# Install dependencies first so Docker can cache this layer separately from
# code changes.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Only what ml_api.py actually needs at runtime — the trained model files
# and the Flask app itself. safenet_model.py (training) and the CICIoT2023
# dataset are deliberately NOT copied in: they're only needed once, offline,
# to produce the files in outputs/, and the dataset alone is gigabytes —
# shipping it in the image would make every deploy huge for no benefit.
COPY outputs ./outputs
COPY ml_api.py .

EXPOSE 5000
CMD ["python", "ml_api.py"]
