package com.shivankkapoor.standbase.dto.response;

import com.shivankkapoor.standbase.dto.request.ResponseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckResponseDTO extends ResponseDTO {
    String userName;
}
