package com.dotcms.ai.api;

/**
 * Provides a facade for all artificial intelligence services offered by dotCMS.
 * @author jsanca
 */
public interface DotAIAPI {

    CompletionsAPI getCompletionsAPI(Object... initArguments);

    EmbeddingsAPI getEmbeddingsAPI(Object... initArguments);

    ChatAPI getChatAPI(Object... initArguments);

    ImageAPI getImageAPI(Object... initArguments);
}
