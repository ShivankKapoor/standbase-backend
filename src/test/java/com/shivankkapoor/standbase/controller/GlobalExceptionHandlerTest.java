package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.service.InvalidCreditDayException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidCreditDay_returns400WithMessage() {
        var response = handler.handleInvalidCreditDay(new InvalidCreditDayException("too low"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "too low");
    }

    @Test
    void dataIntegrityViolation_returns409() {
        var response = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("dup"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void unexpectedError_returns500WithoutDetails() {
        var response = handler.handleUnexpected(new RuntimeException("secret detail"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "An unexpected error occurred");
    }
}
