package com.dotcms.rest.api.v1.container;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

/**
 * It represents a container with its structure/content type. It contents a List of the
 * {@link com.dotmarketing.beans.ContainerStructure} for each Structure
 * and a container {@link com.dotmarketing.portlets.containers.model.Container}
 */

public class ContainerWithContentTypesView {

    private final Container container;
    private final List<ContainerStructure> contentTypes;

    @JsonCreator
    public ContainerWithContentTypesView(final Container container,
            final List<ContainerStructure> contentTypes) {
        this.container = container;
        this.contentTypes = contentTypes;
    }

    public Container getContainer() {
        return container;
    }

    public List<ContainerStructure> getContentTypes() {
        return contentTypes;
    }
}
