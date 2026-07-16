package com.dotcms.rest.api.v1.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = WorkflowSearcherForm.Builder.class)
public class WorkflowSearcherForm {

    private final String keywords;
    private final String assignedTo;
    private final int daysOld;
    private final String schemeId;
    private final String stepId;
    private final boolean open;
    private final boolean closed;
    private final String createdBy;
    private final boolean show4all;
    private final String orderBy;
    private final int count;
    private final int page;

    public String getKeywords() {
        return keywords;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public int getDaysOld() {
        return daysOld;
    }

    public String getSchemeId() {
        return schemeId;
    }

    public String getStepId() {
        return stepId;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public boolean isShow4all() {
        return show4all;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public int getCount() {
        return count;
    }

    public int getPage() {
        return page;
    }

    @Override
    public String toString() {
        return "WorkflowSearcherForm{" +
                "keywords='" + keywords + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", daysOld=" + daysOld +
                ", schemeId='" + schemeId + '\'' +
                ", stepId='" + stepId + '\'' +
                ", open=" + open +
                ", closed=" + closed +
                ", createdBy='" + createdBy + '\'' +
                ", show4all=" + show4all +
                ", orderBy='" + orderBy + '\'' +
                ", count=" + count +
                ", page=" + page +
                '}';
    }

    private WorkflowSearcherForm(final Builder builder) {

        this.keywords = builder.keywords;
        this.assignedTo = builder.assignedTo;
        this.daysOld = builder.daysOld;
        this.schemeId = builder.schemeId;

        this.stepId = builder.stepId;
        this.open = builder.open;
        this.closed = builder.closed;
        this.createdBy = builder.createdBy;

        this.show4all = builder.show4all;
        this.orderBy = builder.orderBy;
        this.count = builder.count;
        this.page = builder.page;
    }

    public static final class Builder {

        @JsonProperty
        String keywords;
        @JsonProperty
        String assignedTo;
        @JsonProperty
        int daysOld=-1;
        @JsonProperty
        String schemeId;
        @JsonProperty
        String stepId;
        @JsonProperty
        boolean open;
        @JsonProperty
        boolean closed;
        @JsonProperty
        String createdBy;
        @JsonProperty
        boolean show4all;

        @JsonProperty
        String orderBy;
        @JsonProperty
        int count = 20;
        @JsonProperty
        int page = 0;

        public Builder keywords(final String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder assignedTo(final String assignedTo) {
            this.assignedTo = assignedTo;
            return this;
        }

        public Builder daysOld(final int daysOld) {
            this.daysOld = daysOld;
            return this;
        }

        public Builder schemeId(final String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        public Builder stepId(final String stepId) {
            this.stepId = stepId;
            return this;
        }

        public Builder open(final boolean open) {
            this.open = open;
            return this;
        }

        public Builder closed(final boolean closed) {
            this.closed = closed;
            return this;
        }

        public Builder createdBy(final String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder show4all(final boolean show4all) {
            this.show4all = show4all;
            return this;
        }

        public Builder orderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder count(final int count) {
            this.count = count;
            return this;
        }

        public Builder page(final int page) {
            this.page = page;
            return this;
        }


        public WorkflowSearcherForm build() {
            return new WorkflowSearcherForm(this);
        }
    }
}
