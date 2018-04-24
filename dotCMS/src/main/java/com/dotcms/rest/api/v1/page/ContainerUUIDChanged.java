package com.dotcms.rest.api.v1.page;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;

/**
 * It is a change do by the end point into a {@link ContainerUUID}
 */
public class ContainerUUIDChanged {
    private final ContainerUUID oldContainer;
    private final ContainerUUID newContainer;

    public ContainerUUIDChanged(final ContainerUUID oldContainer, final ContainerUUID newContainer) {
        this.oldContainer = oldContainer;
        this.newContainer = newContainer;
    }

    public ContainerUUID getOld() {
        return oldContainer;
    }

    public ContainerUUID getNew() {
        return newContainer;
    }
}
