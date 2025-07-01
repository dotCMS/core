package com.dotcms.ai.app;

import com.dotcms.ai.domain.Model;
import com.dotcms.ai.domain.ModelStatus;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.ai.model.OpenAIModel;
import com.dotcms.ai.model.OpenAIModels;
import com.dotcms.ai.model.SimpleModel;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Manages the AI models used in the application. This class handles loading, caching,
 * and retrieving AI models based on the host and model type. It also fetches supported
 * models from external sources and maintains a cache of these models.
 *
 * @author vico
 */
public class AIModels implements EventSubscriber<SystemTableUpdatedKeyEvent> {

    private static final String SUPPORTED_MODELS_KEY = "supportedModels";
    private static final String AI_MODELS_FETCH_ATTEMPTS_KEY = "ai.models.fetch.attempts";
    private static final String AI_MODELS_FETCH_TIMEOUT_KEY = "ai.models.fetch.timeout";
    private static final String AI_MODELS_API_URL_KEY = "AI_MODELS_API_URL";
    private static final String AI_MODELS_API_URL_DEFAULT = "https://api.openai.com/v1/models";
    private static final int AI_MODELS_CACHE_TTL = 28800;  // 8 hours
    private static final int AI_MODELS_CACHE_SIZE = 256;
    private static final Lazy<AIModels> INSTANCE = Lazy.of(AIModels::new);
    public static final String BEARER = "Bearer ";

    private final ConcurrentMap<String, List<Tuple2<AIModelType, AIModel>>> internalModels;
    private final ConcurrentMap<Tuple3<String, Model, AIModelType>, AIModel> modelsByName;
    private final Cache<String, Set<String>> supportedModelsCache;
    private final AtomicInteger modelsFetchAttempts;
    private final AtomicLong modelsFetchTimeout;
    private final AtomicReference<String> aiModelsApiUrl;

    public static AIModels get() {
        return INSTANCE.get();
    }

    private static int resolveModelsFetchAttempts() {
        return Config.getIntProperty(AI_MODELS_FETCH_ATTEMPTS_KEY, 90);
    }

    private static long resolveModelsFetchTimeout() {
        return Config.getLongProperty(AI_MODELS_FETCH_TIMEOUT_KEY, 4000);
    }

    private static String resolveAiModelsApiUrl() {
        return Config.getStringProperty(AI_MODELS_API_URL_KEY, AI_MODELS_API_URL_DEFAULT);
    }

    private AIModels() {
        internalModels = new ConcurrentHashMap<>();
        modelsByName = new ConcurrentHashMap<>();
        supportedModelsCache =
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(AI_MODELS_CACHE_TTL))
                        .maximumSize(AI_MODELS_CACHE_SIZE)
                        .build();
        modelsFetchAttempts = new AtomicInteger(resolveModelsFetchAttempts());
        modelsFetchTimeout = new AtomicLong(resolveModelsFetchTimeout());
        aiModelsApiUrl = new AtomicReference<>(resolveAiModelsApiUrl());
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, this);
    }

    /**
     * Loads the given list of AI models for the specified host. If models for the host
     * are already loaded, this method does nothing. It also maps model names to their
     * corresponding AIModel instances.
     *
     * @param appConfig app config
     * @param loading the list of AI models to load
     */
    public void loadModels(final AppConfig appConfig, final List<AIModel> loading) {
        final String host = appConfig.getHost();

       final List<Tuple2<AIModelType, AIModel>> currentModels = internalModels.get(host);

        if (UtilMethods.isSet(currentModels)) {
            for (AIModel loadingAIModel : loading) {
                final AIModelType type = loadingAIModel.getType();

                for (Tuple2<AIModelType, AIModel> currentTupla : currentModels) {
                    if(type == currentTupla._1()) {
                        final AIModel currentAIModel = currentTupla._2;

                        for (Model currentModel : currentAIModel.getModels()) {
                            final Optional<Model> optionalModel = loadingAIModel.getModels().stream()
                                    .filter(model -> model.getName().equals(currentModel.getName()))
                                    .findFirst();

                            if (optionalModel.isPresent() && optionalModel.get().getStatus() == null) {
                                optionalModel.get().setStatus(currentModel.getStatus());
                            }
                        }
                    }
                }
            }
        }


        final List<Tuple2<AIModelType, AIModel>> added = internalModels.put(
                host,
                loading.stream()
                        .map(model -> Tuple.of(model.getType(), model))
                        .collect(Collectors.toList()));

        loading.forEach(aiModel -> aiModel
                .getModels()
                .forEach(model -> {
                    final Tuple3<String, Model, AIModelType> key = Tuple.of(host, model, aiModel.getType());
                    modelsByName.put(key, aiModel);
                }));

        if (added == null) {
            activateModels(host);
        }
    }

    /**
     * Finds an AI model by the host and model name. The search is case-insensitive.
     *
     * @param appConfig the AppConfig for the host
     * @param modelName the name of the model to find
     * @param type the type of the model to find
     * @return an Optional containing the found AIModel, or an empty Optional if not found
     */
    public Optional<AIModel> findModel(final AppConfig appConfig,
                                       final String modelName,
                                       final AIModelType type) {
        final String lowered = modelName.toLowerCase();
        return Optional.ofNullable(
                modelsByName.get(
                        Tuple.of(
                                appConfig.getHost(),
                                Model.builder().withName(lowered).build(),
                                type)));
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
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     *
     * @param host the host for which the model is being resolved
     * @param type the type of the model to find
     */
    public AIModel resolveModel(final String host, final AIModelType type) {
        return findModel(host, type).orElse(AIModel.NOOP_MODEL);
    }

    /**
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     *
     * @param appConfig the AppConfig for the host
     * @param modelName the name of the model to find
     * @param type the type of the model to find
     */
    public AIModel resolveAIModelOrThrow(final AppConfig appConfig, final String modelName, final AIModelType type) {
        return findModel(appConfig, modelName, type)
                .orElseThrow(() -> new DotAIModelNotFoundException(
                            String.format("Unable to find model: [%s] of type [%s].", modelName, type)));
    }

    /**
     * Resolves a model-specific secret value from the provided secrets map using the specified key and model type.
     * If the model is not found or is not operational, it throws an appropriate exception.
     *
     * @param appConfig the AppConfig for the host
     * @param modelName the name of the model to find
     * @param type the type of the model to find
     * @return  a Tuple2 containing the AIModel and the Model
     */
    public Tuple2<AIModel, Model> resolveModelOrThrow(final AppConfig appConfig,
                                                      final String modelName,
                                                      final AIModelType type) {
        final AIModel aiModel = resolveAIModelOrThrow(appConfig, modelName, type);
        return Tuple.of(aiModel, aiModel.getModel(modelName));
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
        cleanSupportedModelsCache();
    }

    /**
     * Retrieves the list of supported models, either from the cache or by fetching them
     * from an external source if the cache is empty or expired.
     *
     * @return a set of supported model names
     */
    public Set<String> getOrPullSupportedModels(final AppConfig appConfig) {
        final Set<String> cached = supportedModelsCache.getIfPresent(SUPPORTED_MODELS_KEY);
        if (CollectionUtils.isNotEmpty(cached)) {
            return cached;
        }

        if (!appConfig.isEnabled()) {
            appConfig.debugLogger(getClass(), () -> "dotAI is not enabled, returning empty set of supported models");
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
        return internalModels.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .map(Tuple2::_2)
                .filter(AIModel::isOperational)
                .flatMap(aiModel -> aiModel.getModels()
                        .stream()
                        .filter(Model::isOperational)
                        .map(model -> new SimpleModel(
                                model.getName(),
                                aiModel.getType(),
                                aiModel.getCurrentModelIndex() == model.getIndex())))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        Logger.info(this, String.format("Notify for event [%s]", event.getKey()));
        if (event.getKey().contains(AI_MODELS_FETCH_ATTEMPTS_KEY)) {
            modelsFetchAttempts.set(resolveModelsFetchAttempts());
        } else if (event.getKey().contains(AI_MODELS_FETCH_TIMEOUT_KEY)) {
            modelsFetchTimeout.set(Config.getIntProperty(AI_MODELS_FETCH_TIMEOUT_KEY, 14));
        } else if (event.getKey().contains(AI_MODELS_API_URL_KEY)) {
            aiModelsApiUrl.set(resolveAiModelsApiUrl());
        }
    }

    @VisibleForTesting
    void cleanSupportedModelsCache() {
        supportedModelsCache.invalidate(SUPPORTED_MODELS_KEY);
    }

    private CircuitBreakerUrl.Response<OpenAIModels> fetchOpenAIModels(final AppConfig appConfig) {
        final CircuitBreakerUrl.Response<OpenAIModels> response = CircuitBreakerUrl.builder()
                .setMethod(CircuitBreakerUrl.Method.GET)
                .setUrl(aiModelsApiUrl.get())
                .setTimeout(modelsFetchTimeout.get())
                .setTryAgainAttempts(modelsFetchAttempts.get())
                .setAuthHeaders(BEARER + appConfig.getApiKey())
                .setThrowWhenError(true)
                .build()
                .doResponse(OpenAIModels.class);

        if (response.getStatusCode() == 401) {
            throw new InvalidAIKeyException("AI key authentication failed. Please ensure the key is valid, active, and correctly configured.");
        } else if (!CircuitBreakerUrl.isSuccessResponse(response)) {
            appConfig.debugLogger(
                    AIModels.class,
                    () -> String.format(
                            "Error fetching OpenAI supported models from [%s] (status code: [%d])",
                            aiModelsApiUrl.get(),
                            response.getStatusCode()));
            throw new DotRuntimeException("Error fetching OpenAI supported models");
        }

        return response;
    }

    private void activateModels(final String host) {
        final List<AIModel> aiModels = internalModels.get(host)
                .stream()
                .map(tuple -> tuple._2)
                .collect(Collectors.toList());

        aiModels.forEach(aiModel ->
            aiModel.getModels().forEach(model -> {
                final String modelName = model.getName().trim().toLowerCase();
                final ModelStatus status = ModelStatus.ACTIVE;
                if (aiModel.getCurrentModelIndex() == AIModel.NOOP_INDEX) {
                    aiModel.setCurrentModelIndex(model.getIndex());
                }
                Logger.debug(
                        this,
                        String.format("Model [%s] is supported by OpenAI, marking it as [%s]", modelName, status));
                model.setStatus(status);
            }));
    }

}
