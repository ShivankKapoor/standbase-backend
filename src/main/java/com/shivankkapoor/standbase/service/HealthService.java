package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.repository.HealthRepository;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
    private final HealthRepository healthRepository;

    public HealthService(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
    }

    public boolean isDbHealthy() {
        try {
            healthRepository.checkDb();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
