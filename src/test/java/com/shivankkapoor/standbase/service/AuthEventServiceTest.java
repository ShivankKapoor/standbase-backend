package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.AuthEvent;
import com.shivankkapoor.standbase.model.AuthEventType;
import com.shivankkapoor.standbase.repository.AuthEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthEventServiceTest {

    private AuthEventRepository authEventRepository;
    private AuthEventService authEventService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String IP = "1.2.3.4";
    // Port 1 is unreachable — meridian calls will fail fast without hitting a real server
    private static final String UNREACHABLE_MERIDIAN = "http://localhost:1";

    @BeforeEach
    void setUp() {
        authEventRepository = mock(AuthEventRepository.class);
        authEventService = new AuthEventService(authEventRepository, "PROD", UNREACHABLE_MERIDIAN);
    }

    @Test
    void logAuthEvent_savesCorrectEntityFields() {
        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getIpAddress()).isEqualTo(IP);
        assertThat(saved.getEventType()).isEqualTo(AuthEventType.LOGIN_SUCCESS);
    }

    @Test
    void logAuthEvent_savesUnknownLocation_whenMeridianUnreachable() {
        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertThat(saved.getCountry()).isEqualTo("UNKNOWN");
        assertThat(saved.getCity()).isEqualTo("UNKNOWN");
    }

    @Test
    void logAuthEvent_dbFailure_doesNotPropagate() {
        when(authEventRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        verify(authEventRepository).save(any());
    }

    @Test
    void logAuthEvent_devMode_doesNotSave() {
        AuthEventService devService = new AuthEventService(authEventRepository, "DEV", UNREACHABLE_MERIDIAN);

        devService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        verify(authEventRepository, never()).save(any());
    }
}
