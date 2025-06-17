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
    private final String commentDescription;
    private final String taskId;
    private final String type;

    public WorkflowTimelineItemView(final Date createdDate,
                                    final String roleId,
                                    final String postedBy,
                                    final String commentDescription,
                                    final String taskId,
                                    final String type) {
        this.createdDate = createdDate;
        this.roleId = roleId;
        this.postedBy = postedBy;
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