package com.dotcms.rest.api.v1.workflow;

import com.dotcms.workflow.form.FireActionForm;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

/**
 * A command executes an api call (checkin, publish or unpublish, etc) and fires a workflow action as part of the command
 * @author jsanca
 */
public interface SystemActionApiFireCommand {

    /**
     * Fires command
     * @param contentlet {@link Contentlet}
     * @param contentletDependencies {@link ContentletDependencies}
     * @return Contentlet
     */
    Contentlet fire(Contentlet contentlet, ContentletDependencies contentletDependencies) throws DotDataException, DotSecurityException;
}
