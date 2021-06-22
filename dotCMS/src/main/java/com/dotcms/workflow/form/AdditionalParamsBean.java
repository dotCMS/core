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
    private final Map<String, Object> additionalBeanMap;

    @JsonCreator
    public AdditionalParamsBean(
            @JsonProperty("pushPublish") final PushPublishBean pushPublishBean,
            @JsonProperty("assignComment") final AssignCommentBean assignCommentBean,
            @JsonProperty("additionalBeanMap") final Map<String, Object> additionalBeanMap) {
        this.pushPublishBean = pushPublishBean;
        this.assignCommentBean = assignCommentBean;
        this.additionalBeanMap = additionalBeanMap;
    }

    public PushPublishBean getPushPublishBean() {
        return pushPublishBean;
    }

    public AssignCommentBean getAssignCommentBean() {
        return assignCommentBean;
    }

    public Map<String, Object> getAdditionalBeanMap() {
        return additionalBeanMap;
    }
}
