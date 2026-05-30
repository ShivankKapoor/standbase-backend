package com.shivankkapoor.standbase.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "auth_events")
public class AuthEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuthEventType eventType;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "country", nullable = true, updatable = true)
    private String country;
    @Column(name = "city", nullable = true, updatable = true)
    private String city;
}
