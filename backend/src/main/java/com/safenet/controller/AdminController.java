package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.entity.User;
import com.safenet.repository.UserRepository;
import com.safenet.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private UserRepository userRepo;

    // Resolves the acting admin's own user ID from their JWT-backed session,
    // instead of trusting a client-supplied ID. Used for every audit-logged action below.
    private Long currentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepo.findByUsername(auth.getName()).map(User::getId).orElse(null);
    }

    @GetMapping("/approvals")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPendingApprovals()));
    }

    /**
     * Streams back a registrant's uploaded ID proof so the admin panel can
     * actually display it — previously the panel just showed the text "ID
     * proof on file" with nothing behind it, since this endpoint didn't
     * exist and AuthService.register() only ever wrote the file to local
     * disk without exposing a way to read it back.
     *
     * No separate @PreAuthorize needed here: this whole controller is
     * already restricted to Admin_Ops by SecurityConfig's
     * "/api/admin/**".hasRole("Admin_Ops") rule.
     */
    @GetMapping("/id-proof/{userId}")
    public ResponseEntity<?> idProof(@PathVariable Long userId) {
        User u = userRepo.findById(userId).orElse(null);
        if (u == null || u.getIdProofPath() == null || u.getIdProofPath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path path = Paths.get(u.getIdProofPath());
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            byte[] data = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                // "inline" so PDFs/images open directly in the browser tab
                // instead of forcing a download.
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Could not read file: " + e.getMessage()));
        }
    }

    @PutMapping("/approve/{userId}")
    public ResponseEntity<?> approve(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        try {
            adminService.approveUser(userId, body.get("role"), currentAdminId());
            return ResponseEntity.ok(ApiResponse.ok("Staff approved with role: " + body.get("role"), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/reject/{userId}")
    public ResponseEntity<?> reject(@PathVariable Long userId) {
        try {
            adminService.rejectUser(userId, currentAdminId());
            return ResponseEntity.ok(ApiResponse.ok("Registration rejected", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/staff")
    public ResponseEntity<?> staff() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllActiveStaff()));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboardStats()));
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAuditLog()));
    }

    @PostMapping("/btg")
    public ResponseEntity<?> btg(@RequestBody Map<String, String> body) {
        try {
            // Break-the-glass is a security-critical audit event — the actor must be the
            // authenticated caller, never a value the client claims in the request body.
            adminService.logBreakTheGlass(currentAdminId(), body.get("reason"), body.get("nodeId"));
            return ResponseEntity.ok(ApiResponse.ok("Override activated and logged", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/anomalies/{id}/resolve")
    public ResponseEntity<?> resolveAnomaly(@PathVariable Long id) {
        try {
            adminService.resolveAnomaly(id, currentAdminId());
            return ResponseEntity.ok(ApiResponse.ok("Anomaly resolved", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
