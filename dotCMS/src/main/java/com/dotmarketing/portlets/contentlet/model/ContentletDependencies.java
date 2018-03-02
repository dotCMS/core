package com.dotmarketing.portlets.contentlet.model;

import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.liferay.portal.model.User;

import java.util.List;

public class ContentletDependencies {

    private final User                    modUser;
    private final ContentletRelationships relationships;
    private final String                  workflowActionId;
    private final String                  workflowActionComments;
    private final String                  workflowAssignKey;
    private final List<Category>          categories;
    private final boolean                 respectAnonymousPermissions;
    private final boolean                 generateSystemEvent;

    private ContentletDependencies(final ContentletDependencies.Builder builder) {

        this.modUser                     = builder.modUser;
        this.relationships               = builder.relationships;
        this.workflowActionId            = builder.workflowActionId;
        this.workflowActionComments      = builder.workflowActionComments;
        this.workflowAssignKey           = builder.workflowAssignKey;
        this.categories                  = builder.categories;
        this.respectAnonymousPermissions = builder.respectAnonymousPermissions;
        this.generateSystemEvent         = builder.generateSystemEvent;

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

    public static final class Builder {

        private User modUser;
        private ContentletRelationships relationships;
        private String workflowActionId;
        private String workflowActionComments;
        private String workflowAssignKey;
        private List<Category> categories;
        private boolean respectAnonymousPermissions;
        private boolean generateSystemEvent;

        public ContentletDependencies build() {
            return new ContentletDependencies(this);
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

        public ContentletDependencies.Builder workflowActionComments(final String workflowActionComments) {
            this.workflowActionComments = workflowActionComments;
            return this;
        }

        public ContentletDependencies.Builder workflowAssignKey(final String workflowAssignKey) {
            this.workflowAssignKey = workflowAssignKey;
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
    }
}
