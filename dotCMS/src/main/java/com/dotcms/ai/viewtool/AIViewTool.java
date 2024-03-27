package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.service.OpenAIChatService;
import com.dotcms.ai.service.OpenAIChatServiceImpl;
import com.dotcms.ai.service.OpenAIImageService;
import com.dotcms.ai.service.OpenAIImageServiceImpl;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.io.IOException;
import java.util.Map;

public class AIViewTool implements ViewTool {

    AppConfig config;
    private ViewContext context;

    @Override
    public void init(Object obj) {
        context = (ViewContext) obj;
        this.config = ConfigService.INSTANCE.config(
                WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(context.getRequest()));

    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt,
     * textPrompt, imagePrompt)
     *
     * @return JSONObject
     */
    public JSONObject generateText(final String prompt) throws IOException {
        OpenAIChatService service = new OpenAIChatServiceImpl(config);
        return service.sendTextPrompt(prompt);
    }

    public JSONObject generateText(final Map<String, Object> prompt) throws IOException {
        OpenAIChatService service = new OpenAIChatServiceImpl(config);
        return service.sendRawRequest(new JSONObject(prompt));
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in
     * response
     *
     * @param prompt
     * @return
     */
    public JSONObject generateImage(String prompt) {
        User user = PortalUtil.getUser(context.getRequest());
        OpenAIImageService service = new OpenAIImageServiceImpl(config, user);
        try {

            return service.sendTextPrompt(prompt);

        } catch (Exception e) {
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("response", e.getMessage());
            return jsonResponse;
        }
    }

    public JSONObject generateImage(final Map<String, Object> prompt) {
        User user = PortalUtil.getUser(context.getRequest());
        OpenAIImageService service = new OpenAIImageServiceImpl(config, user);
        try {

            return service.sendRequest(new JSONObject(prompt));

        } catch (Exception e) {
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("response", e.getMessage());
            return jsonResponse;
        }
    }

    public EmbeddingsTool getEmbeddings() {
        return new EmbeddingsTool(context);
    }

    public SearchTool getSearch() {
        return new SearchTool(context);
    }

    public CompletionsTool getCompletions() {
        return new CompletionsTool(context);
    }
}
