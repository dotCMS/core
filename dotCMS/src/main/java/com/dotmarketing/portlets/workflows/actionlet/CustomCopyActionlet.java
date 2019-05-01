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
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Custom Copy the edited contentlet
 * It is basically the same {@link CopyActionlet} but includes an optional pre and post hook checkin
 * @author jsanca
 */
public class CustomCopyActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID              = 1L;
    public static final String NOTIFY_SYNC_COPY_EVENT       = "notify.sync.copy.event";
    private final ContentletAPI contentletAPI               = APILocator.getContentletAPI();
    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        final List<WorkflowActionletParameter> paramList = new ArrayList<>();
        paramList.add(new WorkflowActionletParameter("precheckin", "Pre  Hook Checkin Code", null, true));
        paramList.add(new WorkflowActionletParameter("postcheckin","Post Hook Checkin Code", null, true));
        return paramList;
    }

    @Override
    public String getName() {
        return "Custom Copy Contentlet";
    }

    @Override
    public String getHowTo() {
        return "This workflow actionlet copies the edited contentlet with a velocity hooks for pre checkin and post checkin";
    }

    @Override
    public void executePreAction(final WorkflowProcessor processor,
                    final Map<String, WorkflowActionClassParameter> params)
                    throws WorkflowActionFailureException, DotContentletValidationException {

        final  Contentlet contentlet = processor.getContentlet();

        try {

            if (!contentlet.isLocked()) {

                this.performCopy(processor, contentlet, params, processor.getUser());
            }
        } catch (DotContentletStateException | DotDataException | DotSecurityException e) {

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

                this.performCopy(processor, contentlet, params, processor.getUser());
            }
        } catch (DotContentletStateException | DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }
    } // executeAction.

    @WrapInTransaction
    private void performCopy(
            final WorkflowProcessor processor, final Contentlet contentlet,
                             final Map<String, WorkflowActionClassParameter> params,
                             final User user) throws DotDataException, DotSecurityException {

        final WorkflowActionClassParameter precheckinCode  = params.get("precheckin");
        final WorkflowActionClassParameter postcheckinCode = params.get("postcheckin");

        final Contentlet copyContentlet = this.contentletAPI.copyContentlet(contentlet, user, false,
                Arrays.asList(newContentlet -> this.processHook(processor, precheckinCode,  newContentlet)),
                Arrays.asList(newContentlet -> this.processHook(processor, postcheckinCode, newContentlet)));

        if (null != copyContentlet) {

            if (contentlet.getMap().containsKey(NOTIFY_SYNC_COPY_EVENT) && contentlet.getBoolProperty(NOTIFY_SYNC_COPY_EVENT)) { // for testing

                this.localSystemEventsAPI.notify(new CopyActionletEvent(contentlet, copyContentlet));
            } else {
                this.localSystemEventsAPI.asyncNotify(new CopyActionletEvent(contentlet, copyContentlet));
            }
        }
    } // performCopy.

    private Contentlet processHook(final WorkflowProcessor processor,
                                     final WorkflowActionClassParameter hookCode,
                                     final Contentlet contentlet) {

        final Context velocityContext = VelocityUtil.getBasicContext();
        velocityContext.put("workflow", processor);
        velocityContext.put("user", processor.getUser());
        velocityContext.put("contentlet", contentlet);
        velocityContext.put("content", contentlet);

        try {
            VelocityUtil.eval(hookCode.getValue(), velocityContext);
        } catch (Exception e1) {
            Logger.warn(this.getClass(), "unable to parse comment, falling back" + e1);
        }

        return contentlet;
    }

}
