package com.dotcms.ai.api;

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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.knuddels.jtokkit.api.Encoding;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CompletionsAPIImpl implements CompletionsAPI {

    @Override
    public JSONObject prompt(String systemPrompt, String userPrompt, String modelIn, float temperature, int maxTokens) {


        OpenAIModel model = OpenAIModel.resolveModel(modelIn);
        final JSONObject json = new JSONObject();

        json.put("temperature", temperature);
        final List<Map> messages = new ArrayList<>();
        if (UtilMethods.isSet(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        json.put("messages", messages);
        if (maxTokens > 0) {
            json.put("max_tokens", maxTokens);
        }
        if (UtilMethods.isSet(model)) {
            json.put("model", model.modelName);
        }
        return raw(json);



    }

    final Lazy<AppConfig> config;

    final Lazy<AppConfig> defaultConfig = Lazy.of(() -> ConfigService.INSTANCE.config(Try.of(() ->
                    WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
            .getOrElse(APILocator.systemHost()))
    );


    public CompletionsAPIImpl() {
        this(null);
    }


    public CompletionsAPIImpl(Lazy<AppConfig> config) {

        this.config = (config != null)
                ? config
                : defaultConfig;
    }

    @Override
    public JSONObject summarize(CompletionsForm summaryRequest) {
        EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest).build();

        List<EmbeddingsDTO> localResults = EmbeddingsAPI.impl().getEmbeddingResults(searcher);

        // send all this as a json blob to OpenAI
        JSONObject json = buildRequestJson(summaryRequest, localResults);
        if (json.optBoolean("stream", false)) {
            throw new DotRuntimeException("Please use the summarizeStream method to stream results");
        }
        json.put("stream", false);

        String openAiResponse = Try.of(() -> OpenAIRequest.doRequest(config.get().getApiUrl(), "post", config.get().getApiKey(), json)).getOrElseThrow(DotRuntimeException::new);

        JSONObject dotCMSResponse = EmbeddingsAPI.impl().reduceChunksToContent(searcher, localResults);

        dotCMSResponse.put("openAiResponse", new JSONObject(openAiResponse));
        return dotCMSResponse;
    }

    private JSONObject buildRequestJson(CompletionsForm form, List<EmbeddingsDTO> searchResults) {


        OpenAIModel model = OpenAIModel.resolveModel(form.model);
        // aggregate matching results into text
        final StringBuilder supportingContent = new StringBuilder();
        searchResults.forEach(s -> supportingContent.append(s.extractedText).append(" "));


        final String systemPrompt = getSystemPrompt(form.prompt, supportingContent.toString());
        String textPrompt = getTextPrompt(form.prompt, supportingContent.toString());

        final int systemPromptTokens = countTokens(systemPrompt);
        textPrompt = reduceStringToTokenSize(textPrompt, model.maxTokens - form.responseLengthTokens - systemPromptTokens);


        final JSONObject json = new JSONObject();

        json.put("stream", form.stream);
        json.put("temperature", form.temperature);
        final List<Map> messages = new ArrayList<>();
        if (UtilMethods.isSet(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", textPrompt));
        json.put("messages", messages);

        if (UtilMethods.isSet(form.model)) {
            json.put("model", model.modelName);
        }

        if (form.responseLengthTokens > 0) {
            json.put("max_tokens", form.responseLengthTokens);
        }


        return json;
    }

    // TODO refactor to share logic with getTextPrompt()
    private String getSystemPrompt(String prompt, String supportingContent) {
        if (UtilMethods.isEmpty(prompt) || UtilMethods.isEmpty(supportingContent)) {
            throw new DotRuntimeException("no prompt or supporting content to summarize found");

        }
        String systemPrompt = config.get().getConfig(AppKeys.COMPLETION_ROLE_PROMPT);


        HttpServletRequest requestProxy = new FakeHttpRequest("localhost", "/").request();
        HttpServletResponse responseProxy = new BaseResponse().response();
        Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        ctx.put("prompt", prompt);
        ctx.put("supportingContent", supportingContent);

        return Try.of(() -> VelocityUtil.eval(systemPrompt, ctx)).getOrElseThrow(DotRuntimeException::new);


    }

    private String getTextPrompt(String prompt, String supportingContent) {
        if (UtilMethods.isEmpty(prompt) || UtilMethods.isEmpty(supportingContent)) {
            throw new DotRuntimeException("no prompt or supporting content to summarize found");
        }
        String textPrompt = config.get().getConfig(AppKeys.COMPLETION_TEXT_PROMPT);

        HttpServletRequest requestProxy = new FakeHttpRequest("localhost", "/").request();
        HttpServletResponse responseProxy = new BaseResponse().response();
        Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        ctx.put("prompt", prompt);
        ctx.put("supportingContent", supportingContent);

        return Try.of(() -> VelocityUtil.eval(textPrompt, ctx)).getOrElseThrow(DotRuntimeException::new);

    }

    private int countTokens(String testString) {
        return EncodingUtil.registry
                .getEncodingForModel(config.get().getConfig(AppKeys.MODEL))
                .map(enc -> enc.countTokens(testString))
                .orElseThrow(() -> new DotRuntimeException("Encoder not found"));
    }

    /***
     * Reduce prompt to fit the maxTokenSize of the model
     * @param incomingString the String to be reduced
     * @param maxTokenSize
     * @return
     */
    private String reduceStringToTokenSize(String incomingString, int maxTokenSize) {

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

    @Override
    public void summarizeStream(CompletionsForm summaryRequest, OutputStream out) {
        EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest)
                .build();

        List<EmbeddingsDTO> localResults = EmbeddingsAPI.impl().getEmbeddingResults(searcher);

        JSONObject json = buildRequestJson(summaryRequest, localResults);
        json.put("stream", true);
        OpenAIRequest.doPost(config.get().getApiUrl(), config.get().getApiKey(), json, out);

    }

    @Override
    public JSONObject raw(CompletionsForm promptForm) {
        JSONObject jsonObject = buildRequestJson(promptForm);
        return raw(jsonObject);
    }

    @Override
    public JSONObject raw(JSONObject jsonObject) {



        if(ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.info(this.getClass(), "OpenAI request:" + jsonObject.toString(2));
        }


        String response = OpenAIRequest.doRequest(config.get().getApiUrl(), "POST", config.get().getApiKey(), jsonObject);


        if(ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.info(this.getClass(), "OpenAI response:" + response);
        }
        return new JSONObject(response);
    }


    @Override
    public void rawStream(CompletionsForm promptForm, OutputStream out) {
        JSONObject jsonObject = buildRequestJson(promptForm);
        jsonObject.put("stream", true);
        OpenAIRequest.doRequest(config.get().getApiUrl(), "POST", config.get().getApiKey(), jsonObject, out);

    }

    private JSONObject buildRequestJson(CompletionsForm form) {


        int maxTokenSize = OpenAIModel.resolveModel(config.get().getConfig(AppKeys.MODEL)).maxTokens;


        int promptTokens = countTokens(form.prompt);

        JSONArray messages = new JSONArray();
        String textPrompt = reduceStringToTokenSize(form.prompt, maxTokenSize - form.responseLengthTokens - promptTokens);

        messages.add(Map.of("role", "user", "content", textPrompt));

        JSONObject json = new JSONObject();
        json.put("messages", messages);
        json.putIfAbsent("model", config.get().getConfig(AppKeys.MODEL));

        json.put("temperature", form.temperature);
        if (form.responseLengthTokens > 0) {
            json.put("max_tokens", form.responseLengthTokens);
        }
        json.put("stream", form.stream);

        return json;
    }


}
