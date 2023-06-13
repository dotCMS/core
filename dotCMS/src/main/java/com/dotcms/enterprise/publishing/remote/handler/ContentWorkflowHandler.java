package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.publisher.pusher.wrapper.PushContentWorkflowWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

/**
 * This handler will save Workflow-related information in a bundle to the
 * receiving environment.
 * 
 * @author Jorge Urdaneta
 * @version 2.x
 * @since Oct 4, 2013
 *
 */
public class ContentWorkflowHandler implements IHandler {
    
    private final PublisherConfig config;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;
    private final WorkflowAPI workflowAPI;

	/**
	 * Creates a new instance of this handler using the specified publishing
	 * configuration parameters.
	 * 
	 * @param config
	 *            - The main configuration parameters of the bundle.
	 */
	public ContentWorkflowHandler(PublisherConfig config) {
		this(config, APILocator.getRoleAPI(), APILocator.getUserAPI(), APILocator.getWorkflowAPI());
	}

	/**
	 * Visible for testing. Creates a new instance of this handler using the
	 * specified publishing configuration parameters.
	 * 
	 * @param config
	 *            - The main configuration parameters of the bundle.
	 * @param roleAPI
	 *            - The {@link RoleAPI} class.
	 * @param userAPI
	 *            - The {@link UserAPI} class.
	 * @param workflowAPI
	 *            - The {@link WorkflowAPI} class.
	 */
	@VisibleForTesting
	public ContentWorkflowHandler(PublisherConfig config, RoleAPI roleAPI, UserAPI userAPI, WorkflowAPI workflowAPI) {
		this.config = config;
		this.roleAPI = roleAPI;
		this.userAPI = userAPI;
		this.workflowAPI = workflowAPI;
	}

    @Override
    public void handle(File bundleFolder) throws Exception {
        handleContentWorkflow(FileUtil.listFilesRecursively(bundleFolder, new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.getName().endsWith(ContentBundler.CONTENT_WORKFLOW_EXTENSION);
            }
        }));
    }

    /**
	 * Reads the bundle files associated to workflows and processes their data.
	 * 
	 * @param wFiles
	 *            - The collection of workflow files in the bundle.
	 * @throws DotPublishingException
	 *             The workflow data could not be published in the receiving
	 *             server.
	 * @throws DotDataException
	 *             An error occurred when obtaining data from the data source.
	 */
    private void handleContentWorkflow(Collection<File> wFiles) throws DotPublishingException, DotDataException {
        if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
        try {
			final Map<String, String> existingContent = config.getExistingContent();

			XStream xstream=new XStream(new DomDriver());
            for(File wFile : wFiles) {
                if(wFile.isDirectory()) continue;
                
                PushContentWorkflowWrapper workflowWrapper;
                try(final InputStream input = Files.newInputStream(wFile.toPath())){
					workflowWrapper = (PushContentWorkflowWrapper)xstream.fromXML(wFile);
				}

				String taskAssetId = workflowWrapper.getTask().getWebasset();
				if (existingContent.containsKey(taskAssetId)) {
					workflowWrapper.getTask().setWebasset(existingContent.get(taskAssetId));
				}
                
                // remove local task records if exists
                WorkflowTask oldTask= this.workflowAPI.findTaskById(workflowWrapper.getTask().getId());
                if(oldTask!=null) {
                    this.workflowAPI.deleteWorkflowTask(oldTask, APILocator.systemUser());
                }
                
                if(workflowWrapper.getTask().getAssignedTo()!=null && workflowWrapper.getTask().getAssignedTo().equals("__SYSTEM_USER_ROLE__")) {
                    // user role for system user might be different
                    User systemUser = this.userAPI.getSystemUser();
                    workflowWrapper.getTask().setAssignedTo(this.roleAPI.loadRoleByKey( systemUser.getUserId() ).getId());
                }
                if (Config.getBooleanProperty("HEADLESS_USER_CONTENT_DELIVERY", Boolean.TRUE)) {
                    sanitizeWorkflowTask(workflowWrapper.getTask());
                }
                // save task/comment/history coming from sending side
                this.workflowAPI.saveWorkflowTask(checkLanguageIntegrity(workflowWrapper.getTask()));

                for(WorkflowComment comment : workflowWrapper.getComments()) {
                    this.workflowAPI.saveComment(comment);
                }
                for(WorkflowHistory history : workflowWrapper.getHistory()) {
                    this.workflowAPI.saveWorkflowHistory(history);
                }

				PushPublishLogger.log(getClass(), PushPublishHandler.CONTENT_WORKFLOW, PushPublishAction.PUBLISH,
						workflowWrapper.getTask().getWebasset(), config.getId());
            }
        }
        catch(Exception ex) {
            throw new DotPublishingException(ex.getMessage(), ex);
        } finally {
			config.cleanupExistingContent();
		}
    }

    /**
	 * Sanitizes the information included in the bundle in order to avoid
	 * manageable conflicts when deploying in a receiving environment. For
	 * example, changing the {@code assignedTo} property to the CMS
	 * Administrator ID in case the user or role assigned to the task is not
	 * present in the receiver DB. More validations can be added as required.
	 * 
	 * @param task
	 *            - The {@link WorkflowTask} being pushed.
	 * @throws DotDataException
	 *             An error occurred when reading information from the data
	 *             source.
	 */
    private void sanitizeWorkflowTask(WorkflowTask task) throws DotDataException {
		// Verify user/role exists in destination environment
    	final String assignedTo = task.getAssignedTo();
    	Role role = this.roleAPI.loadRoleById(assignedTo);
    	if (null == role || !UtilMethods.isSet(role.getId())) {
    		task.setAssignedTo(this.roleAPI.loadCMSAdminRole().getId());
    	}
	}

	/**
	 *
	 * @param task
	 */
	private WorkflowTask checkLanguageIntegrity(final WorkflowTask task){
		// This checks if any of the languages that were originally sent were replaced
		final Language localLanguage = config.getMappedRemoteLanguage(task.getLanguageId());
		if(null != localLanguage){
			// this comes to solve the problem of inserting a row that does not exist.
			// And makes sure the task is created for the respective language
			task.setLanguageId(localLanguage.getId());
		}
		return task;
    }

	@Override
    public String getName() {
        return "Content Workflow Handler";
    }

}
