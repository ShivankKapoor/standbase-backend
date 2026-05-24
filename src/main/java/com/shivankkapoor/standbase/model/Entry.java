package com.shivankkapoor.standbase.model;

import jakarta.persistence.Column;
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
    @Column(name = "user_id")
    UUID userId,
    @Column(name = "entry_date")
    LocalDate entryDate,
    @Column(name = "day_type")
    String dayType,
    String content,
    @Column(name = "created_at")
    OffsetDateTime createdAt,
    @Column(name = "updated_at")
    OffsetDateTime updatedAt
) {}
