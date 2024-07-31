package com.dotcms.ai.model;

import com.dotmarketing.util.Config;

public enum AIProvider {

    OPEN_AI("OpenAI"),
    AMAZON_BEDROCK("Bedrock"),
    GOOGLE_GEMINI("Gemini"),;

    private final String provider;
    private final boolean enabled;

    AIProvider(final String provider) {
        this.provider = provider;
        enabled = Config.getBooleanProperty("AI_PROVIDER_" + provider.toUpperCase() + "_ENABLED", false);
    }

    public String getProvider() {
        return provider;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
