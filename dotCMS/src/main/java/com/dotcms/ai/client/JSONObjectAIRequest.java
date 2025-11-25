package com.dotcms.ai.client;

import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotmarketing.util.json.JSONObject;

/**
 * Represents a request to an AI service with a JSON payload.
 *
 * <p>
 * This class encapsulates the details of an AI request with a JSON payload, including the URL, HTTP method,
 * configuration, model type, payload, and user ID. It provides methods to create and configure AI requests
 * for different model types such as text, image, and embeddings.
 * </p>
 *
 * @author vico
 */
public class JSONObjectAIRequest extends AIRequest<JSONObject> {

    JSONObjectAIRequest(final Builder builder) {
        super(builder);
    }

    /**
     * Creates a quick text AI request with the specified configuration, payload, and user ID.
     *
     * @param appConfig the application configuration
     * @param payload the request payload
     * @param userId the user ID
     * @return a new JSONObjectAIRequest instance
     */
    public static JSONObjectAIRequest quickText(final AppConfig appConfig,
                                                final JSONObject payload,
                                                final String userId) {

        return quick(AIModelType.TEXT, appConfig, payload, userId);
    }

    /**
     * Creates a quick image AI request with the specified configuration, payload, and user ID.
     *
     * @param appConfig the application configuration
     * @param payload the request payload
     * @param userId the user ID
     * @return a new JSONObjectAIRequest instance
     */
    public static JSONObjectAIRequest quickImage(final AppConfig appConfig,
                                                 final JSONObject payload,
                                                 final String userId) {
        return quick(AIModelType.IMAGE, appConfig, payload, userId);
    }

    /**
     * Creates a quick embeddings AI request with the specified configuration, payload, and user ID.
     *
     * @param appConfig the application configuration
     * @param payload the request payload
     * @param userId the user ID
     * @return a new JSONObjectAIRequest instance
     */
    public static JSONObjectAIRequest quickEmbeddings(final AppConfig appConfig,
                                                      final JSONObject payload,
                                                      final String userId) {
        return quick(AIModelType.EMBEDDINGS, appConfig, payload, userId);
    }

    private static JSONObjectAIRequest quick(final String url,
                                             final AppConfig appConfig,
                                             final AIModelType type,
                                             final JSONObject payload,
                                             final String userId) {
        return JSONObjectAIRequest.builder()
                .withUrl(url)
                .withConfig(appConfig)
                .withType(type)
                .withPayload(payload)
                .withUserId(userId)
                .build();
    }

    private static JSONObjectAIRequest quick(final AIModelType type,
                                             final AppConfig appConfig,
                                             final JSONObject payload,
                                             final String userId) {
        return quick(resolveUrl(type, appConfig), appConfig, type, payload, userId);
    }

    @Override
    public String payloadToString() {
        return getPayload().toString(2);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AIRequest.Builder<JSONObject, Builder> {

        @Override
        public JSONObjectAIRequest build() {
            return new JSONObjectAIRequest(this);
        }

    }

}
