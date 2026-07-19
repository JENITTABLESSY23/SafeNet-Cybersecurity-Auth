package com.safenet.controller;

import com.safenet.dto.ApiResponse;
import com.safenet.service.IoTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/iot")
public class IoTController {

    @Autowired private IoTService iotService;

    @GetMapping("/vitals/{bedId}")
    public ResponseEntity<?> vitals(@PathVariable String bedId) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.getVitals(bedId)));
    }

    @PostMapping("/detect")
    public ResponseEntity<?> detect(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.runDetection(body)));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<?> anomalies(@RequestParam(defaultValue = "false") boolean resolved) {
        return ResponseEntity.ok(ApiResponse.ok(iotService.getAnomalies(resolved)));
    }

    @GetMapping("/anomalies/stats")
    public ResponseEntity<?> anomalyStats() {
        return ResponseEntity.ok(ApiResponse.ok(iotService.getAnomalyStats()));
    }
}
