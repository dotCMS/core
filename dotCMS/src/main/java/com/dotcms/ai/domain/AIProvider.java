package com.dotcms.ai.domain;

/**
 * Enumeration representing different AI service providers.
 *
 * <p>
 * This enum defines various AI service providers that can be used within the application.
 * Each provider is associated with a specific name that identifies the AI service.
 * </p>
 *
 * <p>
 * The providers can be used to configure and manage interactions with different AI services,
 * allowing for flexible integration and switching between multiple AI providers.
 * </p>
 *
 * @author vico
 */
public enum AIProvider {

    NONE("None"),
    OPEN_AI("OpenAI"),
    BEDROCK("Amazon Bedrock"),
    GEMINI("Google Gemini");

    private final String provider;

    AIProvider(final String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

}
