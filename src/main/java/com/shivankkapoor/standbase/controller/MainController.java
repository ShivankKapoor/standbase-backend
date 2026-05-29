package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.service.HealthService;
import com.shivankkapoor.standbase.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private final HealthService healthService;
    private final SessionService sessionService;
    private final String env;
    private final Instant startTime = Instant.now();

    public MainController(HealthService healthService,
                          SessionService sessionService,
                          @Value("${application.env:}") String env) {
        this.healthService = healthService;
        this.sessionService = sessionService;
        this.env = env.isBlank() ? "UNKNOWN" : env.toUpperCase();
    }

    @GetMapping("/")
    ResponseEntity<String> home(){
        log.info("Home endpoint has been called");
        final String homeString = "<html><body><h2>Welcome to Standbase-backend</h2></body></html>";
        return ResponseEntity.ok(homeString);
    }

    @GetMapping("/monitor")
    ResponseEntity<Map<String, String>> monitor() {
        log.info("Monitor endpoint has been called");
        Map<String, String> resp = new LinkedHashMap<>();
        Duration uptime = Duration.between(startTime, Instant.now());
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();
        String uptimeStr = (days > 0 ? days + "d " : "") + hours + "h " + minutes + "m " + seconds + "s";

        resp.put("name", "Standbase-Backend");
        resp.put("status", "Up");
        resp.put("env", env);
        resp.put("uptime", uptimeStr);
        resp.put("platform", "Java");
        resp.put("version", System.getProperty("java.version"));
        resp.put("vendor", System.getProperty("java.vendor"));

        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/admin/session-cleanup")
    ResponseEntity<Map<String, String>> sessionCleanup() {
        sessionService.evictExpiredSessions();
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("status", "ok");
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
