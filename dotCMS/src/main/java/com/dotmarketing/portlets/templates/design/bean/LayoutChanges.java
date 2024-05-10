package com.dotmarketing.portlets.templates.design.bean;

import com.dotcms.rest.api.v1.workflow.SchemesAndSchemesContentTypeView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a set of changes apply to a {@link TemplateLayout}
 */
public class LayoutChanges {

    private Collection<ContainerChanged> changes = new ArrayList<>();

    private LayoutChanges(Builder builder) {
        this.changes = builder.changes;
    }

    /**
     * Get all the changes
     * @return
     */
    public Collection<ContainerChanged> getAll() {
        return changes;
    }

    public static class Builder {

        private Collection<ContainerChanged> changes = new ArrayList<>();

        /**
         * Add a move change on the set, it means a Container was moved from a old UUID to a new UUID
         *
         * @param containerId  Container's ID
         * @param oldIntanceId Old UUID
         * @param newIntanceId New UUID
         */
        public Builder change(final String containerId, final String oldIntanceId, final String newIntanceId) {
            changes.add(new ContainerChanged(getContainerId(containerId), oldIntanceId, newIntanceId));
            return this;
        }


        /**
         * Add a remove change on the set, it means a Container was removed from the Layout
         *
         * @param containerId  Container's ID
         * @param oldIntanceId Old UUID
         */
        public Builder remove(final String containerId, final String oldIntanceId) {
            changes.add(new ContainerChanged(getContainerId(containerId), oldIntanceId, ContainerUUID.UUID_DEFAULT_VALUE));
            return this;
        }

        /**
         * Add a included change on the set, it means that a Container was removed from the Layout
         *
         * @param containerId
         * @param newIntanceId
         */
        public Builder include(final String containerId, final String newIntanceId) {
            changes.add(new ContainerChanged(getContainerId(containerId), ContainerUUID.UUID_DEFAULT_VALUE, newIntanceId));
            return this;
        }

        private String getContainerId(String containerId) {

            if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId)) {
                try {
                    return APILocator.getContainerAPI().findContainer(containerId, APILocator.systemUser(),
                                    false, false)
                            .map(Container::getIdentifier)
                            .orElse(containerId);
                } catch (DotDataException | DotSecurityException e) {
                    return containerId;
                }
            }

            return containerId;
        }

        public LayoutChanges build() {
            return new LayoutChanges(this);
        }
    }

    public static class ContainerChanged {
        private String containerId;
        private String newInstanceId;
        private String oldInstanceId;

        public ContainerChanged(String containerId,  String oldInstanceId, String newInstanceId) {
            this.containerId = containerId;
            this.newInstanceId = newInstanceId;
            this.oldInstanceId = oldInstanceId;
        }

        public String getContainerId() {
            return containerId;
        }

        public String getNewInstanceId() {
            return newInstanceId;
        }

        public String getOldInstanceId() {
            return oldInstanceId;
        }

        public boolean isRemove() {
            return ContainerUUID.UUID_DEFAULT_VALUE.equals(newInstanceId);
        }

        public boolean isNew() {
            return ContainerUUID.UUID_DEFAULT_VALUE.equals(oldInstanceId);
        }

        public boolean isMoved() {
            return !ContainerUUID.UUID_DEFAULT_VALUE.equals(newInstanceId) &&
                   !ContainerUUID.UUID_DEFAULT_VALUE.equals(oldInstanceId);
        }
    }
}
