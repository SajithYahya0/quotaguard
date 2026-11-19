package com.sajith.quotaguard.limiter;

import com.sajith.quotaguard.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "quotaguard.limiter=redis")
class RedisRateLimiterTest {

    @Autowired RateLimiter limiter;

    @Test
    void concurrentConsume_neverOverAdmits() throws Exception {
        Tenant t = new Tenant();
        t.setApiKey(UUID.randomUUID().toString());
        t.setCapacity(100);
        t.setRefillPerSec(0);

        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();

        for (int i = 0; i < 500; i++) {
            pool.submit(() -> {
                start.await();
                if (limiter.tryConsume(t).allowed()) admitted.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        assertEquals(100, admitted.get());
    }
}