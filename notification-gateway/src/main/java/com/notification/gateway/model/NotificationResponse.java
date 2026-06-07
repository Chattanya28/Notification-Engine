package com.notification.gateway.model;

public class NotificationResponse {
    private String messageId;
    private String status;
    private String timestamp;

    public NotificationResponse(String messageId, String status, String timestamp) {
        this.messageId = messageId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
}