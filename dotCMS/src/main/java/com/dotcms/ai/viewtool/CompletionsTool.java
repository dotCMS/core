package com.dotcms.ai.viewtool;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.rest.forms.CompletionsForm;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.json.JSONObject;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class CompletionsTool implements ViewTool {

    private final HttpServletRequest request;
    private final Host host;
    private final AppConfig app;
    private final ViewContext context;


    /**
     * $ai.completions
     *
     * @param initData
     */
    CompletionsTool(Object initData) {
        this.context = (ViewContext) initData;
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
        this.app = ConfigService.INSTANCE.config(this.host);

    }

    @Override
    public void init(Object initData) {
        // unneeded because of constructor
    }


    public Map<String, String> getConfig() {
        return Map.of(
                AppKeys.COMPLETION_ROLE_PROMPT.key,
                this.app.getConfig(AppKeys.COMPLETION_ROLE_PROMPT),
                AppKeys.COMPLETION_TEXT_PROMPT.key,
                this.app.getConfig(AppKeys.COMPLETION_TEXT_PROMPT),
                AppKeys.MODEL.key,
                this.app.getConfig(AppKeys.MODEL)
        );

    }

    public Object summarize(String prompt) {
        return summarize(prompt, "default");
    }

    public Object summarize(String prompt, String indexName) {
        CompletionsForm form = new CompletionsForm.Builder().indexName(indexName).prompt(prompt).build();

        try {
            return CompletionsAPI.impl().summarize(form);
        } catch (Exception e) {
            return handleException(e);

        }
    }

    public Map<String, Object> handleException(Exception e) {
        try (StringWriter out = new StringWriter()) {
            PrintWriter writer = new PrintWriter(out);
            e.printStackTrace(writer);
            return Map.of("error", e.getMessage(), "stackTrace", out.toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public Object raw(String prompt) {
        try {
            return raw(new JSONObject(prompt));
        } catch (Exception e) {
            return handleException(e);
        }

    }

    public Object raw(JSONObject prompt) {
        try {
            return CompletionsAPI.impl().raw(prompt);
        } catch (Exception e) {
            return handleException(e);
        }

    }

    public Object raw(Map prompt) {
        try {
            return raw(new JSONObject(prompt));
        } catch (Exception e) {
            return handleException(e);
        }

    }

}
