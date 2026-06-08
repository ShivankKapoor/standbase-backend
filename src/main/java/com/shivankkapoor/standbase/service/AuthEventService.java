package com.shivankkapoor.standbase.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivankkapoor.standbase.dto.response.IpResponseDTO;
import com.shivankkapoor.standbase.model.AuthEvent;
import com.shivankkapoor.standbase.model.AuthEventType;
import com.shivankkapoor.standbase.repository.AuthEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
public class AuthEventService {
    private static final Logger log = LoggerFactory.getLogger(AuthEventService.class);
    private final AuthEventRepository authEventRepository;
    private final boolean dev;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String meridianBaseUrl;

    public AuthEventService(AuthEventRepository authEventRepository,
                            @Value("${application.env:}") String env,
                            @Value("${meridian.base-url}") String meridianBaseUrl) {
        this.authEventRepository = authEventRepository;
        this.dev = "DEV".equalsIgnoreCase(env);
        this.meridianBaseUrl = meridianBaseUrl;
    }

    @Async
    public void logAuthEvent(UUID userId, String ip, AuthEventType eventType) {
        if (dev) {
            log.warn("[DEV] Skipping auth event storage: {} for user {} from {}", eventType, userId, ip);
            return;
        }
        AuthEvent authEvent = new AuthEvent();
        authEvent.setUserId(userId);
        authEvent.setIpAddress(ip);
        authEvent.setEventType(eventType);
        IpResponseDTO location = getLocation(ip);
        authEvent.setCity(location.getCity());
        authEvent.setCountry(location.getCountry());
        log.info("Logging auth event {} for user {} from {}", eventType, userId, ip);
        try {
            authEventRepository.save(authEvent);
        } catch (Exception e) {
            log.error("Unable to save auth event", e);
        }
    }

    private IpResponseDTO getLocation(String ip) {
        IpResponseDTO resp = new IpResponseDTO();
        resp.setCity("UNKNOWN");
        resp.setCountry("UNKNOWN");
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(meridianBaseUrl + "/location/" + ip))
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Meridian returned status {} for IP {}", response.statusCode(), ip);
                return resp;
            }
            var json = objectMapper.readTree(response.body());
            resp.setCity(json.path("city").textValue());
            resp.setCountry(json.path("country").textValue());
        } catch (Exception e) {
            log.error("Error getting IP location for IP {} from meridian:", ip, e);
        }
        return resp;
    }
}
