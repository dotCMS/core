package com.dotcms.ai.model.request;

import com.dotcms.ai.config.AppConfig;
import com.dotmarketing.util.json.JSONObject;

public class AIJSONObjectRequest extends AIRequest<JSONObject> {

    public AIJSONObjectRequest(final String url,
                               final String method,
                               final AppConfig appConfig,
                               final JSONObject payload) {
        super(url, method, appConfig, payload);
    }

}
