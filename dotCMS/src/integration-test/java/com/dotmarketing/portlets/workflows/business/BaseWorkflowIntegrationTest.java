package com.dotmarketing.portlets.workflows.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.RoleDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;

import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.dotmarketing.business.Role.ADMINISTRATOR;

/**
 * Utility class that provides useful methods to create and modify Workflows in dotCMS.
 *
 * @author Jonathan Sanchez
 * @version 4.3.0
 */
public class BaseWorkflowIntegrationTest extends IntegrationTestBase {

    protected static Role roleAdmin () {

        //Creating a test role
        Role adminRole = null;
        try {

            adminRole = APILocator.getRoleAPI().loadRoleByKey(ADMINISTRATOR);
            if (adminRole == null) {
                adminRole = new RoleDataGen().key(ADMINISTRATOR).nextPersisted();
            }
        } catch (DotDataException e) {
            e.printStackTrace();
        }

        return adminRole;
    }

    /**
     * Creates a new scheme, with a new step, with a new action and action let.
     * the new action will be associated to the step
     *
     * @param schemeName
     * @param stepName
     * @param actionName
     * @param actionClass
     *
     * @return CreateSchemeStepActionResult: scheme, step, action
     *
     * @throws AlreadyExistException
     * @throws DotDataException
     */
    protected static CreateSchemeStepActionResult createSchemeStepActionActionlet(
            final String schemeName,
            final String stepName,
            final String actionName,
            final Class actionClass) throws AlreadyExistException, DotDataException, DotSecurityException {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final WorkflowScheme scheme = new WorkflowScheme();
        scheme.setName(schemeName);
        scheme.setArchived(false);
        scheme.setDescription("Integration test " + schemeName);
        scheme.setModDate(new Date());
        scheme.setCreationDate(new Date());
        scheme.setDefaultScheme(false);
        workflowAPI.saveScheme(scheme, APILocator.systemUser());

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
        workflowAPI.saveStep(step, APILocator.systemUser());
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
     *
     * @param schemeId
     * @param stepId
     * @param actionName
     * @param actionClass
     *
     * @return CreateSchemeStepActionResult action and actionlet
     *
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
        action.setRequiresCheckout(false);
        action.setCondition("");
        action.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        workflowAPI.saveAction(action, null, APILocator.systemUser());

        workflowAPI.saveAction(action.getId(),
                stepId, APILocator.systemUser());

        final WorkflowActionClass workflowActionClass = new WorkflowActionClass();
        workflowActionClass.setActionId(action.getId());
        workflowActionClass.setClazz(actionClass.getName());
        try {
            workflowActionClass
                    .setName(WorkFlowActionlet.class.cast(actionClass.newInstance()).getName());
            workflowActionClass.setOrder(0);
            workflowAPI.saveActionClass(workflowActionClass, APILocator.systemUser());
        } catch (Exception e) {
            Logger.error(BaseWorkflowIntegrationTest.class, e.getMessage());
            Logger.debug(BaseWorkflowIntegrationTest.class, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return new CreateSchemeStepActionResult(null, null, action, workflowActionClass);
    }

    protected static WorkflowAction createWorkflowAction (final String schemeId,
                                                          final String actionName) throws AlreadyExistException, DotDataException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final WorkflowAction action = new WorkflowAction();
        action.setName(actionName);
        action.setSchemeId(schemeId);
        action.setNextStep(WorkflowAction.CURRENT_STEP);
        action.setShowOn(WorkflowAPI.DEFAULT_SHOW_ON);
        action.setRequiresCheckout(false);
        action.setCondition("");
        action.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        workflowAPI.saveAction(action, null, APILocator.systemUser());
        return action;
    }

    /**
     * Deletes the scheme, the actions and steps
     *
     * @param scheme
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws AlreadyExistException
     */
    protected static void cleanScheme(final WorkflowScheme scheme)
            throws DotSecurityException, DotDataException, AlreadyExistException {

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final List<WorkflowAction> schemeActions = workflowAPI
                .findActions(scheme, APILocator.systemUser());

        for (final WorkflowAction action : schemeActions) {

            workflowAPI.deleteAction(action, APILocator.systemUser());
        }

        final List<WorkflowStep> workflowSteps = workflowAPI.findSteps(scheme);
        for (final WorkflowStep step : workflowSteps) {

            try {
                workflowAPI.deleteStep(step, APILocator.systemUser()).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new DotDataException(e);
            }
        }

        scheme.setArchived(true);
        workflowAPI.saveScheme(scheme, APILocator.systemUser());

        try {
            workflowAPI.deleteScheme(scheme, APILocator.systemUser()).get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(BaseWorkflowIntegrationTest.class, e.getMessage(), e);
        }

    }

    protected long getEnglishLanguageId() {

        return APILocator.getLanguageAPI().getLanguage("en", "US").getId();
    }

    protected long getSpanishLanguageId() {

        return APILocator.getLanguageAPI().getLanguage("es", "ES").getId();
    }

    /**
     * Adds the ID of one or more roles or users who can use the specified workflow action. In the
     * UI, this method allows you to set the values for the 'Who Can Use' field.
     *
     * @param action  The ID of the workflow action.
     * @param roleIds The list of IDs of roles or users as they are in the 'cms_role' table.
     *
     * @throws AlreadyExistException
     * @throws DotDataException
     */
    protected static void addWhoCanUseToAction(final WorkflowAction action,
            final List<String> roleIds)
            throws AlreadyExistException, DotDataException {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final List<Permission> permissions = new ArrayList<>();
        for (final String roleId : roleIds) {
            Permission permission = new Permission(action.getId(), roleId,
                    PermissionAPI.PERMISSION_USE);
            permissions.add(permission);
        }
        workflowAPI.saveAction(action, permissions, APILocator.systemUser());
    }

    /**
     * Adds a specific actionlet (sub-action) to a workflow action in a given order.
     *
     * @param actionId The workflow action ID.
     * @param actionletClass The actionlet class.
     * @param order The zero-based order of the actionlet.
     * @throws AlreadyExistException
     * @throws DotDataException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected static void addActionletToAction(final String actionId, final Class actionletClass,
            final int order)
            throws AlreadyExistException, DotDataException, IllegalAccessException, InstantiationException {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final WorkflowActionClass wac = new WorkflowActionClass();
        wac.setActionId(actionId);
        wac.setClazz(actionletClass.getName());
        wac.setName(WorkFlowActionlet.class.cast(actionletClass.newInstance()).getName());
        wac.setOrder(order);
        workflowAPI.saveActionClass(wac, APILocator.systemUser());
    }

    /**
     * Returns the actionlets that are associated to the specified workflow action.
     *
     * @param workflowAction The workflow action.
     * @return The list of actionlet classes that will run when the specified action is executed.
     * @throws DotDataException
     */
    protected static List<WorkflowActionClass> getActionletsFromAction(final WorkflowAction workflowAction)
            throws DotDataException {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        return workflowAPI.findActionClasses(workflowAction);
    }

    /**
     * Some actionlets have fields where users can set parameters for them. This method allows you
     * to set values for the parameters in a given actionlet.
     *
     * @param workflowActionletClass The actionlet class that will have the new parameters.
     * @param paramValues
     * @throws DotDataException
     */
    protected static void addParameterValuesToActionlet(final WorkflowActionClass workflowActionletClass,
            final List<String> paramValues) throws DotDataException {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final WorkFlowActionlet actionlet = workflowAPI.findActionlet(workflowActionletClass.getClazz());
        final List<WorkflowActionletParameter> actionletParams = actionlet.getParameters();
        final List<WorkflowActionClassParameter> newParameters = new ArrayList<>();
        for (int i = 0; i < paramValues.size(); i++) {
            final WorkflowActionClassParameter actionletParam = new WorkflowActionClassParameter();
            actionletParam.setActionClassId(workflowActionletClass.getId());
            actionletParam.setKey(actionletParams.get(i).getKey());
            actionletParam.setValue(paramValues.get(i));
            newParameters.add(actionletParam);
        }

        workflowAPI.saveWorkflowActionClassParameters(newParameters, APILocator.systemUser());
    }

    /**
     * This class represents a simple Workflow Scheme with its minimum parts:
     * <ul>
     * <li>A Workflow Scheme.</li>
     * <li>One Workflow Step.</li>
     * <li>One Workflow Action.</li>
     * <li>One Workflow Sub-Action.</li>
     * </ul>
     */
    public static final class CreateSchemeStepActionResult {

        private final WorkflowScheme scheme;
        private final WorkflowStep step;
        private final WorkflowAction action;
        private final WorkflowActionClass actionClass;

        public CreateSchemeStepActionResult(
                final WorkflowScheme scheme, final WorkflowStep step, final WorkflowAction action,
                final WorkflowActionClass actionClass) {
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

    static ContentType insertContentType(final User user, final String contentTypeName,
            BaseContentType baseType)
            throws DotDataException, DotSecurityException {

        ContentTypeBuilder builder = ContentTypeBuilder.builder(baseType.immutableClass())
                .description("description")
                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .name(contentTypeName).owner("owner")
                .variable("velocityVarName" + contentTypeName);

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        final ContentType type = builder.build();
        return contentTypeAPI.save(type);
    }


    public static ContentType createContentTypeAndAssignPermissions(final String contentTypeName,
            final BaseContentType baseContentType, final int permission, final String roleId)
            throws DotDataException, DotSecurityException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        final ContentType contentType = insertContentType(APILocator.systemUser(), contentTypeName, baseContentType);

        Permission p = new Permission(contentType.getPermissionId(), roleId, permission, true);
        permissionAPI.save(p, contentType, APILocator.systemUser(), true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                roleId, permission, true);
        permissionAPI.save(p, contentType, APILocator.systemUser(), true);

        return contentType;
    }

}
