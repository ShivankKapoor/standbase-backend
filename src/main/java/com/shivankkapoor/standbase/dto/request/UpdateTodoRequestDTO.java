package com.shivankkapoor.standbase.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTodoRequestDTO {
    @Size(max = 500)
    private String content;
    private Boolean completed;
}
