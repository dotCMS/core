package com.dotcms.ai.client;

public enum AIProxyStrategyWrapper {

    DEFAULT(new AIDefaultStrategy()),
    MODEL_FALLBACK(new AIModelFallbackStrategy());

    private final AIProxyStrategy strategy;

    AIProxyStrategyWrapper(final AIProxyStrategy strategy) {
        this.strategy = strategy;
    }

    public AIProxyStrategy getStrategy() {
        return strategy;
    }

}
