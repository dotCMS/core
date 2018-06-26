package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;

public class AdditionalParamsBean {

    private final PushPublishBean pushPublishBean;
    private final AssignCommentBean assignCommentBean;

    @JsonCreator
    public AdditionalParamsBean(
            @JsonProperty("pushPublish") PushPublishBean pushPublishBean,
            @JsonProperty("assignComment") AssignCommentBean assignCommentBean) {
        this.pushPublishBean = pushPublishBean;
        this.assignCommentBean = assignCommentBean;
    }

    public PushPublishBean getPushPublishBean() {
        return pushPublishBean;
    }

    public AssignCommentBean getAssignCommentBean() {
        return assignCommentBean;
    }
}
