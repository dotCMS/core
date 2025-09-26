package com.dotcms.rest.api.v1.workflow;

import java.util.Date;
import java.util.Map;

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
    private final Map<String, String> step;
    private final Map<String, String> action;

    public WorkflowTimelineItemView(final Date createdDate,
                                    final String roleId,
                                    final String postedBy,
                                    final String commentDescription,
                                    final String taskId,
                                    final String type,
                                    final Map<String, String> step,
                                    final Map<String, String> action) {
        this.createdDate = createdDate;
        this.roleId = roleId;
        this.postedBy = postedBy;
        this.commentDescription = commentDescription;
        this.taskId = taskId;
        this.type = type;
        this.step = step;
        this.action = action;
    }

    public Map<String, String> getAction() {
        return action;
    }

    public Map<String, String> getStep() {
        return step;
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