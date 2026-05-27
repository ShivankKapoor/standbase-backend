package com.shivankkapoor.standbase.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TotpVerifyRequestDTO {
    @NotBlank
    private String preAuthToken;
    @NotBlank
    private String totpCode;
}
