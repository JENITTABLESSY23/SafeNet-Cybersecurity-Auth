package com.safenet.service;

import com.safenet.entity.AuditLog;
import com.safenet.entity.PasswordResetToken;
import com.safenet.entity.User;
import com.safenet.repository.AuditLogRepository;
import com.safenet.repository.PasswordResetTokenRepository;
import com.safenet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordResetTokenRepository tokenRepo;
    @Autowired private AuditLogRepository auditRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JavaMailSender mailSender;

    @Value("${safenet.mail.from:no-reply@safenet.local}")
    private String fromAddress;

    // Where the reset link should point — the frontend page that reads the
    // ?token= param and lets the user set a new password.
    @Value("${safenet.frontend-url:http://localhost:5500}")
    private String frontendUrl;

    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(30);

    // Basic per-email cooldown so the reset endpoint can't be hammered to
    // spam someone's inbox. Same in-memory-tracker pattern as the login
    // lockout in AuthService.
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(30);
    private final ConcurrentHashMap<String, RequestTracker> requestTracker = new ConcurrentHashMap<>();

    private static class RequestTracker {
        int count = 0;
        Instant windowStart = Instant.now();
    }

    /**
     * Always returns normally (no exception) whether or not the email
     * belongs to an account — the caller should show the same generic
     * message either way. Revealing "no account with that email" lets an
     * attacker enumerate valid accounts; not worth the minor UX gain here.
     */
    public void requestReset(String email) {
        if (email == null || email.isBlank()) return;
        String key = email.trim().toLowerCase();

        RequestTracker tracker = requestTracker.computeIfAbsent(key, k -> new RequestTracker());
        synchronized (tracker) {
            if (Instant.now().isAfter(tracker.windowStart.plus(REQUEST_WINDOW))) {
                tracker.count = 0;
                tracker.windowStart = Instant.now();
            }
            if (tracker.count >= MAX_REQUESTS_PER_WINDOW) {
                return; // silently drop — same outward behavior as a normal request
            }
            tracker.count++;
        }

        userRepo.findByEmail(key).ifPresent(user -> {
            PasswordResetToken t = new PasswordResetToken();
            t.setUserId(user.getId());
            t.setToken(UUID.randomUUID().toString());
            t.setExpiresAt(LocalDateTime.now().plus(TOKEN_VALIDITY));
            tokenRepo.save(t);

            sendResetEmail(user, t.getToken());
            log(user.getId(), "PASSWORD_RESET_REQUESTED", "USER", String.valueOf(user.getId()),
                "Password reset link emailed to " + user.getEmail());
        });
    }

    private void sendResetEmail(User user, String token) {
        String link = frontendUrl + "/reset-password.html?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(user.getEmail());
        msg.setSubject("SafeNet — Reset your password");
        msg.setText(
            "Hi " + user.getFirstName() + ",\n\n" +
            "We received a request to reset your SafeNet password. Click the link below to choose a new one:\n\n" +
            link + "\n\n" +
            "This link expires in 30 minutes. If you didn't request this, you can safely ignore this email — " +
            "your password will not be changed.\n\n" +
            "— SafeNet"
        );
        mailSender.send(msg);
    }

    public void resetPassword(String token, String newPassword) throws Exception {
        PasswordResetToken t = tokenRepo.findByToken(token)
            .orElseThrow(() -> new Exception("This reset link is invalid."));

        if (!t.isValid()) {
            throw new Exception(t.isUsed()
                ? "This reset link has already been used."
                : "This reset link has expired. Request a new one.");
        }

        if (newPassword == null || newPassword.length() < 8
                || !newPassword.matches(".*[A-Z].*")
                || !newPassword.matches(".*[0-9].*")
                || !newPassword.matches(".*[!@#$%^&*].*")) {
            throw new Exception("Password must be at least 8 characters and include an uppercase letter, a number, and a special character.");
        }

        User user = userRepo.findById(t.getUserId())
            .orElseThrow(() -> new Exception("Account no longer exists."));

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);

        t.setUsed(true);
        tokenRepo.save(t);

        log(user.getId(), "PASSWORD_RESET_COMPLETED", "USER", String.valueOf(user.getId()),
            "Password reset via emailed link");
    }

    private void log(Long uid, String action, String rt, String rid, String details) {
        AuditLog l = new AuditLog();
        l.setUserId(uid); l.setAction(action); l.setResourceType(rt);
        l.setResourceId(rid); l.setDetails(details);
        auditRepo.save(l);
    }
}
