package com.dotcms.ai.service;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;

import javax.ws.rs.HttpMethod;
import java.util.List;
import java.util.Map;

public class OpenAIChatServiceImpl implements OpenAIChatService {

    private final AppConfig config;

    public OpenAIChatServiceImpl(final AppConfig appConfig) {
        this.config = appConfig;
    }

    @Override
    public JSONObject sendRawRequest(final JSONObject prompt) {
        prompt.putIfAbsent(AiKeys.MODEL, config.getModel());
        prompt.putIfAbsent(AiKeys.TEMPERATURE, config.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE));

        if (UtilMethods.isEmpty(prompt.optString(AiKeys.MESSAGES))) {
            prompt.put(
                    AiKeys.MESSAGES,
                    List.of(
                            Map.of(AiKeys.ROLE, AiKeys.SYSTEM, AiKeys.CONTENT, config.getRolePrompt()),
                            Map.of(AiKeys.ROLE, AiKeys.USER, AiKeys.CONTENT, prompt.getString(AiKeys.PROMPT))
                    ));
        }

        prompt.remove(AiKeys.PROMPT);

        return new JSONObject(doRequest(config.getApiUrl(), config.getApiKey(), prompt));
    }

    @Override
    public JSONObject sendTextPrompt(final String textPrompt) {
        final JSONObject newPrompt = new JSONObject();
        newPrompt.put(AiKeys.PROMPT, textPrompt);
        return sendRawRequest(newPrompt);
    }

    @VisibleForTesting
    String doRequest(final String urlIn, final String openAiAPIKey, final JSONObject json) {
        return OpenAIRequest.doRequest(urlIn, HttpMethod.POST, openAiAPIKey, json);
    }

}