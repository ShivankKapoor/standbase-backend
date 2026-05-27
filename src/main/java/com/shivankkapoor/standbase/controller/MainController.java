package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.service.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private final HealthService healthService;

    public MainController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/")
    ResponseEntity<Map<String, String>> home() {
        log.info("Home endpoint has been called");
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("name", "Standbase-Backend");
        resp.put("status", "Up");
        resp.put("platform", "Java");
        resp.put("version", System.getProperty("java.version"));
        resp.put("vendor", System.getProperty("java.vendor"));

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, String>> health() {
        boolean dbHealthy = healthService.isDbHealthy();
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("app", "Up");
        resp.put("db", dbHealthy ? "Up" : "Down");
        return ResponseEntity.ok(resp);
    }
}
