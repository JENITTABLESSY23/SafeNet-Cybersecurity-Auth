package com.safenet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", unique = true, nullable = false)
    private String patientId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String bed;

    @Column(nullable = false)
    private String diagnosis;

    @Column(nullable = false)
    private String status = "Stable";

    private String contact;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "admitted_at", updatable = false)
    private LocalDateTime admittedAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long      getId()                       { return id; }
    public String    getPatientId()                { return patientId; }
    public void      setPatientId(String v)        { patientId = v; }
    public String    getFirstName()                { return firstName; }
    public void      setFirstName(String v)        { firstName = v; }
    public String    getLastName()                 { return lastName; }
    public void      setLastName(String v)         { lastName = v; }
    public int       getAge()                      { return age; }
    public void      setAge(int v)                 { age = v; }
    public String    getGender()                   { return gender; }
    public void      setGender(String v)           { gender = v; }
    public String    getDepartment()               { return department; }
    public void      setDepartment(String v)       { department = v; }
    public String    getBed()                      { return bed; }
    public void      setBed(String v)              { bed = v; }
    public String    getDiagnosis()                { return diagnosis; }
    public void      setDiagnosis(String v)        { diagnosis = v; }
    public String    getStatus()                   { return status; }
    public void      setStatus(String v)           { status = v; }
    public String    getContact()                  { return contact; }
    public void      setContact(String v)          { contact = v; }
    public String    getNotes()                    { return notes; }
    public void      setNotes(String v)            { notes = v; }
    public LocalDateTime getAdmittedAt()           { return admittedAt; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }
}
