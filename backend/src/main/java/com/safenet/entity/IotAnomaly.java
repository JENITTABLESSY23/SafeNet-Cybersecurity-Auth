package com.safenet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "iot_anomalies")
public class IotAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String classification;

    @Column(name = "xgb_confidence")
    private Double xgbConfidence;

    @Column(name = "if_anomaly")
    private Boolean ifAnomaly;

    @Column(name = "combined_score")
    private Double combinedScore;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "detected_at", updatable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    public Long      getId()                        { return id; }
    public String    getNodeId()                    { return nodeId; }
    public void      setNodeId(String v)            { nodeId = v; }
    public String    getSeverity()                  { return severity; }
    public void      setSeverity(String v)          { severity = v; }
    public String    getMessage()                   { return message; }
    public void      setMessage(String v)           { message = v; }
    public String    getClassification()            { return classification; }
    public void      setClassification(String v)    { classification = v; }
    public Double    getXgbConfidence()             { return xgbConfidence; }
    public void      setXgbConfidence(Double v)     { xgbConfidence = v; }
    public Boolean   getIfAnomaly()                 { return ifAnomaly; }
    public void      setIfAnomaly(Boolean v)        { ifAnomaly = v; }
    public Double    getCombinedScore()             { return combinedScore; }
    public void      setCombinedScore(Double v)     { combinedScore = v; }
    public boolean   isResolved()                   { return resolved; }
    public void      setResolved(boolean v)         { resolved = v; }
    public Long      getResolvedBy()                { return resolvedBy; }
    public void      setResolvedBy(Long v)          { resolvedBy = v; }
    public LocalDateTime getResolvedAt()            { return resolvedAt; }
    public void      setResolvedAt(LocalDateTime v) { resolvedAt = v; }
    public LocalDateTime getDetectedAt()            { return detectedAt; }
}
