package com.safenet.dto;

public class LoginRequest {
    private String username;
    private String hospitalId;
    private String password;
    private String department;

    public String getUsername()           { return username; }
    public void   setUsername(String v)   { username = v; }
    public String getHospitalId()         { return hospitalId; }
    public void   setHospitalId(String v) { hospitalId = v; }
    public String getPassword()           { return password; }
    public void   setPassword(String v)   { password = v; }
    public String getDepartment()         { return department; }
    public void   setDepartment(String v) { department = v; }
}
