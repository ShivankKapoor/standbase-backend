package com.shivankkapoor.standbase.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CreditDayResponseDTO {
    private LocalDate date;
    private BigDecimal monthTotal;
    private boolean didNotWork;
    /** Credits used since the previous logged total; null when no total was logged. */
    private BigDecimal used;
}
