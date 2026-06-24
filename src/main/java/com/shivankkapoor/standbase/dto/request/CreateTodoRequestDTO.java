package com.shivankkapoor.standbase.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTodoRequestDTO {
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;
    @NotBlank
    @Size(max = 500)
    private String content;
}
