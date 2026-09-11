package com.shivankkapoor.standbase.dto.aldrop;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AldropValidateSessionResponseDTO(UUID userId, String username, OffsetDateTime expiresAt) {
}
