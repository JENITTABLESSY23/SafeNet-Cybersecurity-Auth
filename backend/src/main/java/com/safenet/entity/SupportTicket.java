package com.safenet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, RESOLVED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long          getId()                  { return id; }
    public Long          getUserId()               { return userId; }
    public void          setUserId(Long v)         { userId = v; }
    public String        getCategory()             { return category; }
    public void          setCategory(String v)     { category = v; }
    public String        getSubject()               { return subject; }
    public void          setSubject(String v)       { subject = v; }
    public String        getDetails()               { return details; }
    public void          setDetails(String v)       { details = v; }
    public String        getStatus()                { return status; }
    public void          setStatus(String v)        { status = v; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
}
