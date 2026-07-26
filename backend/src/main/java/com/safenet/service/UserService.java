package com.safenet.service;

import com.safenet.dto.ChangePasswordRequest;
import com.safenet.dto.UpdatePreferencesRequest;
import com.safenet.dto.UpdateProfileRequest;
import com.safenet.entity.AuditLog;
import com.safenet.entity.User;
import com.safenet.repository.AuditLogRepository;
import com.safenet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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

    private static final String PHOTO_DIR = "uploads/profile-photos/";
    private static final long MAX_PHOTO_BYTES = 2 * 1024 * 1024; // matches the "Max 2 MB" the UI already advertises

    public User uploadPhoto(String username, MultipartFile photo) throws Exception {
        User u = userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));

        if (photo == null || photo.isEmpty()) throw new Exception("No file received.");
        if (photo.getSize() > MAX_PHOTO_BYTES) throw new Exception("Photo must be 2 MB or smaller.");
        String contentType = photo.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
            throw new Exception("Photo must be a JPG or PNG image.");
        }

        // Remove any previous photo so old files don't pile up on disk.
        deletePhotoFileIfPresent(u);

        Path dir = Paths.get(PHOTO_DIR);
        Files.createDirectories(dir);
        String ext = contentType.equals("image/png") ? ".png" : ".jpg";
        String fname = "user" + u.getId() + "_" + UUID.randomUUID() + ext;
        Files.copy(photo.getInputStream(), dir.resolve(fname));

        u.setPhotoPath(dir.resolve(fname).toString());
        userRepo.save(u);
        log(u.getId(), "PROFILE_PHOTO_UPDATE", "USER", String.valueOf(u.getId()), "Profile photo uploaded");
        return u;
    }

    public void removePhoto(String username) throws Exception {
        User u = userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));
        deletePhotoFileIfPresent(u);
        u.setPhotoPath(null);
        userRepo.save(u);
        log(u.getId(), "PROFILE_PHOTO_REMOVE", "USER", String.valueOf(u.getId()), "Profile photo removed");
    }

    private void deletePhotoFileIfPresent(User u) {
        if (u.getPhotoPath() == null) return;
        try { Files.deleteIfExists(Paths.get(u.getPhotoPath())); } catch (Exception ignored) {}
    }

    public User updatePreferences(String username, UpdatePreferencesRequest req) throws Exception {
        User u = userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));

        if (req.getLanguage()   != null && !req.getLanguage().isBlank())   u.setLanguage(req.getLanguage());
        if (req.getTimeZone()   != null && !req.getTimeZone().isBlank())   u.setTimeZone(req.getTimeZone());
        if (req.getDateFormat() != null && !req.getDateFormat().isBlank()) u.setDateFormat(req.getDateFormat());
        if (req.getDarkMode()   != null)                                  u.setDarkMode(req.getDarkMode());

        userRepo.save(u);
        log(u.getId(), "PREFERENCES_UPDATE", "USER", String.valueOf(u.getId()), "Display preferences updated");
        return u;
    }

    private void log(Long uid, String action, String rt, String rid, String details) {
        AuditLog l = new AuditLog();
        l.setUserId(uid); l.setAction(action); l.setResourceType(rt);
        l.setResourceId(rid); l.setDetails(details);
        auditRepo.save(l);
    }
}
