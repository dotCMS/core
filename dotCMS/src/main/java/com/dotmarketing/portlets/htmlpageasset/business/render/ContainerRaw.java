package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;

/**
 * Represents the information of the {@link Container} and its respective {@link ContainerStructure}
 * relationships. These relationships define what Content Types can be added to the container.
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class ContainerRaw implements Serializable {

    private static final long serialVersionUID = 1572918359580445566L;

    private final Container container;
    private final List<ContainerStructure> containerStructures;
    private final Map<String, List<Contentlet>> contentlets;

    /**
     * Creates a new instance of the ContainerRendered.
     *
     * @param container The {@link Container} in the HTML Page.
     * @param containerStructures The list of {@link ContainerStructure} relationships. the browser.
     */
    public ContainerRaw(
            final Container container,
            final List<ContainerStructure> containerStructures,
            final Map<String, List<Contentlet>> contentlets) {
        this.container = container;
        this.containerStructures =  (containerStructures != null)  ?  ImmutableList.copyOf(containerStructures) :  ImmutableList.of();
        this.contentlets = contentlets;
    }

    public Map<String, List<Contentlet>> getContentlets() {
        return contentlets;
    }

    /**
     * Returns the page container.
     *
     * @return The {@link Container} in the page.
     */
    @JsonIgnore
    public Container getContainer() {
        return container;
    }

    @JsonProperty("container")
    public ContainerView getContainerView() {
        return new ContainerView(container);
    }

    /**
     * Returns the relationships that determine what Content Types can be added to a specific Container.
     *
     * @return The list of {@link ContainerStructure} relationships.
     */
    public List<ContainerStructure> getContainerStructures() {
        return containerStructures;
    }

    @Override
    public String toString() {
        return "ContainerRendered{" + "container=" + container + ", containerStructures=" + containerStructures + ", contentlets='"
                + contentlets + '\'' + '}';
    }
}
