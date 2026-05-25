package com.shivankkapoor.standbase.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginResponseDTO extends ResponseDTO {
    String sessionToken;
}
