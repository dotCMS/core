/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ExtensionFileFilter;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.wrapper.PushContentWorkflowWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Workflow Task-related information inside
 * a bundle and saves it in the receiving instance. This class will read and process only the {@link WorkflowTask} data
 * files.
 * <p>
 * A Workflow Tasks is a content item which has been assigned to a specific user or a Role, indicating that the user or
 * some member of the Role needs to take action on the content item.
 *
 * @author Jorge Urdaneta
 * @version 2.x
 * @since Oct 4, 2013
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
        handleContentWorkflow(FileUtil.listFilesRecursively(bundleFolder,
                new ExtensionFileFilter(ContentBundler.CONTENT_WORKFLOW_EXTENSION)));
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
		File workingOn = null;
        WorkflowTask workflowTask = null;
        try {
			final ExistingContentMapping existingContentMap =
					HandlerUtil.getExistingContentByBundleId(config.getId());

			XStream xstream = XStreamHandler.newXStreamInstance();
            for (final File wFile : wFiles) {
            	workingOn = wFile;
                if(wFile.isDirectory()) continue;
                
                PushContentWorkflowWrapper workflowWrapper;
                try(final InputStream input = Files.newInputStream(wFile.toPath())){
					workflowWrapper = (PushContentWorkflowWrapper)xstream.fromXML(wFile);
				}

                //gets the identifier and the lang id of the source content (from the sender)
				//and if the content already exists on the receiver use the identifier and the
				//lang id of the target (receiver)
				final Pair<String,Long> sourceAssetIdAndLang = Pair.of(
						workflowWrapper.getTask().getWebasset(), workflowWrapper.getTask().getLanguageId());
				if (existingContentMap.hasExistingContent(sourceAssetIdAndLang)) {
					Pair<String, Long> targetAssetIdAndLang =
							existingContentMap.getExistingContentIdentifierAndLangId(sourceAssetIdAndLang);
					workflowWrapper.getTask().setWebasset(targetAssetIdAndLang.getLeft());
					workflowWrapper.getTask().setLanguageId(targetAssetIdAndLang.getRight());
					Logger.info(this,"Workflow task " + workflowWrapper.getTask().getId()
							+ " for asset id " + sourceAssetIdAndLang.getLeft() + ", language " + sourceAssetIdAndLang.getRight()
							+ " has been mapped to existing asset id " + targetAssetIdAndLang.getLeft()
							+ ", language " + targetAssetIdAndLang.getRight());
				} else {
					final long remoteLangId = workflowWrapper.getTask().getLanguageId();
					final long localLangId = existingContentMap.getLocalForRemoteLanguage(remoteLangId);
					if (remoteLangId != localLangId && localLangId > 0) {
						workflowWrapper.getTask().setLanguageId(localLangId);
					}
				}
                workflowTask = workflowWrapper.getTask();

                if(workflowWrapper.getTask().getAssignedTo()!=null && workflowWrapper.getTask().getAssignedTo().equals("__SYSTEM_USER_ROLE__")) {
                    // user role for system user might be different
                    User systemUser = this.userAPI.getSystemUser();
                    workflowWrapper.getTask().setAssignedTo(this.roleAPI.loadRoleByKey( systemUser.getUserId() ).getId());
                }
                if (Config.getBooleanProperty("HEADLESS_USER_CONTENT_DELIVERY", Boolean.TRUE)) {
                    sanitizeWorkflowTask(workflowWrapper.getTask());
				} else if (!userExists(workflowWrapper.getTask())) {
						return;
				}

				if (!workflowStepExists(workflowTask)) {
					return;
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
        } catch (final Exception ex) {
            final String errorMsg = String.format("An error occurred when processing Workflow Task in '%s' with Title: %s And ID '%s': %s",
                    workingOn, (null == workflowTask ? "(empty)" : workflowTask.getTitle()), (null == workflowTask ? "(empty)" :
                            workflowTask.getId()), ExceptionUtil.getErrorMessage(ex));
            Logger.error(this.getClass(), errorMsg, ex);
            throw new DotPublishingException(errorMsg, ex);
        } finally {
			HandlerUtil.cleanupExistingContentByBundleId(config.getId());
		}
    }

	private boolean workflowStepExists(WorkflowTask workflowTask) throws DotDataException {
		//No, this is not an error We store the step id in status
		final String stepId = workflowTask.getStatus();
		if (UtilMethods.isNotSet(stepId)) {
			Logger.warn(ContentWorkflowHandler.class,
					String.format(" WorkflowTask %s has unassigned step.", workflowTask.getId()));
			return false;
		}
		try {
			final WorkflowStep step = APILocator.getWorkflowAPI().findStep(stepId);
			return null != step && UtilMethods.isSet(step.getId());
		} catch (DoesNotExistException e) {
			return false;
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
    	if (!userExists(task)) {
    		task.setAssignedTo(this.roleAPI.loadCMSAdminRole().getId());
    	}
	}


	private boolean userExists(final WorkflowTask task) throws DotDataException {
		final String assignedTo = task.getAssignedTo();
		Role role = this.roleAPI.loadRoleById(assignedTo);
		return null != role && UtilMethods.isSet(role.getId());
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
