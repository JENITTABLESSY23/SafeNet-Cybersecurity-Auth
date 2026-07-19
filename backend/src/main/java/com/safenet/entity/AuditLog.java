package com.safenet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long      getId()                    { return id; }
    public Long      getUserId()                { return userId; }
    public void      setUserId(Long v)          { userId = v; }
    public String    getAction()                { return action; }
    public void      setAction(String v)        { action = v; }
    public String    getResourceType()          { return resourceType; }
    public void      setResourceType(String v)  { resourceType = v; }
    public String    getResourceId()            { return resourceId; }
    public void      setResourceId(String v)    { resourceId = v; }
    public String    getDetails()               { return details; }
    public void      setDetails(String v)       { details = v; }
    public String    getIpAddress()             { return ipAddress; }
    public void      setIpAddress(String v)     { ipAddress = v; }
    public String    getNodeId()                { return nodeId; }
    public void      setNodeId(String v)        { nodeId = v; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
}
