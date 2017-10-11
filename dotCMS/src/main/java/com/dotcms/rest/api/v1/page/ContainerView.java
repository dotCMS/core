package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;

import java.io.Serializable;
import java.util.List;

/**
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class ContainerView implements Serializable {

    final Container container;
    final List<ContainerStructure> containerStructures;
    final String rendered;

    public ContainerView(Container container, List<ContainerStructure> containerStructures,
                         String rendered) {
        this.container = container;
        this.containerStructures = containerStructures;
        this.rendered = rendered;
    }

    public Container getContainer() {
        return container;
    }

    public String getRendered() {
        return rendered;
    }

    public List<ContainerStructure> getContainerStructures() {
        return containerStructures;
    }

}
