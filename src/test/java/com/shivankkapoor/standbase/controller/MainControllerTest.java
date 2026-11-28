package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MainControllerTest {

    private final HealthService healthService = mock(HealthService.class);

    @Test
    void home_returnsWelcomePage() {
        assertThat(new MainController(healthService, "").home().getBody()).contains("Welcome to Standbase-backend");
    }

    @Test
    void monitor_reportsEnvAndUptime() {
        assertThat(new MainController(healthService, " ").monitor().getBody())
                .containsEntry("env", "UNKNOWN")
                .containsEntry("status", "Up");

        MainController old = new MainController(healthService, "prod");
        ReflectionTestUtils.setField(old, "startTime", Instant.now().minus(Duration.ofDays(2)));

        assertThat(old.monitor().getBody()).containsEntry("env", "PROD");
        assertThat(old.monitor().getBody().get("uptime")).startsWith("2d ");
    }

    @Test
    void health_reportsDbState() {
        MainController controller = new MainController(healthService, "");
        when(healthService.isDbHealthy()).thenReturn(true, false);

        assertThat(controller.health().getBody()).containsEntry("db", "Up");
        assertThat(controller.health().getBody()).containsEntry("db", "Down");
    }
}
