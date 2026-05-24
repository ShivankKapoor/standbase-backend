package com.shivankkapoor.standbase.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "entries")
public record Entry(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id,
    UUID userId,
    LocalDate entryDate,
    String dayType,
    String content,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
