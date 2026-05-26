package com.shivankkapoor.standbase.dto.response;

import com.shivankkapoor.standbase.model.DayType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = false)
public class CreateEntryResponseDTO extends ResponseDTO {
    private LocalDate date;
    private DayType dayType;
    private String content;
}
