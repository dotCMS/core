package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.enterprise.publishing.remote.bundler.WorkflowBundler;
import com.dotcms.publisher.pusher.wrapper.WorkflowWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
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
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorkflowHandler implements IHandler {

    private PublisherConfig config;
    private User systemUser;
    private UserAPI uAPI;

    public WorkflowHandler(PublisherConfig config) {
        this.config = config;
        uAPI = APILocator.getUserAPI();

        try {
            systemUser = uAPI.getSystemUser();
        } catch (DotDataException e) {
            Logger.fatal(ContainerBundler.class, e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void handle(File bundleFolder) throws Exception {
        if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
        Collection<File> workflows = FileUtil
                .listFilesRecursively(bundleFolder, new WorkflowBundler().getFileFilter());

        handleWorkflows(workflows);
    }

    private void handleWorkflows(Collection<File> workflows)
            throws DotPublishingException {

        if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }

        try {
            XStream xstream = new XStream(new DomDriver());
            WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
            //Handle folders
            for (File workflowFile : workflows) {
                if (workflowFile.isDirectory()) {
                    continue;
                }

                List<String[]> stepsWithtimedActions = new ArrayList<>();
                WorkflowWrapper workflowWrapper;
                try (final InputStream input = Files.newInputStream(workflowFile.toPath())) {
                    workflowWrapper = (WorkflowWrapper) xstream.fromXML(input);
                }

                WorkflowScheme remoteScheme = workflowWrapper.getScheme();
                WorkflowScheme localScheme = null;

                try {
                    //Trying to find the Scheme in this node
                    localScheme = workflowAPI.findScheme(remoteScheme.getId());
                } catch (DoesNotExistException | DotDataException e) {
                    Logger.debug(getClass(), "localScheme not found, moving on");
                }

                boolean localExists = localScheme != null && UtilMethods.isSet(localScheme.getId());

                if (workflowWrapper.getOperation().equals(Operation.UNPUBLISH)) {// delete operation

                    if (localExists) {
                        //First we need to archive the Scheme
                        workflowAPI.archive(localScheme, APILocator.systemUser());

                        //And now we can remove it
                        workflowAPI.deleteScheme(localScheme, APILocator.systemUser());

                        PushPublishLogger.log(getClass(), PushPublishHandler.WORKFLOW,
                                PushPublishAction.UNPUBLISH,
                                localScheme.getId(), null, localScheme.getName(), config.getId());
                    }
                } else { // create/update the workflow scheme

                    if (!localExists) {
                        workflowAPI.saveScheme(remoteScheme, APILocator.systemUser());
                    } else {
                        localScheme.setName(remoteScheme.getName());
                        localScheme.setDescription(remoteScheme.getDescription());
                        localScheme.setArchived(remoteScheme.isArchived());
                        workflowAPI.saveScheme(localScheme, APILocator.systemUser());
                    }

                    // Saving steps
                    List<WorkflowStep> steps = workflowWrapper.getSteps();
                    Set<String> stepIds = new HashSet<>();

                    for (WorkflowStep step : steps) {
                        if (UtilMethods.isSet(step.getEscalationAction())) {
                            String[] x = {step.getId(), step.getEscalationAction()};
                            stepsWithtimedActions.add(x);
                            step.setEscalationAction(null);
                        }
                        workflowAPI.saveStep(step, APILocator.systemUser());
                        stepIds.add(step.getId());
                    }

                    // Saving actions with perms
                    List<WorkflowAction> actions = workflowWrapper.getActions();
                    Map<WorkflowAction, List<Role>> actionRoles = workflowWrapper.getActionRoles();
                    Map<String, String> actionNextAssignRolekeyMap = workflowWrapper
                            .getActionNextAssignRolekeyMap();
                    Set<String> actionIds = new HashSet<>();

                    for (WorkflowAction workflowAction : actions) {
                        List<Role> roles = actionRoles.get(workflowAction);
                        List<Permission> perms = new ArrayList<>();

                        for (Role role : roles) {
                            Role localR = APILocator.getRoleAPI().loadRoleById(role.getId());
                            if (localR != null && InodeUtils.isSet(localR.getId())) {
                                perms.add(new Permission(workflowAction.getId(), role.getId(),
                                        PermissionAPI.PERMISSION_USE));
                            }
                        }

                        Role nextAssign = APILocator.getRoleAPI()
                                .loadRoleById(workflowAction.getNextAssign());

                        // if nextAssign user role does not exist
                        if (nextAssign == null) {
                            // lets try with the rolekey
                            String nextAssignRolekey = actionNextAssignRolekeyMap
                                    .get(workflowAction.getId());
                            nextAssign = APILocator.getRoleAPI().loadRoleByKey(nextAssignRolekey);

                            if (nextAssign != null && UtilMethods.isSet(nextAssign.getId())) {
                                workflowAction.setNextAssign(nextAssign.getId());
                            } else {
                                // lets see if there is a default user defined in the dotmarketing properties
                                String roleKey = Config.getStringProperty(
                                        "PUSH_PUBLISHING_WORKFLOW_ACTION_NEXT_ASSIGN_DEFAULT_USER");

                                if (UtilMethods.isSet(roleKey)) {
                                    nextAssign = APILocator.getRoleAPI().loadRoleByKey(roleKey);

                                    if (nextAssign != null && UtilMethods
                                            .isSet(nextAssign.getId())) {
                                        workflowAction.setNextAssign(nextAssign.getId());
                                    } else {
                                        // if INVALID value provided in property, lets randomly pick one admin user
                                        Logger.error(WorkflowHandler.class,
                                                "The dotmarketing-config property PUSH_PUBLISHING_WORKFLOW_ACTION_NEXT_ASSIGN_DEFAULT_USER is set to an invalid value");
                                        Role cmsAdminRole = APILocator.getRoleAPI()
                                                .loadCMSAdminRole();
                                        List<User> adminUsers = APILocator.getRoleAPI()
                                                .findUsersForRole(cmsAdminRole);
                                        if (adminUsers != null && !adminUsers.isEmpty()) {
                                            Role userRole = APILocator.getRoleAPI()
                                                    .loadRoleByKey(adminUsers.get(0).getUserId());
                                            workflowAction.setNextAssign(userRole.getId());
                                        }
                                    }
                                } else {
                                    // if property not provided, lets randomly pick one admin user
                                    Role cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();
                                    List<User> adminUsers = APILocator.getRoleAPI()
                                            .findUsersForRole(cmsAdminRole);
                                    if (adminUsers != null && !adminUsers.isEmpty()) {
                                        Role userRole = APILocator.getRoleAPI()
                                                .loadRoleByKey(adminUsers.get(0).getUserId());
                                        workflowAction.setNextAssign(userRole.getId());
                                    }
                                }
                            }
                        }

                        workflowAPI.saveAction(workflowAction, perms, APILocator.systemUser());
                        actionIds.add(workflowAction.getId());
                    }

                    //Save the associated action Steps
                    List<Map<String, String>> stepActions = workflowWrapper.getStepActions();
                    for (Map<String, String> stepAction : stepActions) {
                        workflowAPI.saveAction(stepAction.get(WorkflowBundler.ACTION_ID),
                                stepAction.get(WorkflowBundler.STEP_ID), systemUser,
                                Integer.parseInt(stepAction.get(WorkflowBundler.ACTION_ORDER)));
                    }

                    // save action classes
                    List<WorkflowActionClass> actionClasses = workflowWrapper.getActionClasses();
                    Set<String> actionClassesId = new HashSet<>();
                    for (WorkflowActionClass workflowActionClass : actionClasses) {
                        workflowAPI.saveActionClass(workflowActionClass, systemUser);
                        actionClassesId.add(workflowActionClass.getId());
                    }

                    // params
                    Set<String> paramsId = new HashSet<>();
                    workflowAPI.saveWorkflowActionClassParameters(
                            workflowWrapper.getActionClassParams(), systemUser);

                    for (WorkflowActionClassParameter param : workflowWrapper
                            .getActionClassParams()) {
                        paramsId.add(param.getId());
                    }

                    List<WorkflowStep> currentStepsList = workflowAPI.findSteps(remoteScheme);

                    // delete those not mentioned in the bundle
                    for (WorkflowStep step : currentStepsList) {
                        if (!stepIds.contains(step.getId())) {
                            // delete step
                            workflowAPI.deleteStepHardMode(step, systemUser).get();
                        } else {
                            for (WorkflowAction action : workflowAPI
                                    .findActions(step, systemUser)) {
                                if (!actionIds.contains(action.getId())) {
                                    workflowAPI.deleteAction(action, systemUser);
                                } else {
                                    for (WorkflowActionClass ac : workflowAPI
                                            .findActionClasses(action)) {
                                        if (!actionClassesId.contains(ac.getId())) {
                                            workflowAPI.deleteActionClass(ac, systemUser);
                                        } else {
                                            for (WorkflowActionClassParameter param : workflowAPI
                                                    .findParamsForActionClass(ac).values()) {
                                                if (!paramsId.contains(param.getId())) {
                                                    workflowAPI.deleteWorkflowActionClassParameter(
                                                            param);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // if any of the steps have timed actions, same thing, we are deferring this update till
                    // all the actions have been inserted
                    // https://github.com/dotCMS/core/issues/8368
                    for (String[] x : stepsWithtimedActions) {
                        WorkflowStep step = workflowAPI.findStep(x[0]);
                        step.setEscalationAction(x[1]);
                        workflowAPI.saveStep(step, APILocator.systemUser());
                    }

                    PushPublishLogger
                            .log(getClass(), PushPublishHandler.WORKFLOW, PushPublishAction.PUBLISH,
                                    remoteScheme.getId(), null, remoteScheme.getName(),
                                    config.getId());
                }


            }

        } catch (Exception e) {
            throw new DotPublishingException(e.getMessage(), e);
        }
    }

}