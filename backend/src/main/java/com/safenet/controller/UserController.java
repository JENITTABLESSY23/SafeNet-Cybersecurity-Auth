package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.dto.ChangePasswordRequest;
import com.safenet.dto.UpdatePreferencesRequest;
import com.safenet.dto.UpdateProfileRequest;
import com.safenet.entity.User;
import com.safenet.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserService userService;

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(currentUsername())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody UpdateProfileRequest req) {
        try {
            User u = userService.updateProfile(currentUsername(), req);
            return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", u));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req) {
        try {
            userService.changePassword(currentUsername(), req);
            return ResponseEntity.ok(ApiResponse.ok("Password updated successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody UpdatePreferencesRequest req) {
        try {
            User u = userService.updatePreferences(currentUsername(), req);
            return ResponseEntity.ok(ApiResponse.ok("Preferences saved", u));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/me/photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("photo") MultipartFile photo) {
        try {
            User u = userService.uploadPhoto(currentUsername(), photo);
            return ResponseEntity.ok(ApiResponse.ok("Photo uploaded", u));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/me/photo")
    public ResponseEntity<?> removePhoto() {
        try {
            userService.removePhoto(currentUsername());
            return ResponseEntity.ok(ApiResponse.ok("Photo removed", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Any authenticated user can fetch this — it always resolves to the
     *  calling account's own photo (via currentUsername()), never someone
     *  else's, so there's no need to restrict this to a specific role. */
    @GetMapping("/me/photo")
    public ResponseEntity<?> getPhoto() {
        try {
            User u = userService.getProfile(currentUsername());
            if (u.getPhotoPath() == null) return ResponseEntity.notFound().build();
            Path path = Paths.get(u.getPhotoPath());
            if (!Files.exists(path)) return ResponseEntity.notFound().build();
            byte[] data = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
