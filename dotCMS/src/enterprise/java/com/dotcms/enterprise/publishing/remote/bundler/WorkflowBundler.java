/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.WorkflowWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is in charge of generates the xml with the workflow information in order to be PP.
 */
public class WorkflowBundler implements IBundler {

	private PushPublisherConfig config;
	private User systemUser;
	private ContentletAPI contentletAPI     = null;
	private UserAPI userAPI                 = null;
	public static final String ACTION_ID    = "actionId";
	public static final String STEP_ID      = "stepId";
	public static final String ACTION_ORDER = "actionOrder";
    public final static String[] WORKFLOW_EXTENSIONS = {".workflow.xml", ".workflow.json"};

	@Override
	public String getName() {

		return "Workflow bundler";
	}

	@Override
	public void setConfig(final PublisherConfig publisherConfig) {

		this.config        = (PushPublisherConfig) publisherConfig;
		this.contentletAPI = APILocator.getContentletAPI();
		this.userAPI       = APILocator.getUserAPI();

		try {

			this.systemUser = this.userAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(ContainerBundler.class, e.getMessage(), e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    @CloseDBIfOpened
	@Override
	public void generate(final BundleOutput bundleOutput, final BundlerStatus status)
			throws DotBundleException {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {

			throw new RuntimeException("need an enterprise pro license to run this bundler");
		}

		try {

			final Set<String> workflowsIds = config.getWorkflows();
			for(final String workflowId : workflowsIds) {

				final WorkflowScheme workflow = APILocator.getWorkflowAPI().findScheme(workflowId);
				writeWorkflow(bundleOutput, workflow);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	private void writeWorkflow(final BundleOutput bundleOutput, final WorkflowScheme workflow)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		final List<Map<String, String>> actionStepsListMap = new ArrayList<>();
		final List<WorkflowStep> steps     = collectWorkflowSteps(workflow, actionStepsListMap);

		final Map<WorkflowAction, List<Role>> actionRoles    = new HashMap<>();
		final List<WorkflowActionClass> actionClasses        = new ArrayList<>();
		final Map<String, String> actionNextAssignRolekeyMap = new HashMap<>();
		final List<WorkflowAction> actions = collectWorkflowActions(workflow, actionRoles, actionClasses, actionNextAssignRolekeyMap);

		final List<WorkflowActionClassParameter> actionClassParams = new ArrayList<>();
		for (final WorkflowActionClass workflowActionClass : actionClasses) {

			actionClassParams.addAll(APILocator.getWorkflowAPI().findParamsForActionClass(workflowActionClass).values());
		}

		final List<SystemActionWorkflowActionMapping> systemActionMappings =
				APILocator.getWorkflowAPI().findSystemActionsByScheme(workflow, APILocator.systemUser());

		final WorkflowWrapper wrapper = new WorkflowWrapper(workflow, steps, actions, actionRoles,
				actionClasses, actionClassParams, actionNextAssignRolekeyMap, actionStepsListMap, systemActionMappings);
		wrapper.setOperation(config.getOperation());

        for (String extension : WORKFLOW_EXTENSIONS) {
            final String workflowFilePath = getFile(workflow, extension);

            try (final OutputStream outputStream = bundleOutput.addFile(workflowFilePath)) {

                BundlerUtil.writeObject(wrapper, outputStream, workflowFilePath);
            }

            bundleOutput.setLastModified(workflowFilePath, Calendar.getInstance().getTimeInMillis());
        }
		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Workflow Scheme bundled for pushing. Operation: "+config.getOperation()+", Id: "+ workflow.getId(), config.getId());
		}
	}

    private String getFile(final WorkflowScheme workflow, String extension) {

		String uri = workflow.getId();
        if (!uri.endsWith(extension)) {
            uri.replace(extension, StringPool.BLANK);
			uri.trim();
            uri += extension;
		}

		final String myFileUrl = File.separator + uri;

		Logger.debug(this, ()-> "Geneting a bundle file named: " + myFileUrl);

		return myFileUrl;
	}

	private List<WorkflowAction> collectWorkflowActions(final WorkflowScheme workflow,
														final Map<WorkflowAction, List<Role>> actionRoles,
														final List<WorkflowActionClass> actionClasses,
														final Map<String, String> actionNextAssignRolekeyMap)
			throws DotDataException, DotSecurityException {

		final List<WorkflowAction> actions = APILocator.getWorkflowAPI().findActions(workflow,systemUser);
		for (final WorkflowAction action : actions) {

			final Set<Role> roles = APILocator.getPermissionAPI().getRolesWithPermission(action, PermissionAPI.PERMISSION_USE);
			actionRoles.put(action, new ArrayList<>(roles));
			actionClasses.addAll(APILocator.getWorkflowAPI().findActionClasses(action));

			Role role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());
			//https://github.com/dotCMS/core/issues/6554
			//Assign Anonymous role if the role no longer exists.
			if(role == null){
				role = APILocator.getRoleAPI().loadCMSAnonymousRole();
			}

			actionNextAssignRolekeyMap.put(action.getId(), role.getRoleKey());
		}

		return actions;
	}

	private List<WorkflowStep> collectWorkflowSteps(final WorkflowScheme workflow,
													final List<Map<String, String>> actionStepsListMap)
			throws DotDataException, DotSecurityException {

		final List<WorkflowStep> steps = APILocator.getWorkflowAPI().findSteps(workflow);
		for (final WorkflowStep workflowStep : steps) {

			final List<WorkflowAction> stepActions = APILocator.getWorkflowAPI().findActions(workflowStep, systemUser);
			int actionOrder = 0;
			// action by step
			for (final WorkflowAction workflowAction : stepActions) {

				actionStepsListMap.add(Map.of(ACTION_ID, workflowAction.getId(),
						STEP_ID, workflowStep.getId(),
						ACTION_ORDER, String.valueOf(actionOrder++)));
			}
		}

		return steps;
	}

	@Override
	public FileFilter getFileFilter(){
        return new ExtensionFileFilter(WORKFLOW_EXTENSIONS);
	}


}
