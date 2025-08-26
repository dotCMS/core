package com.dotcms.ai.v2.api.provider.config;

import com.dotcms.ai.v2.api.provider.Model;
import com.dotmarketing.util.Config;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ModelConfigFactory {

    public ModelConfig get(String modelProviderKey) {

        if(Model.OPEN_AI_GPT_40.getProviderName().equalsIgnoreCase(modelProviderKey)) {
            final String apiKey = Config.getStringProperty("OPEN_AI_API_KEY"); // todo: this should be delegate to the apps but works by now
            return Model.OPEN_AI_GPT_40.toConfig(apiKey);
        } if(Model.ANTHROPIC_CLAUDE_3_7.getProviderName().equalsIgnoreCase(modelProviderKey)) {

            final String apiKey = Config.getStringProperty("ANTHROPIC_API_KEY");
            return Model.ANTHROPIC_CLAUDE_3_7.toConfig(apiKey);
        }

        return null; // here returns a default model
    }
}
