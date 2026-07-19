package com.safenet.service;

import com.safenet.dto.ChangePasswordRequest;
import com.safenet.dto.UpdateProfileRequest;
import com.safenet.entity.AuditLog;
import com.safenet.entity.User;
import com.safenet.repository.AuditLogRepository;
import com.safenet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired private UserRepository userRepo;
    @Autowired private AuditLogRepository auditRepo;
    @Autowired private PasswordEncoder encoder;

    public User getProfile(String username) throws Exception {
        return userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));
    }

    public User updateProfile(String username, UpdateProfileRequest req) throws Exception {
        User u = userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(u.getEmail())
                && userRepo.existsByEmail(req.getEmail())) {
            throw new Exception("Email already in use by another account.");
        }

        if (req.getFirstName() != null && !req.getFirstName().isBlank()) u.setFirstName(req.getFirstName());
        if (req.getLastName()  != null && !req.getLastName().isBlank())  u.setLastName(req.getLastName());
        if (req.getEmail()     != null && !req.getEmail().isBlank())     u.setEmail(req.getEmail());
        if (req.getPhone()     != null && !req.getPhone().isBlank())     u.setPhone(req.getPhone());
        if (req.getDesignation() != null && !req.getDesignation().isBlank()) u.setDesignation(req.getDesignation());

        userRepo.save(u);
        log(u.getId(), "PROFILE_UPDATE", "USER", String.valueOf(u.getId()), "Profile fields updated");
        return u;
    }

    public void changePassword(String username, ChangePasswordRequest req) throws Exception {
        User u = userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));

        if (req.getCurrentPassword() == null || !encoder.matches(req.getCurrentPassword(), u.getPassword())) {
            throw new Exception("Current password is incorrect.");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 8
                || !req.getNewPassword().matches(".*[A-Z].*")
                || !req.getNewPassword().matches(".*[0-9].*")
                || !req.getNewPassword().matches(".*[!@#$%^&*].*")) {
            throw new Exception("New password must be at least 8 characters and include an uppercase letter, a number, and a special character.");
        }

        u.setPassword(encoder.encode(req.getNewPassword()));
        userRepo.save(u);
        log(u.getId(), "PASSWORD_CHANGE", "USER", String.valueOf(u.getId()), "Password changed by user");
    }

    private void log(Long uid, String action, String rt, String rid, String details) {
        AuditLog l = new AuditLog();
        l.setUserId(uid); l.setAction(action); l.setResourceType(rt);
        l.setResourceId(rid); l.setDetails(details);
        auditRepo.save(l);
    }
}
