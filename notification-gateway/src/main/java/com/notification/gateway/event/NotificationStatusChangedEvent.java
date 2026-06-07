package com.notification.gateway.event;

import com.notification.gateway.model.NotificationStatus;
import com.notification.gateway.model.NotificationType;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class NotificationStatusChangedEvent {
    private final UUID notificationId;
    private final String recipient;
    private final NotificationType type;
    private final NotificationStatus status;
    private final String errorMessage;
    private final LocalDateTime timestamp;

    public NotificationStatusChangedEvent(UUID notificationId, String recipient, NotificationType type, NotificationStatus status, String errorMessage) {
        this.notificationId = notificationId;
        this.recipient = recipient;
        this.type = type;
        this.status = status;
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now();
    }
}
