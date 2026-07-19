package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.dto.ForgotPasswordRequest;
import com.safenet.dto.ResetPasswordRequest;
import com.safenet.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    @Autowired private PasswordResetService resetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        // Always the same response, whether or not the email exists —
        // see PasswordResetService.requestReset() for why.
        resetService.requestReset(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
            "If an account exists with that email, a password reset link has been sent.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        try {
            resetService.resetPassword(req.getToken(), req.getNewPassword());
            return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. You can now sign in.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
