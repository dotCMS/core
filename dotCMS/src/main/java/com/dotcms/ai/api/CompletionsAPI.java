package com.dotcms.ai.api;

import com.dotcms.ai.config.AppConfig;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;

import java.io.OutputStream;

/**
 * The CompletionsAPI interface provides methods for interacting with the AI completion service.
 * It includes methods for summarizing content based on matching embeddings in dotCMS,
 * generating AI responses based on given prompts, and streaming AI responses.
 * Implementations of this interface should provide the specific logic for interacting with the AI service.
 */
public interface CompletionsAPI {

    static CompletionsAPI impl(final AppConfig config) {
        return new CompletionsAPIImpl(Lazy.of(() -> config));
    }

    static CompletionsAPI impl() {
        return new CompletionsAPIImpl(null);
    }

    /**
     * this method takes the query/prompt, searches dotCMS content for matching embeddings and then returns an AI
     * summary based on the matching content in dotCMS
     *
     * @param searcher
     * @return
     */
    JSONObject summarize(CompletionsForm searcher);

    /**
     * this method takes the query/prompt, searches dotCMS content for matching embeddings and then streams the AI
     * response based on the matching content in dotCMS
     *
     * @param searcher
     * @return
     */
    void summarizeStream(CompletionsForm searcher, OutputStream out);

    /**
     * this method takes a prompt in the form of json and returns a json AI response based upon that prompt
     *
     * @param promptJSON
     * @return
     */
    JSONObject raw(JSONObject promptJSON);

    /**
     * this method takes a prompt and returns the AI response based upon that prompt
     *
     * @param promptForm
     * @return
     */
    JSONObject raw(CompletionsForm promptForm);

    /**
     * this method takes a prompt in the form of parameters and returns a json AI response based on the parameters
     * passed in.
     *
     * @param systemPrompt
     * @param userPrompt
     * @param model
     * @param temperature
     * @param maxTokens
     * @return
     */
    JSONObject prompt(String systemPrompt, String userPrompt, String model, float temperature, int maxTokens);

    /**
     * this method takes a prompt in the form of json and returns streaming AI response based upon that prompt
     *
     * @param promptForm
     * @return
     */
    void rawStream(CompletionsForm promptForm, OutputStream out);

}
