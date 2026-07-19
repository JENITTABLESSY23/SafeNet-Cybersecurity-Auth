package com.safenet.dto;

public class LoginResponse {
    private String token;
    private String username;
    private String hospitalId;
    private String department;
    private String role;
    private String fullName;

    public LoginResponse(String token, String username, String hospitalId,
                         String department, String role, String fullName) {
        this.token = token; this.username = username;
        this.hospitalId = hospitalId; this.department = department;
        this.role = role; this.fullName = fullName;
    }

    public String getToken()      { return token; }
    public String getUsername()   { return username; }
    public String getHospitalId() { return hospitalId; }
    public String getDepartment() { return department; }
    public String getRole()       { return role; }
    public String getFullName()   { return fullName; }
}
