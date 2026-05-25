package com.shivankkapoor.standbase.dto.response;

import com.shivankkapoor.standbase.dto.request.ResponseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginResponseDTO extends ResponseDTO {
    String sessionToken;
}
