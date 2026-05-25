package com.shivankkapoor.standbase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResponseDTO {
    @NotBlank
    private String status;
}
