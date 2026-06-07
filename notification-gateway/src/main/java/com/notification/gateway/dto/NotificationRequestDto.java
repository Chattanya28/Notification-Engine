package com.notification.gateway.dto;

import com.notification.gateway.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {

    @NotBlank(message = "Recipient 'to' field is required")
    private String to;

    private String subject;

    private String body; // Optional if template is used

    @NotNull(message = "Notification type is required (EMAIL, SMS, PUSH, SLACK, TEAMS)")
    private NotificationType type;

    private String templateName; // Optional

    private Map<String, Object> variables; // Optional variables for template
}
