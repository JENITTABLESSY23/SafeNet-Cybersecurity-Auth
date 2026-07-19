package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.entity.Patient;
import com.safenet.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Department-scoped patient access.
 *
 * Every clinical role is tied to exactly one department (Doc_ICU / Nurse_ICU
 * -> ICU, Doc_Cardio / Nurse_Cardio -> Cardiology, Doc_Gyno / Nurse_Gyno ->
 * Gynecology — the same mapping AuthService.deptToRole() uses at registration
 * time). Every endpoint here filters through that department before touching
 * the database, so a Cardiology doctor's token simply cannot retrieve, edit,
 * or delete an ICU or Gynecology patient record — not just hidden in the UI,
 * enforced here regardless of how the request is made.
 *
 * Admin_Ops is the only role with unrestricted cross-department access
 * (callerDepartment() returns null for it), since the admin console needs
 * oversight across all wards. Anything else (e.g. "Staff", which isn't tied
 * to a clinical department) is scoped to "General" and will see nothing
 * outside that bucket — deny-by-default rather than falling through to
 * "see everything".
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired private PatientService patientService;

    /** Department the current caller is allowed to see, or null for unrestricted (Admin_Ops). */
    private String callerDepartment() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("");
        return switch (role) {
            case "Doc_ICU", "Nurse_ICU"       -> "ICU";
            case "Doc_Cardio", "Nurse_Cardio" -> "Cardiology";
            case "Doc_Gyno", "Nurse_Gyno"     -> "Gynecology";
            case "Admin_Ops"                  -> null; // unrestricted
            default                           -> "General";
        };
    }

    private boolean inScope(Patient p, String callerDept) {
        return callerDept == null || callerDept.equalsIgnoreCase(p.getDepartment());
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        String dept = callerDepartment();
        List<Patient> patients = dept == null ? patientService.getAll() : patientService.getByDepartment(dept);
        return ResponseEntity.ok(ApiResponse.ok(patients));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        String dept = callerDepartment();
        Optional<Patient> patient = patientService.getById(id).filter(p -> inScope(p, dept));
        // Same 404 whether the id doesn't exist or just isn't in the caller's
        // department — a 403 here would confirm the id belongs to someone
        // else's ward, which is its own small information leak.
        return patient
            .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q) {
        String dept = callerDepartment();
        List<Patient> results = patientService.search(q).stream()
            .filter(p -> inScope(p, dept))
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/department/{dept}")
    public ResponseEntity<?> byDepartment(@PathVariable String dept) {
        String callerDept = callerDepartment();
        if (callerDept != null && !callerDept.equalsIgnoreCase(dept)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Not authorized to view " + dept + " patients"));
        }
        return ResponseEntity.ok(ApiResponse.ok(patientService.getByDepartment(dept)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> byStatus(@PathVariable String status) {
        String dept = callerDepartment();
        List<Patient> results = patientService.getByStatus(status).stream()
            .filter(p -> inScope(p, dept))
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Patient patient) {
        String dept = callerDepartment();
        // Non-admins can only ever create a patient in their own department —
        // silently correct the payload rather than trusting the client's
        // department field, so a crafted request can't plant a record
        // elsewhere.
        if (dept != null) patient.setDepartment(dept);
        try {
            return ResponseEntity.ok(ApiResponse.ok("Patient created", patientService.create(patient)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Patient patient) {
        String dept = callerDepartment();
        Optional<Patient> existing = patientService.getById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        if (!inScope(existing.get(), dept)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Not authorized to modify this patient"));
        }
        if (dept != null) patient.setDepartment(dept); // can't reassign a patient out of your own ward
        try {
            return ResponseEntity.ok(ApiResponse.ok("Patient updated", patientService.update(id, patient)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String dept = callerDepartment();
        Optional<Patient> existing = patientService.getById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        if (!inScope(existing.get(), dept)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Not authorized to delete this patient"));
        }
        try {
            patientService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Patient deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        String dept = callerDepartment();
        return ResponseEntity.ok(ApiResponse.ok(patientService.getStats(dept)));
    }
}
