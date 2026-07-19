package com.safenet.repository;

import com.safenet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByHospitalId(String hospitalId);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByHospitalId(String hospitalId);
    List<User> findByApprovalStatus(String status);
    List<User> findByDepartment(String department);

    @Query("SELECT u FROM User u WHERE u.approvalStatus = 'PENDING' ORDER BY u.createdAt DESC")
    List<User> findAllPendingApprovals();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
}
