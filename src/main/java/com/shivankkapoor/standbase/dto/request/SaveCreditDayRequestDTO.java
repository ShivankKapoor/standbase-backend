package com.shivankkapoor.standbase.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveCreditDayRequestDTO {
    @DecimalMin("0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal monthTotal;
    private boolean didNotWork;
}
