package com.sajith.quotagaurd.limiter;


import com.sajith.quotagaurd.entity.Tenant;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private final ConcurrentHashMap<String,TokenBucket>buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(Tenant tenant)
    {
        TokenBucket bucket=buckets.computeIfAbsent(
            tenant.getApiKey(),
                key-> new TokenBucket(tenant.getCapacity(),tenant.getRefillPerSec())
        );return bucket.tryConsume();
    }
    public void invalidate(String apiKey){
        buckets.remove(apiKey);
    }
}
