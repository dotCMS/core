package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AiAppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.api.ChatAPI;
import com.dotcms.ai.api.ImageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * AIViewTool is a view tool that provides access to the OpenAI API services.
 */
public class AIViewTool implements ViewTool {

    private ViewContext context;
    private AiAppConfig config;
    private ChatAPI chatService;
    private ImageAPI imageService;
    private User user;

    @Override
    public void init(final Object obj) {
        context = (ViewContext) obj;
        config = config();
        user = user();
        chatService = chatService();
        imageService = imageService();
    }

    /**
     * Check if AI is enabled by verifying if the API key is set in the configuration.
     *
     * @return true if AI is enabled, false otherwise
     */
    public boolean isAiEnabled() {
        return Optional.ofNullable(config).map(AiAppConfig::isEnabled).orElse(false);
    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt,
     * textPrompt, imagePrompt)
     *
     * @return JSONObject instance
     */
    public JSONObject generateText(final String prompt) {
        return justGenerate(prompt, chatService::sendTextPrompt);
    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt,
     * textPrompt, imagePrompt)
     *
     * @return prompt map representation of the JSON object
     */
    public JSONObject generateText(final Map<String, Object> prompt) {
        return justGenerate(prompt, p -> chatService.sendRawRequest(new JSONObject(p)));
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in
     * response
     *
     * @param prompt text prompt
     * @return JSONObject instance
     */
    public JSONObject generateImage(final String prompt) {
        return generateHandled(prompt, imageService::sendTextPrompt);
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in
     *
     * @param prompt map representation of a prompt
     * @return JSONObject instance
     */
    public JSONObject generateImage(final Map<String, Object> prompt) {
        return generateHandled(prompt, p -> imageService.sendRequest(new JSONObject(p)));
    }

    /**
     * Processes embedding request by calling EmbeddingsTool.
     *
     * @return {@link EmbeddingsTool} instance
     */
    public EmbeddingsTool getEmbeddings() {
        return new EmbeddingsTool(context);
    }

    /**
     * Processes search request by calling SearchTool.
     *
     * @return {@link SearchTool} instance
     */
    public SearchTool getSearch() {
        return new SearchTool(context);
    }

    /**
     * Processes completions request by calling CompletionsTool.
     *
     * @return {@link CompletionsTool} instance
     */
    public CompletionsTool getCompletions() {
        return new CompletionsTool(context);
    }

    @VisibleForTesting
    AiAppConfig config() {
        return ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(context.getRequest()));
    }

    @VisibleForTesting
    User user() {
        return PortalUtil.getUser(context.getRequest());
    }

    @VisibleForTesting
    ChatAPI chatService() {
        return APILocator.getDotAIAPI().getChatAPI(config, user);
    }

    @VisibleForTesting
    ImageAPI imageService() {
        return APILocator.getDotAIAPI().getImageAPI(config, user, APILocator.getHostAPI(), APILocator.getTempFileAPI());
    }

    private <P extends Object> Try<JSONObject> generate(final P prompt, final Function<P, JSONObject> serviceCall) {
        return Try.of(() -> serviceCall.apply(prompt));
    }

    private <P extends Object> JSONObject justGenerate(final P prompt, final Function<P, JSONObject> serviceCall) {
        return generate(prompt, serviceCall).get();
    }

    private <P extends Object> JSONObject generateHandled(final P prompt, final Function<P, JSONObject> serviceCall) {
        return generate(prompt, serviceCall).getOrElseGet(this::handleException);
    }

    private JSONObject handleException(final Throwable e) {
        final JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(AiKeys.ERROR, e.getMessage());
        return jsonResponse;
    }

}
