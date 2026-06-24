package com.shivankkapoor.standbase.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TodoResponseDTO {
    private UUID id;
    private LocalDate entryDate;
    private String content;
    private boolean completed;
    private int position;
}
