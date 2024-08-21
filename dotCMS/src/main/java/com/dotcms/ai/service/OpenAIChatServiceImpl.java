package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.Map;

public class OpenAIChatServiceImpl implements OpenAIChatService {
    private final AppConfig config;

    public OpenAIChatServiceImpl(final AppConfig appConfig) {
        this.config = appConfig;
    }

    @Override
    public JSONObject sendRawRequest(final JSONObject prompt) {
        prompt.putIfAbsent("model", config.getModel());
        prompt.putIfAbsent("temperature", config.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE));

        if (UtilMethods.isEmpty(prompt.optString("messages"))) {
            prompt.put(
                    "messages",
                    List.of(
                            Map.of("role", "system", "content", config.getRolePrompt()),
                            Map.of("role", "user", "content", prompt.getString("prompt"))
                    ));
        }

        prompt.remove("prompt");

        return new JSONObject(doRequest(config.getApiUrl(), config.getApiKey(), prompt));
    }

    @Override
    public JSONObject sendTextPrompt(final String textPrompt) {
        final JSONObject newPrompt = new JSONObject();
        newPrompt.put("prompt", textPrompt);
        return sendRawRequest(newPrompt);
    }

    @VisibleForTesting
    String doRequest(final String urlIn, final String openAiAPIKey, final JSONObject json) {
        return OpenAIRequest.doRequest(urlIn, "POST", openAiAPIKey, json);
    }

}