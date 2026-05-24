package com.shivankkapoor.standbase.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MainController {
    @GetMapping("/")
    ResponseEntity<Map<String, String>> home(){
        Map<String, String> resp = new LinkedHashMap<>();
           resp.put("name", "Standbase-Backend");
            resp.put("status", "Up");
            resp.put("platform", "Java");
            resp.put("version",System.getProperty("java.version"));
            resp.put("vendor",System.getProperty("java.vendor"));
        
        return ResponseEntity.ok(resp);
    }
}
