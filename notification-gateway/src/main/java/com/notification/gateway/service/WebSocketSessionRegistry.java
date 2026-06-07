package com.notification.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.gateway.event.NotificationStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionRegistry {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public void register(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket session registered: {}", session.getId());
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
        log.debug("WebSocket session unregistered: {}", session.getId());
    }

    /**
     * Broadcasts a text message to all connected clients.
     */
    public void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Listens to status changes of notifications and automatically broadcasts them to the UI dashboard.
     */
    @EventListener
    public void handleNotificationStatusChanged(NotificationStatusChangedEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            broadcast(jsonPayload);
        } catch (Exception e) {
            log.error("Failed to serialize status change event for WebSocket broadcast", e);
        }
    }
}
