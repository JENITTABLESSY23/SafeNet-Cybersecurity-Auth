"""
SafeNet — Hybrid Anomaly Detection Model
=========================================
Base Paper  : Khan & Alkhathami, Scientific Reports 14, 5872 (2024)
              "Anomaly Detection in IoT-Based Healthcare: ML for Enhanced Security"
Dataset     : CICIoT2023 (Canadian Institute for Cybersecurity)
Algorithms  : XGBoost (supervised) + Isolation Forest (unsupervised)  ← proposed
Base model  : Random Forest 99.55%                                    ← base paper
Enhancement : SHAP interpretability
"""

import os
import warnings
warnings.filterwarnings("ignore")

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.model_selection import train_test_split, cross_val_score, StratifiedKFold
from sklearn.ensemble import IsolationForest, RandomForestClassifier
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score,
    f1_score, confusion_matrix, classification_report
)

from xgboost import XGBClassifier
from imblearn.over_sampling import SMOTE

import shap
import joblib

# ─────────────────────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────────────────────
# Resolved relative to this script's own location, not the caller's
# working directory — so `python safenet_model.py` works the same whether
# you run it from ml/ or from the project root or anywhere else.
_SCRIPT_DIR    = os.path.dirname(os.path.abspath(__file__))
DATA_PATH      = os.path.join(_SCRIPT_DIR, "CICIoT2023")
OUTPUT_DIR     = os.path.join(_SCRIPT_DIR, "outputs")
RANDOM_STATE   = 42
TEST_SIZE      = 0.20
PCC_THRESHOLD  = 0.90            # same as base paper
N_SAMPLES_PER_CLASS = 8450       # same as base paper (2-class)
CONTAMINATION  = 0.1             # Isolation Forest contamination rate

os.makedirs(OUTPUT_DIR, exist_ok=True)

# ─────────────────────────────────────────────────────────────
# 1. LOAD DATA  (same approach as base paper)
# ─────────────────────────────────────────────────────────────
def load_dataset_sampled(path: str, n_per_class: int) -> pd.DataFrame:
    """
    Load CICIoT2023 CSVs one file at a time, capping each class's running
    total across ALL files as it goes — never concatenates the full raw
    dataset into memory at once.

    The previous approach (load all ~309 files fully, concat into one
    DataFrame, THEN sample per class) requires materializing the entire
    ~16GB+ dataset before any sampling can shrink it — pd.concat crashes
    with an ArrayMemoryError on that step on a typical laptop, regardless
    of how small the post-sampling target is. Sampling per file as it's
    read keeps peak memory bounded to one file's worth of rows plus the
    small accumulated sample (a few hundred thousand rows at most).

    Trade-off: since classes are capped incrementally per file rather than
    from the full label distribution at once, the exact rows selected
    depend on file read order — fine for training data, just not
    bit-for-bit reproducible against the old whole-dataset-then-sample
    approach.
    """
    print("📂 Loading CICIoT2023 dataset (sampling as we go to bound memory use)...")
    csv_files = sorted(f for f in os.listdir(path) if f.lower().endswith(".csv"))
    if not csv_files:
        raise FileNotFoundError(
            f"No CSV files found in {path}. Download the dataset from "
            "https://www.unb.ca/cic/datasets/iotdataset-2023.html and place "
            "the CSVs there first."
        )

    label_col = None          # discovered from the first successfully-read file
    remaining = {}            # class -> quota left; grows as new classes are discovered
    sampled_parts = []
    total_rows_seen = 0

    for i, f in enumerate(csv_files, 1):
        try:
            df = pd.read_csv(os.path.join(path, f))
            df.columns = df.columns.str.strip()
        except Exception as e:
            print(f"   ⚠ Skipping {f}: {e}")
            continue

       # if label_col is None:
            label_matches = [c for c in df.columns if c.lower() == "label"]
            label_col = label_matches[0] if label_matches else df.columns[-1]
            print(f"   Using '{label_col}' as the label column.")
        if label_col is None:
    # Your dataset has no Label column, so create one from the filename
          df["Label"] = "BenignTraffic" if "benign" in f.lower() else "Attack"
          label_col = "Label"
          print(f"   Using '{label_col}' as the label column.")
        else:
    # Add the Label column for every other file too
         df["Label"] = "BenignTraffic" if "benign" in f.lower() else "Attack"
        total_rows_seen += len(df)

        for cls, group in df.groupby(label_col):
            remaining.setdefault(cls, n_per_class)
            take = min(remaining[cls], len(group))
            if take > 0:
                sampled_parts.append(group.sample(n=take, random_state=RANDOM_STATE))
                remaining[cls] -= take

        if i % 20 == 0 or i == len(csv_files):
            sampled_so_far = sum(len(p) for p in sampled_parts)
            print(f"   Scanned {i}/{len(csv_files)} files — {total_rows_seen:,} rows seen, {sampled_so_far:,} sampled so far")

        # Once every discovered class has hit its quota, stop reading —
        # no benefit to scanning remaining files. (Only safe once we've
        # seen at least a few files, so we don't quit before discovering
        # classes that only appear later in file order.)
        if i > 15 and remaining and all(v <= 0 for v in remaining.values()):
            print(f"   All class quotas filled after {i}/{len(csv_files)} files — stopping early.")
            break

    if not sampled_parts:
        raise RuntimeError("No rows were sampled from any file — check the dataset files aren't empty/corrupt.")

    data = pd.concat(sampled_parts, ignore_index=True).sample(frac=1, random_state=RANDOM_STATE).reset_index(drop=True)
    print(f"   Final sampled shape: {data.shape} ({data[label_col].nunique()} classes)")
    return data



# ─────────────────────────────────────────────────────────────
# 2. PREPROCESSING  (same as base paper)
# ─────────────────────────────────────────────────────────────
def preprocess(data: pd.DataFrame):
    """
    Steps (mirror base paper):
      1. Drop zero-variance features (46 → 40)
      2. Drop highly correlated features (PCC ≥ 0.90) (40 → 31)
      3. StandardScaler normalization
      4. Binary label: 0=normal, 1=attack
    """
    print("\n🔧 Preprocessing...")

    # Separate label
    label_matches = [c for c in data.columns if c.lower() == "label"]
    label_col = label_matches[0] if label_matches else data.columns[-1]
    X = data.drop(columns=[label_col])
    y = data[label_col]


    # Binary encode: 0 = benign/normal, 1 = attack
    # CICIoT2023's real label values are strings like "BenignTraffic",
    # "DDoS-ICMP_Flood", etc. — not literally "benign"/"normal". An exact-
    # match check here would never match "BenignTraffic" and would
    # silently classify every row (benign or attack) as an attack.
    y_binary = y.apply(lambda v: 0 if any(k in str(v).lower() for k in ("benign", "normal")) or str(v) == "0" else 1)

    # Drop non-numeric
    X = X.select_dtypes(include=[np.number])

    # Drop zero-variance features
    var = X.var()
    zero_var_cols = var[var == 0].index.tolist()
    X.drop(columns=zero_var_cols, inplace=True)
    print(f"   After zero-variance removal: {X.shape[1]} features")

    # Drop highly correlated features (PCC ≥ threshold)
    corr_matrix = X.corr().abs()
    upper = corr_matrix.where(np.triu(np.ones(corr_matrix.shape), k=1).astype(bool))
    to_drop = [col for col in upper.columns if any(upper[col] >= PCC_THRESHOLD)]
    X.drop(columns=to_drop, inplace=True)
    print(f"   After PCC ≥ {PCC_THRESHOLD} removal: {X.shape[1]} features")

    # Fill missing
    X.fillna(X.median(), inplace=True)

    # StandardScaler
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    X_scaled = pd.DataFrame(X_scaled, columns=X.columns)

    print(f"   Final feature count: {X_scaled.shape[1]}")
    print(f"   Class distribution:\n{y_binary.value_counts()}")

    joblib.dump(scaler, os.path.join(OUTPUT_DIR, "scaler.pkl"))
    joblib.dump(list(X.columns), os.path.join(OUTPUT_DIR, "feature_names.pkl"))

    return X_scaled, y_binary, scaler


# ─────────────────────────────────────────────────────────────
# 3. BALANCE WITH SMOTE  (same as base paper)
# ─────────────────────────────────────────────────────────────
def balance_data(X, y):
    print("\n⚖️  Applying SMOTE balancing...")
    sm = SMOTE(random_state=RANDOM_STATE)
    X_bal, y_bal = sm.fit_resample(X, y)
    print(f"   Balanced shape: {X_bal.shape}")
    print(f"   Class distribution after SMOTE:\n{pd.Series(y_bal).value_counts()}")
    return X_bal, y_bal


# ─────────────────────────────────────────────────────────────
# 4. TRAIN / EVALUATE HELPER
# ─────────────────────────────────────────────────────────────
def evaluate(name, y_true, y_pred):
    acc  = accuracy_score(y_true, y_pred)
    prec = precision_score(y_true, y_pred, average="weighted", zero_division=0)
    rec  = recall_score(y_true, y_pred, average="weighted", zero_division=0)
    f1   = f1_score(y_true, y_pred, average="weighted", zero_division=0)
    print(f"\n{'─'*50}")
    print(f"  {name}")
    print(f"{'─'*50}")
    print(f"  Accuracy  : {acc*100:.2f}%")
    print(f"  Precision : {prec*100:.2f}%")
    print(f"  Recall    : {rec*100:.2f}%")
    print(f"  F1-Score  : {f1*100:.2f}%")
    return {"model": name, "accuracy": acc, "precision": prec,
            "recall": rec, "f1": f1}


def plot_confusion(name, y_true, y_pred, labels=["Normal", "Attack"]):
    cm = confusion_matrix(y_true, y_pred)
    plt.figure(figsize=(5, 4))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=labels, yticklabels=labels)
    plt.title(f"Confusion Matrix — {name}")
    plt.ylabel("Actual")
    plt.xlabel("Predicted")
    plt.tight_layout()
    fname = name.lower().replace(" ", "_").replace("+", "and")
    plt.savefig(os.path.join(OUTPUT_DIR, f"cm_{fname}.png"), dpi=150)
    plt.close()
    print(f"   Saved confusion matrix → {os.path.join(OUTPUT_DIR, f'cm_{fname}.png')}")


# ─────────────────────────────────────────────────────────────
# 5a. RANDOM FOREST  (base paper best model — for comparison)
# ─────────────────────────────────────────────────────────────
def train_random_forest(X_train, X_test, y_train, y_test):
    print("\n🌲 Training Random Forest (Base Paper Comparison)...")
    rf = RandomForestClassifier(n_estimators=100, random_state=RANDOM_STATE, n_jobs=-1)
    rf.fit(X_train, y_train)
    preds = rf.predict(X_test)
    result = evaluate("Random Forest (Base Paper)", y_test, preds)
    plot_confusion("Random Forest", y_test, preds)
    return rf, result


# ─────────────────────────────────────────────────────────────
# 5b. XGBOOST  (proposed supervised algorithm)
# ─────────────────────────────────────────────────────────────
def train_xgboost(X_train, X_test, y_train, y_test):
    print("\n⚡ Training XGBoost (Proposed Supervised Algorithm)...")
    xgb = XGBClassifier(
        n_estimators=200,
        max_depth=6,
        learning_rate=0.1,
        subsample=0.8,
        colsample_bytree=0.8,
        use_label_encoder=False,
        eval_metric="logloss",
        random_state=RANDOM_STATE,
        n_jobs=-1,
        verbosity=0
    )
    xgb.fit(
        X_train, y_train,
        eval_set=[(X_test, y_test)],
        verbose=False
    )
    preds = xgb.predict(X_test)
    result = evaluate("XGBoost (Proposed)", y_test, preds)
    plot_confusion("XGBoost", y_test, preds)

    # Cross-validation
    print("\n  5-Fold Cross-Validation (XGBoost)...")
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=RANDOM_STATE)
    cv_scores = cross_val_score(xgb, X_train, y_train, cv=cv,
                                scoring="accuracy", n_jobs=-1)
    print(f"  CV Accuracy: {cv_scores.mean()*100:.2f}% ± {cv_scores.std()*100:.2f}%")

    joblib.dump(xgb, os.path.join(OUTPUT_DIR, "xgboost_model.pkl"))
    print(f"  Model saved → {os.path.join(OUTPUT_DIR, 'xgboost_model.pkl')}")
    return xgb, result


# ─────────────────────────────────────────────────────────────
# 5c. ISOLATION FOREST  (proposed unsupervised algorithm)
# ─────────────────────────────────────────────────────────────
def train_isolation_forest(X_train, X_test, y_test):
    print("\n🌳 Training Isolation Forest (Proposed Unsupervised Algorithm)...")

    # Train ONLY on normal samples (unsupervised — no labels used)
    # In a real pipeline use the actual training set's normal rows
    iso = IsolationForest(
        n_estimators=100,
        contamination=CONTAMINATION,
        random_state=RANDOM_STATE,
        n_jobs=-1
    )
    iso.fit(X_train)

    # Predict: 1=normal, -1=anomaly → convert to 0=normal, 1=attack
    raw = iso.predict(X_test)
    preds = np.where(raw == -1, 1, 0)

    result = evaluate("Isolation Forest (Proposed)", y_test, preds)
    plot_confusion("Isolation Forest", y_test, preds)

    joblib.dump(iso, os.path.join(OUTPUT_DIR, "isolation_forest_model.pkl"))
    print(f"  Model saved → {os.path.join(OUTPUT_DIR, 'isolation_forest_model.pkl')}")
    return iso, result


# ─────────────────────────────────────────────────────────────
# 5d. HYBRID FUSION  (XGBoost + Isolation Forest combined)
# ─────────────────────────────────────────────────────────────
def hybrid_predict(xgb_model, iso_model, X_test, y_test):
    """
    Fusion logic:
      - XGBoost predicts known attack classes (supervised)
      - Isolation Forest flags unknown anomalies (unsupervised)
      - If EITHER flags as attack → final = attack (OR fusion)
      - Priority: XGBoost confidence > IF score
    """
    print("\n🔀 Hybrid Fusion (XGBoost + Isolation Forest)...")

    xgb_preds  = xgb_model.predict(X_test)
    xgb_proba  = xgb_model.predict_proba(X_test)[:, 1]  # attack probability

    iso_raw    = iso_model.predict(X_test)
    iso_preds  = np.where(iso_raw == -1, 1, 0)
    iso_scores = iso_model.decision_function(X_test)      # anomaly score

    # Weighted OR fusion
    # XGBoost gets weight 0.7, IF gets 0.3
    combined_score = (0.7 * xgb_proba) + (0.3 * (1 - (iso_scores - iso_scores.min()) /
                      (iso_scores.max() - iso_scores.min() + 1e-8)))

    hybrid_preds = (combined_score >= 0.5).astype(int)

    result = evaluate("Hybrid XGBoost + IF (Proposed)", y_test, hybrid_preds)
    plot_confusion("Hybrid XGBoost+IF", y_test, hybrid_preds)

    # Alert classification for dashboard
    alerts = []
    for i, (xp, ip, score) in enumerate(zip(xgb_preds, iso_preds, combined_score)):
        if xp == 1 and ip == 1:
            alerts.append("CRITICAL")     # Both agree → definite attack
        elif xp == 1 and ip == 0:
            alerts.append("WARNING")      # XGBoost only → known pattern
        elif xp == 0 and ip == 1:
            alerts.append("UNKNOWN")      # IF only → zero-day / novel
        else:
            alerts.append("NORMAL")

    alert_counts = pd.Series(alerts).value_counts()
    print(f"\n  Alert distribution:\n{alert_counts}")

    return hybrid_preds, result, alerts


# ─────────────────────────────────────────────────────────────
# 6. SHAP INTERPRETABILITY  (enhancement not in base paper)
# ─────────────────────────────────────────────────────────────
def explain_with_shap(xgb_model, X_test, feature_names):
    print("\n🔍 SHAP Interpretability (Enhancement — not in base paper)...")
    try:
        explainer = shap.TreeExplainer(xgb_model)
        # Use a sample for speed
        sample = X_test.iloc[:500] if len(X_test) > 500 else X_test
        shap_values = explainer.shap_values(sample)

        # Summary plot
        plt.figure()
        shap.summary_plot(shap_values, sample, feature_names=feature_names,
                          show=False, plot_size=(10, 6))
        plt.tight_layout()
        plt.savefig(os.path.join(OUTPUT_DIR, "shap_summary.png"), dpi=150, bbox_inches="tight")
        plt.close()
        print(f"   SHAP summary plot saved → {os.path.join(OUTPUT_DIR, 'shap_summary.png')}")

        # Bar plot — top 15 features
        plt.figure()
        shap.summary_plot(shap_values, sample, feature_names=feature_names,
                          plot_type="bar", show=False, max_display=15)
        plt.tight_layout()
        plt.savefig(os.path.join(OUTPUT_DIR, "shap_bar.png"), dpi=150, bbox_inches="tight")
        plt.close()
        print(f"   SHAP bar plot saved → {os.path.join(OUTPUT_DIR, 'shap_bar.png')}")

    except Exception as e:
        print(f"   SHAP skipped: {e}")


# ─────────────────────────────────────────────────────────────
# 7. RESULTS COMPARISON TABLE
# ─────────────────────────────────────────────────────────────
def print_comparison_table(results: list):
    print("\n" + "═"*72)
    print("  RESULTS COMPARISON TABLE")
    print("═"*72)
    print(f"  {'Model':<40} {'Acc':>8} {'Prec':>8} {'Rec':>8} {'F1':>8}")
    print("─"*72)

    # Base paper reference (from paper)
    base_paper_models = [
        ("Logistic Regression (Base Paper)",    0.9812, 0.9810, 0.9812, 0.9811),
        ("Perceptron (Base Paper)",             0.9831, 0.9829, 0.9831, 0.9830),
        ("AdaBoost (Base Paper)",               0.9874, 0.9873, 0.9874, 0.9873),
        ("DNN (Base Paper)",                    0.9921, 0.9920, 0.9921, 0.9920),
        ("Random Forest (Base Paper)",          0.9955, 0.9955, 0.9955, 0.9955),
    ]

    for name, acc, prec, rec, f1 in base_paper_models:
        print(f"  {name:<40} {acc*100:>7.2f}% {prec*100:>7.2f}% "
              f"{rec*100:>7.2f}% {f1*100:>7.2f}%")

    print("─"*72)
    for r in results:
        marker = " ◀ Proposed" if "Proposed" in r["model"] or "Hybrid" in r["model"] else ""
        print(f"  {r['model']:<40} {r['accuracy']*100:>7.2f}% "
              f"{r['precision']*100:>7.2f}% {r['recall']*100:>7.2f}% "
              f"{r['f1']*100:>7.2f}%{marker}")

    print("═"*72)


# ─────────────────────────────────────────────────────────────
# 8. SAVE INFERENCE API FUNCTION  (for Spring Boot integration)
# ─────────────────────────────────────────────────────────────
def predict_single(features: dict) -> dict:
    """
    Called by Spring Boot via subprocess or REST wrapper.
    Input : dict of feature_name -> value
    Output: alert level + confidence
    """
    scaler       = joblib.load(os.path.join(OUTPUT_DIR, "scaler.pkl"))
    feature_names= joblib.load(os.path.join(OUTPUT_DIR, "feature_names.pkl"))
    xgb_model    = joblib.load(os.path.join(OUTPUT_DIR, "xgboost_model.pkl"))
    iso_model    = joblib.load(os.path.join(OUTPUT_DIR, "isolation_forest_model.pkl"))

    row = pd.DataFrame([features])[feature_names]
    row_scaled = scaler.transform(row)

    xgb_proba   = xgb_model.predict_proba(row_scaled)[0][1]
    iso_raw     = iso_model.predict(row_scaled)[0]
    iso_anomaly = 1 if iso_raw == -1 else 0

    combined = 0.7 * xgb_proba + 0.3 * iso_anomaly

    if combined >= 0.7:
        level = "CRITICAL"
    elif combined >= 0.4:
        level = "WARNING"
    else:
        level = "NORMAL"

    return {
        "alert_level":    level,
        "xgb_confidence": round(float(xgb_proba), 4),
        "if_anomaly":     bool(iso_anomaly),
        "combined_score": round(float(combined), 4)
    }


# ─────────────────────────────────────────────────────────────
# MAIN PIPELINE
# ─────────────────────────────────────────────────────────────
def main():
    print("="*60)
    print("  SafeNet — Hybrid ML Anomaly Detection Pipeline")
    print("  XGBoost + Isolation Forest vs Random Forest (Base Paper)")
    print("="*60)

    # 1. Load — uses the real CICIoT2023 CSVs if present, otherwise falls
    # back to synthetic demo data so the script still runs end-to-end for
    # a quick pipeline check without the ~13GB dataset downloaded.
    #
    # DATA_PATH is resolved relative to this script's own file location
    # (see CONFIG above), so this works the same regardless of which
    # directory you happened to run `python safenet_model.py` from.
    dir_exists = os.path.isdir(DATA_PATH)
    csv_files_found = [f for f in os.listdir(DATA_PATH) if f.lower().endswith(".csv")] if dir_exists else []
    has_real_data = bool(csv_files_found)

    if not has_real_data:
        print(f"\n⚠️  Looking for CSVs in: {DATA_PATH}")
        if not dir_exists:
            print("    That folder doesn't exist. Check the path above is where you actually placed the dataset.")
        else:
            contents = os.listdir(DATA_PATH)
            print(f"    Folder exists but contains no .csv files. Contents: {contents[:15]}{'...' if len(contents) > 15 else ''}")
            print("    If you see a subfolder here (e.g. CICIoT2023/CICIoT2023/ from how the zip extracted),")
            print("    move the CSVs up one level so they sit directly inside this folder.")

    if has_real_data:
        print("\n✅ Found CICIoT2023 CSV files — running on real data")
        data = load_dataset_sampled(DATA_PATH, N_SAMPLES_PER_CLASS)

        X_scaled, y_bin, scaler = preprocess(data)
        feat_names = list(X_scaled.columns)

        # 2. Balance
        X_bal, y_bal = balance_data(X_scaled, y_bin)
    else:
        print("    Proceeding in DEMO MODE (synthetic data) — fix the path issue above and re-run to train on real data.")
        np.random.seed(RANDOM_STATE)
        n = 20000
        n_features = 31  # after PCC reduction (same as base paper)
        feat_names = [f"feature_{i}" for i in range(n_features)]

        X_demo = pd.DataFrame(
            np.random.randn(n, n_features), columns=feat_names
        )
        # Simulate attacks as slightly shifted distribution
        y_demo = pd.Series(np.random.choice([0, 1], size=n, p=[0.6, 0.4]))
        X_demo[y_demo == 1] += np.random.uniform(0.3, 0.7, X_demo.shape)[y_demo == 1]

        # 2. Balance
        X_bal, y_bal = balance_data(X_demo, y_demo)

    # 3. Split
    X_train, X_test, y_train, y_test = train_test_split(
        X_bal, y_bal, test_size=TEST_SIZE,
        random_state=RANDOM_STATE, stratify=y_bal
    )
    print(f"\n  Train: {X_train.shape}  |  Test: {X_test.shape}")

    results = []

    # 4. Random Forest (base paper comparison)
    rf_model, rf_result = train_random_forest(X_train, X_test, y_train, y_test)
    results.append(rf_result)

    # 5. XGBoost (proposed supervised)
    xgb_model, xgb_result = train_xgboost(X_train, X_test, y_train, y_test)
    results.append(xgb_result)

    # 6. Isolation Forest (proposed unsupervised)
    iso_model, iso_result = train_isolation_forest(X_train, X_test, y_test)
    results.append(iso_result)

    # 7. Hybrid fusion
    _, hybrid_result, _ = hybrid_predict(xgb_model, iso_model, X_test, y_test)
    results.append(hybrid_result)

    # 8. SHAP
    explain_with_shap(xgb_model, pd.DataFrame(X_test, columns=feat_names), feat_names)

    # 9. Print table
    print_comparison_table(results)

    print(f"\n✅ All outputs saved to: {OUTPUT_DIR}")
    print("   xgboost_model.pkl  |  isolation_forest_model.pkl")
    print("   scaler.pkl         |  feature_names.pkl")
    print("   cm_*.png           |  shap_*.png")


if __name__ == "__main__":
    main()
