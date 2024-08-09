package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIProvider;
import com.dotcms.ai.domain.AIRequest;
import com.dotcms.ai.domain.AIResponse;
import io.vavr.Lazy;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class AIProxy {

    private static final Lazy<AIProxy> INSTANCE = Lazy.of(AIProxy::new);

    private final ConcurrentMap<AIProvider, AIProxiedClient> proxiedClients;
    private final AtomicReference<AIProvider> currentProvider;

    private AIProxy() {
        proxiedClients = new ConcurrentHashMap<>();
        addClient(AIProvider.OPEN_AI, AIProxiedClient.of(OpenAIClient.get(), AIProxyStrategyWrapper.DEFAULT));
        currentProvider = new AtomicReference<>(AIProvider.OPEN_AI);
    }

    public static AIProxy get() {
        return INSTANCE.get();
    }

    public AIProxiedClient getClient(final AIProvider provider) {
        return proxiedClients.get(provider);
    }

    public void addClient(final AIProvider provider, final AIProxiedClient client) {
        proxiedClients.put(provider, client);
    }

    public AIResponse sendRequest(final AIProvider provider, final AIRequest request) {
        return Optional.ofNullable(proxiedClients.getOrDefault(provider, AIProxiedClient.NOOP))
                .map(client -> client.callToAI(request))
                .orElse(null);
    }

}
