package com.dotcms.ai.validator;

import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotmarketing.util.DateUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * The AIAppValidator class is responsible for validating AI configurations and model usage.
 * It ensures that the AI models specified in the application configuration are supported
 * and not exhausted.
 *
 * @author vico
 */
public class AIAppValidator {

    private static final Lazy<AIAppValidator> INSTANCE = Lazy.of(AIAppValidator::new);

    private SystemMessageEventUtil systemMessageEventUtil;

    private AIAppValidator() {
        setSystemMessageEventUtil(SystemMessageEventUtil.getInstance());
    }

    public static AIAppValidator get() {
        return INSTANCE.get();
    }

    /**
     * Validates the AI configuration for the specified user.
     * If the user ID is null, the validation is skipped.
     * Checks if the models specified in the application configuration are supported.
     * If any unsupported models are found, a warning message is pushed to the user.
     *
     * @param appConfig the application configuration
     * @param userId the user ID
     */
    public void validateAIConfig(final AppConfig appConfig, final String userId) {
        if (Objects.isNull(userId)) {
            AppConfig.debugLogger(getClass(), () -> "User Id is null, skipping AI configuration validation");
            return;
        }

        // TODO: pr-split -> uncomment this lines
        /*final Set<String> supportedModels = AIModels.get().getOrPullSupportedModels(appConfig.getApiKey());
        final Set<String> unsupportedModels = Stream.of(
                        appConfig.getModel(),
                        appConfig.getImageModel(),
                        appConfig.getEmbeddingsModel())
                .flatMap(aiModel -> aiModel.getModels().stream())
                .map(Model::getName)
                .filter(model -> !supportedModels.contains(model))
                .collect(Collectors.toSet());*/
        final Set<String> supportedModels = Set.of();
        final Set<String> unsupportedModels = Set.of();
        if (unsupportedModels.isEmpty()) {
            return;
        }

        final String unsupported = String.join(", ", unsupportedModels);
        final String message = Try
                .of(() -> LanguageUtil.get("ai.unsupported.models", unsupported))
                .getOrElse(String.format("The following models are not supported: [%s]", unsupported));
        final SystemMessage systemMessage = new SystemMessageBuilder()
                .setMessage(message)
                .setSeverity(MessageSeverity.WARNING)
                .setLife(DateUtil.SEVEN_SECOND_MILLIS)
                .create();

        systemMessageEventUtil.pushMessage(systemMessage, Collections.singletonList(userId));
    }

    /**
     * Validates the usage of AI models for the specified user.
     * If the user ID is null, the validation is skipped.
     * Checks if the models specified in the AI model are exhausted or invalid.
     * If any exhausted or invalid models are found, a warning message is pushed to the user.
     *
     * @param aiModel the AI model
     * @param userId the user ID
     */
    public void validateModelsUsage(final AIModel aiModel, final String userId) {
        if (Objects.isNull(userId)) {
            AppConfig.debugLogger(getClass(), () -> "User Id is null, skipping AI models usage validation");
            return;
        }

        // TODO: pr-split -> uncomment this line
        /*final String unavailableModels = aiModel.getModels()
                .stream()
                .map(Model::getName)
                .collect(Collectors.joining(", "));*/
        final String unavailableModels = "";
        final String message = Try
                .of(() -> LanguageUtil.get("ai.models.exhausted", aiModel.getType(), unavailableModels)).
                getOrElse(
                        String.format(
                                "All the %s models: [%s] have been exhausted since they are invalid or has been decommissioned",
                                aiModel.getType(),
                                unavailableModels));
        final SystemMessage systemMessage = new SystemMessageBuilder()
                .setMessage(message)
                .setSeverity(MessageSeverity.WARNING)
                .setLife(DateUtil.SEVEN_SECOND_MILLIS)
                .create();

        systemMessageEventUtil.pushMessage(systemMessage, Collections.singletonList(userId));
    }

    @VisibleForTesting
    void setSystemMessageEventUtil(SystemMessageEventUtil systemMessageEventUtil) {
        this.systemMessageEventUtil = systemMessageEventUtil;
    }

}
