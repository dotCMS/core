package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;

import java.util.List;

/**
 * This Finder Strategy allows developers to correctly determine the best way of retrieving information about how a
 * given Container can be related to one or more Content Types in dotCMS. There are different ways of creating
 * Containers in the system. For example:
 * <ul>
 *    <li>Default Containers -- added via the dotCMS back-end UI.</li>
 *    <li>Containers as Files.</li>
 * </ul>
 * <p>This means that the metadata for each of them comes from different data sources. In that case, specific strategies
 * must be implemented in order to correctly validate and retrieve such information so that dotCMS can seamlessly work
 * with Containers, regardless the approach that Developers or Content Authors followed to create them.
 * </p>
 *
 * @author jsanca
 * @since May 12th, 2018
 */
public interface ContainerStructureFinderStrategy {

    /**
     * Verifies whether this Finder Strategy can be applied to the specified Container.
     *
     * @param container The {@link Container} whose Strategy must be validated.
     *
     * @return boolean If the current Finder Strategy can be applied to the Container, returns {@code true}. Otherwise,
     * returns {@code false}.
     */
    boolean test(final Container container);

    /**
     * Executes this Container-to-Content Type Finder Strategy on the specified Container. The result of this process is
     * the associations between the Container and one or more Content Types, which are retrieved correctly based on the
     * nature of the Container. Keep in mind that the correct approach is to:
     * <ol>
     *     <li>Call the {@link #test(Container)} to double-check that the Container metadata can be retrieved with this
     *     Finder Strategy.</li>
     *     <li>If it can, then call this method to get the appropriate data.</li>
     * </ol>>
     *
     * @param container The {@link Container} on which this Strategy will be applied.
     *
     * @return List of {@link ContainerStructure} objects containing the expected Container-to-Content Type information.
     */
    List<ContainerStructure> apply(final Container container);

} // E:O:F:ContainerStructureFinderStrategy.
