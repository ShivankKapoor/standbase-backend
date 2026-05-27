package com.shivankkapoor.standbase.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class EntryListResponseDTO extends ResponseDTO {
    private List<EntryOverviewResponseDTO> entries;
}
