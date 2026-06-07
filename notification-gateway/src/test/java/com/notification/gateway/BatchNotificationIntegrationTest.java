package com.notification.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.gateway.dto.NotificationRequestDto;
import com.notification.gateway.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BatchNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate redisTemplate;

    private final String devApiKey = "nk_live_devkey1234567890default";

    @Test
    public void testSendBatchNotifications_Success() throws Exception {
        NotificationRequestDto n1 = NotificationRequestDto.builder()
                .to("email1@example.com")
                .subject("Batch 1")
                .body("Hello 1")
                .type(NotificationType.EMAIL)
                .build();

        NotificationRequestDto n2 = NotificationRequestDto.builder()
                .to("email2@example.com")
                .subject("Batch 2")
                .body("Hello 2")
                .type(NotificationType.EMAIL)
                .build();

        List<NotificationRequestDto> list = List.of(n1, n2);

        mockMvc.perform(post("/api/v1/notifications/batch")
                        .header("X-API-Key", devApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(list)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].recipient").value("email1@example.com"))
                .andExpect(jsonPath("$[1].recipient").value("email2@example.com"));
    }

    @Test
    public void testSendBatchNotifications_OverLimit() throws Exception {
        // Construct an array of 101 requests (exceeding limit of 100)
        NotificationRequestDto base = NotificationRequestDto.builder()
                .to("user@example.com")
                .body("Body")
                .type(NotificationType.EMAIL)
                .build();

        NotificationRequestDto[] array = new NotificationRequestDto[101];
        for (int i = 0; i < 101; i++) {
            array[i] = base;
        }

        mockMvc.perform(post("/api/v1/notifications/batch")
                        .header("X-API-Key", devApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(array)))
                .andExpect(status().isBadRequest());
    }
}
