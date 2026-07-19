package com.safenet.repository;

import com.safenet.entity.IotAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IotAnomalyRepository extends JpaRepository<IotAnomaly, Long> {
    List<IotAnomaly> findByResolvedOrderByDetectedAtDesc(boolean resolved);
    List<IotAnomaly> findBySeverityOrderByDetectedAtDesc(String severity);
    long countBySeverityAndResolved(String severity, boolean resolved);
}
