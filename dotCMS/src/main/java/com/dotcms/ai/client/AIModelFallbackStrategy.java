package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIRequest;
import com.dotcms.ai.domain.AIResponse;

import java.io.Serializable;

public class AIModelFallbackStrategy implements AIProxyStrategy {

    @Override
    public AIResponse applyStrategy(final AIClient client, final AIRequest<? extends Serializable> request) {
        return null;
    }

}
