package com.notification.gateway.config;

import com.notification.gateway.model.ApiKey;
import com.notification.gateway.model.Permission;
import com.notification.gateway.service.ApiKeyService;
import com.notification.gateway.service.AuditLogService;
import com.notification.gateway.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;
    private final RateLimiter rateLimiter;
    private final AuditLogService auditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Exclude options requests (CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Exclude Swagger, WebSocket and static resources
        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || 
            path.startsWith("/ws/events") || path.equals("/") || path.equals("/index.html") || 
            path.startsWith("/static/") || path.equals("/favicon.ico")) {
            return true;
        }

        // Check if handler is a controller method
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check for RequiresPermission annotation
        RequiresPermission annotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        }

        // If it's an API route or has the annotation, validate the API Key
        boolean isApiRoute = path.startsWith("/api/");
        if (annotation != null || isApiRoute) {
            // Exclude public API health check
            if (path.endsWith("/health")) {
                return true;
            }

            String rawKey = request.getHeader("X-API-Key");
            if (rawKey == null || rawKey.isEmpty()) {
                // Check if query parameter is used (useful for SSE or easy testing)
                rawKey = request.getParameter("apiKey");
            }

            if (rawKey == null || rawKey.isEmpty()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("{\"error\": \"Unauthorized: Missing X-API-Key header\"}");
                response.setContentType("application/json");
                return false;
            }

            Optional<ApiKey> apiKeyOpt = apiKeyService.validateApiKey(rawKey);
            if (apiKeyOpt.isEmpty()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("{\"error\": \"Unauthorized: Invalid or revoked API Key\"}");
                response.setContentType("application/json");
                return false;
            }

            ApiKey apiKey = apiKeyOpt.get();

            // Store API key in request attributes for controllers to use
            request.setAttribute("authenticatedApiKey", apiKey);

            // Rate Limiting Check (100 requests/min)
            boolean allowed = rateLimiter.isAllowed(apiKey.getKeyHash());
            if (!allowed) {
                auditLogService.logAsync(
                    apiKey.getName(),
                    "RATE_LIMIT_EXCEEDED",
                    getClientIp(request),
                    "API Key exceeded the limit of 100 requests per minute on path: " + path
                );
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("{\"error\": \"Too Many Requests: Rate limit exceeded. 100 requests per minute limit.\"}");
                response.setContentType("application/json");
                return false;
            }

            // Permission check
            if (annotation != null && annotation.value().length > 0) {
                Permission[] required = annotation.value();
                boolean hasPermission = Arrays.stream(required)
                        .anyMatch(p -> apiKey.getPermissions().contains(p));

                if (!hasPermission) {
                    auditLogService.logAsync(
                        apiKey.getName(),
                        "FORBIDDEN_ACCESS",
                        getClientIp(request),
                        "Denied access to " + path + " due to missing permissions: " + Arrays.toString(required)
                    );
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("{\"error\": \"Forbidden: Insufficient permissions\"}");
                    response.setContentType("application/json");
                    return false;
                }
            }
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
