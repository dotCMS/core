package com.dotmarketing.portlets.workflows.model;

/**
 * This represents a single checkbox parameter
 * @author jsanca
 */
public class CheckboxWorkflowActionletParameter extends WorkflowActionletParameter {

    public CheckboxWorkflowActionletParameter(final String key, final String displayName,
                                              final String defaultValue, final boolean isRequired) {
        super(key, displayName, defaultValue, isRequired);
    }

    @Override
    public String toString() {
        return "CheckboxWorkflowActionletParameter [key=" + getKey() + "]";
    }

}
