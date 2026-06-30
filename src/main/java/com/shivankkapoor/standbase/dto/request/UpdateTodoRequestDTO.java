package com.shivankkapoor.standbase.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateTodoRequestDTO {
    @Size(max = 500)
    private String content;
    private Boolean completed;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;
}
