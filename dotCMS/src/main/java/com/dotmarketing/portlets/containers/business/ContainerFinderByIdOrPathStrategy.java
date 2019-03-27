package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.containers.model.Container;
import com.liferay.portal.model.User;

import java.util.function.Supplier;

/**
 * Strategy to find a container by id, relate or absolute path (file asset containers)
 * @author jsanca
 */
public interface ContainerFinderByIdOrPathStrategy {

    /**
     * Test if the Strategy could applies for the arguments
     * @param containerIdOrPath String could be uuid or path
     * @return boolean
     */
    boolean test (final String containerIdOrPath);

    /**
     * Applies the strategy
     * @param containerIdOrPath String  could be uuid or path, the path could contains or not the host,
     *                          if does not have a host, the supplier resourceHost will be called, if it returns null the default host will be used
     * @param user {@link User}
     * @param respectFrontEndPermissions {@link Boolean}
     * @param resourceHost {@link Supplier} of Host
     * @return Container
     */
    Container apply(final String containerIdOrPath, final User user, final boolean respectFrontEndPermissions, final Supplier<Host> resourceHost) throws NotFoundInDbException;
} // E:O:F:ContainerFinderByIdOrPathStrategy.
