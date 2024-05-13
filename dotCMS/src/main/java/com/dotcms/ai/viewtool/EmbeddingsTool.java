package com.dotcms.ai.viewtool;

import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.EncodingUtil;
import com.dotcms.ai.util.OpenAIModel;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * This class provides functionality for generating and managing embeddings.
 * It implements the ViewTool interface and provides methods for counting tokens,
 * generating embeddings, and getting index counts.
 */
public class EmbeddingsTool implements ViewTool {

    final private HttpServletRequest request;
    final private Host host;
    final private AppConfig appConfig;

    /**
     * Constructor for the EmbeddingsTool class.
     * Initializes the request, host, and app fields.
     *
     * @param initData Initialization data for the tool.
     */
    EmbeddingsTool(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = host();
        this.appConfig = appConfig();
    }

    @Override
    public void init(Object initData) {
        /* unneeded because of constructor */
    }

    /**
     * Counts the number of tokens in a given prompt.
     * The count is based on the encoding for the model used by the app.
     *
     * @param prompt The text to count tokens in.
     * @return The number of tokens in the prompt, or -1 if no encoding is found for the model.
     */
    public int countTokens(final String prompt) {
        return EncodingUtil.registry
                .getEncodingForModel(appConfig.getModel())
                .map(encoding -> encoding.countTokens(prompt))
                .orElse(-1);
    }

    /**
     * Generates embeddings for a given prompt.
     * If the number of tokens in the prompt exceeds the maximum allowed by the model,
     * a warning is logged.
     *
     * @param prompt The text to generate embeddings for.
     * @return A list of embeddings for the prompt.
     */
    public List<Float> generateEmbeddings(final String prompt) {
        int tokens = countTokens(prompt);
        int maxTokens = OpenAIModel.resolveModel(ConfigService.INSTANCE.config(host).getConfig(AppKeys.EMBEDDINGS_MODEL)).maxTokens;
        if (tokens > maxTokens) {
            Logger.warn(EmbeddingsTool.class, "Prompt is too long.  Maximum prompt size is " + maxTokens + " tokens (roughly ~" + maxTokens * .75 + " words).  Your prompt was " + tokens + " tokens ");
        }
        return EmbeddingsAPI.impl().pullOrGenerateEmbeddings(prompt)._2;
    }

    /**
     * Gets the count of embeddings by index.
     *
     * @return A map where the keys are index names and the values are maps of index properties.
     */
    public Map<String, Map<String, Object>> getIndexCount() {
        return EmbeddingsAPI.impl().countEmbeddingsByIndex();
    }

    @VisibleForTesting
    Host host() {
        return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
    }

    @VisibleForTesting
    AppConfig appConfig() {
        return ConfigService.INSTANCE.config(host);
    }

}
