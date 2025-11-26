package com.dotcms.ai.api;

import com.dotmarketing.util.json.JSONObject;

public interface ChatAPI {

    /**
     * Returns a JSONObject with the results of the text generation given the provided prompt
     * @param prompt a String representing the provided prompt to generate text
     * @return a JSONObject including the generated text and metadata
     */
    JSONObject sendTextPrompt(String prompt);

    /**
     * Returns a JSONObject with the results of the text generation given the provided prompt
     * @param prompt the provided prompt, as JSON, to generate text. The available properties
     *               for the JSON are:
     * <ul>
     * <li>{@code "prompt"}: the actual prompt text
     * <li>{@code "model"}: the model used for the generation
     * <li>{@code "temperature"}: determines the randomness of the response. 0 = deterministic, 2 = most random
     * </ul>
     * @return a JSONObject including the generated text and metadata
     * <p>
     * Example of usage:
     * <p>
     * <pre>{@code
     *  JSONObject prompt = new JSONObject();
     *  prompt.put("model","gpt-3.5-turbo-16k");
     *  prompt.put("temperature",1);
     *  prompt.put("prompt","Short text about dotCMS");
     *  sendRawRequest(prompt);
     * }</pre>
     *
     */
    JSONObject sendRawRequest(JSONObject prompt);

}
