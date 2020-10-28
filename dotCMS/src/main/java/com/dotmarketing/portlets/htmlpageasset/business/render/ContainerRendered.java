package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;

/**
 * Represents the information of the {@link Container} and its respective
 * {@link ContainerStructure} relationships. These relationships define what Content Types can be
 * added to the container.
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class ContainerRendered extends ContainerRaw {

    private static final long serialVersionUID = 1572918359580445566L;


    private final Map<String, String> rendered;


    /**
     * Creates a new instance of the ContainerRendered.
     *
     * @param container           The {@link Container} in the HTML Page.
     * @param containerStructures The list of {@link ContainerStructure} relationships.
     *                           the browser.
     */
    public ContainerRendered(final Container container, final List<ContainerStructure> containerStructures,
                             final Map<String, String> rendered, final Map<String,List<Contentlet>> contentlets) {
        super(container, containerStructures, contentlets);

        this.rendered = rendered;

    }
    
    /**
     * Creates a new instance of the ContainerRendered.
     *
     * @param container           The {@link Container} in the HTML Page.
     * @param containerStructures The list of {@link ContainerStructure} relationships.
     *                           the browser.
     */
    public ContainerRendered(final ContainerRaw containerRaw, final Map<String, String> rendered) {
        this(containerRaw.getContainer(), containerRaw.getContainerStructures(), rendered, containerRaw.getContentlets());

    }

    /**
     * Returns the {@link Container} as rendered in the browser.
     *
     * @return The HTML representation of the container.
     */
    public Map<String, String> getRendered() {
        return rendered;
    }


    @Override
    public String toString() {
        return "ContainerRendered{" + "container=" + getContainer() + ", containerStructures=" +
                getContainerStructures() + ", rendered='" + rendered + '\'' + '}';
    }
}
