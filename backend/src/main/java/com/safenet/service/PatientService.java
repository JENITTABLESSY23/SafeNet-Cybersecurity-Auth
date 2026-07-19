package com.safenet.service;

import com.safenet.entity.Patient;
import com.safenet.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PatientService {

    @Autowired private PatientRepository patientRepo;

    public List<Patient> getAll()                     { return patientRepo.findAll(); }
    public Optional<Patient> getById(Long id)         { return patientRepo.findById(id); }
    public List<Patient> search(String q)             { return patientRepo.searchPatients(q); }
    public List<Patient> getByDepartment(String dept) { return patientRepo.findByDepartment(dept); }
    public List<Patient> getByStatus(String s)        { return patientRepo.findByStatus(s); }

    public Patient create(Patient p) {
        p.setPatientId(generateId());
        if (p.getStatus() == null || p.getStatus().isBlank()) p.setStatus("Stable");
        return patientRepo.save(p);
    }

    public Patient update(Long id, Patient u) throws Exception {
        Patient e = patientRepo.findById(id).orElseThrow(() -> new Exception("Patient not found: " + id));
        e.setFirstName(u.getFirstName()); e.setLastName(u.getLastName());
        e.setAge(u.getAge()); e.setGender(u.getGender());
        e.setDepartment(u.getDepartment()); e.setBed(u.getBed());
        e.setDiagnosis(u.getDiagnosis()); e.setStatus(u.getStatus());
        e.setContact(u.getContact()); e.setNotes(u.getNotes());
        return patientRepo.save(e);
    }

    public void delete(Long id) throws Exception {
        if (!patientRepo.existsById(id)) throw new Exception("Patient not found: " + id);
        patientRepo.deleteById(id);
    }

    public Map<String, Object> getStats() {
        return getStats(null);
    }

    /** Pass null for an unrestricted (Admin_Ops) view; any other value scopes every count to that department. */
    public Map<String, Object> getStats(String department) {
        Map<String, Object> s = new LinkedHashMap<>();
        if (department == null) {
            s.put("total",    patientRepo.count());
            s.put("critical", patientRepo.countByStatus("Critical"));
            s.put("stable",   patientRepo.countByStatus("Stable"));
            s.put("watch",    patientRepo.countByStatus("Watch"));
            s.put("icu",      patientRepo.countByDepartment("ICU"));
            s.put("cardio",   patientRepo.countByDepartment("Cardiology"));
            s.put("gynecology", patientRepo.countByDepartment("Gynecology"));
        } else {
            List<Patient> deptPatients = patientRepo.findByDepartment(department);
            s.put("total",    (long) deptPatients.size());
            s.put("critical", deptPatients.stream().filter(p -> "Critical".equalsIgnoreCase(p.getStatus())).count());
            s.put("stable",   deptPatients.stream().filter(p -> "Stable".equalsIgnoreCase(p.getStatus())).count());
            s.put("watch",    deptPatients.stream().filter(p -> "Watch".equalsIgnoreCase(p.getStatus())).count());
        }
        return s;
    }

    private String generateId() {
        return String.format("SN-%04d", patientRepo.count() + 1);
    }
}
