package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
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
 *
 * <p>Output is HTML-escaped by default (#37153): {@code generateText} and {@code generateImage}
 * return a copy of the provider payload with every string encoded, and the {@code completions} and
 * {@code search} sub-tools escape by source (see {@link CompletionsTool} / {@link SearchTool}).
 * Templates that need the unescaped payload go through {@link #getUnsafe() $ai.unsafe}, which is the
 * same tool with escaping turned off and behaves exactly as {@code $ai} did before #37153, for every
 * method.
 */
public class AIViewTool implements ViewTool {

    private ViewContext context;
    private AppConfig config;
    private ChatAPI chatService;
    private ImageAPI imageService;
    private User user;
    private final boolean escapeOutput;

    public AIViewTool() {
        this.escapeOutput = true;
    }

    /**
     * Copy constructor for {@link #getUnsafe()}: reuses the fields already resolved by
     * {@link #init(Object)} on {@code source} (so overridden factory methods keep their effect) and
     * turns escaping off. The copy is never {@code init()}ed again by Velocity.
     */
    private AIViewTool(final AIViewTool source) {
        this.context = source.context;
        this.config = source.config;
        this.user = source.user;
        this.chatService = source.chatService;
        this.imageService = source.imageService;
        this.escapeOutput = false;
    }

    @Override
    public void init(final Object obj) {
        context = (ViewContext) obj;
        config = config();
        user = user();
        chatService = chatService();
        imageService = imageService();
    }

    /**
     * The explicit opt-in for unescaped output: {@code $ai.unsafe}. Returns this tool with escaping
     * turned off; every method behaves exactly as it did before #37153. Calling it on an already
     * unsafe tool returns the same instance.
     *
     * @return the unescaped variant of this tool
     */
    public AIViewTool getUnsafe() {
        return escapeOutput ? new AIViewTool(this) : this;
    }

    /**
     * Check if AI is enabled by verifying if the API key is set in the configuration.
     *
     * @return true if AI is enabled, false otherwise
     */
    public boolean isAiEnabled() {
        return Optional.ofNullable(config).map(AppConfig::isEnabled).orElse(false);
    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt,
     * textPrompt, imagePrompt)
     *
     * @return JSONObject instance
     */
    public JSONObject generateText(final String prompt) {
        return escape(justGenerate(prompt, chatService::sendTextPrompt));
    }

    /**
     * Generate a response from the AI prompt service with adding config data to original prompt (rolePrompt,
     * textPrompt, imagePrompt)
     *
     * @return prompt map representation of the JSON object
     */
    public JSONObject generateText(final Map<String, Object> prompt) {
        return escape(justGenerate(prompt, p -> chatService.sendRawRequest(new JSONObject(p))));
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in
     * response
     *
     * @param prompt text prompt
     * @return JSONObject instance
     */
    public JSONObject generateImage(final String prompt) {
        return escape(generateHandled(prompt, imageService::sendTextPrompt));
    }

    /**
     * Processes image request by calling ImageService. If response is OK creates temp file and adds its name in
     *
     * @param prompt map representation of a prompt
     * @return JSONObject instance
     */
    public JSONObject generateImage(final Map<String, Object> prompt) {
        return escape(generateHandled(prompt, p -> imageService.sendRequest(new JSONObject(p))));
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
        return new SearchTool(context, escapeOutput);
    }

    /**
     * Processes completions request by calling CompletionsTool.
     *
     * @return {@link CompletionsTool} instance
     */
    public CompletionsTool getCompletions() {
        return new CompletionsTool(context, escapeOutput);
    }

    @VisibleForTesting
    AppConfig config() {
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

    /** Whole-payload escaping (provider output and error payloads), or pass-through when unsafe. */
    private JSONObject escape(final JSONObject payload) {
        return escapeOutput ? AIViewToolOutputEscaper.deepEscape(payload) : payload;
    }

    private JSONObject handleException(final Throwable e) {
        final JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(AiKeys.ERROR, e.getMessage());
        return jsonResponse;
    }

}
