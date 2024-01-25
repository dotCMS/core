package com.dotcms.rest.api.v1.page;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;

/**
 * Represents a Container layout change in a dotCMS Template.
 *
 * <p>An instance of a Container in a Template is defined by its ID -- or file path for File
 * Containers -- and its current instance ID. Such an instance ID allows content authors to add the
 * same Container more than once in a Template. If one or more Containers of the same type are moved
 * or deleted from the Template, the instance IDs of the remaining Containers may need to be updated
 * so that such IDs are sorted sequentially. This class helps keep track of such changes.</p>
 *
 * <p>Instance IDs are simple numeric values that are sorted in ascendant order, from top to bottom,
 * from left to right, and their value starts with 1.</p>
 *
 * @author Freddy Rodriguez
 * @since Apr 23rd, 2018
 */
public class ContainerUUIDChanged {

    private final ContainerUUID oldContainer;
    private final ContainerUUID newContainer;

    public ContainerUUIDChanged(final ContainerUUID oldContainer, final ContainerUUID newContainer) {
        this.oldContainer = oldContainer;
        this.newContainer = newContainer;
    }

    /**
     * Returns the Container instance ID information before the Template's layout was changed.
     *
     * @return The {@link ContainerUUID} instance before the Template's layout was changed.
     */
    public ContainerUUID getPreviousInfo() {
        return oldContainer;
    }

    /**
     * Returns the Container instance ID information after the Template's layout was changed. This
     * can translate into the instance ID being updated.
     *
     * @return The {@link ContainerUUID} instance after the Template's layout was changed.
     */
    public ContainerUUID getNewInfo() {
        return newContainer;
    }

}
