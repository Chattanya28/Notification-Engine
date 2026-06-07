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
public class SlackChannel implements NotificationChannel {

    private final RestTemplate restTemplate;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SLACK;
    }

    @Override
    public void send(Notification notification) throws Exception {
        String webhookUrl = notification.getRecipient();
        log.info("Sending Slack message to webhook: {}", webhookUrl);

        if (webhookUrl == null || !webhookUrl.startsWith("http")) {
            throw new IllegalArgumentException("Invalid Slack Webhook URL: " + webhookUrl);
        }

        // Create standard Slack webhook text payload
        Map<String, String> payload = Map.of("text", notification.getBody());

        try {
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("Slack webhook call completed successfully.");
        } catch (Exception e) {
            log.error("Failed to post message to Slack webhook: {}", e.getMessage());
            throw e;
        }
    }
}
