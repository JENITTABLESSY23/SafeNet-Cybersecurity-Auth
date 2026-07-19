package com.safenet.service;

import com.safenet.dto.SupportTicketRequest;
import com.safenet.entity.SupportTicket;
import com.safenet.entity.User;
import com.safenet.repository.SupportTicketRepository;
import com.safenet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupportService {

    @Autowired private SupportTicketRepository ticketRepo;
    @Autowired private UserRepository userRepo;

    private static final java.util.Set<String> VALID_CATEGORIES = java.util.Set.of(
        "Login / Access", "Patient Records", "IoT / Sensors", "RBAC / Permissions",
        "Security Concern", "Other"
    );

    public SupportTicket createTicket(String username, SupportTicketRequest req) throws Exception {
        User u = userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));

        if (req.getCategory() == null || !VALID_CATEGORIES.contains(req.getCategory())) {
            throw new Exception("Select a valid issue category.");
        }
        if (req.getSubject() == null || req.getSubject().trim().length() < 5) {
            throw new Exception("Subject must be at least 5 characters.");
        }
        if (req.getDetails() == null || req.getDetails().trim().length() < 15) {
            throw new Exception("Details must be at least 15 characters.");
        }

        SupportTicket t = new SupportTicket();
        t.setUserId(u.getId());
        t.setCategory(req.getCategory());
        t.setSubject(req.getSubject().trim());
        t.setDetails(req.getDetails().trim());
        return ticketRepo.save(t);
    }

    public List<SupportTicket> myTickets(String username) throws Exception {
        User u = userRepo.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));
        return ticketRepo.findByUserIdOrderByCreatedAtDesc(u.getId());
    }
}
