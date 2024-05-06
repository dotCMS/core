package com.dotcms.ai.app;

import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AppConfig implements Serializable {

    public final String model;
    public final String imageModel;
    private final String apiUrl;
    private final String apiImageUrl;
    private final String apiKey;
    private final String rolePrompt;
    private final String textPrompt;
    private final String imagePrompt;
    private final String imageSize;
    private final Map<String, Secret> configValues;

    public AppConfig(Map<String, Secret> secrets) {
        this.configValues = secrets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        apiUrl = Try.of(() -> secrets.get(AppKeys.API_URL.key).getString()).getOrElse(StringPool.BLANK);
        apiImageUrl = Try.of(() -> secrets.get(AppKeys.API_IMAGE_URL.key).getString()).getOrElse(StringPool.BLANK);
        apiKey = Try.of(() -> secrets.get(AppKeys.API_KEY.key).getString()).getOrElse(StringPool.BLANK);
        rolePrompt = Try.of(() -> secrets.get(AppKeys.ROLE_PROMPT.key).getString()).getOrElse(StringPool.BLANK);
        textPrompt = Try.of(() -> secrets.get(AppKeys.TEXT_PROMPT.key).getString()).getOrElse(StringPool.BLANK);
        imagePrompt = Try.of(() -> secrets.get(AppKeys.IMAGE_PROMPT.key).getString()).getOrElse(StringPool.BLANK);
        imageSize = Try.of(() -> secrets.get(AppKeys.IMAGE_SIZE.key).getString()).getOrElse(StringPool.BLANK);
        model = Try.of(() -> secrets.get(AppKeys.MODEL.key).getString()).getOrElse(StringPool.BLANK);
        imageModel = Try.of(() -> secrets.get(AppKeys.IMAGE_MODEL.key).getString()).getOrElse("dall-e-3");
        Logger.debug(this.getClass().getName(), () -> "apiUrl: " + apiUrl);
        Logger.debug(this.getClass().getName(), () -> "apiImageUrl: " + apiImageUrl);
        Logger.debug(this.getClass().getName(), () -> "apiKey: " + apiKey);
        Logger.debug(this.getClass().getName(), () -> "rolePrompt: " + rolePrompt);
        Logger.debug(this.getClass().getName(), () -> "textPrompt: " + textPrompt);
        Logger.debug(this.getClass().getName(), () -> "imagePrompt: " + imagePrompt);
        Logger.debug(this.getClass().getName(), () -> "imageModel: " + imageModel);
        Logger.debug(this.getClass().getName(), () -> "imageSize: " + imageSize);
        Logger.debug(this.getClass().getName(), () -> "model: " + model);

    }

    public static final Pattern SPLITTER= Pattern.compile("\\s?,\\s?");

    public String getApiUrl() {
        return UtilMethods.isEmpty(apiUrl) ? "https://api.openai.com/v1/chat/completions" : apiUrl;
    }

    public String getApiImageUrl() {
        return UtilMethods.isEmpty(apiImageUrl)? "https://api.openai.com/v1/images/generations" : apiImageUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getRolePrompt() {
        return rolePrompt;
    }

    public String getImageModel() {return imageModel;}

    public String getTextPrompt() {
        return textPrompt;
    }

    public String getImagePrompt() {
        return imagePrompt;
    }

    public String getImageSize() {
        return imageSize;
    }

    public String getModel() {
        return model;
    }

    public int getConfigInteger(AppKeys appKey) {
        String value =  Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(()->Integer.parseInt(value)).getOrElse(0);
    }

    public float getConfigFloat(AppKeys appKey) {
        String value =  Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(()->Float.parseFloat(value)).getOrElse(0f);
    }

    public boolean getConfigBoolean(AppKeys appKey) {
        String value =  Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        return Try.of(()->Boolean.parseBoolean(value)).getOrElse(false);
    }

    public String[] getConfigArray(AppKeys appKey) {
        String returnValue = getConfig(appKey);

        return returnValue != null ? SPLITTER.split(returnValue) : new String[0];

    }

    /**
     * this is needed to allow for custom config properties to be added to the APP
     * defaults for the values can
     *
     * @param appKey
     * @return
     */

    public String getConfig(AppKeys appKey) {
        if (configValues.containsKey(appKey.key)) {
            return Try.of(() -> configValues.get(appKey.key).getString()).getOrElse(appKey.defaultValue);
        }
        return appKey.defaultValue;
    }
}