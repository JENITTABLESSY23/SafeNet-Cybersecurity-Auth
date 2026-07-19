package com.safenet.service;

import com.safenet.entity.Order;
import com.safenet.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepo;

    public List<Order> getByDepartment(String dept) {
        return orderRepo.findByDepartmentAndStatusOrderByRequestedAtDesc(dept, "PENDING");
    }

    public List<Order> getByPatient(Long patientId) {
        return orderRepo.findByPatientIdOrderByRequestedAtDesc(patientId);
    }

    public Order create(Order order) {
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setStatus("PENDING");
        return orderRepo.save(order);
    }

    public Order confirm(Long orderId, Long confirmedBy) throws Exception {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new Exception("Order not found"));
        o.setStatus("CONFIRMED"); o.setConfirmedBy(confirmedBy);
        o.setConfirmedAt(LocalDateTime.now());
        return orderRepo.save(o);
    }

    public Order cancel(Long orderId) throws Exception {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> new Exception("Order not found"));
        o.setStatus("CANCELLED");
        return orderRepo.save(o);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("pending",   orderRepo.countByStatus("PENDING"));
        s.put("confirmed", orderRepo.countByStatus("CONFIRMED"));
        s.put("completed", orderRepo.countByStatus("COMPLETED"));
        return s;
    }
}
