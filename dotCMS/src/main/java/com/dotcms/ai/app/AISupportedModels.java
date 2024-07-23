package com.dotcms.ai.app;

import com.dotcms.ai.model.OpenAIModel;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AISupportedModels {

    private static final String key = "supportedModels";
    private static final String AI_MODELS_FETCH_ATTEMPTS_KEY = "ai.models.fetch.attempts";
    private static final int AI_MODELS_FETCH_ATTEMPTS = Config.getIntProperty(AI_MODELS_FETCH_ATTEMPTS_KEY, 3);
    private static final String AI_MODELS_FETCH_TIMEOUT_KEY = "ai.models.fetch.timeout";
    private static final int AI_MODELS_FETCH_TIMEOUT = Config.getIntProperty(AI_MODELS_FETCH_TIMEOUT_KEY, 4000);
    private static final Lazy<AISupportedModels> INSTANCE = Lazy.of(AISupportedModels::new);

    private final Cache<String, List<String>> supportedModelsCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(
                            Duration.ofSeconds(
                                    ConfigService.INSTANCE
                                            .config()
                                            .getConfigInteger(AppKeys.AI_MODELS_CACHE_TTL)))
                    .maximumSize(ConfigService.INSTANCE.config().getConfigInteger(AppKeys.AI_MODELS_CACHE_SIZE))
                    .build();

    private AISupportedModels() {
        // Private constructor to prevent instantiation
    }

    public static AISupportedModels get() {
        return INSTANCE.get();
    }

    public List<String> getOrPullModels() {
        final List<String> cached = supportedModelsCache.getIfPresent(key);
        if (CollectionUtils.isNotEmpty(cached)) {
            return cached;
        }

        final AppConfig appConfig = ConfigService.INSTANCE.config();
        return Try
                .of(() -> Stream
                        .of(fetchOpenAIModels(appConfig).getResponse())
                        .map(OpenAIModel::getId)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()))
                .getOrElse(Optional.ofNullable(cached).orElse(List.of()));
    }

    private CircuitBreakerUrl.Response<OpenAIModel[]> fetchOpenAIModels(final AppConfig appConfig) {
        final String url = appConfig.getConfig(AppKeys.OPEN_AI_MODELS_URL);
        final CircuitBreakerUrl.Response<OpenAIModel[]> response = CircuitBreakerUrl.builder()
                .setMethod(CircuitBreakerUrl.Method.GET)
                .setUrl(appConfig.getConfig(AppKeys.OPEN_AI_MODELS_URL))
                .setTimeout(AI_MODELS_FETCH_TIMEOUT)
                .setTryAgainAttempts(AI_MODELS_FETCH_ATTEMPTS)
                .setHeaders(CircuitBreakerUrl.authHeaders("Bearer " + appConfig.getApiKey()))
                .setThrowWhenNot2xx(false)
                .build()
                .doResponse(OpenAIModel[].class);

        Logger.debug(this, String.format(
                "Error requesting analytics key from analytics config server %s (status code: %d)",
                url,
                response.getStatusCode()));

        return response;
    }

}
