package com.dotcms.ai.app;

import io.vavr.Lazy;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AIModels {

    private final ConcurrentMap<String, AIModel> aiModels = new ConcurrentHashMap<>();

    private static final Lazy<AIModels> INSTANCE = Lazy.of(AIModels::new);

    private static final AIModel NOOP_MODEL = new AIModel("noop", 0, 0, 0, false);

    private AIModels() {
        // Private constructor to prevent instantiation
    }

    public static AIModels get() {
        return INSTANCE.get();
    }

    public void loadModels(final List<AIModel> models) {
        aiModels.clear();

        models.forEach(model -> aiModels.put(model.getName(), model));
    }

    public AIModel getModel(final String modelName) {
        return aiModels.getOrDefault(modelName, NOOP_MODEL);
    }

    public boolean isValid(final AIModel aiModel) {
        return aiModel != NOOP_MODEL;
    }

}
