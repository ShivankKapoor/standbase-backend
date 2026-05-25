package com.shivankkapoor.standbase.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResponseDTO {
    @NotBlank
    private String status;
}
