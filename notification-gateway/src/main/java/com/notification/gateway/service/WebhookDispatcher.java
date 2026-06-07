package com.notification.gateway.service;

import com.notification.gateway.event.NotificationStatusChangedEvent;
import com.notification.gateway.model.WebhookSubscription;
import com.notification.gateway.repository.WebhookSubscriptionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcher {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate;

    /**
     * Listens asynchronously to notification status changes and matches active webhooks.
     */
    @Async
    @EventListener
    public void handleNotificationStatusChanged(NotificationStatusChangedEvent event) {
        String statusName = event.getStatus().name(); // SENT, DELIVERED, FAILED, etc.
        log.debug("Webhook dispatcher processing event: {} for notification ID {}", statusName, event.getNotificationId());

        List<WebhookSubscription> activeSubscriptions = subscriptionRepository.findByActiveTrue();
        if (activeSubscriptions.isEmpty()) {
            return;
        }

        // Build standard webhook event payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", java.util.UUID.randomUUID().toString());
        payload.put("notificationId", event.getNotificationId().toString());
        payload.put("recipient", event.getRecipient());
        payload.put("type", event.getType().name());
        payload.put("event", statusName);
        payload.put("errorMessage", event.getErrorMessage());
        payload.put("timestamp", event.getTimestamp().toString());

        for (WebhookSubscription subscription : activeSubscriptions) {
            // Check if subscribed to this status event
            if (subscription.getEvents().contains(statusName)) {
                try {
                    // Call delivery method with circuit breaker and retry support
                    dispatchWebhookWithResilience(subscription.getUrl(), payload);
                } catch (Exception e) {
                    log.error("Final delivery failure for webhook to URL {}: {}", subscription.getUrl(), e.getMessage());
                }
            }
        }
    }

    /**
     * Dispatches the webhook payload.
     * Annotated with Resilience4j Circuit Breaker and Spring Retry.
     */
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 4, // 1 initial + 3 retries
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    @CircuitBreaker(name = "webhookCircuitBreaker", fallbackMethod = "webhookFallback")
    public void dispatchWebhookWithResilience(String url, Map<String, Object> payload) {
        log.info("Attempting webhook dispatch to URL: {} (Retry/CB enabled)", url);
        restTemplate.postForEntity(url, payload, String.class);
        log.info("Webhook successfully dispatched to URL: {}", url);
    }

    /**
     * Resilience4j Fallback method if Circuit Breaker opens or maximum retries fail.
     */
    public void webhookFallback(String url, Map<String, Object> payload, Throwable t) {
        log.warn("Resilience4j Webhook Circuit Breaker OPEN or failed. Fallback triggered for URL: {}. Error: {}", url, t.getMessage());
        // In a real application, you could save this to a DLQ (Dead Letter Queue) or Retry Log database table.
    }

    /**
     * Spring Retry Recover method when retries are exhausted.
     */
    @Recover
    public void recover(Exception e, String url, Map<String, Object> payload) {
        log.error("Spring Retry exhausted all attempts for Webhook URL: {}. Error: {}", url, e.getMessage());
    }
}
