package com.dotcms.rest.api.v1.workflow;

import java.util.List;

public class BulkActionsResultView {

    private final Long successCount;
    private final Long skippedCount;
    private final List<ActionFail> fails;
    private final String skipReason;


    public BulkActionsResultView(final Long successCount, final Long skippedCount, final List<ActionFail> fails) {
        this(successCount, skippedCount, fails, null);
    }

    public BulkActionsResultView(final Long successCount, final Long skippedCount, final List<ActionFail> fails,
                                 final String skipReason) {
        this.successCount = successCount;
        this.skippedCount = skippedCount;
        this.fails = fails;
        this.skipReason = skipReason;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public Long getSkippedCount() {
        return skippedCount;
    }

    public List<ActionFail> getFails() {
        return fails;
    }

    /**
     * Human-readable explanation of why {@link #skippedCount} contentlets were not processed.
     * Populated when the supplied workflow action does not own the workflow steps that the
     * input contentlets are currently in (i.e. wrong-scheme mismatch). May be {@code null} when
     * the cause is unknown or no contentlets were skipped.
     */
    public String getSkipReason() {
        return skipReason;
    }
}
