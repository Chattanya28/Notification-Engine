package com.notification.gateway.controller;

import com.notification.gateway.config.RequiresPermission;
import com.notification.gateway.dto.ApiKeyRequestDto;
import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.AuditLog;
import com.notification.gateway.model.Permission;
import com.notification.gateway.service.ApiKeyService;
import com.notification.gateway.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final AuditLogService auditLogService;

    @PostMapping("/api-keys")
    @RequiresPermission(Permission.MANAGE_KEYS)
    public ResponseEntity<?> createApiKey(@Valid @RequestBody ApiKeyRequestDto request, HttpServletRequest httpRequest) {
        String rawKey = apiKeyService.createApiKey(request.getName(), request.getPermissions());
        
        ApiKey authenticatedKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        auditLogService.logAsync(
                authenticatedKey != null ? authenticatedKey.getName() : "SYSTEM",
                "CREATE_API_KEY",
                httpRequest.getRemoteAddr(),
                "Created API Key '" + request.getName() + "' with permissions: " + request.getPermissions()
        );

        return ResponseEntity.ok(Map.of(
                "key", rawKey,
                "message", "Please store this key securely. You will not be able to retrieve it again."
        ));
    }

    @GetMapping("/api-keys")
    @RequiresPermission(Permission.MANAGE_KEYS)
    public ResponseEntity<List<ApiKey>> listApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    @DeleteMapping("/api-keys/{id}")
    @RequiresPermission(Permission.MANAGE_KEYS)
    public ResponseEntity<?> revokeApiKey(@PathVariable Long id, HttpServletRequest httpRequest) {
        apiKeyService.revokeApiKey(id);

        ApiKey authenticatedKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        auditLogService.logAsync(
                authenticatedKey != null ? authenticatedKey.getName() : "SYSTEM",
                "REVOKE_API_KEY",
                httpRequest.getRemoteAddr(),
                "Revoked API Key with ID " + id
        );

        return ResponseEntity.ok(Map.of("message", "API Key successfully revoked"));
    }

    @GetMapping("/audit-logs")
    @RequiresPermission(Permission.READ_HISTORY)
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }
}
