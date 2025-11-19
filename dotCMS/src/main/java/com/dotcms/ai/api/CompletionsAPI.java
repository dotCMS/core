package com.dotcms.ai.api;

import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.util.json.JSONObject;

import java.io.OutputStream;

/**
 * The CompletionsAPI interface provides methods for interacting with the AI completion service.
 * It includes methods for summarizing content based on matching embeddings in dotCMS,
 * generating AI responses based on given prompts, and streaming AI responses.
 * Implementations of this interface should provide the specific logic for interacting with the AI service.
 */
public interface CompletionsAPI {

    /**
     * this method takes the query/prompt, searches dotCMS content for matching embeddings and then returns an AI
     * summary based on the matching content in dotCMS
     *
     * @param searcher
     * @return JSONObject
     */
    JSONObject summarize(CompletionsForm searcher);

    /**
     * this method takes the query/prompt, searches dotCMS content for matching embeddings and then returns an AI
     * summary based on the matching content in dotCMS
     *
     * @param summarizeRequest
     * @return JSONObject
     */
    JSONObject summarize(SummarizeRequest summarizeRequest);

    /**
     * this method takes the query/prompt, searches dotCMS content for matching embeddings and returns the summary
     * based on the matching content in dotCMS, the response is served in streaming.
     *
     * @param summarizeRequest
     * @param out
     */
    void summarize(SummarizeRequest summarizeRequest, OutputStream out);

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
     * @param userId
     * @return
     */
    JSONObject raw(JSONObject promptJSON, String userId);

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
     * @param userId
     * @return
     */
    JSONObject prompt(String systemPrompt,
                      String userPrompt,
                      String model,
                      float temperature,
                      int maxTokens,
                      String userId);

    /**
     * this method takes a prompt in the form of json and returns streaming AI response based upon that prompt
     *
     * @param promptForm
     * @return
     */
    void rawStream(CompletionsForm promptForm, OutputStream out);

    /**
     * this method takes a prompt in the request and returns a json AI response based on the parameters
     * passed in.
     * @param completionRequest
     * @return
     */
    CompletionResponse raw(CompletionRequest completionRequest);
}
