package com.dotcms.rest.api.v1.workflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is used to represent a WorkflowCommentForm object.
 * @author jsanca
 */
public class WorkflowCommentForm {

    private String comment;

    @JsonCreator
    public WorkflowCommentForm(@JsonProperty("comment") final String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }
}
