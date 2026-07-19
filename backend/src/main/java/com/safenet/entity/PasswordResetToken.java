package com.safenet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long          getId()                    { return id; }
    public Long          getUserId()                 { return userId; }
    public void          setUserId(Long v)            { userId = v; }
    public String        getToken()                   { return token; }
    public void          setToken(String v)            { token = v; }
    public LocalDateTime getExpiresAt()                { return expiresAt; }
    public void          setExpiresAt(LocalDateTime v)  { expiresAt = v; }
    public boolean       isUsed()                       { return used; }
    public void          setUsed(boolean v)              { used = v; }
    public LocalDateTime getCreatedAt()                  { return createdAt; }

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public boolean isValid()   { return !used && !isExpired(); }
}
