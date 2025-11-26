package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIResponseData;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;

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
    public AIResponseData applyStrategy(final AIClient client,
                                        final AIResponseEvaluator handler,
                                        final AIRequest<? extends Serializable> request,
                                        final OutputStream incoming) {
        final JSONObjectAIRequest jsonRequest = AIClient.useRequestOrThrow(request);
        final boolean isStream = AIClientStrategy.isStream(jsonRequest);
        final OutputStream output = Optional.ofNullable(incoming).orElseGet(ByteArrayOutputStream::new);

        client.sendRequest(jsonRequest, output);

        return AIClientStrategy.response(output, isStream);
    }

}
