package com.dotcms.ai.app;

import com.dotcms.ai.domain.Model;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The AppConfig class provides a configuration for the AI application.
 * It includes methods for retrieving configuration values based on given keys.
 */
public class AppConfig implements Serializable {

    private static final String AI_API_URL_KEY = "AI_API_URL";
    private static final String AI_IMAGE_API_URL_KEY = "AI_IMAGE_API_URL";
    private static final String AI_EMBEDDINGS_API_URL_KEY = "AI_EMBEDDINGS_API_URL";
    private static final String AI_DEBUG_LOGGING_KEY = "AI_DEBUG_LOGGING";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final Pattern SPLITTER = Pattern.compile("\\s?,\\s?");

    private final String host;
    private final String apiKey;
    private final transient AIModel model;
    private final transient AIModel imageModel;
    private final transient AIModel embeddingsModel;
    private final String apiUrl;
    private final String apiImageUrl;
    private final String apiEmbeddingsUrl;
    private final String rolePrompt;
    private final String textPrompt;
    private final String imagePrompt;
    private final String imageSize;
    private final String listenerIndexer;
    private final String providerConfig;
    private final String providerConfigHash;
    private final Map<String, Secret> configValues;

    public AppConfig(final String host, final Map<String, Secret> secrets) {
        this.host = host;

        final AIAppUtil aiAppUtil = AIAppUtil.get();
        apiKey = aiAppUtil.discoverSecret(secrets, AppKeys.API_KEY);
        apiUrl = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_URL, AI_API_URL_KEY);
        apiImageUrl = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_IMAGE_URL, AI_IMAGE_API_URL_KEY);
        apiEmbeddingsUrl = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_EMBEDDINGS_URL, AI_EMBEDDINGS_API_URL_KEY);
        providerConfig = aiAppUtil.discoverSecret(secrets, AppKeys.PROVIDER_CONFIG);

        if (StringUtils.isNotBlank(providerConfig)) {
            providerConfigHash = sha256Hex(providerConfig);
            final JsonNode providerConfigRoot = parseProviderConfig(providerConfig);
            model = buildModelFromProviderConfigNode(providerConfigRoot, "chat", AIModelType.TEXT);
            imageModel = buildModelFromProviderConfigNode(providerConfigRoot, "image", AIModelType.IMAGE);
            embeddingsModel = buildModelFromProviderConfigNode(providerConfigRoot, "embeddings", AIModelType.EMBEDDINGS);
        } else {
            providerConfigHash = null;
            model = AIModel.NOOP_MODEL;
            imageModel = AIModel.NOOP_MODEL;
            embeddingsModel = AIModel.NOOP_MODEL;
        }

        rolePrompt = aiAppUtil.discoverSecret(secrets, AppKeys.ROLE_PROMPT);
        textPrompt = aiAppUtil.discoverSecret(secrets, AppKeys.TEXT_PROMPT);
        imagePrompt = aiAppUtil.discoverSecret(secrets, AppKeys.IMAGE_PROMPT);
        imageSize = aiAppUtil.discoverSecret(secrets, AppKeys.IMAGE_SIZE);
        listenerIndexer = aiAppUtil.discoverSecret(secrets, AppKeys.LISTENER_INDEXER);

        configValues = secrets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Logger.debug(this, this::toString);
    }

    /**
     * Prints a specific error message to the log, based on the {@link AppKeys#DEBUG_LOGGING}
     * property instead of the usual Log4j configuration.
     *
     * @param appConfig The {#link AppConfig} to be used when logging.
     * @param clazz   The {@link Class} to log the message for.
     * @param message The {@link Supplier} with the message to log.
     */
    public static void debugLogger(final AppConfig appConfig, final Class<?> clazz, final Supplier<String> message) {
        if (appConfig == null) {
            Logger.debug(clazz, message);
            return;
        }
        if (appConfig.getConfigBoolean(AppKeys.DEBUG_LOGGING)
                || Config.getBooleanProperty(AI_DEBUG_LOGGING_KEY, false)) {
            Logger.info(clazz, message.get());
        }
    }

    /**
     * Retrieves the host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Retrieves the API URL.
     *
     * @return the API URL
     */
    public String getApiUrl() {
        return UtilMethods.isEmpty(apiUrl) ? AppKeys.API_URL.defaultValue : apiUrl;
    }

    /**
     * Retrieves the API Image URL.
     *
     * @return the API Image URL
     */
    public String getApiImageUrl() {
        return UtilMethods.isEmpty(apiImageUrl) ? AppKeys.API_IMAGE_URL.defaultValue : apiImageUrl;
    }

    /**
     * Retrieves the API Embeddings URL.
     *
     * @return the API Embeddings URL
     */
    public String getApiEmbeddingsUrl() {
        return UtilMethods.isEmpty(apiEmbeddingsUrl) ? AppKeys.API_EMBEDDINGS_URL.defaultValue : apiEmbeddingsUrl;
    }

    /**
     * Retrieves the API Key.
     *
     * @return the API Key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Retrieves the Model.
     *
     * @return the Model
     */
    public AIModel getModel() {
        return model;
    }

    /**
     * Retrieves the Image Model.
     *
     * @return the Image Model
     */
    public AIModel getImageModel() {
        return imageModel;
    }

    /**
     * Retrieves the Embeddings Model.
     *
     * @return the Embeddings Model
     */
    public AIModel getEmbeddingsModel() {
        return embeddingsModel;
    }

    /**
     * Retrieves the Role Prompt.
     *
     * @return the Role Prompt
     */
    public String getRolePrompt() {
        return rolePrompt;
    }

    /**
     * Retrieves the Text Prompt.
     *
     * @return the Text Prompt
     */
    public String getTextPrompt() {
        return textPrompt;
    }

    /**
     * Retrieves the Image Prompt.
     *
     * @return the Image Prompt
     */
    public String getImagePrompt() {
        return imagePrompt;
    }

    /**
     * Retrieves the Image Size.
     *
     * @return the Image Size
     */
    public String getImageSize() {
        return imageSize;
    }

    /**
     * Retrieves the Listener Indexer.
     *
     * @return the Listener Indexer
     */
    public String getListenerIndexer() {
        return listenerIndexer;
    }

    /**
     * Retrieves the integer configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the integer configuration value
     */
    public int getConfigInteger(final AppKeys appKey) {
        String value = Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(() -> Integer.parseInt(value)).getOrElse(0);
    }

    /**
     * Retrieves the float configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the float configuration value
     */
    public float getConfigFloat(final AppKeys appKey) {
        String value = Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(() -> Float.parseFloat(value)).getOrElse(0f);
    }

    /**
     * Retrieves the boolean configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the boolean configuration value
     */
    public boolean getConfigBoolean(final AppKeys appKey) {
        final String value = Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(() -> Boolean.parseBoolean(value)).getOrElse(false);
    }

    /**
     * Retrieves the array configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the array configuration value
     */
    public String[] getConfigArray(final AppKeys appKey) {
        final String returnValue = getConfig(appKey);
        return returnValue != null ? SPLITTER.split(returnValue) : new String[0];
    }

    /**
     * Retrieves the configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the configuration value
     */
    public String getConfig(final AppKeys appKey) {
        if (configValues.containsKey(appKey.key)) {
            return Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        }
        return appKey.defaultValue;
    }

    /**
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     *
     * @param type the type of the model to find
     */
    public AIModel resolveModel(final AIModelType type) {
        switch (type) {
            case TEXT: return model;
            case IMAGE: return imageModel;
            case EMBEDDINGS: return embeddingsModel;
            default: return AIModel.NOOP_MODEL;
        }
    }

    /**
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     * If the model is not found or is not operational, it throws an appropriate exception.
     *
     * @param modelName the name of the model to find
     * @param type the type of the model to find
     * @return the resolved Model
     */
    public Tuple2<AIModel, Model> resolveModelOrThrow(final String modelName, final AIModelType type) {
        final AIModel aiModel = resolveModel(type);
        if (aiModel == AIModel.NOOP_MODEL) {
            throw new DotAIModelNotFoundException(
                    String.format("Unable to find model: [%s] of type [%s].", modelName, type));
        }
        return Tuple.of(aiModel, aiModel.getModel(modelName));
    }

    /**
     * Returns the raw {@code providerConfig} JSON string, or {@code null} if not set.
     */
    public String getProviderConfig() {
        return providerConfig;
    }

    /**
     * Returns the SHA-256 hex digest of the {@code providerConfig} JSON, or {@code null} if not set.
     * Computed once at construction time — safe to use as a cache key on every request.
     */
    public String getProviderConfigHash() {
        return providerConfigHash;
    }

    /**
     * Checks if the configuration is enabled.
     * Returns true when a non-blank {@code providerConfig} JSON is present and at least one
     * model section (chat, image, embeddings) parsed successfully.
     *
     * @return true if the configuration is enabled, false otherwise
     */
    public boolean isEnabled() {
        if (StringUtils.isBlank(providerConfig)) {
            Logger.info(AppConfig.class, "dotAI not enabled for host [" + host + "]: providerConfig is blank");
            return false;
        }
        if (model == AIModel.NOOP_MODEL && imageModel == AIModel.NOOP_MODEL && embeddingsModel == AIModel.NOOP_MODEL) {
            Logger.info(AppConfig.class, "dotAI not enabled for host [" + host + "]: providerConfig set but no model section parsed successfully");
            return false;
        }
        return true;
    }

    private static JsonNode parseProviderConfig(final String json) {
        try {
            return MAPPER.readTree(json.replaceAll("[\\r\\n\\t]", ""));
        } catch (final Exception e) {
            Logger.warn(AppConfig.class, "Failed to parse providerConfig JSON"
                    + " (" + e.getClass().getSimpleName() + "): " + e.getMessage(), e);
            return MAPPER.createObjectNode();
        }
    }

    private static AIModel buildModelFromProviderConfigNode(final JsonNode root, final String section, final AIModelType type) {
        try {
            final JsonNode sectionNode = root.get(section);
            if (sectionNode == null) {
                return AIModel.NOOP_MODEL;
            }
            final JsonNode modelNode = sectionNode.get("model");
            if (modelNode == null || modelNode.asText().isBlank()) {
                return AIModel.NOOP_MODEL;
            }
            final AIModel.Builder builder = AIModel.builder()
                    .withType(type)
                    .withModelNames(modelNode.asText());
            final JsonNode maxTokensNode = sectionNode.get("maxTokens");
            if (maxTokensNode != null && maxTokensNode.isInt()) {
                builder.withMaxTokens(maxTokensNode.asInt());
            }
            return builder.build();
        } catch (final Exception e) {
            Logger.warn(AppConfig.class, "Failed to parse model from providerConfig section '" + section + "': " + e.getMessage());
            return AIModel.NOOP_MODEL;
        }
    }

    private static String sha256Hex(final String input) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public void debugLogger(final Class<?> clazz, final Supplier<String> message) {
        debugLogger(this, clazz, message);
    }

    @Override
    public String toString() {
        return "AppConfig{\n" +
                "  host='" + host + "',\n" +
                "  apiKey='" + Optional.ofNullable(apiKey).map(key -> "*****").orElse(StringPool.BLANK) + "',\n" +
                "  model=" + model + "',\n" +
                "  imageModel=" + imageModel + "',\n" +
                "  embeddingsModel=" + embeddingsModel + "',\n" +
                "  apiUrl='" + apiUrl + "',\n" +
                "  apiImageUrl='" + apiImageUrl + "',\n" +
                "  apiEmbeddingsUrl='" + apiEmbeddingsUrl + "',\n" +
                "  rolePrompt='" + rolePrompt + "',\n" +
                "  textPrompt='" + textPrompt + "',\n" +
                "  imagePrompt='" + imagePrompt + "',\n" +
                "  imageSize='" + imageSize + "',\n" +
                "  listenerIndexer='" + listenerIndexer + "',\n" +
                "  providerConfig='" + (StringUtils.isNotBlank(providerConfig) ? "[set]" : "[not set]") + "'\n" +
                '}';
    }

}
