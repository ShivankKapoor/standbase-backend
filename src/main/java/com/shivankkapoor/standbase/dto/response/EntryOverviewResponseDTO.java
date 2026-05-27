package com.shivankkapoor.standbase.dto.response;

import com.shivankkapoor.standbase.model.DayType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EntryOverviewResponseDTO {
    private LocalDate date;
    private DayType dayType;
}
