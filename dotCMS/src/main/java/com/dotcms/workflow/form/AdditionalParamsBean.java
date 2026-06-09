package com.dotcms.workflow.form;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;


/***
 * This bean ins meant to encapsulate all the information captured by actions that that require of some interaction with the user
 * Through a pop-up such as Push Publish and Comment and Assign
 */
@Schema(description = "Action-specific parameters for bulk fire. Wraps push-publish settings, "
        + "assign-and-comment metadata, and a free-form map for actionlet-specific keys.")
public class AdditionalParamsBean {

    @Schema(description = "Push-publish settings applied when the resolved action wires the "
            + "Push Publish actionlet.")
    private final PushPublishBean pushPublishBean;

    @Schema(description = "Assignee and comment to attach to the workflow task created by the "
            + "fired action.")
    private final AssignCommentBean assignCommentBean;

    @Schema(description = "Free-form map for actionlet-specific keys. Well-known keys: "
            + "'_path_to_move' (String) — full target path including host, e.g. "
            + "'//hostname/folder/path/'. Used by the Move actionlet when the resolved bulk "
            + "action wires it. Silently ignored if no Move actionlet is in the resolved action.",
            example = "{\"_path_to_move\": \"//demo.dotcms.com/destination/folder/\"}")
    private final Map<String, Object> additionalParamsMap;

    @JsonCreator
    public AdditionalParamsBean(
            @JsonProperty("pushPublish") final PushPublishBean pushPublishBean,
            @JsonProperty("assignComment") final AssignCommentBean assignCommentBean,
            @JsonProperty("additionalParamsMap") final Map<String, Object> additionalParamsMap) {
        this.pushPublishBean = pushPublishBean;
        this.assignCommentBean = assignCommentBean;
        this.additionalParamsMap = UtilMethods.isSet(additionalParamsMap)
                ? additionalParamsMap : new HashMap<>();
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
