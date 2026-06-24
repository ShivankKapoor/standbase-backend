package com.shivankkapoor.standbase.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TodoSummaryResponseDTO {
    private LocalDate date;
    private boolean allCompleted;
}
