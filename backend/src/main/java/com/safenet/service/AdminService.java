package com.safenet.service;

import com.safenet.entity.AuditLog;
import com.safenet.entity.IotAnomaly;
import com.safenet.entity.User;
import com.safenet.repository.AuditLogRepository;
import com.safenet.repository.IotAnomalyRepository;
import com.safenet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    @Autowired private UserRepository       userRepo;
    @Autowired private AuditLogRepository   auditRepo;
    @Autowired private IotAnomalyRepository anomalyRepo;

    public List<User> getPendingApprovals() { return userRepo.findAllPendingApprovals(); }

    public void approveUser(Long userId, String role, Long adminId) throws Exception {
        User u = userRepo.findById(userId).orElseThrow(() -> new Exception("User not found"));
        u.setApprovalStatus("APPROVED"); u.setRole(role); u.setActive(true);
        userRepo.save(u);
        log(adminId, "APPROVE", "USER", String.valueOf(userId), "Approved: " + u.getEmail());
    }

    public void rejectUser(Long userId, Long adminId) throws Exception {
        User u = userRepo.findById(userId).orElseThrow(() -> new Exception("User not found"));
        u.setApprovalStatus("REJECTED"); u.setActive(false);
        userRepo.save(u);
        log(adminId, "REJECT", "USER", String.valueOf(userId), "Rejected: " + u.getEmail());
    }

    public List<User> getAllActiveStaff() {
        return userRepo.findAll().stream().filter(User::isActive).toList();
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("activeUsers",    userRepo.countActiveUsers());
        s.put("pendingCount",   userRepo.findAllPendingApprovals().size());
        s.put("criticalAlerts", anomalyRepo.countBySeverityAndResolved("CRITICAL", false));
        s.put("warningAlerts",  anomalyRepo.countBySeverityAndResolved("WARNING",  false));
        return s;
    }

    public List<AuditLog> getAuditLog() { return auditRepo.findAllOrderByCreatedAtDesc(); }

    public void logBreakTheGlass(Long userId, String reason, String nodeId) {
        log(userId, "BTG", "SYSTEM", null, "Break-the-Glass. Reason: " + reason + ". Node: " + nodeId);
    }

    public List<IotAnomaly> getActiveAnomalies() {
        return anomalyRepo.findByResolvedOrderByDetectedAtDesc(false);
    }

    public void resolveAnomaly(Long anomalyId, Long adminId) throws Exception {
        IotAnomaly a = anomalyRepo.findById(anomalyId).orElseThrow(() -> new Exception("Anomaly not found"));
        a.setResolved(true); a.setResolvedBy(adminId); a.setResolvedAt(LocalDateTime.now());
        anomalyRepo.save(a);
        log(adminId, "RESOLVE", "IOT_ANOMALY", String.valueOf(anomalyId), "Resolved: " + a.getNodeId());
    }

    private void log(Long uid, String action, String rt, String rid, String details) {
        AuditLog l = new AuditLog();
        l.setUserId(uid); l.setAction(action); l.setResourceType(rt);
        l.setResourceId(rid); l.setDetails(details);
        auditRepo.save(l);
    }
}
