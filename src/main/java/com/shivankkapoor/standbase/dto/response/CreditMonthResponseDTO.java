package com.shivankkapoor.standbase.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CreditMonthResponseDTO {
    private BigDecimal used;
    private int workedDays;
    /** Credits per worked day; null until a worked day has a total behind it. */
    private BigDecimal average;
    private List<CreditDayResponseDTO> days;
}
