package com.dotcms.rendering.velocity.services;

import com.dotmarketing.portlets.containers.model.Container;

/**
 * Encapsulates the logic to Find a Container based on the strategy (it could be db, file system, etc)
 * @author jsanca
 */
public interface ContainerFinderStrategy {

    /**
     * Test if the Strategy could applies for the arguments
     * @param key
     * @return boolean
     */
    boolean test(final VelocityResourceKey key);

    /**
     * Applies the strategy
     * @param key
     * @return Container
     */
    Container apply(final VelocityResourceKey key);
}
