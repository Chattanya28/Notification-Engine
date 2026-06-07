package com.notification.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.gateway.event.ScheduledNotificationTriggeredEvent;
import com.notification.gateway.model.ScheduledNotification;
import com.notification.gateway.repository.ScheduledNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskRegistry {

    private final ScheduledNotificationRepository scheduledRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleAllActiveTasks() {
        log.info("System Ready: Loading active cron notifications from database...");
        scheduledRepository.findByActiveTrue().forEach(task -> {
            try {
                schedule(task);
            } catch (Exception e) {
                log.error("Failed to schedule job ID {}: {}", task.getId(), e.getMessage());
            }
        });
        log.info("Dynamic scheduler initialized with {} jobs.", scheduledTasks.size());
    }

    public void schedule(ScheduledNotification task) {
        cancel(task.getId());

        Runnable runnable = () -> {
            try {
                log.info("Firing scheduled cron job: ID={}, Recipient={}", task.getId(), task.getRecipient());
                Map<String, Object> variables = null;
                if (task.getVariablesJson() != null && !task.getVariablesJson().isEmpty()) {
                    variables = objectMapper.readValue(task.getVariablesJson(), new TypeReference<Map<String, Object>>() {});
                }
                eventPublisher.publishEvent(new ScheduledNotificationTriggeredEvent(task, variables));
            } catch (Exception e) {
                log.error("Error executing cron job ID {}: {}", task.getId(), e.getMessage());
            }
        };

        try {
            ScheduledFuture<?> future = taskScheduler.schedule(runnable, new CronTrigger(task.getCronExpression()));
            scheduledTasks.put(task.getId(), future);
            log.info("Job ID {} successfully registered with cron: '{}'", task.getId(), task.getCronExpression());
        } catch (IllegalArgumentException e) {
            log.error("Failed to schedule job ID {}: Invalid cron expression '{}'", task.getId(), task.getCronExpression());
            throw e;
        }
    }

    public void cancel(Long id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(true);
            log.info("Job ID {} cancelled and unregistered from task scheduler.", id);
        }
    }
}
