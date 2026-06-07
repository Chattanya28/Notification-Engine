package com.notification.gateway.config;

import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.Permission;
import com.notification.gateway.model.Template;
import com.notification.gateway.model.NotificationType;
import com.notification.gateway.repository.ApiKeyRepository;
import com.notification.gateway.repository.TemplateRepository;
import com.notification.gateway.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final ApiKeyRepository apiKeyRepository;
    private final TemplateRepository templateRepository;
    private final ApiKeyService apiKeyService;

    @Override
    public void run(String... args) {
        seedApiKey();
        seedTemplates();
    }

    private void seedApiKey() {
        if (apiKeyRepository.count() == 0) {
            log.info("Database Seeder: Initializing default developer API key...");
            
            // Raw key to be given to the user / dashboard
            String rawKey = "nk_live_devkey1234567890default";
            String hash = apiKeyService.hashKey(rawKey);
            String prefix = rawKey.substring(0, 12);

            ApiKey defaultKey = ApiKey.builder()
                    .name("Default Developer Key")
                    .keyHash(hash)
                    .keyPrefix(prefix)
                    .permissions(Set.of(
                            Permission.SEND_NOTIFICATION,
                            Permission.READ_HISTORY,
                            Permission.MANAGE_KEYS,
                            Permission.MANAGE_TEMPLATES
                    ))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            apiKeyRepository.save(defaultKey);
            log.info("Database Seeder: Registered key '{}' with hash '{}'", rawKey, hash);
        }
    }

    private void seedTemplates() {
        if (templateRepository.count() == 0) {
            log.info("Database Seeder: Provisioning starter message templates...");

            Template welcomeEmail = Template.builder()
                    .name("welcome-email")
                    .subject("Welcome to our Platform, {{name}}! 🚀")
                    .content("Hello {{name}},\n\nThank you for signing up! We are thrilled to have you.\nPlease click this link to verify your email address: {{link}}\n\nWarm regards,\nThe SaaS Team")
                    .channel(NotificationType.EMAIL)
                    .build();

            Template smsOtp = Template.builder()
                    .name("sms-otp")
                    .content("Your Notification Platform verification code is: {{code}}. This code expires in 5 minutes. Do not share it.")
                    .channel(NotificationType.SMS)
                    .build();

            Template slackAlert = Template.builder()
                    .name("slack-alert")
                    .content("🚨 *System Alert* 🚨\n*Service:* {{service}}\n*Status:* {{status}}\n*Timestamp:* {{time}}\n*Details:* {{message}}")
                    .channel(NotificationType.SLACK)
                    .build();

            templateRepository.save(welcomeEmail);
            templateRepository.save(smsOtp);
            templateRepository.save(slackAlert);
            log.info("Database Seeder: Successfully loaded 3 templates.");
        }
    }
}
