package com.dotcms.ai.api;

/**
 * Provides a facade for all artificial intelligence services offered by dotCMS.
 * @author jsanca
 */
public interface DotAIAPI {

    /**
     * Returns the completions API
     * @param initArguments
     * @return
     */
    CompletionsAPI getCompletionsAPI(Object... initArguments);

    /**
     * Returns the embeddings API
     * @param initArguments
     * @return
     */
    EmbeddingsAPI getEmbeddingsAPI(Object... initArguments);

    /**
     * Returns the chat API
     * @param initArguments
     * @return
     */
    ChatAPI getChatAPI(Object... initArguments);

    /**
     * Returns the image API
     * @param initArguments
     * @return
     */
    ImageAPI getImageAPI(Object... initArguments);
}
