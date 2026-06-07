package com.notification.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookRequestDto {

    @NotBlank(message = "Webhook destination URL is required")
    private String url;

    @NotEmpty(message = "At least one event type is required (e.g. SENT, DELIVERED, FAILED)")
    private Set<String> events;
}
