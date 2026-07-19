package com.safenet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String bed;

    @Column(nullable = false)
    private String department;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String priority = "ROUTINE";

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    public Long      getId()                          { return id; }
    public String    getOrderNumber()                 { return orderNumber; }
    public void      setOrderNumber(String v)         { orderNumber = v; }
    public Long      getPatientId()                   { return patientId; }
    public void      setPatientId(Long v)             { patientId = v; }
    public String    getBed()                         { return bed; }
    public void      setBed(String v)                 { bed = v; }
    public String    getDepartment()                  { return department; }
    public void      setDepartment(String v)          { department = v; }
    public String    getOrderType()                   { return orderType; }
    public void      setOrderType(String v)           { orderType = v; }
    public String    getDescription()                 { return description; }
    public void      setDescription(String v)         { description = v; }
    public String    getPriority()                    { return priority; }
    public void      setPriority(String v)            { priority = v; }
    public String    getStatus()                      { return status; }
    public void      setStatus(String v)              { status = v; }
    public Long      getRequestedBy()                 { return requestedBy; }
    public void      setRequestedBy(Long v)           { requestedBy = v; }
    public Long      getConfirmedBy()                 { return confirmedBy; }
    public void      setConfirmedBy(Long v)           { confirmedBy = v; }
    public LocalDateTime getRequestedAt()             { return requestedAt; }
    public LocalDateTime getConfirmedAt()             { return confirmedAt; }
    public void      setConfirmedAt(LocalDateTime v)  { confirmedAt = v; }
}
