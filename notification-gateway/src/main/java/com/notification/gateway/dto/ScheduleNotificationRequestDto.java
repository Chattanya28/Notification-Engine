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
public class ScheduleNotificationRequestDto {

    @NotBlank(message = "Recipient 'to' field is required")
    private String to;

    private String subject;

    private String bodyTemplate;

    private String templateName;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Cron expression is required")
    private String cronExpression;

    private Map<String, Object> variables; // Optional variables
}
