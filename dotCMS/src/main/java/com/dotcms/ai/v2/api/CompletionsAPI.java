package com.dotcms.ai.v2.api;


import java.io.OutputStream;

/**
 * Stateless text generation ("completions") API.
 * A completion is a one-shot request that may optionally leverage retrieval (RAG),
 * but does not rely on conversation memory/state.
 * @author jsanca
 */
public interface CompletionsAPI {

    /**
     * Generate text given a prompt (and optional retrieval parameters).
     *
     * @param request Completion request containing prompt, model and generation settings.
     * @return The generated text and optional citations/metadata.
     */
    CompletionResponse complete(CompletionRequest request);

    /**
     * Specialized shortcut for summarization scenarios.
     * Implementations typically build a system prompt that instructs the model to summarize
     * clearly and concisely in the requested language/style.
     *
     * @param request Summarization request (inherits CompletionRequest fields).
     * @return The generated summary.
     */
    CompletionResponse summarize(SummarizeRequest request);

    /**
     * Streaming variant of {@link #complete(CompletionRequest)}. Implementations should emit
     * partial tokens to the provided OutputStream and flush periodically.
     *
     * @param request Completion request.
     * @param output  Target output stream to receive the incremental tokens.
     */
    void completeStream(CompletionRequest request, OutputStream output);
}
