package com.safenet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "hospital_id", nullable = false, unique = true)
    private String hospitalId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    // Free-text job title (e.g. "Consultant Physician") — distinct from
    // `role`, which is the RBAC role string (Doc_ICU, Admin_Ops, etc.)
    // used for authorization. This is purely descriptive.
    private String designation;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String role;

    @Column(name = "approval_status", nullable = false)
    private String approvalStatus = "PENDING";

    @Column(name = "id_proof_path")
    private String idProofPath;

    @Column(name = "photo_path")
    private String photoPath;

    // Display preferences — defaults match what settings.html already shows
    // as placeholder values, so existing accounts don't see a sudden change.
    @Column(name = "language")
    private String language = "English (India)";

    @Column(name = "time_zone")
    private String timeZone = "IST — Asia/Kolkata (UTC+5:30)";

    @Column(name = "date_format")
    private String dateFormat = "DD/MM/YYYY";

    @Column(name = "dark_mode")
    private boolean darkMode = false;

    @Column(name = "is_active")
    private boolean isActive = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long      getId()                       { return id; }
    public String    getFirstName()                { return firstName; }
    public void      setFirstName(String v)        { firstName = v; }
    public String    getLastName()                 { return lastName; }
    public void      setLastName(String v)         { lastName = v; }
    public String    getUsername()                 { return username; }
    public void      setUsername(String v)         { username = v; }
    public String    getHospitalId()               { return hospitalId; }
    public void      setHospitalId(String v)       { hospitalId = v; }
    @JsonIgnore
    public String    getPassword()                 { return password; }
    public void      setPassword(String v)         { password = v; }
    public String    getEmail()                    { return email; }
    public void      setEmail(String v)            { email = v; }
    public String    getPhone()                    { return phone; }
    public void      setPhone(String v)            { phone = v; }
    public String    getDesignation()              { return designation; }
    public void      setDesignation(String v)      { designation = v; }
    public String    getDepartment()               { return department; }
    public void      setDepartment(String v)       { department = v; }
    public String    getRole()                     { return role; }
    public void      setRole(String v)             { role = v; }
    public String    getApprovalStatus()           { return approvalStatus; }
    public void      setApprovalStatus(String v)   { approvalStatus = v; }
    public String    getIdProofPath()              { return idProofPath; }
    public void      setIdProofPath(String v)      { idProofPath = v; }
    public String    getPhotoPath()                { return photoPath; }
    public void      setPhotoPath(String v)        { photoPath = v; }
    public String    getLanguage()                 { return language; }
    public void      setLanguage(String v)         { language = v; }
    public String    getTimeZone()                 { return timeZone; }
    public void      setTimeZone(String v)         { timeZone = v; }
    public String    getDateFormat()               { return dateFormat; }
    public void      setDateFormat(String v)       { dateFormat = v; }
    public boolean   isDarkMode()                  { return darkMode; }
    public void      setDarkMode(boolean v)        { darkMode = v; }
    public boolean   isActive()                    { return isActive; }
    public void      setActive(boolean v)          { isActive = v; }
    public LocalDateTime getLastLogin()            { return lastLogin; }
    public void      setLastLogin(LocalDateTime v) { lastLogin = v; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
}
