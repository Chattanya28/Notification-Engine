package com.notification.gateway.service.channel;

import com.notification.gateway.model.Notification;
import com.notification.gateway.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushChannel implements NotificationChannel {

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.PUSH;
    }

    @Override
    public void send(Notification notification) throws Exception {
        log.info("Dispatching PUSH to {}", notification.getRecipient());
        
        System.out.println("\n--- [MOCK PUSH SENT] ---");
        System.out.println("Device Token: " + notification.getRecipient());
        System.out.println("Title:        " + notification.getSubject());
        System.out.println("Body:         " + notification.getBody());
        System.out.println("------------------------\n");
    }
}
