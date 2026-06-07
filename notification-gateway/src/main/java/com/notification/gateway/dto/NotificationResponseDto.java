package com.notification.gateway.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {
    private UUID messageId;
    private String status;
    private String timestamp;
    private String recipient;
    private String type;
}
