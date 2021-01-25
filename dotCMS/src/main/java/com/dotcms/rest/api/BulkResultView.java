package com.dotcms.rest.api;

import java.util.List;

public class BulkResultView {
    private final Long successCount;
    private final Long skippedCount;
    private final List<FailedResultView> fails;


    public BulkResultView(final Long successCount, final Long skippedCount, final List<FailedResultView> fails) {
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

    public List<FailedResultView> getFails() {
        return fails;
    }
}