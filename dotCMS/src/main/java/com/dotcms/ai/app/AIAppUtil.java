package com.dotcms.ai.app;

import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiModelConfigCatalog;
import com.dotcms.ai.config.AiVendor;
import com.dotcms.security.apps.AppsUtil;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for handling AI application configurations and secrets.
 * This class provides methods to resolve secrets, normalize model names,
 * split model names, and create AI model instances based on the provided
 * configuration and secrets.
 *
 * @author vico
 */
public class AIAppUtil {

    private static final Lazy<AIAppUtil> INSTANCE = Lazy.of(AIAppUtil::new);

    private AIAppUtil() {
        // Private constructor to prevent instantiation
    }

    public static AIAppUtil get() {
        return INSTANCE.get();
    }

    /**
     * Creates a text model instance based on the provided secrets.
     *
     * @param secrets the map of secrets
     * @return the created text model instance
     */
    public AIModel createTextModel(final Map<String, Secret> secrets) {

        List<String> modelNames = getModelNamesFromCatalogs(secrets, AppKeys.ADVANCE_PROVIDER_SETTINGS_KEY);
        int tokensPerMinute = 0;

        if (UtilMethods.isSet(modelNames)) {

            // it is hardcoded by now to open ai.
            final AiModelConfig modelConfig = modelConfigCatalogOpt.get().getChatConfig(AiVendor.OPEN_AI.getVendorName());
            if (modelConfig != null) {


            }
        }

        if (modelNames == null) {

            modelNames = splitDiscoveredSecret(secrets, AppKeys.TEXT_MODEL_NAMES);
            if (CollectionUtils.isEmpty(modelNames)) {
                return AIModel.NOOP_MODEL;
            }

            tokensPerMinute = discoverIntSecret(secrets, AppKeys.TEXT_MODEL_TOKENS_PER_MINUTE)
        }

        return AIModel.builder()
                .withType(AIModelType.TEXT)
                .withModelNames(modelNames)
                .withTokensPerMinute(tokensPerMinute)
                .withApiPerMinute(discoverIntSecret(secrets, AppKeys.TEXT_MODEL_API_PER_MINUTE))
                .withMaxTokens(discoverIntSecret(secrets, AppKeys.TEXT_MODEL_MAX_TOKENS))
                .withIsCompletion(discoverBooleanSecret(secrets, AppKeys.TEXT_MODEL_COMPLETION))
                .build();
    }

    /**
     * Creates an image model instance based on the provided secrets.
     *
     * @param secrets the map of secrets
     * @return the created image model instance
     */
    public AIModel createImageModel(final Map<String, Secret> secrets) {
        final List<String> modelNames = splitDiscoveredSecret(secrets, AppKeys.IMAGE_MODEL_NAMES);
        if (CollectionUtils.isEmpty(modelNames)) {
            return AIModel.NOOP_MODEL;
        }

        return AIModel.builder()
                .withType(AIModelType.IMAGE)
                .withModelNames(modelNames)
                .withTokensPerMinute(discoverIntSecret(secrets, AppKeys.IMAGE_MODEL_TOKENS_PER_MINUTE))
                .withApiPerMinute(discoverIntSecret(secrets, AppKeys.IMAGE_MODEL_API_PER_MINUTE))
                .withMaxTokens(discoverIntSecret(secrets, AppKeys.IMAGE_MODEL_MAX_TOKENS))
                .withIsCompletion(discoverBooleanSecret(secrets, AppKeys.IMAGE_MODEL_COMPLETION))
                .build();
    }

    /**
     * Creates an embeddings model instance based on the provided secrets.
     *
     * @param secrets the map of secrets
     * @return the created embeddings model instance
     */
    public AIModel createEmbeddingsModel(final Map<String, Secret> secrets) {
        final List<String> modelNames = splitDiscoveredSecret(secrets, AppKeys.EMBEDDINGS_MODEL_NAMES);
        if (CollectionUtils.isEmpty(modelNames)) {
            return AIModel.NOOP_MODEL;
        }

        return AIModel.builder()
                .withType(AIModelType.EMBEDDINGS)
                .withModelNames(modelNames)
                .withTokensPerMinute(discoverIntSecret(secrets, AppKeys.EMBEDDINGS_MODEL_TOKENS_PER_MINUTE))
                .withApiPerMinute(discoverIntSecret(secrets, AppKeys.EMBEDDINGS_MODEL_API_PER_MINUTE))
                .withMaxTokens(discoverIntSecret(secrets, AppKeys.EMBEDDINGS_MODEL_MAX_TOKENS))
                .withIsCompletion(discoverBooleanSecret(secrets, AppKeys.EMBEDDINGS_MODEL_COMPLETION))
                .build();
    }

    /**
     * Resolves a secret value from the provided secrets map using the specified key.
     * If the secret is not found, the default value is returned.
     *
     * @param secrets the map of secrets
     * @param key the key to look up the secret
     * @param defaultValue the default value to return if the secret is not found
     * @return the resolved secret value or the default value if the secret is not found
     */
    public String discoverSecret(final Map<String, Secret> secrets, final AppKeys key, final String defaultValue) {

        return Try.of(() -> secrets.get(key.key).getString()).getOrElse(defaultValue);
    }

    /**
     * Resolves a secret value from the provided secrets map using the specified key.
     * If the secret is not found, the default value defined in the key is returned.
     *
     * @param secrets the map of secrets
     * @param key the key to look up the secret
     * @return the resolved secret value or the default value defined in the key if the secret is not found
     */
    public String discoverSecret(final Map<String, Secret> secrets, final AppKeys key) {
        return discoverSecret(secrets, key, key.defaultValue);
    }

    /**
     * Splits a model-specific secret value from the provided secrets map using the specified key.
     *
     * @param secrets the map of secrets
     * @param key the key to look up the secret
     * @return the list of split secret values
     */
    public List<String> splitDiscoveredSecret(final Map<String, Secret> secrets, final AppKeys key) {
        return Arrays
                .stream(Optional
                        .ofNullable(discoverSecret(secrets, key))
                        .map(secret -> secret.split(StringPool.COMMA))
                        .orElse(new String[0]))
                .collect(Collectors.toList());
    }

    /**
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     *
     * @param secrets the map of secrets
     * @param key the key to look up the secret
     */
    public int discoverIntSecret(final Map<String, Secret> secrets, final AppKeys key) {
        return toInt(discoverSecret(secrets, key));
    }

    /**
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     *
     * @param secrets the map of secrets
     * @param key the key to look up the secret
     */
    public boolean discoverBooleanSecret(final Map<String, Secret> secrets, final AppKeys key) {
        return Boolean.parseBoolean(discoverSecret(secrets, key));
    }

    /**
     * Resolves a secret value from the provided secrets map using the specified key and environment variable.
     * If the secret is not found in the secrets map, it attempts to discover the value from the environment variable.
     *
     * @param secrets the map of secrets
     * @param key the key to look up the secret
     * @param envVar the environment variable name to look up if the secret is not found in the secrets map
     * @return the resolved secret value or the value from the environment variable if the secret is not found
     */
    public String discoverEnvSecret(final Map<String, Secret> secrets, final AppKeys key, final String envVar) {
        return Optional
                .ofNullable(AppsUtil.discoverEnvVarValue(AppKeys.APP_KEY, key.key, envVar))
                .orElseGet(() -> {
                    final String secret = discoverSecret(secrets, key);
                    return UtilMethods.isSet(secret) ? secret : null;
                });
    }

    private int toInt(final String value) {
        return Try.of(() -> Integer.parseInt(value)).getOrElse(0);
    }

}
