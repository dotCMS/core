package com.dotcms.ai.api;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.api.provider.VendorModelProviderFactory;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.client.AIProxyClient;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.db.EmbeddingsDTO;
import com.dotcms.ai.domain.AIResponse;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotcms.ai.util.AIUtil;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class implements the CompletionsAPI interface and provides the specific logic for interacting with the AI service.
 * It includes methods for summarizing content based on matching embeddings in dotCMS, generating AI responses based on given prompts, and streaming AI responses.
 * It also provides methods for building request JSON for the AI service and reducing string size to fit the max token size of the model.
 */
public class CompletionsAPIImpl implements CompletionsAPI {

    private static String DEFAULT_AI_MAX_NUMBER_OF_TOKENS = "AI_DEFAULT_MAX_NUMBER_OF_TOKENS";
    public static final Lazy<Integer> DEFAULT_AI_MAX_NUMBER_OF_TOKENS_VALUE =
            Lazy.of(() -> Config.getIntProperty(DEFAULT_AI_MAX_NUMBER_OF_TOKENS, 16384));

    private final AppConfig config;
    private final VendorModelProviderFactory modelProviderFactory;

    public CompletionsAPIImpl(final AppConfig config) {
        this(config, CDIUtils.getBeanThrows(VendorModelProviderFactory.class));
    }

    public CompletionsAPIImpl(final AppConfig config,
                              final VendorModelProviderFactory modelProviderFactory) {
        final Lazy<AppConfig> defaultConfig = Lazy.of(() -> ConfigService.INSTANCE.config(
                Try.of(() -> WebAPILocator
                                .getHostWebAPI()
                                .getCurrentHostNoThrow(HttpServletRequestThreadLocal.INSTANCE.getRequest()))
                        .getOrElse(APILocator.systemHost())));
        this.config = Optional.ofNullable(config).orElse(defaultConfig.get());
        this.modelProviderFactory = modelProviderFactory;
    }

    @Override
    public JSONObject prompt(final String systemPrompt,
                             final String userPrompt,
                             final String modelIn,
                             final float temperature,
                             final int maxTokens,
                             final String userId) {
        final Model model = config.resolveModelOrThrow(modelIn, AIModelType.TEXT)._2;
        final JSONObject json = new JSONObject();

        json.put(AiKeys.TEMPERATURE, temperature);
        buildMessages(systemPrompt, userPrompt, json);

        if (maxTokens > 0) {
            json.put(AiKeys.MAX_TOKENS, maxTokens);
        }

        json.put(AiKeys.MODEL, model.getName());

        return raw(json, userId);
    }

    @Override
    public JSONObject summarize(final CompletionsForm summaryRequest) {
        final EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest).build();
        final List<EmbeddingsDTO> localResults = APILocator.getDotAIAPI()
                .getEmbeddingsAPI()
                .getEmbeddingResults(searcher);

        // send all this as a json blob to OpenAI
        final JSONObject json = buildRequestJson(summaryRequest, localResults);
        if (json.optBoolean(AiKeys.STREAM, false)) {
            throw new DotRuntimeException("Please use the summarizeStream method to stream results");
        }

        json.put(AiKeys.STREAM, false);
        final String openAiResponse = Try
                .of(() -> sendRequest(config, json, UtilMethods.extractUserIdOrNull(summaryRequest.user)))
                .getOrElseThrow(DotRuntimeException::new)
                .getResponse();
        final JSONObject dotCMSResponse = APILocator.getDotAIAPI()
                .getEmbeddingsAPI()
                .reduceChunksToContent(searcher, localResults);
        dotCMSResponse.put(AiKeys.OPEN_AI_RESPONSE, new JSONObject(openAiResponse));

        return dotCMSResponse;
    }

    @Override
    public void summarizeStream(final CompletionsForm summaryRequest, final OutputStream output) {
        final EmbeddingsDTO searcher = EmbeddingsDTO.from(summaryRequest).build();
        final List<EmbeddingsDTO> localResults = APILocator.getDotAIAPI()
                .getEmbeddingsAPI()
                .getEmbeddingResults(searcher);

        final JSONObject json = buildRequestJson(summaryRequest, localResults);
        json.put(AiKeys.STREAM, true);
        AIProxyClient.get().callToAI(
                JSONObjectAIRequest.quickText(
                        config,
                        json,
                        UtilMethods.extractUserIdOrNull(summaryRequest.user)),
                output);
    }

    @Override
    public JSONObject raw(final JSONObject json, final String userId) {
        config.debugLogger(this.getClass(), () -> "OpenAI request:" + json.toString(2));

        final String response = sendRequest(config, json, userId).getResponse();
        config.debugLogger(this.getClass(), () -> "OpenAI response:" + response);

        return new JSONObject(response);
    }

    @Override
    public JSONObject raw(final CompletionsForm promptForm) {
        JSONObject jsonObject = buildRequestJson(promptForm);
        return raw(jsonObject, UtilMethods.extractUserIdOrNull(promptForm.user));
    }

    @Override
    public CompletionResponse raw(final CompletionRequest completionRequest) {

        Logger.debug(this, ()-> "Doing raw request: " + completionRequest);
        final AiModelConfig modelConfig = completionRequest.getChatModelConfig();
        final String vendorName = AIUtil.getVendorFromPath(completionRequest.getVendorModelPath());
        final Float temperature = completionRequest.getTemperature();

        final ChatModel chatModel = this.modelProviderFactory.get(vendorName,
                Objects.nonNull(temperature)? AiModelConfig.withTemperature(modelConfig, temperature).build():
                        completionRequest.getChatModelConfig());
        final String userPrompt = completionRequest.getPrompt();
        final String systemPrompt = completionRequest.getSystemPrompt();
        final UserMessage userMessage     = new UserMessage(userPrompt);
        final List<ChatMessage> messages = StringUtils.isSet(systemPrompt)?
                List.of(new SystemMessage(systemPrompt), userMessage):List.of(userMessage);
        final ChatResponse chatResponse = chatModel.chat(messages);
        return new CompletionResponse(chatResponse.aiMessage().text(), chatResponse.aiMessage(), chatResponse.metadata());
    }

    @Override
    public void rawStream(final CompletionsForm promptForm, final OutputStream output) {
        final JSONObject json = buildRequestJson(promptForm);
        json.put(AiKeys.STREAM, true);
        AIProxyClient.get().callToAI(JSONObjectAIRequest.quickText(
                config,
                json,
                UtilMethods.extractUserIdOrNull(promptForm.user)),
                output);
    }

    private AIResponse sendRequest(final AppConfig appConfig, final JSONObject payload, final String userId) {
        return AIProxyClient.get().callToAI(JSONObjectAIRequest.quickText(appConfig, payload, userId));
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
        final ResolvedModel resolvedModel = resolveModel(form);

        // aggregate matching results into text
        final StringBuilder supportingContent = new StringBuilder();
        searchResults.forEach(s -> supportingContent.append(s.extractedText).append(" "));

        final String systemPrompt = getSystemPrompt(form.prompt, supportingContent.toString());
        String textPrompt = getTextPrompt(form.prompt, supportingContent.toString());

        final int systemPromptTokens = countTokens(systemPrompt);
        textPrompt = reduceStringToTokenSize(
                textPrompt,
                resolvedModel.maxTokens - form.responseLengthTokens - systemPromptTokens);

        final JSONObject json = new JSONObject();
        json.put(AiKeys.STREAM, form.stream);
        json.put(AiKeys.TEMPERATURE, form.temperature);

        buildMessages(systemPrompt, textPrompt, json);

        if (UtilMethods.isSet(form.model)) {
            json.put(AiKeys.MODEL, resolvedModel.name);
        }

        if (UtilMethods.isSet(form.responseFormat)) {
            json.put(AiKeys.RESPONSE_FORMAT, form.responseFormat);
        }

        json.put(AiKeys.MAX_TOKENS, form.responseLengthTokens);

        return json;
    }


    /**
     * Determines the current model and the maximum number of tokens to use when making a request to the OpenAI server.
     * Here's how it works:
     *
     * - First, it checks if a whitelist of allowed models is configured in the DotAI App.
     * - If a whitelist exists:
     *    - It verifies that the model name provided in the request is in the whitelist.
     *    - If it is, the method uses that model and the max number of tokens defined in the DotAI App.
     *    - If it isn't, a {@link DotAIModelNotFoundException} is thrown.
     * - If no whitelist is configured (i.e., it's empty), then:
     *    - The provided model name is used.
     *    - The max number of tokens is taken from the DEFAULT_AI_MAX_NUMBER_OF_TOKENS variable.
     *
     * @param completionsForm
     * @return
     */
    private ResolvedModel resolveModel(final CompletionsForm completionsForm) {
        final AIModel aiModel = config.resolveModel(AIModelType.TEXT);
        final List<Model> models = aiModel.getModels().stream()
                .filter(model -> UtilMethods.isSet(model.getName()))
                .collect(Collectors.toList());

        if (UtilMethods.isSet(models)) {
            final Tuple2<AIModel, Model> modelTuple = config
                    .resolveModelOrThrow(completionsForm.model, AIModelType.TEXT);

            return new ResolvedModel(modelTuple._2.getName(), modelTuple._1.getMaxTokens());
        } else if (UtilMethods.isSet(completionsForm.model)) {
            return new ResolvedModel(completionsForm.model, DEFAULT_AI_MAX_NUMBER_OF_TOKENS_VALUE.get());
        } else {
            throw new DotAIModelNotFoundException(
                    "The model is mandatory, you need to set one neither in the dotAPI APP or in the request");
        }
    }

    private String getPrompt(final String prompt, final String supportingContent, final AppKeys key) {
        if (UtilMethods.isEmpty(prompt) || UtilMethods.isEmpty(supportingContent)) {
            throw new DotRuntimeException("no prompt or supporting content to summarize found");
        }

        final String resolvedPrompt = config.getConfig(key);
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
        return EncodingUtil.get()
                .getEncoding(config, AIModelType.TEXT)
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
        final AIModel aiModel = config.getModel();
        final int promptTokens = countTokens(form.prompt);

        final JSONArray messages = new JSONArray();
        final String textPrompt = reduceStringToTokenSize(
                form.prompt,
                aiModel.getMaxTokens() - form.responseLengthTokens - promptTokens);

        messages.add(Map.of(AiKeys.ROLE, AiKeys.USER, AiKeys.CONTENT, textPrompt));

        final JSONObject json = new JSONObject();
        json.put(AiKeys.MESSAGES, messages);
        json.putIfAbsent(AiKeys.MODEL, config.getModel().getCurrentModel());
        json.put(AiKeys.TEMPERATURE, form.temperature);
        json.put(AiKeys.MAX_TOKENS, form.responseLengthTokens);
        json.put(AiKeys.STREAM, form.stream);

        return json;
    }

    /**
     * Use in resolveModel(CompletionsForm) method to return the model and max number of token can must ve used in any
     * request to OpenAI. this can be different according to the request parameters
     */
    private static class ResolvedModel {
        private String name;
        private int maxTokens;

        private ResolvedModel(final String name, final int maxTokens) {
            this.name = name;
            this.maxTokens = maxTokens;
        }
    }

}
