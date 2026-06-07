package com.notification.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.notification.gateway.dto.NotificationRequestDto;
import com.notification.gateway.dto.NotificationResponseDto;
import com.notification.gateway.dto.ScheduleNotificationRequestDto;
import com.notification.gateway.event.NotificationStatusChangedEvent;
import com.notification.gateway.model.*;
import com.notification.gateway.repository.NotificationRepository;
import com.notification.gateway.repository.ScheduledNotificationRepository;
import com.notification.gateway.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TemplateRepository templateRepository;
    private final ScheduledNotificationRepository scheduledRepository;
    private final TemplateService templateService;
    private final NotificationDispatcher notificationDispatcher;
    private final ScheduledTaskRegistry scheduledTaskRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Sends a single notification. Resolves template variables if templateName is provided.
     */
    @Transactional
    public NotificationResponseDto sendNotification(NotificationRequestDto dto, ApiKey apiKey) {
        Notification notification = prepareNotification(dto, apiKey);
        
        // Save database entry in PENDING status
        notificationRepository.save(notification);

        // Publish event for PENDING status
        eventPublisher.publishEvent(new NotificationStatusChangedEvent(
                notification.getId(), notification.getRecipient(), notification.getType(), NotificationStatus.PENDING, null
        ));

        // Asynchronously dispatch to retry/channel handler
        sendAsync(notification);

        return NotificationResponseDto.builder()
                .messageId(notification.getId())
                .status("PENDING")
                .recipient(notification.getRecipient())
                .type(notification.getType().name())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    /**
     * Sends batch notifications (up to 100 in a single request).
     */
    @Transactional
    public List<NotificationResponseDto> sendBatchNotifications(List<NotificationRequestDto> requests, ApiKey apiKey) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Batch request list cannot be empty");
        }
        if (requests.size() > 100) {
            throw new IllegalArgumentException("Batch request exceeds maximum limit of 100 notifications");
        }

        log.info("Processing batch send request of size {} for key prefix {}", requests.size(), apiKey.getKeyPrefix());

        return requests.stream()
                .map(req -> sendNotification(req, apiKey))
                .collect(Collectors.toList());
    }

    /**
     * Dynamically registers a dynamic scheduled cron job.
     */
    @Transactional
    public ScheduledNotification scheduleNotification(ScheduleNotificationRequestDto dto, ApiKey apiKey) {
        // Validate either bodyTemplate or templateName is populated
        boolean hasBodyTemplate = dto.getBodyTemplate() != null && !dto.getBodyTemplate().trim().isEmpty();
        boolean hasTemplateName = dto.getTemplateName() != null && !dto.getTemplateName().trim().isEmpty();

        if (!hasBodyTemplate && !hasTemplateName) {
            throw new IllegalArgumentException("Either bodyTemplate or templateName must be provided");
        }

        // Validate cron expression
        if (dto.getCronExpression() == null || !org.springframework.scheduling.support.CronExpression.isValidExpression(dto.getCronExpression())) {
            throw new IllegalArgumentException("Invalid cron expression: " + dto.getCronExpression());
        }

        String resolvedBodyTemplate = dto.getBodyTemplate();
        String resolvedSubject = dto.getSubject();

        if (hasTemplateName) {
            Template template = templateRepository.findByName(dto.getTemplateName())
                    .orElseThrow(() -> new IllegalArgumentException("Template with name '" + dto.getTemplateName() + "' not found"));

            if (template.getChannel() != dto.getType()) {
                throw new IllegalArgumentException(String.format("Template channel '%s' does not match request channel '%s'", 
                        template.getChannel(), dto.getType()));
            }

            resolvedBodyTemplate = template.getContent();
            if ((resolvedSubject == null || resolvedSubject.trim().isEmpty()) && template.getSubject() != null) {
                resolvedSubject = template.getSubject();
            }
        }

        String varsJson = null;
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            try {
                varsJson = objectMapper.writeValueAsString(dto.getVariables());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid template variables: " + e.getMessage());
            }
        }

        ScheduledNotification scheduledNotification = ScheduledNotification.builder()
                .apiKey(apiKey)
                .recipient(dto.getTo())
                .subject(resolvedSubject)
                .bodyTemplate(resolvedBodyTemplate)
                .type(dto.getType())
                .cronExpression(dto.getCronExpression())
                .variablesJson(varsJson)
                .active(true)
                .build();

        scheduledRepository.save(scheduledNotification);

        // Schedule programmatically in the task registry
        scheduledTaskRegistry.schedule(scheduledNotification);

        return scheduledNotification;
    }

    @Transactional(readOnly = true)
    public List<ScheduledNotification> getActiveSchedules() {
        return scheduledRepository.findByActiveTrue();
    }

    @Transactional
    public void revokeSchedule(Long id) {
        scheduledRepository.findById(id).ifPresent(schedule -> {
            schedule.setActive(false);
            scheduledRepository.save(schedule);
            scheduledTaskRegistry.cancel(id);
            log.info("Revoked and cancelled scheduled cron job ID {}", id);
        });
    }

    @org.springframework.context.event.EventListener
    public void handleScheduledNotificationTriggered(com.notification.gateway.event.ScheduledNotificationTriggeredEvent event) {
        sendCronNotification(event.task(), event.variables());
    }

    /**
     * Triggers when a dynamically scheduled cron job fires.
     */
    @Transactional
    public void sendCronNotification(ScheduledNotification task, Map<String, Object> variables) {
        String body = templateService.render(task.getBodyTemplate(), variables);
        
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .apiKey(task.getApiKey())
                .recipient(task.getRecipient())
                .subject(task.getSubject())
                .body(body)
                .type(task.getType())
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        notificationRepository.save(notification);

        eventPublisher.publishEvent(new NotificationStatusChangedEvent(
                notification.getId(), notification.getRecipient(), notification.getType(), NotificationStatus.PENDING, null
        ));

        sendAsync(notification);
    }

    @Async
    public void sendAsync(Notification notification) {
        try {
            notificationDispatcher.dispatchWithRetry(notification);
        } catch (Exception e) {
            log.error("Asynchronous dispatch failed for notification ID {}: {}", notification.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getHistory() {
        return notificationRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(n -> NotificationResponseDto.builder()
                        .messageId(n.getId())
                        .status(n.getStatus().name())
                        .recipient(n.getRecipient())
                        .type(n.getType().name())
                        .timestamp(n.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public void exportToCsv(Writer writer) {
        List<Notification> list = notificationRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("MessageID,Recipient,Type,Status,Subject,Retries,CreatedAt,ErrorMessage");
        
        for (Notification n : list) {
            String error = n.getErrorMessage() != null ? n.getErrorMessage().replace("\"", "\"\"") : "";
            printWriter.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\"\n",
                    n.getId(),
                    n.getRecipient(),
                    n.getType().name(),
                    n.getStatus().name(),
                    n.getSubject() != null ? n.getSubject().replace("\"", "\"\"") : "",
                    n.getRetryCount(),
                    n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    error
            );
        }
        printWriter.flush();
    }

    @Transactional(readOnly = true)
    public void exportToPdf(OutputStream outputStream) {
        List<Notification> list = notificationRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Set Title Font
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Notification Delivery Audit Logs", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Table of records: 6 columns
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{3.2f, 1.2f, 1.0f, 1.0f, 0.8f, 2.8f});

            // Headers
            table.addCell("Message ID");
            table.addCell("Type");
            table.addCell("Status");
            table.addCell("Retries");
            table.addCell("Channel");
            table.addCell("Recipient");

            for (Notification n : list) {
                table.addCell(n.getId().toString().substring(0, 8) + "...");
                table.addCell(n.getType().name());
                table.addCell(n.getStatus().name());
                table.addCell(String.valueOf(n.getRetryCount()));
                table.addCell(n.getSubject() != null ? n.getSubject() : "-");
                table.addCell(n.getRecipient());
            }

            document.add(table);
        } catch (Exception e) {
            log.error("Failed to generate PDF export", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    private Notification prepareNotification(NotificationRequestDto dto, ApiKey apiKey) {
        String resolvedBody = dto.getBody();
        String resolvedSubject = dto.getSubject();

        // Template logic
        if (dto.getTemplateName() != null && !dto.getTemplateName().isEmpty()) {
            Template template = templateRepository.findByName(dto.getTemplateName())
                    .orElseThrow(() -> new IllegalArgumentException("Template with name '" + dto.getTemplateName() + "' not found"));

            if (template.getChannel() != dto.getType()) {
                throw new IllegalArgumentException(String.format("Template channel '%s' does not match request channel '%s'", 
                        template.getChannel(), dto.getType()));
            }

            resolvedBody = templateService.render(template.getContent(), dto.getVariables());
            if (template.getSubject() != null && !template.getSubject().isEmpty()) {
                resolvedSubject = templateService.render(template.getSubject(), dto.getVariables());
            }
        }

        if (resolvedBody == null || resolvedBody.isEmpty()) {
            throw new IllegalArgumentException("Notification body content is empty");
        }

        return Notification.builder()
                .id(UUID.randomUUID())
                .apiKey(apiKey)
                .recipient(dto.getTo())
                .subject(resolvedSubject)
                .body(resolvedBody)
                .type(dto.getType())
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }
}