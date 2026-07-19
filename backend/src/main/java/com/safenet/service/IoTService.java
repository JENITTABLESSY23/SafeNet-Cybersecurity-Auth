package com.safenet.service;

import com.safenet.entity.IotAnomaly;
import com.safenet.repository.IotAnomalyRepository;
import com.safenet.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class IoTService {

    @Autowired private IotAnomalyRepository anomalyRepo;
    @Autowired private PatientRepository patientRepo;

    @Value("${ml.api.url:http://localhost:5000}")
    private String mlApiUrl;

    @Value("${iot.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random rnd = new Random();

    /**
     * Runs every 30s, pulling live bed occupancy from Patients and pushing a
     * synthetic telemetry reading for each bedside IoT node through the ML
     * detection pipeline. This is what actually keeps the anomaly dashboard
     * and admin stats populated — previously vitals() and runDetection()
     * were never connected to each other, so nothing triggered detection
     * unless a client called /api/iot/detect manually.
     *
     * Feature values are synthetic stand-ins for real network/device
     * telemetry (packet timing, request rate, etc. from the CICIoT2023
     * feature space) since no physical IoT hardware is attached in this
     * deployment. Swap generateTelemetry() for a real sensor/gateway feed
     * when hardware is available.
     */
    @Scheduled(fixedDelayString = "${iot.monitoring.interval-ms:30000}")
    public void monitorBeds() {
        if (!monitoringEnabled) return;
        try {
            var beds = patientRepo.findAll().stream()
                .map(p -> p.getBed())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
            for (String bed : beds) {
                Map<String, Object> body = new HashMap<>();
                body.put("nodeId", "BED-" + bed);
                body.put("features", generateTelemetry());
                runDetection(body);
            }
        } catch (Exception ignored) {
            // ML API may be offline in dev; detection simply resumes next cycle.
        }
    }

    private Map<String, Object> generateTelemetry() {
        Map<String, Object> features = new HashMap<>();
        // ~92% of cycles look normal; ~8% simulate a suspicious spike so the
        // demo dashboard has something to show without manual triggering.
        boolean simulateAnomaly = rnd.nextDouble() < 0.08;
        for (int i = 0; i < 5; i++) {
            double base = rnd.nextGaussian() * (simulateAnomaly ? 4.0 : 1.0);
            features.put("feature_" + i, base);
        }
        return features;
    }

    public Map<String, Object> getVitals(String bedId) {
        Random rnd = new Random();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("bedId", bedId);
        if (bedId.contains("07")) {
            v.put("heartRate", 105 + rnd.nextInt(15));
            v.put("spo2",       91 + rnd.nextInt(4));
            v.put("rr",         18 + rnd.nextInt(4));
            v.put("bp",         "158/98");
            v.put("temp",       38.2 + rnd.nextDouble() * 0.5);
            v.put("status",     "CRITICAL");
        } else {
            v.put("heartRate",  70 + rnd.nextInt(12));
            v.put("spo2",       96 + rnd.nextInt(4));
            v.put("rr",         14 + rnd.nextInt(6));
            v.put("bp",         "118/76");
            v.put("temp",       36.5 + rnd.nextDouble() * 0.8);
            v.put("status",     "NORMAL");
        }
        v.put("timestamp", System.currentTimeMillis());
        return v;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> runDetection(Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                mlApiUrl + "/predict", new HttpEntity<>(body, headers), Map.class);
            Map<String, Object> result = resp.getBody();
            if (result != null && !"NORMAL".equals(result.get("alert_level"))) {
                IotAnomaly a = new IotAnomaly();
                a.setNodeId((String) body.getOrDefault("nodeId", "UNKNOWN"));
                a.setSeverity((String) result.get("alert_level"));
                a.setMessage("Anomaly detected by XGBoost + Isolation Forest hybrid engine");
                a.setClassification((String) result.getOrDefault("attack_category", "Unknown"));
                a.setXgbConfidence(toDouble(result.get("xgb_confidence")));
                a.setIfAnomaly((Boolean) result.getOrDefault("if_anomaly", false));
                a.setCombinedScore(toDouble(result.get("combined_score")));
                anomalyRepo.save(a);
            }
            return result != null ? result : Map.of("alert_level", "NORMAL");
        } catch (Exception e) {
            return Map.of("alert_level", "UNKNOWN", "error", "ML API unavailable: " + e.getMessage());
        }
    }

    public List<IotAnomaly> getAnomalies(boolean resolved) {
        return anomalyRepo.findByResolvedOrderByDetectedAtDesc(resolved);
    }

    public Map<String, Object> getAnomalyStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("critical", anomalyRepo.countBySeverityAndResolved("CRITICAL", false));
        s.put("warning",  anomalyRepo.countBySeverityAndResolved("WARNING",  false));
        s.put("total",    anomalyRepo.count());
        return s;
    }

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }
}
