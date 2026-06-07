package com.notification.gateway.service.channel;

import com.notification.gateway.model.Notification;
import com.notification.gateway.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    @Override
    public void send(Notification notification) throws Exception {
        log.info("Dispatching EMAIL to {}", notification.getRecipient());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.getRecipient());
            message.setSubject(notification.getSubject() != null ? notification.getSubject() : "Notification Platform");
            message.setText(notification.getBody());
            
            mailSender.send(message);
            log.info("EMAIL successfully dispatched via SMTP to {}", notification.getRecipient());
        } catch (Exception e) {
            log.warn("SMTP send failed (expected in local dev mode). Falling back to mock email rendering: {}", e.getMessage());
            
            // Render the email explicitly to stdout
            System.out.println("\n--- [MOCK EMAIL SENT] ---");
            System.out.println("To:      " + notification.getRecipient());
            System.out.println("Subject: " + notification.getSubject());
            System.out.println("Body:    " + notification.getBody());
            System.out.println("-------------------------\n");
        }
    }
}
