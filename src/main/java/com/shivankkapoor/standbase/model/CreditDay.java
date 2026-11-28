package com.shivankkapoor.standbase.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "credit_days")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditDay {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "entry_date")
    private LocalDate entryDate;
    @Column(name = "month_total", precision = 10, scale = 2)
    private BigDecimal monthTotal;
    @Column(name = "did_not_work")
    private boolean didNotWork;
}
