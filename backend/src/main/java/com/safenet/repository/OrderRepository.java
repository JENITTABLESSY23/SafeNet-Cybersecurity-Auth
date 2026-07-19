package com.safenet.repository;

import com.safenet.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByDepartmentAndStatusOrderByRequestedAtDesc(String dept, String status);
    List<Order> findByPatientIdOrderByRequestedAtDesc(Long patientId);
    long countByStatus(String status);
}
