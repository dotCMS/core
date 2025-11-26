package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIResponseData;

/**
 * Interface for evaluating AI responses.
 * It provides methods to process responses and exceptions, updating the provided metadata.
 *
 * <p>Methods:</p>
 * <ul>
 *   <li>\fromResponse\ - Processes a response string and updates the metadata.</li>
 *   <li>\fromThrowable\ - Processes an exception and updates the metadata.</li>
 * </ul>
 *
 * @author vico
 */
public interface AIResponseEvaluator {

    /**
     * Processes a response string and updates the metadata.
     *
     * @param response the response string to process
     * @param metadata the metadata to update based on the response
     * @param jsonExpected flag for expecting the response to be a JSON
     */
    void fromResponse(String response, AIResponseData metadata, boolean jsonExpected);

    /**
     * Processes an exception and updates the metadata.
     *
     * @param exception the exception to process
     * @param metadata the metadata to update based on the exception
     */
    void fromException(Throwable exception, AIResponseData metadata);

}
