package com.dotcms.ai.domain;

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
