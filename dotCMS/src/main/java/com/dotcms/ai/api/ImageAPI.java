package com.dotcms.ai.api;

import com.dotcms.ai.model.AIImageRequestDTO;
import com.dotmarketing.util.json.JSONObject;

/**
 * Service to interact with the OpenAI Image API
 */
public interface ImageAPI {

    /**
     * Sends a text prompt to the OpenAI API.
     *
     * @param prompt the text prompt to send
     * @return a JSONObject with the response from the API
     */
    JSONObject sendTextPrompt(String prompt);

    /**
     * Sends a raw request to the OpenAI API.
     *
     * @param prompt the raw request to send
     * @return a JSONObject with the response from the API
     */
    JSONObject sendRawRequest(String prompt);

    /**
     * Sends a {@link JSONObject} request to the OpenAI API.
     *
     * @param jsonObject json object to send
     * @return a JSONObject with the response from the API
     */
    JSONObject sendRequest(JSONObject jsonObject);

    /**
     * Sends a request to the OpenAI API.
     *
     * @param dto the request to send
     * @return a JSONObject with the response from the API
     */
    JSONObject sendRequest(AIImageRequestDTO dto);

}
