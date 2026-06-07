package com.notification.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.gateway.dto.NotificationRequestDto;
import com.notification.gateway.dto.ScheduleNotificationRequestDto;
import com.notification.gateway.model.NotificationType;
import com.notification.gateway.model.Template;
import com.notification.gateway.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TemplateRepository templateRepository;

    // Mock Redis to prevent connection failures during test runs
    @MockBean
    private StringRedisTemplate redisTemplate;

    private final String devApiKey = "nk_live_devkey1234567890default";

    @Test
    public void testHealthEndpoint_Public() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/health"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSendNotification_Unauthorized() throws Exception {
        NotificationRequestDto dto = NotificationRequestDto.builder()
                .to("recipient@example.com")
                .subject("Test")
                .body("Hello")
                .type(NotificationType.EMAIL)
                .build();

        mockMvc.perform(post("/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSendNotification_Authorized() throws Exception {
        NotificationRequestDto dto = NotificationRequestDto.builder()
                .to("recipient@example.com")
                .subject("Welcome!")
                .body("Hello World")
                .type(NotificationType.EMAIL)
                .build();

        mockMvc.perform(post("/api/v1/notifications/send")
                        .header("X-API-Key", devApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.recipient").value("recipient@example.com"));
    }

    @Test
    public void testSendNotification_ValidationError() throws Exception {
        NotificationRequestDto invalidDto = NotificationRequestDto.builder()
                .to("") // Blank recipient (violates validation)
                .body("Hello World")
                .type(NotificationType.EMAIL)
                .build();

        mockMvc.perform(post("/api/v1/notifications/send")
                        .header("X-API-Key", devApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testScheduleNotification_WithTemplateName() throws Exception {
        // Pre-seed a template
        Template template = Template.builder()
                .name("test-schedule-template")
                .subject("Schedule Subject for {{name}}")
                .content("Schedule Content for {{name}} at {{link}}")
                .channel(NotificationType.EMAIL)
                .build();
        templateRepository.save(template);

        ScheduleNotificationRequestDto dto = ScheduleNotificationRequestDto.builder()
                .to("recipient@example.com")
                .templateName("test-schedule-template")
                .type(NotificationType.EMAIL)
                .cronExpression("0 0/5 * * * ?") // Valid cron: every 5 minutes
                .variables(Map.of("name", "Shreya", "link", "https://google.com"))
                .build();

        mockMvc.perform(post("/api/v1/notifications/schedule")
                        .header("X-API-Key", devApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipient").value("recipient@example.com"))
                .andExpect(jsonPath("$.subject").value("Schedule Subject for {{name}}"))
                .andExpect(jsonPath("$.bodyTemplate").value("Schedule Content for {{name}} at {{link}}"))
                .andExpect(jsonPath("$.cronExpression").value("0 0/5 * * * ?"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    public void testScheduleNotification_ValidationError() throws Exception {
        ScheduleNotificationRequestDto invalidDto = ScheduleNotificationRequestDto.builder()
                .to("recipient@example.com")
                .bodyTemplate("Plain template")
                .type(NotificationType.EMAIL)
                .cronExpression("Helllo") // Invalid cron expression
                .build();

        mockMvc.perform(post("/api/v1/notifications/schedule")
                        .header("X-API-Key", devApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid cron expression: Helllo"));
    }
}
