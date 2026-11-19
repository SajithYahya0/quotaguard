package com.sajith.quotaguard.limiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sajith.quotaguard.entity.Tenant;
import com.sajith.quotaguard.service.TenantService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitInterceptorTest {

    private final TenantService tenants = mock(TenantService.class);

    // Stands in for Redis being down: every decision fails.
    private final RateLimiter redisDown = new RateLimiter() {
        @Override
        public Decision tryConsume(Tenant tenant) {
            throw new LimiterUnavailableException("Redis unavailable", null);
        }

        @Override
        public void invalidate(Tenant tenant) {}
    };

    @BeforeEach
    void knownTenant() {
        when(tenants.byApiKey("k")).thenReturn(Optional.of(new Tenant()));
    }

    @Test
    void failOpen_servesTheRequest() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean proceed = new RateLimitInterceptor(tenants, redisDown, true).preHandle(
            request(),
            res,
            new Object()
        );

        assertTrue(proceed);
        assertEquals("degraded", res.getHeader("X-RateLimit-Status"));
    }

    @Test
    void failClosed_rejectsWith503() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean proceed = new RateLimitInterceptor(tenants, redisDown, false).preHandle(
            request(),
            res,
            new Object()
        );

        assertFalse(proceed);
        assertEquals(503, res.getStatus());
        assertEquals("1", res.getHeader("Retry-After"));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/data/quote");
        req.addHeader("X-API-Key", "k");
        return req;
    }
}
