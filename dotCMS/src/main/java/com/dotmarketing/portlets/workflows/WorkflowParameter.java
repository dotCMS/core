package com.dotmarketing.portlets.workflows;

import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;

public enum WorkflowParameter {
    // Email parameters
    CUSTOM_HEADERS(
            "customHeaders",
            "Custom Headers <br>(one per line: Header-Name: Header-Value)",
            "",
            false

    );

    private final String key;
    private final String description;
    private final String defaultValue;
    private final boolean required;


    WorkflowParameter(String key, String description, String defaultValue, boolean required) {
        this.key = key;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public WorkflowActionletParameter toWorkflowActionletParameter() {
        return new WorkflowActionletParameter(key, description, defaultValue, required);
    }
}
