package com.notification.gateway;

import com.notification.gateway.model.Notification;
import com.notification.gateway.model.NotificationStatus;
import com.notification.gateway.model.NotificationType;
import com.notification.gateway.repository.NotificationRepository;
import com.notification.gateway.service.NotificationDispatcher;
import com.notification.gateway.service.channel.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class NotificationDispatcherTest {

    private NotificationDispatcher notificationDispatcher;

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(emailChannel.supports(NotificationType.EMAIL)).thenReturn(true);
        when(emailChannel.supports(NotificationType.SMS)).thenReturn(false);

        notificationDispatcher = new NotificationDispatcher(
                List.of(emailChannel),
                notificationRepository,
                eventPublisher,
                taskScheduler
        );
    }

    @Test
    public void testDispatchToCorrectChannel() throws Exception {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipient("test@example.com")
                .body("Hello Test")
                .type(NotificationType.EMAIL)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        notificationDispatcher.dispatchWithRetry(notification);

        // Verify the email channel send was invoked
        verify(emailChannel, times(1)).send(notification);
        // Verify repository saved status updates
        verify(notificationRepository, atLeastOnce()).save(notification);
    }
}
