package com.dotcms.rest.api.v1.workflow;

import java.util.List;

public class BulkActionsResultView {

    private final Long successCount;
    private final Long skippedCount;
    private final List<ActionFail> fails;


    public BulkActionsResultView(final Long successCount, final Long skippedCount, final List<ActionFail> fails) {
        this.successCount = successCount;
        this.skippedCount = skippedCount;
        this.fails = fails;
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
}
