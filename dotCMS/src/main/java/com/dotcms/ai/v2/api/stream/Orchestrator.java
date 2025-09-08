package com.dotcms.ai.v2.api.stream;

import com.dotcms.ai.v2.api.CompletionRequest;
import com.dotcms.ai.v2.api.CompletionSpec;
import com.dotcms.ai.v2.api.SummarizeRequest;

import java.io.OutputStream;

/**
 * Orchestrator abstraction to keep service thin.
 * Implement this to wire ModelProviderFactory, Retriever and Prompt composing.
 * @author jsanca
 */
public interface Orchestrator {
    /**
     * Non-streaming completion.
     */
    String complete(CompletionRequest request);

    /**
     * Streaming completion, writing partial tokens to the output stream.
     */
    void completeStream(CompletionRequest request, OutputStream out);

    /**
     * Specialized summarization shortcut (optional system/prompt tuning).
     */
    String summarize(SummarizeRequest request, String style, Integer maxChars);
}
