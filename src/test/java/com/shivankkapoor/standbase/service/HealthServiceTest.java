package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.repository.HealthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HealthServiceTest {

    private HealthRepository healthRepository;
    private HealthService healthService;

    @BeforeEach
    void setUp() {
        healthRepository = mock(HealthRepository.class);
        healthService = new HealthService(healthRepository);
    }

    @Test
    void isDbHealthy_returnsTrue_whenCheckDbSucceeds() {
        assertThat(healthService.isDbHealthy()).isTrue();
    }

    @Test
    void isDbHealthy_returnsFalse_whenCheckDbThrows() {
        doThrow(new RuntimeException("connection refused")).when(healthRepository).checkDb();
        assertThat(healthService.isDbHealthy()).isFalse();
    }
}
