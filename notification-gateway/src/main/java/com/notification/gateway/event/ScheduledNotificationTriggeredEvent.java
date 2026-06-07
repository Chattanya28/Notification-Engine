package com.notification.gateway.event;

import com.notification.gateway.model.ScheduledNotification;
import java.util.Map;

public record ScheduledNotificationTriggeredEvent(
        ScheduledNotification task,
        Map<String, Object> variables
) {}
