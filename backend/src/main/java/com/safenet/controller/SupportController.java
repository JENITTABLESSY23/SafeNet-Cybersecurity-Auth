package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.dto.SupportTicketRequest;
import com.safenet.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired private SupportService supportService;

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @PostMapping("/tickets")
    public ResponseEntity<?> create(@RequestBody SupportTicketRequest req) {
        try {
            var ticket = supportService.createTicket(currentUsername(), req);
            return ResponseEntity.ok(ApiResponse.ok("Support ticket submitted", ticket));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/tickets")
    public ResponseEntity<?> mine() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(supportService.myTickets(currentUsername())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
