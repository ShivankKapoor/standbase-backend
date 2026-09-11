package com.shivankkapoor.standbase.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Id is Aldrop's own user id, assigned explicitly rather than generated locally — Aldrop owns
 * registration now, and this row is provisioned on first-seen login for that id.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private UUID id;
    private String username;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
