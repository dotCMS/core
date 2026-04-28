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
 */
public class CompletionsTool implements ViewTool {

    private final ViewContext context;
    private final HttpServletRequest request;
    private final Host host;
    private final AppConfig config;
    private final User user;

    CompletionsTool(Object initData) {
        this.context = (ViewContext) initData;
        this.request = this.context.getRequest();
        this.host = host();
        this.config = config();
        this.user = user();
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
                this.config.getConfig(AppKeys.COMPLETION_TEXT_PROMPT),
                AppKeys.TEXT_MODEL_NAMES.key,
                this.config.getConfig(AppKeys.TEXT_MODEL_NAMES));
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
            return APILocator.getDotAIAPI().getCompletionsAPI(config).summarize(form);
        } catch (Exception e) {
            return handleException(e);
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
            return handleException(e);
        }
    }

    /**
     * Processes the given prompt in raw format.
     * @param prompt The prompt to process.
     * @return The processed object.
     */
    public Object raw(final JSONObject prompt) {
        try {
            return APILocator.getDotAIAPI()
                    .getCompletionsAPI(config)
                    .raw(prompt, UtilMethods.extractUserIdOrNull(user));
        } catch (Exception e) {
            return handleException(e);
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
            return handleException(e);
        }
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
