package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AssignCommentBean {

    private final String assign;
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
