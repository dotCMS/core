package com.dotcms.ai.app;

import io.vavr.Lazy;
import io.vavr.Tuple2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the AI models used in the application. This class handles loading, caching,
 * and retrieving AI models based on the host and model type.
 *
 * @author vico
 */
public class AIModels {

    private static final Lazy<AIModels> INSTANCE = Lazy.of(AIModels::new);

    private final ConcurrentMap<String, List<Tuple2<AIModelType, AIModel>>> internalModels;

    public static AIModels get() {
        return INSTANCE.get();
    }

    private AIModels() {
        internalModels = new ConcurrentHashMap<>();
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

}
