package com.safenet.repository;

import com.safenet.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPatientId(String patientId);
    List<Patient> findByDepartment(String department);
    List<Patient> findByStatus(String status);
    long countByDepartment(String department);
    long countByStatus(String status);

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(CONCAT(p.firstName,' ',p.lastName)) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(p.patientId) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(p.diagnosis) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Patient> searchPatients(@Param("q") String q);
}
