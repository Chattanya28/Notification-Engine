package com.notification.gateway.service.channel;

import com.notification.gateway.model.Notification;
import com.notification.gateway.model.NotificationType;

public interface NotificationChannel {
    void send(Notification notification) throws Exception;
    boolean supports(NotificationType type);
}
