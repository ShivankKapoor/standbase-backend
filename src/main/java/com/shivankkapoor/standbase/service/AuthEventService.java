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

    public AuthEventService(AuthEventRepository authEventRepository,
                            @Value("${application.env:}") String env) {
        this.authEventRepository = authEventRepository;
        this.dev = "DEV".equalsIgnoreCase(env);
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
        log.info("Getting ip location via DB");
        IpResponseDTO location = getLocationFromDB(ip);
        if (location != null){
            return location;
        };
        log.info("Not found in DB getting via IP API");
        return getLocationViaAPI(ip);
    }

    private IpResponseDTO getLocationFromDB(String ip) {
        return authEventRepository.findMostRecentWithLocationByIp(ip)
                .map(event -> {
                    IpResponseDTO resp = new IpResponseDTO();
                    resp.setCity(event.getCity());
                    resp.setCountry(event.getCountry());
                    return resp;
                })
                .orElse(null);
    }

    private IpResponseDTO getLocationViaAPI(String ip){
        IpResponseDTO resp = new IpResponseDTO();
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/" + ip + "/json/"))
                    .header("User-Agent", "java-ip-client")
                    .GET()
                    .build();
            var responseBody = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            var json = objectMapper.readTree(responseBody);

            String city = json.path("city").textValue();
            String country = json.path("country_name").textValue();

            resp.setCity(city);
            resp.setCountry(country);

        } catch (Exception e) {
            log.error("Error getting IP location for IP {} Error:",ip,e);
        }
        return resp;
    }
}
