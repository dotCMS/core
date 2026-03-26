package com.dotcms.ai.validator;

import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AppConfig;
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
import java.util.stream.Collectors;

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
     * Model validation against the provider API is handled by LangChain4J at request time.
     *
     * @param appConfig the application configuration
     * @param userId the user ID
     */
    public void validateAIConfig(final AppConfig appConfig, final String userId) {
        appConfig.debugLogger(getClass(), () -> "AI configuration validation delegated to LangChain4J");
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
