package com.dotcms.rest.api.v1.page;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;

/**
 * Created by freddyrodriguez on 4/17/18.
 */
public class ContainerUUIDChanged {
    private ContainerUUID oldContainer;
    private ContainerUUID newContainer;

    public ContainerUUIDChanged(ContainerUUID oldContainer, ContainerUUID newContainer) {
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
