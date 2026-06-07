package com.notification.gateway.service;

import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.Permission;
import com.notification.gateway.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new API Key for a client, hashes it, and persists the hash.
     * Returns the raw key once (it cannot be retrieved again).
     */
    @Transactional
    public String createApiKey(String name, Set<Permission> permissions) {
        String rawKey = generateSecureKey();
        String hash = hashKey(rawKey);
        String prefix = rawKey.substring(0, 12); // nk_live_xxxx

        ApiKey apiKey = ApiKey.builder()
                .name(name)
                .keyHash(hash)
                .keyPrefix(prefix)
                .permissions(permissions)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        apiKeyRepository.save(apiKey);
        log.info("Created new API key for client '{}' with prefix '{}'", name, prefix);
        return rawKey;
    }

    /**
     * Validates a raw API key. If valid, returns the associated ApiKey.
     */
    public Optional<ApiKey> validateApiKey(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) {
            return Optional.empty();
        }
        String hash = hashKey(rawKey);
        return apiKeyRepository.findByKeyHash(hash)
                .filter(ApiKey::isActive);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> getAllApiKeys() {
        return apiKeyRepository.findAll();
    }

    @Transactional
    public void revokeApiKey(Long id) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            key.setActive(false);
            apiKeyRepository.save(key);
            log.info("Revoked API key with prefix '{}'", key.getKeyPrefix());
        });
    }

    private String generateSecureKey() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        String randomHex = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                .replace('-', 'x')
                .replace('_', 'y');
        return "nk_live_" + randomHex;
    }

    public String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash API key", e);
            throw new RuntimeException("SHA-256 hashing algorithm not found", e);
        }
    }
}
