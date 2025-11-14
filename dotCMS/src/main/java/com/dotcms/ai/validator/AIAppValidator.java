package com.dotcms.ai.validator;

import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModels;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.InvalidAIKeyException;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.domain.Model;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            appConfig.debugLogger(getClass(), () -> "User Id is null, skipping AI configuration validation");
            return;
        }

        try {
            final Set<String> supportedModels = AIModels.get().getOrPullSupportedModels(appConfig);
            final Set<String> unsupportedModels = Stream.of(
                            appConfig.getModel(),
                            appConfig.getImageModel(),
                            appConfig.getEmbeddingsModel())
                    .flatMap(aiModel -> aiModel.getModels().stream())
                    .map(Model::getName)
                    .filter(model -> !supportedModels.contains(model))
                    .collect(Collectors.toSet());
            if (unsupportedModels.isEmpty()) {
                return;
            }

            sendUnsupportedModelsNotification(userId, unsupportedModels);
        } catch (InvalidAIKeyException e) {
            sendInvalidAIKeyNotification(userId);  
        }
    }

    private void sendInvalidAIKeyNotification(final String userId) {
        final String message = Try
                .of(() -> LanguageUtil.get("ai.key.invalid"))
                .getOrElse("AI key authentication failed. Please ensure the key is valid, active, and correctly configured.");

        final SystemMessage systemMessage = new SystemMessageBuilder()
                .setMessage(message)
                .setSeverity(MessageSeverity.ERROR)
                .setLife(DateUtil.SEVEN_SECOND_MILLIS)
                .create();

        systemMessageEventUtil.pushMessage(systemMessage, Collections.singletonList(userId));
    }

    /**
     * Sends a notification to the specified user about unsupported AI models.
     * Creates a warning message containing the names of the unsupported models
     * and pushes it to the user's notification system.
     *
     * @param userId the ID of the user to receive the notification
     * @param unsupportedModels a set of model names that are not supported
     */
    private void sendUnsupportedModelsNotification(String userId, Set<String> unsupportedModels) {
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
     * @param request the ai request
     */
    public void validateModelsUsage(final AIModel aiModel, final JSONObjectAIRequest request) {
        if (Objects.isNull(request.getUserId())) {
            request.getConfig().debugLogger(getClass(), () -> "User Id is null, skipping AI models usage validation");
            return;
        }

        final String unavailableModels = aiModel.getModels()
                .stream()
                .map(Model::getName)
                .collect(Collectors.joining(", "));
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

        systemMessageEventUtil.pushMessage(systemMessage, Collections.singletonList(request.getUserId()));
    }

    @VisibleForTesting
    void setSystemMessageEventUtil(SystemMessageEventUtil systemMessageEventUtil) {
        this.systemMessageEventUtil = systemMessageEventUtil;
    }

}
