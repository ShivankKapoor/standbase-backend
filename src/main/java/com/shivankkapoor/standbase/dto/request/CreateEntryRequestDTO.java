package com.shivankkapoor.standbase.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shivankkapoor.standbase.model.DayType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateEntryRequestDTO {
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private DayType dayType;
    @Size(max = 2000)
    private String content;
}
