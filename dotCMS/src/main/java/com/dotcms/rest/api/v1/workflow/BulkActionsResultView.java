package com.dotcms.rest.api.v1.workflow;

import java.util.List;

public class BulkActionsResultView {

    private final List<String> successContentletidList;
    private final List<String> skipContentletidList;
    private final List<String> failContentletidList;

    public BulkActionsResultView(List<String> successContentletidList, List<String> skipContentletidList, List<String> failContentletidList) {
        this.successContentletidList = successContentletidList;
        this.skipContentletidList = skipContentletidList;
        this.failContentletidList = failContentletidList;
    }

    public List<String> getSuccessContentletidList() {
        return successContentletidList;
    }

    public List<String> getSkipContentletidList() {
        return skipContentletidList;
    }

    public List<String> getFailContentletidList() {
        return failContentletidList;
    }
}
