package com.notification.gateway.dto;

import com.notification.gateway.model.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyRequestDto {

    @NotBlank(message = "API Key name is required")
    private String name;

    @NotEmpty(message = "At least one permission is required")
    private Set<Permission> permissions;
}
