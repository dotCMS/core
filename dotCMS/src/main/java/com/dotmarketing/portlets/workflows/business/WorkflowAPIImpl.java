package com.dotmarketing.portlets.workflows.business;

import com.dotcms.api.system.event.SystemMessageEventUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.FriendClass;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
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
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.MessageActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ArchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckURLAccessibilityActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckinContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckoutContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CommentOnWorkflowActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CopyActionlet;
import com.dotmarketing.portlets.workflows.actionlet.DeleteContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.EmailActionlet;
import com.dotmarketing.portlets.workflows.actionlet.FourEyeApproverActionlet;
import com.dotmarketing.portlets.workflows.actionlet.MultipleApproverActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyUsersActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ResetTaskActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SetValueActionlet;
import com.dotmarketing.portlets.workflows.actionlet.TranslationActionlet;
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
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.StopWatch;
import org.osgi.framework.BundleContext;


public class WorkflowAPIImpl implements WorkflowAPI, WorkflowAPIOsgiService {

	private final List<Class<? extends WorkFlowActionlet>> actionletClasses;

	private static Map<String, WorkFlowActionlet> actionletMap;

	private final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();

	private final PermissionAPI  permissionAPI    = APILocator.getPermissionAPI();

	private final RoleAPI roleAPI				  = APILocator.getRoleAPI();

	private final ShortyIdAPI shortyIdAPI		  = APILocator.getShortyAPI();

	private final WorkflowStateFilter workflowStatusFilter =
			new WorkflowStateFilter();

	private final SystemMessageEventUtil systemMessageEventUtil =
			SystemMessageEventUtil.getInstance();

	// not very fancy, but the WorkflowImport is a friend of WorkflowAPI
	private volatile FriendClass  friendClass = null;

	//This by default tells if a license is valid or not.
	private LicenseValiditySupplier licenseValiditySupplierSupplier = new LicenseValiditySupplier() {};

	private final DotConcurrentFactory concurrentFactory = DotConcurrentFactory.getInstance();

	private static final boolean RESPECT_FRONTEND_ROLES = WorkflowActionUtils.RESPECT_FRONTEND_ROLES;

	private final WorkflowActionUtils workflowActionUtils;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WorkflowAPIImpl() {

		actionletClasses = new ArrayList<>();

		// Add default actionlet classes
		actionletClasses.addAll(Arrays.asList(
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
				PushPublishActionlet.class,
				CheckURLAccessibilityActionlet.class,
                EmailActionlet.class,
                SetValueActionlet.class,
                PushNowActionlet.class,
				TranslationActionlet.class,
				SaveContentActionlet.class,
				SaveContentAsDraftActionlet.class,
				CopyActionlet.class,
				MessageActionlet.class
		));

		refreshWorkFlowActionletMap();
		registerBundleService();

		try {
			workflowActionUtils = new WorkflowActionUtils();
		} catch (DotDataException e) {
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * This allows me to override the license supplier for testing purposes
	 * @param licenseValiditySupplierSupplier
	 */
	@VisibleForTesting
	public WorkflowAPIImpl(final LicenseValiditySupplier licenseValiditySupplierSupplier) {
		this();
		this.licenseValiditySupplierSupplier = licenseValiditySupplierSupplier;
	}

	public boolean hasValidLicense(){
		return (licenseValiditySupplierSupplier.hasValidLicense());
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

	/**
	 * Converts the shortyId to long id
	 * @param shortyId String
	 * @return String id
	 */
	private String getLongId (final String shortyId, final ShortyIdAPI.ShortyInputType type) {

		final Optional<ShortyId> shortyIdOptional =
				this.shortyIdAPI.getShorty(shortyId, type);

		return shortyIdOptional.isPresent()?
				shortyIdOptional.get().longId:shortyId;
	} // getLongId.

	private String getLongIdForScheme(final String schemeId) {
		return this.getLongId(schemeId, ShortyIdAPI.ShortyInputType.WORKFLOW_SCHEME);
	}


    @Override
    public void isUserAllowToModifiedWorkflow(final User user) {

        // if the class calling the workflow api is not friend, so checks the validation
        if (!this.getFriendClass().isFriend()) {
            if (!hasValidLicense()) {
                throw new InvalidLicenseException("Workflow-Schemes-License-required");
            }

            boolean hasAccessToPortlet = false;

            try {
                hasAccessToPortlet = (APILocator.getLayoutAPI()
                        .doesUserHaveAccessToPortlet("workflow-schemes", user));
            } catch (DotDataException e) {
                Logger.error(this,
                        "Unable to verify access to portlet : workflow-schemes for user with id: "
                                + user.getUserId(), e);
            }

            if (!hasAccessToPortlet) {
                throw new WorkflowPortletAccessException("Workflow-Portlet-Access-denied");
            }
        }

    }

	@Override
	public WorkFlowActionlet newActionlet(String className) throws DotDataException {
		for ( Class<? extends WorkFlowActionlet> z : actionletClasses ) {
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

	@Override
	public String addActionlet(final Class<? extends WorkFlowActionlet> workFlowActionletClass) {

		Logger.debug(this,
				() -> "Adding actionlet class: " + workFlowActionletClass);

		actionletClasses.add(workFlowActionletClass);
		refreshWorkFlowActionletMap();
		return workFlowActionletClass.getCanonicalName();
	}

	@Override
	public void removeActionlet(final String workFlowActionletName) {

		Logger.debug(this,
				() -> "Removing actionlet: " + workFlowActionletName);

		final WorkFlowActionlet actionlet = actionletMap.get(workFlowActionletName);
		actionletClasses.remove(actionlet.getClass());
		refreshWorkFlowActionletMap();
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowTask> searchTasks(final WorkflowSearcher searcher) throws DotDataException {
		return workFlowFactory.searchTasks(searcher);
	}

	@Override
	@CloseDBIfOpened
	public WorkflowTask findTaskByContentlet(final Contentlet contentlet) throws DotDataException {
		return workFlowFactory.findTaskByContentlet(contentlet);
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowStep> findStepsByContentlet(final Contentlet contentlet) throws DotDataException{

		final List<WorkflowScheme> schemes = hasValidLicense() ?
				workFlowFactory.findSchemesForStruct(contentlet.getContentTypeId()) :
				Arrays.asList(workFlowFactory.findSystemWorkflow()) ;
		return workFlowFactory.findStepsByContentlet(contentlet, schemes);
	}

	@Override
	@CloseDBIfOpened
	public WorkflowStep findStepByContentlet(final Contentlet contentlet) throws DotDataException {

		WorkflowStep step = null;
		List<WorkflowStep> steps = findStepsByContentlet(contentlet);
		if( null != steps && !steps.isEmpty() && steps.size() == 1) {
			step = steps.get(0);
		}

		return step;
	}

	@Override
	public boolean existSchemeIdOnSchemesList(final String schemeId, final List<WorkflowScheme> schemes) {

		return workFlowFactory.existSchemeIdOnSchemesList(this.getLongId(schemeId, ShortyIdAPI.ShortyInputType.WORKFLOW_SCHEME), schemes);
	}

	@Override
	public WorkflowTask findTaskById(final String id) throws DotDataException {

		return workFlowFactory.findWorkFlowTaskById(id);
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowScheme> findSchemes(final boolean showArchived) throws DotDataException {
		if (!hasValidLicense()) {
			return new ImmutableList.Builder<WorkflowScheme>().add(findSystemWorkflowScheme()).build();
		}
		return workFlowFactory.findSchemes(showArchived);
	}


	@Override
	@CloseDBIfOpened
	public WorkflowScheme findSystemWorkflowScheme() throws DotDataException {
		return workFlowFactory.findSystemWorkflow();
	}

	@Override
	@CloseDBIfOpened
	public WorkflowScheme findScheme(final String id) throws DotDataException, DotSecurityException {

		final String schemeId = this.getLongId(id, ShortyIdAPI.ShortyInputType.WORKFLOW_SCHEME);

		if (!SYSTEM_WORKFLOW_ID.equals(schemeId)) {
			if (!hasValidLicense() && !this.getFriendClass().isFriend()) {
				throw new InvalidLicenseException("Workflow-Schemes-License-required");
			}
		}

		return workFlowFactory.findScheme(schemeId);
	}

	@Override
	@WrapInTransaction
	public void saveSchemesForStruct(final Structure contentType,
									 final List<WorkflowScheme> schemes) throws DotDataException {

		try {

			Logger.debug(this, ()-> "Saving schemes: " + schemes +
									", to the content type: " + contentType);

			this.workFlowFactory.saveSchemesForStruct(contentType.getInode(), schemes);
		} catch(DotDataException e){
			throw e;
		}
	}

	@Override
	@WrapInTransaction
	public void saveSchemeIdsForContentType(final ContentType contentType,
											final List<String> schemesIds) throws DotDataException {


		try {

			Logger.info(WorkflowAPIImpl.class, String.format("Saving Schemas: %s for Content type %s",
					String.join(",", schemesIds), contentType.inode()));

			workFlowFactory.saveSchemeIdsForContentType(contentType.inode(),
					schemesIds.stream().map(this::getLongIdForScheme).collect(CollectionsUtils.toImmutableList()));
		} catch(DotDataException e) {

			Logger.error(WorkflowAPIImpl.class, String.format("Error saving Schemas: %s for Content type %s",
					String.join(",", schemesIds), contentType.inode()));
		}
	}


	@CloseDBIfOpened
	@Override
	public List<WorkflowScheme> findSchemesForContentType(final ContentType contentType)
			throws DotDataException {

		final ImmutableList.Builder<WorkflowScheme> schemes =
				new ImmutableList.Builder<>();

		if (contentType == null || !UtilMethods.isSet(contentType.inode()) || !hasValidLicense()) {

			schemes.add(this.findSystemWorkflowScheme());
		} else {

			try {
					Logger.debug(this, () -> "Finding the schemes for: " + contentType);
					final List<WorkflowScheme> contentTypeSchemes = hasValidLicense() ?
							this.workFlowFactory.findSchemesForStruct(contentType.inode()) :
							Arrays.asList(workFlowFactory.findSystemWorkflow()) ;
					schemes.addAll(contentTypeSchemes);
			} catch(Exception e) {

				Logger.debug(this,e.getMessage(),e);
			}
		}

		return schemes.build();
	} // findSchemesForContentType.


	@Override
	@CloseDBIfOpened
	public List<ContentType> findContentTypesForScheme(final WorkflowScheme workflowScheme) {

		if (!SYSTEM_WORKFLOW_ID.equals(workflowScheme.getId())) {
			if (!hasValidLicense() && !this.getFriendClass().isFriend()) {
				throw new InvalidLicenseException("Workflow-Schemes-License-required");
			}
		}
        try {
			return workFlowFactory.findContentTypesByScheme(workflowScheme);
		}catch(Exception e){
			Logger.debug(this,e.getMessage(),e);
            throw new DoesNotExistException(e);
		}
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowScheme> findSchemesForStruct(final Structure structure) throws DotDataException {

		final ImmutableList.Builder<WorkflowScheme> schemes =
				new ImmutableList.Builder<>();

		if(structure ==null || ! UtilMethods.isSet(structure.getInode()) || !hasValidLicense()) {

			schemes.add(this.findSystemWorkflowScheme());
		} else {

			try {

				if(hasValidLicense()) {

					schemes.addAll(workFlowFactory.findSchemesForStruct(structure.getInode()));
				} else {
					schemes.add(workFlowFactory.findSystemWorkflow());
				}
			} catch (Exception e) {

				Logger.debug(this, e.getMessage(), e);
			}
		}

		return schemes.build();
	}

	@Override
	@WrapInTransaction
	public void saveScheme(final WorkflowScheme scheme, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		if (UtilMethods.isSet(scheme.getId())) {
			scheme.setId(this.getLongIdForScheme(scheme.getId()));
		}

		if (null != scheme && SYSTEM_WORKFLOW_ID.equals(scheme.getId())
				&& scheme.isArchived()) {

			Logger.warn(this, "Can not archive the system workflow");
			throw new DotWorkflowException("Can not archive the system workflow");
		}

		workFlowFactory.saveScheme(scheme);

	}

	@Override
	public Future<WorkflowScheme> deleteScheme(final WorkflowScheme scheme, final User user)
			throws DotDataException, DotSecurityException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		if (null == scheme){
			Logger.warn(this, "Can not delete a null workflow");
			throw new DotWorkflowException("Can not delete a null workflow");
		}
		if( SYSTEM_WORKFLOW_ID.equals(scheme.getId()) || !scheme.isArchived()) {

			Logger.warn(this,
					"Can not delete workflow Id:" + scheme.getId() + ", name:" + scheme.getName());
			throw new DotWorkflowException(
					"Can not delete workflow Id:" + scheme.getId() + ", name:" + scheme.getName());
		}

		final DotSubmitter submitter = this.concurrentFactory.getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
		//Delete the Scheme in a separated thread
		return submitter.submit(() -> deleteSchemeTask(scheme, user));
	}

	/**
	 * This task allows to elimiminate the workflow scheme on a separate thread
	 * @param scheme Workflow Scheme to be delete
	 * @param user The user
	 */
	@WrapInTransaction
	private WorkflowScheme deleteSchemeTask(final WorkflowScheme scheme, final User user) {
		try {
			final StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			Logger.info(this, "Begin the Delete Workflow Scheme task. workflow Id:" + scheme.getId()
					+ ", name:" + scheme.getName());

			final List<WorkflowStep> steps = this.findSteps(scheme);
			for (final WorkflowStep step : steps) {
				//delete workflow tasks
				this.findTasksByStep(step.getId())
						.forEach(task -> this.deleteWorkflowTaskWrapper(task, user));
			}
			//delete actions
			this.findActions(scheme, user)
					.forEach(action -> this.deleteWorkflowActionWrapper(action, user));

			//delete steps
			steps.forEach(step -> this.deleteWorkflowStepWrapper(step, user));

			//delete scheme
			this.workFlowFactory.deleteScheme(scheme);
			SecurityLogger.logInfo(this.getClass(),
					"The Workflow Scheme with id:" + scheme.getId() + ", name:" + scheme.getName()
							+ " was deleted");

			stopWatch.stop();
			Logger.info(this, "Delete Workflow Scheme task DONE, duration:" +
					DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");

			this.systemMessageEventUtil.pushSimpleTextEvent
					(LanguageUtil.get(user.getLocale(), "Workflow-deleted", scheme.getName()), user.getUserId());
		} catch (Exception e) {
			Logger.error(this.getClass(),
					"Error deleting Scheme: " + scheme.getId() + ", name:" + scheme.getName() + ". "
							+ e.getMessage(), e);
			throw new DotRuntimeException(e);
		}
		return scheme;
	}

	/**
	 * Wrap the delete Workflow Step method to be use by lambdas
	 *
	 * @param step The workflow step to be deleted
	 * @param user The user
	 * @throws DotRuntimeException
	 */
	@CloseDBIfOpened
	private void deleteWorkflowStepWrapper(final WorkflowStep step, final User user)
			throws DotRuntimeException {
		try {
			//delete step
			this.deleteStep(step, user);
		} catch (Exception e) {
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * Wrap the delete Workflow Action method to be use by lambdas
	 *
	 * @param action The workflow action to be deleted
	 * @param user The user
	 * @throws DotRuntimeException
	 */
	@CloseDBIfOpened
	private void deleteWorkflowActionWrapper(final WorkflowAction action, final User user)
			throws DotRuntimeException {
		try {
			//delete action
			this.deleteAction(action,user);
		} catch (Exception e) {
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * Wrap the delete Workflow Task method to be use by lambdas
	 *
	 * @param task The workflow task to be deleted
	 * @param user The user
	 * @throws DotRuntimeException
	 */
	@CloseDBIfOpened
	private void deleteWorkflowTaskWrapper(final WorkflowTask task, final User user)
			throws DotRuntimeException {
		try {
			//delete task comment
			this.findWorkFlowComments(task).forEach(this::deleteCommentWrapper);

			//delete task history
			this.findWorkflowHistory(task).forEach(this::deleteWorkflowHistoryWrapper);

			//delete task
			this.deleteWorkflowTask(task, user);
		} catch (Exception e) {
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * Wrap the delete Workflow Comment method to be use by lambdas
	 * @param workflowComment The workflow comment object to be deleted
	 * @throws DotRuntimeException
	 */
	@CloseDBIfOpened
	private void deleteCommentWrapper(final WorkflowComment workflowComment)
			throws DotRuntimeException{
		try{
			this.deleteComment(workflowComment);
		}catch(Exception e){
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * Wrap the delete Workflow History method to be use by lambdas
	 * @param workflowHistory The workflow History object to be deleted
	 * @throws DotRuntimeException
	 */
	@CloseDBIfOpened
	private void deleteWorkflowHistoryWrapper(final WorkflowHistory workflowHistory)
			throws DotRuntimeException{
		try{
			this.deleteWorkflowHistory(workflowHistory);
		}catch(Exception e){
			throw new DotRuntimeException(e);
		}
	}

	@CloseDBIfOpened
	public List<WorkflowStep> findSteps(final WorkflowScheme scheme) throws DotDataException {
		return workFlowFactory.findSteps(scheme);
	}

	@WrapInTransaction
	public void saveStep(final WorkflowStep step, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		DotPreconditions.isTrue(UtilMethods.isSet(step.getName()) && UtilMethods.isSet(step.getSchemeId()),
				()-> "Step name and Scheme are required", DotStateException.class);

		if (UtilMethods.isSet(step.getSchemeId())) {
			step.setSchemeId(this.getLongIdForScheme(step.getSchemeId()));
		}

		if (UtilMethods.isSet(step.getId())) {
			step.setId(this.getLongId(step.getId(), ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
		}

		workFlowFactory.saveStep(step);
	}

	@WrapInTransaction
	public void deleteStep(final WorkflowStep step, final User user) throws DotDataException {

		this.isUserAllowToModifiedWorkflow(user);

		try {

			// Checking for Next Step references
			for(final WorkflowStep otherStep : findSteps(findScheme(step.getSchemeId()))){

				/*
				Verify we are not validating the next step is the step we want to delete.
				Remember the step can point to itself and that should not be a restriction when deleting.
				 */
				if (!otherStep.getId().equals(step.getId())) {
					for(WorkflowAction action : findActions(otherStep, APILocator.getUserAPI().getSystemUser())){

						if (action.getNextStep().equals(step.getId())) {
							final String validationExceptionMessage = LanguageUtil.format(user.getLocale(),
									"Workflow-delete-step-reference-by-step-error",
									new String[]{step.getName(), otherStep.getName(),
											action.getName()}, false);
							throw new DotDataValidationException(validationExceptionMessage);
						}
					}
				}
			}
			
			final int countContentletsReferencingStep = getCountContentletsReferencingStep(step);
			if(countContentletsReferencingStep > 0){

				final String validationExceptionMessage = LanguageUtil.format(user.getLocale(),
						"Workflow-delete-step-reference-by-contentlet-error",
						new String[]{step.getName(), Integer.toString(countContentletsReferencingStep)}, false);
				throw new DotDataValidationException(validationExceptionMessage);
			}

			this.workFlowFactory.deleteActions(step); // workflow_action_step
			this.workFlowFactory.deleteStep(step);    // workflow_step
			SecurityLogger.logInfo(this.getClass(),
					"The Workflow Step with id:" + step.getId() + ", name:" + step.getName()
							+ " was deleted");

		} catch(Exception e){

			throw new DotDataException(e.getMessage(), e);
		}
	}

	@CloseDBIfOpened
	private int getCountContentletsReferencingStep(final WorkflowStep step) throws DotDataException{
		return workFlowFactory.getCountContentletsReferencingStep(step);
	}

	@WrapInTransaction
	@Override
	public void reorderStep(final WorkflowStep step, final int order, final User user) throws DotDataException, AlreadyExistException, DotSecurityException {

		this.isUserAllowToModifiedWorkflow(user);

		final WorkflowScheme scheme     = this.findScheme(step.getSchemeId());
		final List<WorkflowStep> steps  = this.findSteps (scheme);

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

	@Override
	@WrapInTransaction
	public void deleteComment(final WorkflowComment comment) throws DotDataException {

		this.workFlowFactory.deleteComment(comment);
		SecurityLogger.logInfo(this.getClass(),
				"The Workflow Comment with id:" + comment.getId() + " was deleted.");
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		return workFlowFactory.findWorkFlowComments(task);
	}

	@Override
	@WrapInTransaction
	public void saveComment(final WorkflowComment comment) throws DotDataException {

		if(UtilMethods.isSet(comment.getComment())) {

			this.workFlowFactory.saveComment(comment);
		}
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowHistory> findWorkflowHistory(final WorkflowTask task) throws DotDataException {
		return workFlowFactory.findWorkflowHistory(task);
	}

	@Override
	@WrapInTransaction
	public void deleteWorkflowHistory(final WorkflowHistory history) throws DotDataException {

		this.workFlowFactory.deleteWorkflowHistory(history);
		SecurityLogger.logInfo(this.getClass(),
				"The Workflow History with id:" + history.getId() + " was deleted.");
	}

	@Override
	@WrapInTransaction
	public void saveWorkflowHistory(final WorkflowHistory history) throws DotDataException {

		this.workFlowFactory.saveWorkflowHistory(history);
	}

	@Override
	@WrapInTransaction
	public void deleteWorkflowTask(final WorkflowTask task, final User user)
			throws DotDataException {

		this.workFlowFactory.deleteWorkflowTask(task);
		SecurityLogger.logInfo(this.getClass(),
				"The Workflow Task with id:" + task.getId() + " was deleted.");
	}

	@CloseDBIfOpened
	public WorkflowTask findWorkFlowTaskById(final String id) throws DotDataException {
		return workFlowFactory.findWorkFlowTaskById(id);
	}

	@Override
	@CloseDBIfOpened
	public List<IFileAsset> findWorkflowTaskFilesAsContent(final WorkflowTask task, final User user) throws DotDataException {

		final List<Contentlet> contents =  workFlowFactory.findWorkflowTaskFilesAsContent(task, user);
		return APILocator.getFileAssetAPI().fromContentletsI(contents);
	}

	@Override
	@WrapInTransaction
	public void saveWorkflowTask(final WorkflowTask task) throws DotDataException {

		if (task.getLanguageId() <= 0) {

			Logger.error(this, "The task: " + task.getId() +
								", does not have language id, setting to the default one");
			task.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		workFlowFactory.saveWorkflowTask(task);
	}

	@Override
	@WrapInTransaction
	public void saveWorkflowTask(final WorkflowTask task, final WorkflowProcessor processor) throws DotDataException {

		this.saveWorkflowTask(task);
		final WorkflowHistory history = new WorkflowHistory();
		history.setWorkflowtaskId(task.getId());
		history.setActionId(processor.getAction().getId());
		history.setCreationDate(new Date());
		history.setMadeBy(processor.getUser().getUserId());
		history.setStepId(processor.getNextStep().getId());

		final String comment = (UtilMethods.isSet(processor.getWorkflowMessage()))? processor.getWorkflowMessage()   : StringPool.BLANK;
		final String nextAssignName = (UtilMethods.isSet(processor.getNextAssign()))? processor.getNextAssign().getName() : StringPool.BLANK;


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

	@Override
	@WrapInTransaction
	public void attachFileToTask(final WorkflowTask task, final String fileInode) throws DotDataException {
		workFlowFactory.attachFileToTask(task, fileInode);
	}

	@Override
	@WrapInTransaction
	public void removeAttachedFile(final WorkflowTask task, final String fileInode) throws DotDataException {
		workFlowFactory.removeAttachedFile(task, fileInode);
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowStep step, final User user) throws DotDataException,
	DotSecurityException {
		return findActions(step, user, null);
	}

	@Override
    @CloseDBIfOpened
    public List<WorkflowAction> findActions(final WorkflowStep step, final User user, final Permissionable permissionable) throws DotDataException,
            DotSecurityException {

		if(null == step) {
			return Collections.emptyList();
		}
        final List<WorkflowAction> actions = workFlowFactory.findActions(step);
        return workflowActionUtils
				.filterActions(actions, user, RESPECT_FRONTEND_ROLES, permissionable);
    }

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowStep step, final Role role, @Nullable final Permissionable permissionable) throws DotDataException,
           DotSecurityException {

		if(null == step) {
			return Collections.emptyList();
		}
		final List<WorkflowAction> actions = workFlowFactory.findActions(step);

		return workflowActionUtils.filterActions(actions, role, permissionable);

	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowScheme scheme, final User user)
			throws DotDataException,
			DotSecurityException {
		if (!SYSTEM_WORKFLOW_ID.equals(scheme.getId())) {
			if (!hasValidLicense() && !this.getFriendClass().isFriend()) {
				throw new InvalidLicenseException("Workflow-Actions-License-required");
			}
		}

		final List<WorkflowAction> actions = workFlowFactory.findActions(scheme);
		return permissionAPI.filterCollection(actions,
				PermissionAPI.PERMISSION_USE, RESPECT_FRONTEND_ROLES, user);
	} // findActions.

	@Override
    @CloseDBIfOpened
    public List<WorkflowAction> findActions(final List<WorkflowStep> steps, final User user) throws DotDataException,
			DotSecurityException {
		return findActions(steps, user, null);
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final List<WorkflowStep> steps, final User user, final Permissionable permissionable) throws DotDataException,
			DotSecurityException {
		final ImmutableList.Builder<WorkflowAction> actions = new ImmutableList.Builder<>();
		for(WorkflowStep step : steps) {
			actions.addAll(workFlowFactory.findActions(step));
		}

		return workflowActionUtils
				.filterActions(actions.build(), user, RESPECT_FRONTEND_ROLES, permissionable);
	}


	/**
	 * This method will return the list of workflows actions available to a user on any give
	 * piece of content, based on how and who has the content locked and what workflow step the content
	 * is in
	 */
	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findAvailableActions(final Contentlet contentlet, final User user)
			throws DotDataException, DotSecurityException {

		if(contentlet == null || contentlet.getStructure() ==null) {

			Logger.debug(this, () -> "the Contentlet: " + contentlet + " or their structure could be null");
			throw new DotStateException("content is null");
		}

		final ImmutableList.Builder<WorkflowAction> actions =
				new ImmutableList.Builder<>();

		if(Host.HOST_VELOCITY_VAR_NAME.equals(contentlet.getStructure().getVelocityVarName())) {

			Logger.debug(this, () -> "The contentlet: " +
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
			isPublish    = isNew? false:  APILocator.getVersionableAPI().hasLiveVersion(contentlet);
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
	@Override
	@Deprecated
	@WrapInTransaction
	public void reorderAction(final WorkflowAction action, final int order) throws DotDataException, AlreadyExistException, DotSecurityException {

		final WorkflowStep step = findStep(action.getStepId());
		this.reorderAction(action, step, APILocator.systemUser(), order);
	}

	@Override
	@WrapInTransaction
	public void reorderAction(final WorkflowAction action,
							  final WorkflowStep step,
							  final User user,
							  final int order) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		final List<WorkflowAction> actions;

		try {
			actions = findActions(step, user);
		} catch (Exception e) {
			throw new DotDataException(e.getLocalizedMessage());
		}

		//if the user is not allowed to see actions. No exception is thrown.
		//We need to ensure we're not dealing with an empty collection.
		if (!UtilMethods.isSet(actions)) {
			Logger.debug(WorkflowAPIImpl.class,
					() -> "Empty actions retrieved for step `" + step.getId() + "` and user `"
							+ user.getUserId() + "` no reorder will be perform.");
			return;
		}

		final List<WorkflowAction> newActions = new ArrayList<>(actions.size());

		final int normalizedOrder =
				(order < 0) ? 0 : (order >= actions.size()) ? actions.size()-1 : order;
		for (final WorkflowAction currentAction : actions) {

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

	@Override
	@CloseDBIfOpened
	public WorkflowAction findAction(final String id, final User user) throws DotDataException, DotSecurityException {

		final WorkflowAction workflowAction = this.workFlowFactory.findAction(this.getLongId(id, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION));
		if(null != workflowAction){
			if (!SYSTEM_WORKFLOW_ID.equals(workflowAction.getSchemeId())) {
				if (!hasValidLicense() && !this.getFriendClass().isFriend()) {
					throw new InvalidLicenseException("Workflow-Actions-License-required");
				}
			}
		}
		return workflowAction;
	}

	@Override
	@CloseDBIfOpened
	public WorkflowAction findAction(final String actionId,
									 final String stepId,
									 final User user) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()->"Finding the action: " + actionId + " for the step: " + stepId);
		final WorkflowAction workflowAction = this.workFlowFactory
				.findAction(this.getLongId(actionId, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION),
						this.getLongId(stepId, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
		if (null != workflowAction) {
			if (!SYSTEM_WORKFLOW_ID.equals(workflowAction.getSchemeId())) {
				if (!hasValidLicense() && !this.getFriendClass().isFriend()) {
					throw new InvalidLicenseException("Workflow-Actions-License-required");
				}
			}
		}
		return workflowAction;
	}

	@Override
	@WrapInTransaction
	public void saveAction(final WorkflowAction action,
						   final List<Permission> permissions,
						   final User user) throws DotDataException {

		DotPreconditions.isTrue(UtilMethods.isSet(action.getSchemeId()) && this.existsScheme(action.getSchemeId()),
				()-> "Workflow-does-not-exists-scheme",
				DoesNotExistException.class);

		try {

			if (UtilMethods.isSet(action.getSchemeId())) {
				action.setSchemeId(this.getLongId(action.getSchemeId(), ShortyIdAPI.ShortyInputType.WORKFLOW_SCHEME));
			}

			if (UtilMethods.isSet(action.getId())) {
				action.setId(this.getLongId(action.getId(), ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION));
			}

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
		} catch (InvalidLicenseException e){
			throw e;
		} catch (Exception e) {
			existsScheme = false;
		}

		return existsScheme;
	}

	@Override
	@WrapInTransaction
	public void saveAction(final String actionId, final String stepId,
						   final User user, final int order) {

		WorkflowAction workflowAction = null;
		WorkflowStep   workflowStep   = null;

		this.isUserAllowToModifiedWorkflow(user);

		try {

			Logger.debug(this, () -> "Saving (doing the relationship) the actionId: " + actionId + ", stepId: " + stepId);

			workflowAction = this.findAction(this.getLongId(actionId, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION), user);

			if (null == workflowAction) {

				Logger.debug(this, () -> "The action: " + actionId + ", does not exists");
				throw new DoesNotExistException("Workflow-does-not-exists-action");
			}

			workflowStep   = this.findStep  (this.getLongId(stepId, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));

			if (null == workflowStep) {

				Logger.debug(this, () -> "The step: " + stepId + ", does not exists");
				throw new DoesNotExistException("Workflow-does-not-exists-step");
			}

			this.workFlowFactory.saveAction(workflowAction, workflowStep, order);
		} catch (DoesNotExistException e) {

			throw e;
		} catch ( IndexOutOfBoundsException e){

			throw new DoesNotExistException(e);
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

	@Override
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

		action.setSchemeId(this.getLongIdForScheme(action.getSchemeId()));
		if (UtilMethods.isSet(action.getId())) {
			action.setId(this.getLongId(action.getId(), ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION));
		}

		workFlowFactory.saveAction(action);
	}

	@Override
	@CloseDBIfOpened
	public WorkflowStep findStep(final String id) throws DotDataException {

		final WorkflowStep step = workFlowFactory.findStep(this.getLongId(id, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
		if (!SYSTEM_WORKFLOW_ID.equals(step.getSchemeId())) {
			if (!hasValidLicense() && !this.getFriendClass().isFriend()) {

				throw new InvalidLicenseException(
						"You must have a valid license to see any available step.");
			}
		}
		return step;
	}

	@Override
	@WrapInTransaction
	public void deleteAction(final WorkflowAction action, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		Logger.debug(this,
				() -> "Removing the WorkflowAction: " + action.getId() + ", name:" + action
						.getName());

		final List<WorkflowActionClass> workflowActionClasses =
				findActionClasses(action);

		Logger.debug(this,
				() -> "Removing the WorkflowActionClass, for action: " + action.getId() + ", name:"
						+ action.getName());

		if(workflowActionClasses != null && workflowActionClasses.size() > 0) {
			for(final WorkflowActionClass actionClass : workflowActionClasses) {
				this.deleteActionClass(actionClass, user);
			}
		}

		Logger.debug(this,
				() -> "Removing the WorkflowAction and Step Dependencies, for action: " + action
						.getId() + ", name:" + action.getName());
		this.workFlowFactory.deleteAction(action);
		SecurityLogger.logInfo(this.getClass(),
				"The Workflow Action with id:" + action.getId() + ", name:" + action.getName()
						+ " was deleted");

	}

	@Override
	@WrapInTransaction
	public void deleteAction(final WorkflowAction action,
							 final WorkflowStep step, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		Logger.debug(this, () -> "Deleting the action: " + action.getId() +
					", from the step: " + step.getId());

		this.workFlowFactory.deleteAction(action, step);

	} // deleteAction.

	@Override
	@CloseDBIfOpened
	public List<WorkflowActionClass> findActionClasses(final WorkflowAction action) throws DotDataException {

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
					for (Class<? extends WorkFlowActionlet> z : actionletClasses) {
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

	@Override
	public WorkFlowActionlet findActionlet(final String clazz) throws DotRuntimeException {
		return getActionlets().get(clazz);
	}

	@Override
	public List<WorkFlowActionlet> findActionlets() throws DotDataException {
		List<WorkFlowActionlet> l = new ArrayList<WorkFlowActionlet>();
		Map<String,WorkFlowActionlet>  m = getActionlets();
		for (String x : m.keySet()) {
			l.add(getActionlets().get(x));
		}
		return l;

	}

	@Override
	@CloseDBIfOpened
	public WorkflowActionClass findActionClass(final String id) throws DotDataException {
		return workFlowFactory.findActionClass(id);
	}

	@Override
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
			SecurityLogger.logInfo(this.getClass(),
					"The Workflow Action Class with id:" + actionClass.getId() + " was deleted");

		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage(),e);
		}
	}

	@Override
	@WrapInTransaction
	public void saveActionClass(final WorkflowActionClass actionClass, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		this.workFlowFactory.saveActionClass(actionClass);
	}

	@Override
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

	@Override
	@CloseDBIfOpened
	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(final WorkflowActionClass actionClass) throws  DotDataException {

		return workFlowFactory.findParamsForActionClass(actionClass);
	}

	@Override
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
				HibernateUtil.commitTransaction();
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

	@Override
	public WorkflowProcessor fireWorkflowPreCheckin(final Contentlet contentlet, final User user) throws DotDataException,DotWorkflowException, DotContentletValidationException{
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

	@Override
	public void fireWorkflowPostCheckin(final WorkflowProcessor processor) throws DotDataException,DotWorkflowException{
		boolean local 			= false;
		boolean isNewConnection = false;

		try{
			if(!processor.inProcess()){
				return;
			}

			isNewConnection    = !DbConnectionFactory.connectionExists();
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

			if (!processor.abort()) {

				this.saveWorkflowTask(processor);

				if (UtilMethods.isSet(processor.getContentlet())) {
					APILocator.getContentletAPI().refresh(processor.getContentlet());
				}
			}

			if(local){
				HibernateUtil.commitTransaction();
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
			if(isNewConnection){

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

		WorkflowTask task = processor.getTask();

		if (null == task) {
			task = new WorkflowTask();
		}

		final Role role = roleAPI.getUserRole(processor.getUser());
		if (task.isNew()) {

			DotPreconditions.isTrue(UtilMethods.isSet(processor.getContentlet()) && UtilMethods.isSet(processor.getContentlet().getIdentifier()),
					() -> getWorkflowContentNeedsBeSaveMessage(processor.getUser()),
					DotWorkflowException.class);

			task.setCreatedBy(role.getId());
			task.setWebasset(processor.getContentlet().getIdentifier());
			task.setLanguageId(processor.getContentlet().getLanguageId());
			if (processor.getWorkflowMessage() != null) {
				task.setDescription(processor.getWorkflowMessage());
			}
		}
		task.setTitle(processor.getContentlet().getTitle());
		task.setModDate(new Date());
		if (processor.getNextAssign() != null) {
			task.setAssignedTo(processor.getNextAssign().getId());
		}
		task.setStatus(processor.getNextStep().getId());

		saveWorkflowTask(task, processor);

		if (null == processor.getTask()) {
			processor.setTask(task); // when the content is new there might be the case than an action is waiting for the task in some commit listener
		}

		if (processor.getWorkflowMessage() != null) {
			WorkflowComment comment = new WorkflowComment();
			comment.setComment(processor.getWorkflowMessage());

			comment.setWorkflowtaskId(task.getId());
			comment.setCreationDate(new Date());
			comment.setPostedBy(processor.getUser().getUserId());
			saveComment(comment);
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
			contentlet.setActionId(dependencies.getWorkflowActionId());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
		}

		this.validateActionStepAndWorkflow(contentlet, dependencies.getModUser());
		this.checkShorties (contentlet);

		final WorkflowProcessor processor = this.fireWorkflowPreCheckin(contentlet, dependencies.getModUser());

		processor.setContentletDependencies(dependencies);
		this.fireWorkflowPostCheckin(processor);

		return processor.getContentlet();
	} // fireContentWorkflow

	private void checkShorties(final Contentlet contentlet) {

		final String actionId = contentlet.getActionId();
		if(UtilMethods.isSet(actionId)) {

			contentlet.setActionId(this.getLongId(actionId, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION));
		}
	}

	@CloseDBIfOpened
	@Override
	public void validateActionStepAndWorkflow(final Contentlet contentlet, final User user)
			throws DotDataException {

		final String actionId = contentlet.getActionId();

		if (null != actionId) {

			try {

				// validates the action belongs to the scheme
				final WorkflowAction action 	   = this.findAction(actionId, user);
				final List<WorkflowScheme> schemes = this.findSchemesForContentType(contentlet.getContentType());

				if(!UtilMethods.isSet(action)){
                    throw new DoesNotExistException("Workflow-does-not-exists-action");
				}

				if(!UtilMethods.isSet(schemes)){
					throw new DoesNotExistException("Workflow-does-not-exists-schemes-by-content-type");
				}

					if (!schemes.stream().anyMatch(scheme -> scheme.getId().equals(action.getSchemeId()))) {
						throw new IllegalArgumentException(LanguageUtil
								.get(user.getLocale(), "Invalid-Action-Scheme-Error", actionId));
					}

				// if we are on a step, validates that the action belongs to this step
				final WorkflowTask   workflowTask   = this.findTaskByContentlet(contentlet);

				if (null != workflowTask && null != workflowTask.getStatus()) {

					if (null == this.findAction(action.getId(), workflowTask.getStatus(), user)) {

						throw new IllegalArgumentException(LanguageUtil
								.get(user.getLocale(), "Invalid-Action-Step-Error", actionId));
					}
				} else {  // if the content is not in any step (may be is new), will check the first step.

					// we are sure in this moment that the scheme id on the action is in the list.
					final WorkflowScheme  scheme = schemes.stream().filter
							(aScheme -> aScheme.getId().equals(action.getSchemeId())).findFirst().get();

					final Optional<WorkflowStep> workflowStepOptional = this.findFirstStep(scheme);

					if (!workflowStepOptional.isPresent() ||
							null == this.findAction(action.getId(), workflowStepOptional.get().getId(), user)) {

						throw new IllegalArgumentException(LanguageUtil
								.get(user.getLocale(), "Invalid-Action-Step-Error", actionId));
					}
				}
			} catch (DotSecurityException | LanguageException e) {
				throw new DotDataException(e);
			}
		}
	} // validateAction.

	/**
	 * Finds the first step of the scheme, it is an optional so can be present or not depending if the scheme is not empty.
 	 * @param scheme WorkflowScheme
	 * @return Optional WorkflowStep
	 * @throws DotDataException
	 */
	private Optional<WorkflowStep> findFirstStep (final WorkflowScheme  scheme) throws DotDataException {

		return this.workFlowFactory
				.findSteps(scheme).stream().findFirst();
	}

	@Override
	public WorkflowProcessor fireWorkflowNoCheckin(final Contentlet contentlet, final User user) throws DotDataException,DotWorkflowException, DotContentletValidationException{

		WorkflowProcessor processor = fireWorkflowPreCheckin(contentlet, user);

		fireWorkflowPostCheckin(processor);
		return processor;

	}

	@Override
    @CloseDBIfOpened
	public int countTasks(final WorkflowSearcher searcher)  throws DotDataException{
		return workFlowFactory.countTasks(searcher);
	}

	@WrapInTransaction
	@Override
	public List<WorkflowActionClassParameter> copyWorkflowActionClassParameters(final Collection<WorkflowActionClassParameter> fromParams,
																		  final WorkflowActionClass to,
																		  final User user) throws DotDataException {

		final List<WorkflowActionClassParameter> params = new ArrayList<>();

		for (final WorkflowActionClassParameter from : fromParams) {

			final WorkflowActionClassParameter param = new WorkflowActionClassParameter();

			Logger.debug(this, () -> "Copying the WorkflowActionClassParameter: " + from.getId());

			param.setActionClassId(to.getActionId());
			param.setKey		  (from.getKey());
			param.setValue		  (from.getValue());

			params.add(param);
		}

		this.saveWorkflowActionClassParameters(params, user);

		return params;
	}

	@WrapInTransaction
	@Override
	public WorkflowActionClass copyWorkflowActionClass(final WorkflowActionClass from,
													   final WorkflowAction to,
													   final User user) throws DotDataException, AlreadyExistException {

		final WorkflowActionClass actionClass = new WorkflowActionClass();

		Logger.debug(this, ()-> "Copying the WorkflowActionClass: " + from.getId() +
							", name: " + from.getName());

		actionClass.setClazz   (from.getClazz());
		actionClass.setOrder   (from.getOrder());
		actionClass.setName    (from.getName());
		actionClass.setActionId(to.getId());

		this.saveActionClass(actionClass, user);

		return actionClass;
	}

	@WrapInTransaction
	@Override
	public WorkflowAction copyWorkflowAction(final WorkflowAction from,
											 final WorkflowScheme to,
											 final User user) throws DotDataException, AlreadyExistException, DotSecurityException {

		Logger.debug(this, ()-> "Copying the WorkflowAction: " + from.getId() +
				", name: " + from.getName());

		final WorkflowAction action = new WorkflowAction();

		action.setSchemeId   (to.getId());
		action.setAssignable (from.isAssignable());
		action.setCommentable(from.isCommentable());
		action.setCondition  (from.getCondition());
		action.setName		 (from.getName());
		action.setShowOn	 (from.getShowOn());
		action.setNextAssign (from.getNextAssign());
		action.setNextStep	 (from.getNextStep());
		action.setOrder		 (from.getOrder());
		action.setOwner		 (from.getOwner());
		action.setRoleHierarchyForAssign(from.isRoleHierarchyForAssign());

		this.saveAction(action, user);

		final List<Permission> permissionsFrom =
				this.permissionAPI.getPermissions(from);

		for (final Permission permissionfrom: permissionsFrom) {

			this.permissionAPI.save(new Permission(permissionfrom.getType(), action.getPermissionId(),
							permissionfrom.getRoleId(), permissionfrom.getPermission(), permissionfrom.isBitPermission()),
					action, user, false);
		}

		return action;
	}

	@WrapInTransaction
	@Override
	public WorkflowStep copyWorkflowStep(final WorkflowStep from,
										 final WorkflowScheme to,
										 final User user) throws DotDataException, AlreadyExistException {

		Logger.debug(this, ()-> "Copying the WorkflowStep: " + from.getId() +
				", name: " + from.getName());

		final WorkflowStep step = new WorkflowStep();

		step.setSchemeId   		(to.getId());
		step.setMyOrder    		(from.getMyOrder());
		step.setCreationDate	(new Date());
		step.setEnableEscalation(from.isEnableEscalation());
		step.setName		 	(from.getName());
		step.setEscalationAction(from.getEscalationAction());
		step.setEscalationTime  (from.getEscalationTime());
		step.setResolved	 	(from.isResolved());

		this.saveStep(step, user);

		return step;
	}

	@WrapInTransaction
	@Override
	public WorkflowScheme deepCopyWorkflowScheme(final WorkflowScheme from, final User user,
												 final Optional<String> optionalName) throws DotDataException, AlreadyExistException, DotSecurityException {

		// 1) create the scheme with a copy_name_timestamp
		// 2) get the stepsFrom and do a copy with the same name
		// 3) get the scheme actions and copy with a diff id
		// 4) add action class and parameters
		// 4) associate the stepsFrom to the actions.
		this.isUserAllowToModifiedWorkflow(user);

		final WorkflowScheme scheme    = new WorkflowScheme();

		Logger.debug(this, ()-> "Copying a new scheme from: "
				+ from.getId() + ", name= " + from.getName());

		scheme.setName(optionalName.isPresent() && UtilMethods.isSet(optionalName.get())?optionalName.get():from.getName() + "_" + System.currentTimeMillis());
		scheme.setArchived(from.isArchived());
		scheme.setCreationDate(new Date());
		scheme.setDescription(from.getDescription());
		scheme.setModDate(new Date());

		this.saveScheme(scheme, user);

		final Map<String, WorkflowStep> steps		  = new HashMap<>();
		final List<WorkflowStep> 	    stepsFrom 	  = this.findSteps(from);
		for (final WorkflowStep step : stepsFrom) {

			Logger.debug(this, ()-> "Copying a new step from: "
					+ step.getId() + ", name= " + step.getName());

			steps.put(step.getId(), this.copyWorkflowStep(step, scheme, user));
		}

		final Map<String, WorkflowAction> actions	   = new HashMap<>();
		final List<WorkflowAction> 		  actionsFrom  = this.findActions(from, user);

		for (final WorkflowAction action : actionsFrom) {

			Logger.debug(this, ()-> "Copying a new action from: "
					+ action.getId() + ", name= " + action.getName());

			actions.put(action.getId(), this.copyWorkflowAction(action, scheme, user));
		}

		for (final WorkflowStep step : stepsFrom) {

			int   actionOrder 						= 0;
			final List<WorkflowAction>  actionSteps =
					this.findActions(step, user);

			for (final WorkflowAction action : actionSteps) {

				final String stepId   = steps.get(step.getId()).getId();
				final String actionId = actions.get(action.getId()).getId();

				this.saveAction(actionId, stepId, user, actionOrder++);
			}
		}

		for (final WorkflowAction action : actionsFrom) {

			final List<WorkflowActionClass> workflowActionClasses =
					this.findActionClasses(action);

			for (final WorkflowActionClass fromActionClass : workflowActionClasses) {

				final WorkflowActionClass actionClass =
						this.copyWorkflowActionClass(fromActionClass, actions.get(action.getId()), user);
				final  Map<String, WorkflowActionClassParameter> classParameterMap =
						this.findParamsForActionClass(fromActionClass);

				this.copyWorkflowActionClassParameters(classParameterMap.values(), actionClass, user);
			}
		}

		return scheme;
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowTask> searchAllTasks(final WorkflowSearcher searcher) throws DotDataException {
		return workFlowFactory.searchAllTasks(searcher);
	}

	@Override
	@CloseDBIfOpened
	public WorkflowHistory retrieveLastStepAction(final String taskId) throws DotDataException {

		return workFlowFactory.retrieveLastStepAction(taskId);
	}

	@Override
	@CloseDBIfOpened
	public WorkflowAction findEntryAction(final Contentlet contentlet, final User user)  throws DotDataException, DotSecurityException {
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

		if (!permissionAPI.doesUserHavePermission(entryAction, PermissionAPI.PERMISSION_USE, user, RESPECT_FRONTEND_ROLES)) {
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
	public WorkflowScheme findSchemeByName(final String schemaName) throws DotDataException {
		return workFlowFactory.findSchemeByName(schemaName);
	}

	@WrapInTransaction
	@Override
	public void deleteWorkflowActionClassParameter(final WorkflowActionClassParameter param) throws DotDataException, AlreadyExistException {
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
	@Override
	public void updateUserReferences(final String userId, final String userRoleId,
									 final String replacementUserId, final String replacementUserRoleId) throws DotDataException, DotSecurityException {

		workFlowFactory.updateUserReferences(userId, userRoleId, replacementUserId, replacementUserRoleId);
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
	@Override
	public void updateStepReferences(final String stepId, final String replacementStepId) throws DotDataException, DotSecurityException {
		workFlowFactory.updateStepReferences(this.getLongId(stepId, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP), this.getLongId(replacementStepId, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
	}

	@Override
    @CloseDBIfOpened
    public WorkflowAction findActionRespectingPermissions(final String id, final Permissionable permissionable,
            final User user) throws DotDataException, DotSecurityException {

        final WorkflowAction action = workFlowFactory.findAction(this.getLongId(id, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION));

        DotPreconditions.isTrue(
                workflowActionUtils
						.hasSpecialWorkflowPermission(user, RESPECT_FRONTEND_ROLES, permissionable, action) ||
                        this.permissionAPI
                                .doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user,
                                        RESPECT_FRONTEND_ROLES),
                () -> "User " + user + " cannot read action " + action.getName(),
                DotSecurityException.class);

        return action;
    }

	@Override
    @CloseDBIfOpened
    public WorkflowAction findActionRespectingPermissions(final String actionId,
            final String stepId, final Permissionable permissionable,
            final User user) throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> "Finding the action: " + actionId + " for the step: " + stepId);
        final WorkflowAction action = this.workFlowFactory.findAction(this.getLongId(actionId, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION),
				this.getLongId(stepId, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
        if (null != action) {

            DotPreconditions.isTrue(
                    workflowActionUtils.hasSpecialWorkflowPermission(user, RESPECT_FRONTEND_ROLES, permissionable, action) ||
                            this.permissionAPI
                                    .doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, true),
                    () -> "User " + user + " cannot read action " + action.getName(),
                    DotSecurityException.class);
        }

        return action;
    }

	/**
	 * Returns the actions associted to the content type if there's no license. only actions
	 * associated with the system workflow will be returned.
	 *
	 * @param contentType ContentType to be processed
	 * @param user The current User
	 */
	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findAvailableDefaultActionsByContentType(final ContentType contentType,
			final User user)
			throws DotDataException, DotSecurityException {

		DotPreconditions.isTrue(this.permissionAPI.
						doesUserHavePermission(contentType, PermissionAPI.PERMISSION_READ, user, true),
				() -> "User " + user + " cannot read content type " + contentType.name(),
				DotSecurityException.class);

		List<WorkflowScheme> schemes = findSchemesForContentType(contentType);

		if (!hasValidLicense()) {
			//When no License, we only allow the system workflow
			final WorkflowScheme systemWorkflow = schemes.stream()
					.filter(WorkflowScheme::isSystem).findFirst()
					.orElse(workFlowFactory.findSystemWorkflow());
			schemes = new ImmutableList.Builder<WorkflowScheme>().add(systemWorkflow).build();

		}
		return findAvailableDefaultActionsBySchemes(schemes,
				APILocator.getUserAPI().getSystemUser());
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findAvailableDefaultActionsBySchemes(final List<WorkflowScheme> schemes, final User user)
			throws DotDataException, DotSecurityException{
		final ImmutableList.Builder<WorkflowAction> actions = new ImmutableList.Builder<>();
		for(WorkflowScheme scheme: schemes){
			final List<WorkflowStep> steps = findSteps(scheme);
			actions.addAll(findActions(steps.get(0), user));
		}
		return actions.build();

	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findInitialAvailableActionsByContentType(final ContentType contentType, final User user)
			throws DotDataException, DotSecurityException {
		final ImmutableList.Builder<WorkflowAction> actions = new ImmutableList.Builder<>();
		final List<WorkflowScheme> schemes = findSchemesForContentType(contentType);
		for(WorkflowScheme scheme: schemes){
			final List<WorkflowStep> steps = findSteps(scheme);
			actions.addAll(findActions(steps.stream().findFirst().orElse(null), user, contentType));
		}

		return actions.build();
	}

	@CloseDBIfOpened
	public List<WorkflowTask> findTasksByStep(final String stepId) throws DotDataException, DotSecurityException{
		return this.workFlowFactory.findTasksByStep(this.getLongId(stepId, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
	}

	@Override
	@WrapInTransaction
	public void archive(final WorkflowScheme scheme, final User user)
			throws DotDataException, AlreadyExistException {
		scheme.setArchived(Boolean.TRUE);
		saveScheme(scheme, user);
	}

}