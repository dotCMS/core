package com.dotmarketing.portlets.contentlet.util;

import com.dotcms.util.AnnotationUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.actionlet.Actionlet;
import com.dotmarketing.portlets.workflows.actionlet.MoveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class to recover actionlets metadata
 * @author jsanca
 */
public class ActionletUtil {

    /**
     * This method tells you if an action has been set to have a sub actionlet only on batch
     * @param action
     * @return
     */
    public static boolean hasOnlyBatchActionlet(final WorkflowAction action) {
        try {

            if (Objects.nonNull(action)) {
                final List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(action);

                if (Objects.nonNull(actionlets)) {
                    for (final WorkflowActionClass actionletClass : actionlets) {

                        if (Objects.nonNull(actionletClass)) {
                            final WorkFlowActionlet workFlowActionlet = APILocator.getWorkflowAPI().findActionlet(actionletClass.getClazz());

                            if (Objects.nonNull(workFlowActionlet)) {
                                final Actionlet actionlet = AnnotationUtils.
                                        getBeanAnnotation(workFlowActionlet.getClass(), Actionlet.class);

                                if (Objects.nonNull(actionlet) && actionlet.onlyBatch()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (DotDataException e) {
            Logger.error(ActionletUtil.class, String.format(
                    "Can't determine if action '%s' has OnlyOnBatch.",
                    action.getName()), e);
        }
        return false;
    }

    /**
     * This method tells you if an action has been set to have a sub actionlet Push-Publish
     * @param action
     * @return
     */
    public static boolean hasPushPublishActionlet(final WorkflowAction action) {
        try {

            if (Objects.nonNull(action)) {
                final List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(action);

                if (Objects.nonNull(actionlets)) {
                    for (final WorkflowActionClass actionletClass : actionlets) {

                        if (Objects.nonNull(actionletClass)) {
                            final WorkFlowActionlet workFlowActionlet = APILocator.getWorkflowAPI().findActionlet(actionletClass.getClazz());

                            if (Objects.nonNull(workFlowActionlet)) {
                                final Actionlet actionlet = AnnotationUtils.
                                        getBeanAnnotation(workFlowActionlet.getClass(), Actionlet.class);

                                if (Objects.nonNull(actionlet) && actionlet.pushPublish()) {
                                    return true;
                                }
                            }
                        }
                    }
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

    /**
     * Returns true if has a {@link com.dotmarketing.portlets.workflows.actionlet.MoveContentActionlet} and the path is empty
     * @param action {@link WorkflowAction}
     * @return boolean
     */
    public static boolean isMoveableActionlet(final WorkflowAction action) {

        try {
            final List<WorkflowActionClass> actionClasses = APILocator.getWorkflowAPI().findActionClasses(action);

            final Optional<WorkflowActionClass> actionClassesOpt =
                    null != actionClasses?  actionClasses.stream()
                            .filter(wfClass -> wfClass.getClazz().equals(MoveContentActionlet.class.getName()))
                            .findFirst(): Optional.empty();

            if (actionClassesOpt.isPresent()) {

                final Map<String, WorkflowActionClassParameter> workflowActionClassParameterMap =
                        APILocator.getWorkflowAPI().findParamsForActionClass(actionClassesOpt.get());

                return UtilMethods.isSet(workflowActionClassParameterMap) &&
                        !UtilMethods.isSet(workflowActionClassParameterMap.get(MoveContentActionlet.PATH_KEY).getValue());
            }
        } catch (DotDataException e) {
            Logger.error(ActionletUtil.class, String.format(
                    "Can't determine if action '%s' has MoveContentActionlet.",
                    action.getName()), e);
        }
        return false;
    }

    /**
     * Returns true if the class is annotated as a batch only actionlet
     * @param clazz String (expects a WorkflowActionlet) but it is not mandatory
     * @return true if the class is batch only marked
     */
    public static boolean isOnlyBatch(final String clazz) {

        final WorkFlowActionlet workFlowActionlet = Try.of(()->APILocator.getWorkflowAPI().findActionlet(clazz)).getOrNull();

        return Objects.nonNull(workFlowActionlet)?isOnlyBatch(workFlowActionlet.getClass()):false;
    }

    /**
     * Returns true if the class is annotated as a batch only actionlet
     * @param clazz Class (expects a WorkflowActionlet) but it is not mandatory
     * @return true if the class is batch only marked
     */
    public static boolean isOnlyBatch(final Class clazz) {

        final Actionlet actionlet = AnnotationUtils.
                getBeanAnnotation(clazz, Actionlet.class);
        return Objects.nonNull(actionlet)?actionlet.onlyBatch():false;
    }
}
