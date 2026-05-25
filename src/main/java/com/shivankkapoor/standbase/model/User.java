package com.shivankkapoor.standbase.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String username;
    private String password;
    @Column(name = "totp_secret")
    private String totpSecret;
    @Column(name = "totp_enabled")
    private Boolean totpEnabled;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
