package com.shivankkapoor.standbase.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public record User(
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        UUID id,
        String username,
        String password,
        @Column(name = "totp_secret")
        String totpSecret,
        @Column(name = "totp_enabled")
        Boolean totpEnabled,
        @Column(name = "created_at")
        OffsetDateTime createdAt
) {}
