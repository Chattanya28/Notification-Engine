package com.notification.gateway.service.channel;

import com.notification.gateway.model.Notification;
import com.notification.gateway.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamsChannel implements NotificationChannel {

    private final RestTemplate restTemplate;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.TEAMS;
    }

    @Override
    public void send(Notification notification) throws Exception {
        String webhookUrl = notification.getRecipient();
        log.info("Sending MS Teams message to webhook: {}", webhookUrl);

        if (webhookUrl == null || !webhookUrl.startsWith("http")) {
            throw new IllegalArgumentException("Invalid MS Teams Webhook URL: " + webhookUrl);
        }

        // Teams message payload format
        Map<String, String> payload = Map.of(
                "title", notification.getSubject() != null ? notification.getSubject() : "Notification Platform Alert",
                "text", notification.getBody()
        );

        try {
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("Teams webhook call completed successfully.");
        } catch (Exception e) {
            log.error("Failed to post message to MS Teams webhook: {}", e.getMessage());
            throw e;
        }
    }
}
