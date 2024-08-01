package com.dotcms.ai.app;

import com.dotcms.ai.api.AIModels;
import com.dotcms.ai.model.AIModel;
import com.dotcms.ai.util.AIAppUtil;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The AppConfig class provides a configuration for the AI application.
 * It includes methods for retrieving configuration values based on given keys.
 */
public class AppConfig implements Serializable {

    private static final String OPEN_AI_EMBEDDINGS_URL_KEY = "OPEN_AI_EMBEDDINGS_URL";
    public static final Pattern SPLITTER = Pattern.compile("\\s?,\\s?");

    private final String host;
    private final transient AIModel model;
    private final transient AIModel imageModel;
    private final transient AIModel embeddingsModel;
    private final String apiUrl;
    private final String apiImageUrl;
    private final String apiEmbeddingsUrl;
    private final String apiKey;
    private final String rolePrompt;
    private final String textPrompt;
    private final String imagePrompt;
    private final String imageSize;
    private final String listenerIndexer;
    private final Map<String, Secret> configValues;

    public AppConfig(final String host, final Map<String, Secret> secrets) {
        this.host = host;

        final AIAppUtil aiAppUtil = AIAppUtil.get();
        AIModels.get().loadModels(
                this.host,
                List.of(
                        aiAppUtil.createTextModel(secrets),
                        aiAppUtil.createImageModel(secrets),
                        aiAppUtil.createEmbeddingsModel(secrets)));

        model = resolveModel(AIModelType.TEXT);
        imageModel = resolveModel(AIModelType.IMAGE);
        embeddingsModel = resolveModel(AIModelType.EMBEDDINGS);

        apiUrl = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_URL);
        apiImageUrl = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_IMAGE_URL);
        apiEmbeddingsUrl = discoverEmbeddingsApiUrl(secrets);
        apiKey = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_KEY);
        rolePrompt = aiAppUtil.discoverSecret(secrets, AppKeys.ROLE_PROMPT);
        textPrompt = aiAppUtil.discoverSecret(secrets, AppKeys.TEXT_PROMPT);
        imagePrompt = aiAppUtil.discoverSecret(secrets, AppKeys.IMAGE_PROMPT);
        imageSize = aiAppUtil.discoverSecret(secrets, AppKeys.IMAGE_SIZE);
        listenerIndexer = aiAppUtil.discoverSecret(secrets, AppKeys.LISTENER_INDEXER);

        configValues = secrets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Logger.debug(getClass(), () -> "apiUrl: " + apiUrl);
        Logger.debug(getClass(), () -> "apiImageUrl: " + apiImageUrl);
        Logger.debug(getClass(), () -> "embeddingsUrl: " + apiEmbeddingsUrl);
        Logger.debug(getClass(), () -> "apiKey: " + apiKey);
        Logger.debug(getClass(), () -> "model: " + model);
        Logger.debug(getClass(), () -> "imageModel: " + imageModel);
        Logger.debug(getClass(), () -> "embeddingsModel: " + embeddingsModel);
        Logger.debug(getClass(), () -> "rolePrompt: " + rolePrompt);
        Logger.debug(getClass(), () -> "textPrompt: " + textPrompt);
        Logger.debug(getClass(), () -> "imagePrompt: " + imagePrompt);
        Logger.debug(getClass(), () -> "imageSize: " + imageSize);
        Logger.debug(getClass(), () -> "listerIndexer: " + listenerIndexer);
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
     * @return
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
        return AIModels.get().findModel(host, type).orElse(AIModels.NOOP_MODEL);
    }

    /**
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     *
     * @param modelName the name of the model to find
     */
    public AIModel resolveModelOrThrow(final String modelName) {
        return AIModels.get()
                .findModel(host, modelName)
                .orElseThrow(() -> {
                    final String supported = String.join(", ", AIModels.get().getOrPullSupportedModels());
                    return new DotRuntimeException(
                            "Unable to find model: [" + modelName + "]. Only [" + supported + "] are supported ");
                });
    }

    /**
     * Prints a specific error message to the log, based on the {@link AppKeys#DEBUG_LOGGING}
     * property instead of the usual Log4j configuration.
     *
     * @param clazz   The {@link Class} to log the message for.
     * @param message The {@link Supplier} with the message to log.
     */
    public static void debugLogger(final Class<?> clazz, final Supplier<String> message) {
        if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.info(clazz, message.get());
        }
    }

    private String discoverEmbeddingsApiUrl(final Map<String, Secret> secrets) {
        final String url = AIAppUtil.get().discoverEnvSecret(secrets, AppKeys.API_EMBEDDINGS_URL);
        return StringUtils.isBlank(url)
                ? Config.getStringProperty(OPEN_AI_EMBEDDINGS_URL_KEY, "https://api.openai.com/v1/embeddings")
                : url;
    }

}
