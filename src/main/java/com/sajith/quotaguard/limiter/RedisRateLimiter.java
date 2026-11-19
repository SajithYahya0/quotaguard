package com.sajith.quotaguard.limiter;

import com.sajith.quotaguard.entity.Tenant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "quotaguard.limiter", havingValue = "redis")
public class RedisRateLimiter implements RateLimiter {

    private static final long TTL_FLOOR_SECONDS = 60;

    private final StringRedisTemplate redis;
    private final RedisScript<List> script;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        DefaultRedisScript<List> s = new DefaultRedisScript<>();
        s.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        s.setResultType(List.class);
        this.script = s;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Decision tryConsume(Tenant tenant) {
        List<Long> r = redis.execute(
            script,
            List.of(key(tenant)),
            String.valueOf(tenant.getCapacity()),
            String.valueOf(tenant.getRefillPerSec()),
            String.valueOf(ttlFor(tenant))
        );
        return new Decision(r.get(0) == 1L, r.get(1), r.get(2));
    }

    @Override
    public void invalidate(Tenant tenant) {
        redis.delete(key(tenant));
    }

    // Keyed by tenant id, not API key: no secret sits in Redis, and rotating a key keeps the bucket.
    private String key(Tenant tenant) {
        return "quota:tenant:" + tenant.getId();
    }

    private long ttlFor(Tenant t) {
        if (t.getRefillPerSec() <= 0) {
            return TTL_FLOOR_SECONDS;
        }
        return Math.max(TTL_FLOOR_SECONDS, (long) Math.ceil(t.getCapacity() / t.getRefillPerSec()) + 10);
    }
}
