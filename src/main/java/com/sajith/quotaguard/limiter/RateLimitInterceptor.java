package com.sajith.quotaguard.limiter;

import com.sajith.quotaguard.entity.Tenant;
import com.sajith.quotaguard.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final TenantService tenantService;
    private final RateLimiter rateLimiter;
    private final boolean failOpen;

    public RateLimitInterceptor(
        TenantService tenantService,
        RateLimiter rateLimiter,
        @Value("${quotaguard.fail-open:true}") boolean failOpen
    ) {
        this.tenantService = tenantService;
        this.rateLimiter = rateLimiter;
        this.failOpen = failOpen;
    }

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            return reject(response, 401, "Missing X-API-Key header");
        }
        Optional<Tenant> tenant = tenantService.byApiKey(apiKey);
        if (tenant.isEmpty()) {
            return reject(response, 401, "Invalid API Key");
        }

        Tenant t = tenant.get();
        RateLimiter.Decision d;
        try {
            d = rateLimiter.tryConsume(t);
        } catch (LimiterUnavailableException e) {
            return onLimiterDown(response, e);
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(t.getCapacity()));
        if (d.remaining() >= 0) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(d.remaining()));
        }

        if (!d.allowed()) {
            if (d.retryAfterSeconds() > 0) {
                response.setHeader("Retry-After", String.valueOf(d.retryAfterSeconds()));
            }
            return reject(response, 429, "Rate limit exceeded");
        }
        return true;
    }

    // Fail open = keep serving without limits (availability first).
    // Fail closed = refuse with 503 (protection first, e.g. paid or expensive APIs).
    // Either way the client gets an answer within the Redis timeout, not after 60 s.
    private boolean onLimiterDown(
        HttpServletResponse response,
        LimiterUnavailableException e
    ) throws IOException {
        log.warn("Rate limiter unavailable, failing {}: {}", failOpen ? "open" : "closed", e.getMessage());
        if (failOpen) {
            response.setHeader("X-RateLimit-Status", "degraded");
            return true;
        }
        response.setHeader("Retry-After", "1");
        return reject(response, 503, "Rate limiter unavailable");
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        return false;
    }
}
