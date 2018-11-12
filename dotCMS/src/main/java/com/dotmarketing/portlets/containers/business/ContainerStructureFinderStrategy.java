package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;

import java.util.List;

/**
 * Depending on the type of {@link com.dotmarketing.portlets.containers.model.Container}
 * will use an strategy to get the {@link com.dotmarketing.beans.ContainerStructure}
 * @author jsanca
 */
public interface ContainerStructureFinderStrategy {

    /**
     * Test if the Strategy could applies for the arguments
     * @param container
     * @return boolean
     */
    boolean test(final Container container);

    /**
     * Applies the strategy
     * @param container
     * @return List of ContainerStructure's
     */
    List<ContainerStructure> apply(final Container container);
} // E:O:F:ContainerStructureFinderStrategy.
