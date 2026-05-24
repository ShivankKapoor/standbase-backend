package com.shivankkapoor.standbase.controller;

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
}
