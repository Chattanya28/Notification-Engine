package com.notification.gateway.controller;

import com.notification.gateway.config.RequiresPermission;
import com.notification.gateway.dto.TemplateRequestDto;
import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.Permission;
import com.notification.gateway.model.Template;
import com.notification.gateway.service.AuditLogService;
import com.notification.gateway.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TemplateController {

    private final TemplateService templateService;
    private final AuditLogService auditLogService;

    @PostMapping
    @RequiresPermission(Permission.MANAGE_TEMPLATES)
    public ResponseEntity<Template> createTemplate(@Valid @RequestBody TemplateRequestDto request, HttpServletRequest httpRequest) {
        Template template = Template.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .content(request.getContent())
                .channel(request.getChannel())
                .build();

        Template saved = templateService.saveTemplate(template);

        ApiKey authenticatedKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        auditLogService.logAsync(
                authenticatedKey != null ? authenticatedKey.getName() : "SYSTEM",
                "CREATE_TEMPLATE",
                httpRequest.getRemoteAddr(),
                "Created template '" + request.getName() + "' for channel: " + request.getChannel()
        );

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @RequiresPermission(Permission.SEND_NOTIFICATION)
    public ResponseEntity<List<Template>> listTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.MANAGE_TEMPLATES)
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id, HttpServletRequest httpRequest) {
        templateService.deleteTemplate(id);

        ApiKey authenticatedKey = (httpRequest != null) ? (ApiKey) httpRequest.getAttribute("authenticatedApiKey") : null;
        auditLogService.logAsync(
                authenticatedKey != null ? authenticatedKey.getName() : "SYSTEM",
                "DELETE_TEMPLATE",
                httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1",
                "Deleted template with ID: " + id
        );

        return ResponseEntity.ok(Map.of("message", "Template successfully deleted"));
    }
}
