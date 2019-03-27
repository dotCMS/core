package com.dotmarketing.portlets.workflows.actionlet;


import com.dotcms.business.WrapInTransaction;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.event.CopyActionletEvent;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Copy the edited contentlet
 * @author jsanca
 */
public class CopyActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID              = 1L;
    public static final String NOTIFY_SYNC_COPY_EVENT       = "notify.sync.copy.event";
    private final ContentletAPI contentletAPI               = APILocator.getContentletAPI();
    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();



    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Copy Contentlet";
    }

    @Override
    public String getHowTo() {
        return "This workflow actionlet copies the edited contentlet ";
    }

    @Override
    public void executePreAction(final WorkflowProcessor processor,
                    final Map<String, WorkflowActionClassParameter> params)
                    throws WorkflowActionFailureException, DotContentletValidationException {

        final  Contentlet contentlet = processor.getContentlet();

        try {

            if (!contentlet.isLocked()) {

                this.performCopy(contentlet, processor.getUser());
            }
        } catch (DotContentletStateException | DotDataException | DotSecurityException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }
    } // executePreAction.

    @Override
    public void executeAction(final WorkflowProcessor processor,
                    final Map<String, WorkflowActionClassParameter> params)
                    throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();

        try {

            if (contentlet.isLocked()) {

                this.performCopy(contentlet, processor.getUser());
            }
        } catch (DotContentletStateException | DotDataException | DotSecurityException | IOException e) {

            Logger.error(this, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }
    } // executeAction.

    @WrapInTransaction
    private void performCopy(final Contentlet contentlet,
                             final User user) throws DotDataException, DotSecurityException, IOException {

        final Contentlet copyContentlet = this.contentletAPI.copyContentlet(contentlet, user, false);

        if (null != copyContentlet) {

            if (contentlet.getMap().containsKey(NOTIFY_SYNC_COPY_EVENT) && contentlet.getBoolProperty(NOTIFY_SYNC_COPY_EVENT)) { // for testing

                this.localSystemEventsAPI.notify(new CopyActionletEvent(contentlet, copyContentlet));
            } else {
                this.localSystemEventsAPI.asyncNotify(new CopyActionletEvent(contentlet, copyContentlet));
            }
        }
    } // performCopy.

}
