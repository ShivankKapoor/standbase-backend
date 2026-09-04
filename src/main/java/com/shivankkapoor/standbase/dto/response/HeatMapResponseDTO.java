package com.shivankkapoor.standbase.dto.response;

import com.shivankkapoor.standbase.model.EntryLength;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class HeatMapResponseDTO extends ResponseDTO {
    private int average;
    private List<EntryLength> entries;
}

