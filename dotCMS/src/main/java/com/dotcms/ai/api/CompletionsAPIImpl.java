package com.dotcms.ai.api;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIModel;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class implements the CompletionsAPI interface and provides the specific logic for interacting with the AI service.
 * It includes methods for summarizing content based on matching embeddings in dotCMS, generating AI responses based on given prompts, and streaming AI responses.
 * It also provides methods for building request JSON for the AI service and reducing string size to fit the max token size of the model.
 */
public class CompletionsAPIImpl implements CompletionsAPI {

    private final Lazy<AppConfig> config;

    final Lazy<AppConfig> defaultConfig =
            Lazy.of(() -> ConfigService.INSTANCE.config(
                    Try.of(() -> WebAPILocator
                                    .getHostWebAPI()
                                    .getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                            .getOrElse(APILocator.systemHost()))
    );

    public CompletionsAPIImpl(final Lazy<AppConfig> config) {
        this.config = (config != null) ? config : defaultConfig;
    }

    @Override
    public JSONObject prompt(final String systemPrompt,
                             final String userPrompt,
                             final String modelIn,
                             final float temperature,
                             final int maxTokens) {
        final OpenAIModel model = OpenAIModel.resolveModel(modelIn);
        final JSONObject json = new JSONObject();

        json.put(AiKeys.TEMPERATURE, temperature);
        buildMessages(systemPrompt, userPrompt, json);

        if (maxTokens > 0) {
            json.put(AiKeys.MAX_TOKENS, maxTokens);
        }

        json.put(AiKeys.MODEL, model.modelName);

        return raw(json);
    }

    @Override
    public JSONObject summarize(final CompletionsForm summaryRequest) {
        final EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest).build();
        final List<EmbeddingsDTO> localResults = EmbeddingsAPI.impl().getEmbeddingResults(searcher);

        // send all this as a json blob to OpenAI
        final JSONObject json = buildRequestJson(summaryRequest, localResults);
        if (json.optBoolean(AiKeys.STREAM, false)) {
            throw new DotRuntimeException("Please use the summarizeStream method to stream results");
        }

        json.put(AiKeys.STREAM, false);
        final String openAiResponse =
                Try.of(() -> OpenAIRequest.doRequest(
                        config.get().getApiUrl(),
                        HttpMethod.POST,
                        config.get().getApiKey(),
                        json))
                .getOrElseThrow(DotRuntimeException::new);
        final JSONObject dotCMSResponse = EmbeddingsAPI.impl().reduceChunksToContent(searcher, localResults);

        dotCMSResponse.put(AiKeys.OPEN_AI_RESPONSE, new JSONObject(openAiResponse));
        return dotCMSResponse;
    }

    @Override
    public void summarizeStream(final CompletionsForm summaryRequest, final OutputStream out) {
        final EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest).build();
        final List<EmbeddingsDTO> localResults = EmbeddingsAPI.impl().getEmbeddingResults(searcher);

        final JSONObject json = buildRequestJson(summaryRequest, localResults);
        json.put(AiKeys.STREAM, true);
        OpenAIRequest.doPost(config.get().getApiUrl(), config.get().getApiKey(), json, out);
    }

    @Override
    public JSONObject raw(final JSONObject jsonObject) {
        if (config.get().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.info(this.getClass(), "OpenAI request:" + jsonObject.toString(2));
        }

        final String response = OpenAIRequest.doRequest(
                config.get().getApiUrl(),
                HttpMethod.POST,
                config.get().getApiKey(),
                jsonObject);
        if (config.get().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.info(this.getClass(), "OpenAI response:" + response);
        }

        return new JSONObject(response);
    }

    @Override
    public JSONObject raw(CompletionsForm promptForm) {
        JSONObject jsonObject = buildRequestJson(promptForm);
        return raw(jsonObject);
    }

    @Override
    public void rawStream(final CompletionsForm promptForm, final OutputStream out) {
        final JSONObject jsonObject = buildRequestJson(promptForm);
        jsonObject.put(AiKeys.STREAM, true);
        OpenAIRequest.doRequest(config.get().getApiUrl(), HttpMethod.POST, config.get().getApiKey(), jsonObject, out);
    }

    private void buildMessages(final String systemPrompt, final String userPrompt, final JSONObject json) {
        final List<Map<String, Object>> messages = new ArrayList<>();
        if (UtilMethods.isSet(systemPrompt)) {
            messages.add(Map.of(AiKeys.ROLE, AiKeys.SYSTEM, AiKeys.CONTENT, systemPrompt));
        }
        messages.add(Map.of(AiKeys.ROLE, AiKeys.USER, AiKeys.CONTENT, userPrompt));
        json.put(AiKeys.MESSAGES, messages);
    }

    private JSONObject buildRequestJson(final CompletionsForm form, final List<EmbeddingsDTO> searchResults) {
        final OpenAIModel model = OpenAIModel.resolveModel(form.model);
        // aggregate matching results into text
        final StringBuilder supportingContent = new StringBuilder();
        searchResults.forEach(s -> supportingContent.append(s.extractedText).append(" "));

        final String systemPrompt = getSystemPrompt(form.prompt, supportingContent.toString());
        String textPrompt = getTextPrompt(form.prompt, supportingContent.toString());

        final int systemPromptTokens = countTokens(systemPrompt);
        textPrompt = reduceStringToTokenSize(
                textPrompt,
                model.maxTokens - form.responseLengthTokens - systemPromptTokens);

        final JSONObject json = new JSONObject();
        json.put(AiKeys.STREAM, form.stream);
        json.put(AiKeys.TEMPERATURE, form.temperature);

        buildMessages(systemPrompt, textPrompt, json);

        if (UtilMethods.isSet(form.model)) {
            json.put(AiKeys.MODEL, model.modelName);
        }

        json.put(AiKeys.MAX_TOKENS, form.responseLengthTokens);

        return json;
    }

    private String getPrompt(final String prompt, final String supportingContent, final AppKeys key) {
        if (UtilMethods.isEmpty(prompt) || UtilMethods.isEmpty(supportingContent)) {
            throw new DotRuntimeException("no prompt or supporting content to summarize found");
        }

        final String resolvedPrompt = config.get().getConfig(key);
        final HttpServletRequest requestProxy = new FakeHttpRequest("localhost", "/").request();
        final HttpServletResponse responseProxy = new BaseResponse().response();

        final Context ctx = VelocityUtil.getInstance().getContext(requestProxy, responseProxy);
        ctx.put(AiKeys.PROMPT, prompt);
        ctx.put(AiKeys.SUPPORTING_CONTENT, supportingContent);

        return Try.of(() -> VelocityUtil.eval(resolvedPrompt, ctx)).getOrElseThrow(DotRuntimeException::new);
    }

    private String getSystemPrompt(final String prompt, final String supportingContent) {
        return getPrompt(prompt, supportingContent, AppKeys.COMPLETION_ROLE_PROMPT);
    }

    private String getTextPrompt(final String prompt, final String supportingContent) {
        return getPrompt(prompt, supportingContent, AppKeys.COMPLETION_TEXT_PROMPT);
    }

    private int countTokens(final String testString) {
        return EncodingUtil.registry
                .getEncodingForModel(config.get().getConfig(AppKeys.MODEL))
                .map(enc -> enc.countTokens(testString))
                .orElseThrow(() -> new DotRuntimeException("Encoder not found"));
    }

    /***
     * Reduce prompt to fit the maxTokenSize of the model
     * @param incomingString the String to be reduced
     * @param maxTokenSize the max token size
     * @return the reduced string
     */
    private String reduceStringToTokenSize(final String incomingString, final int maxTokenSize) {
        if (maxTokenSize <= 0) {
            throw new DotRuntimeException("maxToken size must be greater than 0");
        }

        int tokenCount = countTokens(incomingString);
        if (tokenCount <= maxTokenSize) {
            return incomingString;
        }

        String[] wordsToKeep = incomingString.trim().split("\\s+");
        String textToKeep = null;
        for (int i = 0; i < 10000; i++) {
            // decrease by 10%
            int toRemove = Math.round(wordsToKeep.length * .1f);

            wordsToKeep = ArrayUtils.subarray(wordsToKeep, 0, wordsToKeep.length - toRemove);
            textToKeep = String.join(" ", wordsToKeep);
            tokenCount = countTokens(textToKeep);
            if (tokenCount < maxTokenSize) {
                break;
            }
        }

        return textToKeep;
    }

    private JSONObject buildRequestJson(final CompletionsForm form) {
        final int maxTokenSize = OpenAIModel.resolveModel(config.get().getConfig(AppKeys.MODEL)).maxTokens;
        final int promptTokens = countTokens(form.prompt);

        final JSONArray messages = new JSONArray();
        final String textPrompt = reduceStringToTokenSize(
                form.prompt,
                maxTokenSize - form.responseLengthTokens - promptTokens);

        messages.add(Map.of(AiKeys.ROLE, AiKeys.USER, AiKeys.CONTENT, textPrompt));

        final JSONObject json = new JSONObject();
        json.put(AiKeys.MESSAGES, messages);
        json.putIfAbsent(AiKeys.MODEL, config.get().getConfig(AppKeys.MODEL));

        json.put(AiKeys.TEMPERATURE, form.temperature);
        json.put(AiKeys.MAX_TOKENS, form.responseLengthTokens);
        json.put(AiKeys.STREAM, form.stream);

        return json;
    }

}
