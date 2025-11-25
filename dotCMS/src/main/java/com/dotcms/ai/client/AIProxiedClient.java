package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIResponse;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;

/**
 * A proxy client for interacting with an AI service using a specified strategy.
 *
 * <p>
 * This class provides a mechanism to send requests to an AI service through a proxied client,
 * applying a given strategy for handling the requests and responses. It supports a NOOP implementation
 * that performs no operations.
 * </p>
 *
 * <p>
 * The class allows for the creation of proxied clients with different strategies and response evaluators,
 * enabling flexible handling of AI service interactions.
 * </p>
 *
 * @author vico
 */
public class AIProxiedClient {

    public static final AIProxiedClient NOOP = new AIProxiedClient(null, AIClientStrategy.NOOP, null);

    private final AIClient client;
    private final AIClientStrategy strategy;
    private final AIResponseEvaluator responseEvaluator;

    private AIProxiedClient(final AIClient client,
                            final AIClientStrategy strategy,
                            final AIResponseEvaluator responseEvaluator) {
        this.client = client;
        this.strategy = strategy;
        this.responseEvaluator = responseEvaluator;
    }

    /**
     * Creates an AIProxiedClient with the specified client, strategy, and response evaluator.
     *
     * @param client the AI client to be proxied
     * @param strategy the strategy to be applied for handling requests and responses
     * @param responseParser the response evaluator to process responses
     * @return a new instance of AIProxiedClient
     */
    public static AIProxiedClient of(final AIClient client,
                                     final AIProxyStrategy strategy,
                                     final AIResponseEvaluator responseParser) {
        return new AIProxiedClient(client, strategy.getStrategy(), responseParser);
    }

    /**
     * Creates an AIProxiedClient with the specified client and strategy.
     *
     * @param client the AI client to be proxied
     * @param strategy the strategy to be applied for handling requests and responses
     * @return a new instance of AIProxiedClient
     */
    public static AIProxiedClient of(final AIClient client, final AIProxyStrategy strategy) {
        return of(client, strategy, null);
    }

    /**
     * Sends the given AI request to the AI service and writes the response to the provided output stream.
     *
     * @param <T> the type of the request payload
     * @param request the AI request to be sent
     * @param output the output stream to which the response will be written
     * @return the AI response
     */
    public <T extends Serializable> AIResponse sendToAI(final AIRequest<T> request, final OutputStream output) {
        final OutputStream finalOutput = Optional.ofNullable(output).orElseGet(ByteArrayOutputStream::new);

        strategy.applyStrategy(client, responseEvaluator, request, finalOutput);

        return Optional.ofNullable(output)
                .map(out -> AIResponse.EMPTY)
                .orElseGet(() -> AIResponse.builder().withResponse(finalOutput.toString()).build());
    }

}
