package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


/***
 * This bean ins meant to encapsulate all the information captured by actions that that require of some interaction with the user
 * Through a pop-up such as Push Publish and Comment and Assign
 */
public class AdditionalParamsBean {

    private final PushPublishBean pushPublishBean;
    private final AssignCommentBean assignCommentBean;

    @JsonCreator
    public AdditionalParamsBean(
            @JsonProperty("pushPublish") final PushPublishBean pushPublishBean,
            @JsonProperty("assignComment") final AssignCommentBean assignCommentBean) {
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
