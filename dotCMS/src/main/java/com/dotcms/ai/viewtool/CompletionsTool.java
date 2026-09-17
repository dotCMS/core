package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * This class is a ViewTool that provides functionality related to completions.
 * It interacts with the CompletionsAPI to perform operations such as summarizing and raw processing.
 *
 * <p>Output is HTML-escaped by default (#37153): {@code summarize} escapes the provider's
 * {@code openAiResponse} subtree, the echoed {@code query} and each match's {@code extractedText};
 * {@code raw} escapes the whole provider payload; error payloads are escaped in full. Contentlet
 * fields under {@code dotCMSResults} are returned as stored. Templates that need the unescaped
 * payload go through {@code $ai.unsafe.completions}, which constructs this tool with escaping off
 * and behaves exactly as before #37153.
 */
public class CompletionsTool implements ViewTool {

    private final ViewContext context;
    private final HttpServletRequest request;
    private final Host host;
    private final AppConfig config;
    private final User user;
    private final boolean escapeOutput;

    CompletionsTool(final Object initData) {
        this(initData, true);
    }

    /**
     * @param initData     the Velocity {@link ViewContext}
     * @param escapeOutput {@code true} to HTML-escape results (the {@code $ai} default),
     *                     {@code false} for the {@code $ai.unsafe} behaviour
     */
    CompletionsTool(final Object initData, final boolean escapeOutput) {
        this.context = (ViewContext) initData;
        this.request = this.context.getRequest();
        this.host = host();
        this.config = config();
        this.user = user();
        this.escapeOutput = escapeOutput;
    }

    @Override
    public void init(Object initData) {
        // unneeded because of constructor
    }

    /**
     * Returns the configuration for the CompletionsTool.
     * @return A map containing the configuration.
     */
    public Map<String, String> getConfig() {
        return Map.of(
                AppKeys.COMPLETION_ROLE_PROMPT.key,
                this.config.getConfig(AppKeys.COMPLETION_ROLE_PROMPT),
                AppKeys.COMPLETION_TEXT_PROMPT.key,
                this.config.getConfig(AppKeys.COMPLETION_TEXT_PROMPT));
    }

    /**
     * Summarizes the given prompt using the default index.
     * @param prompt The prompt to summarize.
     * @return The summarized object.
     */
    public Object summarize(final String prompt) {
        return summarize(prompt, "default");
    }

    /**
     * Summarizes the given prompt using the specified index.
     * @param prompt The prompt to summarize.
     * @param indexName The name of the index to use.
     * @return The summarized object.
     */
    public Object summarize(final String prompt, final String indexName) {
        final CompletionsForm form = new CompletionsForm.Builder()
                .indexName(indexName)
                .prompt(prompt)
                .user(user)
                .build();
        try {
            return escapeSearchShaped(APILocator.getDotAIAPI().getCompletionsAPI(config).summarize(form));
        } catch (Exception e) {
            return escape(handleException(e));
        }
    }

    /**
     * Handles exceptions that occur during the execution of the tool.
     * @param e The exception to handle.
     * @return A map containing the error message and stack trace.
     */
    private Map<String, Object> handleException(final Exception e) {
        try (StringWriter out = new StringWriter()) {
            final PrintWriter writer = new PrintWriter(out);
            e.printStackTrace(writer);
            return Map.of("error", e.getMessage(), "stackTrace", out.toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Processes the given prompt in raw format.
     * @param prompt The prompt to process.
     * @return The processed object.
     */
    public Object raw(String prompt) {
        try {
            return raw(new JSONObject(prompt));
        } catch (Exception e) {
            return escape(handleException(e));
        }
    }

    /**
     * Processes the given prompt in raw format.
     * @param prompt The prompt to process.
     * @return The processed object.
     */
    public Object raw(final JSONObject prompt) {
        try {
            return escape(APILocator.getDotAIAPI()
                    .getCompletionsAPI(config)
                    .raw(prompt, UtilMethods.extractUserIdOrNull(user)));
        } catch (Exception e) {
            return escape(handleException(e));
        }
    }

    /**
     * Processes the given prompt in raw format.
     * @param prompt The prompt to process.
     * @return The processed object.
     */
    public Object raw(final Map prompt) {
        try {
            return raw(new JSONObject(prompt));
        } catch (Exception e) {
            return escape(handleException(e));
        }
    }

    /** Whole-payload escaping (provider output and error payloads), or pass-through when unsafe. */
    private Object escape(final Object payload) {
        return escapeOutput ? AIViewToolOutputEscaper.deepEscape(payload) : payload;
    }

    /** By-source escaping for the summarize shape, or pass-through when unsafe. */
    private Object escapeSearchShaped(final Object payload) {
        return escapeOutput ? AIViewToolOutputEscaper.escapeSearchShaped(payload) : payload;
    }

    @VisibleForTesting
    Host host() {
        return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
    }

    @VisibleForTesting
    AppConfig config() {
        return ConfigService.INSTANCE.config(this.host);
    }

    @VisibleForTesting
    User user() {
        return PortalUtil.getUser(context.getRequest());
    }

}
