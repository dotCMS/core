package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIRequest;
import com.dotcms.ai.domain.AIResponse;

import java.io.Serializable;

public class AIProxiedClient {

    public static final AIProxiedClient NOOP = new AIProxiedClient(null, AIProxyStrategy.NOOP);

    private final AIClient client;
    private final AIProxyStrategy strategy;

    private AIProxiedClient(final AIClient client, final AIProxyStrategy strategy) {
        this.client = client;
        this.strategy = strategy;
    }

    public static AIProxiedClient of(final AIClient client, final AIProxyStrategyWrapper strategy) {
        return new AIProxiedClient(client, strategy.getStrategy());
    }

    public AIResponse callToAI(final AIRequest<? extends Serializable> request) {
        return strategy.applyStrategy(client, request);
    }

}
