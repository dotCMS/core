package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AiAppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.EncodingUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
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

    private final ViewContext context;
    private final HttpServletRequest request;
    private final Host host;
    private final AiAppConfig appConfig;
    private final User user;

    /**
     * Constructor for the EmbeddingsTool class.
     * Initializes the request, host, and app fields.
     *
     * @param initData Initialization data for the tool.
     */
    EmbeddingsTool(Object initData) {
        this.context = (ViewContext) initData;
        this.request = this.context.getRequest();
        this.host = host();
        this.appConfig = appConfig();
        this.user = user();
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
        return EncodingUtil.get()
                .getEncoding(appConfig, AIModelType.TEXT)
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
        int maxTokens = ConfigService.INSTANCE.config(host).getEmbeddingsModel().getMaxTokens();
        if (tokens > maxTokens) {
            Logger.warn(
                    EmbeddingsTool.class,
                    "Prompt is too long.  Maximum prompt size is " + maxTokens + " tokens (roughly ~"
                            + maxTokens * .75 + " words).  Your prompt was " + tokens + " tokens ");
        }

        return APILocator.getDotAIAPI()
                .getEmbeddingsAPI()
                .pullOrGenerateEmbeddings(prompt, UtilMethods.extractUserIdOrNull(user))
                ._2;
    }

    /**
     * Gets the count of embeddings by index.
     *
     * @return A map where the keys are index names and the values are maps of index properties.
     */
    public Map<String, Map<String, Object>> getIndexCount() {
        return APILocator.getDotAIAPI().getEmbeddingsAPI().countEmbeddingsByIndex();
    }

    @VisibleForTesting
    Host host() {
        return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
    }

    @VisibleForTesting
    AiAppConfig appConfig() {
        return ConfigService.INSTANCE.config(host);
    }

    @VisibleForTesting
    User user() {
        return PortalUtil.getUser(context.getRequest());
    }

}
