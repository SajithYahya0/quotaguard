package com.sajith.quotagaurd.limiter;

import com.sajith.quotagaurd.entity.Tenant;

public interface RateLimiter {
    record Decision(boolean allowed,long remaining,long retryAfterSeconds)
    {

    }
    Decision tryConsume(Tenant tenant);

    void invalidate(String apiKey);
}
