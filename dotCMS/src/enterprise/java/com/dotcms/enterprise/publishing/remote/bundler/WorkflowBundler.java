/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
	public final static String WORKFLOW_EXTENSION = ".workflow.xml" ;

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

		final String workflowFilePath = getFile(workflow);

		try (final OutputStream outputStream = bundleOutput.addFile(workflowFilePath)) {

			BundlerUtil.objectToXML(wrapper, outputStream);
		}

		bundleOutput.setLastModified(workflowFilePath, Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Workflow Scheme bundled for pushing. Operation: "+config.getOperation()+", Id: "+ workflow.getId(), config.getId());
		}
	}

	private String getFile(final WorkflowScheme workflow) {

		String uri = workflow.getId();
		if(!uri.endsWith(WORKFLOW_EXTENSION)){
			uri.replace(WORKFLOW_EXTENSION, StringPool.BLANK);
			uri.trim();
			uri += WORKFLOW_EXTENSION;
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
		return new ContainerBundlerFilter();
	}

	public class ContainerBundlerFilter implements FileFilter{

		@Override
		public boolean accept(final File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(WORKFLOW_EXTENSION));
		}

	}
}
