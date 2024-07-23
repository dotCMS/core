package com.dotcms.ai.app;

import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.Lazy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIModels {

    private static final Lazy<AIModels> INSTANCE = Lazy.of(AIModels::new);

    private final ConcurrentMap<String, AIModel> aiModels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> models = new ConcurrentHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private AIModels() {
        // Private constructor to prevent instantiation
    }

    public static AIModels get() {
        return INSTANCE.get();
    }

    void loadModels(final List<AIModel> models, final boolean force) {
        if (hasLoaded() || force) {
            models.forEach(model -> {
                aiModels.put(model.getId(), model);
                model.getNames().forEach(name -> this.models.put(name, model.getId()));
            });
            loaded.compareAndSet(false, true);
        }
    }

    void loadModels(final List<AIModel> models) {
        loadModels(models, false);
    }

    public Optional<AIModel> getModelById(final String id) {
        return Optional.ofNullable(aiModels.get(id));
    }

    public AIModel getModelByName(final String modelName) {
        final String normalized = AIAppUtil.get().normalizeModel(modelName);
        return Optional
                .ofNullable(models.get(normalized))
                .flatMap(this::getModelById)
                .orElseThrow(() -> {
                    final String supported = String.join(", ", AISupportedModels.get().getOrPullModels());
                    return new DotRuntimeException(
                            "Unable to parse model: '" + modelName + "'.  Only [" + supported + "] are supported ");
                });
    }

    public boolean hasLoaded() {
        return loaded.get();
    }

}
