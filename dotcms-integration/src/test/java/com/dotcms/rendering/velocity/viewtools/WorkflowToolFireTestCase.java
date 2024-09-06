package com.dotcms.rendering.velocity.viewtools;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.function.Predicate;

public class WorkflowToolFireTestCase {

    private String workflowActionId;
    private Map<String, Object> properties;
    private List<Assertion> assertions = new ArrayList<>();
    private String userId = "system";
    private boolean workflowToolAllowFrontEndSaving;

    String getWorkflowActionId() {
        return workflowActionId;
    }

    void setWorkflowActionId(final String workflowActionId) {
        this.workflowActionId = workflowActionId;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(final Map<String, Object> properties) {
        this.properties = properties;
    }

    List<Assertion> getAssertions() {
        return assertions;
    }

    void addAssertion(final Assertion assertion) {
        this.assertions.add(assertion);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public boolean isWorkflowToolAllowFrontEndSaving() {
        return workflowToolAllowFrontEndSaving;
    }

    public void setWorkflowToolAllowFrontEndSaving(boolean workflowToolAllowFrontEndSaving) {
        this.workflowToolAllowFrontEndSaving = workflowToolAllowFrontEndSaving;
    }

    static class Assertion {
        final Predicate<Contentlet> predicate;
        final String message;

        public Assertion(final Predicate<Contentlet> predicate, final String message) {
            this.predicate = predicate;
            this.message = message;
        }

        public Predicate<Contentlet> getPredicate() {
            return predicate;
        }

        public String getMessage() {
            return message;
        }
    }
}
