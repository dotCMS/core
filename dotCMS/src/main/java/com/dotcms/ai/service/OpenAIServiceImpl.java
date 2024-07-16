package com.dotcms.ai.service;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotmarketing.util.json.JSONObject;

import javax.ws.rs.HttpMethod;

public class OpenAIServiceImpl implements OpenAIService {

    private static final String AI_MODELS_URL = "https://api.openai.com/v1/models";

    private final AppConfig appConfig;

    public OpenAIServiceImpl(final AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public JSONObject getSupportedModels() {
        final String response = OpenAIRequest.doRequest(AI_MODELS_URL, HttpMethod.GET, appConfig.getApiKey(), null);
        return new JSONObject(response);
    }

}
