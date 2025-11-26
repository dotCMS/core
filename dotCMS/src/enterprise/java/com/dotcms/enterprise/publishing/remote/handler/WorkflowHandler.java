/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.enterprise.publishing.remote.bundler.WorkflowBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.wrapper.WorkflowWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotcms.workflow.helper.SystemActionMappingsHandlerMerger;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Workflow-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link WorkflowScheme} data
 * files.
 * <p>
 * Custom Workflows allow you to specify how content moves through your system, from initial creation, through to
 * publishing, and even to archival, deletion or other final disposition.
 *
 * @author Daniel Silva
 * @since Aug 28, 2013
 */
public class WorkflowHandler implements IHandler {

    private final PublisherConfig config;
    private final UserAPI userAPI;
    private User systemUser;

    public WorkflowHandler(final PublisherConfig config) {

        this.config  = config;
        this.userAPI = APILocator.getUserAPI();

        try {
            this.systemUser = this.userAPI.getSystemUser();
        } catch (DotDataException e) {

            Logger.fatal(ContainerBundler.class, ExceptionUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @WrapInTransaction
    @Override
    public void handle(final File bundleFolder) throws Exception {

        if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

            throw new RuntimeException("need an enterprise pro license to run this");
        }

        final Collection<File> workflows = FileUtil
                .listFilesRecursively(bundleFolder, new WorkflowBundler().getFileFilter());

        handleWorkflows(workflows);
    }

    private void handleWorkflows(final Collection<File> workflows)
            throws DotPublishingException {

        if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

            throw new RuntimeException("need an enterprise pro license to run this");
        }
        File workingOn = null;
        try {

            final XStream xstream = XStreamHandler.newXStreamInstance();
            final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
            //Handle folders
            for (final File workflowFile : workflows) {
                workingOn = workflowFile;
                if (workflowFile.isDirectory()) {
                    continue;
                }

                this.handleWorkflow(xstream, workflowAPI, workflowFile);
            }

        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Workflow in '%s': %s",
                    workingOn, ExceptionUtil.getErrorMessage(e));
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
        }
    }

    private WorkflowWrapper readFile (final XStream xstream, final File workflowFile) throws IOException {

        try (final InputStream input = Files.newInputStream(workflowFile.toPath())) {
            return (WorkflowWrapper) xstream.fromXML(input);
        }
    }

    private WorkflowScheme findScheme (final WorkflowAPI workflowAPI, final String workflowId)
            throws DotSecurityException {

        WorkflowScheme scheme = null;
        try {
            //Trying to find the Scheme in this node
            scheme = workflowAPI.findScheme(workflowId);
        } catch (DoesNotExistException | DotDataException e) {
            Logger.debug(getClass(), "localScheme: " + workflowId + " not found, moving on");
        }
        return scheme;
    }

    private void handleWorkflow(final XStream xstream,
                                final WorkflowAPI workflowAPI,
                                final File workflowFile)
            throws IOException, DotSecurityException, DotDataException, AlreadyExistException,
            InterruptedException, java.util.concurrent.ExecutionException {

        final List<String[]> stepsWithTimedActions = new ArrayList<>();
        final WorkflowWrapper workflowWrapper      = this.readFile(xstream, workflowFile);
        final WorkflowScheme remoteScheme          = workflowWrapper.getScheme();
        final WorkflowScheme localScheme           = this.findScheme(workflowAPI, remoteScheme.getId());
        final boolean localExists = localScheme != null && UtilMethods.isSet(localScheme.getId());

        if (workflowWrapper.getOperation().equals(Operation.UNPUBLISH)) {// delete operation

            if (localExists) {

                this.deleteScheme(workflowAPI, localScheme);
            }
        } else { // create/update the workflow scheme

            this.saveOrUpdateSchemeData(workflowAPI, stepsWithTimedActions,
                    workflowWrapper, remoteScheme, localScheme, localExists);

            PushPublishLogger
                    .log(getClass(), PushPublishHandler.WORKFLOW, PushPublishAction.PUBLISH,
                            remoteScheme.getId(), null, remoteScheme.getName(),
                            config.getId());
        }
    }

    private void saveOrUpdateSchemeData(final WorkflowAPI workflowAPI,
                                        final List<String[]> stepsWithTimedActions,
                                        final WorkflowWrapper workflowWrapper,
                                        final WorkflowScheme remoteScheme,
                                        final WorkflowScheme localScheme,
                                        final boolean localExists) throws DotDataException, DotSecurityException,
            AlreadyExistException, InterruptedException, java.util.concurrent.ExecutionException {

        this.saveScheme(workflowAPI, remoteScheme, localScheme, localExists);

        // Saving steps
        final Set<String> stepIds        = saveSteps(workflowAPI, stepsWithTimedActions, workflowWrapper);

        // Saving actions with perms
        final Set<String> actionIds      = saveActions(workflowAPI, workflowWrapper);

        //Save the associated action Steps
        this.saveStepActions(workflowAPI, workflowWrapper);

        // save action classes
        final Set<String> actionClassesId = saveActionClasses(workflowAPI, workflowWrapper);

        // params
        final Set<String> paramsId        = saveActionClassParameters(workflowAPI, workflowWrapper);

        // delete those not mentioned in the bundle
        this.purgeStepsActionAndActionlets(workflowAPI, remoteScheme, stepIds, actionIds, actionClassesId, paramsId);

        // if any of the steps have timed actions, same thing, we are deferring this update till
        // all the actions have been inserted
        // https://github.com/dotCMS/core/issues/8368
        this.saveStepWithTimeActions(workflowAPI, stepsWithTimedActions);

        // Saves the system mappings and deletes the unused ones.
        new SystemActionMappingsHandlerMerger(workflowAPI).mergeSystemActions(
                localExists? localScheme: remoteScheme, workflowWrapper.getSystemActionMappings());
    }

    private void saveStepWithTimeActions(final WorkflowAPI workflowAPI,
                                         final List<String[]> stepsWithTimedActions)
            throws DotDataException, AlreadyExistException {

        for (final String[] stepWithTimeAction : stepsWithTimedActions) {

            final WorkflowStep step = workflowAPI.findStep(stepWithTimeAction[0]);
            step.setEscalationAction(stepWithTimeAction[1]);
            workflowAPI.saveStep(step, APILocator.systemUser());
        }
    }

    /**
     * This method removes the unused steps, unused action, actionlets etc.
     * @param workflowAPI
     * @param remoteScheme
     * @param stepIds
     * @param actionIds
     * @param actionClassesId
     * @param paramsId
     * @throws DotDataException
     * @throws InterruptedException
     * @throws java.util.concurrent.ExecutionException
     * @throws DotSecurityException
     * @throws AlreadyExistException
     */
    private void purgeStepsActionAndActionlets(final WorkflowAPI workflowAPI,
                                               final WorkflowScheme remoteScheme,
                                               final Set<String> stepIds,
                                               final Set<String> actionIds,
                                               final Set<String> actionClassesId,
                                               final Set<String> paramsId)
            throws DotDataException, InterruptedException, java.util.concurrent.ExecutionException,
            DotSecurityException, AlreadyExistException {

        final List<WorkflowStep> currentStepsList = workflowAPI.findSteps(remoteScheme);

        for (final WorkflowStep step : currentStepsList) {

            if (!stepIds.contains(step.getId())) {
                // delete step
                workflowAPI.deleteStepHardMode(step, systemUser).get();
            } else {
                for (final WorkflowAction action : workflowAPI.findActions(step, systemUser)) {

                    if (!actionIds.contains(action.getId())) {

                        workflowAPI.deleteAction(action, systemUser);
                    } else {

                        this.purgeActionlets(workflowAPI, actionClassesId, paramsId, action);
                    }
                }
            }
        }
    }

    private void purgeActionlets(final WorkflowAPI workflowAPI,
                                 final Set<String> actionClassesId,
                                 final Set<String> paramsId,
                                 final WorkflowAction action) throws DotDataException, AlreadyExistException {

        for (final WorkflowActionClass workflowActionClass : workflowAPI.findActionClasses(action)) {

            if (!actionClassesId.contains(workflowActionClass.getId())) {

                workflowAPI.deleteActionClass(workflowActionClass, systemUser);
            } else {

                for (final WorkflowActionClassParameter param : workflowAPI.findParamsForActionClass(workflowActionClass).values()) {

                    if (!paramsId.contains(param.getId())) {

                        workflowAPI.deleteWorkflowActionClassParameter(param);
                    }
                }
            }
        }
    }

    private Set<String> saveActionClassParameters(final WorkflowAPI workflowAPI,
                                                  final WorkflowWrapper workflowWrapper) throws DotDataException {

        final Set<String> paramsId = new HashSet<>();
        workflowAPI.saveWorkflowActionClassParameters(
                workflowWrapper.getActionClassParams(), systemUser);

        for (final WorkflowActionClassParameter param : workflowWrapper.getActionClassParams()) {

            paramsId.add(param.getId());
        }

        return paramsId;
    }

    private Set<String> saveActionClasses(final WorkflowAPI workflowAPI,
                                          final WorkflowWrapper workflowWrapper) throws DotDataException, AlreadyExistException {

        final List<WorkflowActionClass> actionClasses = workflowWrapper.getActionClasses();
        final Set<String> actionClassesId             = new HashSet<>();

        for (final WorkflowActionClass workflowActionClass : actionClasses) {

            workflowAPI.saveActionClass(workflowActionClass, systemUser);
            actionClassesId.add(workflowActionClass.getId());
        }

        return actionClassesId;
    }

    private void saveStepActions(final WorkflowAPI workflowAPI, final WorkflowWrapper workflowWrapper) {

        final List<Map<String, String>> stepActions = workflowWrapper.getStepActions();
        for (final Map<String, String> stepAction : stepActions) {

            workflowAPI.saveAction(stepAction.get(WorkflowBundler.ACTION_ID),
                    stepAction.get(WorkflowBundler.STEP_ID), systemUser,
                    Integer.parseInt(stepAction.get(WorkflowBundler.ACTION_ORDER)));
        }
    }

    private Set<String> saveActions(final WorkflowAPI workflowAPI,
                                    final WorkflowWrapper workflowWrapper)
            throws DotDataException, DotSecurityException, AlreadyExistException {

        final Map<WorkflowAction, List<Role>> actionRoles    = workflowWrapper.getActionRoles();
        final Map<String, String> actionNextAssignRolekeyMap = workflowWrapper
                .getActionNextAssignRolekeyMap();
        final List<WorkflowAction> actions = workflowWrapper.getActions();
        final Set<String> actionIds        = new HashSet<>();

        for (final WorkflowAction workflowAction : actions) {

            final List<Permission> permissions =
                    savePermissions(actionRoles, actionNextAssignRolekeyMap, workflowAction);

            workflowAPI.saveAction(workflowAction, permissions, APILocator.systemUser());
            actionIds.add(workflowAction.getId());
        }

        return actionIds;
    }

    private List<Permission> savePermissions(final Map<WorkflowAction, List<Role>> actionRoles,
                                             final Map<String, String> actionNextAssignRolekeyMap,
                                             final WorkflowAction workflowAction)
            throws DotDataException, DotSecurityException {

        final List<Role> roles       = actionRoles.get(workflowAction);
        final List<Permission> perms = new ArrayList<>();

        for (final Role role : roles) {

            final Role localRemote = APILocator.getRoleAPI().loadRoleById(role.getId());
            if (localRemote != null && InodeUtils.isSet(localRemote.getId())) {

                perms.add(new Permission(workflowAction.getId(), role.getId(),
                        PermissionAPI.PERMISSION_USE));
            }
        }

        Role nextAssign = APILocator.getRoleAPI()
                .loadRoleById(workflowAction.getNextAssign());

        // if nextAssign user role does not exist
        if (nextAssign == null) {
            // lets try with the rolekey
            final String nextAssignRolekey = actionNextAssignRolekeyMap
                    .get(workflowAction.getId());
            nextAssign = APILocator.getRoleAPI().loadRoleByKey(nextAssignRolekey);

            if (nextAssign != null && UtilMethods.isSet(nextAssign.getId())) {
                workflowAction.setNextAssign(nextAssign.getId());
            } else {
                saveNextAssignDefault(workflowAction);
            }
        }
        return perms;
    }

    private void saveNextAssignDefault(final WorkflowAction workflowAction)
            throws DotDataException, DotSecurityException {

        Role nextAssign;// lets see if there is a default user defined in the dotmarketing properties
        final String roleKey = Config.getStringProperty(
                "PUSH_PUBLISHING_WORKFLOW_ACTION_NEXT_ASSIGN_DEFAULT_USER");

        if (UtilMethods.isSet(roleKey)) {
            nextAssign = APILocator.getRoleAPI().loadRoleByKey(roleKey);

            if (nextAssign != null && UtilMethods.isSet(nextAssign.getId())) {

                workflowAction.setNextAssign(nextAssign.getId());
            } else {

                // if INVALID value provided in property, lets randomly pick one admin user
                Logger.error(WorkflowHandler.class,
                        "The dotmarketing-config property PUSH_PUBLISHING_WORKFLOW_ACTION_NEXT_ASSIGN_DEFAULT_USER is set to an invalid value");
                final Role cmsAdminRole     = APILocator.getRoleAPI().loadCMSAdminRole();
                final List<User> adminUsers = APILocator.getRoleAPI().findUsersForRole(cmsAdminRole);

                if (adminUsers != null && !adminUsers.isEmpty()) {

                    final Role userRole = APILocator.getRoleAPI().loadRoleByKey(adminUsers.get(0).getUserId());
                    workflowAction.setNextAssign(userRole.getId());
                }
            }
        } else {

            // if property not provided, lets randomly pick one admin user
            final Role cmsAdminRole     = APILocator.getRoleAPI().loadCMSAdminRole();
            final List<User> adminUsers = APILocator.getRoleAPI().findUsersForRole(cmsAdminRole);

            if (adminUsers != null && !adminUsers.isEmpty()) {

                final Role userRole = APILocator.getRoleAPI().loadRoleByKey(adminUsers.get(0).getUserId());
                workflowAction.setNextAssign(userRole.getId());
            }
        }
    }

    private Set<String> saveSteps(final WorkflowAPI workflowAPI,
                                  final List<String[]> stepsWithTimedActions,
                                  final WorkflowWrapper workflowWrapper) throws DotDataException, AlreadyExistException {

        final Set<String> stepIds      = new HashSet<>();
        final List<WorkflowStep> steps = workflowWrapper.getSteps();

        for (final WorkflowStep step : steps) {

            if (UtilMethods.isSet(step.getEscalationAction())) {

                String[] x = {step.getId(), step.getEscalationAction()};
                stepsWithTimedActions.add(x);
                step.setEscalationAction(null);
            }

            workflowAPI.saveStep(step, APILocator.systemUser());
            stepIds.add(step.getId());
        }

        return stepIds;
    }

    private void saveScheme(final WorkflowAPI workflowAPI,
                            final WorkflowScheme remoteScheme,
                            final WorkflowScheme localScheme,
                            final boolean localExists) throws DotDataException, DotSecurityException, AlreadyExistException {

        if (!localExists) {

            workflowAPI.saveScheme(remoteScheme, APILocator.systemUser());
        } else {

            localScheme.setName(remoteScheme.getName());
            localScheme.setDescription(remoteScheme.getDescription());
            localScheme.setArchived(remoteScheme.isArchived());
            workflowAPI.saveScheme(localScheme, APILocator.systemUser());
        }
    }

    private void deleteScheme(final WorkflowAPI workflowAPI,
                              final WorkflowScheme localScheme) throws DotDataException, DotSecurityException, AlreadyExistException {
        //First we need to archive the Scheme
        workflowAPI.archive(localScheme, APILocator.systemUser());

        //And now we can remove it
        workflowAPI.deleteScheme(localScheme, APILocator.systemUser());

        PushPublishLogger.log(getClass(), PushPublishHandler.WORKFLOW,
                PushPublishAction.UNPUBLISH,
                localScheme.getId(), null, localScheme.getName(), config.getId());
    }

}
