package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.entity.Order;
import com.safenet.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderService orderService;

    @GetMapping("/department/{dept}")
    public ResponseEntity<?> byDept(@PathVariable String dept) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getByDepartment(dept)));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getByPatient(patientId)));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Order order) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Order created", orderService.create(order)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long confirmedBy = body.get("confirmedBy") != null
                ? Long.parseLong(body.get("confirmedBy").toString()) : null;
            return ResponseEntity.ok(ApiResponse.ok("Order confirmed", orderService.confirm(id, confirmedBy)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Order cancelled", orderService.cancel(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getStats()));
    }
}
