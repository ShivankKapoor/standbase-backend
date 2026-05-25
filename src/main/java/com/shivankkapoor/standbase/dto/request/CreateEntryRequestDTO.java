package com.shivankkapoor.standbase.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shivankkapoor.standbase.model.DayType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateEntryRequestDTO {
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private DayType dayType;
    private String content;
}
