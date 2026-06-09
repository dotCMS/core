package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Assignee and comment to attach to the workflow task created when an "
        + "action is fired.")
public class AssignCommentBean {

    @Schema(description = "Identifier of the user or role to next receive the workflow task. "
            + "Empty string keeps the existing assignee.")
    private final String assign;

    @Schema(description = "Free-form comment recorded against the workflow task.")
    private final String comment;

    @JsonCreator
    public AssignCommentBean(
            @JsonProperty("assign") final String assign,
            @JsonProperty("comment") final String comment) {
        this.assign = assign;
        this.comment = comment;
    }

    public String getAssign() {
        return assign;
    }

    public String getComment() {
        return comment;
    }

}
