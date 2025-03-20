package com.dotcms.http;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.jodah.failsafe.CircuitBreaker;

public class CircuitBreakerPool {
    

    final static int FAIL_AFTER = Config.getIntProperty("CIRCUIT_BREAKER_URL_MAX_FAILURES", 5);
    final static int TRY_AGAIN_ATTEMPTS = Config.getIntProperty("CIRCUIT_BREAKER_URL_TRY_AGAIN_ATTEMPTS", 3);
    final static int TRY_AGAIN_DELAY_SEC = Config.getIntProperty("CIRCUIT_BREAKER_URL_TRY_AGAIN_DELEY_SEC", 10);
    
    private static final Cache<String, CircuitBreaker> pool =
                    CacheBuilder.newBuilder()
                    .maximumSize(Config.getLongProperty("URL_CONNECTION_CIRCUIT_POOL_SIZE", 1000))
                    .expireAfterAccess(1, TimeUnit.HOURS)
                    .build();

    @VisibleForTesting
    public static CircuitBreaker getBreaker(final String key)  {
        try {
            return pool.get(key, () -> new CircuitBreaker()
                            .withFailureThreshold(FAIL_AFTER)
                            .withSuccessThreshold(TRY_AGAIN_ATTEMPTS)
                            .withDelay(TRY_AGAIN_DELAY_SEC, TimeUnit.SECONDS));
        } catch (ExecutionException e) {
            throw new DotExecutionException(e);
        }

    }
    @VisibleForTesting
    public static CircuitBreaker getBreaker(final String key, int failAfter, int tryAgainAfter, int workAgainAfter)  {
        try {
            return pool.get(key, () -> new CircuitBreaker()
                            .withFailureThreshold(failAfter)
                            .withSuccessThreshold(tryAgainAfter)
                            .withDelay(workAgainAfter, TimeUnit.SECONDS));
        } catch (ExecutionException e) {
            throw new DotExecutionException(e);
        }

    }
    @VisibleForTesting
    public static void putBreaker(final String key, final CircuitBreaker breaker)  {
        pool.put(key, breaker);
    }
    
    public static void flushPool(){
        pool.invalidateAll();
    }
    
    
    
}
