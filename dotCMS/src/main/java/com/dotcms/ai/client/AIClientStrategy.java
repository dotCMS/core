package com.dotcms.ai.client;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.domain.AIResponse;
import com.dotcms.ai.domain.AIResponseData;

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

    AIClientStrategy NOOP = (client, handler, request, output) -> {
        AIResponse.builder().build();
        return null;
    };

    /**
     * Applies the strategy to the given AI client request and handles the response.
     *
     * @param client the AI client to which the request is sent
     * @param handler the response evaluator to handle the response
     * @param request the AI request to be processed
     * @param incoming the output stream to which the response will be written
     * @return response data object
     */
    AIResponseData applyStrategy(AIClient client,
                                 AIResponseEvaluator handler,
                                 AIRequest<? extends Serializable> request,
                                 OutputStream incoming);

    /**
     * Converts the given output stream to an AIResponseData object.
     *
     * <p>
     * This method takes an output stream, converts its content to a string, and
     * sets it as the response in an AIResponseData object. The output stream is
     * also set in the AIResponseData object.
     * </p>
     *
     * @param output   the output stream containing the response data
     * @param isStream is stream flag
     * @return an AIResponseData object containing the response and the output stream
     */
    static AIResponseData response(final OutputStream output, boolean isStream) {
        final AIResponseData responseData = new AIResponseData();
        if (!isStream) {
            responseData.setResponse(output.toString());
        }
        responseData.setOutput(output);

        return responseData;
    }

    /**
     * Checks if the given request is a stream request.
     *
     * <p>
     * This method examines the payload of the provided `JSONObjectAIRequest` to determine
     * if it contains a stream flag set to true. If the stream flag is present and set to true,
     * the method returns true, indicating that the request is a stream request.
     * </p>
     *
     * @param request the `JSONObjectAIRequest` to be checked
     * @return true if the request is a stream request, false otherwise
     */
    static boolean isStream(final JSONObjectAIRequest request) {
        return request.getPayload().optBoolean(AiKeys.STREAM, false);
    }

}
