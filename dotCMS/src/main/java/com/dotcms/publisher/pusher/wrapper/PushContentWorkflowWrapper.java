package com.dotcms.publisher.pusher.wrapper;

import java.util.List;

import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;

public class PushContentWorkflowWrapper {
    
    private WorkflowTask task;
    private List<WorkflowHistory> history;
    private List<WorkflowComment> comments;

    public WorkflowTask getTask() {
        return task;
    }

    public void setTask(WorkflowTask task) {
        this.task = task;
    }

    public List<WorkflowHistory> getHistory() {
        return history;
    }

    public void setHistory(List<WorkflowHistory> history) {
        this.history = history;
    }

    public List<WorkflowComment> getComments() {
        return comments;
    }

    public void setComments(List<WorkflowComment> comments) {
        this.comments = comments;
    }
    
}
