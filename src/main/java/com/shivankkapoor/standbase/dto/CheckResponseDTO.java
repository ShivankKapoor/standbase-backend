package com.shivankkapoor.standbase.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckResponseDTO extends ResponseDTO {
    String userName;
}
