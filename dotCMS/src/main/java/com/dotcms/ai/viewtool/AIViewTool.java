package com.dotcms.ai.viewtool;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.service.OpenAIChatService;
import com.dotcms.ai.service.OpenAIChatServiceImpl;
import com.dotcms.ai.service.OpenAIImageService;
import com.dotcms.ai.service.OpenAIImageServiceImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.util.PortalUtil;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.io.IOException;
import java.util.Map;


public class AIViewTool implements ViewTool {

    private AppConfig config;
    private ViewContext context;
    private OpenAIChatService chatService;
    private OpenAIImageService imageService;

    @Override
    public void init(final Object obj) {
        context = (ViewContext) obj;
        config = config();
        chatService = chatService();
        imageService = imageService();
    }

    @VisibleForTesting
    protected AppConfig config() {
        return ConfigService
                .INSTANCE
                .config(WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(context.getRequest()));
    }

    @VisibleForTesting
    protected OpenAIChatService chatService() {
        return new OpenAIChatServiceImpl(config);
    }

    @VisibleForTesting
    protected OpenAIImageService imageService() {
        return new OpenAIImageServiceImpl(
                config,
                PortalUtil.getUser(context.getRequest()),
                APILocator.getHostAPI(),
                APILocator.getTempFileAPI());
    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt,
     * textPrompt, imagePrompt)
     *
     * @return JSONObject
     */
    public JSONObject generateText(final String prompt) throws IOException {
        return chatService.sendTextPrompt(prompt);
    }

    public JSONObject generateText(final Map<String, Object> prompt) throws IOException {
        return chatService.sendRawRequest(new JSONObject(prompt));
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in
     * response
     *
     * @param prompt
     * @return
     */
    public JSONObject generateImage(final String prompt) {
        try {
            return imageService.sendTextPrompt(prompt);
        } catch (Exception e) {
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("response", e.getMessage());
            return jsonResponse;
        }
    }

    public JSONObject generateImage(final Map<String, Object> prompt) {
        try {
            return imageService.sendRequest(new JSONObject(prompt));
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
