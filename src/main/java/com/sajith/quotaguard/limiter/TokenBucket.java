package com.sajith.quotaguard.limiter;

public class TokenBucket {
    private final int capacity;
    private final double refillPerSec;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(int capacity,double refillPerSec){
        this.capacity=capacity;this.refillPerSec=refillPerSec;
        this.tokens=capacity;this.lastRefillNanos=System.nanoTime();;
    }




    public synchronized boolean tryConsume(){
        refill();
        if(tokens>=1){
            tokens-=1;return true;
        }return false;
    }

    private void refill(){
        long now = System.nanoTime();
        double elapsedSec = (now-lastRefillNanos)/1_000_000_000.0;
        tokens=Math.min(capacity,tokens+elapsedSec*refillPerSec);
        lastRefillNanos=now;
    }






}
