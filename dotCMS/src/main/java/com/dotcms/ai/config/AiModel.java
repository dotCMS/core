package com.dotcms.ai.config;

import java.util.Map;

/**
 * Model encapsulates a default configuration for known models, however the Models still able to be created dynamically
 * @author jsanca
 */
public enum AiModel {

    GEMINI_1_5_FLASH(AiVendor.GEMINI, "gemini-2.0-flash", "https://api.openai.com/v1"),
    OPEN_AI_GPT_4O_MINI(AiVendor.OPEN_AI, "gpt-4o-mini", "https://api.openai.com/v1"),
    OPEN_AI_TEXT_EMBEDDING_3_SMALL(AiVendor.OPEN_AI, "text-embedding-3-small", "https://api.openai.com/v1"),
    ANTHROPIC_CLAUDE_3_7(AiVendor.ANTHROPIC, "claude-3-7-sonnet-20250219", "https://api.anthropic.com");

    private final AiVendor vendor;
    private final String model;
    private final String apiUrl;

    AiModel(final AiVendor vendor, final String model, final String apiUrl) {
        this.vendor = vendor;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    public AiVendor getVendor() { return vendor; }
    public String getModel() { return model; }
    public String getApiUrl() { return apiUrl; }
    public String getProviderName() {
        return vendor.getVendorName() + "/" +  model;
    }

    public AiModelConfig toConfig(final String apiKey) {

        return new AiModelConfig(this.model,
                Map.of(AiModelConfig.API_KEY, apiKey,
                        AiModelConfig.VENDOR, vendor.getVendorName(),
                        AiModelConfig.MODEL, model,
                        AiModelConfig.API_URL, apiUrl));
    }
}
