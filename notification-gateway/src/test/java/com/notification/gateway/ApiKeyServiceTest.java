package com.notification.gateway;

import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.Permission;
import com.notification.gateway.repository.ApiKeyRepository;
import com.notification.gateway.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ApiKeyServiceTest {

    private ApiKeyService apiKeyService;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        apiKeyService = new ApiKeyService(apiKeyRepository);
    }

    @Test
    public void testCreateApiKey() {
        String name = "Test Client";
        Set<Permission> permissions = Set.of(Permission.SEND_NOTIFICATION);

        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawKey = apiKeyService.createApiKey(name, permissions);

        assertNotNull(rawKey);
        assertTrue(rawKey.startsWith("nk_live_"));
        verify(apiKeyRepository, times(1)).save(any(ApiKey.class));
    }

    @Test
    public void testValidateApiKey_Valid() {
        String rawKey = "nk_live_testkey123";
        String hash = apiKeyService.hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .name("Test Key")
                .keyHash(hash)
                .keyPrefix("nk_live_test")
                .active(true)
                .permissions(Set.of(Permission.SEND_NOTIFICATION))
                .createdAt(LocalDateTime.now())
                .build();

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey);

        assertTrue(result.isPresent());
        assertEquals("Test Key", result.get().getName());
        assertTrue(result.get().isActive());
    }

    @Test
    public void testValidateApiKey_Invalid() {
        String rawKey = "nk_live_invalidkey";
        String hash = apiKeyService.hashKey(rawKey);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.empty());

        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey);

        assertFalse(result.isPresent());
    }

    @Test
    public void testValidateApiKey_Inactive() {
        String rawKey = "nk_live_inactivekey";
        String hash = apiKeyService.hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .name("Inactive Key")
                .keyHash(hash)
                .keyPrefix("nk_live_inac")
                .active(false)
                .permissions(Set.of(Permission.SEND_NOTIFICATION))
                .build();

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKey);

        assertFalse(result.isPresent()); // Should filter out inactive keys
    }
}
