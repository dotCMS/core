package com.dotcms.ai.domain;

import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;

import java.io.Serializable;

public class AIRequest<T extends Serializable> {

    private final String url;
    private final String method;
    private final AppConfig config;
    private final AIModelType type;
    private final T payload;
    private boolean useOutput;

    AIRequest(final String url,
              final String method,
              final AppConfig config,
              final AIModelType type,
              final T payload,
              final boolean useOutput) {
        this.config = config;
        this.url = url;
        this.method = method;
        this.type = type;
        this.payload = payload;
        this.useOutput = useOutput;
    }

    static <T extends Serializable> Builder<T> builder() {
        return new Builder<>();
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public AppConfig getConfig() {
        return config;
    }

    public AIModelType getType() {
        return type;
    }

    public T getPayload() {
        return payload;
    }

    public boolean isUseOutput() {
        return useOutput;
    }

    @Override
    public String toString() {
        return "AIRequest{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", config=" + config +
                ", type=" + type +
                ", payload=" + payload +
                ", useOutput=" + useOutput +
                '}';
    }

    static class Builder<T extends Serializable> {

        String url;
        String method;
        AppConfig config;
        AIModelType type;
        T data;
        boolean useOutput;

        Builder() {
        }

        public Builder withUrl(final String url) {
            this.url = url;
            return this;
        }

        public Builder withMethod(final String method) {
            this.method = method;
            return this;
        }

        public Builder withConfig(final AppConfig config) {
            this.config = config;
            return this;
        }

        public Builder withType(final AIModelType type) {
            this.type = type;
            return this;
        }

        public Builder withData(final T data) {
            this.data = data;
            return this;
        }

        public Builder withUseOutput(final boolean useOutput) {
            this.useOutput = useOutput;
            return this;
        }

        public AIRequest<T> build() {
            return new AIRequest<>(url, method, config, type, data, useOutput);
        }

    }
}
