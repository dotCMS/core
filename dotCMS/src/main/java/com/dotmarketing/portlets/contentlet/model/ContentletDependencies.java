package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.liferay.portal.model.User;
import org.elasticsearch.action.support.WriteRequest;

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
    private final Object                  refreshPolicy;

    private ContentletDependencies(final ContentletDependencies.Builder builder) {

        this.modUser                     = builder.modUser;
        this.relationships               = builder.relationships;
        this.workflowActionId            = builder.workflowActionId;
        this.workflowActionComments      = builder.workflowActionComments;
        this.workflowAssignKey           = builder.workflowAssignKey;
        this.categories                  = builder.categories;
        this.respectAnonymousPermissions = builder.respectAnonymousPermissions;
        this.generateSystemEvent         = builder.generateSystemEvent;
        this.refreshPolicy               = builder.refreshPolicy;

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

    public Object getRefreshPolicy() {
        return refreshPolicy;
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
        private Object  refreshPolicy = null;

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

        /**
         * A process saved with this flag on, will wait until the content is already searchable in the index.
         */
        public ContentletDependencies.Builder waitUntilContentRefresh () {

            this.refreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL;
            return this;
        }

        /**
         * The contentlet refreshing on the index will be immediate refreshed.
         * Important node: use this flag only and just only development environments, on production might experiments high scalability issues.
         */
        @VisibleForTesting
        public ContentletDependencies.Builder immediateContentRefresh () {

            this.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE;
            return this;
        }
    }
}
