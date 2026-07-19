package com.safenet.service;

import com.safenet.dto.LoginRequest;
import com.safenet.dto.LoginResponse;
import com.safenet.dto.RegisterRequest;
import com.safenet.entity.AuditLog;
import com.safenet.entity.User;
import com.safenet.repository.AuditLogRepository;
import com.safenet.repository.UserRepository;
import com.safenet.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    @Autowired private UserRepository     userRepo;
    @Autowired private AuditLogRepository auditRepo;
    @Autowired private JwtUtil            jwtUtil;
    @Autowired private PasswordEncoder    encoder;

    private static final String UPLOAD_DIR = "uploads/id-proofs/";
    private static final java.util.Set<String> ALLOWED_ID_PROOF_TYPES =
        java.util.Set.of("image/jpeg", "image/png", "application/pdf");

    // Brute-force protection: tracks failed attempts per username in memory.
    // Fine for a single-instance deployment; move to Redis/DB if scaled horizontally.
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(5);
    private final ConcurrentHashMap<String, FailedLoginTracker> failedAttempts = new ConcurrentHashMap<>();

    private static class FailedLoginTracker {
        int count = 0;
        Instant lockedUntil = null;
    }

    public LoginResponse login(LoginRequest req, String ip) throws Exception {
        String key = req.getUsername() == null ? "" : req.getUsername().toLowerCase();
        FailedLoginTracker tracker = failedAttempts.computeIfAbsent(key, k -> new FailedLoginTracker());

        if (tracker.lockedUntil != null) {
            if (Instant.now().isBefore(tracker.lockedUntil)) {
                long minsLeft = Math.max(1, Duration.between(Instant.now(), tracker.lockedUntil).toMinutes());
                throw new Exception("Too many failed attempts. Try again in " + minsLeft + " minute(s).");
            }
            // Lockout expired — reset.
            tracker.count = 0;
            tracker.lockedUntil = null;
        }

        try {
            User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new Exception("Invalid username or password"));
            if (!user.getHospitalId().equalsIgnoreCase(req.getHospitalId()))
                throw new Exception("Invalid username or password");
            if (!encoder.matches(req.getPassword(), user.getPassword()))
                throw new Exception("Invalid username or password");
            if (!"APPROVED".equals(user.getApprovalStatus()))
                throw new Exception("Account pending admin approval.");
            if (!user.isActive())
                throw new Exception("Account is inactive. Contact admin.");

            failedAttempts.remove(key);
            user.setLastLogin(LocalDateTime.now());
            userRepo.save(user);
            log(user.getId(), "LOGIN", "USER", String.valueOf(user.getId()), "Login from " + ip, ip);
            return new LoginResponse(
                jwtUtil.generateToken(user.getUsername(), user.getRole()),
                user.getUsername(), user.getHospitalId(),
                user.getDepartment(), user.getRole(),
                user.getFirstName() + " " + user.getLastName());
        } catch (Exception e) {
            tracker.count++;
            if (tracker.count >= MAX_ATTEMPTS) {
                tracker.lockedUntil = Instant.now().plus(LOCKOUT_DURATION);
                log(null, "LOGIN_LOCKOUT", "USER", req.getUsername(),
                    "Account locked after " + MAX_ATTEMPTS + " failed attempts from " + ip, ip);
            }
            throw e;
        }
    }

    public String register(RegisterRequest req, MultipartFile idProof) throws Exception {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new Exception("Email already registered.");
        User u = new User();
        u.setFirstName(req.getFirstName()); u.setLastName(req.getLastName());
        u.setUsername(buildUsername(req.getFirstName(), req.getLastName()));
        u.setEmail(req.getEmail()); u.setPhone(req.getPhone());
        u.setDepartment(req.getDepartment()); u.setRole(deptToRole(req.getDepartment()));
        u.setHospitalId(buildHospitalId(req));
        u.setPassword(encoder.encode("TempPass@" + UUID.randomUUID().toString().substring(0, 6)));
        u.setApprovalStatus("PENDING"); u.setActive(false);
        if (idProof != null && !idProof.isEmpty()) {
            String contentType = idProof.getContentType();
            if (contentType == null || !ALLOWED_ID_PROOF_TYPES.contains(contentType)) {
                throw new Exception("ID proof must be a JPEG, PNG, or PDF file.");
            }
            Path dir = Paths.get(UPLOAD_DIR);
            Files.createDirectories(dir);
            // Strip any path components from the client-supplied filename before
            // using it — an unsanitized "../../etc/cron.d/evil" would otherwise
            // let a crafted upload write outside this directory.
            String safeName = Paths.get(idProof.getOriginalFilename() != null ? idProof.getOriginalFilename() : "upload")
                .getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
            String fname = UUID.randomUUID() + "_" + safeName;
            Files.copy(idProof.getInputStream(), dir.resolve(fname));
            u.setIdProofPath(UPLOAD_DIR + fname);
        }
        userRepo.save(u);
        log(null, "REGISTER", "USER", null, "New registration: " + req.getEmail(), null);
        return "Registration submitted. Your Hospital ID: " + u.getHospitalId();
    }

    public void logout(String authHeader, Long userId) {
        String token = authHeader.replace("Bearer ", "");
        try {
            String username = jwtUtil.extractUsername(token);
            User u = userRepo.findByUsername(username).orElse(null);
            if (u != null) userId = u.getId();
        } catch (Exception ignored) { /* token already invalid; blacklist it anyway */ }
        jwtUtil.blacklistToken(token);
        log(userId, "LOGOUT", "USER", userId != null ? String.valueOf(userId) : null, "User signed out", null);
    }

    public User getCurrentUser(String authHeader) throws Exception {
        String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
        return userRepo.findByUsername(username).orElseThrow(() -> new Exception("User not found"));
    }

    private String buildUsername(String first, String last) {
        String base = (first.charAt(0) + "." + last).toLowerCase().replaceAll("[^a-z.]", "");
        String u = base; int c = 1;
        while (userRepo.existsByUsername(u)) u = base + c++;
        return u;
    }

    private String buildHospitalId(RegisterRequest req) {
        String d = req.getDepartment().substring(0, Math.min(2, req.getDepartment().length())).toUpperCase();
        String i = ("" + req.getFirstName().charAt(0) + req.getLastName().charAt(0)).toUpperCase();
        return String.format("SN-%s%s-%03d", d, i, (int)(Math.random() * 900) + 100);
    }

    private String deptToRole(String dept) {
        return switch (dept.toLowerCase()) {
            case "icu" -> "Doc_ICU"; case "cardiology" -> "Doc_Cardio";
            case "gynecology" -> "Doc_Gyno"; case "admin" -> "Admin_Ops";
            default -> "Staff";
        };
    }

    private void log(Long uid, String action, String rt, String rid, String details, String ip) {
        AuditLog l = new AuditLog();
        l.setUserId(uid); l.setAction(action); l.setResourceType(rt);
        l.setResourceId(rid); l.setDetails(details); l.setIpAddress(ip);
        auditRepo.save(l);
    }
}
