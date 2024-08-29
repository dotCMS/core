package com.dotcms.ai.app;

import com.dotcms.ai.model.OpenAIModel;
import com.dotcms.ai.model.OpenAIModels;
import com.dotcms.ai.model.SimpleModel;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Manages the AI models used in the application. This class handles loading, caching,
 * and retrieving AI models based on the host and model type. It also fetches supported
 * models from external sources and maintains a cache of these models.
 *
 * @author vico
 */
public class AIModels {

    private static final String SUPPORTED_MODELS_KEY = "supportedModels";
    private static final String AI_MODELS_FETCH_ATTEMPTS_KEY = "ai.models.fetch.attempts";
    private static final int AI_MODELS_FETCH_ATTEMPTS = Config.getIntProperty(AI_MODELS_FETCH_ATTEMPTS_KEY, 3);
    private static final String AI_MODELS_FETCH_TIMEOUT_KEY = "ai.models.fetch.timeout";
    private static final int AI_MODELS_FETCH_TIMEOUT = Config.getIntProperty(AI_MODELS_FETCH_TIMEOUT_KEY, 4000);
    private static final Lazy<AIModels> INSTANCE = Lazy.of(AIModels::new);
    private static final String AI_MODELS_API_URL_KEY = "DOT_AI_MODELS_API_URL";
    private static final String AI_MODELS_API_URL = Config.getStringProperty(
            AI_MODELS_API_URL_KEY,
            "https://api.openai.com/v1/models");
    private static final int AI_MODELS_CACHE_TTL = 28800;  // 8 hours
    private static final int AI_MODELS_CACHE_SIZE = 128;

    private final ConcurrentMap<String, List<Tuple2<AIModelType, AIModel>>> internalModels = new ConcurrentHashMap<>();
    private final ConcurrentMap<Tuple2<String, String>, AIModel> modelsByName = new ConcurrentHashMap<>();
    private final Cache<String, Set<String>> supportedModelsCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(AI_MODELS_CACHE_TTL))
                    .maximumSize(AI_MODELS_CACHE_SIZE)
                    .build();
    private Supplier<AppConfig> appConfigSupplier = ConfigService.INSTANCE::config;

    public static AIModels get() {
        return INSTANCE.get();
    }

    private AIModels() {
    }

    /**
     * Loads the given list of AI models for the specified host. If models for the host
     * are already loaded, this method does nothing. It also maps model names to their
     * corresponding AIModel instances.
     *
     * @param host the host for which the models are being loaded
     * @param loading the list of AI models to load
     */
    public void loadModels(final String host, final List<AIModel> loading) {
        Optional.ofNullable(internalModels.get(host))
                .ifPresentOrElse(
                        model -> {},
                        () -> internalModels.putIfAbsent(
                                host,
                                loading.stream()
                                        .map(model -> Tuple.of(model.getType(), model))
                                        .collect(Collectors.toList())));
        loading.forEach(aiModel -> aiModel
                .getModels()
                .forEach(model -> {
                    final Tuple2<String, String> key = Tuple.of(
                            host,
                            model.getName().toLowerCase().trim());
                    if (modelsByName.containsKey(key)) {
                        Logger.debug(
                                this,
                                String.format(
                                        "Model [%s] already exists for host [%s], ignoring it",
                                        model.getName(),
                                        host));
                        return;
                    }
                    modelsByName.putIfAbsent(key, aiModel);
                }));
    }

    /**
     * Finds an AI model by the host and model name. The search is case-insensitive.
     *
     * @param host the host for which the model is being searched
     * @param modelName the name of the model to find
     * @return an Optional containing the found AIModel, or an empty Optional if not found
     */
    public Optional<AIModel> findModel(final String host, final String modelName) {
        final String lowered = modelName.toLowerCase();
        final Set<String> supported = getOrPullSupportedModels();
        return supported.contains(lowered)
                ? Optional.ofNullable(modelsByName.get(Tuple.of(host, lowered)))
                : Optional.empty();
    }

    /**
     * Finds an AI model by the host and model type.
     *
     * @param host the host for which the model is being searched
     * @param type the type of the model to find
     * @return an Optional containing the found AIModel, or an empty Optional if not found
     */
    public Optional<AIModel> findModel(final String host, final AIModelType type) {
        return Optional.ofNullable(internalModels.get(host))
                .flatMap(tuples -> tuples.stream()
                        .filter(tuple -> tuple._1 == type)
                        .map(Tuple2::_2)
                        .findFirst());
    }

    /**
     * Resets the internal models cache for the specified host.
     *
     * @param host the host for which the models are being reset
     */
    public void resetModels(final String host) {
        Optional.ofNullable(internalModels.get(host)).ifPresent(models -> {
            models.clear();
            internalModels.remove(host);
        });
        modelsByName.keySet()
                .stream()
                .filter(key -> key._1.equals(host))
                .collect(Collectors.toSet())
                .forEach(modelsByName::remove);
    }

    /**
     * Retrieves the list of supported models, either from the cache or by fetching them
     * from an external source if the cache is empty or expired.
     *
     * @return a set of supported model names
     */
    public Set<String> getOrPullSupportedModels() {
        final Set<String> cached = supportedModelsCache.getIfPresent(SUPPORTED_MODELS_KEY);
        if (CollectionUtils.isNotEmpty(cached)) {
            return cached;
        }

        final AppConfig appConfig = appConfigSupplier.get();
        if (!appConfig.isEnabled()) {
            AppConfig.debugLogger(getClass(), () -> "dotAI is not enabled, returning empty set of supported models");
            return Set.of();
        }

        final CircuitBreakerUrl.Response<OpenAIModels> response = fetchOpenAIModels(appConfig);
        if (Objects.nonNull(response.getResponse().getError())) {
            throw new DotRuntimeException("Found error in AI response: " + response.getResponse().getError().getMessage());
        }

        final Set<String> supported = response
                .getResponse()
                .getData()
                .stream()
                .map(OpenAIModel::getId)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        supportedModelsCache.put(SUPPORTED_MODELS_KEY, supported);

        return supported;
    }

    /**
     * Retrieves the list of available models that are both configured and supported.
     *
     * @return a list of available model names
     */
    public List<SimpleModel> getAvailableModels() {
        final Set<SimpleModel> configured = internalModels.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .map(Tuple2::_2)
                .flatMap(aiModel -> aiModel.getModels()
                        .stream()
                        .map(model -> new SimpleModel(model.getName(), aiModel.getType())))
                .collect(Collectors.toSet());
        final Set<SimpleModel> supported = getOrPullSupportedModels()
                .stream()
                .map(SimpleModel::new)
                .collect(Collectors.toSet());
        configured.retainAll(supported);

        return new ArrayList<>(configured);
    }

    private static CircuitBreakerUrl.Response<OpenAIModels> fetchOpenAIModels(final AppConfig appConfig) {
        final CircuitBreakerUrl.Response<OpenAIModels> response = CircuitBreakerUrl.builder()
                .setMethod(CircuitBreakerUrl.Method.GET)
                .setUrl(AI_MODELS_API_URL)
                .setTimeout(AI_MODELS_FETCH_TIMEOUT)
                .setTryAgainAttempts(AI_MODELS_FETCH_ATTEMPTS)
                .setHeaders(CircuitBreakerUrl.authHeaders("Bearer " + appConfig.getApiKey()))
                .setThrowWhenNot2xx(true)
                .build()
                .doResponse(OpenAIModels.class);

        if (!CircuitBreakerUrl.isSuccessResponse(response)) {
            Logger.debug(
                    AIModels.class,
                    String.format(
                            "Error fetching OpenAI supported models from [%s] (status code: [%d])",
                            AI_MODELS_API_URL,
                            response.getStatusCode()));
            throw new DotRuntimeException("Error fetching OpenAI supported models");
        }

        return response;
    }

    @VisibleForTesting
    void setAppConfigSupplier(final Supplier<AppConfig> appConfigSupplier) {
        this.appConfigSupplier = appConfigSupplier;
    }

    @VisibleForTesting
    void cleanSupportedModelsCache() {
        supportedModelsCache.invalidateAll();
    }

}
