package com.dotcms.inference.model;

import java.io.Serializable;

/**
 * Tokens consumed by one exchange.
 *
 * <p>Every field is nullable because not every provider reports usage. Absent counts are left
 * absent and are never estimated — a fabricated number here would be indistinguishable from a
 * real one to anyone reconciling spend.</p>
 *
 * @param inputTokens  tokens in the prompt, or null if unreported
 * @param outputTokens tokens generated, or null if unreported
 * @param totalTokens  the sum as the provider reported it, or null
 */
public record InferenceUsage(Integer inputTokens, Integer outputTokens, Integer totalTokens)
        implements Serializable {

    /** Usage the provider did not report. */
    public static final InferenceUsage UNREPORTED = new InferenceUsage(null, null, null);

    /** @return whether the provider reported any counts at all */
    public boolean isReported() {
        return inputTokens != null || outputTokens != null || totalTokens != null;
    }
}
