package com.dotcms.ai.domain;

import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotmarketing.util.json.JSONObject;

public class JSONObjectAIRequest extends AIRequest<JSONObject> {

    JSONObjectAIRequest(final String url,
                        final String method,
                        final AppConfig config,
                        final AIModelType type,
                        final JSONObject data,
                        final boolean useOutput) {
        super(url, method, config, type, data, useOutput);
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder extends AIRequest.Builder<JSONObject> {

        @Override
        public JSONObjectAIRequest build() {
            return new JSONObjectAIRequest(url, method, config, type, data, useOutput);
        }

    }

}
