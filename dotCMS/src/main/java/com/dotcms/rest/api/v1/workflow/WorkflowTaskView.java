package com.dotcms.rest.api.v1.workflow;

import java.util.Date;

/**
 * Lightweight view for exposing WorkflowTask data via REST.
 * Provides a fluent Builder used by WorkflowHelper.toWorkflowTaskView.
 */
public class WorkflowTaskView {

    private final String id;
    private final String assignedTo;
    private final String assignedUserName;
    private final String createdBy;
    private final String description;
    private final String belongsTo;
    private final Date creationDate;
    private final Date dueDate;
    private final long languageId;
    private final Date modDate;
    private final String title;
    private final String status;

    private WorkflowTaskView(Builder b) {
        this.id = b.id;
        this.assignedTo = b.assignedTo;
        this.assignedUserName = b.assignedUserName;
        this.createdBy = b.createdBy;
        this.description = b.description;
        this.belongsTo = b.belongsTo;
        this.creationDate = b.creationDate;
        this.dueDate = b.dueDate;
        this.languageId = b.languageId;
        this.modDate = b.modDate;
        this.title = b.title;
        this.status = b.status;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String assignedTo;
        private String assignedUserName;
        private String createdBy;
        private String description;
        private String belongsTo;
        private Date creationDate;
        private Date dueDate;
        private long languageId;
        private Date modDate;
        private String title;
        private String status;

        public Builder id(String id) { this.id = id; return this; }
        public Builder assignedTo(String assignedTo) { this.assignedTo = assignedTo; return this; }
        public Builder assignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder belongsTo(String belongsTo) { this.belongsTo = belongsTo; return this; }
        public Builder creationDate(Date creationDate) { this.creationDate = creationDate; return this; }
        // Note: WorkflowHelper currently calls getDueDate(...). Provide both for compatibility.
        public Builder dueDate(Date dueDate) { this.dueDate = dueDate; return this; }
        public Builder getDueDate(Date dueDate) { this.dueDate = dueDate; return this; }
        public Builder languageId(long languageId) { this.languageId = languageId; return this; }
        public Builder modDate(Date modDate) { this.modDate = modDate; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public WorkflowTaskView build() { return new WorkflowTaskView(this); }
    }

    public String getId() { return id; }
    public String getAssignedTo() { return assignedTo; }
    public String getAssignedUserName() { return assignedUserName; }
    public String getCreatedBy() { return createdBy; }
    public String getDescription() { return description; }
    public String getBelongsTo() { return belongsTo; }
    public Date getCreationDate() { return creationDate; }
    public Date getDueDate() { return dueDate; }
    public long getLanguageId() { return languageId; }
    public Date getModDate() { return modDate; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
}
