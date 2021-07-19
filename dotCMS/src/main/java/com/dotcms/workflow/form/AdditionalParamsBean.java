package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;


/***
 * This bean ins meant to encapsulate all the information captured by actions that that require of some interaction with the user
 * Through a pop-up such as Push Publish and Comment and Assign
 */
public class AdditionalParamsBean {

    private final PushPublishBean pushPublishBean;
    private final AssignCommentBean assignCommentBean;
    private final Map<String, Object> additionalParamsMap;

    @JsonCreator
    public AdditionalParamsBean(
            @JsonProperty("pushPublish") final PushPublishBean pushPublishBean,
            @JsonProperty("assignComment") final AssignCommentBean assignCommentBean,
            @JsonProperty("additionalParamsMap") final Map<String, Object> additionalParamsMap) {
        this.pushPublishBean = pushPublishBean;
        this.assignCommentBean = assignCommentBean;
        this.additionalParamsMap = additionalParamsMap;
    }

    public PushPublishBean getPushPublishBean() {
        return pushPublishBean;
    }

    public AssignCommentBean getAssignCommentBean() {
        return assignCommentBean;
    }

    public Map<String, Object> getAdditionalParamsMap() {
        return additionalParamsMap;
    }
}
