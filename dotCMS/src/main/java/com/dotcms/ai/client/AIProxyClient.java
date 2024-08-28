package com.dotcms.ai.client;

import com.dotcms.ai.client.openai.OpenAIClient;
import com.dotcms.ai.client.openai.OpenAIResponseEvaluator;
import com.dotcms.ai.domain.AIProvider;
import com.dotcms.ai.domain.AIResponse;
import io.vavr.Lazy;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A proxy client for managing and interacting with multiple AI service providers.
 *
 * <p>
 * This class provides a mechanism to send requests to various AI service providers through proxied clients,
 * applying different strategies for handling the requests and responses. It supports adding new clients and
 * switching between different AI providers.
 * </p>
 *
 * <p>
 * The class allows for flexible handling of AI service interactions by maintaining a map of proxied clients
 * and providing methods to send requests to the current or specified provider.
 * </p>
 *
 * @author vico
 */
public class AIProxyClient {

    private static final Lazy<AIProxyClient> INSTANCE = Lazy.of(AIProxyClient::new);

    private final ConcurrentMap<String, AIProxiedClient> proxiedClients;
    private final AtomicReference<AIProvider> currentProvider;

    private AIProxyClient() {
        proxiedClients = new ConcurrentHashMap<>();
        addClient(
                AIProvider.OPEN_AI.name(),
                AIProxiedClient.of(OpenAIClient.get(), AIProxyStrategy.MODEL_FALLBACK, OpenAIResponseEvaluator.get()));
        currentProvider = new AtomicReference<>(AIProvider.OPEN_AI);
    }

    public static AIProxyClient get() {
        return INSTANCE.get();
    }

    /**
     * Adds a proxied client for the specified AI provider.
     *
     * @param provider the AI provider for which the client is added
     * @param client the proxied client to be added
     */
    public void addClient(final String provider, final AIProxiedClient client) {
        proxiedClients.put(provider, client);
    }

    /**
     * Sends the given AI request to the specified AI provider and writes the response to the provided output stream.
     *
     * @param provider the AI provider to which the request is sent
     * @param request the AI request to be sent
     * @param output the output stream to which the response will be written
     * @return the AI response
     */
    public AIResponse callToAI(final String provider,
                               final AIRequest<? extends Serializable> request,
                               final OutputStream output) {
        return Optional.ofNullable(proxiedClients.getOrDefault(provider, AIProxiedClient.NOOP))
                .map(client -> client.sendToAI(request, output))
                .orElse(AIResponse.EMPTY);
    }

    /**
     * Sends the given AI request to the specified AI provider.
     *
     * @param <T> the type of the request payload
     * @param provider the AI provider to which the request is sent
     * @param request the AI request to be sent
     * @return the AI response
     */
    public <T extends Serializable> AIResponse callToAI(final String provider, final AIRequest<T> request) {
        return callToAI(provider, request, null);
    }

    /**
     * Sends the given AI request to the current AI provider and writes the response to the provided output stream.
     *
     * @param <T> the type of the request payload
     * @param request the AI request to be sent
     * @param output the output stream to which the response will be written
     * @return the AI response
     */
    public <T extends Serializable> AIResponse callToAI(final AIRequest<T> request, final OutputStream output) {
        return callToAI(currentProvider.get().name(), request, output);
    }

    /**
     * Sends the given AI request to the current AI provider.
     *
     * @param <T> the type of the request payload
     * @param request the AI request to be sent
     * @return the AI response
     */
    public <T extends Serializable> AIResponse callToAI(final AIRequest<T> request) {
        return callToAI(request, null);
    }

}
