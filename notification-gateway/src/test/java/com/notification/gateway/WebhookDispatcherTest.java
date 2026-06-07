package com.notification.gateway;

import com.notification.gateway.event.NotificationStatusChangedEvent;
import com.notification.gateway.model.NotificationStatus;
import com.notification.gateway.model.NotificationType;
import com.notification.gateway.model.WebhookSubscription;
import com.notification.gateway.repository.WebhookSubscriptionRepository;
import com.notification.gateway.service.WebhookDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class WebhookDispatcherTest {

    private WebhookDispatcher webhookDispatcher;

    @Mock
    private WebhookSubscriptionRepository subscriptionRepository;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        webhookDispatcher = new WebhookDispatcher(subscriptionRepository, restTemplate);
    }

    @Test
    public void testHandleNotificationStatusChanged_MatchesSubscriptions() {
        UUID notificationId = UUID.randomUUID();
        NotificationStatusChangedEvent event = new NotificationStatusChangedEvent(
                notificationId,
                "user@example.com",
                NotificationType.EMAIL,
                NotificationStatus.DELIVERED,
                null
        );

        WebhookSubscription sub1 = WebhookSubscription.builder()
                .id(1L)
                .url("https://example.com/webhook1")
                .events(Set.of("DELIVERED", "FAILED"))
                .active(true)
                .build();

        WebhookSubscription sub2 = WebhookSubscription.builder()
                .id(2L)
                .url("https://example.com/webhook2")
                .events(Set.of("FAILED")) // Not subscribed to DELIVERED
                .active(true)
                .build();

        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of(sub1, sub2));

        webhookDispatcher.handleNotificationStatusChanged(event);

        // Verify dispatch only called for sub1
        verify(restTemplate, times(1)).postForEntity(eq("https://example.com/webhook1"), any(), eq(String.class));
        verify(restTemplate, never()).postForEntity(eq("https://example.com/webhook2"), any(), eq(String.class));
    }
}
