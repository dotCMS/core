package com.dotcms.ai.app;

import com.dotcms.ai.domain.Model;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AppConfig class provides a configuration for the AI application.
 * It includes methods for retrieving configuration values based on given keys.
 */
public class AppConfig implements Serializable {

    private static final String AI_API_URL_KEY = "AI_API_URL";
    private static final String AI_IMAGE_API_URL_KEY = "AI_IMAGE_API_URL";
    private static final String AI_EMBEDDINGS_API_URL_KEY = "AI_EMBEDDINGS_API_URL";
    private static final String AI_DEBUG_LOGGING_KEY = "AI_DEBUG_LOGGING";

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
    private final Map<String, Secret> configValues;

    public AppConfig(final String host, final Map<String, Secret> secrets) {
        this.host = host;

        final AIAppUtil aiAppUtil = AIAppUtil.get();
        apiKey = aiAppUtil.discoverApiKeySecret(secrets);
        apiUrl = aiAppUtil.discoverApiUrlEnvSecret(secrets, AI_API_URL_KEY);
        apiImageUrl = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_IMAGE_URL, AI_IMAGE_API_URL_KEY);
        apiEmbeddingsUrl = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_EMBEDDINGS_URL, AI_EMBEDDINGS_API_URL_KEY);

        if (!secrets.isEmpty() || isEnabled()) {
            AIModels.get().loadModels(
                    this,
                    List.of(
                            aiAppUtil.createTextModel(secrets),
                            aiAppUtil.createImageModel(secrets),
                            aiAppUtil.createEmbeddingsModel(secrets)));
        }

        model = resolveModel(AIModelType.TEXT);
        imageModel = resolveModel(AIModelType.IMAGE);
        embeddingsModel = resolveModel(AIModelType.EMBEDDINGS);

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
        return AIModels.get().resolveModel(host, type);
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
        return AIModels.get().resolveModelOrThrow(this, modelName, type);
    }

    /**
     * Checks if the configuration is enabled.
     *
     * @return true if the configuration is enabled, false otherwise
     */
    public boolean isEnabled() {
        return Stream.of(apiUrl, apiImageUrl, apiEmbeddingsUrl, apiKey).allMatch(StringUtils::isNotBlank);
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
                "  listenerIndexer='" + listenerIndexer + "'\n" +
                '}';
    }

}
