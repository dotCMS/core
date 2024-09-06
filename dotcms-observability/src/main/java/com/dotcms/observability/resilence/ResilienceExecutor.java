package com.dotcms.observability.resilence;

import java.util.concurrent.Callable;

public class ResilienceExecutor {

    private CircuitBreaker circuitBreaker;
    private RetryConfig retryConfig;

    public ResilienceExecutor(CircuitBreaker circuitBreaker, RetryConfig retryConfig) {
        this.circuitBreaker = circuitBreaker;
        this.retryConfig = retryConfig;
    }

    public <T> T execute(Callable<T> task, Callable<T> fallback) throws Exception {
        // implementation
        return task.call();
    }

    private <T> T executeWithRetry(Callable<T> task) throws Exception {
        // implementation
        return task.call();
    }

}
