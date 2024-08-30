package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIResponse;

import java.io.OutputStream;
import java.io.Serializable;

/**
 * Interface representing a strategy for handling AI client requests and responses.
 *
 * <p>
 * This interface defines a method for applying a strategy to an AI client request,
 * allowing for different handling mechanisms to be implemented. The NOOP strategy
 * is provided as a default implementation that performs no operations.
 * </p>
 *
 * <p>
 * Implementations of this interface should define how to process the AI request
 * and handle the response, potentially writing the response to an output stream.
 * </p>
 *
 * @author vico
 */
public interface AIClientStrategy {

    AIClientStrategy NOOP = (client, handler, request, output) -> AIResponse.builder().build();

    /**
     * Applies the strategy to the given AI client request and handles the response.
     *
     * @param client the AI client to which the request is sent
     * @param handler the response evaluator to handle the response
     * @param request the AI request to be processed
     * @param output the output stream to which the response will be written
     */
    void applyStrategy(AIClient client,
                       AIResponseEvaluator handler,
                       AIRequest<? extends Serializable> request,
                       OutputStream output);

}
