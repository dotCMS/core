package com.dotcms.ai.app;

import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The AppConfig class provides a configuration for the AI application.
 * It includes methods for retrieving configuration values based on given keys.
 */
public class AppConfig implements Serializable {

    public static final Pattern SPLITTER = Pattern.compile("\\s?,\\s?");

    private final AIModel model;
    private final AIModel imageModel;
    private final String apiUrl;
    private final String apiImageUrl;
    private final String apiKey;
    private final String rolePrompt;
    private final String textPrompt;
    private final String imagePrompt;
    private final String imageSize;
    private final String listenerIndexer;
    private final Map<String, Secret> configValues;

    public AppConfig(final Map<String, Secret> secrets) {
        configValues = secrets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final AIAppUtil aiAppUtil = AIAppUtil.get();
        aiAppUtil.loadModels(secrets);

        model = AIModels.get().getModelByName(aiAppUtil.resolveSecret(secrets, AppKeys.MODEL_NAME));
        imageModel = AIModels.get().getModelByName(aiAppUtil.resolveSecret(secrets, AppKeys.IMAGE_MODEL_NAME));

        apiUrl = aiAppUtil.resolveEnvSecret(secrets, AppKeys.API_URL);
        apiImageUrl = aiAppUtil.resolveEnvSecret(secrets, AppKeys.API_IMAGE_URL);
        apiKey = aiAppUtil.resolveEnvSecret(secrets, AppKeys.API_KEY);
        rolePrompt = aiAppUtil.resolveSecret(secrets, AppKeys.ROLE_PROMPT);
        textPrompt = aiAppUtil.resolveSecret(secrets, AppKeys.TEXT_PROMPT);
        imagePrompt = aiAppUtil.resolveSecret(secrets, AppKeys.IMAGE_PROMPT);
        imageSize = aiAppUtil.resolveSecret(secrets, AppKeys.IMAGE_SIZE);
        listenerIndexer = aiAppUtil.resolveSecret(secrets, AppKeys.LISTENER_INDEXER);

        Logger.debug(getClass(), () -> "apiUrl: " + apiUrl);
        Logger.debug(getClass(), () -> "apiImageUrl: " + apiImageUrl);
        Logger.debug(getClass(), () -> "apiKey: " + apiKey);
        Logger.debug(getClass(), () -> "rolePrompt: " + rolePrompt);
        Logger.debug(getClass(), () -> "textPrompt: " + textPrompt);
        Logger.debug(getClass(), () -> "model: " + model);
        Logger.debug(getClass(), () -> "imageModel: " + imageModel);
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
        return UtilMethods.isEmpty(apiImageUrl)? AppKeys.API_IMAGE_URL.defaultValue : apiImageUrl;
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
     * Retrieves the Role Prompt.
     *
     * @return the Role Prompt
     */
    public String getRolePrompt() {
        return rolePrompt;
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
     * Retrieves the Model.
     *
     * @return the Model
     */
    public AIModel getModel() {
        return model;
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
    public int getConfigInteger(AppKeys appKey) {
        String value =  Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(()->Integer.parseInt(value)).getOrElse(0);
    }

    /**
     * Retrieves the float configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the float configuration value
     */
    public float getConfigFloat(AppKeys appKey) {
        String value =  Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(()->Float.parseFloat(value)).getOrElse(0f);
    }

    /**
     * Retrieves the boolean configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the boolean configuration value
     */
    public boolean getConfigBoolean(AppKeys appKey) {
        String value =  Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(()->Boolean.parseBoolean(value)).getOrElse(false);
    }

    /**
     * Retrieves the array configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the array configuration value
     */
    public String[] getConfigArray(AppKeys appKey) {
        String returnValue = getConfig(appKey);

        return returnValue != null ? SPLITTER.split(returnValue) : new String[0];
    }

    /**
     * Retrieves the configuration value for a given key.
     *
     * @param appKey the key to retrieve the configuration value for
     * @return the configuration value
     */
    public String getConfig(AppKeys appKey) {
        if (configValues.containsKey(appKey.key)) {
            return Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        }
        return appKey.defaultValue;
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

}