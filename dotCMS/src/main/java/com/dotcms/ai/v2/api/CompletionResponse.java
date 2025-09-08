package com.dotcms.ai.v2.api;

import java.util.List;
import java.util.Map;

/**
 * Completion response payload.
 * @author jsanca
 */
public final class CompletionResponse {

    /** Final generated text. */
    public String text;

    /**
     * Optional citations/metadata for retrieved context.
     * Each entry may include: title, url, contentType, identifier, languageId, score.
     */
    public List<Map<String, Object>> citations;

    /** Optional auxiliary metadata (token usage, model name, timings, etc.). */
    public Map<String, Object> meta;

    public static CompletionResponse of(final String text) {
        CompletionResponse r = new CompletionResponse();
        r.text = text;
        return r;
    }
}
