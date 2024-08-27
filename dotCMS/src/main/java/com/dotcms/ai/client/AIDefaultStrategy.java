package com.dotcms.ai.client;

import java.io.OutputStream;
import java.io.Serializable;

/**
 * Default implementation of the {@link AIClientStrategy} interface.
 *
 * <p>
 * This class provides a default strategy for handling AI client requests by
 * directly sending the request using the provided AI client and writing the
 * response to the given output stream.
 * </p>
 *
 * <p>
 * The default strategy does not perform any additional processing or handling
 * of the request or response, delegating the entire operation to the AI client.
 * </p>
 *
 * @author vico
 */
public class AIDefaultStrategy implements AIClientStrategy {

    @Override
    public void applyStrategy(final AIClient client,
                              final AIResponseEvaluator handler,
                              final AIRequest<? extends Serializable> request,
                              final OutputStream output) {
        client.sendRequest(request, output);
    }

}
