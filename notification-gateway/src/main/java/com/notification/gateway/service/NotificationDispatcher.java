package com.notification.gateway.service;

import com.notification.gateway.event.NotificationStatusChangedEvent;
import com.notification.gateway.model.Notification;
import com.notification.gateway.model.NotificationStatus;
import com.notification.gateway.model.NotificationType;
import com.notification.gateway.repository.NotificationRepository;
import com.notification.gateway.service.channel.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final List<NotificationChannel> channels;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ThreadPoolTaskScheduler taskScheduler;

    /**
     * Dispatches notification to supported channel with Spring Retry backoff.
     */
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 4, // 1 initial + 3 retries
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    @Transactional
    public void dispatchWithRetry(Notification notification) throws Exception {
        log.info("Dispatching notification ID {}, Type={}, Attempt={}", 
                 notification.getId(), notification.getType(), notification.getRetryCount() + 1);

        NotificationChannel targetChannel = channels.stream()
                .filter(c -> c.supports(notification.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported channel type: " + notification.getType()));

        try {
            targetChannel.send(notification);
            
            // On success, update status to SENT
            notification.setStatus(NotificationStatus.SENT);
            notificationRepository.save(notification);

            // Publish status change event
            eventPublisher.publishEvent(new NotificationStatusChangedEvent(
                    notification.getId(), notification.getRecipient(), notification.getType(), NotificationStatus.SENT, null
            ));

            // Schedule background simulation for realistic delivery events (DELIVERED, OPENED, CLICKED)
            scheduleDeliverySimulation(notification.getId(), notification.getType());

        } catch (Exception e) {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
            log.error("Failed send attempt for notification ID {}: {}", notification.getId(), e.getMessage());
            throw e; // Rethrow to trigger retry
        }
    }

    /**
     * Fallback recovery when all retries are exhausted.
     */
    @Recover
    @Transactional
    public void recover(Exception e, Notification notification) {
        log.error("Exhausted all retry attempts for notification ID {}. Marking status as FAILED.", notification.getId());
        
        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage("Failed after maximum retries. Root cause: " + e.getMessage());
        notificationRepository.save(notification);

        eventPublisher.publishEvent(new NotificationStatusChangedEvent(
                notification.getId(), notification.getRecipient(), notification.getType(), NotificationStatus.FAILED, e.getMessage()
        ));
    }

    /**
     * Schedules state transitions to simulate real-world events.
     */
    private void scheduleDeliverySimulation(UUID notificationId, NotificationType type) {
        // 1. Transition to DELIVERED after 2 seconds
        taskScheduler.schedule(() -> updateStatus(notificationId, NotificationStatus.DELIVERED), 
                               Instant.now().plusSeconds(2));

        // 2. If EMAIL, transition to OPENED (after 5s) and CLICKED (after 8s)
        if (type == NotificationType.EMAIL) {
            taskScheduler.schedule(() -> updateStatus(notificationId, NotificationStatus.OPENED), 
                                   Instant.now().plusSeconds(5));
            taskScheduler.schedule(() -> updateStatus(notificationId, NotificationStatus.CLICKED), 
                                   Instant.now().plusSeconds(8));
        }
    }

    @Transactional
    public void updateStatus(UUID notificationId, NotificationStatus status) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            // Prevent changing status if it failed
            if (notification.getStatus() == NotificationStatus.FAILED) {
                return;
            }
            notification.setStatus(status);
            notificationRepository.save(notification);

            eventPublisher.publishEvent(new NotificationStatusChangedEvent(
                    notification.getId(), notification.getRecipient(), notification.getType(), status, null
            ));
            log.debug("Simulation: Notification ID {} updated status to {}", notificationId, status);
        });
    }
}
