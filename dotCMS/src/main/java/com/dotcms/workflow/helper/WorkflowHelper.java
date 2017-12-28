package com.dotcms.workflow.helper;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowActionStepBean;
import com.dotcms.workflow.form.WorkflowReorderBean;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.*;

import static com.dotmarketing.db.HibernateUtil.addSyncCommitListener;

/**
 * Helper for Workflow Actions
 * @author jsanca
 */
public class WorkflowHelper {

    private final WorkflowAPI   workflowAPI;
    private final RoleAPI       roleAPI;
    private final ContentletAPI contentletAPI;

    private static class SingletonHolder {
        private static final WorkflowHelper INSTANCE = new WorkflowHelper();
    }

    public static WorkflowHelper getInstance() {
        return WorkflowHelper.SingletonHolder.INSTANCE;
    }

    private WorkflowHelper() {
        this( APILocator.getWorkflowAPI(),
                APILocator.getRoleAPI(),
                APILocator.getContentletAPI());
    }


    @VisibleForTesting
    protected WorkflowHelper(final WorkflowAPI workflowAPI,
                             final RoleAPI     roleAPI,
                             final ContentletAPI contentletAPI) {

        this.workflowAPI   = workflowAPI;
        this.roleAPI       = roleAPI;
        this.contentletAPI = contentletAPI;
    }

    /**
     * Finds the available actions for an inode and user.
     * @param inode String
     * @param user  User
     * @return List of WorkflowAction
     */
    @CloseDBIfOpened
    public List<WorkflowAction> findAvailableActions(final String inode, final User user) {

        Contentlet contentlet        = null;
        List<WorkflowAction> actions = Collections.emptyList();

        try {

            Logger.debug(this, "Asking for the available actions for the inode: " + inode);
            contentlet =
                    this.contentletAPI.find(inode, user, true);

            if (null != contentlet) {

                actions = this.workflowAPI.findAvailableActions(contentlet, user);
            }
        } catch (DotDataException  | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return actions;
    } // findAvailableActions.

    /**
     * Reorder the action associated to the scheme.
     * @param workflowReorderActionStepForm WorkflowReorderActionStepForm
     */
    @WrapInTransaction
    public void reorderAction(final WorkflowReorderBean workflowReorderActionStepForm,
                              final User user)  {

        WorkflowAction action = null;
        WorkflowStep step     = null;

        try {

            Logger.debug(this, "Looking for the actionId: "
                    + workflowReorderActionStepForm.getActionId());
            action =
                    this.workflowAPI.findAction(workflowReorderActionStepForm.getActionId(), user);

            Logger.debug(this, "Looking for the stepId: "
                    + workflowReorderActionStepForm.getStepId());
            step =
                    this.workflowAPI.findStep(workflowReorderActionStepForm.getStepId());

            if (null == action) {
                throw new DoesNotExistException("Workflow-does-not-exists-action");
            }

            if (null == step) {
                throw new DoesNotExistException("Workflow-does-not-exists-step");
            }

            Logger.debug(this, "Reordering the action: " + action.getId()
                    + " for the stepId: " + step.getId() + ", order: " +
                    workflowReorderActionStepForm.getOrder()
            );

            this.workflowAPI.reorderAction(action, step, user,
                    workflowReorderActionStepForm.getOrder());
        } catch (DotDataException | DotSecurityException | AlreadyExistException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }
    }  // reorderAction.

    /**
     * Deletes the step
     * @param stepId String
     */
    @WrapInTransaction
    public void deleteStep(final String stepId) {

        WorkflowStep workflowStep = null;

        try {

            Logger.debug(this, "Looking for the stepId: " + stepId);
            workflowStep = this.workflowAPI.findStep(stepId);
        } catch (Exception e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
        }

        if (null != workflowStep) {

            try {

                Logger.debug(this, "deleting step: " + stepId);
                this.workflowAPI.deleteStep(workflowStep);
            } catch (DotDataException e) {
                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {

            throw new DoesNotExistException("Workflow-does-not-exists-step");
        }
    } // deleteStep.

    /**
     * Deletes the action which is part of the step, but the action still being part of the scheme.
     * @param actionId String action id
     * @param user     User   the user that makes the request
     * @return WorkflowStep
     */
    @WrapInTransaction
    public void deleteAction(final String actionId,
                             final User user) {

        WorkflowAction action = null;

        try {

            Logger.debug(this, "Looking for the action: " + actionId);
            action = this.workflowAPI.findAction
                    (actionId, user);
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
        }

        if (null != action) {

            try {

                Logger.debug(this, "Deleting the action: " + actionId);
                this.workflowAPI.deleteAction(action);
            } catch (DotDataException | AlreadyExistException e) {

                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {

            throw new DoesNotExistException("Workflow-does-not-exists-action");
        }
    } // deleteAction.

    /**
     * Deletes the action which is part of the step, but the action still being part of the scheme.
     * @param actionId String action id
     * @param stepId   String step   id
     * @param user     User   the user that makes the request
     * @return WorkflowStep
     */
    @WrapInTransaction
    public WorkflowStep deleteAction(final String actionId,
                                     final String stepId,
                                     final User user) {

        WorkflowAction action = null;
        WorkflowStep step     = null;

        try {

            Logger.debug(this, "Looking for the actionId: " + actionId);
            action =
                    this.workflowAPI.findAction(actionId, user);

            Logger.debug(this, "Looking for the stepId: " + stepId);
            step =
                    this.workflowAPI.findStep(stepId);

            if (null == action) {
                throw new DoesNotExistException("Workflow-does-not-exists-action");
            }

            if (null == step) {
                throw new DoesNotExistException("Workflow-does-not-exists-step");
            }

            Logger.debug(this, "Deleting the action: " + actionId
                    + " for the stepId: " + stepId);

            this.workflowAPI.deleteAction(action, step);
        } catch (DotDataException | DotSecurityException | AlreadyExistException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return step;
    } // deleteAction.

    /**
     * Find Schemes by content type id
     * @param contentTypeId String
     * @param user          User   the user that makes the request
     * @return List
     */
    public List<WorkflowScheme> findSchemesByContentType(final String contentTypeId,
                                                         final User   user) {

        final ContentTypeAPI contentTypeAPI =
                APILocator.getContentTypeAPI(user);
        List<WorkflowScheme> schemes = Collections.emptyList();
        try {

            Logger.debug(this, "Getting the schemes by content type: " + contentTypeId);

            schemes = this.workflowAPI.findSchemesForContentType
                    (contentTypeAPI.find(contentTypeId));
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return schemes;
    } // findSchemesByContentType.

    /**
     * Finds the non-archived schemes
     * @return List
     */
    public List<WorkflowScheme> findSchemes() {

        List<WorkflowScheme> schemes = null;

        try {

            Logger.debug(this, "Getting all non-archived schemes");
            schemes =
                    this.workflowAPI.findSchemes(false);
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return schemes;
    } // findSchemes.

    /**
     * Finds the action associated to the stepId
     * @param stepId String
     * @param user   User
     * @return List of WorkflowAction
     */
    public List<WorkflowAction> findActions(final String stepId, final User user) {

        WorkflowStep workflowStep    = null;
        List<WorkflowAction> actions = null;

        try {

            Logger.debug(this, "Looking for the stepId: " + stepId);
            workflowStep = this.workflowAPI.findStep(stepId);
        } catch (Exception e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
        }

        if (null != workflowStep) {

            try {

                Logger.debug(this, "Looking for the actions associated to the step: " + stepId);
                actions = this.workflowAPI.findActions(workflowStep, user);
            } catch (DotDataException  | DotSecurityException e) {

                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {
            throw new DoesNotExistException("Workflow-does-not-exists-step");
        }

        return (null == actions)? Collections.emptyList(): actions;
    } // findActions.

    /**
     * Finds the steps by schemeId
     * @param schemeId String
     * @return List of WorkflowStep
     */
    public List<WorkflowStep> findSteps(final String schemeId) {


        List<WorkflowStep> workflowSteps  = null;
        WorkflowScheme     workflowScheme = null;

        try {

            Logger.debug(this, "Looking for the schemeId: " + schemeId);
            workflowScheme = this.workflowAPI.findScheme(schemeId);
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
        }

        if (null != workflowScheme) {

            try {

                workflowSteps = this.workflowAPI.findSteps(workflowScheme);
            } catch (DotDataException e) {

                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {

            throw new DoesNotExistException("Workflow-does-not-exists-scheme");
        }

        return workflowSteps;
    } // findSteps.

    /**
     * Returns if the action associated to the actionId parameter is new and clone the retrieved action into a new one.
     * @param actionId String
     * @return IsNewAndCloneItResult
     */
    protected IsNewAndCloneItResult isNewAndCloneIt (final String actionId) {


        final WorkflowAction newAction = new WorkflowAction();
        boolean isNew = true;

        try {

            final WorkflowAction origAction = this.workflowAPI.findAction
                    (actionId, APILocator.getUserAPI().getSystemUser());
            BeanUtils.copyProperties(newAction, origAction);
            isNew = !(origAction !=null || !origAction.isNew());
        } catch (Exception e) {

            Logger.debug(this.getClass(), "Unable to find action" + actionId);
        }

        return new IsNewAndCloneItResult(isNew, newAction);
    } // isNewAndCloneIt.

    /**
     * Resolve the role based on the id
     * @param id String
     * @return Role
     * @throws DotDataException
     */
    protected Role resolveRole(final String id) throws DotDataException {

        Role role = null;
        final String newid = id.substring
                (id.indexOf("-") + 1, id.length());

        if(id.startsWith("user-")) {

            role = this.roleAPI.loadRoleByKey(newid);
        } else if(id.startsWith("role-")) {

            role = this.roleAPI.loadRoleById (newid);
        } else {

            role = this.roleAPI.loadRoleById (id);
        }

        return role;
    } // resolveRole.

    /**
     * Finds the actions by scheme
     * @param schemeId String
     * @param user     User
     * @return List of WorkflowAction
     */
    public List<WorkflowAction> findActionsByScheme(final String schemeId,
                                                    final User user) {

        List<WorkflowAction> actions = null;
        final WorkflowScheme workflowSchemeProxy = new WorkflowScheme();

        try {

            workflowSchemeProxy.setId(schemeId);
            actions =
                    this.workflowAPI.findActions(workflowSchemeProxy, (null == user)?
                            APILocator.getUserAPI().getSystemUser(): user);
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this.getClass(), e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return actions;
    } // findActionsByScheme.

    /**
     * Save a WorkflowActionForm returning the WorkflowAction created.
     * A WorkflowActionForm can send a stepId in that case the Action will be associated to the Step in the same transaction.
     * @param workflowActionForm WorkflowActionForm
     * @return WorkflowAction (workflow action created)
     */
    @WrapInTransaction
    public WorkflowAction save (final WorkflowActionForm workflowActionForm, final User user) {

        String actionNextAssign     = workflowActionForm.getActionNextAssign();
        if (actionNextAssign != null && actionNextAssign.startsWith("role-")) {
            actionNextAssign  = actionNextAssign.replaceAll("role-", StringPool.BLANK);
        }

        final WorkflowHelper.IsNewAndCloneItResult isNewAndCloneItResult =
                this.isNewAndCloneIt(workflowActionForm.getActionId());
        final WorkflowAction newAction = isNewAndCloneItResult.getAction();
        final boolean isNew            = isNewAndCloneItResult.isNew();

        newAction.setName       (workflowActionForm.getActionName());
        newAction.setAssignable (workflowActionForm.isActionAssignable());
        newAction.setCommentable(workflowActionForm.isActionCommentable());
        newAction.setIcon       (workflowActionForm.getActionIcon());
        newAction.setNextStep   (workflowActionForm.getActionNextStep());
        newAction.setSchemeId   (workflowActionForm.getSchemeId());
        newAction.setCondition  (workflowActionForm.getActionCondition());
        newAction.setRequiresCheckout(workflowActionForm.isRequiresCheckout());
        newAction.setShowOn((null != workflowActionForm.getShowOn() && !workflowActionForm.getShowOn().isEmpty())?
                workflowActionForm.getShowOn():WorkflowAPI.DEFAULT_SHOW_ON);
        newAction.setRoleHierarchyForAssign(workflowActionForm.isRoleHierarchyForAssign());

        try {

            newAction.setNextAssign(this.resolveRole(actionNextAssign).getId());
            if(!UtilMethods.isSet(newAction.getNextAssign())){
                newAction.setNextAssign(null);
            }

            final List<Permission> permissions = new ArrayList<>();

            for (final String permissionName : workflowActionForm.getWhoCanUse()) {

                if (UtilMethods.isSet(permissionName)) {

                    this.processPermission(newAction, permissions, permissionName);
                }
            }

            Logger.debug(this, "Saving new Action: " + newAction.getName());
            this.workflowAPI.saveAction(newAction, permissions);

            if(isNew) {

                // if should be associated to a stepId right now
                if (UtilMethods.isSet(workflowActionForm.getStepId())) {

                    Logger.debug(this, "The Action: " + newAction.getId() +
                            ", is going to be associated to the step: " + workflowActionForm.getStepId());
                    this.workflowAPI.saveAction(newAction.getId(),
                            workflowActionForm.getStepId(), user);
                }

                Logger.debug(this, "Saving new WorkflowActionClass, for the Workflow action: "
                        + newAction.getId());

                addSyncCommitListener(() -> {
                    WorkflowActionClass workflowActionClass = new WorkflowActionClass();
                    workflowActionClass.setActionId(newAction.getId());
                    workflowActionClass.setClazz(NotifyAssigneeActionlet.class.getName());
                    try {
                        workflowActionClass.setName(NotifyAssigneeActionlet.class.newInstance().getName());
                        workflowActionClass.setOrder(0);
                        this.workflowAPI.saveActionClass(workflowActionClass);
                    } catch (Exception e) {
                        Logger.error(this.getClass(), e.getMessage());
                        Logger.debug(this, e.getMessage(), e);
                        throw new DotWorkflowException(e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return newAction;
    } // save.

    @WrapInTransaction
    public void saveActionToStep(final WorkflowActionStepBean workflowActionStepForm, final User user) {

        this.workflowAPI.saveAction(workflowActionStepForm.getActionId(),
                workflowActionStepForm.getStepId(), user);
    } // addActionToStep.



    private void processPermission(final WorkflowAction newAction,
                                   final List<Permission> permissions,
                                   final String permissionName) throws DotDataException {

        final Role role = this.resolveRole(permissionName);
        final Permission permission =
                new Permission(newAction.getId(), role.getId(), PermissionAPI.PERMISSION_USE);

        boolean exists = false;
        for (final Permission permissionItem : permissions) {
            exists = exists || permissionItem.getRoleId().equals(permission.getRoleId());
        }

        if (!exists) {
            permissions.add(permission);
        }
    } // processPermission.

    public class IsNewAndCloneItResult {

        final boolean        isNew;
        final WorkflowAction action;

        public IsNewAndCloneItResult(final boolean isNew,
                                     final WorkflowAction action) {
            this.isNew = isNew;
            this.action = action;
        }

        public boolean isNew() {
            return isNew;
        }

        public WorkflowAction getAction() {
            return action;
        }
    } // IsNewAndCloneItResult.


} // E:O:F:WorkflowHelper.