package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.AuthEvent;
import com.shivankkapoor.standbase.model.AuthEventType;
import com.shivankkapoor.standbase.repository.AuthEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
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

    private AuthEvent cachedEventWithLocation(String country, String city) {
        AuthEvent event = new AuthEvent();
        event.setCountry(country);
        event.setCity(city);
        return event;
    }

    @Test
    void logAuthEvent_savesCorrectEntity() {
        when(authEventRepository.findMostRecentWithLocationByIp(IP))
                .thenReturn(Optional.of(cachedEventWithLocation("United States", "New York")));

        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getIpAddress()).isEqualTo(IP);
        assertThat(saved.getEventType()).isEqualTo(AuthEventType.LOGIN_SUCCESS);
        assertThat(saved.getCountry()).isEqualTo("United States");
        assertThat(saved.getCity()).isEqualTo("New York");
    }

    @Test
    void logAuthEvent_usesLocationFromDB_whenAvailable() {
        when(authEventRepository.findMostRecentWithLocationByIp(IP))
                .thenReturn(Optional.of(cachedEventWithLocation("Germany", "Berlin")));

        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertThat(saved.getCountry()).isEqualTo("Germany");
        assertThat(saved.getCity()).isEqualTo("Berlin");
    }

    @Test
    void logAuthEvent_savesNullLocation_whenDBEmptyAndAPIUnreachable() {
        when(authEventRepository.findMostRecentWithLocationByIp(IP))
                .thenReturn(Optional.empty());

        // IP "1.2.3.4" will hit the real API — use an invalid IP to force failure
        authEventService.logAuthEvent(USER_ID, "999.999.999.999", AuthEventType.LOGIN_SUCCESS);

        ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository).save(captor.capture());

        AuthEvent saved = captor.getValue();
        assertThat(saved.getCountry()).isNull();
        assertThat(saved.getCity()).isNull();
    }

    @Test
    void logAuthEvent_dbFailure_doesNotPropagate() {
        when(authEventRepository.findMostRecentWithLocationByIp(IP))
                .thenReturn(Optional.of(cachedEventWithLocation("United States", "New York")));
        when(authEventRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        authEventService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        verify(authEventRepository).save(any());
    }

    @Test
    void logAuthEvent_devMode_doesNotSave() {
        AuthEventService devService = new AuthEventService(authEventRepository, "DEV");

        devService.logAuthEvent(USER_ID, IP, AuthEventType.LOGIN_SUCCESS);

        verify(authEventRepository, never()).save(any());
    }
}
