package com.dotmarketing.portlets.contentlet.util;

import com.dotcms.util.AnnotationUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.actionlet.Actionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.List;

public class ActionletUtil {

    /**
     * This method tells you if an action has been set to have a sub actionlet Push-Publish
     * @param action
     * @return
     */
    public static boolean hasPushPublishActionlet(final WorkflowAction action) {
        try {
            final List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(action);
            for (final WorkflowActionClass actionletClass : actionlets) {

                final Actionlet actionlet = AnnotationUtils.
                        getBeanAnnotation(ReflectionUtils.getClassFor(actionletClass.getClazz()), Actionlet.class);

                if (null != actionlet && actionlet.pushPublish()) {
                    return true;
                }

            }
        } catch (DotDataException e) {
            Logger.error(ActionletUtil.class, String.format(
                    "Can't determine if action '%s' has PushPublishActionlet.",
                    action.getName()), e);
        }
        return false;
    }

    /**
     * Evaluates an action and tells you if it requires the additional info that is collected via a popup dialog.
     * e.g. Comments, Assign, Push-Publish.
     * @param action
     * @return
     */
    public static boolean requiresPopupAdditionalParams(final WorkflowAction action){
        return (action.isAssignable() || action.isCommentable() || UtilMethods.isSet(action.getCondition()) || hasPushPublishActionlet(action));
    }
}
