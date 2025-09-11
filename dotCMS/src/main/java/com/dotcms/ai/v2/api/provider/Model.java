package com.dotcms.ai.v2.api.provider;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import java.util.Map;

/**
 * Model encapsulates a default configuration for known models, however the Models still able to be created dynamically
 * @author jsanca
 */
public enum Model {

    OPEN_AI_GPT_40("openai", "gpt-4o-mini", "https://api.openai.com/v1"),
    OPEN_AI_TEXT_EMBEDDING_3_SMALL("openai", "text-embedding-3-small", "https://api.openai.com/v1"),
    ANTHROPIC_CLAUDE_3_7("anthropic", "claude-3-7-sonnet-20250219", "https://api.openai.com/v1");


    private final String vendor;
    private final String model;
    private final String apiUrl;

    private Model(final String vendor, final String model, final String apiUrl) {
        this.vendor = vendor;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    public String getVendor() { return vendor; }
    public String getModel() { return model; }
    public String getApiUrl() { return apiUrl; }
    public String getProviderName() {
        return vendor + "/" +  model;
    }

    public ModelConfig toConfig(final String apiKey) {

        return new ModelConfig(this.model,
                Map.of(ModelConfig.API_KEY, apiKey,
                        ModelConfig.VENDOR, vendor,
                        ModelConfig.MODEL, model,
                        ModelConfig.API_URL, apiUrl));
    }
}
