package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;
import static com.dotcms.util.CollectionsUtils.map;

public class WorkflowBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	public static final String ACTION_ID = "actionId";
	public static final String STEP_ID = "stepId";
	public static final String ACTION_ORDER = "actionOrder";

	public final static String WORKFLOW_EXTENSION = ".workflow.xml" ;

	@Override
	public String getName() {
		return "Workflow bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();

		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(ContainerBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		try {
			Set<String> workflowsIds = config.getWorkflows();

			for(String workflowId : workflowsIds) {
				WorkflowScheme workflow = APILocator.getWorkflowAPI().findScheme(workflowId);
				writeWorkflow(bundleRoot, workflow);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}



	private void writeWorkflow(File bundleRoot, WorkflowScheme workflow)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{

		List<WorkflowStep> steps = APILocator.getWorkflowAPI().findSteps(workflow);
		final List<WorkflowAction> actions = APILocator.getWorkflowAPI().findActions(workflow,systemUser);
		Map<WorkflowAction, List<Role>> actionRoles = new HashMap<WorkflowAction, List<Role>>();
		List<WorkflowActionClass> actionClasses = new ArrayList<WorkflowActionClass>();
		List<WorkflowActionClassParameter> actionClassParams = new ArrayList<WorkflowActionClassParameter>();
		Map<String, String> actionNextAssignRolekeyMap = new HashMap<String, String>();
		final List<Map<String, String>> actionStepsListMap = new ArrayList<>();

		for (WorkflowStep workflowStep : steps) {
			List<WorkflowAction> stepActions = APILocator.getWorkflowAPI().findActions(workflowStep, systemUser);
			int actionOrder = 0;
			// action by step
			for (WorkflowAction workflowAction : stepActions) {

				actionStepsListMap.add(map(ACTION_ID, workflowAction.getId(),
						STEP_ID, workflowStep.getId(),
						ACTION_ORDER, String.valueOf(actionOrder++)));
			}
		}

		for (WorkflowAction action : actions) {
			Set<Role> roles = APILocator.getPermissionAPI().getRolesWithPermission(action, PermissionAPI.PERMISSION_USE);
			actionRoles.put(action, new ArrayList<Role>(roles));
			actionClasses.addAll(APILocator.getWorkflowAPI().findActionClasses(action));
			Role role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());

			//https://github.com/dotCMS/core/issues/6554
			//Assign Anonymous role if the role no longer exists.
			if(role == null){
				role = APILocator.getRoleAPI().loadCMSAnonymousRole();
			}
			
			actionNextAssignRolekeyMap.put(action.getId(), role.getRoleKey());
		}

		for (WorkflowActionClass workflowActionClass : actionClasses) {
			actionClassParams.addAll(APILocator.getWorkflowAPI().findParamsForActionClass(workflowActionClass).values());
		}

		WorkflowWrapper wrapper = new WorkflowWrapper(workflow, steps, actions, actionRoles, actionClasses, actionClassParams, actionNextAssignRolekeyMap, actionStepsListMap);
		wrapper.setOperation(config.getOperation());

		String uri = workflow.getId();
		if(!uri.endsWith(WORKFLOW_EXTENSION)){
			uri.replace(WORKFLOW_EXTENSION, "");
			uri.trim();
			uri += WORKFLOW_EXTENSION;
		}

		String myFileUrl = bundleRoot.getPath() + File.separator
				+ uri;

		File workflowFile = new File(myFileUrl);
		workflowFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, workflowFile, true);
		workflowFile.setLastModified(Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Workflow Scheme bundled for pushing. Operation: "+config.getOperation()+", Id: "+ workflow.getId(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new ContainerBundlerFilter();
	}

	public class ContainerBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(WORKFLOW_EXTENSION));
		}

	}
}
