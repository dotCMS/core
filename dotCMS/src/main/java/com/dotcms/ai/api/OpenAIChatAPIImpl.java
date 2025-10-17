package com.dotcms.ai.api;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AiAppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.client.AIProxyClient;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

public class OpenAIChatAPIImpl implements ChatAPI {

    private final AiAppConfig config;
    private final User user;

    public OpenAIChatAPIImpl(final AiAppConfig appConfig, final User user) {
        this.config = appConfig;
        this.user = user;
    }

    @Override
    public JSONObject sendRawRequest(final JSONObject prompt) {
        prompt.putIfAbsent(AiKeys.MODEL, config.getModel().getCurrentModel());
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

        return new JSONObject(doRequest(prompt, UtilMethods.extractUserIdOrNull(user)));
    }

    @Override
    public JSONObject sendTextPrompt(final String textPrompt) {
        final JSONObject newPrompt = new JSONObject();
        newPrompt.put(AiKeys.PROMPT, textPrompt);
        return sendRawRequest(newPrompt);
    }

    @VisibleForTesting
    String doRequest(final JSONObject json, final String userId) {
        return AIProxyClient.get().callToAI(JSONObjectAIRequest.quickText(config, json, userId)).getResponse();
    }

}
