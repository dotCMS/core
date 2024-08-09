package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIRequest;
import com.dotcms.ai.domain.AIResponse;

import java.io.OutputStream;
import java.io.Serializable;

public class AIDefaultStrategy implements AIProxyStrategy {

    @Override
    public AIResponse applyStrategy(final AIClient client, final AIRequest<? extends Serializable> request) {
        final OutputStream output = client.sendRequest(request);
        return AIResponse.builder()
                .output(output)
                .response(output.toString())
                .build();
    }

}
