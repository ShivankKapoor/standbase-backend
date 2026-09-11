package com.shivankkapoor.standbase.dto.aldrop;

import java.time.OffsetDateTime;

/**
 * Mirrors Aldrop's own response shape: on a non-2FA login {@code token} is set and
 * {@code totpToken} is null; when 2FA is required it's the other way round and
 * {@code totpToken} must be completed at /auth/login/verify-totp.
 */
public record AldropLoginResponseDTO(String token, String totpToken, OffsetDateTime expiresAt) {
}
