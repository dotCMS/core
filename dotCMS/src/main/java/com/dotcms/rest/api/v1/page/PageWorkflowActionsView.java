package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.api.v1.workflow.WorkflowActionView;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;
import java.util.Map;

public class PageWorkflowActionsView {

    private final Map page;
    private final List<WorkflowActionView> actions;

    public PageWorkflowActionsView(final Map page, final List<WorkflowActionView> actions) {
        this.page = page;
        this.actions = actions;
    }

    public Map getPage() {
        return page;
    }

    public List<WorkflowActionView> getActions() {
        return actions;
    }
}
