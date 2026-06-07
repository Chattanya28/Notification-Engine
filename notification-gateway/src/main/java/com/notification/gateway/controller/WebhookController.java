package com.notification.gateway.controller;

import com.notification.gateway.config.RequiresPermission;
import com.notification.gateway.dto.WebhookRequestDto;
import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.Permission;
import com.notification.gateway.model.WebhookSubscription;
import com.notification.gateway.repository.WebhookSubscriptionRepository;
import com.notification.gateway.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebhookController {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final AuditLogService auditLogService;

    @PostMapping
    @RequiresPermission(Permission.MANAGE_KEYS)
    public ResponseEntity<WebhookSubscription> subscribe(@Valid @RequestBody WebhookRequestDto request, HttpServletRequest httpRequest) {
        WebhookSubscription subscription = WebhookSubscription.builder()
                .url(request.getUrl())
                .events(request.getEvents())
                .active(true)
                .build();

        WebhookSubscription saved = subscriptionRepository.save(subscription);

        ApiKey authenticatedKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        auditLogService.logAsync(
                authenticatedKey != null ? authenticatedKey.getName() : "SYSTEM",
                "CREATE_WEBHOOK",
                httpRequest.getRemoteAddr(),
                "Registered webhook URL '" + request.getUrl() + "' for events: " + request.getEvents()
        );

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @RequiresPermission(Permission.MANAGE_KEYS)
    public ResponseEntity<List<WebhookSubscription>> listWebhooks() {
        return ResponseEntity.ok(subscriptionRepository.findAll());
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.MANAGE_KEYS)
    public ResponseEntity<?> unsubscribe(@PathVariable Long id, HttpServletRequest httpRequest) {
        subscriptionRepository.findById(id).ifPresent(sub -> {
            sub.setActive(false);
            subscriptionRepository.save(sub);
            
            // Delete subscription for demo cleanup
            subscriptionRepository.deleteById(id);

            ApiKey authenticatedKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
            auditLogService.logAsync(
                    authenticatedKey != null ? authenticatedKey.getName() : "SYSTEM",
                    "DELETE_WEBHOOK",
                    httpRequest.getRemoteAddr(),
                    "Removed webhook subscription ID: " + id + " for URL: " + sub.getUrl()
            );
        });

        return ResponseEntity.ok(Map.of("message", "Webhook subscription successfully deleted"));
    }
}
