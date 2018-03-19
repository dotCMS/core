package com.dotmarketing.portlets.workflows.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.FriendClass;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.*;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.*;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.*;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.osgi.framework.BundleContext;

import java.util.*;
import java.util.stream.IntStream;


public class WorkflowAPIImpl implements WorkflowAPI, WorkflowAPIOsgiService {

	private final List<Class> actionletClasses;

	private static Map<String, WorkFlowActionlet> actionletMap;

	private final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();

	private final PermissionAPI  permissionAPI    = APILocator.getPermissionAPI();

	private final RoleAPI roleAPI = APILocator.getRoleAPI();

	private final WorkflowStateFilter workflowStatusFilter =
			new WorkflowStateFilter();

	// not very fancy, but the WorkflowImport is a friend of WorkflowAPI
	private volatile FriendClass  friendClass = null;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WorkflowAPIImpl() {

		actionletClasses = new ArrayList<Class>();

		// Add default actionlet classes
		actionletClasses	.addAll(Arrays.asList(new Class[] {
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
				FourEyeApproverActionlet.class,
				TwitterActionlet.class,
				PushPublishActionlet.class,
				CheckURLAccessibilityActionlet.class,
                EmailActionlet.class,
                SetValueActionlet.class,
                PushNowActionlet.class,
				TranslationActionlet.class,
				SaveContentActionlet.class,
				SaveContentAsDraftActionlet.class,
				CopyActionlet.class
		}));

		refreshWorkFlowActionletMap();
		registerBundleService();
	}

	private FriendClass getFriendClass () {

		if (null == this.friendClass) {
			synchronized (this) {

				if (null == this.friendClass) {
					this.friendClass =
							new FriendClass("com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil");
				}
			}
		}

		return this.friendClass;
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

	private void isUserAllowToModifiedWorkflow (final User user) {

		try {
			// if the class calling the workflow api is not friend, so checks the validation
			if (!this.getFriendClass().isFriend()) {
				DotPreconditions.isTrue(
						(LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level) &&
								APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("workflow-schemes", user),
						() -> "User " + user + " cannot access workflows ", NotAllowedUserWorkflowException.class);
			}
		} catch (DotDataException e) {
			throw new NotAllowedUserWorkflowException(e);
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

	public WorkflowTask findTaskById(final String id) throws DotDataException {
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
	public void saveSchemesForStruct(final Structure contentType,
									 final List<WorkflowScheme> schemes) throws DotDataException {


		try {

			this.workFlowFactory.saveSchemesForStruct(contentType.getInode(), schemes);
		} catch(DotDataException e){
			throw e;
		}
	}

	@WrapInTransaction
	public void saveSchemeIdsForContentType(final ContentType contentType,
											final List<String> schemesIds) throws DotDataException {


		try {
			Logger.info(WorkflowAPIImpl.class, String.format("Saving Schemas: %s for Content type %s",
					String.join(",", schemesIds), contentType.inode()));

			workFlowFactory.saveSchemeIdsForContentType(contentType.inode(), schemesIds);
		} catch(DotDataException e){
			Logger.error(WorkflowAPIImpl.class, String.format("Error saving Schemas: %s for Content type %s",
					String.join(",", schemesIds), contentType.inode()));
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
	public void saveScheme(final WorkflowScheme scheme, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		if (null != scheme && SYSTEM_WORKFLOW_ID.equals(scheme.getId())
				&& scheme.isArchived()) {

			Logger.warn(this, "Can not archive the system workflow");
			throw new DotWorkflowException("Can not archive the system workflow");
		}

		workFlowFactory.saveScheme(scheme);

	}

	@WrapInTransaction
	public void deleteScheme(final WorkflowScheme scheme, final User user) throws DotDataException {

	}

	@CloseDBIfOpened
	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
		return workFlowFactory.findSteps(scheme);
	}

	@WrapInTransaction
	public void saveStep(final WorkflowStep step, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		DotPreconditions.isTrue(UtilMethods.isSet(step.getName()) && UtilMethods.isSet(step.getSchemeId()),
				()-> "Step name and Scheme are required", DotStateException.class);

		workFlowFactory.saveStep(step);
	}

	@WrapInTransaction
	public void deleteStep(final WorkflowStep step, final User user) throws DotDataException {

		this.isUserAllowToModifiedWorkflow(user);

		try {

			// Checking for Next Step references
			for(WorkflowStep otherStep : findSteps(findScheme(step.getSchemeId()))){

				/*
				Verify we are not validating the next step is the step we want to delete.
				Remember the step can point to itself and that should not be a restriction when deleting.
				 */
				if (!otherStep.getId().equals(step.getId())) {
					for(WorkflowAction action : findActions(otherStep, APILocator.getUserAPI().getSystemUser())){

                        if(action.getNextStep().equals(step.getId())) {
                            throw new DotDataException("</br> <b> Step : '" + step.getName() + "' is being referenced by </b> </br></br>" +
                                    " Step : '"+otherStep.getName() + "' ->  Action : '" + action.getName() + "' </br></br>");
                        }
                    }
				}
			}
			
			final int countContentletsReferencingStep = getCountContentletsReferencingStep(step);
			if(countContentletsReferencingStep > 0){
				throw new DotDataException("</br> <b> Step : '" + step.getName() + "' is being referenced by: "+countContentletsReferencingStep+" contenlet(s)</b> </br></br>");
			}

			this.workFlowFactory.deleteActions(step); // workflow_action_step
			this.workFlowFactory.deleteStep(step);    // workflow_step
		} catch(Exception e){

			throw new DotDataException(e.getMessage(), e);
		}
	}

	@CloseDBIfOpened
	private int getCountContentletsReferencingStep(WorkflowStep step) throws DotDataException{
		return workFlowFactory.getCountContentletsReferencingStep(step);
	}

	@WrapInTransaction
	@Override
	public void reorderStep(final WorkflowStep step, final int order, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		final List<WorkflowStep> steps;
		final WorkflowScheme scheme = this.findScheme(step.getSchemeId());

		try {

			steps  = this.findSteps (scheme);
		} catch (Exception e) {
			throw new DotDataException(e.getLocalizedMessage(), e);
		}

		IntStream.range(0, steps.size())
				.filter(i -> steps.get(i).getId().equals(step.getId()))
				.boxed()
				.findFirst()
				.map(i -> steps.remove((int) i));

		final int newOrder = (order > steps.size()) ? steps.size():order;
		steps.add(newOrder, step);

		int i = 0;
		for(final WorkflowStep stepp : steps) {
			stepp.setMyOrder(i++);
			this.saveStep(stepp, user);
		}
	}

	@WrapInTransaction
	public void deleteComment(final WorkflowComment comment) throws DotDataException {

		this.workFlowFactory.deleteComment(comment);
	}

	@CloseDBIfOpened
	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		return workFlowFactory.findWorkFlowComments(task);
	}

	@WrapInTransaction
	public void saveComment(final WorkflowComment comment) throws DotDataException {

		if(UtilMethods.isSet(comment.getComment())) {

			this.workFlowFactory.saveComment(comment);
		}
	}

	@CloseDBIfOpened
	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {
		return workFlowFactory.findWorkflowHistory(task);
	}

	@WrapInTransaction
	public void deleteWorkflowHistory(final WorkflowHistory history) throws DotDataException {

		this.workFlowFactory.deleteWorkflowHistory(history);
	}

	@WrapInTransaction
	public void saveWorkflowHistory(final WorkflowHistory history) throws DotDataException {

		this.workFlowFactory.saveWorkflowHistory(history);
	}

	@WrapInTransaction
	public void deleteWorkflowTask(final WorkflowTask task, final User user) throws DotDataException {

		this.workFlowFactory.deleteWorkflowTask(task);
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
	public void saveWorkflowTask(final WorkflowTask task) throws DotDataException {

		if (task.getLanguageId() <= 0) {

			Logger.error(this, "The task: " + task.getId() +
								", does not have language id, setting to the default one");
			task.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		workFlowFactory.saveWorkflowTask(task);
	}

	@WrapInTransaction
	public void saveWorkflowTask(WorkflowTask task, WorkflowProcessor processor) throws DotDataException {
		this.saveWorkflowTask(task);
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
			Logger.error(WorkflowAPIImpl.class,e.getMessage());
			Logger.debug(WorkflowAPIImpl.class,e.getMessage(),e);
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

		if(null == step) {
			return Collections.emptyList();
		}
        final List<WorkflowAction> actions = workFlowFactory.findActions(step);
        return filterActionsCollection(actions, user, true, permissionable);
    }

	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowScheme scheme, final User user) throws DotDataException,
			DotSecurityException {

		final List<WorkflowAction> actions = workFlowFactory.findActions(scheme);
		return permissionAPI.filterCollection(actions,
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
	public List<WorkflowAction> findAvailableActions(final Contentlet contentlet, final User user)
			throws DotDataException, DotSecurityException {

		if(contentlet == null || contentlet.getStructure() ==null) {

			Logger.debug(this, "the Contentlet: " + contentlet + " or their structure could be null");
			throw new DotStateException("content is null");
		}

		final ImmutableList.Builder<WorkflowAction> actions =
				new ImmutableList.Builder<>();

		if(Host.HOST_VELOCITY_VAR_NAME.equals(contentlet.getStructure().getVelocityVarName())) {

			Logger.debug(this, "The contentlet: " +
					contentlet.getIdentifier() + ", is a host. Returning zero available actions");

			return Collections.emptyList();
		}

		final boolean isNew  = !UtilMethods.isSet(contentlet.getInode());
		boolean canLock      = false;
		boolean isLocked     = false;
		boolean isPublish    = false;
		boolean isArchived   = false;

		try {

			canLock      = APILocator.getContentletAPI().canLock(contentlet, user);
			isLocked     = isNew? true :  APILocator.getVersionableAPI().isLocked(contentlet);
			isPublish    = isNew? false:  APILocator.getVersionableAPI().isLive(contentlet);
			isArchived   = isNew? false:  APILocator.getVersionableAPI().isDeleted(contentlet);
		} catch(Exception e) {

		}

		final List<WorkflowStep> steps = findStepsByContentlet(contentlet);

		Logger.debug(this, "#findAvailableActions: for content: "   + contentlet.getIdentifier()
								+ ", isNew: "    + isNew
								+ ", canLock: "        + canLock + ", isLocked: " + isLocked);


		return isNew? this.doFilterActions(actions, true, false, false, canLock, isLocked, findActions(steps, user, contentlet.getContentType())):
				this.doFilterActions(actions, false, isPublish, isArchived, canLock, isLocked, findActions(steps, user, contentlet));
	}


	private List<WorkflowAction> doFilterActions(final ImmutableList.Builder<WorkflowAction> actions,
								 final boolean isNew,
								 final boolean isPublished,
								 final boolean isArchived,
								 final boolean canLock,
								 final boolean isLocked,
								 final List<WorkflowAction> unfilteredActions) {

		for (final WorkflowAction workflowAction : unfilteredActions) {

			if (this.workflowStatusFilter.filter(workflowAction,
					new ContentletStateOptions(isNew, isPublished, isArchived, canLock, isLocked))) {

            	actions.add(workflowAction);
            }
        }

        return actions.build();
	}



	/**
	 * This is a legacy method for reorder
	 *
	 * @deprecated On release 4.3, replaced by {@link #reorderAction(WorkflowAction, WorkflowStep, User, int)}
	 * @param action WorkflowAction action you want to reorder, the getStepid has to be not empty and has to have the associated step to the action
	 * @param order  int			Order for the action
	 * @throws DotDataException
	 * @throws AlreadyExistException
	 */
	@Deprecated
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

		this.isUserAllowToModifiedWorkflow(user);

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

		DotPreconditions.isTrue(
                null != action &&
                        APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("workflow-schemes",user),
				()-> "User " + user + " cannot read action " + action.getName(), DotSecurityException.class);

		return action;
	}

	@CloseDBIfOpened
	public WorkflowAction findAction(final String actionId,
									 final String stepId,
									 final User user) throws DotDataException, DotSecurityException {

		Logger.debug(this, "Finding the action: " + actionId + " for the step: " + stepId);
		final WorkflowAction action = this.workFlowFactory.findAction(actionId, stepId);
		if (null != action) {

			DotPreconditions.isTrue(APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("workflow-schemes",user),
						()-> "User " + user + " cannot read action " + action.getName(),
						DotSecurityException.class);
		}

		return action;
	}

	@WrapInTransaction
	public void saveAction(final WorkflowAction action,
						   final List<Permission> permissions,
						   final User user) throws DotDataException {

		DotPreconditions.isTrue(UtilMethods.isSet(action.getSchemeId()) && this.existsScheme(action.getSchemeId()),
				()-> "Workflow-does-not-exists-scheme",
				DoesNotExistException.class);

		try {

			this.saveAction(action, user);

			permissionAPI.removePermissions(action);
			if(permissions != null){
				for (Permission permission : permissions) {

					permission.setInode(action.getId());
					permissionAPI.save
							(permission, action, APILocator.getUserAPI().getSystemUser(), false);
				}
			}
		} catch (Exception e) {
			Logger.error(WorkflowAPIImpl.class, e.getMessage());
			Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private boolean isValidShowOn(final Set<WorkflowState> showOn) {
		return null != showOn && !showOn.isEmpty();
	}

	private boolean existsScheme(final String schemeId) {

		boolean existsScheme = false;

		try {

			existsScheme = null != this.findScheme(schemeId);
		} catch (Exception e) {
			existsScheme = false;
		}

		return existsScheme;
	}

	@WrapInTransaction
	public void saveAction(final String actionId, final String stepId,
						   final User user, final int order) {

		WorkflowAction workflowAction = null;
		WorkflowStep   workflowStep   = null;

		this.isUserAllowToModifiedWorkflow(user);

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

			this.workFlowFactory.saveAction(workflowAction, workflowStep, order);
		} catch (DoesNotExistException  e) {

			throw e;
		} catch (AlreadyExistException e) {

			Logger.error(WorkflowAPIImpl.class, e.getMessage());
			Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotWorkflowException("Workflow-action-already-exists", e);
		} catch (DotSecurityException e) {

			Logger.error(WorkflowAPIImpl.class, e.getMessage());
			Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotWorkflowException("Workflow-permission-issue-save-action", e);
		} catch (Exception e) {
			if (DbConnectionFactory.isConstraintViolationException(e.getCause())) {

				Logger.error(WorkflowAPIImpl.class, e.getMessage());
				Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
				throw new DotWorkflowException("Workflow-action-already-exists", e);
			} else {

				Logger.error(WorkflowAPIImpl.class, e.getMessage());
				Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
				throw new DotWorkflowException("Workflow-could-not-save-action", e);
			}
		}
	} // saveAction.

	@WrapInTransaction
	public void saveAction(final String actionId, final String stepId, final User user) {

		this.saveAction(actionId, stepId, user, 0);
	} // saveAction.

	@WrapInTransaction
	private void saveAction(final WorkflowAction action, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		DotPreconditions.isTrue(UtilMethods.isSet(action.getSchemeId()) && this.existsScheme(action.getSchemeId()),
				()-> "Workflow-does-not-exists-scheme", DoesNotExistException.class);

		if (!this.isValidShowOn(action.getShowOn())) {

			Logger.error(this, "No show On data on workflow action record, bad data?");
			action.setShowOn(WorkflowAPI.DEFAULT_SHOW_ON);
		}

		workFlowFactory.saveAction(action);
	}

	@CloseDBIfOpened
	public WorkflowStep findStep(String id) throws DotDataException {
		return workFlowFactory.findStep(id);
	}

	@WrapInTransaction
	public void deleteAction(final WorkflowAction action, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		Logger.debug(this, "Removing the WorkflowAction: " + action.getId());

		final List<WorkflowActionClass> workflowActionClasses =
				findActionClasses(action);

		Logger.debug(this, "Removing the WorkflowActionClass, for action: " + action.getId());

		if(workflowActionClasses != null && workflowActionClasses.size() > 0) {
			for(final WorkflowActionClass actionClass : workflowActionClasses) {
				this.deleteActionClass(actionClass, user);
			}
		}

		Logger.debug(this,
				"Removing the WorkflowAction and Step Dependencies, for action: " + action.getId());
		this.workFlowFactory.deleteAction(action);
	}

	@WrapInTransaction
	public void deleteAction(final WorkflowAction action,
							 final WorkflowStep step, User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
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
							Logger.error(WorkflowAPIImpl.class, e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}

					// get the included (shipped with) actionlet classes
					for (Class<WorkFlowActionlet> z : actionletClasses) {
						try {
							actionletList.add(z.newInstance());
						} catch (InstantiationException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
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
							Logger.error(WorkflowAPIImpl.class,e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class,e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
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
	public void deleteActionClass(final WorkflowActionClass actionClass, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

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
			final List<WorkflowActionClass> actionClasses = findActionClasses(baseAction);
			if((actionClasses.size() > 1) && (actionClasses.size() != orderOfActionClassToDelete)) {
				// Only update when there are action classes in the database and when the user is NOT deleting
				// the last action class
				for(WorkflowActionClass action : actionClasses) {
					if(action.getOrder() > orderOfActionClassToDelete) {
						// Subtract by 1 for those that are higher than the
						// action class deleted
						action.setOrder(action.getOrder()-1);
						saveActionClass(action, user);
					}
				}
			}
		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage(),e);
		}
	}

	@WrapInTransaction
	public void saveActionClass(final WorkflowActionClass actionClass, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		this.workFlowFactory.saveActionClass(actionClass);
	}

	@WrapInTransaction
	public void reorderActionClass(final WorkflowActionClass actionClass,
								   final int order,
								   final User user) throws DotDataException {

		this.isUserAllowToModifiedWorkflow(user);

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
				saveActionClass(action, user);
			}
		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage(),e);
		}
	}

	@CloseDBIfOpened
	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws  DotDataException{
		return workFlowFactory.findParamsForActionClass(actionClass);
	}

	public void saveWorkflowActionClassParameters(final List<WorkflowActionClassParameter> params,
												  final User user) throws DotDataException{

		if(params ==null || params.size() ==0){
			return;
		}

		this.isUserAllowToModifiedWorkflow(user);

		boolean localTransaction = false;
		boolean isNewConnection  = false;

		try {

			isNewConnection    = !DbConnectionFactory.connectionExists();
			localTransaction   = HibernateUtil.startLocalTransactionIfNeeded();

			for(WorkflowActionClassParameter param : params){
				workFlowFactory.saveWorkflowActionClassParameter(param);
			}

			if(localTransaction){
				HibernateUtil.closeAndCommitTransaction();
			}
		} catch (Exception e) {
			Logger.error(WorkflowAPIImpl.class,e.getMessage());
			Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
			if(localTransaction) {
				HibernateUtil.rollbackTransaction();
			}
		} finally {
			if(isNewConnection) {
				HibernateUtil.closeSessionSilently();
			}
		}
	}

	public WorkflowProcessor fireWorkflowPreCheckin(Contentlet contentlet, User user) throws DotDataException,DotWorkflowException, DotContentletValidationException{
		WorkflowProcessor processor = new WorkflowProcessor(contentlet, user);
		if(!processor.inProcess()){
			return processor;
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

			this.saveWorkflowTask(processor);

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
			Logger.error(WorkflowAPIImpl.class, "There was an unexpected error: " + e.getMessage());
			Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotWorkflowException(e.getMessage(), e);
		} finally {
			if(local){

				HibernateUtil.closeSessionSilently();
			}
		}
	}

	private String getWorkflowContentNeedsBeSaveMessage (final User user) {

		try {
			return LanguageUtil.get(user, "Workflow-Content-Needs-Be-Saved");
		} catch (LanguageException e) {
			// quiet
		}

		return "Unable to apply the workflow step, the contentlet should be saved in order to execute this workflow action";
	}

	private void saveWorkflowTask(final WorkflowProcessor processor) throws DotDataException {

		final WorkflowTask task = processor.getTask();
		if(task != null){
            Role r = roleAPI.getUserRole(processor.getUser());
            if(task.isNew()){

            	DotPreconditions.isTrue(UtilMethods.isSet(processor.getContentlet().getIdentifier()),
						() -> getWorkflowContentNeedsBeSaveMessage(processor.getUser()), DotWorkflowException.class);

                task.setCreatedBy(r.getId());
                task.setWebasset(processor.getContentlet().getIdentifier());
                task.setLanguageId(processor.getContentlet().getLanguageId());
                if(processor.getWorkflowMessage() != null){
                    task.setDescription(processor.getWorkflowMessage());
                }
            }
            task.setTitle(processor.getContentlet().getTitle());
            task.setModDate(new Date());
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
	}

	// todo: note; this method is not referer by anyone, should it be removed?
	private void updateTask(WorkflowProcessor processor) throws DotDataException{
		WorkflowTask task = processor.getTask();
		task.setModDate(new java.util.Date());
		if(task.isNew()){
			Role r = roleAPI.getUserRole(processor.getUser());
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

	@WrapInTransaction
	@Override
	public Contentlet fireContentWorkflow(final Contentlet contentlet, final ContentletDependencies dependencies) throws DotDataException {

		if(UtilMethods.isSet(dependencies.getWorkflowActionId())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, dependencies.getWorkflowActionId());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
		}

		this.validateAction (contentlet, dependencies.getModUser());

		final WorkflowProcessor processor = this.fireWorkflowPreCheckin(contentlet, dependencies.getModUser());

		processor.setContentletDependencies(dependencies);
		this.fireWorkflowPostCheckin(processor);

		return processor.getContentlet();
	} // fireContentWorkflow

	private void validateAction(final Contentlet contentlet, final User user) throws DotDataException {

		final String actionId = contentlet.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY);

		if (null != actionId) {

			try {

				final WorkflowAction action 	   = this.findAction(actionId, user);
				final List<WorkflowScheme> schemes = this.findSchemesForContentType(contentlet.getContentType());

				if (null != action && null != schemes) {

					if (!schemes.stream().anyMatch(scheme -> scheme.getId().equals(action.getSchemeId()))) {

						throw new DotDataException("Invalid-Action-Scheme-Error");
					}
				}
			} catch (DotSecurityException e) {
				throw new DotDataException(e);
			}
		}
	} // validateAction.


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

		if (!permissionAPI.doesUserHavePermission(entryAction, PermissionAPI.PERMISSION_USE, user, true)) {
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
	private List<WorkflowAction> filterActionsCollection(final List<WorkflowAction> actions,
			final User user, final boolean respectFrontEndRoles,
			final Permissionable permissionable) throws DotDataException {


		if ((user != null) && roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole())) {
			return actions;
		}

		List<WorkflowAction> permissionables = new ArrayList<>(actions);
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
                        action);

			}
			/* Validate if has other rolers permissions */
			if(permissionAPI.doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, respectFrontEndRoles)){
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
	 * Return true if the action has one of the workflow action roles and if the user has  any of
	 * those permission over the content or content type
	 * @param user User to validate
	 * @param respectFrontEndRoles indicates if should respect frontend roles
	 * @param permissionable ContentType or contentlet to validate special workflow roles
	 * @param action The action to validate
	 * @return true if the user has one of the special workflow action role permissions, false if not
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	private boolean hasSpecialWorkflowPermission(User user, boolean respectFrontEndRoles,
			Permissionable permissionable, WorkflowAction action) throws DotDataException {

		Role anyWhoCanViewContent = roleAPI
				.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_VIEW_ROLE_KEY);
		Role anyWhoCanEditContent = roleAPI
				.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
		Role anyWhoCanPublishContent = roleAPI
				.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
		Role anyWhoCanEditPermisionsContent = roleAPI
				.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_PERMISSIONS_ROLE_KEY);

		if (permissionAPI
				.doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
						anyWhoCanViewContent)) {
			return validateUserPermissionsOnPermissionable(permissionable,
					PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles);
		}
		if (permissionAPI
				.doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
						anyWhoCanEditContent)) {
			return validateUserPermissionsOnPermissionable(permissionable,
					PermissionAPI.PERMISSION_WRITE, user, respectFrontEndRoles);
		}
		if (permissionAPI
				.doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
						anyWhoCanPublishContent)) {
			return validateUserPermissionsOnPermissionable(permissionable,
					PermissionAPI.PERMISSION_PUBLISH, user, respectFrontEndRoles);
		}
		if (permissionAPI
				.doesRoleHavePermission(action, PermissionAPI.PERMISSION_USE,
						anyWhoCanEditPermisionsContent)) {
			return validateUserPermissionsOnPermissionable(permissionable,
					PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, respectFrontEndRoles);
		}
		return false;
	}

	/**
	 * Return true if the user have over the permissionable the specified
	 * permission.
	 * @param permissionable the ContentType or contentlet to validate
	 * @param permissiontype The type of permission to validate
	 * @param user           The User over who the permissions are going to be validate
	 * @param respectFrontEndRoles boolean indicating if the frontend roles should be repected
	 * @return true if the user have permissions, false if not
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	private boolean validateUserPermissionsOnPermissionable(Permissionable permissionable,
			int permissiontype, User user, boolean respectFrontEndRoles) throws DotDataException {
		if (permissionable instanceof Contentlet && !InodeUtils
				.isSet(permissionable.getPermissionId())) {
			if (permissionAPI.doesUserHavePermission(((Contentlet) permissionable).getContentType(),
					permissiontype, user, respectFrontEndRoles)) {
				return true;
			}
		} else {
			if (permissionAPI.doesUserHavePermission(permissionable, permissiontype, user,
					respectFrontEndRoles)) {
				return true;
			}
		}
		return false;
	}

    @CloseDBIfOpened
    public WorkflowAction findActionRespectingPermissions(final String id, final Permissionable permissionable,
            final User user) throws DotDataException, DotSecurityException {

        final WorkflowAction action = workFlowFactory.findAction(id);

        DotPreconditions.isTrue(
                hasSpecialWorkflowPermission(user, true, permissionable, action) ||
                        this.permissionAPI
                                .doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user,
                                        true),
                () -> "User " + user + " cannot read action " + action.getName(),
                DotSecurityException.class);

        return action;
    }

    @CloseDBIfOpened
    public WorkflowAction findActionRespectingPermissions(final String actionId,
            final String stepId, final Permissionable permissionable,
            final User user) throws DotDataException, DotSecurityException {

        Logger.debug(this, "Finding the action: " + actionId + " for the step: " + stepId);
        final WorkflowAction action = this.workFlowFactory.findAction(actionId, stepId);
        if (null != action) {

            DotPreconditions.isTrue(
                    hasSpecialWorkflowPermission(user, true, permissionable, action) ||
                            this.permissionAPI
                                    .doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, true),
                    () -> "User " + user + " cannot read action " + action.getName(),
                    DotSecurityException.class);
        }

        return action;
    }

	@CloseDBIfOpened
	public List<WorkflowAction> findAvailableDefaultActionsByContentType(ContentType contentType, User user)
			throws DotDataException, DotSecurityException {

		DotPreconditions.isTrue(this.permissionAPI.
						doesUserHavePermission(contentType, PermissionAPI.PERMISSION_READ, user,true),
				() -> "User " + user + " cannot read content type " + contentType.name(),
				DotSecurityException.class);

		List<WorkflowScheme> schemes = findSchemesForContentType(contentType);
		return findAvailableDefaultActionsBySchemes(schemes, APILocator.getUserAPI().getSystemUser());
	}


	@CloseDBIfOpened
	public List<WorkflowAction> findAvailableDefaultActionsBySchemes(List<WorkflowScheme> schemes, User user)
			throws DotDataException, DotSecurityException{
		final ImmutableList.Builder<WorkflowAction> actions = new ImmutableList.Builder<>();
		for(WorkflowScheme scheme: schemes){
			List<WorkflowStep> steps = findSteps(scheme);
			actions.addAll(findActions(steps.get(0), user));
		}
		return actions.build();

	}

	@CloseDBIfOpened
	public List<WorkflowAction> findInitialAvailableActionsByContentType(ContentType contentType, User user)
			throws DotDataException, DotSecurityException{
		final ImmutableList.Builder<WorkflowAction> actions = new ImmutableList.Builder<>();
		final List<WorkflowScheme> schemes = findSchemesForContentType(contentType);
		for(WorkflowScheme scheme: schemes){
			List<WorkflowStep> steps = findSteps(scheme);
			actions.addAll(findActions(steps.stream().findFirst().orElse(null), user));
		}

		return actions.build();
	}

}