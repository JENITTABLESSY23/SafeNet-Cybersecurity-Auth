package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.dto.LoginRequest;
import com.safenet.dto.LoginResponse;
import com.safenet.dto.RegisterRequest;
import com.safenet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(ApiResponse.ok("SafeNet API is running", null));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest httpReq) {
        try {
            LoginResponse resp = authService.login(req, httpReq.getRemoteAddr());
            return ResponseEntity.ok(ApiResponse.ok("Login successful", resp));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String department,
            @RequestParam(required = false, defaultValue = "") String role,
            @RequestParam(required = false) MultipartFile idProof) {
        try {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName(firstName); req.setLastName(lastName);
            req.setEmail(email);         req.setPhone(phone);
            req.setDepartment(department); req.setRole(role);
            String msg = authService.register(req, idProof);
            return ResponseEntity.ok(ApiResponse.ok(msg, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            authService.logout(authHeader, null);
            return ResponseEntity.ok(ApiResponse.ok("Signed out successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(authService.getCurrentUser(authHeader)));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }
}
