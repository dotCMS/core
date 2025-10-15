package com.dotmarketing.portlets.workflows.business;

import static com.dotmarketing.portlets.contentlet.util.ContentletUtil.isHost;

import com.dotcms.ai.workflow.DotEmbeddingsActionlet;
import com.dotcms.ai.workflow.OpenAIAutoTagActionlet;
import com.dotcms.ai.workflow.OpenAIContentPromptActionlet;
import com.dotcms.ai.workflow.OpenAIGenerateImageActionlet;
import com.dotcms.ai.workflow.OpenAITranslationActionlet;
import com.dotcms.ai.workflow.OpenAIVisionAutoTagActionlet;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.event.ContentTypeDeletedEvent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.rekognition.actionlet.RekognitionActionlet;
import com.dotcms.rendering.js.JsScriptActionlet;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.api.v1.workflow.ActionFail;
import com.dotcms.rest.api.v1.workflow.BulkActionsResultView;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotcms.util.AnnotationUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.FriendClass;
import com.dotcms.util.I18NMessage;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotcms.util.ThreadContext;
import com.dotcms.util.ThreadContextUtil;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.workflow.form.AdditionalParamsBean;
import com.dotcms.workflow.form.AssignCommentBean;
import com.dotcms.workflow.form.PushPublishBean;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.util.ActionletUtil;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageDeletedEvent;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.LargeMessageActionlet;
import com.dotmarketing.portlets.workflows.MessageActionlet;
import com.dotmarketing.portlets.workflows.actionlet.Actionlet;
import com.dotmarketing.portlets.workflows.actionlet.AnalyticsFireUserEventActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ArchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.AsyncEmailActionlet;
import com.dotmarketing.portlets.workflows.actionlet.BatchAction;
import com.dotmarketing.portlets.workflows.actionlet.CheckURLAccessibilityActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckinContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckoutContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CommentOnWorkflowActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CopyActionlet;
import com.dotmarketing.portlets.workflows.actionlet.DeleteContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.DestroyContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.EmailActionlet;
import com.dotmarketing.portlets.workflows.actionlet.FourEyeApproverActionlet;
import com.dotmarketing.portlets.workflows.actionlet.MoveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.MultipleApproverActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyUsersActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ReindexContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ResetApproversActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ResetPermissionsActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ResetTaskActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SendFormEmailActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SetValueActionlet;
import com.dotmarketing.portlets.workflows.actionlet.TranslationActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnarchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnpublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryState;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryType;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.model.WorkflowTimelineItem;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.LuceneQueryUtils;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.felix.framework.OSGIUtil;
import org.elasticsearch.search.query.QueryPhaseExecutionException;
import org.osgi.framework.BundleContext;

/**
 * Implementation class for {@link WorkflowAPI}.
 *
 * @author root
 * @since Mar 22, 2012
 */
public class WorkflowAPIImpl implements WorkflowAPI, WorkflowAPIOsgiService {

	private final List<Class<? extends WorkFlowActionlet>> actionletClasses;

	private static Map<String, WorkFlowActionlet> actionletMap;

	private final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();

	private final PermissionAPI  permissionAPI    = APILocator.getPermissionAPI();

	private final RoleAPI roleAPI				  = APILocator.getRoleAPI();

	private final ShortyIdAPI shortyIdAPI		  = APILocator.getShortyAPI();

	private final LocalSystemEventsAPI localSystemEventsAPI =
			APILocator.getLocalSystemEventsAPI();

	private final WorkflowStateFilter workflowStatusFilter =
			new WorkflowStateFilter();

	private final SystemMessageEventUtil systemMessageEventUtil =
			SystemMessageEventUtil.getInstance();

	private final ReindexQueueAPI reindexQueueAPI =
			APILocator.getReindexQueueAPI();

	private final ContentletIndexAPI contentletIndexAPI =
			APILocator.getContentletIndexAPI();

	// not very fancy, but the WorkflowImport is a friend of WorkflowAPI
	private volatile FriendClass  friendClass = null;

	//This by default tells if a license is valid or not.
	private LicenseValiditySupplier licenseValiditySupplierSupplier = new LicenseValiditySupplier() {};

	private final DotConcurrentFactory concurrentFactory = DotConcurrentFactory.getInstance();

	private static final boolean RESPECT_FRONTEND_ROLES = WorkflowActionUtils.RESPECT_FRONTEND_ROLES;

	private final WorkflowActionUtils workflowActionUtils;

	private final ContentletAPI contentletAPI = APILocator.getContentletAPI();

	private final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser(), RESPECT_FRONTEND_ROLES);

	private static final String MAX_THREADS_ALLOWED_TO_HANDLE_BULK_ACTIONS = "workflow.action.bulk.maxthreads";

	private static final int MAX_THREADS_ALLOWED_TO_HANDLE_BULK_ACTIONS_DEFAULT = 5;

	private static final String MAX_EXCEPTIONS_REPORTED_ON_BULK_ACTIONS = "workflow.action.bulk.maxexceptions";

	private static final int MAX_EXCEPTIONS_REPORTED_ON_BULK_ACTIONS_DEFAULT = 1000;

	public static final int BULK_ACTIONS_SLEEP_THRESHOLD_DEFAULT = 400;

	public static final String BULK_ACTIONS_SLEEP_THRESHOLD = "workflow.action.bulk.sleep";

	private static final int BULK_ACTIONS_CONTENTLET_FETCH_STEP_DEFAULT = 350;

	private static final String BULK_ACTIONS_CONTENTLET_FETCH_STEP = "workflow.action.bulk.fetch.step";

	private static final String LICENSE_REQUIRED_MESSAGE_KEY = "Workflow-Schemes-License-required";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WorkflowAPIImpl() {

		actionletClasses = new ArrayList<>();

      // Add default actionlet classes
      actionletClasses.addAll(Arrays.asList(
              CommentOnWorkflowActionlet.class,
              NotifyUsersActionlet.class,
              ArchiveContentActionlet.class,
              DeleteContentActionlet.class,
              DestroyContentActionlet.class,
              CheckinContentActionlet.class,
              CheckoutContentActionlet.class,
              UnpublishContentActionlet.class,
              PublishContentActionlet.class,
              NotifyAssigneeActionlet.class,
              UnarchiveContentActionlet.class,
              ResetTaskActionlet.class,
              ResetPermissionsActionlet.class,
              MultipleApproverActionlet.class,
              FourEyeApproverActionlet.class,
              PushPublishActionlet.class,
              CheckURLAccessibilityActionlet.class,
              EmailActionlet.class,
              AsyncEmailActionlet.class,
              SetValueActionlet.class,
              ReindexContentActionlet.class,
              PushNowActionlet.class,
              TranslationActionlet.class,
              SaveContentActionlet.class,
              SaveContentAsDraftActionlet.class,
              CopyActionlet.class,
              MessageActionlet.class,
              VelocityScriptActionlet.class,
              JsScriptActionlet.class,
              LargeMessageActionlet.class,
              SendFormEmailActionlet.class,
              ResetApproversActionlet.class,
              RekognitionActionlet.class,
              MoveContentActionlet.class,
              DotEmbeddingsActionlet.class,
              OpenAIContentPromptActionlet.class,
              OpenAIGenerateImageActionlet.class,
              OpenAIAutoTagActionlet.class,
              AnalyticsFireUserEventActionlet.class,
              OpenAIVisionAutoTagActionlet.class,
              OpenAITranslationActionlet.class
      ));

		refreshWorkFlowActionletMap();
		registerBundleService();

		try {
			workflowActionUtils = new WorkflowActionUtils();
		} catch (DotDataException e) {
			throw new DotRuntimeException(e);
		}

		this.localSystemEventsAPI.subscribe(this);
	}

	@Subscriber
	@WrapInTransaction
	public void onDeleteContentType (final ContentTypeDeletedEvent contentTypeDeletedEvent) {

		try {

			Logger.debug(this, ()-> "Deleting system mapping actions associated to the content type: "
					+ contentTypeDeletedEvent.getContentTypeVar() );

			this.workFlowFactory.deleteSystemActionsByContentType(contentTypeDeletedEvent.getContentTypeVar());
		} catch (final DotDataException e) {
            Logger.error(this, String.format("Cannot delete system mapping actions associated to Content Type '%s': " +
                    "%s", contentTypeDeletedEvent.getContentTypeVar(), e.getMessage()), e);
        }
	}

	@Subscriber
	@WrapInTransaction
	public void onLanguageDeletedEvent (final LanguageDeletedEvent languageDeletedEvent) {

		try {

			Logger.debug(this, ()-> "Deleting workflow tasks associated to the language: "
					+ languageDeletedEvent.getLanguage() );

			this.workFlowFactory.deleteWorkflowTaskByLanguage(languageDeletedEvent.getLanguage());
		} catch (final DotDataException e) {
            Logger.error(this, String.format("Cannot delete workflow tasks associated to language '%s': %s",
                    languageDeletedEvent.getLanguage(), e.getMessage()), e);
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
							new FriendClass("com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil",
									"com.dotcms.content.elasticsearch.business.ESMappingAPIImpl");
				}
			}
		}

		return this.friendClass;
	}

	public void registerBundleService () {

		// force init if not already done
		OSGIUtil.getInstance().initializeFramework();
		if (System.getProperty(WebKeys.OSGI_ENABLED) == null) {
			// OSGI is not inited
			throw new DotRuntimeException("Unable to register WorkflowAPIOsgiService as OSGI is not inited");
		}

		BundleContext context = HostActivator.instance().getBundleContext();
		if (context == null) {
			throw new DotRuntimeException("Bundle Context is null, WorkflowAPIOsgiService has been not registered");
		}
		if (context.getServiceReference(WorkflowAPIOsgiService.class.getName()) == null) {
			Hashtable<String, String> props = new Hashtable<>();
			context.registerService(WorkflowAPIOsgiService.class.getName(), this, props);
		}

	}

	/**
	 * Converts the shortyId to long id
	 * @param shortyId String
	 * @return String id
	 */
	private String getLongId (final String shortyId, final ShortyIdAPI.ShortyInputType type) {

		//  it is already long
		if (null != shortyId && shortyId.length() == 36) {

			return shortyId;
		}

		final Optional<ShortyId> shortyIdOptional =
				this.shortyIdAPI.getShorty(shortyId, type);

		return shortyIdOptional.isPresent()?
				shortyIdOptional.get().longId:shortyId;
	} // getLongId.

	private String getLongIdForScheme(final String schemeId) {
		return this.getLongId(schemeId, ShortyIdAPI.ShortyInputType.WORKFLOW_SCHEME);
	}


	@CloseDBIfOpened
    @Override
    public void isUserAllowToModifiedWorkflow(final User user) {

        // if the class calling the workflow api is not friend, so checks the validation
        if (!this.getFriendClass().isFriend()) {
            if (!hasValidLicense()) {
                throw new InvalidLicenseException(LICENSE_REQUIRED_MESSAGE_KEY);
            }

            boolean hasAccessToPortlet = false;

            try {
                hasAccessToPortlet = (APILocator.getLayoutAPI()
                        .doesUserHaveAccessToPortlet("workflow-schemes", user));
            } catch (final DotDataException e) {
                Logger.error(this, String.format("Unable to verify access to portlet 'Workflow Schemes' for user ID " +
                        "'%s': %s", user.getUserId(), e.getMessage()), e);
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

    private Class<? extends WorkFlowActionlet> getActionletClass(String className) {
        for ( Class<? extends WorkFlowActionlet> z : actionletClasses ) {
            if ( z.getName().equals(className.trim())) {
                return z;
            }
        }
        return null;
    }

	@Override
	public String addActionlet(final Class<? extends WorkFlowActionlet> workFlowActionletClass) {

		Logger.debug(this,
				() -> "Adding actionlet class: " + workFlowActionletClass);

        //Prevent dupes
        removeActionlet(workFlowActionletClass);
		actionletClasses.add(workFlowActionletClass);
		refreshWorkFlowActionletMap();
		return workFlowActionletClass.getCanonicalName();
	}

	@Override
	public void removeActionlet(final String workFlowActionletName) {

		Logger.debug(this,
				() -> "Removing actionlet: " + workFlowActionletName);

		final WorkFlowActionlet actionlet = actionletMap.get(workFlowActionletName);
		removeActionlet(actionlet.getClass());
		refreshWorkFlowActionletMap();

		try {
		    final User user = APILocator.systemUser();
			final List<WorkflowActionClass> actionClasses = findActionClassesByClassName(actionlet.getActionClass());
            for(final WorkflowActionClass clazz:actionClasses) {
				deleteActionClass(clazz, user);
			}
		} catch (final Exception e) {
		    Logger.error(WorkflowAPIImpl.class,String.format("Error removing Actionlet with className '%s'", workFlowActionletName), e);
			throw new DotRuntimeException(e);
		}
	}

	/**
	 * This method applies an additional "remove-code" in case the first direct remove attempt reports to have failed.
	 * The reason is that the same class could have been loaded from different ClassLoaders (When they come from OSGI)
	 * If removing the class directly from the class instance fails then we look it up by name.
	 * @param workFlowActionletClass
	 */
	private void removeActionlet(final Class<? extends WorkFlowActionlet> workFlowActionletClass) {
		final boolean found = actionletClasses.remove(workFlowActionletClass);
		if (!found) {
			final String canonicalName = workFlowActionletClass.getCanonicalName();
			final Optional<Class<? extends WorkFlowActionlet>> optionalClass = actionletClasses
					.stream().filter(s -> s.getCanonicalName().equals(canonicalName))
					.findFirst();
			optionalClass.ifPresent(actionletClasses::remove);
		}
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

		return this.findStepsByContentlet(contentlet, true);
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowStep> findStepsByContentlet(final Contentlet contentlet, final boolean showArchive) throws DotDataException{

		final List<WorkflowScheme> schemes = hasValidLicense() ?
				workFlowFactory.findSchemesForStruct(contentlet.getContentTypeId()) :
				Arrays.asList(workFlowFactory.findSystemWorkflow()) ;

		final List<WorkflowStep> steps =
				this.workFlowFactory.findStepsByContentlet(contentlet, schemes);

		if (!showArchive) {

			final Set<String> nonArchiveSchemeIdSet =
					schemes.stream().filter(scheme -> !scheme.isArchived())
						    .map(scheme -> scheme.getId())
							.collect(Collectors.toSet());

			return steps.stream().filter(step -> nonArchiveSchemeIdSet.contains(step.getSchemeId()))
					.collect(CollectionsUtils.toImmutableList());
		}

		return steps;
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
	@CloseDBIfOpened
	public Optional<WorkflowStep> findCurrentStep(final Contentlet contentlet) throws DotDataException {

		final WorkflowTask task = findTaskByContentlet(contentlet);
		if(task != null && task.getId() != null && null != task.getStatus()) {
			final WorkflowStep step = findStep(task.getStatus());
			return Optional.of(step);
		}

		return Optional.empty();
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
	public WorkflowScheme findScheme(final String idOrVar) throws DotDataException, DotSecurityException {

		final String schemeIdOrVar = this.getLongId(idOrVar, ShortyIdAPI.ShortyInputType.WORKFLOW_SCHEME);

		validateWorkflowLicense(schemeIdOrVar, LICENSE_REQUIRED_MESSAGE_KEY);

		return workFlowFactory.findScheme(schemeIdOrVar);
	}

	@Override
	@WrapInTransaction
	public void saveSchemesForStruct(final Structure contentType,
									 final List<WorkflowScheme> schemes) throws DotDataException {

		try {

			Logger.debug(this, ()-> "Saving schemes: " + schemes +
									", to the content type: " + contentType);
            SecurityLogger.logInfo(this.getClass(), () -> String.format("Saving schemes [ %s ] for Content Type [ " +
                    "%s ]", schemes, contentType));

			this.workFlowFactory.saveSchemesForStruct(contentType.getInode(), schemes,
					this::consumeWorkflowTask);
        } catch (final DotDataException e) {
            Logger.error(WorkflowAPIImpl.class, String.format("Error saving Schemas for Content type '%s': %s",
                    contentType.getInode(), e.getMessage()));
            throw e;
		}
	}

	@Override
	@WrapInTransaction
	public void saveSchemeIdsForContentType(final ContentType contentType,
											final Set<String> schemesIds) throws DotDataException {

		try {

			Logger.info(WorkflowAPIImpl.class, String.format("Saving Schemas [ %s ] for Content type '%s'",
					String.join(",", schemesIds), contentType.inode()));
			SecurityLogger.logInfo(this.getClass(), ()-> String.format("Saving Schemas [ %s ] for Content type '%s'",
					String.join(",", schemesIds), contentType.inode()));

			workFlowFactory.saveSchemeIdsForContentType(contentType.inode(),
					schemesIds.stream().map(this::getLongIdForScheme).collect(Collectors.toSet()),
					this::consumeWorkflowTask);
			if(schemesIds.isEmpty()){
				contentTypeAPI.updateModDate(contentType);
			}

			this.cleanInvalidDefaultActionForContentType(contentType, schemesIds);
        } catch (final DotDataException | DotSecurityException e) {

			Logger.error(WorkflowAPIImpl.class, String.format("Error saving Schemas [ %s ] for Content Type '%s': %s",
					String.join(",", schemesIds), contentType.inode(), e.getMessage()));
		}
	}

	private void cleanInvalidDefaultActionForContentType(final ContentType contentType,
			final Set<String> schemesIds) throws DotDataException, DotSecurityException {

		if (UtilMethods.isSet(schemesIds)) {

			final List<Map<String, Object>> mappings = this.workFlowFactory
					.findSystemActionsByContentType(contentType);
			if (UtilMethods.isSet(mappings)) {

				for (final Map<String, Object> mappingRow : mappings) {

					final SystemActionWorkflowActionMapping mapping =
							this.toSystemActionWorkflowActionMapping(mappingRow, contentType, APILocator.systemUser());
					if (UtilMethods.isSet(mapping) && UtilMethods.isSet(mapping.getWorkflowAction())) {

						if (!schemesIds.contains(mapping.getWorkflowAction().getSchemeId())) {

                            Logger.info(this, String.format("Removing invalid system default action [ %s ] on content" +
                                            " type '%s'. The Scheme ID '%s' is no longer valid on the content type schemes [ %s ]",
                                    mapping.getWorkflowAction(), contentType.variable(), mapping.getWorkflowAction()
                                            .getSchemeId(), schemesIds));
                            this.workFlowFactory.deleteSystemAction(mapping);
						}
					}
				}
			}
		} else {

			// no scheme remove all content type default actions.
			this.workFlowFactory.deleteSystemActionsByContentType(contentType.variable());
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

		validateWorkflowLicense(workflowScheme.getId(), LICENSE_REQUIRED_MESSAGE_KEY);
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
	public void saveScheme(final WorkflowScheme scheme, final User user) throws DotDataException, DotSecurityException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		if(null == scheme){
		   throw new DotWorkflowException("Workflow-delete-null-workflow");
		}

		if (UtilMethods.isSet(scheme.getId())) {
			scheme.setId(this.getLongIdForScheme(scheme.getId()));
		}

		if (SYSTEM_WORKFLOW_ID.equals(scheme.getId()) && scheme.isArchived()) {

			Logger.warn(this, "Can not archive the system workflow");
			throw new DotSecurityException("Workflow-cannot-archive-system-workflow");
		}

		workFlowFactory.saveScheme(scheme);

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Workflow Scheme '%s' [%s] has been saved by User" +
                " ID '%s'", scheme.getName(), scheme.getId(), user.getUserId()));
    }

	@CloseDBIfOpened
	public List<WorkflowScheme> findArchivedSchemes() throws DotDataException{
		return workFlowFactory.findArchivedSchemes();
	}

	/**
	 * This method collects all the identifiers of contents that reference the workflow and then pushes such ids into the 'distributed index journal' queue
	 * @param scheme
	 * @param user
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@CloseDBIfOpened
	int pushIndexUpdate(final WorkflowScheme scheme, final User user) throws  DotDataException, DotSecurityException {
		final String schemeId = scheme.getId();
		final String luceneQuery = String.format(" +wfscheme:( %s ) +working:true ", schemeId);

		//We should only be considering Working content.
		final List<ContentletSearch> searchResults = contentletAPI.searchIndex(luceneQuery, -1, 0, null, user, RESPECT_FRONTEND_ROLES);
		final Set<String> identifiers = searchResults.stream().map(ContentletSearch::getIdentifier).collect(Collectors.toSet());
		return reindexQueueAPI.addIdentifierReindex(identifiers);
	}

	@Override
	public Future<WorkflowScheme> deleteScheme(final WorkflowScheme scheme, final User user)
			throws DotDataException, DotSecurityException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);

		if (null == scheme){
			Logger.warn(this, "Workflow Scheme cannot be null");
			throw new DotWorkflowException("Workflow-delete-null-workflow");
		}

		if( SYSTEM_WORKFLOW_ID.equals(scheme.getId())) {

            Logger.warn(this, String.format("Workflow Scheme '%s' [%s] could not be deleted", scheme.getName(),
                    scheme.getId()));
            throw new DotSecurityException("Workflow-delete-system-workflow");
		}

		if(!scheme.isArchived()){
			throw new DotSecurityException("Workflow-delete-non-archived");
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
	@VisibleForTesting
	@Override
	public WorkflowScheme deleteSchemeTask(final WorkflowScheme scheme, final User user) {

		try {

			final StopWatch stopWatch = new StopWatch();
			stopWatch.start();
            Logger.info(this, String.format("Deleting Workflow Scheme '%s' [%s]", scheme.getName(), scheme.getId()));

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
            SecurityLogger.logInfo(this.getClass(), String.format("Workflow Scheme '%s' [%s] has been deleted",
                    scheme.getName(), scheme.getId()));

			stopWatch.stop();
			Logger.info(this, "Delete Workflow Scheme task has finished. Duration: " +
					DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");

			//Update index
			pushIndexUpdate(scheme, user);

			this.systemMessageEventUtil.pushSimpleTextEvent
					(LanguageUtil.get(user.getLocale(), "Workflow-deleted", scheme.getName()), user.getUserId());

            SecurityLogger.logInfo(this.getClass(), () -> String.format("Worklow Scheme '%s' [%s] has been deleted by" +
                    " User ID '%s'", scheme.getName(), scheme.getId(), user.getUserId()));
        } catch (final Exception e) {
            Logger.error(this.getClass(), String.format("Error deleting Workflow Scheme '%s' [%s]: %s", scheme
                    .getName(), scheme.getId(), e.getMessage()), e);
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
	@WrapInTransaction
	private void deleteWorkflowStepWrapper(final WorkflowStep step, final User user)
			throws DotRuntimeException {
		try {
			//delete step
			this.doDeleteStep(step, user, false);
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

	@CloseDBIfOpened
	public Optional<WorkflowStep> findFirstStepForAction(final WorkflowAction workflowAction) throws DotDataException {

		return workFlowFactory.findFirstStep (workflowAction.getId(), workflowAction.getSchemeId());
	}

	@CloseDBIfOpened
	public Optional<WorkflowStep> findFirstStep(final String schemeId) throws DotDataException {

		DotPreconditions.isTrue(UtilMethods.isSet(schemeId),
				()-> "Scheme is required", DotStateException.class);

		return workFlowFactory.findFirstStep (this.getLongIdForScheme(schemeId));
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

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Workflow Step '%s' [%s] has been saved by User ID" +
                " '%s'", step.getName(), step.getId(), user.getUserId()));
    }

	@CloseDBIfOpened
	public Future<WorkflowStep> deleteStepHardMode(final WorkflowStep step, final User user)
			throws DotDataException {

		return this.doDeleteStep(step, user, false, true);
	}

	@CloseDBIfOpened
	public Future<WorkflowStep> deleteStep(final WorkflowStep step, final User user)
			throws DotDataException {

		return this.doDeleteStep(step, user, true, false); // runs async
	}

	private Future<WorkflowStep> doDeleteStep(final WorkflowStep step, final User user,
			final boolean async) throws DotDataException {
		return doDeleteStep(step, user, async, false);
	}

	/**
	 * Deletes a given step with all dependencies: actions, actionlets and tasks.
	 *
	 * @param step Workflow step to delte
	 * @param user To who perform the delete operation
	 * @param async If the delete should run in separated thread
	 * @param hardMode If validations should be skipped. <strong>Note:</strong> Use this parameter
	 * with caution, was created for hard deletes from Push publish avoiding all validations.
	 */
	private Future<WorkflowStep> doDeleteStep(final WorkflowStep step, final User user,
			final boolean async, final boolean hardMode) throws DotDataException {

		this.isUserAllowToModifiedWorkflow(user);

		try {

			if (!hardMode) {
				// Checking for Next Step references
				for (final WorkflowStep otherStep : findSteps(findScheme(step.getSchemeId()))) {

					/*
					Verify we are not validating the next step is the step we want to delete.
					Remember the step can point to itself and that should not be a restriction when deleting.
					 */
					if (!otherStep.getId().equals(step.getId())) {
						for (final WorkflowAction action : findActions(otherStep,
								APILocator.getUserAPI().getSystemUser())) {

							if (action.getNextStep().equals(step.getId())) {
								final String validationExceptionMessage = LanguageUtil
										.format(user.getLocale(),
												"Workflow-delete-step-reference-by-step-error",
												new String[]{step.getName(), otherStep.getName(),
														action.getName()}, false);
								throw new DotDataValidationException(validationExceptionMessage);
							}
						}
					}
				}
			}

			if (!hardMode) {
				final int countContentletsReferencingStep = getCountContentletsReferencingStep(
						step);
				if (countContentletsReferencingStep > 0) {

					final String validationExceptionMessage = LanguageUtil.format(user.getLocale(),
							"Workflow-delete-step-reference-by-contentlet-error",
							new String[]{step.getName(),
									Integer.toString(countContentletsReferencingStep)}, false);
					throw new DotDataValidationException(validationExceptionMessage);
				}
			}

			final DotSubmitter submitter = this.concurrentFactory
					.getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
			//Delete the Scheme in a separated thread
			return (async) ?
					submitter.submit(() -> deleteStepTask(step, user, true)) :
					ConcurrentUtils.constantFuture(deleteStepTask(step, user, false));
        } catch (final Exception e) {
            Logger.error(this.getClass(), String.format("An error occurred when deleting Workflow Step '%s' [%s]: " +
                    "%s", step.getName(), step.getId(), e.getMessage()), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

	private void consumeWorkflowTask (final WorkflowTask workflowTask) {

		try {
			HibernateUtil.addCommitListener( () -> {
				try {
					this.reindexQueueAPI.addIdentifierReindex(workflowTask.getWebasset());
				} catch (DotDataException e) {
					Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
				}
			});
        } catch (final DotHibernateException e) {
            Logger.error(WorkflowAPIImpl.class, String.format("An error occurred on Commit Listener when adding " +
                    "content ID '%s' to ES index: %s", workflowTask.getWebasset(), e.getMessage()), e);
        }
    }

	@WrapInTransaction
	private WorkflowStep deleteStepTask(final WorkflowStep step, final User user, final boolean sendSystemEvent) {

		try {

			final StopWatch stopWatch = new StopWatch();
			stopWatch.start();

			Logger.info(this, String.format("Deleting Workflow Step '%s' [%s]", step.getName(), step.getId()));

			this.workFlowFactory.deleteActions(step); // workflow_action_step
			this.workFlowFactory.deleteStep(step, this::consumeWorkflowTask); // workflow_step
            SecurityLogger.logInfo(this.getClass(), String.format("Workflow Step '%s' [%s] has been deleted",
                    step.getName(), step.getId()));

			stopWatch.stop();
			Logger.info(this, "Delete Workflow Step task has finished. Duration: " +
					DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");

			if (sendSystemEvent) {
				HibernateUtil.addCommitListener(() -> {
					try {
						this.systemMessageEventUtil.pushSimpleTextEvent
								(LanguageUtil.get(user.getLocale(), "Workflow-Step-deleted", step.getName()), user.getUserId());
					} catch (LanguageException e1) {
						Logger.error(this.getClass(), e1.getMessage(), e1);
					}
				});
			}

            SecurityLogger.logInfo(this.getClass(), String.format("Workflow Step '%s' [%s] has been deleted by User " +
                    "ID '%s'", step.getName(), step.getId(), user.getUserId()));
        } catch (final Exception e) {
			HibernateUtil.addRollbackListener(() -> {
				try {
					this.systemMessageEventUtil.pushSimpleErrorEvent(new ErrorEntity("Workflow-delete-step-error",
							LanguageUtil.get(user.getLocale(), "Workflow-delete-step-error", step.getName())), user.getUserId());
				} catch (LanguageException e1) {
					Logger.error(this.getClass(), e1.getMessage(), e1);
				}
			});
            Logger.error(this.getClass(), String.format("An error occurred when deleting Workflow Step '%s' [%s]: " +
                    "%s", step.getName(), step.getId(), e.getMessage()), e);

			throw new DotRuntimeException(e);
		}

		return step;
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

		final boolean firstStepChanges = order == 0 ||
				steps.stream().findFirst().map(
						workflowStep -> workflowStep.getId().equals(step.getId())
				).orElse(false);

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

		if (firstStepChanges) {
			final DotSubmitter submitter = this.concurrentFactory
					.getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
			submitter.submit(() -> {
				try {
					pushIndexUpdateOnReorder(scheme.getId());
				} catch (DotDataException e) {
					Logger.error(getClass(),
							"An Error occurred while attempting a reindex on reorder.", e);
				}
			});
		}

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Workflow Step '%s' [%s] has been reordered by " +
                "User ID '%s'", step.getName(), step.getId(), user.getUserId()));
    }

	/**
	 * Once we have determined that a reorder event has affected the first position, we need to update the index.
	 * @param schemeId
	 * @return
	 */
	@CloseDBIfOpened
	private int pushIndexUpdateOnReorder(final String schemeId) throws DotDataException {
       final Set<String> identifiers = workFlowFactory.findNullTaskContentletIdentifiersForScheme(schemeId);
       return reindexQueueAPI.addIdentifierReindex(identifiers);
	}

	@Override
	@WrapInTransaction
	public void deleteComment(final WorkflowComment comment) throws DotDataException {

		this.workFlowFactory.deleteComment(comment);
        SecurityLogger.logInfo(this.getClass(), String.format("Workflow Comment '%s' has been deleted.", comment.getId()));
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
            SecurityLogger.logInfo(this.getClass(), String.format("Workflow Comment '%s' has been saved.", comment.getId()));
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
        SecurityLogger.logInfo(this.getClass(), String.format("Workflow History '%s' has been deleted.", history.getId()));
    }

	@Override
	@WrapInTransaction
	public int deleteWorkflowHistoryOldVersions(final Date olderThan) throws DotDataException {

		return this.workFlowFactory.deleteWorkflowHistoryOldVersions(olderThan);
	}

	@Override
	@WrapInTransaction
	public void saveWorkflowHistory(final WorkflowHistory history) throws DotDataException {

		this.workFlowFactory.saveWorkflowHistory(history);
        SecurityLogger.logInfo(this.getClass(), String.format("Workflow History '%s' has been saved.", history.getId()));
	}

	@Override
	@WrapInTransaction
	public void deleteWorkflowTask(final WorkflowTask task, final User user)
			throws DotDataException {

		this.workFlowFactory.deleteWorkflowTask(task);
		SecurityLogger.logInfo(this.getClass(), String.format("Workflow Task '%s' has been deleted.", task.getId()));
		try {
			if (UtilMethods.isSet(task.getWebasset())) {
				HibernateUtil.addCommitListener(() -> {
					try {
						this.reindexQueueAPI.addIdentifierReindex(task.getWebasset());
					} catch (final DotDataException e) {
                        Logger.error(WorkflowAPIImpl.class, String.format("An error occurred when reindexing webasset" +
                                " '%s' on Commit Listener for  Workflow Task '%s': %s", task.getWebasset(), task.getId(), e
                                .getMessage()), e);

                    }
				});
			}
		} catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when deleting Workflow Task '%s': %s", task.getId(),
                    e.getMessage()), e);
        }

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Workflow Task '%s' has been deleted by User ID " +
                "'%s'", task.getId(), user.getUserId()));
    }

	@Override
	@WrapInTransaction
	public void deleteWorkflowTaskByContentletIdAnyLanguage(final Contentlet contentlet, final User user) throws DotDataException {

        SecurityLogger.logInfo(this.getClass(), String.format("Removing Workflow Tasks by contentlet '%s' in any " +
                "language", contentlet.getIdentifier()));

		if(contentlet==null || !UtilMethods.isSet(contentlet.getIdentifier())) {
			return;
		}

		this.workFlowFactory.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet.getIdentifier());

		try {
			HibernateUtil.addCommitListener(() -> {
				try {
					this.contentletIndexAPI.addContentToIndex(contentlet);
				} catch (final DotDataException e) {
                    Logger.error(WorkflowAPIImpl.class, String.format("An error occurred when reindexing Contentlet " +
                                    "with ID '%s' / Inode '%s' on Commit Listener in any language: %s", contentlet.getIdentifier(),
                            contentlet.getInode(), e.getMessage()), e);
                }
			});
		} catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when deleting Workflow Tasks by Contentlet with ID " +
                    "'%s' / Inode '%s' in any language: %s", contentlet.getIdentifier(), contentlet.getInode(), e.getMessage
                    ()), e);
        }

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Workflow Tasks by contentlet '%s' in any " +
                "language have been deleted by User ID '%s'", contentlet.getIdentifier(), user.getUserId()));
    }

	@Override
	@WrapInTransaction
	public void deleteWorkflowTaskByContentlet(final Contentlet contentlet, final long languageId, final User user) throws DotDataException {

        SecurityLogger.logInfo(this.getClass(), String.format("Removing Workflow Tasks by contentlet '%s' in lang " +
                "'%s'", contentlet.getIdentifier(), languageId));

		if(contentlet==null || !UtilMethods.isSet(contentlet.getIdentifier())) {
			return;
		}

		this.workFlowFactory.deleteWorkflowTaskByContentletIdAndLanguage(contentlet.getIdentifier(), languageId);

		try {
			HibernateUtil.addCommitListener(() -> {
				try {
					this.contentletIndexAPI.addContentToIndex(contentlet);
				} catch (final DotDataException e) {
					Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
                    Logger.error(WorkflowAPIImpl.class, String.format("An error occurred when reindexing Contentlet " +
                                    "with ID '%s' / Inode '%s' on Commit Listener in lang '%s': %s", contentlet.getIdentifier(),
                            contentlet.getInode(), languageId, e.getMessage()), e);
				}
			});
		} catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when deleting Workflow Tasks by Contentlet with ID " +
                    "'%s' / Inode '%s' in lang '%s': %s", contentlet.getIdentifier(), contentlet.getInode(), languageId, e
                    .getMessage()), e);
        }

        SecurityLogger.logInfo(this.getClass(), () -> String.format("Workflow Tasks by contentlet '%s' in lang '%s' " +
                "have been deleted by User ID '%s'", contentlet.getIdentifier(), languageId, user.getUserId()));
    }

	@CloseDBIfOpened
	public WorkflowTask findWorkFlowTaskById(final String id) throws DotDataException {
		return workFlowFactory.findWorkFlowTaskById(id);
	}

	@Override
	@CloseDBIfOpened
	public List<IFileAsset> findWorkflowTaskFilesAsContent(final WorkflowTask task, final User user) throws DotDataException{

		final List<Contentlet> contents =  workFlowFactory.findWorkflowTaskFilesAsContent(task, user);
		return APILocator.getFileAssetAPI().fromContentletsI(contents);
	}

	@Override
	@WrapInTransaction
	public void saveWorkflowTask(final WorkflowTask task) throws DotDataException {

		if (task.getLanguageId() <= 0) {

			Logger.warn(this, "Workflow Task: '" + task.getId() +
								"' doesn't have any language ID. Setting the default one");
			task.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		this.workFlowFactory.saveWorkflowTask(task);
		try {
			if (UtilMethods.isSet(task.getWebasset())) {
				HibernateUtil.addCommitListener(() -> {
					try {
						this.reindexQueueAPI.addIdentifierReindex(task.getWebasset());
					} catch (DotDataException e) {
						Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
					}
				});
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}

		SecurityLogger.logInfo(this.getClass(),
				"Workflow task with id: '" + task.getId() + "' has been saved.");
	}

	@Override
	@WrapInTransaction
	public void saveWorkflowTask(final WorkflowTask task, final WorkflowProcessor processor) throws DotDataException {

		this.saveWorkflowTaskInternal(task, processor, true);
	}

	private void saveWorkflowTaskInternal(final WorkflowTask task, final WorkflowProcessor processor, final boolean doIndex) throws DotDataException {

		if (doIndex) {
			this.saveWorkflowTask(task);
		} else {
			this.saveWorkflowTaskWithoutIndexing(task);
		}

		final WorkflowHistory history = new WorkflowHistory();
		history.setWorkflowtaskId(task.getId());
		history.setActionId(processor.getAction().getId());
		history.setCreationDate(new Date());
		history.setMadeBy(processor.getUser().getUserId());
		history.setStepId(processor.getNextStep().getId());

		final String comment = (UtilMethods.isSet(processor.getWorkflowMessage()))? processor.getWorkflowMessage()   : StringPool.BLANK;
		final String nextAssignName = (UtilMethods.isSet(processor.getNextAssign()))? processor.getNextAssign().getName() : StringPool.BLANK;

		try {

			String description = LanguageUtil.format(processor.getUser().getLocale(), "workflow.history.description", new String[]{
					processor.getUser().getFullName(),
					processor.getAction().getName(),
					processor.getNextStep().getName(),
					nextAssignName,
					comment}, false);

			if ( processor.getContextMap().containsKey("type") && WorkflowHistoryType.APPROVAL == processor.getContextMap().get("type")) {
				description = "{\"description\":\""+ description +
						"\", \"type\":\"" + WorkflowHistoryType.APPROVAL.name() +
						"\", \"state\":\""+  WorkflowHistoryState.NONE.name() +"\" }";
			}

			history.setChangeDescription(description);
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
		SecurityLogger.logInfo(this.getClass(),
				"File id: '" + fileInode + "' has been attached to task: " + task.getId());
	}

	@Override
	@WrapInTransaction
	public void removeAttachedFile(final WorkflowTask task, final String fileInode) throws DotDataException {
		workFlowFactory.removeAttachedFile(task, fileInode);
		SecurityLogger.logInfo(this.getClass(),
				"File id: '" + fileInode + "' has been removed from task: " + task.getId());
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
	public List<WorkflowAction> findBulkActions(final WorkflowStep step, final User user)
			throws DotDataException, DotSecurityException {

		if (null == step) {
			return Collections.emptyList();
		}
		final List<WorkflowAction> actions = workFlowFactory.findActions(step);
		return workflowActionUtils
				.filterBulkActions(actions, user, RESPECT_FRONTEND_ROLES);
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
		throws DotDataException, DotSecurityException {

		validateWorkflowLicense(scheme.getId(), "Workflow-Actions-License-required");

		final List<WorkflowAction> actions = workFlowFactory.findActions(scheme);
		return permissionAPI.filterCollection(actions,
				PermissionAPI.PERMISSION_USE, RESPECT_FRONTEND_ROLES, user);
	} // findActions.

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowScheme scheme, final User user, final Contentlet contentlet)
		throws DotDataException, DotSecurityException {

		validateWorkflowLicense(scheme.getId(), "Workflow-Actions-License-required");

		return permissionAPI.filterCollection(
			workFlowFactory.findActions(scheme),
			PermissionAPI.PERMISSION_USE,
			RESPECT_FRONTEND_ROLES,
			user,
			contentlet);
	} // findActions.

	private void validateWorkflowLicense(String scheme, String message) {
		if (!SYSTEM_WORKFLOW_ID.equals(scheme) && !SYSTEM_WORKFLOW_VARIABLE_NAME.equals(scheme)
				&& (!hasValidLicense() && !this.getFriendClass().isFriend())) {
			throw new InvalidLicenseException(message);
		}
	}

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
		for(final WorkflowStep step : steps) {
			actions.addAll(workFlowFactory.findActions(step));
		}

		return this.fillActionsInfo(this.workflowActionUtils
                .filterActions(actions.build(), user, PageMode.get().respectAnonPerms, permissionable)) ;
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findActions(final WorkflowScheme scheme, final User user, final
	Predicate<WorkflowAction> actionFilter) throws DotDataException, DotSecurityException {
		final List<WorkflowAction> unfilteredActions = this.findActions(scheme, user);
		return unfilteredActions.stream().filter(actionFilter).collect(Collectors.toList());
	}

	private List<WorkflowAction> fillActionsInfo(final List<WorkflowAction> workflowActions) throws DotDataException {

	    for (final WorkflowAction action : workflowActions) {

	        this.fillActionInfo(action, this.workFlowFactory.findActionClasses(action));
        }

        return workflowActions;
    }

	/*
	 * This method will fill the action info based on the action classes
	 * @param action
	 * @param actionClasses
	 */
	@Override
    public void fillActionInfo(final WorkflowAction action,
                                final List<WorkflowActionClass> actionClasses) {

	    boolean isSave        = false;
	    boolean isPublish     = false;
		boolean isUnPublish   = false;
		boolean isArchive     = false;
		boolean isUnArchive   = false;
		boolean isDelete      = false;
		boolean isDestroy     = false;
        boolean isPushPublish = false;
		boolean isMove        = false;
		boolean isMoveHasPath = false;
		boolean isComment     = false;
		boolean isReset       = false;

        for (final WorkflowActionClass actionClass : actionClasses) {

            final Actionlet actionlet = AnnotationUtils.
                    getBeanAnnotation(this.getActionletClass(actionClass.getClazz()), Actionlet.class);

                isSave        |= (null != actionlet) && actionlet.save();
                isPublish     |= (null != actionlet) && actionlet.publish();
			    isUnPublish   |= (null != actionlet) && actionlet.unpublish();
			    isArchive     |= (null != actionlet) && actionlet.archive();
			    isUnArchive   |= (null != actionlet) && actionlet.unarchive();
			    isDelete      |= (null != actionlet) && actionlet.delete();
			    isDestroy     |= (null != actionlet) && actionlet.destroy();
                isPushPublish |= (null != actionlet) && actionlet.pushPublish();
			    isComment     |= (null != actionlet) && actionlet.comment();
				isReset       |= (null != actionlet) && actionlet.reset();

			/*
			 * In order to determine if an action is moveable, it needs to have a MoveContentActionlet assigned AND
			 * their parameter MoveContentActionlet#PATH_KEY should be not set (b/c if it is, the path is already hardcored and does not need to ask for it)
			 */
			if (actionClass.getClazz().equals(MoveContentActionlet.class.getName())) {

				final Map<String, WorkflowActionClassParameter> workflowActionClassParameterMap =
						Try.of(() -> this.findParamsForActionClass(actionClass)).getOrNull();

				isMove         = true;
				isMoveHasPath |= UtilMethods.isSet(workflowActionClassParameterMap) &&
						UtilMethods.isSet(workflowActionClassParameterMap.get(MoveContentActionlet.PATH_KEY).getValue());
			}
		}

	    action.setSaveActionlet(isSave);
        action.setPublishActionlet(isPublish);
		action.setUnpublishActionlet(isUnPublish);
		action.setArchiveActionlet(isArchive);
		action.setUnarchiveActionlet(isUnArchive);
		action.setDeleteActionlet(isDelete);
		action.setDestroyActionlet(isDestroy);
        action.setPushPublishActionlet(isPushPublish);
        action.setMoveActionlet(isMove);
        action.setMoveActionletHashPath(isMoveHasPath);
		action.setCommentActionlet(isComment);
		action.setResetable(isReset);
    }


    @Override
    @CloseDBIfOpened
    public List<WorkflowAction> findAvailableActionsEditing(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {

        return this.findAvailableActions(contentlet, user, RenderMode.EDITING);
    }

	 /**
     * This method will return the list of workflows actions available to a user on any give
     * piece of content, based on how and who has the content locked and what workflow step the content
     * is in
     */
    @Override
    @CloseDBIfOpened
    public List<WorkflowAction> findAvailableActionsListing(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {

		return this.findAvailableActions(contentlet, user, RenderMode.LISTING);
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

		return this.findAvailableActions(contentlet, user, RenderMode.EDITING);
	}

	@Override
	@CloseDBIfOpened
	public boolean isActionAvailable(final Contentlet contentlet, final User user, final String actionId) {

		final Set<WorkflowAction> workflowActions = new HashSet<>();

		try {

			if (contentlet == null || contentlet.getContentType() == null) {

				Logger.debug(this, () -> "Contentlet: " + contentlet + " or its Content Type is null");
				throw new DotStateException("Content or Content Type is null");
			}

			final boolean isNew  			= !UtilMethods.isSet(contentlet.getInode());
			final boolean isValidContentlet = isNew || contentlet.isWorking();
			if(Host.HOST_VELOCITY_VAR_NAME.equals(contentlet.getStructure().getVelocityVarName()) || !isValidContentlet) {

				return false;
			}

			final List<WorkflowStep> steps = findStepsByContentlet(contentlet, false);
			workflowActions.addAll(isNew?
					this.findActions(steps, user, contentlet.getContentType()):
					this.findActions(steps, user, contentlet));

		} catch (final DotDataException | DotSecurityException e) {
            Logger.error(this, String.format("An error occurred when checking Action ID '%s' for Contentlet '%s': " +
                    "%s", actionId, contentlet.getIdentifier(), e.getMessage()), e);
        }

		return UtilMethods.isSet(workflowActions) &&
				workflowActions.stream().anyMatch(workflowAction -> actionId.equals(workflowAction.getId()));
	}

	private List<WorkflowAction> doFilterActions(final ImmutableList.Builder<WorkflowAction> actions,
								 final boolean isNew,
								 final boolean isPublished,
								 final boolean isArchived,
								 final boolean canLock,
								 final boolean isLocked,
								 final RenderMode renderMode,
								 final List<WorkflowAction> unfilteredActions) {

		for (final WorkflowAction workflowAction : unfilteredActions) {

			if (this.workflowStatusFilter.filter(workflowAction,
					new ContentletStateOptions(isNew, isPublished, isArchived,
							canLock(canLock, workflowAction), isLocked, renderMode))) {

            	actions.add(workflowAction);
            }
        }

        return actions.build();
	}

	/**
	 * Figure out the can lock, if the can lock is false, check if the workflow has
	 * 1) the lock action
	 * 2) if forze lock is enabled
	 * if both conditions are meet, then the can lock is true
	 * @param canLock
	 * @param workflowAction
	 * @return boolean
	 */
	private boolean canLock(final boolean canLock, final WorkflowAction workflowAction) {

		if (!canLock && Objects.nonNull(workflowAction)) {

			final List<WorkflowActionClass> actionClasses = Try.of(()->this.findActionClasses(workflowAction)).getOrNull();
			if(Objects.nonNull(actionClasses)) {

				return actionClasses.stream().filter(actionClass -> actionClass.getClazz().equals(CheckinContentActionlet.class.getName()))
						.map(actionClass -> Try.of(()->this.findParamsForActionClass(actionClass)).getOrNull())
						.filter(workflowActionClassParameterMap -> Objects.nonNull(workflowActionClassParameterMap)  &&
								workflowActionClassParameterMap.containsKey(CheckinContentActionlet.FORCE_UNLOCK_ALLOWED))
						.anyMatch(workflowActionClassParameterMap -> "true".equalsIgnoreCase(
								workflowActionClassParameterMap.get(CheckinContentActionlet.FORCE_UNLOCK_ALLOWED).getValue()));
			}
		}

		return canLock;
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

		SecurityLogger.logInfo(this.getClass(),
				"Action id: '" + action.getId() + "' has been reordered by user: " + user.getUserId());
	}

	@Override
	@CloseDBIfOpened
	public WorkflowAction findAction(final String id, final User user) throws DotDataException, DotSecurityException {

		final WorkflowAction workflowAction = this.workFlowFactory.findAction(this.getLongId(id, ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION));
		if(null != workflowAction){
			validateWorkflowLicense(workflowAction.getSchemeId(), "Workflow-Actions-License-required");
		}

		if (null != workflowAction) {
			this.fillActionInfo(workflowAction, this.workFlowFactory.findActionClasses(workflowAction));
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
			validateWorkflowLicense(workflowAction.getSchemeId(), "Workflow-Actions-License-required");
		}

		if (null != workflowAction) {
			this.fillActionInfo(workflowAction, this.workFlowFactory.findActionClasses(workflowAction));
		}

		return workflowAction;
	}

	@Override
	@WrapInTransaction
	public void saveAction(final WorkflowAction action,
						   final List<Permission> permissions,
						   final User user) throws DotDataException {

		DotPreconditions.isTrue(UtilMethods.isSet(action.getSchemeId()) && this.existsScheme(action.getSchemeId()),
				()-> {
					Logger.error(this, String.format("Workflow Scheme '%s' doesn't exist.", action.getSchemeId()));
					return "Workflow-does-not-exists-scheme";
				},
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
		} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when saving Workflow Action '%s' [%s]: %s", action
                    .getName(), action.getId(), e.getMessage());
            Logger.error(WorkflowAPIImpl.class, errorMsg);
			Logger.debug(WorkflowAPIImpl.class, errorMsg, e);
			throw new DotDataException(errorMsg, e);
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

			SecurityLogger.logInfo(this.getClass(),
					"The action id: '" + actionId + "' has been saved by the user: " + user.getUserId());
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

				Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
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


		action.setSchemeId(this.getLongIdForScheme(action.getSchemeId()));
		if (UtilMethods.isSet(action.getId())) {
			action.setId(this.getLongId(action.getId(), ShortyIdAPI.ShortyInputType.WORKFLOW_ACTION));
		}

		workFlowFactory.saveAction(action);

		SecurityLogger.logInfo(this.getClass(),
				"Action id: '" + action.getId() + "' has been reordered by user: " + user.getUserId());
	}

	@Override
	@CloseDBIfOpened
	public WorkflowStep findStep(final String id) throws DotDataException {

		final WorkflowStep step = workFlowFactory.findStep(this.getLongId(id, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
		validateWorkflowLicense(step.getSchemeId(), "You must have a valid license to see any available step.");
		return step;
	}

    @Override
    @CloseDBIfOpened
    public boolean isSystemStep (final String stepId) {

	    boolean isSystemStep;
	    WorkflowStep step;

        try {
            step = this.workFlowFactory.findStep(this.getLongId(stepId, ShortyIdAPI.ShortyInputType.WORKFLOW_STEP));
            isSystemStep = null != step && SYSTEM_WORKFLOW_ID.equals(step.getSchemeId());
        } catch (DotDataException e) {
            isSystemStep = false;
        }

        return isSystemStep;
    } // isSystemStep.

	@Override
	@WrapInTransaction
	public void deleteAction(final WorkflowAction action, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		Logger.debug(this,
				() -> "Removing the WorkflowAction: " + action.getId() + ", name: " + action
						.getName());

		final List<WorkflowActionClass> workflowActionClasses =
				findActionClasses(action);

		Logger.debug(this,
				() -> "Removing the WorkflowActionClass, for action: " + action.getId() + ", name: "
						+ action.getName());

		if(workflowActionClasses != null && workflowActionClasses.size() > 0) {
			for(final WorkflowActionClass actionClass : workflowActionClasses) {
				this.deleteActionClass(actionClass, user);
			}
		}

		Logger.debug(this,
				() -> "Removing the System Action Mappings, for action: " + action.getId() + ", name: "
						+ action.getName());
		this.workFlowFactory.deleteSystemActionsByWorkflowAction(action);

		Logger.debug(this,
				() -> "Removing the WorkflowAction and Step Dependencies, for action: " + action
						.getId() + ", name: " + action.getName());
		this.workFlowFactory.deleteAction(action);
		SecurityLogger.logInfo(this.getClass(),
				"Workflow Action with id: " + action.getId() + ", name: " + action.getName()
						+ " has been deleted");

	}

	@Override
	@WrapInTransaction
	public void deleteAction(final WorkflowAction action,
							 final WorkflowStep step, final User user) throws DotDataException, AlreadyExistException {

		this.isUserAllowToModifiedWorkflow(user);
		Logger.debug(this, () -> "Deleting the action: " + action.getId() +
					", from the step: " + step.getId());

		this.workFlowFactory.deleteAction(action, step);

		SecurityLogger.logInfo(this.getClass(),
				"Action id: '" + action.getName() + "' has been deleted by user: " + user.getUserId());
	} // deleteAction.

	@Override
	@CloseDBIfOpened
	public List<WorkflowActionClass> findActionClasses(final WorkflowAction action) throws DotDataException {

		return  workFlowFactory.findActionClasses(action);
	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowActionClass> findActionClassesByClassName(final String actionClassName) throws DotDataException {

		return  workFlowFactory.findActionClassesByClassName(actionClassName);
	}

	private void refreshWorkFlowActionletMap() {
		actionletMap = null;
		if (actionletMap == null) {
			synchronized (this.getClass()) {
				if (actionletMap == null) {

					List<WorkFlowActionlet> actionletList = new ArrayList<>();

					// get the dotmarketing-config.properties actionlet classes
					String customActionlets = Config.getStringProperty(WebKeys.WORKFLOW_ACTIONLET_CLASSES, "");

					StringTokenizer st = new StringTokenizer(customActionlets, ",");
					while (st.hasMoreTokens()) {
						String clazz = st.nextToken();
						try {
							WorkFlowActionlet actionlet = (WorkFlowActionlet) Class.forName(clazz.trim()).getDeclaredConstructor().newInstance();
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
					actionletMap = new LinkedHashMap<>();
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
		final List<WorkFlowActionlet> workFlowActionlets = new ArrayList<>();
		final Map<String,WorkFlowActionlet>  actionlets  = getActionlets();
		for (final String actionletName : actionlets.keySet()) {
			workFlowActionlets.add(getActionlets().get(actionletName));
		}
		return workFlowActionlets;

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
					"Workflow Action Class with id: '" + actionClass.getId() + "' has been deleted");

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
				final WorkflowAction baseAction = new WorkflowAction();
				baseAction.setId(actionClass.getActionId());

				actionClasses = findActionClasses(baseAction);
			} catch (Exception e) {
				throw new DotDataException(e.getLocalizedMessage());
			}

			final List<WorkflowActionClass> newActionClasses = new ArrayList<>(actionClasses.size());
			final int normalizedOrder =
					(order < 0) ? 0 : (order >= actionClasses.size()) ? actionClasses.size()-1 : order;
			for(final WorkflowActionClass currentActionClass : actionClasses) {

				if (currentActionClass.equals(actionClass)) {
					continue;
				}
				newActionClasses.add(currentActionClass);
			}

			newActionClasses.add(normalizedOrder, actionClass);
			for (int i = 0; i < newActionClasses.size(); i++) {
				newActionClasses.get(i).setOrder(i);
				saveActionClass(newActionClasses.get(i), user);
			}

			SecurityLogger.logInfo(this.getClass(),
					"Actionlet '" + actionClass.getName() + "' has been reordered by user: " + user.getUserId());
		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage(),e);
		}
	}

	@Override
	@CloseDBIfOpened
	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(final WorkflowActionClass actionClass) throws  DotDataException {

		return workFlowFactory.findParamsForActionClass(actionClass);
	}

	// note: does not need WrapInTransaction b/c it is already handling their own transaction
	@Override
	public void saveWorkflowActionClassParameters(final List<WorkflowActionClassParameter> params,
												  final User user) throws DotDataException{

		if(params == null || params.size() == 0){
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

			SecurityLogger.logInfo(this.getClass(),
					"Saving class parameters by the user: " + user.getUserId());
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

	@WrapInTransaction
	@Override
	public WorkflowProcessor fireWorkflowPreCheckin(final Contentlet contentlet, final User user) throws DotDataException,DotWorkflowException, DotContentletValidationException{
		return fireWorkflowPreCheckin(contentlet, user, null);
	}


    private Lazy<Method> firePreActionMethod = Lazy.of(() -> {
        return Try.of(() -> WorkFlowActionlet.class.getMethod("executePreAction", WorkflowProcessor.class, Map.class))
                .getOrElseThrow(
                        DotRuntimeException::new);
    });


	private WorkflowProcessor fireWorkflowPreCheckin(final Contentlet contentlet, final User user, final ConcurrentMap<String,Object> context) throws DotDataException,DotWorkflowException, DotContentletValidationException{
		WorkflowProcessor processor = new WorkflowProcessor(contentlet, user, context);
		if(!processor.inProcess()){
			return processor;
		}

		final List<WorkflowActionClass> actionClasses = processor.getActionClasses();

		final boolean isPublish = actionClasses.stream()
				.anyMatch((WorkflowActionClass workflowActionClass) -> workflowActionClass.getClazz().equals(
						PublishContentActionlet.class.getName()));

		contentlet.setProperty(Contentlet.TO_BE_PUBLISH, isPublish);


		if(actionClasses != null){
			for(WorkflowActionClass actionClass : actionClasses){
				final WorkFlowActionlet actionlet = actionClass.getActionlet();
				//Validate the actionlet exists and the OSGI is installed and running.
				if(UtilMethods.isSet(actionlet)){
					final Map<String,WorkflowActionClassParameter> params = findParamsForActionClass(actionClass);

                    APILocator.getRequestCostAPI()
                            .incrementCost(Price.WORKFLOW_ACTION_RUN, this.getClass(), "fireWorkflowPreCheckin",
                                    new Object[]{actionlet.getName(), params});


					actionlet.executePreAction(processor, params);
					//if we should stop processing further actionlets
					if(actionlet.stopProcessing()){
						break;
					}
				}else {
					throw new DotWorkflowException("Actionlet '" + actionClass.getName() + "' is null. Check if the Plugin is installed and running.");
				}

			}
		}

		return processor;
	}


	@WrapInTransaction
	@Override
	public void fireWorkflowPostCheckin(final WorkflowProcessor processor) throws DotDataException,DotWorkflowException{
		try{
			if(!processor.inProcess()){
				return;
			}
			processor.getContentlet().setActionId(processor.getAction().getId());

			final List<WorkflowActionClass> actionClasses = processor.getActionClasses();
			if(actionClasses != null){
				for(final WorkflowActionClass actionClass : actionClasses){

					final WorkFlowActionlet actionlet = actionClass.getActionlet();
					final Map<String,WorkflowActionClassParameter> params = findParamsForActionClass(actionClass);

                    APILocator.getRequestCostAPI()
                            .incrementCost(Price.WORKFLOW_ACTION_RUN, this.getClass(), "fireWorkflowPostCheckin",
                            new Object[]{actionlet.getName(), params});

					if (processor.isRunningBulk() && actionlet instanceof BatchAction) {
						final BatchAction batchable = (BatchAction) actionlet;
						batchable.preBatchAction(processor, actionClass, params);
						//gather data to run in batch later
					} else {
						actionlet.executeAction(processor, params);
					}

					//if we should stop processing further actionlets
					if(actionlet.stopProcessing()){
						break;
					}
				}
			}

			if (!processor.abort()) {

				this.saveWorkflowTask(processor);

				if (UtilMethods.isSet(processor.getContentlet()) && processor.getContentlet().needsReindex()) {

					Logger.info(this, "Needs reindex, adding the contentlet to the index at the end of the workflow execution");
				    final Contentlet content = processor.getContentlet();
				    final ThreadContext threadContext = ThreadContextUtil.getOrCreateContext();
					final boolean includeDependencies = null != threadContext && threadContext.isIncludeDependencies();
					this.contentletIndexAPI.addContentToIndex(content, includeDependencies);
					Logger.info(this, "Added contentlet to the index at the end of the workflow execution, dependencies: " + includeDependencies);
				}
			}
		} catch (final Exception e) {
			final String errorMsg = String.format("Failed to fire Workflow Action '%s' [%s]: %s",
					processor.getAction().getName(), processor.getAction().getId(), ExceptionUtil.getErrorMessage(e));
			Logger.error(WorkflowAPIImpl.class, errorMsg);
			Logger.debug(WorkflowAPIImpl.class, errorMsg, e);
			throw new DotWorkflowException(ExceptionUtil.getErrorMessage(e), e);
		} finally {
			// not matters what we need to reindex in deferred the index just in case.
			if (UtilMethods.isSet(processor.getContentlet()) &&
					UtilMethods.isSet(processor.getContentlet().getIdentifier())) {

				try {
					this.reindexQueueAPI.addIdentifierReindex(processor.getContentlet().getIdentifier());
				} catch (DotDataException e) {
					Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
				}
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

		saveWorkflowTaskWithoutIndexing(task, processor);

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

	private void saveWorkflowTaskWithoutIndexing(final WorkflowTask task) throws DotDataException {

		if (task.getLanguageId() <= 0) {

			Logger.error(this, "Workflow task: " + task.getId() +
					", doesn't have any language ID. Setting to default one");
			task.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		this.workFlowFactory.saveWorkflowTask(task);

		SecurityLogger.logInfo(this.getClass(), "Workflow task '" + task.getId() + "' has been saved.");
	}


	@WrapInTransaction
	private void saveWorkflowTaskWithoutIndexing(final WorkflowTask task, final WorkflowProcessor processor) throws DotDataException {

		this.saveWorkflowTaskInternal(task, processor, false);
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

	/**
	 * Entry point that fires up the actions associated with the contentles. Expects a lucene query
	 * that holds the logic to retrive a large selection of items performed on the UI.
	 *  @param action {@link WorkflowAction}
	 * @param user {@link User}
	 * @param luceneQuery luceneQuery
	 * @param additionalParamsBean
	 */
	@CloseDBIfOpened
	public BulkActionsResultView fireBulkActions(final WorkflowAction action,
			final User user, final String luceneQuery, final AdditionalParamsBean additionalParamsBean) throws DotDataException {

		final Set<String> workflowAssociatedStepsIds = workFlowFactory.findProxiesSteps(action)
				.stream().map(WorkflowStep::getId).collect(Collectors.toSet());

		return fireBulkActionsTaskForQuery(action, user, luceneQuery, workflowAssociatedStepsIds,
				additionalParamsBean);
	}

	/**
	 * Entry point that fires up the actions associated with the contentles. Expects a lucene query
	 * that holds the logic to retrive a large selection of items performed on the UI.
	 *  @param action {@link WorkflowAction}
	 * @param user {@link User}
	 * @param luceneQuery luceneQuery
	 * @param additionalParamsBean
	 */
	@CloseDBIfOpened
	public void fireBulkActionsNoReturn(final WorkflowAction action,
			final User user, final String luceneQuery, final AdditionalParamsBean additionalParamsBean) throws DotDataException {

		final Set<String> workflowAssociatedStepsIds = workFlowFactory.findProxiesSteps(action)
				.stream().map(WorkflowStep::getId).collect(Collectors.toSet());

		fireBulkActionsTaskForQueryNoReturn(action, user, luceneQuery, workflowAssociatedStepsIds,
				additionalParamsBean);
	}

    /**
     * This method will return the list of workflows actions available to a user on any give
     * piece of content, based on how and who has the content locked and what workflow step the content
     * is in
     */
	@Override
	@CloseDBIfOpened
	public List<WorkflowAction> findAvailableActions(final Contentlet contentlet, final User user,
													 final RenderMode renderMode) throws DotDataException, DotSecurityException {

		if(contentlet == null || contentlet.getStructure() ==null) {

			Logger.debug(this, () -> "the Contentlet: " + contentlet + " or their structure could be null");
			throw new DotStateException("content is null");
		}

		final boolean isNew  								= !UtilMethods.isSet(contentlet.getInode());
		final ImmutableList.Builder<WorkflowAction> actions =
				new ImmutableList.Builder<>();

		if(Host.HOST_VELOCITY_VAR_NAME.equals(contentlet.getStructure().getVelocityVarName())) {

			Logger.debug(this, () -> "The contentlet: " +
					contentlet.getIdentifier() + ", is a host. Returning zero available actions");

			return Collections.emptyList();
		}

		final boolean isValidContentlet = isNew || contentlet.isWorking();
		if (!isValidContentlet) {

			Logger.debug(this, () -> "The contentlet: " +
					contentlet.getIdentifier() + ", is not new or live/working version. Returning zero available actions");
			return Collections.emptyList();
		}

		final boolean canLock    = Try.of(()->APILocator.getContentletAPI().canLock(contentlet, user)).getOrElse(false);
		final boolean isLocked   = isNew? true :  Try.of(()->APILocator.getVersionableAPI().isLocked(contentlet)).getOrElse(false);
		final boolean isPublish  = isNew? false:  Try.of(()->APILocator.getVersionableAPI().hasLiveVersion(contentlet)).getOrElse(false);
		final boolean isArchived = isNew? false:  Try.of(()->APILocator.getVersionableAPI().isDeleted(contentlet)).getOrElse(false);

		final List<WorkflowStep> steps = findStepsByContentlet(contentlet, false);

		Logger.debug(this, "#findAvailableActions: for content: "   + contentlet.getIdentifier()
				+ ", isNew: "    + isNew
				+ ", canLock: "        + canLock + ", isLocked: " + isLocked);


		return isNew?
				this.doFilterActions(actions, true, false, false, canLock, isLocked, renderMode, findActions(steps, user, contentlet.getContentType())):
				this.doFilterActions(actions, false, isPublish, isArchived, canLock, isLocked, renderMode, findActions(steps, user, contentlet));
	}

	/**
	 * Entry point that fires up the actions associated with the contentles. Expects a list of ids
	 * selected on the UI.
	 *  @param action {@link WorkflowAction}
	 * @param user {@link User}
	 * @param contentletIds {@link List}
	 * @param additionalParamsBean
	 */
	@CloseDBIfOpened
	public BulkActionsResultView fireBulkActions(final WorkflowAction action,
			final User user, final List<String> contentletIds, final AdditionalParamsBean additionalParamsBean) throws DotDataException {

		final Set<String> workflowAssociatedStepsIds = workFlowFactory.findProxiesSteps(action)
				.stream().map(WorkflowStep::getId).collect(Collectors.toSet());
		return fireBulkActionsTaskForSelectedInodes(action, user, contentletIds, workflowAssociatedStepsIds,
				additionalParamsBean);
	}

	/**
	 * Entry point that fires up the actions associated with the contentles. Expects a list of ids
	 * selected on the UI.
	 *  @param action {@link WorkflowAction}
	 * @param user {@link User}
	 * @param contentletIds {@link List}
	 * @param additionalParamsBean
	 */
	@CloseDBIfOpened
	public void fireBulkActionsNoReturn(final WorkflowAction action,
			final User user, final List<String> contentletIds, final AdditionalParamsBean additionalParamsBean) throws DotDataException {

		final Set<String> workflowAssociatedStepsIds = workFlowFactory.findProxiesSteps(action)
				.stream().map(WorkflowStep::getId).collect(Collectors.toSet());
		fireBulkActionsTaskForSelectedInodesNoReturn(action, user, contentletIds, workflowAssociatedStepsIds,
				additionalParamsBean);
	}

	/**
	 * This version of the method expects to work with a larg set of contentlets, But instead of
	 * having all the ids set a lucene query is used.
	 */
	private List<Contentlet> findContentletsToProcess(final String luceneQuery,
			final Iterable<String> workflowAssociatedStepIds, final User user, final int limit, final int offset) {

		try{
		final String luceneQueryWithSteps = String.format(" %s +(wfstep:%s ) ", luceneQuery,
				String.join(" wfstep:", workflowAssociatedStepIds));

		return ImmutableList.<Contentlet>builder().addAll(
				contentletAPI.search(luceneQueryWithSteps, limit, offset, null, user, !RESPECT_FRONTEND_ROLES)
		).build();
		}catch (Exception e){
			final Throwable rootCause = ExceptionUtil.getRootCause(e);
			if(rootCause instanceof QueryPhaseExecutionException){
				final QueryPhaseExecutionException qpe = QueryPhaseExecutionException.class.cast(rootCause);
				Logger.debug(getClass(),()->String.format("Unable to fetch contentlets beyond an offset of %d. %s ", offset, qpe.getMessage()));
			} else {
				Logger.error(getClass(),"Unexpected Error fetching contentlets from ES", e);
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Out of a given query computes the number of contentlets that will get skipped.
	 * @param luceneQuery
	 * @param workflowAssociatedStepsIds
	 * @param user
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	private long computeSkippedContentletsCount(final String luceneQuery,
			final Iterable<String> workflowAssociatedStepsIds, User user)
			throws DotSecurityException, DotDataException {

		final long totalCount = APILocator.getContentletAPI()
				.indexCount(luceneQuery, user, RESPECT_FRONTEND_ROLES);

		final String contentletsWithinStepsQuery = String
				.format("%s  +(wfstep:%s ) ", String.join(StringPool.SPACE, luceneQuery),
						String.join(" wfstep:", workflowAssociatedStepsIds));
		final long withinStepsCount = APILocator.getContentletAPI()
				.indexCount(contentletsWithinStepsQuery, user, RESPECT_FRONTEND_ROLES);

		return (totalCount - withinStepsCount);
	}

	/**
	 * This version of the method deals with inodes selected directly on the UI
	 */
	private BulkActionsResultView fireBulkActionsTaskForSelectedInodes(final WorkflowAction action,
			final User user,
			final List<String> inodes, final Set<String> workflowAssociatedStepIds,
			final AdditionalParamsBean additionalParamsBean)
			throws DotDataException {

		try {

			final String luceneQuery = String
					.format("+inode:( %s ) +(wfstep:%s )", String.join(StringPool.SPACE, inodes),
							String.join(" wfstep:", workflowAssociatedStepIds));

			final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(luceneQuery);
			return distributeWorkAndProcess(action, user, sanitizedQuery, workflowAssociatedStepIds,
                    additionalParamsBean);
		} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when firing actions in bulk for Action '%s' " +
                    "[%s]: %s", action.getName(), action.getId(), e.getMessage());
            Logger.error(getClass(), errorMsg, e);
            throw new DotDataException(errorMsg, e);
		}
	}

	/**
	 * This version of the method deals with inodes selected directly on the UI
	 */
	private void fireBulkActionsTaskForSelectedInodesNoReturn(final WorkflowAction action,
			final User user,
			final List<String> inodes, final Set<String> workflowAssociatedStepIds,
			final AdditionalParamsBean additionalParamsBean)
			throws DotDataException {

		try {

			final String luceneQuery = String
					.format("+inode:( %s ) +(wfstep:%s )", String.join(StringPool.SPACE, inodes),
							String.join(" wfstep:", workflowAssociatedStepIds));

			final String sanitizedQuery = LuceneQueryUtils.sanitizeBulkActionsQuery(luceneQuery);
			distributeWorkAndProcessNoReturn(action, user, sanitizedQuery, workflowAssociatedStepIds,
					additionalParamsBean, inodes.size());
		} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when firing actions in bulk for Action '%s' " +
					"[%s]: %s", action.getName(), action.getId(), e.getMessage());
			Logger.error(getClass(), errorMsg, e);
			throw new DotDataException(errorMsg, e);
		}
	}


	/**
	 * This version of the method deals with a large selection of items which gets translated into a
	 * lucene query
	 */
	private BulkActionsResultView fireBulkActionsTaskForQuery(final WorkflowAction action,
			final User user,
			final String luceneQuery, final Set<String> workflowAssociatedStepsIds,
			final AdditionalParamsBean additionalParamsBean)
			throws DotDataException {

		try {

			final String sanitizedQuery = LuceneQueryUtils
					.sanitizeBulkActionsQuery(luceneQuery);
			Logger.debug(getClass(), ()->"luceneQuery: " + sanitizedQuery);

			return distributeWorkAndProcess(action, user, sanitizedQuery, workflowAssociatedStepsIds, additionalParamsBean);
		} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when firing actions in bulk for Action '%s' " +
                    "[%s]: %s", action.getName(), action.getId(), e.getMessage());
            Logger.error(getClass(), errorMsg, e);
            throw new DotDataException(errorMsg, e);
		}

	}

	/**
	 * This version of the method deals with a large selection of items which gets translated into a
	 * lucene query
	 */
	private void fireBulkActionsTaskForQueryNoReturn(final WorkflowAction action,
			final User user,
			final String luceneQuery, final Set<String> workflowAssociatedStepsIds,
			final AdditionalParamsBean additionalParamsBean)
			throws DotDataException {

		try {

			final String sanitizedQuery = LuceneQueryUtils
					.sanitizeBulkActionsQuery(luceneQuery);
			Logger.debug(getClass(), ()->"luceneQuery: " + sanitizedQuery);

			distributeWorkAndProcessNoReturn(action, user, sanitizedQuery, workflowAssociatedStepsIds, additionalParamsBean, -1);
		} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when firing actions in bulk for Action '%s' " +
					"[%s]: %s", action.getName(), action.getId(), e.getMessage());
			Logger.error(getClass(), errorMsg, e);
			throw new DotDataException(errorMsg, e);
		}

	}

	/**
	 * This method takes the contentlets that are the result of a selection made on the UI or the
	 * product of a lucene Query The list then gets partitioned and distributed between the list of
	 * available workers (threads).
	 * @param action
	 * @param user
	 * @param additionalParamsBean
	 * @return
	 */
	private BulkActionsResultView distributeWorkAndProcess(final WorkflowAction action,
			final User user, final String sanitizedQuery,
			final Set<String> workflowAssociatedStepsIds,
			final AdditionalParamsBean additionalParamsBean)
			throws DotDataException, DotSecurityException {
		// We use a dedicated pool for bulk actions processing.
		final DotSubmitter submitter = this.concurrentFactory
				.getSubmitter(DotConcurrentFactory.BULK_ACTIONS_THREAD_POOL);

		//Max number of exceptions we desire to capture
		final int maxExceptions = Config.getIntProperty(MAX_EXCEPTIONS_REPORTED_ON_BULK_ACTIONS,
				MAX_EXCEPTIONS_REPORTED_ON_BULK_ACTIONS_DEFAULT);

		//The long we want to sleep to avoid threads starvation.
		final int sleepThreshold = Config
				.getIntProperty(BULK_ACTIONS_SLEEP_THRESHOLD, BULK_ACTIONS_SLEEP_THRESHOLD_DEFAULT);

		//This defines the number of contentlets that gets pulled from ES on each iteration.
		final int limit = Config
				.getIntProperty(BULK_ACTIONS_CONTENTLET_FETCH_STEP, BULK_ACTIONS_CONTENTLET_FETCH_STEP_DEFAULT);

		//Actions Shared context.
		final ConcurrentMap<String, Object> actionsContext = new ConcurrentHashMap<>();

		final AtomicLong successCount = new AtomicLong();

		final List<ActionFail> fails = new ArrayList<>();

		final ReentrantLock lock = new ReentrantLock();

		final AtomicBoolean acceptingExceptions = new AtomicBoolean(true);

		final List<Future> futures = new ArrayList<>();

		final Long skipsCount = computeSkippedContentletsCount(sanitizedQuery,
				workflowAssociatedStepsIds,
				user);

		int offset = 0;

		while (!submitter.isAborting()) {
			final List<Contentlet> contentlets = findContentletsToProcess(sanitizedQuery,
					workflowAssociatedStepsIds,
					user, limit, offset
			);

			if (contentlets.isEmpty()) {
				///We're done let's get out of here.
				break;
			}

			final List<List<Contentlet>> partitions = partitionContentletInput(contentlets);

			final Consumer<Long> sucessCallback = UtilMethods.isSet(additionalParamsBean
					.getAdditionalParamsMap().get(SUCCESS_ACTION_CALLBACK))
					? (Consumer<Long>) additionalParamsBean
							.getAdditionalParamsMap().get(SUCCESS_ACTION_CALLBACK)
					: successCount::addAndGet;

			additionalParamsBean.getAdditionalParamsMap().remove(SUCCESS_ACTION_CALLBACK);

			final BiConsumer<String,Exception> failCallback = UtilMethods.isSet(additionalParamsBean
					.getAdditionalParamsMap().get(FAIL_ACTION_CALLBACK))
					? (BiConsumer<String,Exception>) additionalParamsBean
					.getAdditionalParamsMap().get(FAIL_ACTION_CALLBACK)
					: (inode, e) -> {
						//if not accepting exceptions no need to lock and process. We're simply not accepting more.
						if (acceptingExceptions.get()) {
							lock.lock();
							try {
								fails.add(ActionFail.newInstance(user, inode, e));
								acceptingExceptions
										.set(fails.size() < maxExceptions);
							} finally {
								lock.unlock();
							}
						}
					};

			additionalParamsBean.getAdditionalParamsMap().remove(FAIL_ACTION_CALLBACK);

			for (final List<Contentlet> partition : partitions) {
				futures.add(
						submitter.submit(() -> {

							if(submitter.isAborting()){
							   Logger.debug(getClass(),()->"Bulk Actions Halted!");
                               return;
							}

								fireBulkActionTasks(action, user, partition,
										additionalParamsBean,sucessCallback, failCallback,
										actionsContext, sleepThreshold);
								}
						)
				);
			}

			offset += limit;

		}


		Logger.debug(getClass(),String.format("A grand total of %d contentlets has been pulled from ES.",offset));

		for (final Future future : futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				Logger.error(this, e.getMessage(), e);
			}
		}

		//Actions which implement the interface BatchAction are shared between threads using the actionsContext
		executeBatchActions(user, action, actionsContext, (list, e) -> {
			list.forEach(o -> {
						fails.add(ActionFail.newInstance(user, o.toString(), e));
					}
			);
			//Initially When adding up to the list of batch actions we assume a success.
			//So we need to Update success count in case something has gone wrong internally.
			//This assumes that if one single action fails internally the whole batch has failed.
			successCount.updateAndGet(value -> value - list.size());
		});

		return new BulkActionsResultView(
				successCount.get(),
				skipsCount,
				ImmutableList.copyOf(fails)
		);
	}

	/**
	 * This method takes the contentlets that are the result of a selection made on the UI or the
	 * product of a lucene Query The list then gets partitioned and distributed between the list of
	 * available workers (threads).
	 * @param action
	 * @param user
	 * @param additionalParamsBean
	 * @return
	 */
	private void distributeWorkAndProcessNoReturn(final WorkflowAction action,
			final User user, final String sanitizedQuery,
			final Set<String> workflowAssociatedStepsIds,
			final AdditionalParamsBean additionalParamsBean,
			final int totalCount)
			throws DotDataException, DotSecurityException {
		// We use a dedicated pool for bulk actions processing.
		final DotSubmitter submitter = this.concurrentFactory
				.getSubmitter(DotConcurrentFactory.BULK_ACTIONS_THREAD_POOL);

		//Max number of exceptions we desire to capture
		final int maxExceptions = Config.getIntProperty(MAX_EXCEPTIONS_REPORTED_ON_BULK_ACTIONS,
				MAX_EXCEPTIONS_REPORTED_ON_BULK_ACTIONS_DEFAULT);

		//The long we want to sleep to avoid threads starvation.
		final int sleepThreshold = Config
				.getIntProperty(BULK_ACTIONS_SLEEP_THRESHOLD, BULK_ACTIONS_SLEEP_THRESHOLD_DEFAULT);

		//This defines the number of contentlets that gets pulled from ES on each iteration.
		final int limit = Config
				.getIntProperty(BULK_ACTIONS_CONTENTLET_FETCH_STEP, BULK_ACTIONS_CONTENTLET_FETCH_STEP_DEFAULT);

		//Actions Shared context.
		final ConcurrentMap<String, Object> actionsContext = new ConcurrentHashMap<>();

		final AtomicLong successCount = new AtomicLong();

		final List<ActionFail> fails = new ArrayList<>();

		final ReentrantLock lock = new ReentrantLock();

		final AtomicBoolean acceptingExceptions = new AtomicBoolean(true);

		final List<Future> futures = new ArrayList<>();

		int offset = 0;

		while (!submitter.isAborting()) {
			final List<Contentlet> contentlets = findContentletsToProcess(sanitizedQuery,
					workflowAssociatedStepsIds,
					user, limit, offset
			);

			if (contentlets.isEmpty()) {
				///We're done let's get out of here.
				break;
			}

			final List<List<Contentlet>> partitions = partitionContentletInput(contentlets);

			final Consumer<Long> sucessCallback = UtilMethods.isSet(additionalParamsBean
					.getAdditionalParamsMap().get(SUCCESS_ACTION_CALLBACK))
					? (Consumer<Long>) additionalParamsBean
					.getAdditionalParamsMap().get(SUCCESS_ACTION_CALLBACK)
					: successCount::addAndGet;

			final BiConsumer<String,Exception> failCallback = UtilMethods.isSet(additionalParamsBean
					.getAdditionalParamsMap().get(FAIL_ACTION_CALLBACK))
					? (BiConsumer<String,Exception>) additionalParamsBean
					.getAdditionalParamsMap().get(FAIL_ACTION_CALLBACK)
					: (inode, e) -> {
						//if not accepting exceptions no need to lock and process. We're simply not accepting more.
						if (acceptingExceptions.get()) {
							lock.lock();
							try {
								fails.add(ActionFail.newInstance(user, inode, e));
								acceptingExceptions
										.set(fails.size() < maxExceptions);
							} finally {
								lock.unlock();
							}
						}
					};

			for (final List<Contentlet> partition : partitions) {
				futures.add(
						submitter.submit(() -> {

									if(submitter.isAborting()){
										Logger.info(getClass(),()->"Bulk Actions Halted!");
										return;
									}

									fireBulkActionTasks(action, user, partition,
											additionalParamsBean,sucessCallback, failCallback,
											actionsContext, sleepThreshold);
								}
						)
				);
			}

			offset += limit;
		}

		sendNotification(user, futures, totalCount);

		Logger.debug(getClass(),String.format("A grand total of %d contentlets has been pulled from ES.",offset));
	}

	private void sendNotification(final User user, final List<Future> futures,
			final int totalCount)
			throws DotDataException {

		if(totalCount>Config.getIntProperty("WORKFLOW_BULK_SENDNOTIFICATIONAFTER", 250)) {

			final Role cmsAdminRole = this.roleAPI.loadCMSAdminRole();

			DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
				for (final Future future : futures) {
					try {
						future.get();
					} catch (InterruptedException | ExecutionException e) {
						Logger.error(this, e.getMessage(), e);
					}
				}

				Try.run(() -> {
					APILocator.getNotificationAPI()
							.generateNotification(new I18NMessage("Workflow-Bulk-Actions-Title"),
									// title = Reindex Notification
									new I18NMessage("Workflow-Bulk-Actions-Finished", null,
											(Object) null),
									null, // no actions
									NotificationLevel.INFO, NotificationType.GENERIC,
									Visibility.ROLE, cmsAdminRole.getId(), user.getUserId(),
									user.getLocale());
				});
			});
		}
	}


	/**
	 * This method takes the all the Batch actions saved in the batchActionContext and executes them
	 * Also gathers any exception the might result of an internal error while processing the actions getting executed as a single batch.
	 * @param user
	 * @param action
	 * @param actionsContext
	 * @param failsConsumer
	 * @throws DotDataException
	 */
	private void executeBatchActions(
			final User user,
			final WorkflowAction action,
			final ConcurrentMap<String, Object> actionsContext,
			final BiConsumer<List<?>, Exception> failsConsumer) throws DotDataException {

		final List<WorkflowActionClass> actionClasses = this.findActionClasses(action);
		for (final WorkflowActionClass actionClass : actionClasses) {
			final WorkFlowActionlet actionlet = actionClass.getActionlet();
			if (actionlet instanceof BatchAction) {
				final Map<String, WorkflowActionClassParameter> params = findParamsForActionClass(actionClass);
				final BatchAction batchAction = BatchAction.class.cast(actionlet);
				final List <?> objects = batchAction.getObjectsForBatch(actionsContext, actionClass);
				try {
					this.executeBatchAction(user, actionsContext, actionClass, params, batchAction);
				} catch (final Exception e) {
					failsConsumer.accept(objects, e);
					Logger.error(getClass(), String.format("Exception while trying to execute action '%s' [%s] in " +
							"batch: %s", actionClass.getName(), actionClass.getActionId(), e.getMessage()), e);
					// We assume the entire batch is has failed. So break;
					break;
				}
			}
		}
	}

	@WrapInTransaction
	private void executeBatchAction(final User user,
									final ConcurrentMap<String, Object> actionsContext,
									final WorkflowActionClass actionClass,
									final Map<String, WorkflowActionClassParameter> params,
									final BatchAction batchAction) {

		batchAction.executeBatchAction(user, actionsContext, actionClass, params);
	}

	/**
	 * Takes a input list of contentlets and creates sub groups to distribute the workload
	 */
	private List<List<Contentlet>> partitionContentletInput(final List<Contentlet> contentlets) {
		final int maxThreads = Config.getIntProperty(MAX_THREADS_ALLOWED_TO_HANDLE_BULK_ACTIONS, MAX_THREADS_ALLOWED_TO_HANDLE_BULK_ACTIONS_DEFAULT);
		final int partitionSize = Math
				.max((contentlets.size() / maxThreads), 10);

		Logger.info(getClass(),
				String.format(
						"Number of threads is limited to %d. Number of Contentlets to process is %d. Load will be distributed in groups of %d ",
						maxThreads, contentlets.size(),
						partitionSize)
		);
		return Lists.partition(contentlets, partitionSize);

	}

	/**
	 * Applies the values captured via pop-up on the UI (If any) And gets them applied to the contentlet through the dependencies builder.
	 * @param additionalParamsBean
	 * @param dependenciesBuilder
	 * @return
	 */
	private ContentletDependencies.Builder applyAdditionalParams(final AdditionalParamsBean additionalParamsBean, ContentletDependencies.Builder dependenciesBuilder){
		if(UtilMethods.isSet(additionalParamsBean) && UtilMethods.isSet(additionalParamsBean.getAssignCommentBean()) ){
			final AssignCommentBean assignCommentBean = additionalParamsBean.getAssignCommentBean();
			if(UtilMethods.isSet(assignCommentBean.getComment())){
				dependenciesBuilder = dependenciesBuilder.workflowActionComments(assignCommentBean.getComment());
			}
			if(UtilMethods.isSet(assignCommentBean.getAssign())){
				dependenciesBuilder = dependenciesBuilder.workflowAssignKey(assignCommentBean.getAssign());
			}
		}
		return dependenciesBuilder;
	}

	/**
	 * This process a batch of Contents all within the same Thread
	 * @param action
	 * @param user
	 * @param contentlets
	 * @param additionalParamsBean
	 * @param successConsumer
	 * @param failConsumer
	 * @param context
	 * @param sleep
	 */
	public void fireBulkActionTasks(final WorkflowAction action,
			final User user,
			final List<Contentlet> contentlets,
			final AdditionalParamsBean additionalParamsBean,
			final Consumer<Long> successConsumer,
			final BiConsumer<String,Exception> failConsumer,
			final ConcurrentMap<String,Object> context,
			final int sleep) {

		ContentletDependencies.Builder dependenciesBuilder = new ContentletDependencies.Builder();
		dependenciesBuilder = dependenciesBuilder
				.respectAnonymousPermissions(false)
				.generateSystemEvent(false)
				.modUser(user)
				.workflowActionId(action.getId());

		final boolean requiresAdditionalParams = ActionletUtil.requiresPopupAdditionalParams(action);

		if(requiresAdditionalParams){
			// additional params applied through the builder
			dependenciesBuilder = applyAdditionalParams(additionalParamsBean, dependenciesBuilder);
		}

		final ContentletDependencies dependencies = dependenciesBuilder.build();

		for (Contentlet contentlet : contentlets) {
			try {
				try {
					if (requiresAdditionalParams) {
						// additional params applied directly to the contentlet.
						contentlet = applyAdditionalParams(additionalParamsBean, contentlet);
					}

					if (UtilMethods.isSet(additionalParamsBean) && UtilMethods.isSet(additionalParamsBean.getAdditionalParamsMap())) {

						for (Map.Entry<String, Object> entry : additionalParamsBean.getAdditionalParamsMap().entrySet()) {
							if(entry.getKey().equals(SUCCESS_ACTION_CALLBACK)
									|| entry.getKey().equals(FAIL_ACTION_CALLBACK)) {
								continue;
							}
							contentlet.setProperty(entry.getKey(), entry.getValue());
						}
					}

					fireBulkActionTask(action, contentlet, dependencies, successConsumer,
							failConsumer, context);
				} catch (Exception e) {
					// Additional catch block to handle any exceptions when processing the Transactional Annotation.
					Logger.error(getClass(), "Error processing fire Action Task", e);
				}
			} finally {
				DateUtil.sleep(sleep);
			}
		}
	}

	/**
	 * Applies the values captured via pop-up on the UI (If any) And gets them applied directly to the contentlet.
	 * @param additionalParamsBean
	 * @param contentlet
	 * @return
	 */
	private Contentlet applyAdditionalParams(final AdditionalParamsBean additionalParamsBean, Contentlet contentlet){
		if(UtilMethods.isSet(additionalParamsBean) && UtilMethods.isSet(additionalParamsBean.getPushPublishBean()) ){
			final PushPublishBean pushPublishBean = additionalParamsBean.getPushPublishBean();
			contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE, pushPublishBean.getPublishDate());
			contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME, pushPublishBean.getPublishTime());
			contentlet.setStringProperty(Contentlet.WORKFLOW_TIMEZONE_ID, pushPublishBean.getTimezoneId());
			contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE, pushPublishBean.getExpireDate());
			contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME, pushPublishBean.getExpireTime());
			contentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE, pushPublishBean.getNeverExpire());
			contentlet.setStringProperty(Contentlet.WHERE_TO_SEND, pushPublishBean.getWhereToSend());
			contentlet.setStringProperty(Contentlet.FILTER_KEY, pushPublishBean.getFilterKey());
			contentlet.setStringProperty(Contentlet.I_WANT_TO, pushPublishBean.getIWantTo());
		}

		return contentlet;
	}

	@WrapInTransaction
	private void fireBulkActionTask(final WorkflowAction action,
			final Contentlet contentlet,
			final ContentletDependencies dependencies,
			final Consumer<Long> successConsumer,
			final BiConsumer<String,Exception> failConsumer,
			final ConcurrentMap<String,Object> context) {
		try {

			final String successInode;
			Logger.debug(this,
					() -> "Firing the action: " + action + " to the inode: " + contentlet
							.getInode() +" Executed by Thread: " +Thread.currentThread().getName());

			contentlet.setTags();
			contentlet.getMap().put(Contentlet.WORKFLOW_BULK_KEY, true);
			contentlet.setIndexPolicy(IndexPolicy.DEFER);
			try{
				final Contentlet afterFireContentlet = fireContentWorkflow(contentlet, dependencies, context);
				if(afterFireContentlet != null){
					successInode = afterFireContentlet.getInode();
				} else {
					successInode = "Unavailable";
				}

				Logger.debug(this, () -> "Successfully fired the contentlet: " + contentlet.getInode() +
						", success inode: " + successInode);
				try {
					successConsumer.accept(1L);
				} catch(Exception e) {
					throw new DotRuntimeException(e);
				}
			}finally{
				// This tends to stick around in cache. So.. get rid of it.
				contentlet.getMap().remove(Contentlet.WORKFLOW_BULK_KEY);
			}

		} catch (Exception e) {
			failConsumer.accept(contentlet.getInode(), e);
			Logger.error(this, e.getMessage(), e);
		}
	}

	@WrapInTransaction
	@Override
	public Contentlet fireContentWorkflow(final Contentlet contentlet, final ContentletDependencies dependencies) throws DotDataException, DotSecurityException {

		if (isHost(contentlet)) {
			final User user = dependencies.getModUser();
			throw new DotSecurityException(
					ExceptionUtil
							.getLocalizedMessageOrDefault(user, "Workflow-restricted-content-type",
									"Invalid attempt to execute a workflow on a restricted Content type",
									getClass()
							)
			);
		}

		setWorkflowPropertiesToContentlet(contentlet, dependencies);

		this.validateActionStepAndWorkflow(contentlet, dependencies.getModUser());
		this.checkShorties (contentlet);

		final WorkflowProcessor processor   = ThreadContextUtil.wrapReturnNoReindex(()-> this.fireWorkflowPreCheckin(contentlet, dependencies.getModUser()));

		processor.setContentletDependencies(dependencies);
		processor.getContentlet().setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);

		ThreadContextUtil.wrapVoidNoReindex(() -> this.fireWorkflowPostCheckin(processor));

		if (null != processor.getContentlet()) {
			processor.getContentlet().setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.FALSE);
		} else {
            Logger.info(this, String.format("Workflow Action '%s' was not executed on Contentlet with ID '%s'", (null
                    != contentlet ? contentlet.getActionId() : "Unknown"), (null != contentlet ? contentlet.getIdentifier()
                    : "Unknown")));
        }

		return processor.getContentlet();
	} // fireContentWorkflow

	private void setWorkflowPropertiesToContentlet(final Contentlet contentlet,
			final ContentletDependencies dependencies) {

		if(UtilMethods.isSet(dependencies.getWorkflowActionId())){
			contentlet.setActionId(dependencies.getWorkflowActionId());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowActionComments())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, dependencies.getWorkflowActionComments());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowAssignKey())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, dependencies.getWorkflowAssignKey());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowPublishDate())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE, dependencies.getWorkflowPublishDate());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowPublishTime())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME, dependencies.getWorkflowPublishTime());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowTimezoneId())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_TIMEZONE_ID, dependencies.getWorkflowTimezoneId());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowExpireDate())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE, dependencies.getWorkflowExpireDate());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowExpireTime())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME, dependencies.getWorkflowExpireTime());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowNeverExpire())){
			contentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE, dependencies.getWorkflowNeverExpire());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowWhereToSend())){
			contentlet.setStringProperty(Contentlet.WHERE_TO_SEND, dependencies.getWorkflowWhereToSend());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowFilterKey())){
			contentlet.setStringProperty(Contentlet.FILTER_KEY, dependencies.getWorkflowFilterKey());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowIWantTo())){
			contentlet.setStringProperty(Contentlet.I_WANT_TO, dependencies.getWorkflowIWantTo());
		}

		if(UtilMethods.isSet(dependencies.getWorkflowPathToMove())){
			contentlet.setStringProperty(Contentlet.PATH_TO_MOVE, dependencies.getWorkflowPathToMove());
		}
	}


	@WrapInTransaction
	private Contentlet fireContentWorkflow(final Contentlet contentlet, final ContentletDependencies dependencies, final ConcurrentMap<String,Object> context) throws DotDataException, DotSecurityException {

		if (isHost(contentlet)) {
			final User user = dependencies.getModUser();
			throw new DotSecurityException(
					ExceptionUtil
							.getLocalizedMessageOrDefault(user, "Workflow-restricted-content-type",
									"Invalid attempt to execute a workflow on a restricted Content type",
									getClass()
							)
			);
		}


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

		final WorkflowProcessor processor = this.fireWorkflowPreCheckin(contentlet, dependencies.getModUser(), context);

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

				final boolean isValidContentlet = !InodeUtils.isSet(contentlet.getInode())
						|| contentlet.isWorking() ||
						APILocator.getVersionableAPI().hasWorkingVersionInAnyOtherLanguage(contentlet, contentlet.getLanguageId());
				if (!isValidContentlet) {

					throw new IllegalArgumentException(LanguageUtil
							.get(user.getLocale(), "Invalid-Contentlet-State-Fire-Action-Error",
									contentlet.getIdentifier(), contentlet.getInode(), contentlet.getContentType().name()));
				}

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
							.get(user.getLocale(), "Invalid-Action-Scheme-Error", actionId, contentlet.getContentType().name(), contentlet.getInode()));
				}

				final WorkflowScheme  scheme = schemes.stream().filter
						(aScheme -> aScheme.getId().equals(action.getSchemeId())).findFirst().get();

				if (scheme.isArchived()) {
					throw new IllegalArgumentException(LanguageUtil
							.get(user.getLocale(), "Invalid-Scheme-Archive-Error", actionId));
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

					final Optional<WorkflowStep> workflowStepOptional = this.findFirstStep(scheme);

					if (workflowStepOptional.isEmpty() ||
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

	@WrapInTransaction
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

			param.setActionClassId(to.getId());
			param.setKey		  (from.getKey());
			param.setValue		  (from.getValue());

			params.add(param);
		}

		this.saveWorkflowActionClassParameters(params, user);

		SecurityLogger.logInfo(this.getClass(),
				"Copying class parameters by the user: " + user.getUserId());

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

        SecurityLogger.logInfo(this.getClass(), String.format("Copying Workflow Action Class '%s' [%s] by User ID '%s'",
                from.getName(), from.getId(), user.getUserId()));

		return actionClass;
	}

	@Override
	public WorkflowAction copyWorkflowAction(final WorkflowAction from,
											 final WorkflowScheme to,
											 final User user) throws DotDataException, AlreadyExistException, DotSecurityException {
		return this.copyWorkflowAction(from, to, user, Collections.emptyMap());
	}

	@WrapInTransaction
	@Override
	public WorkflowAction copyWorkflowAction(final WorkflowAction from,
											 final WorkflowScheme to,
											 final User user,
											 final Map<String, WorkflowStep> stepsFromToMapping) throws DotDataException, AlreadyExistException, DotSecurityException {

		Logger.debug(this, ()-> "Copying the WorkflowAction: " + from.getId() +
				", name: " + from.getName());

		final WorkflowAction action = new WorkflowAction();
		final WorkflowStep workflowStep = stepsFromToMapping.getOrDefault(from.getNextStep(), null);

		action.setSchemeId   (to.getId());
		action.setAssignable (from.isAssignable());
		action.setCommentable(from.isCommentable());
		action.setCondition  (from.getCondition());
		action.setName		 (from.getName());
		action.setShowOn	 (from.getShowOn());
		action.setNextAssign (from.getNextAssign());
		action.setNextStep	 (null != workflowStep? workflowStep.getId(): null);
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

        SecurityLogger.logInfo(this.getClass(), String.format("Copying Workflow Action '%s' [%s] by User ID '%s'",
                from.getName(), from.getId(), user.getUserId()));

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

        SecurityLogger.logInfo(this.getClass(), String.format("Copying Workflow Step '%s' [%s] by User ID '%s'", from
                .getName(), from.getId(), user.getUserId()));

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

			actions.put(action.getId(), this.copyWorkflowAction(action, scheme, user, steps));
		}

		for (final WorkflowStep step : stepsFrom) {

			int   actionOrder 						= 0;
			final List<WorkflowAction>  actionSteps =
					this.findActions(step, user);

			for (final WorkflowAction action : actionSteps) {
				if (steps.containsKey(step.getId()) && actions.containsKey(action.getId())) {
					final String stepId = steps.get(step.getId()).getId();
					final String actionId = actions.get(action.getId()).getId();
					this.saveAction(actionId, stepId, user, actionOrder++);
				}
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

        SecurityLogger.logInfo(this.getClass(), String.format("Executing deep copy for Scheme '%s' [%s] by User ID " +
                "'%s'", from.getName(), from.getId(), user.getUserId()));

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
			throw new DotSecurityException("User '" + user + "' cannot read action '" + entryAction.getName() + "'");
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

        SecurityLogger.logInfo(this.getClass(), String.format("Workflow action class parameter '%s' has been " +
                "deleted", param.getId()));
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
                () -> "User '" + user + "' cannot read action: " + action.getName(),
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
                    () -> "User '" + user + "' cannot read action: " + action.getName(),
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
				() -> "User '" + user + "' cannot read content type: " + contentType.name(),
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
			throws DotDataException, DotSecurityException,  AlreadyExistException {
		scheme.setArchived(Boolean.TRUE);
		saveScheme(scheme, user);

	}

	@Override
	@CloseDBIfOpened
	public List<WorkflowTimelineItem> getCommentsAndChangeHistory(final WorkflowTask task) throws DotDataException{

	    final List<WorkflowTimelineItem> workflowTimelineItems =
				CollectionsUtils.join(this.findWorkFlowComments(task),
						this.findWorkflowHistory (task));

		final Comparator<WorkflowTimelineItem> comparator = Comparator.comparing(WorkflowTimelineItem::createdDate).reversed();
		return workflowTimelineItems.stream()
				.sorted(comparator)
				.collect(CollectionsUtils.toImmutableList());

	}


	/**
	 * Maps a {@link SystemAction} to a {@link WorkflowAction} for a {@link ContentType}
	 * @param systemAction   {@link SystemAction}   System Action to mapping
	 * @param workflowAction {@link WorkflowAction} Workflow Action to map to the SystemAction
	 * @param contentType    {@link ContentType}    The Map is associated to a content type
	 * @throws DotDataException
	 */
	@Override
	@WrapInTransaction
	public SystemActionWorkflowActionMapping mapSystemActionToWorkflowActionForContentType (final SystemAction systemAction, final WorkflowAction workflowAction,
														final ContentType contentType) throws DotDataException {

		DotPreconditions.checkArgument(null != systemAction, "System Action can not be null");
		DotPreconditions.checkArgument(null != contentType, "Content Type can not be null");
		DotPreconditions.checkArgument(null != workflowAction, "Workflow Action can not be null");
		DotPreconditions.checkArgument(!Host.HOST_VELOCITY_VAR_NAME.equals(contentType.variable()), "The Content Type can not be a Host");

		final  List<WorkflowScheme> contentTypeSchemes = this.findSchemesForContentType(contentType);

		if (UtilMethods.isSet(contentTypeSchemes)) {

			if (contentTypeSchemes.stream().noneMatch(scheme -> scheme.getId().equals(workflowAction.getSchemeId()))) {
                throw new IllegalArgumentException(String.format("Workflow Action '%s' [%s] doesn't belong to Content" +
                                " Type '%s' [%s] or any other Content Type", workflowAction.getName(), workflowAction.getId(),
                        contentType.name(), contentType.id()));
            }
		} else {
            throw new IllegalArgumentException(String.format("Content Type '%s' [%s] doesn't have any associated " +
                    "Workflow Schemes", contentType.name(), contentType.id()));
        }

		Logger.info(this, "Mapping the systemAction: " + systemAction +
				", workflowAction: " + workflowAction.getName() + " and contentType: " + contentType.variable());

		final SystemActionWorkflowActionMapping mapping =
				new SystemActionWorkflowActionMapping(UUIDGenerator.generateUuid(),
						systemAction, workflowAction, contentType);
		return this.workFlowFactory.saveSystemActionWorkflowActionMapping(mapping);
	}


	/**
	 * Maps a {@link SystemAction} to a {@link WorkflowAction} for a {@link WorkflowScheme}
	 * @param systemAction   {@link SystemAction}   System Action to mapping
	 * @param workflowAction {@link WorkflowAction} Workflow Action to map to the SystemAction
	 * @param workflowScheme {@link WorkflowScheme} The Map is associated to a scheme
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Override
	@WrapInTransaction
	public SystemActionWorkflowActionMapping mapSystemActionToWorkflowActionForWorkflowScheme (final SystemAction systemAction, final WorkflowAction workflowAction,
														   final WorkflowScheme workflowScheme) throws DotDataException {

		DotPreconditions.checkArgument(null != systemAction, "System Action can not be null");
		DotPreconditions.checkArgument(null != workflowScheme, "Workflow Scheme can not be null");
		DotPreconditions.checkArgument(null != workflowAction, "Workflow Action can not be null");
		DotPreconditions.checkArgument(workflowAction.getSchemeId().equals(workflowScheme.getId()), "The Workflow Action has to belong the workflow Scheme");
		final SystemActionWorkflowActionMapping mapping =
				new SystemActionWorkflowActionMapping(UUIDGenerator.generateUuid(),
						systemAction, workflowAction, workflowScheme);

		return this.workFlowFactory.saveSystemActionWorkflowActionMapping(mapping);
	}

	/**
	 * Finds the {@link SystemActionWorkflowActionMapping}'s associated to a {@link ContentType}
	 * @param contentType {@link ContentType} to be processed
	 * @param user {@link User} t user used to check permissions
	 * @return List of SystemActionWorkflowActionMapping
	 */
	@Override
	@CloseDBIfOpened
	public List<SystemActionWorkflowActionMapping> findSystemActionsByContentType (final ContentType contentType, final User user) throws DotSecurityException, DotDataException {

		final List<Map<String, Object>> mappings = this.workFlowFactory.findSystemActionsByContentType (contentType);
		final ImmutableList.Builder<SystemActionWorkflowActionMapping> actionsBuilder =
				new ImmutableList.Builder<>();

		for (final Map<String, Object> rowMap : mappings) {

            actionsBuilder.add(this.toSystemActionWorkflowActionMapping(rowMap, contentType, user));
		}

		return actionsBuilder.build();
	}

	@Override
	@CloseDBIfOpened
	public Map<String, List<WorkflowScheme>> findSchemesMapForContentType(final List<ContentType> contentTypes)  throws DotDataException {

		final ImmutableMap.Builder<String, List<WorkflowScheme>> schemesMapBuilder =
				new ImmutableMap.Builder<>();

		for (final ContentType contentType : contentTypes) {

			schemesMapBuilder.put(contentType.variable(), this.findSchemesForContentType(contentType));
		}

		return schemesMapBuilder.build();
	}

	@Override
	@CloseDBIfOpened
	public Map<String, List<SystemActionWorkflowActionMapping>> findSystemActionsMapByContentType(final List<ContentType> contentTypes,
																								  final User user)
			throws DotDataException, DotSecurityException {

		final Map<String, List<Map<String, Object>>> mappingsMap =
				this.workFlowFactory.findSystemActionsMapByContentType (contentTypes);
		final ImmutableMap.Builder<String, List<SystemActionWorkflowActionMapping>> actionsMapBuilder =
				new ImmutableMap.Builder<>();
		final Map<String, ContentType> contentTypeMap = contentTypes.stream()
				.collect(Collectors.toMap(contentType->contentType.variable(), contentType -> contentType));

		for (final Map.Entry<String, List<Map<String, Object>>> entry : mappingsMap.entrySet()) {

			final ImmutableList.Builder<SystemActionWorkflowActionMapping> mappingListBuilder =
					new ImmutableList.Builder<>();

			for (final Map<String, Object> rowMap : entry.getValue()) {

				mappingListBuilder.add(this.toSystemActionWorkflowActionMapping(
						rowMap, contentTypeMap.get(entry.getKey()), user));
			}

			actionsMapBuilder.put(entry.getKey(), mappingListBuilder.build());
		}

		return actionsMapBuilder.build();
	}

	/**
	 * Finds the {@link SystemActionWorkflowActionMapping}'s associated to a {@link WorkflowScheme}
	 * @param workflowScheme {@link WorkflowScheme}
	 * @param user {@link User}  user used to check permissions
	 * @return List of SystemActionWorkflowActionMapping
	 */
	@Override
	@CloseDBIfOpened
	public List<SystemActionWorkflowActionMapping> findSystemActionsByScheme(final WorkflowScheme workflowScheme, final User user) throws DotDataException, DotSecurityException {

        final List<Map<String, Object>> mappings = this.workFlowFactory.findSystemActionsByScheme (workflowScheme);
        final ImmutableList.Builder<SystemActionWorkflowActionMapping> actionsBuilder =
                new ImmutableList.Builder<>();

        for (final Map<String, Object> rowMap : mappings) {

            actionsBuilder.add(this.toSystemActionWorkflowActionMapping(rowMap, workflowScheme, user));
        }

        return actionsBuilder.build();
	}

	@Override
	@CloseDBIfOpened
	public Optional<SystemActionWorkflowActionMapping> findSystemActionByScheme(final SystemAction systemAction,
																				final WorkflowScheme workflowScheme,
																				final User user) throws DotSecurityException, DotDataException {

		final List<Map<String, Object>> mappings = this.workFlowFactory.findSystemActionsByScheme (workflowScheme);
		for (final Map<String, Object> rowMap : mappings) {

			final SystemActionWorkflowActionMapping mapping =
					this.toSystemActionWorkflowActionMapping(rowMap, workflowScheme, user);
			if (Objects.nonNull(mapping) && mapping.getSystemAction().equals(systemAction)) {
				return Optional.ofNullable(mapping);
			}
		}

		return Optional.empty();
	}

	@Override
	@CloseDBIfOpened
	public Optional<SystemActionWorkflowActionMapping> findSystemActionByContentType(final WorkflowAPI.SystemAction systemAction,
																			  final ContentType contentType, final User user)
			throws DotDataException, DotSecurityException {

		final Map<String, Object> mappingRow = this.workFlowFactory.findSystemActionByContentType(systemAction, contentType);
		return Optional.ofNullable(this.toSystemActionWorkflowActionMapping(mappingRow, contentType, user));
	}

	/**
	 * Tries to find a {@link WorkflowAction} based on a {@link Contentlet} and {@link SystemAction}, first will find a workflow action
	 * associated to the {@link Contentlet} {@link ContentType}, if there is not any match, will tries to find by {@link WorkflowScheme}
	 * if not any, Optional returned will be empty.
	 * @param contentlet    {@link Contentlet}   contentlet will helps to find by content type or associated schemes
	 * @param systemAction  {@link SystemAction} action to find possible mapped actions
	 * @param user {@link User} user used to check permissions
	 * @return Optional WorkflowAction, present if exists action associated to the search criterias
	 */
	@Override
	@CloseDBIfOpened
	public Optional<WorkflowAction> findActionMappedBySystemActionContentlet (final Contentlet contentlet,
                                                                              final SystemAction systemAction,
                                                                              final User user) throws DotDataException, DotSecurityException {

	    final ContentType contentType        = contentlet.getContentType();
        final Map<String, Object> mappingRow =
                this.workFlowFactory.findSystemActionByContentType(systemAction, contentType);
        if (UtilMethods.isSet(mappingRow)) {

        	final String workflowActionId       = (String)mappingRow.get("workflow_action");
			final Optional<WorkflowAction> workflowAction = this.findActionAvailable(contentlet, workflowActionId, user);
			if (workflowAction.isPresent()) {
				return workflowAction;
			} else {

				Logger.warn(this, "Workflow Action Id: " + workflowActionId +
						", used as default for: " + contentType.variable() + " doesn't exist or is not available");
			}
        }

		return this.findActionAvailableOnSchemes(contentlet, contentType, systemAction, user);
	}

	private Optional<WorkflowAction> findActionAvailableOnSchemes (final Contentlet contentlet, final ContentType contentType,
																   final SystemAction systemAction,
																   final User user) throws DotSecurityException, DotDataException {

		final List<WorkflowScheme> schemes          = this.findSchemesForContentType(contentType);

		if (UtilMethods.isSet(schemes)) {
			final List<Map<String, Object>> unsortedMappingRows =
					this.workFlowFactory.findSystemActionsBySchemes(systemAction, schemes);

			if (UtilMethods.isSet(unsortedMappingRows)) {

				final List<Map<String, Object>> mappingRows = new ArrayList<>(unsortedMappingRows);
				mappingRows.sort(this::compareScheme);
				return this.findActionAvailable(contentlet,
						(String) mappingRows.get(0).get("workflow_action"), user);
			}
		}

		return Optional.empty();
	}

	/*
	 * This comparator will give precedence to the System Workflow to the rest of them
	 */
	private int compareScheme (final Map<String, Object> map1, final Map<String, Object> map2) {

		final String schemeId1 = (String) map1.get("scheme_or_content_type");
		final String schemeId2 = (String) map2.get("scheme_or_content_type");
		final boolean isSystemWorkflow1 = SystemWorkflowConstants.SYSTEM_WORKFLOW_ID.equals(schemeId1);
		final boolean isSystemWorkflow2 = SystemWorkflowConstants.SYSTEM_WORKFLOW_ID.equals(schemeId2);

		return  isSystemWorkflow1? -1:
				isSystemWorkflow2? 1: 0;
	}

	private Optional<WorkflowAction> findActionAvailable (final Contentlet contentlet,
														  final String workflowActionId, final User user) throws DotSecurityException, DotDataException {

		final WorkflowAction workflowAction = this.findAction(workflowActionId, user);
		return null != workflowAction && this.isActionAvailable(contentlet, user, workflowActionId)?
					Optional.of(workflowAction):Optional.empty();
	}

	@Override
	@CloseDBIfOpened
	public Optional<SystemActionWorkflowActionMapping> findSystemActionByIdentifier(final String identifier, final User user)
			throws DotDataException, DotSecurityException {

		final Map<String, Object> mapping =
				this.workFlowFactory.findSystemActionByIdentifier (identifier);

		return UtilMethods.isSet(mapping)?
				Optional.ofNullable(toSystemActionWorkflowActionMapping(
						mapping, this.toOwner((String)mapping.get("scheme_or_content_type")), user))
				:Optional.empty();
	}

	@Override
	@CloseDBIfOpened
	public List<SystemActionWorkflowActionMapping> findSystemActionsByWorkflowAction(final WorkflowAction workflowAction, final User user)
			throws DotDataException, DotSecurityException {

		final ImmutableList.Builder<SystemActionWorkflowActionMapping> mappingBuilder =
				new ImmutableList.Builder<>();
		final List<Map<String, Object>> mappings =
				this.workFlowFactory.findSystemActionsByWorkflowAction (workflowAction);

		if(UtilMethods.isSet(mappings)) {

			for (final Map<String, Object> mappingRow : mappings) {

				final SystemActionWorkflowActionMapping mapping = toSystemActionWorkflowActionMapping(
						mappingRow, this.toOwner((String) mappingRow.get("scheme_or_content_type")), user);

				if (null != mapping) {

					mappingBuilder.add(mapping);
				}
			}
		}

		return mappingBuilder.build();
	}

	@Override
	@CloseDBIfOpened
	public boolean hasSaveActionlet(final WorkflowAction action) {

		return this.hasActionlet(action, Actionlet::save);
	}

	@Override
	@CloseDBIfOpened
	public boolean hasPublishActionlet(final WorkflowAction action) {

		return this.hasActionlet(action, Actionlet::publish);
	}

	@Override
	@CloseDBIfOpened
	public boolean hasUnpublishActionlet(final WorkflowAction action) {

		return this.hasActionlet(action, Actionlet::unpublish);
	}

	@Override
	@CloseDBIfOpened
	public boolean hasArchiveActionlet(final WorkflowAction action) {

		return this.hasActionlet(action, Actionlet::archive);
	}

	@Override
	@CloseDBIfOpened
	public boolean hasUnarchiveActionlet(final WorkflowAction action) {

		return this.hasActionlet(action, Actionlet::unarchive);
	}

	@Override
	@CloseDBIfOpened
	public boolean hasDeleteActionlet(final WorkflowAction action) {

		return this.hasActionlet(action, Actionlet::delete);
	}

	@Override
	@CloseDBIfOpened
	public boolean hasDestroyActionlet(final WorkflowAction action) {

		return this.hasActionlet(action, Actionlet::destroy);
	}

	@CloseDBIfOpened
	@Override
	public long countAllSchemasSteps(final User user) throws DotDataException, DotSecurityException {
		try {
			this.isUserAllowToModifiedWorkflow(user);
		} catch (WorkflowPortletAccessException | InvalidLicenseException e) {
			throw new DotSecurityException(e.getMessage(), e);
		}

		return workFlowFactory.countAllSchemasSteps();
	}

	@CloseDBIfOpened
	@Override
	public long countAllSchemasActions(final User user) throws DotDataException, DotSecurityException {
		try {
			this.isUserAllowToModifiedWorkflow(user);
		} catch (WorkflowPortletAccessException | InvalidLicenseException e) {
			throw new DotSecurityException(e.getMessage(), e);
		}

		return workFlowFactory.countAllSchemasActions();
	}

	@CloseDBIfOpened
	@Override
	public long countAllSchemasSubActions(final User user) throws DotDataException, DotSecurityException {
		try {
			this.isUserAllowToModifiedWorkflow(user);
		} catch (WorkflowPortletAccessException | InvalidLicenseException e) {
			throw new DotSecurityException(e.getMessage(), e);
		}

		return workFlowFactory.countAllSchemasSubActions();
	}

	@CloseDBIfOpened
	@Override
	public long countAllSchemasUniqueSubActions(final User user) throws DotDataException, DotSecurityException {
		try {
			this.isUserAllowToModifiedWorkflow(user);
		} catch (WorkflowPortletAccessException | InvalidLicenseException e) {
			throw new DotSecurityException(e.getMessage(), e);
		}

		return workFlowFactory.countAllSchemasUniqueSubActions();
	}

	@Override
	public WorkflowTask createWorkflowTask(final Contentlet contentlet, final User user,
									final WorkflowStep workflowStep, final String title, String description) throws DotDataException {

		final WorkflowTask task = new WorkflowTask();
		final Date now          = new Date();

      task.setTitle(title);
		task.setDescription(description);
		task.setAssignedTo(APILocator.getRoleAPI().getUserRole(user).getId());
		task.setModDate(now);
		task.setCreationDate(now);
		task.setCreatedBy(user.getUserId());
		task.setStatus(workflowStep.getId());
		task.setDueDate(null);
		task.setWebasset(contentlet.getIdentifier());
		task.setLanguageId(contentlet.getLanguageId());

		return task;
	}

	private boolean hasActionlet(final WorkflowAction action, final Predicate<Actionlet> successFilter) {

		try {

			final List<WorkflowActionClass> actionClasses = this.workFlowFactory.findActionClasses(action);
			if (UtilMethods.isSet(actionClasses)) {

				for (final WorkflowActionClass actionClass : actionClasses) {

					final Actionlet actionlet = AnnotationUtils.
							getBeanAnnotation(this.getActionletClass(actionClass.getClazz()), Actionlet.class);
					if (null != actionlet && successFilter.test(actionlet)) {
						return true;
					}
				}
			}
		} catch (DotDataException e) {
			return false;
		}

		return false;
	}

	@Override
	@WrapInTransaction
	public Optional<SystemActionWorkflowActionMapping> deleteSystemAction(final SystemActionWorkflowActionMapping mapping)  throws DotDataException  {

		return this.workFlowFactory.deleteSystemAction(mapping)?Optional.ofNullable(mapping):Optional.empty();
	}

	private Object toOwner(final String schemeOrContentType) throws DotSecurityException, DotDataException {

		return UUIDUtil.isUUID(schemeOrContentType)?
				this.findScheme(schemeOrContentType):this.contentTypeAPI.find(schemeOrContentType);
	}

	private SystemActionWorkflowActionMapping toSystemActionWorkflowActionMapping(final Map<String, Object> rowMap, final Object owner, final User user)
            throws DotSecurityException, DotDataException {

		if (UtilMethods.isSet(rowMap)) {

			final String identifier = (String) rowMap.get("id");
			final SystemAction systemAction = SystemAction.fromString((String) rowMap.get("action"));
			final WorkflowAction workflowAction = this.findAction((String) rowMap.get("workflow_action"), user);

			return new SystemActionWorkflowActionMapping(identifier, systemAction, workflowAction, owner);
		}

		return null;
    }

	@Override
	@CloseDBIfOpened
	public int countWorkflowSchemes(final User user) {
		isUserAllowToModifiedWorkflow(user);
		return workFlowFactory.countWorkflowSchemes(false);
	}

	@Override
	@CloseDBIfOpened
	public int countWorkflowSchemesIncludeArchived(final User user) {
		isUserAllowToModifiedWorkflow(user);
		return workFlowFactory.countWorkflowSchemes(true);
	}

}
