package com.dotcms.ai.validator;

import com.dotcms.ai.app.AppConfig;
import io.vavr.Lazy;

/**
 * The AIAppValidator class is responsible for validating AI configurations.
 *
 * @author vico
 */
public class AIAppValidator {

    private static final Lazy<AIAppValidator> INSTANCE = Lazy.of(AIAppValidator::new);

    private AIAppValidator() {}

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

}
