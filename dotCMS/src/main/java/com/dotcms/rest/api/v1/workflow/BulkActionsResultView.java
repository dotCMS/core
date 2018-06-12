package com.dotcms.rest.api.v1.workflow;

public class BulkActionsResultView {

    private final Long successCount;
    private final Long skippedCount;
    private final Long failsCount;


    public BulkActionsResultView(final Long successCount, final Long skippedCount, final Long failsCount) {
        this.successCount = successCount;
        this.skippedCount = skippedCount;
        this.failsCount = failsCount;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public Long getSkippedCount() {
        return skippedCount;
    }

    public Long getFailsCount() {
        return failsCount;
    }
}
