package com.sajith.quotaguard.limiter;

/** The limiter couldn't decide (e.g. Redis is down). The interceptor chooses fail-open or fail-closed. */
public class LimiterUnavailableException extends RuntimeException {

    public LimiterUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
