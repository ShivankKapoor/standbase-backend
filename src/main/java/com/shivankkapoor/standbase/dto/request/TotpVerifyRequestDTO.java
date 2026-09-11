package com.shivankkapoor.standbase.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TotpVerifyRequestDTO {
    @NotBlank
    @Size(max = 128)
    private String preAuthToken;
    @NotBlank
    @Size(max = 16)
    private String totpCode;
}
