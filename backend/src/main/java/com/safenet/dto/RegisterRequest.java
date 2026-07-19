package com.safenet.dto;

public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private String role;
    private String notes;

    public String getFirstName()           { return firstName; }
    public void   setFirstName(String v)   { firstName = v; }
    public String getLastName()            { return lastName; }
    public void   setLastName(String v)    { lastName = v; }
    public String getEmail()               { return email; }
    public void   setEmail(String v)       { email = v; }
    public String getPhone()               { return phone; }
    public void   setPhone(String v)       { phone = v; }
    public String getDepartment()          { return department; }
    public void   setDepartment(String v)  { department = v; }
    public String getRole()                { return role; }
    public void   setRole(String v)        { role = v; }
    public String getNotes()               { return notes; }
    public void   setNotes(String v)       { notes = v; }
}
