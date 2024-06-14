package com.dotcms.ai.app;

import com.dotcms.security.apps.AppsUtil;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The AppConfig class provides a configuration for the AI application.
 * It includes methods for retrieving configuration values based on given keys.
 */
public class AppConfig implements Serializable {

    public static final Pattern SPLITTER= Pattern.compile("\\s?,\\s?");

    public final String model;
    public final String imageModel;
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
        this.configValues = secrets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        apiUrl = resolveEnvSecret(secrets, AppKeys.API_URL);
        apiImageUrl = resolveEnvSecret(secrets, AppKeys.API_IMAGE_URL);
        apiKey = resolveEnvSecret(secrets, AppKeys.API_KEY);
        rolePrompt = resolveSecretOrBlank(secrets, AppKeys.ROLE_PROMPT);
        textPrompt = resolveSecretOrBlank(secrets, AppKeys.TEXT_PROMPT);
        imagePrompt = resolveSecretOrBlank(secrets, AppKeys.IMAGE_PROMPT);
        imageSize = resolveSecret(secrets, AppKeys.IMAGE_SIZE, AppKeys.IMAGE_SIZE.defaultValue);
        model = resolveSecretOrBlank(secrets, AppKeys.MODEL);
        imageModel = resolveSecret(secrets, AppKeys.IMAGE_MODEL, "dall-e-3");
        listenerIndexer = resolveSecretOrBlank(secrets, AppKeys.LISTENER_INDEXER);
        Logger.debug(this.getClass().getName(), () -> "apiUrl: " + apiUrl);
        Logger.debug(this.getClass().getName(), () -> "apiImageUrl: " + apiImageUrl);
        Logger.debug(this.getClass().getName(), () -> "apiKey: " + apiKey);
        Logger.debug(this.getClass().getName(), () -> "rolePrompt: " + rolePrompt);
        Logger.debug(this.getClass().getName(), () -> "textPrompt: " + textPrompt);
        Logger.debug(this.getClass().getName(), () -> "imagePrompt: " + imagePrompt);
        Logger.debug(this.getClass().getName(), () -> "imageModel: " + imageModel);
        Logger.debug(this.getClass().getName(), () -> "imageSize: " + imageSize);
        Logger.debug(this.getClass().getName(), () -> "model: " + model);
        Logger.debug(this.getClass().getName(), () -> "listerIndexer: " + listenerIndexer);
    }

    private String resolveSecret(final Map<String, Secret> secrets, final AppKeys key, final String defaultValue) {
        return Try.of(() -> secrets.get(key.key).getString()).getOrElse(defaultValue);
    }

    private String resolveSecretOrBlank(final Map<String, Secret> secrets, final AppKeys key) {
        return resolveSecret(secrets, key, StringPool.BLANK);
    }

    private String resolveEnvSecret(final Map<String, Secret> secrets, final AppKeys key) {
        final String secret = resolveSecretOrBlank(secrets, key);
        if (UtilMethods.isSet(secret)) {
            return secret;
        }

        return Optional
                .ofNullable(AppsUtil.discoverEnvVarValue(AppKeys.APP_KEY, key.key, null))
                .orElse(StringPool.BLANK);
    }

    /**
     * Retrieves the API URL.
     *
     * @return the API URL
     */
    public String getApiUrl() {
        return UtilMethods.isEmpty(apiUrl) ? "https://api.openai.com/v1/chat/completions" : apiUrl;
    }

    /**
     * Retrieves the API Image URL.
     *
     * @return the API Image URL
     */
    public String getApiImageUrl() {
        return UtilMethods.isEmpty(apiImageUrl)? "https://api.openai.com/v1/images/generations" : apiImageUrl;
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
    public String getImageModel() {return imageModel;}

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
    public String getModel() {
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

}