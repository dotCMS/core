package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIRequest;
import com.dotcms.ai.domain.AIResponse;

import java.io.Serializable;

public interface AIProxyStrategy {

    AIProxyStrategy NOOP = (client, request) -> AIResponse.builder().build();

    AIResponse applyStrategy(final AIClient client, final AIRequest<? extends Serializable> request);

}
