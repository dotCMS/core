package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * This delegates has the responsability to auto asign a contentlet that has been check in but it is not on a workflow step.
 * @author jsanca
 */
public interface AutoAssignWorkflowDelegate {

    /**
     * Name to identify the delegate
     * @return
     */
    default String getName () { return this.getClass().getName(); }

    /**
     * This method will be called when a contentlet without step has been check in.
     * @param contentlet {@link Contentlet}
     * @param user {@link User}
     */
    void assign (final Contentlet contentlet, final User user);
}
