package com.sajith.quotaguard.limiter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TokenBucketTest {
    @Test
    void allowsUpToCapacity_thenRejects(){
        TokenBucket bucket = new TokenBucket(3,0);
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume());
    }

    @Test
    void concurrentConsume_neverOverAdmits() throws Exception{
        TokenBucket bucket = new TokenBucket(100,0);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger admitted = new AtomicInteger();

        for(int i=0;i<500;i++)
        {
            pool.submit(()->{
                start.await();
                if(bucket.tryConsume()){
                    admitted.incrementAndGet();
                }return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(100,admitted.get());

    }

}
