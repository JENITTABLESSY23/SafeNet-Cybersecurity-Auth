package com.safenet.dto;

public class ResetPasswordRequest {
    private String token;
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String v) { token = v; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String v) { newPassword = v; }
}
