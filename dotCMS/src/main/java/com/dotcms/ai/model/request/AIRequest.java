package com.dotcms.ai.model.request;

import com.dotcms.ai.config.AppConfig;

import java.io.Serializable;

public class AIRequest<T extends Serializable> {

    private final String url;
    private final String method;
    private final AppConfig appConfig;
    private final T payload;

    AIRequest(final String url,
                      final String method,
                      final AppConfig appConfig,
                      final T payload) {
        this.url = url;
        this.method = method;
        this.appConfig = appConfig;
        this.payload = payload;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "AIRequest{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", appConfig=" + appConfig +
                ", payload=" + payload +
                '}';
    }

    static <T extends Serializable> Builder<T> builder() {
        return new Builder<>();
    }

    private static class Builder<T extends Serializable> {

        private String url;
        private String method;
        private AppConfig appConfig;
        private T payload;

        public Builder url(final String url) {
            this.url = url;
            return this;
        }

        public Builder method(final String method) {
            this.method = method;
            return this;
        }

        public Builder appConfig(final AppConfig appConfig) {
            this.appConfig = appConfig;
            return this;
        }

        public Builder payload(final T payload) {
            this.payload = payload;
            return this;
        }

        public AIRequest build() {
            return new AIRequest(url, method, appConfig, payload);
        }

    }

}
