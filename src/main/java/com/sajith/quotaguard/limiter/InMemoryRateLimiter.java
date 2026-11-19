package com.sajith.quotaguard.limiter;


import com.sajith.quotaguard.entity.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name ="quotaguard.limiter",havingValue="memory",matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter{
    private final ConcurrentHashMap<String,TokenBucket>buckets = new ConcurrentHashMap<>();
    @Override
    public Decision tryConsume(Tenant tenant)
    {
        TokenBucket bucket=buckets.computeIfAbsent(
            tenant.getApiKey(),
                key-> new TokenBucket(tenant.getCapacity(),tenant.getRefillPerSec())
        );return new Decision(bucket.tryConsume(),-1,1);
    }

    @Override
    public void invalidate(String apiKey){
        buckets.remove(apiKey);
    }
}
