package com.dotmarketing.portlets.templates.design.bean;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a set of changes apply to a {@link TemplateLayout}
 */
public class LayoutChanges {

    private Collection<ContainerChanged> changes = new ArrayList<>();

    public void change(final String containerId, final String oldIntanceId, final String newIntanceId) {
        changes.add(new ContainerChanged(containerId, oldIntanceId, newIntanceId));
    }

    public Collection<ContainerChanged> getAll() {
        return changes;
    }

    public void remove(final String containerId, final String oldIntanceId) {
        changes.add(new ContainerChanged(containerId, oldIntanceId, ContainerUUID.UUID_DEFAULT_VALUE));
    }

    public void include(final String containerId, final String newIntanceId) {
        changes.add(new ContainerChanged(containerId, ContainerUUID.UUID_DEFAULT_VALUE, newIntanceId));
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
