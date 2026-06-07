package com.notification.gateway.dto;

import com.notification.gateway.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateRequestDto {

    @NotBlank(message = "Template name is required")
    private String name;

    private String subject; // Optional (e.g. for EMAIL)

    @NotBlank(message = "Template content is required")
    private String content;

    @NotNull(message = "Channel is required (EMAIL, SMS, PUSH, SLACK, TEAMS)")
    private NotificationType channel;
}
