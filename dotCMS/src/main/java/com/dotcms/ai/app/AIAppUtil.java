package com.dotcms.ai.app;

import com.dotcms.security.apps.AppsUtil;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AIAppUtil {

    private static final Lazy<AIAppUtil> INSTANCE = Lazy.of(AIAppUtil::new);

    private AIAppUtil() {
        // Private constructor to prevent instantiation
    }

    public static AIAppUtil get() {
        return INSTANCE.get();
    }

    public String resolveSecret(final Map<String, Secret> secrets, final AppKeys key, final String defaultValue) {
        return Try.of(() -> secrets.get(key.key).getString()).getOrElse(defaultValue);
    }

    public String resolveSecret(final Map<String, Secret> secrets, final AppKeys key) {
        return resolveSecret(secrets, key, key.defaultValue);
    }

    public String resolveModelSecret(final Map<String, Secret> secrets, final AppKeys key, final AIModelType type) {
        return resolveSecret(secrets, prefixKey(key, type));
    }

    public String resolveEnvSecret(final Map<String, Secret> secrets, final AppKeys key) {
        final String secret = resolveSecret(secrets, key, StringPool.BLANK);
        if (UtilMethods.isSet(secret)) {
            return secret;
        }

        return Optional
                .ofNullable(AppsUtil.discoverEnvVarValue(AppKeys.APP_KEY, key.key, null))
                .orElse(StringPool.BLANK);
    }

    public String normalizeModel(final String model) {
        return Optional.ofNullable(model).map(String::trim).map(String::toLowerCase).orElse(null);
    }

    public List<String> splitModels(final String models) {
        return new ArrayList<>(
                new LinkedHashSet<>(
                        Stream.of(models.split(StringPool.COMMA))
                                .map(this::normalizeModel)
                                .collect(Collectors.toList())));
    }

    public AIModel createModel(final Map<String, Secret> secrets, final AIModelType type) {
        return AIModel.builder()
                .withType(type)
                .withNames(splitModels(resolveModelSecret(secrets, AppKeys.MODEL_NAMES, type)))
                .withTokensPerMinute(toInt(resolveModelSecret(secrets, AppKeys.MODEL_TOKENS_PER_MINUTE, type)))
                .withApiPerMinute(toInt(resolveModelSecret(secrets, AppKeys.MODEL_API_PER_MINUTE, type)))
                .withMaxTokens(toInt(resolveModelSecret(secrets, AppKeys.MODEL_MAX_TOKENS, type)))
                .withIsCompletion(Boolean.parseBoolean(resolveModelSecret(secrets, AppKeys.MODEL_COMPLETION, type)))
                .build();
    }

    private int toInt(final String value) {
        return Try.of(() -> Integer.parseInt(value)).getOrElse(0);
    }

    private AppKeys prefixKey(final AppKeys key, final AIModelType type) {
        return AppKeys.valueOf(UtilMethods.isSet(type) ? type.name() + '_' + key.key : key.key);
    }

}
