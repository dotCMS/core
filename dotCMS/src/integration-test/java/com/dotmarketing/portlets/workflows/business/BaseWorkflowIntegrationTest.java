package com.dotmarketing.portlets.workflows.business;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

import java.util.Date;
import java.util.List;

public class BaseWorkflowIntegrationTest extends IntegrationTestBase {


    /**
     * Creates a new scheme, with a new step, with a new action and action let.
     * the new action will be associated to the step
     * @param schemeName
     * @param stepName
     * @param actionName
     * @param actionClass
     * @return CreateSchemeStepActionResult: scheme, step, action
     * @throws AlreadyExistException
     * @throws DotDataException
     */
    protected static CreateSchemeStepActionResult createSchemeStepActionActionlet (final String schemeName,
                                                                            final String stepName,
                                                                            final String actionName,
                                                                            final Class  actionClass) throws AlreadyExistException, DotDataException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final WorkflowScheme scheme = new WorkflowScheme();
        scheme.setName(schemeName);
        scheme.setArchived(false);
        scheme.setDescription("test");
        scheme.setMandatory(false);
        scheme.setModDate(new Date());
        scheme.setCreationDate(new Date());
        scheme.setDefaultScheme(false);
        workflowAPI.saveScheme(scheme);

        final WorkflowStep step = createNewWorkflowStep(stepName, scheme.getId());
        final CreateSchemeStepActionResult result =
                createActionActionlet(scheme.getId(), step.getId(), actionName, actionClass);

        return new CreateSchemeStepActionResult(scheme, step, result.action, result.actionClass);
    }


    protected static WorkflowStep createNewWorkflowStep (final String name, final String schemeId) throws AlreadyExistException, DotDataException {

        final WorkflowAPI  workflowAPI = APILocator.getWorkflowAPI();
        final WorkflowStep step = new WorkflowStep();
        step.setName(name);
        step.setMyOrder(0);
        step.setResolved(false);
        step.setCreationDate(new Date());
        step.setSchemeId(schemeId);
        workflowAPI.saveStep(step);
        return step;
    }

    protected static CreateSchemeStepActionResult createActionActionlet (final String schemeId,
                                                                         final String stepId,
                                                                         final String actionName,
                                                                         final Class  actionClass) throws AlreadyExistException, DotDataException {

        return  createActionActionlet (schemeId,
                                            stepId,
                                            actionName,
                                            actionClass,
                                            WorkflowAction.CURRENT_STEP);
    }

    /**
     * Create an action and actionlet associated to scheme and step
     * @param schemeId
     * @param stepId
     * @param actionName
     * @param actionClass
     * @return CreateSchemeStepActionResult action and actionlet
     * @throws AlreadyExistException
     * @throws DotDataException
     */
    protected static CreateSchemeStepActionResult createActionActionlet (final String schemeId,
                                                                            final String stepId,
                                                                            final String actionName,
                                                                            final Class  actionClass,
                                                                            final String nextStep) throws AlreadyExistException, DotDataException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        final WorkflowAction action = new WorkflowAction();
        action.setName(actionName);
        action.setSchemeId(schemeId);
        action.setNextStep(nextStep);
        action.setShowOn(WorkflowAPI.DEFAULT_SHOW_ON);
        action.setRequiresCheckout(true);
        action.setCondition("");
        action.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        workflowAPI.saveAction(action, null);

        workflowAPI.saveAction(action.getId(),
                stepId, APILocator.systemUser());

        final WorkflowActionClass workflowActionClass = new WorkflowActionClass();
        workflowActionClass.setActionId(action.getId());
        workflowActionClass.setClazz(actionClass.getName());
        try {
            workflowActionClass.setName(WorkFlowActionlet.class.cast(actionClass.newInstance()).getName());
            workflowActionClass.setOrder(0);
            workflowAPI.saveActionClass(workflowActionClass);
        } catch (Exception e) {
            Logger.error(BaseWorkflowIntegrationTest.class, e.getMessage());
            Logger.debug(BaseWorkflowIntegrationTest.class, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return new CreateSchemeStepActionResult(null, null, action, workflowActionClass);
    }

    /**
     * Deletes the scheme, the actions and steps
     * @param scheme
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws AlreadyExistException
     */
    protected static void cleanScheme (final WorkflowScheme scheme) throws DotSecurityException, DotDataException, AlreadyExistException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final List<WorkflowAction> schemeActions = workflowAPI.findActions(scheme, APILocator.systemUser());

        for (final WorkflowAction action: schemeActions) {

            workflowAPI.deleteAction(action);
        }

        final List<WorkflowStep> workflowSteps = workflowAPI.findSteps(scheme);
        for (final WorkflowStep step: workflowSteps) {

            workflowAPI.deleteStep(step);
        }

        scheme.setArchived(true);
        workflowAPI.saveScheme(scheme);
        workflowAPI.deleteScheme(scheme);
    }

    protected long getEnglishLanguageId() {

        return APILocator.getLanguageAPI().getLanguage("en", "US").getId();
    }

    protected long getSpanishLanguageId() {

        return APILocator.getLanguageAPI().getLanguage("es", "ES").getId();
    }

    public static final class CreateSchemeStepActionResult {

        private final WorkflowScheme scheme;
        private final WorkflowStep   step;
        private final WorkflowAction action;
        private final WorkflowActionClass    actionClass;


        public CreateSchemeStepActionResult(
                final WorkflowScheme scheme, final WorkflowStep step, final WorkflowAction action, final WorkflowActionClass actionClass) {
            this.scheme = scheme;
            this.step = step;
            this.action = action;
            this.actionClass = actionClass;
        }

        public WorkflowScheme getScheme() {
            return scheme;
        }

        public WorkflowStep getStep() {
            return step;
        }

        public WorkflowAction getAction() {
            return action;
        }

        public WorkflowActionClass getActionClass() {
            return actionClass;
        }
    }

}
