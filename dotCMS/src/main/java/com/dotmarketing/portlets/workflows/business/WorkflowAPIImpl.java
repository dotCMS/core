package com.dotmarketing.portlets.workflows.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.*;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.ArchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckURLAccessibilityActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckinContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckoutContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CommentOnWorkflowActionlet;
import com.dotmarketing.portlets.workflows.actionlet.DeleteContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.EmailActionlet;
import com.dotmarketing.portlets.workflows.actionlet.MultipleApproverActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyUsersActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ResetTaskActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SetValueActionlet;
import com.dotmarketing.portlets.workflows.actionlet.TranslationActionlet;
import com.dotmarketing.portlets.workflows.actionlet.TwitterActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnarchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnpublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class WorkflowAPIImpl implements WorkflowAPI, WorkflowAPIOsgiService {

	private final List<Class> actionletClasses;

	private static Map<String, WorkFlowActionlet> actionletMap;

	private final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WorkflowAPIImpl() {

		actionletClasses = new ArrayList<Class>();

		// Add default actionlet classes
		actionletClasses.addAll(Arrays.asList(new Class[] {
				CommentOnWorkflowActionlet.class,
				NotifyUsersActionlet.class,
				ArchiveContentActionlet.class,
				DeleteContentActionlet.class,
				CheckinContentActionlet.class,
				CheckoutContentActionlet.class,
				UnpublishContentActionlet.class,
				PublishContentActionlet.class,
				NotifyAssigneeActionlet.class,
				UnarchiveContentActionlet.class,
				ResetTaskActionlet.class,
				MultipleApproverActionlet.class,
				TwitterActionlet.class,
				PushPublishActionlet.class,
				CheckURLAccessibilityActionlet.class,
                EmailActionlet.class,
                SetValueActionlet.class,
                PushNowActionlet.class,
				TranslationActionlet.class
		}));

		refreshWorkFlowActionletMap();
		registerBundleService();
	}

	public void registerBundleService () {
		if(System.getProperty(WebKeys.OSGI_ENABLED)!=null){
			// Register main service
			BundleContext context = HostActivator.instance().getBundleContext();
			if (null != context) {
				Hashtable<String, String> props = new Hashtable<String, String>();
				context.registerService(WorkflowAPIOsgiService.class.getName(), this, props);
			} else {
				Logger.error(this, "Bundle Context is null, WorkflowAPIOsgiService has been not registered");
			}
		}
	}

	public WorkFlowActionlet newActionlet(String className) throws DotDataException {
		for ( Class<WorkFlowActionlet> z : actionletClasses ) {
			if ( z.getName().equals(className.trim())) {
				try {
					return z.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public String addActionlet(Class workFlowActionletClass) {
		actionletClasses.add(workFlowActionletClass);
		refreshWorkFlowActionletMap();
		return workFlowActionletClass.getCanonicalName();
	}

	public void removeActionlet(String workFlowActionletName) {
		WorkFlowActionlet actionlet = actionletMap.get(workFlowActionletName);
		actionletClasses.remove(actionlet.getClass());
		refreshWorkFlowActionletMap();
	}

	@CloseDBIfOpened
	public List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException {
		return workFlowFactory.searchTasks(searcher);
	}

	@CloseDBIfOpened
	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException {
		return workFlowFactory.findTaskByContentlet(contentlet);
	}

	@CloseDBIfOpened
	public List<WorkflowStep> findStepsByContentlet(Contentlet contentlet) throws DotDataException{
		return workFlowFactory.findStepsByContentlet(contentlet);
	}

	@CloseDBIfOpened
	public WorkflowStep findStepByContentlet(Contentlet contentlet) throws DotDataException {
		WorkflowStep step = null;
		List<WorkflowStep> steps = findStepsByContentlet(contentlet);
		if( null != steps && !steps.isEmpty() && steps.size() == 1) {
			step = steps.get(0);
		}

		return step;
	}

	public boolean existSchemeIdOnSchemesList(String schemeId, List<WorkflowScheme> schemes){
		return workFlowFactory.existSchemeIdOnSchemesList(schemeId, schemes);
	}

	public WorkflowTask findTaskById(String id) throws DotDataException {
		return workFlowFactory.findWorkFlowTaskById(id);
	}

	@CloseDBIfOpened
	public List<WorkflowScheme> findSchemes(final boolean showArchived) throws DotDataException {
		return workFlowFactory.findSchemes(showArchived);
	}

	@CloseDBIfOpened
	public WorkflowScheme findDefaultScheme() throws DotDataException {
		return workFlowFactory.findDefaultScheme();
	}

	@CloseDBIfOpened
	public boolean isDefaultScheme(WorkflowScheme scheme) throws DotDataException {
		if (scheme == null || scheme.getId() == null) {
			return false;
		}
		if (workFlowFactory.findDefaultScheme().getId().equals(scheme.getId())) {
			return true;
		}
		return false;
	}

	@CloseDBIfOpened
	public WorkflowScheme findScheme(String id) throws DotDataException {
		return workFlowFactory.findScheme(id);
	}

	@WrapInTransaction
	public void saveSchemesForStruct(final Structure struc, final List<WorkflowScheme> schemes) throws DotDataException {

		try {
			workFlowFactory.saveSchemesForStruct(struc.getInode(), schemes);
		} catch(DotDataException e){
			throw e;
		}
	}

	@CloseDBIfOpened
	@Override
	public List<WorkflowScheme> findSchemesForContentType(final ContentType contentType) throws DotDataException {

		final ImmutableList.Builder<WorkflowScheme> schemes =
				new ImmutableList.Builder<>();

		if (contentType == null || ! UtilMethods.isSet(contentType.inode())
				|| LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {

			schemes.add(findDefaultScheme());
		} else {

			try {

				Logger.debug(this, "Finding the schemes for: " + contentType);
				final List<WorkflowScheme> contentTypeSchemes =
						this.workFlowFactory.findSchemesForStruct(contentType.inode());
				if(contentTypeSchemes.isEmpty()){
					schemes.add(findDefaultScheme());
				} else {
					schemes.addAll(contentTypeSchemes);
				}
			}
			catch(Exception e) {
				schemes.add(findDefaultScheme());
			}
		}

		return schemes.build();
	} // findSchemesForContentType.

	@CloseDBIfOpened
	public List<WorkflowScheme> findSchemesForStruct(final Structure structure) throws DotDataException {

        List<WorkflowScheme> schemes = new ArrayList<>();
		if(structure ==null || ! UtilMethods.isSet(structure.getInode()) || LicenseUtil.getLevel() < LicenseLevel.STANDARD.level){
			schemes.add(findDefaultScheme());
			return schemes;
		}
		try{
			schemes = workFlowFactory.findSchemesForStruct(structure.getInode());
			if(schemes.isEmpty()){
				schemes.add(findDefaultScheme());
			}
			return schemes;
		}
		catch(Exception e){
			schemes.add(findDefaultScheme());
			return schemes;
		}
	}

	@WrapInTransaction
	public void saveScheme(final WorkflowScheme scheme) throws DotDataException, AlreadyExistException {
		
		workFlowFactory.saveScheme(scheme);

	}

	public void deleteScheme(WorkflowScheme scheme) throws DotDataException {

	}

	public void activateScheme(WorkflowScheme scheme) throws DotDataException {

	}

	public void deactivateScheme(WorkflowScheme scheme) throws DotDataException {

	}

	@CloseDBIfOpened
	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
		return workFlowFactory.findSteps(scheme);
	}

	@WrapInTransaction
	public void saveStep(WorkflowStep step) throws DotDataException, AlreadyExistException {

		if (!UtilMethods.isSet(step.getName()) || !UtilMethods.isSet(step.getSchemeId())) {
			throw new DotStateException("Step name and Scheme are required");
		}
		workFlowFactory.saveStep(step);
	}

	@WrapInTransaction
	public void deleteStep(final WorkflowStep step) throws DotDataException {

		try {

			// Checking for Next Step references
			for(WorkflowStep otherStep : findSteps(findScheme(step.getSchemeId()))){
				if(otherStep.equals(step))
					continue;

				for(WorkflowAction a : findActions(otherStep, APILocator.getUserAPI().getSystemUser())){
					if(a.getNextStep().equals(step.getId())){
						throw new DotDataException("</br> <b> Step : '" + step.getName() + "' is being referenced by </b> </br></br>" + 
								" Step : '"+otherStep.getName() + "' ->  Action : '" + a.getName() + "' </br></br>");
					}
				}
			}
			
			final int countContentletsReferencingStep = getCountContentletsReferencingStep(step);
			if(countContentletsReferencingStep > 0){
				throw new DotDataException("</br> <b> Step : '" + step.getName() + "' is being referenced by: "+countContentletsReferencingStep+" contenlet(s)</b> </br></br>");
			}

			this.workFlowFactory.deleteActions(step);
			this.workFlowFactory.deleteStep(step);
		}
		catch(Exception e){
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@CloseDBIfOpened
	private int getCountContentletsReferencingStep(WorkflowStep step) throws DotDataException{
		return workFlowFactory.getCountContentletsReferencingStep(step);
	}

	@WrapInTransaction
	public void reorderStep(WorkflowStep step, int order) throws DotDataException, AlreadyExistException {
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		List<WorkflowStep> steps = null;

		try {
			steps = findSteps(scheme);
		} catch (Exception e) {
			throw new DotDataException(e.getLocalizedMessage());
		}
		List<WorkflowStep> newSteps = new ArrayList<WorkflowStep>();
		order = (order < 0) ? 0 : (order >= steps.size()) ? (steps.size() - 1) : order;
		for (int i = 0; i < steps.size(); i++) {
			WorkflowStep s = steps.get(i);
			if (s.equals(step)) {
				continue;
			}
			newSteps.add(s);
		}

		newSteps.add(order, step);
		int newOrder=0;
		for(WorkflowStep newStep : newSteps){
			newStep.setMyOrder(newOrder++);
			saveStep(newStep);
		}
	}

	@WrapInTransaction
	public void deleteComment(WorkflowComment comment) throws DotDataException {
		workFlowFactory.deleteComment(comment);
	}

	@CloseDBIfOpened
	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		return workFlowFactory.findWorkFlowComments(task);
	}

	@WrapInTransaction
	public void saveComment(WorkflowComment comment) throws DotDataException {
		if(UtilMethods.isSet(comment.getComment())){
			workFlowFactory.saveComment(comment);
		}
	}

	@CloseDBIfOpened
	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {
		return workFlowFactory.findWorkflowHistory(task);
	}

	@WrapInTransaction
	public void deleteWorkflowHistory(WorkflowHistory history) throws DotDataException {
		workFlowFactory.deleteWorkflowHistory(history);
	}

	@WrapInTransaction
	public void saveWorkflowHistory(WorkflowHistory history) throws DotDataException {
		workFlowFactory.saveWorkflowHistory(history);
	}

	@WrapInTransaction
	public void deleteWorkflowTask(WorkflowTask task) throws DotDataException {
		workFlowFactory.deleteWorkflowTask(task);
	}

	@CloseDBIfOpened
	public WorkflowTask findWorkFlowTaskById(String id) throws DotDataException {
		return workFlowFactory.findWorkFlowTaskById(id);
	}

	@CloseDBIfOpened
	public List<IFileAsset> findWorkflowTaskFilesAsContent(WorkflowTask task, User user) throws DotDataException {
		List<Contentlet> contents =  workFlowFactory.findWorkflowTaskFilesAsContent(task, user);
		return APILocator.getFileAssetAPI().fromContentletsI(contents);
	}

	@WrapInTransaction
	public void saveWorkflowTask(WorkflowTask task) throws DotDataException {
		workFlowFactory.saveWorkflowTask(task);
	}

	@WrapInTransaction
	public void saveWorkflowTask(WorkflowTask task, WorkflowProcessor processor) throws DotDataException {
		saveWorkflowTask(task);
		WorkflowHistory history = new WorkflowHistory();
		history.setWorkflowtaskId(task.getId());
		history.setActionId(processor.getAction().getId());
		history.setCreationDate(new Date());
		history.setMadeBy(processor.getUser().getUserId());
		history.setStepId(processor.getNextStep().getId());

		String comment = (UtilMethods.isSet(processor.getWorkflowMessage()))? processor.getWorkflowMessage() : "";
		String nextAssignName = (UtilMethods.isSet(processor.getNextAssign()))? processor.getNextAssign().getName() : "";


		try {
			history.setChangeDescription(
					LanguageUtil.format(processor.getUser().getLocale(), "workflow.history.description", new String[]{
						processor.getUser().getFullName(),
						processor.getAction().getName(),
						processor.getNextStep().getName(),
						nextAssignName,
						comment}, false)
					);
		} catch (LanguageException e) {
			Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
		}
		saveWorkflowHistory(history);
	}

	@WrapInTransaction
	public void attachFileToTask(WorkflowTask task, String fileInode) throws DotDataException {
		workFlowFactory.attachFileToTask(task, fileInode);
	}

	@WrapInTransaction
	public void removeAttachedFile(WorkflowTask task, String fileInode) throws DotDataException {
		workFlowFactory.removeAttachedFile(task, fileInode);
	}

	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowStep step, final User user) throws DotDataException,
	DotSecurityException {
		return findActions(step, user, null);
	}

    @CloseDBIfOpened
    public List<WorkflowAction> findActions(final WorkflowStep step, final User user, final Permissionable permissionable) throws DotDataException,
            DotSecurityException {
        List<WorkflowAction> actions = workFlowFactory.findActions(step);
        actions = filterActionsCollection(actions, user, true, permissionable);
        return actions;
    }

	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowScheme scheme, final User user) throws DotDataException,
			DotSecurityException {

		final List<WorkflowAction> actions = workFlowFactory.findActions(scheme);
		return APILocator.getPermissionAPI().filterCollection(actions,
				PermissionAPI.PERMISSION_USE, true, user);
	} // findActions.
    

    @CloseDBIfOpened
    public List<WorkflowAction> findActions(List<WorkflowStep> steps, User user) throws DotDataException,
			DotSecurityException {
		return findActions(steps, user, null);
	}

	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final List<WorkflowStep> steps,final User user, final Permissionable permissionable) throws DotDataException,
			DotSecurityException {
		final ImmutableList.Builder<WorkflowAction> actions = new ImmutableList.Builder<>();
		for(WorkflowStep step : steps) {
			actions.addAll(workFlowFactory.findActions(step));
		}

		return filterActionsCollection(actions.build(), user, true, permissionable);
	}


	/**
	 * This method will return the list of workflows actions available to a user on any give
	 * piece of content, based on how and who has the content locked and what workflow step the content
	 * is in
	 */
	@CloseDBIfOpened
	public List<WorkflowAction> findAvailableActions(Contentlet contentlet, User user) throws DotDataException,
	DotSecurityException {

		if(contentlet == null || contentlet.getStructure() ==null){
			throw new DotStateException("content is null");
		}

		List<WorkflowAction> actions= new ArrayList<WorkflowAction>();
		if("Host".equals(contentlet.getStructure().getVelocityVarName())){
			return actions;
		}

		boolean isNew  = !UtilMethods.isSet(contentlet.getInode());
		//boolean isLocked = contentlet.isLocked();
		boolean canLock = false;
		String lockedUserId =  null;
		try{
			canLock = APILocator.getContentletAPI().canLock(contentlet, user);
			lockedUserId =  APILocator.getVersionableAPI().getLockedBy(contentlet);
		} catch(Exception e){

		}

		boolean hasLock = user.getUserId().equals(lockedUserId);
		List<WorkflowStep> steps = findStepsByContentlet(contentlet);
		List<WorkflowAction> unfilteredActions = findActions(steps, user,contentlet);

		if(hasLock || isNew){
			return unfilteredActions;
		} else if(canLock){
			for(WorkflowAction workflowAction : unfilteredActions){
				if(!workflowAction.requiresCheckout()){
					actions.add(workflowAction);
				}
			}
		}

		return actions;
	}

	@WrapInTransaction
	public void reorderAction(WorkflowAction action, int order) throws DotDataException, AlreadyExistException {

		final WorkflowStep step = findStep(action.getStepId());
		this.reorderAction(action, step, APILocator.systemUser(), order);
	}

	@WrapInTransaction
	public void reorderAction(final WorkflowAction action,
							  final WorkflowStep step,
							  final User user,
							  final int order) throws DotDataException, AlreadyExistException {

		List<WorkflowAction> actions = null;
		final List<WorkflowAction> newActions = new ArrayList<WorkflowAction>();

		try {
			actions = findActions(step, user);
		} catch (Exception e) {
			throw new DotDataException(e.getLocalizedMessage());
		}

		final int normalizedOrder =
				(order < 0) ? 0 : (order >= actions.size()) ? actions.size()-1 : order;
		for (int i = 0; i < actions.size(); i++) {

			final WorkflowAction currentAction = actions.get(i);
			if (action.equals(currentAction)) {
				continue;
			}
			newActions.add(currentAction);
		}

		newActions.add(normalizedOrder, action);
		for (int i = 0; i < newActions.size(); i++) {
			this.workFlowFactory.updateOrder(newActions.get(i), step, i);
		}
	}

	@CloseDBIfOpened
	public WorkflowAction findAction(final String id, final User user) throws DotDataException, DotSecurityException {

		final WorkflowAction action = workFlowFactory.findAction(id);
		if (!APILocator.getPermissionAPI().doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, true)) {
			throw new DotSecurityException("User " + user + " cannot read action " + action.getName());
		}
		return action;
	}

	@CloseDBIfOpened
	public WorkflowAction findAction(final String actionId,
									 final String stepId,
									 final User user) throws DotDataException, DotSecurityException {

		Logger.debug(this, "Finding the action: " + actionId + " for the step: " + stepId);
		final WorkflowAction action = this.workFlowFactory.findAction(actionId, stepId);
		if (null != action && !APILocator.getPermissionAPI().doesUserHavePermission
				(action, PermissionAPI.PERMISSION_USE, user, true)) {

			throw new DotSecurityException("User " + user + " cannot read action " + action.getName());
		}

		return action;
	}

	@WrapInTransaction
	public void saveAction(final WorkflowAction action,
						   final List<Permission> permissions) throws DotDataException {
		try {

			this.saveAction(action);

			APILocator.getPermissionAPI().removePermissions(action);
			if(permissions != null){
				for (Permission permission : permissions) {

					permission.setInode(action.getId());
					APILocator.getPermissionAPI().save
							(permission, action, APILocator.getUserAPI().getSystemUser(), false);
				}
			}
		} catch (Exception e) {
			Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	@WrapInTransaction
	public void saveAction(final String actionId, final String stepId, final User user) {

		WorkflowAction workflowAction = null;
		WorkflowStep   workflowStep   = null;

		try {

			Logger.debug(this, "Saving (doing the relationship) the actionId: " + actionId + ", stepId: " + stepId);

			workflowAction = this.findAction(actionId, user);
			workflowStep   = this.findStep  (stepId);

			if (null == workflowAction) {

				Logger.debug(this, "The action: " + actionId + ", does not exists");
				throw new DoesNotExistException("Workflow-does-not-exists-action");
			}

			if (null == workflowStep) {

				Logger.debug(this, "The step: " + stepId + ", does not exists");
				throw new DoesNotExistException("Workflow-does-not-exists-step");
			}

			this.workFlowFactory.saveAction(workflowAction, workflowStep);
		} catch (DoesNotExistException  e) {

			throw e;
		} catch (AlreadyExistException e) {

			Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotWorkflowException("Workflow-action-already-exists", e);
		} catch (DotSecurityException e) {

			Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotWorkflowException("Workflow-permission-issue-save-action", e);
		} catch (Exception e) {
			if (DbConnectionFactory.isConstraintViolationException(e.getCause())) {

				Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
				throw new DotWorkflowException("Workflow-action-already-exists", e);
			} else {
				Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
				throw new DotWorkflowException("Workflow-could-not-save-action", e);
			}
		}
	} // saveAction.

	@WrapInTransaction
	private void saveAction(final WorkflowAction action) throws DotDataException, AlreadyExistException {
		workFlowFactory.saveAction(action);
	}

	@CloseDBIfOpened
	public WorkflowStep findStep(String id) throws DotDataException {
		return workFlowFactory.findStep(id);
	}

	@WrapInTransaction
	public void deleteAction(final WorkflowAction action) throws DotDataException, AlreadyExistException {

		Logger.debug(this, "Removing the WorkflowAction: " + action.getId());

		final List<WorkflowActionClass> workflowActionClasses =
				findActionClasses(action);

		Logger.debug(this, "Removing the WorkflowActionClass, for action: " + action.getId());

		if(workflowActionClasses != null && workflowActionClasses.size() > 0) {
			for(WorkflowActionClass actionClass : workflowActionClasses) {
				this.deleteActionClass(actionClass);
			}
		}

		Logger.debug(this,
				"Removing the WorkflowAction and Step Dependencies, for action: " + action.getId());
		this.workFlowFactory.deleteAction(action);
	}

	@WrapInTransaction
	public void deleteAction(final WorkflowAction action,
							 final WorkflowStep step) throws DotDataException, AlreadyExistException {

		Logger.debug(this, "Deleting the action: " + action.getId() +
					", from the step: " + step.getId());

		this.workFlowFactory.deleteAction(action, step);

	} // deleteAction.

	@CloseDBIfOpened
	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException {
		return  workFlowFactory.findActionClasses(action);
	}

	private void refreshWorkFlowActionletMap() {
		actionletMap = null;
		if (actionletMap == null) {
			synchronized (this.getClass()) {
				if (actionletMap == null) {

					List<WorkFlowActionlet> actionletList = new ArrayList<WorkFlowActionlet>();

					// get the dotmarketing-config.properties actionlet classes
					String customActionlets = Config.getStringProperty(WebKeys.WORKFLOW_ACTIONLET_CLASSES);

					StringTokenizer st = new StringTokenizer(customActionlets, ",");
					while (st.hasMoreTokens()) {
						String clazz = st.nextToken();
						try {
							WorkFlowActionlet actionlet = (WorkFlowActionlet) Class.forName(clazz.trim()).newInstance();
							actionletList.add(actionlet);
						} catch (Exception e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}

					// get the included (shipped with) actionlet classes
					for (Class<WorkFlowActionlet> z : actionletClasses) {
						try {
							actionletList.add(z.newInstance());
						} catch (InstantiationException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}

					Collections.sort(actionletList, new ActionletComparator());
					actionletMap = new LinkedHashMap<String, WorkFlowActionlet>();
					for(WorkFlowActionlet actionlet : actionletList){

						try {
							actionletMap.put(actionlet.getClass().getCanonicalName(),actionlet.getClass().newInstance());
							if ( !actionletClasses.contains( actionlet.getClass() ) ) {
								actionletClasses.add( actionlet.getClass() );
							}
						} catch (InstantiationException e) {
							Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
						}
					}
				}
			}

		}
	}

	private Map<String, WorkFlowActionlet> getActionlets() throws DotRuntimeException {
		return actionletMap;
	}

	private class ActionletComparator implements Comparator<WorkFlowActionlet>{

		public int compare(WorkFlowActionlet o1, WorkFlowActionlet o2) {
			return o1.getLocalizedName().compareTo(o2.getLocalizedName());

		}
	}

	public WorkFlowActionlet findActionlet(String clazz) throws DotRuntimeException {
		return getActionlets().get(clazz);
	}

	public List<WorkFlowActionlet> findActionlets() throws DotDataException {
		List<WorkFlowActionlet> l = new ArrayList<WorkFlowActionlet>();
		Map<String,WorkFlowActionlet>  m = getActionlets();
		for (String x : m.keySet()) {
			l.add(getActionlets().get(x));
		}
		return l;

	}

	@CloseDBIfOpened
	public WorkflowActionClass findActionClass(String id) throws DotDataException {
		return workFlowFactory.findActionClass(id);
	}

	@WrapInTransaction
	public void deleteActionClass(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException {
		try {
			// Delete action class
			final int orderOfActionClassToDelete = actionClass.getOrder();
			workFlowFactory.deleteActionClass(actionClass);
			
			// We don't need to get "complete" base action object from the database 
			// to retrieve all action classes from him. So, we can create the base action object
			// with the "action id" contain in actionClass parameter.
			WorkflowAction baseAction = new WorkflowAction();
			baseAction.setId(actionClass.getActionId());
			
			// Reorder the action classes in the database
			List<WorkflowActionClass> actionClasses = findActionClasses(baseAction);
			if((actionClasses.size() > 1) && (actionClasses.size() != orderOfActionClassToDelete)) {
				// Only update when there are action classes in the database and when the user is NOT deleting
				// the last action class
				for(WorkflowActionClass action : actionClasses) {
					if(action.getOrder() > orderOfActionClassToDelete) {
						// Subtract by 1 for those that are higher than the
						// action class deleted
						action.setOrder(action.getOrder()-1);
						saveActionClass(action);
					}
				}
			}
		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage());
		}
	}

	@WrapInTransaction
	public void saveActionClass(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException {
		workFlowFactory.saveActionClass(actionClass);
	}

	@WrapInTransaction
	public void reorderActionClass(WorkflowActionClass actionClass, int order) throws DotDataException {
		try {
			List<WorkflowActionClass> actionClasses = null;
			try {
				// We don't need to get "complete" base action object from the database 
				// to retrieve all action classes from him. So, we can create the base action object
				// with the "action id" contain in actionClass parameter.
				WorkflowAction baseAction = new WorkflowAction();
				baseAction.setId(actionClass.getActionId());
				
				actionClasses = findActionClasses(baseAction);
			} catch (Exception e) {
				throw new DotDataException(e.getLocalizedMessage());
			}
			
			final int currentOrder = actionClass.getOrder();
			for(WorkflowActionClass action : actionClasses) {
				if(currentOrder == action.getOrder()) {
					// Assign the new order to the action class
					action.setOrder(order);
				} else {
					if(currentOrder > order) {
						// When we want to move it to a lower level
						if(action.getOrder() < order) {
							continue;
						} else {
							if(action.getOrder() > currentOrder) {
								// If current item order is higher than the last order position,
								// we don't need to fix the order.
								return;
							}
							
							action.setOrder(action.getOrder() + 1);
						}
					} else {
						// When we want to move it to a higher level
						if(action.getOrder() < currentOrder) {
							continue;
						} else {
							if(action.getOrder() > order) {
								// If current item is higher than the new order position,
								// we don't need to fix the order.
								return;
							}
							
							action.setOrder(action.getOrder() - 1);
						}
					}
				}
				saveActionClass(action);
			}
		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage());
		}
	}

	@CloseDBIfOpened
	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws  DotDataException{
		return workFlowFactory.findParamsForActionClass(actionClass);
	}

	public void saveWorkflowActionClassParameters(List<WorkflowActionClassParameter> params) throws DotDataException{

		if(params ==null || params.size() ==0){
			return;
		}

		boolean localTransaction=false;
		try {
			localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

			for(WorkflowActionClassParameter param : params){
				workFlowFactory.saveWorkflowActionClassParameter(param);
			}
			if(localTransaction){
				HibernateUtil.closeAndCommitTransaction();
			}
		} catch (Exception e) {
			Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
			if(localTransaction) {
				HibernateUtil.rollbackTransaction();
			}
		} finally {
			if(localTransaction) {
				HibernateUtil.closeSessionSilently();
			}
		}
	}

	public WorkflowProcessor fireWorkflowPreCheckin(Contentlet contentlet, User user) throws DotDataException,DotWorkflowException, DotContentletValidationException{
		WorkflowProcessor processor = new WorkflowProcessor(contentlet, user);
		if(!processor.inProcess()){
			return processor;
		}

		if(processor.getScheme() != null && processor.getScheme().isMandatory()){
			if(!UtilMethods.isSet(processor.getAction())){
				throw new DotWorkflowException("A workflow action in workflow : " + processor.getScheme().getName() + " must be executed"  );
			}
		}

		List<WorkflowActionClass> actionClasses = processor.getActionClasses();
		if(actionClasses != null){
			for(WorkflowActionClass actionClass : actionClasses){
				WorkFlowActionlet actionlet= actionClass.getActionlet();
				//Validate the actionlet exists and the OSGI is installed and running. 
				if(UtilMethods.isSet(actionlet)){
					Map<String,WorkflowActionClassParameter> params = findParamsForActionClass(actionClass);
					actionlet.executePreAction(processor, params);
					//if we should stop processing further actionlets
					if(actionlet.stopProcessing()){
						break;
					}
				}else {
					throw new DotWorkflowException("Actionlet: " + actionClass.getName() + " is null. Check if the Plugin is installed and running.");
				}
				
			}
		}

		return processor;
	}

	public void fireWorkflowPostCheckin(WorkflowProcessor processor) throws DotDataException,DotWorkflowException{
		boolean local = false;

		try{
			if(!processor.inProcess()){
				return;
			}

			local = HibernateUtil.startLocalTransactionIfNeeded();

			processor.getContentlet().setStringProperty("wfActionId", processor.getAction().getId());



			WorkflowTask task = processor.getTask();
			if(task != null){
				Role r = APILocator.getRoleAPI().getUserRole(processor.getUser());
				if(task.isNew()){

					task.setCreatedBy(r.getId());
					task.setWebasset(processor.getContentlet().getIdentifier());
					if(processor.getWorkflowMessage() != null){
						task.setDescription(processor.getWorkflowMessage());
					}
				}
				task.setTitle(processor.getContentlet().getTitle());
				task.setModDate(new java.util.Date());
				if(processor.getNextAssign() != null)
					task.setAssignedTo(processor.getNextAssign().getId());
				task.setStatus(processor.getNextStep().getId());

				saveWorkflowTask(task,processor);
				if(processor.getWorkflowMessage() != null){
					WorkflowComment comment = new WorkflowComment();
					comment.setComment(processor.getWorkflowMessage());

					comment.setWorkflowtaskId(task.getId());
					comment.setCreationDate(new Date());
					comment.setPostedBy(r.getId());
					saveComment(comment);
				}
			}

			List<WorkflowActionClass> actionClasses = processor.getActionClasses();
			if(actionClasses != null){
				for(WorkflowActionClass actionClass : actionClasses){
					WorkFlowActionlet actionlet= actionClass.getActionlet();
					Map<String,WorkflowActionClassParameter> params = findParamsForActionClass(actionClass);
					actionlet.executeAction(processor, params);

					//if we should stop processing further actionlets
					if(actionlet.stopProcessing()){
						break;
					}
				}
			}
			if(UtilMethods.isSet(processor.getContentlet())){
			    APILocator.getContentletAPI().refresh(processor.getContentlet());
			}
			if(local){
				HibernateUtil.closeAndCommitTransaction();
			}

		} catch(Exception e) {
			if(local){
				HibernateUtil.rollbackTransaction();
			}
			/* Show a more descriptive error of what caused an issue here */
			Logger.error(WorkflowAPIImpl.class, "There was an unexpected error: " + e.getMessage(), e);
			throw new DotWorkflowException(e.getMessage(), e);
		} finally {
			if(local){

				HibernateUtil.closeSessionSilently();
			}
		}
	}

	// todo: note; this method is not referer by anyone, should it be removed?
	private void updateTask(WorkflowProcessor processor) throws DotDataException{
		WorkflowTask task = processor.getTask();
		task.setModDate(new java.util.Date());
		if(task.isNew()){
			Role r = APILocator.getRoleAPI().getUserRole(processor.getUser());
			task.setCreatedBy(r.getId());
			task.setTitle(processor.getContentlet().getTitle());
		}


		if(processor.getWorkflowMessage() != null){
			WorkflowComment comment = new WorkflowComment();
			comment.setComment(processor.getWorkflowMessage());
			comment.setWorkflowtaskId(task.getId());
			saveComment(comment);
		}

	}


	public WorkflowProcessor fireWorkflowNoCheckin(Contentlet contentlet, User user) throws DotDataException,DotWorkflowException, DotContentletValidationException{

		WorkflowProcessor processor = fireWorkflowPreCheckin(contentlet, user);

		fireWorkflowPostCheckin(processor);
		return processor;

	}

    @CloseDBIfOpened
	public int countTasks(WorkflowSearcher searcher)  throws DotDataException{
		return workFlowFactory.countTasks(searcher);
	}

	public void copyWorkflowActionClassParameter(WorkflowActionClassParameter from, WorkflowActionClass to) throws DotDataException{
		workFlowFactory.copyWorkflowActionClassParameter(from, to);
	}
	public void copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction to) throws DotDataException{
		workFlowFactory.copyWorkflowActionClass(from, to);
	}
	public void copyWorkflowAction(WorkflowAction from, WorkflowStep to) throws DotDataException{
		workFlowFactory.copyWorkflowAction(from, to);
	}
	public void copyWorkflowStep(WorkflowStep from, WorkflowScheme to) throws DotDataException{
		workFlowFactory.copyWorkflowStep(from, to);
	}

	@CloseDBIfOpened
	public List<WorkflowTask> searchAllTasks(WorkflowSearcher searcher) throws DotDataException {
		return workFlowFactory.searchAllTasks(searcher);
	}

	@CloseDBIfOpened
	public WorkflowHistory retrieveLastStepAction(String taskId) throws DotDataException {

		return workFlowFactory.retrieveLastStepAction(taskId);
	}

	@CloseDBIfOpened
	public WorkflowAction findEntryAction(Contentlet contentlet, User user)  throws DotDataException, DotSecurityException {
		WorkflowScheme scheme = null;
		List<WorkflowScheme> schemes = findSchemesForStruct(contentlet.getStructure());
		if(null !=  schemes && schemes.size() ==1){
			scheme =  schemes.get(0);
		}else{
			return null;
		}

		WorkflowStep entryStep = null;
		List<WorkflowStep> wfSteps = findSteps(scheme);

		for(WorkflowStep wfStep : wfSteps){
			if(!UtilMethods.isSet(entryStep))
				entryStep = wfStep;
			if(wfStep.getMyOrder() < entryStep.getMyOrder())
				entryStep = wfStep;
		}

		WorkflowAction entryAction = null;
		List<WorkflowAction> wfActions = findActions(entryStep, user);

		for(WorkflowAction wfAction : wfActions){
			if(!UtilMethods.isSet(entryAction))
				entryAction = wfAction;
			if(wfAction.getOrder() < entryAction.getOrder())
				entryAction = wfAction;
		}

		if (!APILocator.getPermissionAPI().doesUserHavePermission(entryAction, PermissionAPI.PERMISSION_USE, user, true)) {
			throw new DotSecurityException("User " + user + " cannot read action " + entryAction.getName());
		}
		return entryAction;
	}

	@CloseDBIfOpened
	@Override
	public List<WorkflowTask> findExpiredTasks() throws DotDataException, DotSecurityException {
		return workFlowFactory.findExpiredTasks();
	}

	@CloseDBIfOpened
	@Override
	public WorkflowScheme findSchemeByName(String schemaName) throws DotDataException {
		return workFlowFactory.findSchemeByName(schemaName);
	}

	@WrapInTransaction
	@Override
	public void deleteWorkflowActionClassParameter(WorkflowActionClassParameter param) throws DotDataException, AlreadyExistException {
		workFlowFactory.deleteWorkflowActionClassParameter(param);

	}

	/**
	 * Method will replace user references of the given userId in workflow, workflow_ action task and workflow comments
	 * with the replacement user id 
	 * @param userId User Identifier
	 * @param userRoleId The role id of the user
	 * @param replacementUserId The user id of the replacement user
	 * @param replacementUserRoleId The role Id of the replacemente user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	@WrapInTransaction
	public void updateUserReferences(String userId, String userRoleId, String replacementUserId, String replacementUserRoleId)throws DotDataException, DotSecurityException{
		workFlowFactory.updateUserReferences(userId, userRoleId, replacementUserId,replacementUserRoleId);
	}

	/**
	 * Method will replace step references of the given stepId in workflow, workflow_action task and contentlets
	 * with the replacement step id 
	 * @param stepId Step Identifier
	 * @param replacementStepId The step id of the replacement step
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	@WrapInTransaction
	public void updateStepReferences(String stepId, String replacementStepId) throws DotDataException, DotSecurityException {
		workFlowFactory.updateStepReferences(stepId, replacementStepId);
	}

    /**
     * Filter the list of actions to display according to the user logged permissions
     * @param actions List of action to filter
     * @param user User to validate
     * @param respectFrontEndRoles indicates if should respect frontend roles
     * @param permissionable ContentType or contentlet to validate special workflow roles
     * @return List<WorkflowAction>
     * @throws DotDataException
     */
    @CloseDBIfOpened
	private List<WorkflowAction> filterActionsCollection(final List<WorkflowAction> actions, final User user, final boolean respectFrontEndRoles, final Permissionable permissionable) throws DotDataException {

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role anyWhoCanViewContent = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
		Role anyWhoCanEditContent = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
		Role anyWhoCanPublishContent = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
		Role anyWhoCanEditPermisionsContent = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);

		if ((user != null) && roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole()))
			return actions;

		List<WorkflowAction> permissionables = new ArrayList<WorkflowAction>(actions);
		if(permissionables.isEmpty()){
			return permissionables;
		}

		WorkflowAction action;
		int i = 0;

		while (i < permissionables.size()) {
			action = permissionables.get(i);
			boolean havePermission = false;
			if(null != permissionable) {
			/* Validate if the action has one of the workflow special roles*/
                havePermission = hasSpecialWorkflowPermission(user, respectFrontEndRoles, permissionable,
                        anyWhoCanViewContent,
                        anyWhoCanEditContent, anyWhoCanPublishContent,
                        anyWhoCanEditPermisionsContent, action);

			}
			/* Validate if has other rolers permissions */
			if(APILocator.getPermissionAPI().doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, respectFrontEndRoles)){
				havePermission=true;
			}

			/* Remove the action if the user dont have permission */
			if(!havePermission){
				permissionables.remove(i);
			}else {
				++i;
			}
		}

		return permissionables;
	}

    /**
     * Return true if the action has one of the workflow action roles and if the user havas those permission
     * over the content or content type
     * @param user User to validate
     * @param respectFrontEndRoles indicates if should respect frontend roles
     * @param permissionable ContentType or contentlet to validate special workflow roles
     * @param anyWhoCanViewContent Workflow action role
     * @param anyWhoCanEditContent Workflow action role
     * @param anyWhoCanPublishContent Workflow action role
     * @param anyWhoCanEditPermisionsContent Workflow action role
     * @param action The action to validate
     * @return true if the user has one of the special workflow action role permissions, false if not
     * @throws DotDataException
     */
    @CloseDBIfOpened
    private boolean hasSpecialWorkflowPermission(User user, boolean respectFrontEndRoles,
            Permissionable permissionable, Role anyWhoCanViewContent, Role anyWhoCanEditContent,
            Role anyWhoCanPublishContent, Role anyWhoCanEditPermisionsContent,
            WorkflowAction action) throws DotDataException {
        if (APILocator.getPermissionAPI()
                .doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
                        anyWhoCanViewContent)) {
            if (APILocator.getPermissionAPI()
                    .doesUserHavePermission(permissionable, PermissionAPI.PERMISSION_READ,
                            user, respectFrontEndRoles)) {
                return true;
            }
        }
        if (APILocator.getPermissionAPI()
                .doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
                        anyWhoCanEditContent)) {
            if (APILocator.getPermissionAPI().doesUserHavePermission(permissionable,
                    PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_WRITE, user,
                    respectFrontEndRoles)) {
                return true;
            }
        }
        if (APILocator.getPermissionAPI()
                .doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
                        anyWhoCanPublishContent)) {
            if (APILocator.getPermissionAPI().doesUserHavePermission(permissionable,
                    PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_WRITE
                            + PermissionAPI.PERMISSION_PUBLISH, user,
                    respectFrontEndRoles)) {
                return true;
            }
        }
        if (APILocator.getPermissionAPI()
                .doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
                        anyWhoCanEditPermisionsContent)) {
            if (APILocator.getPermissionAPI().doesUserHavePermission(permissionable,
                    PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_WRITE
                            + PermissionAPI.PERMISSION_PUBLISH
                            + PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user,
                    respectFrontEndRoles)) {
                return true;
            }
        }
        return false;
    }


}