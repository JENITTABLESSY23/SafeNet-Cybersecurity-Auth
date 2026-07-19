package com.safenet.dto;

public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String designation;

    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { lastName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getDesignation() { return designation; }
    public void setDesignation(String v) { designation = v; }
}
