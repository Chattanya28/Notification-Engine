package com.notification.gateway.controller;

import com.notification.gateway.config.RequiresPermission;
import com.notification.gateway.dto.NotificationRequestDto;
import com.notification.gateway.dto.NotificationResponseDto;
import com.notification.gateway.dto.ScheduleNotificationRequestDto;
import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.Permission;
import com.notification.gateway.model.ScheduledNotification;
import com.notification.gateway.service.AuditLogService;
import com.notification.gateway.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @PostMapping("/send")
    @RequiresPermission(Permission.SEND_NOTIFICATION)
    public ResponseEntity<NotificationResponseDto> sendNotification(
            @Valid @RequestBody NotificationRequestDto request,
            HttpServletRequest httpRequest) {
        
        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        NotificationResponseDto response = notificationService.sendNotification(request, apiKey);

        auditLogService.logAsync(
                apiKey.getName(),
                "SEND_NOTIFICATION",
                httpRequest.getRemoteAddr(),
                "Sent single " + request.getType() + " notification to: " + request.getTo()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    @RequiresPermission(Permission.SEND_NOTIFICATION)
    public ResponseEntity<List<NotificationResponseDto>> sendBatch(
            @Valid @RequestBody List<NotificationRequestDto> requests,
            HttpServletRequest httpRequest) {

        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        List<NotificationResponseDto> response = notificationService.sendBatchNotifications(requests, apiKey);

        auditLogService.logAsync(
                apiKey.getName(),
                "SEND_BATCH_NOTIFICATION",
                httpRequest.getRemoteAddr(),
                "Sent batch of " + requests.size() + " notifications"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/schedule")
    @RequiresPermission(Permission.SEND_NOTIFICATION)
    public ResponseEntity<ScheduledNotification> scheduleNotification(
            @Valid @RequestBody ScheduleNotificationRequestDto request,
            HttpServletRequest httpRequest) {

        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        ScheduledNotification scheduled = notificationService.scheduleNotification(request, apiKey);

        auditLogService.logAsync(
                apiKey.getName(),
                "SCHEDULE_NOTIFICATION",
                httpRequest.getRemoteAddr(),
                "Scheduled dynamic " + request.getType() + " recurring job for " + request.getTo() + " with cron '" + request.getCronExpression() + "'"
        );

        return ResponseEntity.ok(scheduled);
    }

    @GetMapping("/schedules")
    @RequiresPermission(Permission.SEND_NOTIFICATION)
    public ResponseEntity<List<ScheduledNotification>> listSchedules() {
        return ResponseEntity.ok(notificationService.getActiveSchedules());
    }

    @DeleteMapping("/schedules/{id}")
    @RequiresPermission(Permission.SEND_NOTIFICATION)
    public ResponseEntity<?> revokeSchedule(@PathVariable Long id, HttpServletRequest httpRequest) {
        notificationService.revokeSchedule(id);

        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        auditLogService.logAsync(
                apiKey.getName(),
                "REVOKE_SCHEDULE",
                httpRequest.getRemoteAddr(),
                "Revoked scheduled cron job ID " + id
        );

        return ResponseEntity.ok(Map.of("message", "Scheduled notification successfully cancelled"));
    }

    @GetMapping("/history")
    @RequiresPermission(Permission.READ_HISTORY)
    public ResponseEntity<List<NotificationResponseDto>> getHistory() {
        return ResponseEntity.ok(notificationService.getHistory());
    }

    @GetMapping("/export/csv")
    @RequiresPermission(Permission.READ_HISTORY)
    public void exportCsv(HttpServletResponse response, HttpServletRequest httpRequest) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notifications_history.csv\"");

        notificationService.exportToCsv(response.getWriter());

        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        auditLogService.logAsync(
                apiKey.getName(),
                "EXPORT_CSV",
                httpRequest.getRemoteAddr(),
                "Exported notification logs history to CSV"
        );
    }

    @GetMapping("/export/pdf")
    @RequiresPermission(Permission.READ_HISTORY)
    public void exportPdf(HttpServletResponse response, HttpServletRequest httpRequest) throws IOException {
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notifications_history.pdf\"");

        notificationService.exportToPdf(response.getOutputStream());

        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("authenticatedApiKey");
        auditLogService.logAsync(
                apiKey.getName(),
                "EXPORT_PDF",
                httpRequest.getRemoteAddr(),
                "Exported notification logs history to PDF"
        );
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gateway is healthy and running!");
    }
}