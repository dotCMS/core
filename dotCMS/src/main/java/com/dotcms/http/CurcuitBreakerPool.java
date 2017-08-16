package com.dotcms.http;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.jodah.failsafe.CircuitBreaker;

public class CurcuitBreakerPool {
    
    private final static long DELAY     = Config.getLongProperty("URL_CONNECTION_CIRCUIT_BREAKER_DELEY_SEC", 30);
    private final static int FAILURES   = Config.getIntProperty("URL_CONNECTION_CIRCUIT_BREAKER_FAILURES", 5);
    private final static int SUCCESSES  = Config.getIntProperty("URL_CONNECTION_CIRCUIT_BREAKER_SUCCESSES", 3);
    
    
    private static final Cache<String, CircuitBreaker> pool =
                    CacheBuilder.newBuilder()
                    .maximumSize(Config.getLongProperty("URL_CONNECTION_CIRCUIT_POOL_SIZE", 1000))
                    .expireAfterAccess(1, TimeUnit.HOURS)
                    .build();

    @VisibleForTesting
    public static CircuitBreaker getBreaker(final String key)  {
        try {
            return pool.get(key, new Callable<CircuitBreaker>() {
                @Override
                public CircuitBreaker call() {
                    return new CircuitBreaker()
                                    .withFailureThreshold(FAILURES)
                                    .withSuccessThreshold(SUCCESSES)
                                    .withDelay(DELAY, TimeUnit.SECONDS);
                }
            });
        } catch (ExecutionException e) {
            throw new DotExecutionException(e);
        }


    }
    
    public static void flushPool(){
        pool.invalidateAll();
    }
    
    
    
}
