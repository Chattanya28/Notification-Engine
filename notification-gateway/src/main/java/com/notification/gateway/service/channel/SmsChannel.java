package com.notification.gateway.service.channel;

import com.notification.gateway.model.Notification;
import com.notification.gateway.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsChannel implements NotificationChannel {

    private final RestTemplate restTemplate;

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.from.number:}")
    private String fromNumber;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }

    @Override
    public void send(Notification notification) throws Exception {
        boolean isTwilioConfigured = accountSid != null && !accountSid.trim().isEmpty() &&
                                     authToken != null && !authToken.trim().isEmpty() &&
                                     fromNumber != null && !fromNumber.trim().isEmpty();

        if (isTwilioConfigured) {
            log.info("Dispatching real SMS to {} via Twilio REST API", notification.getRecipient());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String auth = accountSid + ":" + authToken;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("To", notification.getRecipient());
            map.add("From", fromNumber);
            map.add("Body", notification.getBody());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            try {
                restTemplate.postForEntity(url, request, String.class);
                log.info("Twilio SMS sent successfully to {}", notification.getRecipient());
            } catch (Exception e) {
                log.error("Failed to send Twilio SMS to {}: {}", notification.getRecipient(), e.getMessage());
                throw e;
            }
        } else {
            log.info("Twilio properties not configured. Dispatching mock SMS to {}", notification.getRecipient());
            
            System.out.println("\n--- [MOCK SMS SENT] ---");
            System.out.println("To:   " + notification.getRecipient());
            System.out.println("Body: " + notification.getBody());
            System.out.println("------------------------\n");
        }
    }
}
