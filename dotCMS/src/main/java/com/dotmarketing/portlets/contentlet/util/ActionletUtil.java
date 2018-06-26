package com.dotmarketing.portlets.contentlet.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.List;

public class ActionletUtil {

    /**
     *
     * @param action
     * @return
     */
    public static boolean hasPushPublishActionlet(final WorkflowAction action) {
        try {
            final List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI()
                    .findActionClasses(action);
            for (WorkflowActionClass actionlet : actionlets) {
                if (actionlet.getActionlet() != null && actionlet.getActionlet().getClass()
                        .getCanonicalName().equals(PushPublishActionlet.class.getCanonicalName())) {
                    return true;
                }
            }
        } catch (DotDataException e) {
            Logger.error(ActionletUtil.class, String.format(
                    "Can't determine weather or not action '%s' has PushPublishActionlet.",
                    action.getName()), e);
        }
        return false;
    }

    /**
     *
     * @param action
     * @return
     */
    public static boolean requiresPopupAdditionalParams(final WorkflowAction action){
        return (action.isAssignable() || action.isCommentable() || UtilMethods.isSet(action.getCondition()) || hasPushPublishActionlet(action));
    }
}
