package com.dotcms.rest.api.v1.container;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

public class ContainerWithStructuresView {

    private final Container container;
    private final List<ContainerStructure> containerStructures;

    @JsonCreator
    public ContainerWithStructuresView(final Container container,
            final List<ContainerStructure> containerStructures) {
        this.container = container;
        this.containerStructures = containerStructures;
    }

    public Container getContainer() {
        return container;
    }

    public List<ContainerStructure> getContainerStructures() {
        return containerStructures;
    }
}
