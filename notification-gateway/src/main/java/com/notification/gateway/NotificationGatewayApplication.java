package com.notification.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableRetry
public class NotificationGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationGatewayApplication.class, args);
        System.out.println("✅ Notification Gateway Started on http://localhost:8080");
    }
}