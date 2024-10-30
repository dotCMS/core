package com.dotcms.rest.api.v1.workflow;

import java.util.Date;

/**
 * This class is used to return a list of WorkflowTimelineItem objects as a response entity.
 *
 * @author jsanca
 */
public class WorkflowTimelineItemView {

    private final Date createdDate;
    private final String roleId;
    private final String postedBy;
    private final String actionId;
    private final String stepId;
    private final String commentDescription;
    private final String taskId;
    private final String type;

    public WorkflowTimelineItemView(Date createdDate, String roleId, String postedBy, String actionId, String stepId, String commentDescription, String taskId, String type) {
        this.createdDate = createdDate;
        this.roleId = roleId;
        this.postedBy = postedBy;
        this.actionId = actionId;
        this.stepId = stepId;
        this.commentDescription = commentDescription;
        this.taskId = taskId;
        this.type = type;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public String getActionId() {
        return actionId;
    }

    public String getStepId() {
        return stepId;
    }

    public String getCommentDescription() {
        return commentDescription;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getType() {
        return type;
    }
}
