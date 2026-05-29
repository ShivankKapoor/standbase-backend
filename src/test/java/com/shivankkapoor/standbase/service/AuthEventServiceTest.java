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

    @BeforeEach
    void setUp() {
        authEventRepository = mock(AuthEventRepository.class);
        authEventService = new AuthEventService(authEventRepository, "PROD");
    }

    @Test
    void logAuthEvent_savesCorrectEntity() {
        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getIpAddress()).isEqualTo(IP);
        assertThat(saved.getEventType()).isEqualTo(AuthEventType.LOGIN_SUCCESS);
    }

    @Test
    void logAuthEvent_dbFailure_doesNotPropagate() {
        when(authEventRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        verify(authEventRepository).save(any());
    }
}
