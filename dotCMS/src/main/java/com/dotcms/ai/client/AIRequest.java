package com.dotcms.ai.client;

import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;

import javax.ws.rs.HttpMethod;
import java.io.Serializable;

/**
 * Represents a request to an AI service.
 *
 * <p>
 * This class encapsulates the details of an AI request, including the URL, HTTP method,
 * configuration, model type, payload, and user ID. It provides methods to create and
 * configure AI requests for different model types such as text, image, and embeddings.
 * </p>
 *
 * @param <T> the type of the request payload
 * @author vico
 */
public class AIRequest<T extends Serializable> {

    private final String url;
    private final String method;
    private final AppConfig config;
    private final AIModelType type;
    private final T payload;
    private final String userId;

    <B extends AIRequest.Builder<T, B>> AIRequest(final Builder<T, B> builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.config = builder.config;
        this.type = builder.type;
        this.payload = builder.payload;
        this.userId = builder.userId;
    }

    /**
     * Creates a quick text AI request with the specified configuration, payload, and user ID.
     *
     * @param appConfig the application configuration
     * @param payload the request payload
     * @param userId the user ID
     * @param <T> the type of the request payload
     * @param <R> the type of the AIRequest
     * @return a new AIRequest instance
     */
    public static <T extends Serializable, R extends AIRequest<T>> R quickText(final AppConfig appConfig,
                                                                               final T payload,
                                                                               final String userId) {
        return quick(AIModelType.TEXT, appConfig, payload, userId);
    }

    /**
     * Creates a quick image AI request with the specified configuration, payload, and user ID.
     *
     * @param appConfig the application configuration
     * @param payload the request payload
     * @param userId the user ID
     * @param <T> the type of the request payload
     * @param <R> the type of the AIRequest
     * @return a new AIRequest instance
     */
    public static <T extends Serializable, R extends AIRequest<T>> R quickImage(final AppConfig appConfig,
                                                                                final T payload,
                                                                                final String userId) {
        return quick(AIModelType.IMAGE, appConfig, payload, userId);
    }

    /**
     * Creates a quick embeddings AI request with the specified configuration, payload, and user ID.
     *
     * @param appConfig the application configuration
     * @param payload the request payload
     * @param userId the user ID
     * @param <T> the type of the request payload
     * @param <R> the type of the AIRequest
     * @return a new AIRequest instance
     */
    public static <T extends Serializable, R extends AIRequest<T>> R quickEmbeddings(final AppConfig appConfig,
                                                                                     final T payload,
                                                                                     final String userId) {
        return quick(AIModelType.EMBEDDINGS, appConfig, payload, userId);
    }

    public static <T extends Serializable, B extends AIRequest.Builder<T, B>> Builder<T, B> builder() {
        return new Builder<>();
    }

    /**
     * Resolves the URL for the specified model type and application configuration.
     *
     * @param type the AI model type
     * @param appConfig the application configuration
     * @return the resolved URL
     */
    static String resolveUrl(final AIModelType type, final AppConfig appConfig) {
        final String resolved;
        switch (type) {
            case TEXT:
                resolved = appConfig.getApiUrl();
                break;
            case IMAGE:
                resolved = appConfig.getApiImageUrl();
                break;
            case EMBEDDINGS:
                resolved = appConfig.getApiEmbeddingsUrl();
                break;
            default:
                throw new IllegalArgumentException("Invalid AIModelType: " + type);
        }

        return resolved;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable, B extends AIRequest.Builder<T, B>, R extends AIRequest<T>> R quick(
            final String url,
            final AppConfig appConfig,
            final AIModelType type,
            final T payload,
            final String usderId) {
        return (R) AIRequest.<T, B>builder()
                .withUrl(url)
                .withConfig(appConfig)
                .withType(type)
                .withPayload(payload)
                .withUserId(usderId)
                .build();
    }

    private static <T extends Serializable, R extends AIRequest<T>> R quick(
            final AIModelType type,
            final AppConfig appConfig,
            final T payload,
            final String userId) {
        return quick(resolveUrl(type, appConfig), appConfig, type, payload, userId);
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

    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "AIRequest{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", config=" + config +
                ", type=" + type +
                ", payload=" + payloadToString() +
                ", userId='" + userId + '\'' +
                '}';
    }

    public String payloadToString() {
        return payload.toString();
    }

    public static class Builder<T extends Serializable, B extends AIRequest.Builder<T, B>> {

        String url;
        String method = HttpMethod.POST;
        AppConfig config;
        AIModelType type;
        T payload;
        String userId;

        @SuppressWarnings("unchecked")
        B self() {
            return (B) this;
        }

        public B withUrl(final String url) {
            this.url = url;
            return self();
        }

        public B withConfig(final AppConfig config) {
            this.config = config;
            return self();
        }

        public B withType(final AIModelType type) {
            this.type = type;
            return self();
        }

        public B withPayload(final T payload) {
            this.payload = payload;
            return self();
        }

        public B withUserId(final String userId) {
            this.userId = userId;
            return self();
        }

        public AIRequest<T> build() {
            return new AIRequest<>(this);
        }

    }
}
