package com.shivankkapoor.standbase.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderTodosRequestDTO {
    @NotNull
    private List<UUID> ids;
}
