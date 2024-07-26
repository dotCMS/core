package com.dotcms.ai.app;

import com.dotcms.ai.model.OpenAIModel;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AIModels {

    private static final String SUPPORTED_MODELS_KEY = "supportedModels";
    private static final String AI_MODELS_FETCH_ATTEMPTS_KEY = "ai.models.fetch.attempts";
    private static final int AI_MODELS_FETCH_ATTEMPTS = Config.getIntProperty(AI_MODELS_FETCH_ATTEMPTS_KEY, 3);
    private static final String AI_MODELS_FETCH_TIMEOUT_KEY = "ai.models.fetch.timeout";
    private static final int AI_MODELS_FETCH_TIMEOUT = Config.getIntProperty(AI_MODELS_FETCH_TIMEOUT_KEY, 4000);
    private static final Lazy<AIModels> INSTANCE = Lazy.of(AIModels::new);

    public static final AIModel NOOP_MODEL = AIModel.builder().withNames(List.of()).build();

    private final ConcurrentMap<String, List<Tuple2<AIModelType, AIModel>>> internalModels = new ConcurrentHashMap<>();
    private final ConcurrentMap<Tuple2<String, String>, AIModel> modelsByName = new ConcurrentHashMap<>();

    private final Cache<String, List<String>> supportedModelsCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(
                            Duration.ofSeconds(
                                    ConfigService.INSTANCE
                                            .config()
                                            .getConfigInteger(AppKeys.AI_MODELS_CACHE_TTL)))
                    .maximumSize(ConfigService.INSTANCE.config().getConfigInteger(AppKeys.AI_MODELS_CACHE_SIZE))
                    .build();

    public static AIModels get() {
        return INSTANCE.get();
    }

    private AIModels() {
    }

    public void loadModels(final String host, final List<AIModel> loading) {
        Optional.ofNullable(internalModels.get(host))
                .ifPresentOrElse(
                        model -> {},
                        () -> internalModels.putIfAbsent(
                                host,
                                loading.stream()
                                        .map(model -> Tuple.of(model.getType(), model))
                                        .collect(Collectors.toList())));
        loading.forEach(model -> model
                .getNames()
                .forEach(name -> {
                    final Tuple2<String, String> key = Tuple.of(
                            host.toLowerCase(),
                            name.toLowerCase().trim());
                    if (modelsByName.containsKey(key)) {
                        Logger.debug(
                                this,
                                String.format(
                                        "Model [%s] already exists for host [%s], ignoring it",
                                        name,
                                        host));
                        return;
                    }
                    modelsByName.putIfAbsent(key, model);
                }));
    }

    public Optional<AIModel> findModel(final String host, final String modelName) {
        return Optional.ofNullable(modelsByName.get(Tuple.of(host, modelName.toLowerCase())));
    }

    public Optional<AIModel> findModel(final String host, final AIModelType type) {
        return Optional.ofNullable(internalModels.get(host))
                .flatMap(tuples -> tuples.stream()
                        .filter(tuple -> tuple._1 == type)
                        .map(Tuple2::_2)
                        .findFirst());
    }

    public List<String> getOrPullSupportedModels() {
        final List<String> cached = supportedModelsCache.getIfPresent(SUPPORTED_MODELS_KEY);
        if (CollectionUtils.isNotEmpty(cached)) {
            return cached;
        }

        final AppConfig appConfig = ConfigService.INSTANCE.config();
        return Try.of(() ->
                        Stream
                            .of(fetchOpenAIModels(appConfig).getResponse())
                            .map(OpenAIModel::getId)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()))
                .getOrElse(Optional.ofNullable(cached).orElse(List.of()));
    }

    private static CircuitBreakerUrl.Response<OpenAIModel[]> fetchOpenAIModels(final AppConfig appConfig) {
        final String url = appConfig.getConfig(AppKeys.OPEN_AI_MODELS_URL);

        final CircuitBreakerUrl.Response<OpenAIModel[]> response = CircuitBreakerUrl.builder()
                .setMethod(CircuitBreakerUrl.Method.GET)
                .setUrl(url)
                .setTimeout(AI_MODELS_FETCH_TIMEOUT)
                .setTryAgainAttempts(AI_MODELS_FETCH_ATTEMPTS)
                .setHeaders(CircuitBreakerUrl.authHeaders("Bearer " + appConfig.getApiKey()))
                .setThrowWhenNot2xx(false)
                .build()
                .doResponse(OpenAIModel[].class);

        if (!CircuitBreakerUrl.isSuccessResponse(response)) {
            Logger.debug(
                    AIModels.class,
                    String.format(
                            "Error fetching OpenAI supported models from [%s] (status code: [%d])",
                            url,
                            response.getStatusCode()));
        }

        return response;
    }

}
