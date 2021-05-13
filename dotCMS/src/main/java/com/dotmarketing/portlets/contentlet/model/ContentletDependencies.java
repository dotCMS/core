package com.dotmarketing.portlets.contentlet.model;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.liferay.portal.model.User;

import java.util.List;

public class ContentletDependencies {

    private final User                    modUser;
    private final ContentletRelationships relationships;
    private final String                  workflowActionId;
    private final String                  workflowActionComments;
    private final String                  workflowAssignKey;
    private final String                  workflowPublishDate;
    private final String                  workflowPublishTime;
    private final String                  workflowExpireDate;
    private final String                  workflowExpireTime;
    private final String                  workflowNeverExpire;
    private final String                  workflowWhereToSend;
    private final String                  workflowFilterKey;
    private final String                  workflowIWantTo;
    private final List<Category>          categories;
    private final boolean                 respectAnonymousPermissions;
    private final boolean                 generateSystemEvent;
    private final IndexPolicy             indexPolicy;
    private final IndexPolicy             indexPolicyDependencies;
    private final List<Permission>        permissions;

    private ContentletDependencies(final ContentletDependencies.Builder builder) {

        this.modUser                     = builder.modUser;
        this.relationships               = builder.relationships;
        this.workflowActionId            = builder.workflowActionId;
        this.workflowActionComments      = builder.workflowActionComments;
        this.workflowAssignKey           = builder.workflowAssignKey;
        this.categories                  = builder.categories;
        this.respectAnonymousPermissions = builder.respectAnonymousPermissions;
        this.generateSystemEvent         = builder.generateSystemEvent;
        this.indexPolicy                 = builder.indexPolicy;
        this.indexPolicyDependencies     = builder.indexPolicyDependencies;
        this.permissions                 = builder.permissions;
        this.workflowPublishDate = builder.workflowPublishDate;
        this.workflowPublishTime = builder.workflowPublishTime;
        this.workflowExpireDate = builder.workflowExpireDate;
        this.workflowExpireTime = builder.workflowExpireTime;
        this.workflowNeverExpire = builder.workflowNeverExpire;
        this.workflowWhereToSend = builder.workflowWhereToSend;
        this.workflowFilterKey = builder.workflowFilterKey;
        this.workflowIWantTo = builder.workflowIWantTo;

    }

    public User getModUser() {
        return modUser;
    }

    public ContentletRelationships getRelationships() {
        return relationships;
    }

    public String getWorkflowActionId() {
        return workflowActionId;
    }

    public String getWorkflowActionComments() {
        return workflowActionComments;
    }

    public String getWorkflowAssignKey() {
        return workflowAssignKey;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public boolean isRespectAnonymousPermissions() {
        return respectAnonymousPermissions;
    }

    public boolean isGenerateSystemEvent() {
        return generateSystemEvent;
    }

    public IndexPolicy getIndexPolicy() {
        return indexPolicy;
    }

    public IndexPolicy getIndexPolicyDependencies() {
        return indexPolicyDependencies;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public String getWorkflowPublishDate() { return workflowPublishDate; }

    public String getWorkflowPublishTime() {
        return workflowPublishTime;
    }

    public String getWorkflowExpireDate() {
        return workflowExpireDate;
    }

    public String getWorkflowExpireTime() {
        return workflowExpireTime;
    }

    public String getWorkflowNeverExpire() {
        return workflowNeverExpire;
    }

    public String getWorkflowWhereToSend() {
        return workflowWhereToSend;
    }

    public String getWorkflowFilterKey() {
        return workflowFilterKey;
    }

    public String getWorkflowIWantTo() {
        return workflowIWantTo;
    }

    public static final class Builder {

        private User modUser;
        private ContentletRelationships relationships;
        private String workflowActionId;
        private String workflowActionComments;
        private String workflowAssignKey;
        private String workflowPublishDate;
        private String workflowPublishTime;
        private String workflowExpireDate;
        private String workflowExpireTime;
        private String workflowNeverExpire;
        private String workflowWhereToSend;
        private String workflowFilterKey;
        private String workflowIWantTo;
        private List<Category> categories;
        private boolean respectAnonymousPermissions;
        private boolean generateSystemEvent;
        private IndexPolicy  indexPolicy = null;
        private IndexPolicy  indexPolicyDependencies = null;
        private List<Permission>        permissions  = null;

        public ContentletDependencies build() {
            return new ContentletDependencies(this);
        }

        public ContentletDependencies.Builder permissions(final List<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }

        public ContentletDependencies.Builder modUser(final User user) {
            this.modUser = user;
            return this;
        }

        public ContentletDependencies.Builder relationships(final ContentletRelationships relationships) {
            this.relationships = relationships;
            return this;
        }

        public ContentletDependencies.Builder workflowActionId(final String workflowActionId) {
            this.workflowActionId = workflowActionId;
            return this;
        }

        public ContentletDependencies.Builder workflowActionId(final WorkflowAction workflowAction) {
            this.workflowActionId = workflowAction.getId();
            return this;
        }

        public ContentletDependencies.Builder workflowActionComments(final String workflowActionComments) {
            this.workflowActionComments = workflowActionComments;
            return this;
        }

        public ContentletDependencies.Builder workflowAssignKey(final String workflowAssignKey) {
            this.workflowAssignKey = workflowAssignKey;
            return this;
        }

        public ContentletDependencies.Builder workflowPublishDate(final String workflowPublishDate) {
            this.workflowPublishDate = workflowPublishDate;
            return this;
        }

        public ContentletDependencies.Builder workflowPublishTime(final String workflowPublishTime) {
            this.workflowPublishTime = workflowPublishTime;
            return this;
        }

        public ContentletDependencies.Builder workflowExpireDate(final String workflowExpireDate) {
            this.workflowExpireDate = workflowExpireDate;
            return this;
        }

        public ContentletDependencies.Builder workflowExpireTime(final String workflowExpireTime) {
            this.workflowExpireTime = workflowExpireTime;
            return this;
        }

        public ContentletDependencies.Builder workflowNeverExpire(final String workflowNeverExpire) {
            this.workflowNeverExpire = workflowNeverExpire;
            return this;
        }

        public ContentletDependencies.Builder workflowWhereToSend(final String workflowWhereToSend) {
            this.workflowWhereToSend = workflowWhereToSend;
            return this;
        }

        public ContentletDependencies.Builder workflowFilterKey(final String workflowFilterKey) {
            this.workflowFilterKey = workflowFilterKey;
            return this;
        }

        public ContentletDependencies.Builder workflowIWantTo(final String workflowIWantTo) {
            this.workflowIWantTo = workflowIWantTo;
            return this;
        }

        public ContentletDependencies.Builder categories(final List<Category> categories) {
            this.categories = categories;
            return this;
        }

        public ContentletDependencies.Builder respectAnonymousPermissions(final boolean respectAnonymousPermissions) {
            this.respectAnonymousPermissions = respectAnonymousPermissions;
            return this;
        }

        public ContentletDependencies.Builder generateSystemEvent(final boolean generateSystemEvent) {
            this.generateSystemEvent = generateSystemEvent;
            return this;
        }

        public ContentletDependencies.Builder indexPolicy (final IndexPolicy indexPolicy) {

            this.indexPolicy = indexPolicy;
            return this;
        }

        public ContentletDependencies.Builder indexPolicyDependencies (final IndexPolicy indexPolicy) {

            this.indexPolicyDependencies = indexPolicy;
            return this;
        }

    }
}
