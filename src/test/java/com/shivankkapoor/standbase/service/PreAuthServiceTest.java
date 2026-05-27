package com.shivankkapoor.standbase.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PreAuthServiceTest {

    private PreAuthService preAuthService;

    @BeforeEach
    void setUp() {
        preAuthService = new PreAuthService();
    }

    @Test
    void createToken_returnsToken() {
        String token = preAuthService.createToken(UUID.randomUUID());
        assertThat(token).isNotNull().hasSize(64);
    }

    @Test
    void validateAndConsume_validToken_returnsUserId() {
        UUID userId = UUID.randomUUID();
        String token = preAuthService.createToken(userId);
        assertThat(preAuthService.validateAndConsume(token)).isEqualTo(userId);
    }

    @Test
    void validateAndConsume_unknownToken_returnsNull() {
        assertThat(preAuthService.validateAndConsume("bogus")).isNull();
    }

    @Test
    void validateAndConsume_isSingleUse() {
        UUID userId = UUID.randomUUID();
        String token = preAuthService.createToken(userId);
        preAuthService.validateAndConsume(token);
        assertThat(preAuthService.validateAndConsume(token)).isNull();
    }
}
