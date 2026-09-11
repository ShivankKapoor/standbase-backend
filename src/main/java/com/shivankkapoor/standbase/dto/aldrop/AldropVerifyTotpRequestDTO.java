package com.shivankkapoor.standbase.dto.aldrop;

public record AldropVerifyTotpRequestDTO(String totpToken, String code, String ipAddress, String userAgent) {
}
