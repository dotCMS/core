package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;

import java.util.List;
import java.util.Map;

public class OpenAIChatServiceImpl implements OpenAIChatService {
    final AppConfig config;

    public OpenAIChatServiceImpl(AppConfig appConfig) {

        this.config=appConfig;
    }

    @Override
    public JSONObject sendTextPrompt(String textPrompt) {
        JSONObject newPrompt = new JSONObject();
        newPrompt.put("prompt", textPrompt);
        return sendRawRequest(newPrompt);
    }

    @Override
    public JSONObject sendRawRequest(JSONObject prompt) {
        prompt.putIfAbsent("model",config.getModel());
        prompt.putIfAbsent("temperature",config.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE));

        if(UtilMethods.isEmpty(prompt.optString("messages"))){
            List messages = List.of(
                    Map.of("role","system", "content", config.getRolePrompt()),
                    Map.of("role","user", "content", prompt.getString("prompt"))
            );
            prompt.put("messages", messages);
        }

        prompt.remove("prompt");
        return new JSONObject(OpenAIRequest.doRequest(config.getApiUrl(),"POST",config.getApiKey(),prompt));
    }
}
