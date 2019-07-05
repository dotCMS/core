package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

public interface AutoAssignWorkflowDelegate {

    default String getName () { return this.getName(); }

    void assign (final Contentlet contentlet, final User user);
}
