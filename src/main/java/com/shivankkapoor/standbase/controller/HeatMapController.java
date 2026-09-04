package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.response.HeatMapResponseDTO;
import com.shivankkapoor.standbase.service.HeatMapService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/heatmap")
public class HeatMapController {

    private final HeatMapService heatMapService;

    public HeatMapController(HeatMapService heatMapService) {
        this.heatMapService = heatMapService;
    }

    @GetMapping("")
    public ResponseEntity<HeatMapResponseDTO> getHeatMap(
            @RequestParam LocalDate today,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        HeatMapResponseDTO response = heatMapService.getHeatMap(userId, today);
        response.setStatus("ok");
        return ResponseEntity.ok(response);
    }
}
