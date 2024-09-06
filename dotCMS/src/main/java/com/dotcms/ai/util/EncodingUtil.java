package com.dotcms.ai.util;

import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.domain.ModelStatus;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import io.vavr.Lazy;

import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for handling encoding operations related to AI models.
 * It provides a registry for encoding and a lazy-loaded encoding instance based on the current model.
 * The class uses the ConfigService to retrieve the current model configuration.
 */
public class EncodingUtil {

    private static final Lazy<EncodingUtil> INSTANCE = Lazy.of(EncodingUtil::new);

    public final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    private EncodingUtil() {
    }

    public static EncodingUtil get() {
        return INSTANCE.get();
    }

    public Optional<Encoding> getEncoding(final AppConfig appConfig, final AIModelType type) {
        final AIModel aiModel = appConfig.resolveModel(type);
        final Model currentModel = aiModel.getCurrent();

        if (Objects.isNull(currentModel)) {
            AppConfig.debugLogger(
                    getClass(),
                    () -> String.format(
                            "No current model found for type [%s], meaning the are all are exhausted",
                            type));
            return Optional.empty();
        }

        return registry
                .getEncodingForModel(currentModel.getName())
                .or(() -> modelFallback(aiModel, currentModel));
    }

    public Optional<Encoding> getEncoding() {
        return getEncoding(ConfigService.INSTANCE.config(), AIModelType.EMBEDDINGS);
    }

    private Optional<Encoding> modelFallback(final AIModel aiModel,
                                             final Model currentModel) {
        AppConfig.debugLogger(
                getClass(),
                () -> String.format(
                        "Model [%s] is not suitable for encoding, marking it as invalid and falling back to other models",
                        currentModel.getName()));
        currentModel.setStatus(ModelStatus.INVALID);

        return aiModel.getModels()
                .stream()
                .filter(model -> !model.equals(currentModel))
                .map(model -> {
                    if (aiModel.getCurrentModelIndex() != currentModel.getIndex()) {
                        return null;
                    }

                    final Optional<Encoding> encoding = registry.getEncodingForModel(model.getName());
                    if (encoding.isEmpty()) {
                        model.setStatus(ModelStatus.INVALID);
                        AppConfig.debugLogger(
                                getClass(),
                                () -> String.format(
                                        "Model [%s] is not suitable for encoding, marking as invalid",
                                        model.getName()));
                        return null;
                    }

                    aiModel.setCurrentModelIndex(model.getIndex());
                    AppConfig.debugLogger(
                            getClass(),
                            () -> "Model [" + model.getName() + "] found, setting as current model");
                    return encoding.get();

                })
                .filter(Objects::nonNull)
                .findFirst();
    }

}
