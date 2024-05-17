package com.dotcms.ai.util;

import com.dotmarketing.exception.DotRuntimeException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum representing different models of OpenAI.
 * Each enum value contains the model name, tokens per minute, API per minute, maximum tokens, and a flag indicating if it's a completion model.
 */
public enum OpenAIModel {

    GPT_3_5_TURBO("gpt-3.5-turbo", 3000, 3500, 4096, true),
    GPT_3_5_TURBO_16k("gpt-3.5-turbo-16k", 180000, 3500, 16384, true),
    GPT_4("gpt-4", 10000, 200, 8191, true),
    GPT_4_TURBO("gpt-4-1106-preview", 10000, 200, 128000, true),
    GPT_4_TURBO_PREVIEW("gpt-4-turbo-preview", 10000, 200, 128000, true),
    TEXT_EMBEDDING_ADA_002("text-embedding-ada-002", 1000000, 3000, 8191, false),
    DALL_E_2("dall-e-2", 0, 50, 0, false),
    DALL_E_3("dall-e-3", 0, 50, 0, false);

    public final int tokensPerMinute;
    public final int apiPerMinute;
    public final int maxTokens;
    public final String modelName;
    public final boolean completionModel;

    OpenAIModel(final String modelName,
                final int tokensPerMinute,
                final int apiPerMinute,
                final int maxTokens,
                final boolean completionModel) {
        this.modelName = modelName;
        this.tokensPerMinute = tokensPerMinute;
        this.apiPerMinute = apiPerMinute;
        this.maxTokens = maxTokens;
        this.completionModel = completionModel;
    }

    /**
     * Resolves the model based on the input string.
     *
     * @param modelIn The input string representing the model.
     * @return The corresponding OpenAIModel.
     * @throws DotRuntimeException If the input string does not correspond to any OpenAIModel.
     */
    public static OpenAIModel resolveModel(final String modelIn) {
        final String modelOut = modelIn.replace("-", "_").replace(".", "_").toUpperCase().trim();
        for (final OpenAIModel openAiModel : OpenAIModel.values()) {
            if (openAiModel.modelName.equalsIgnoreCase(modelIn) || openAiModel.name().equalsIgnoreCase(modelOut)) {
                return openAiModel;
            }
        }

        throw new DotRuntimeException(
                "Unable to parse model:'" + modelIn + "'.  Only " + supportedModels() + " are supported ");
    }

    /**
     * Returns a string representing the supported models.
     *
     * @return A string representing the supported models.
     */
    private static String supportedModels() {
        return Arrays.stream(OpenAIModel.values()).map(o -> o.modelName).collect(Collectors.joining(", "));
    }

    /**
     * Returns the minimum interval between calls for the model.
     *
     * @return The minimum interval between calls for the model.
     */
    public long minIntervalBetweenCalls() {
        return 60000 / apiPerMinute;
    }

}
