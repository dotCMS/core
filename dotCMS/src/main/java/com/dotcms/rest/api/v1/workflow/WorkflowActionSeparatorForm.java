package com.dotcms.rest.api.v1.workflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulate the form for the WorkflowActionSeparator
 * @author jsanca
 */
public class WorkflowActionSeparatorForm {

    private final String schemeId;
    private final String stepId;

    @JsonCreator
    public WorkflowActionSeparatorForm(@JsonProperty("schemeId") final String schemeId,
                                       @JsonProperty("stepId") final String stepId) {
        this.schemeId = schemeId;
        this.stepId = stepId;
    }

    public String getSchemeId() {
        return schemeId;
    }

    public String getStepId() {
        return stepId;
    }
}
