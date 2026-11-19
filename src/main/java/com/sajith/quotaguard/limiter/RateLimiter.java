package com.sajith.quotaguard.limiter;

import com.sajith.quotaguard.entity.Tenant;

public interface RateLimiter {
    record Decision(boolean allowed, long remaining, long retryAfterSeconds) {}

    Decision tryConsume(Tenant tenant);

    void invalidate(String apiKey);
}
