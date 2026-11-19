package com.sajith.quotaguard.limiter;

import com.sajith.quotaguard.entity.Tenant;

public interface RateLimiter {
    record Decision(boolean allowed, long remaining, long retryAfterSeconds) {}

    /** @throws LimiterUnavailableException when the backing store can't answer in time */
    Decision tryConsume(Tenant tenant);

    void invalidate(Tenant tenant);
}
