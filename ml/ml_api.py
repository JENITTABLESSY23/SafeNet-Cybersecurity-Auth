"""
SafeNet ML API — Flask REST wrapper
Spring Boot calls POST /predict with sensor features
Returns alert level, confidence, and classification
"""

from flask import Flask, request, jsonify
import joblib, numpy as np, pandas as pd
import os

app = Flask(__name__)

MODEL_DIR = "outputs/"

# Load models once on startup
try:
    scaler        = joblib.load(f"{MODEL_DIR}scaler.pkl")
    feature_names = joblib.load(f"{MODEL_DIR}feature_names.pkl")
    xgb_model     = joblib.load(f"{MODEL_DIR}xgboost_model.pkl")
    iso_model     = joblib.load(f"{MODEL_DIR}isolation_forest_model.pkl")
    print("✅ Models loaded successfully")
except FileNotFoundError:
    print("⚠️  Model files not found — run safenet_model.py first")
    scaler = feature_names = xgb_model = iso_model = None


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "running",
        "models_loaded": xgb_model is not None,
        "version": "1.0.0"
    })


@app.route("/predict", methods=["POST"])
def predict():
    """
    POST /predict
    Body: { "features": { "feature_0": 1.2, "feature_1": -0.5, ... } }
    Returns: { "alert_level": "CRITICAL", "xgb_confidence": 0.92,
                "if_anomaly": true, "combined_score": 0.87,
                "attack_type": "DDoS" }
    """
    if xgb_model is None:
        return jsonify({"error": "Models not loaded"}), 503

    data = request.get_json()
    if not data or "features" not in data:
        return jsonify({"error": "Missing 'features' in request body"}), 400

    try:
        features = data["features"]
        row = pd.DataFrame([features])

        # Align columns
        for col in feature_names:
            if col not in row.columns:
                row[col] = 0.0
        row = row[feature_names]

        row_scaled = scaler.transform(row)

        # XGBoost prediction
        xgb_proba  = float(xgb_model.predict_proba(row_scaled)[0][1])
        xgb_pred   = int(xgb_model.predict(row_scaled)[0])

        # Isolation Forest prediction
        iso_raw     = iso_model.predict(row_scaled)[0]
        iso_anomaly = bool(iso_raw == -1)
        iso_score   = float(iso_model.decision_function(row_scaled)[0])

        # Normalize IF score to 0-1 (lower = more anomalous)
        iso_norm = max(0, min(1, 1 - (iso_score + 0.5)))

        # Hybrid fusion (weighted)
        combined = round(0.7 * xgb_proba + 0.3 * iso_norm, 4)

        # Classify alert level
        if combined >= 0.70:
            alert_level = "CRITICAL"
        elif combined >= 0.40:
            alert_level = "WARNING"
        else:
            alert_level = "NORMAL"

        # Map to attack category (for dashboard display)
        attack_categories = {
            "CRITICAL": "Privilege Escalation / Insider Threat",
            "WARNING":  "Sensor Drift / Device Malfunction",
            "NORMAL":   "—"
        }

        return jsonify({
            "alert_level":      alert_level,
            "xgb_confidence":   round(xgb_proba, 4),
            "xgb_prediction":   xgb_pred,
            "if_anomaly":       iso_anomaly,
            "if_score":         round(iso_score, 4),
            "combined_score":   combined,
            "attack_category":  attack_categories[alert_level]
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/predict/batch", methods=["POST"])
def predict_batch():
    """
    POST /predict/batch
    Body: { "records": [ { "feature_0": ..., ... }, ... ] }
    Returns list of predictions.
    """
    if xgb_model is None:
        return jsonify({"error": "Models not loaded"}), 503

    data = request.get_json()
    if not data or "records" not in data:
        return jsonify({"error": "Missing 'records'"}), 400

    try:
        df = pd.DataFrame(data["records"])
        for col in feature_names:
            if col not in df.columns:
                df[col] = 0.0
        df = df[feature_names]

        scaled     = scaler.transform(df)
        xgb_probas = xgb_model.predict_proba(scaled)[:, 1]
        iso_raws   = iso_model.predict(scaled)
        iso_scores = iso_model.decision_function(scaled)

        results = []
        for i in range(len(df)):
            xp   = float(xgb_probas[i])
            ia   = bool(iso_raws[i] == -1)
            isn  = max(0, min(1, 1 - (float(iso_scores[i]) + 0.5)))
            comb = round(0.7 * xp + 0.3 * isn, 4)
            lvl  = "CRITICAL" if comb >= 0.70 else ("WARNING" if comb >= 0.40 else "NORMAL")
            results.append({
                "index":          i,
                "alert_level":    lvl,
                "xgb_confidence": round(xp, 4),
                "if_anomaly":     ia,
                "combined_score": comb
            })

        summary = {
            "CRITICAL": sum(1 for r in results if r["alert_level"] == "CRITICAL"),
            "WARNING":  sum(1 for r in results if r["alert_level"] == "WARNING"),
            "NORMAL":   sum(1 for r in results if r["alert_level"] == "NORMAL"),
        }

        return jsonify({"predictions": results, "summary": summary})

    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    print(f"🚀 SafeNet ML API running on http://0.0.0.0:{port}")
    app.run(host="0.0.0.0", port=port, debug=False)
