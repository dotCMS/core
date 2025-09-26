package com.dotcms.rest.api.v1.workflow;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.rest.PATCH;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.IncludePermissions;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.authentication.RequestUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.workflow.form.BulkActionForm;
import com.dotcms.workflow.form.FireActionByNameForm;
import com.dotcms.workflow.form.FireActionForm;
import com.dotcms.workflow.form.FireBulkActionsForm;
import com.dotcms.workflow.form.FireMultipleActionForm;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowActionStepBean;
import com.dotcms.workflow.form.WorkflowActionStepForm;
import com.dotcms.workflow.form.WorkflowActionletActionBean;
import com.dotcms.workflow.form.WorkflowActionletActionForm;
import com.dotcms.workflow.form.WorkflowCopyForm;
import com.dotcms.workflow.form.WorkflowReorderBean;
import com.dotcms.workflow.form.WorkflowReorderWorkflowActionStepForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowSchemeImportObjectForm;
import com.dotcms.workflow.form.WorkflowSchemesForm;
import com.dotcms.workflow.form.WorkflowStepAddForm;
import com.dotcms.workflow.form.WorkflowStepUpdateForm;
import com.dotcms.workflow.form.WorkflowSystemActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.HttpHeaders;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.time.StopWatch;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.dotcms.rest.ResponseEntityView.OK;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.DotLambdas.not;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.FAIL_ACTION_CALLBACK;
import static com.dotmarketing.portlets.workflows.business.WorkflowAPI.SUCCESS_ACTION_CALLBACK;

/**
 * Encapsulates all the interaction with dotCMS Workflows. This REST Endpoint allows you to execute
 * operations such as:
 * <ul>
 *     <li>Create schemes, action into schemes, steps into schemes.</li>
 *     <li>Associated actions to steps.</li>
 *     <li>Get schemes, steps, actions and relation information.</li>
 *     <li>Get available actions for a content.</li>
 *     <li>Fire an action for a single content or bulk (action for a several contents).</li>
 * </ul>
 * <p>You can find more information in the
 * {@code dotcms-postman/src/main/resources/postman/documentation/Workflow_Resource_Tests.json} file. It's a complete
 * collection of examples on how to interact with this Resource.</p>
 *
 * @author jsanca
 * @since Dec 6th, 2017
 */
@Path("/v1/workflow")
@Tag(name = "Workflow")
public class WorkflowResource {

    public  final static String VERSION       = "1.0";
    private static final String LISTING       = "listing";
    private static final String EDITING       = "editing";
    private static final String ASSIGN        = "assign";
    private static final String COMMENTS      = "comments";
    private static final String PUBLISH_DATE  = "publishDate";
    private static final String PUBLISH_TIME  = "publishTime";
    private static final String EXPIRE_DATE   = "expireDate";
    private static final String EXPIRE_TIME   = "expireTime";
    private static final String NEVER_EXPIRE  = "neverExpire";
    private static final String BINARY_FIELDS = "binaryFields";
    private static final String PREFIX_BINARY = "binary";
    private static final String ACTION_NAME   = "actionName";
    private static final String CONTENTLET    = "contentlet";
    private static final int CONTENTLETS_LIMIT = 100000;
    private static final String WORKFLOW_SUBMITTER = "workflow_submitter";


    private final WorkflowHelper   workflowHelper;
    private final ContentHelper    contentHelper;
    private final WebResource      webResource;
    private final WorkflowAPI      workflowAPI;
    private final ResponseUtil     responseUtil;
    private final ContentletAPI    contentletAPI;
    private final PermissionAPI    permissionAPI;
    private final WorkflowImportExportUtil workflowImportExportUtil;
    private final MultiPartUtils   multiPartUtils;
    private final SystemActionApiFireCommandFactory systemActionApiFireCommandProvider;
    private final Set<String>   validRenderModeSet = ImmutableSet.of(LISTING, EDITING);


    /**
     * Default constructor.
     */
    @SuppressWarnings("unused")
    public WorkflowResource() {
        this(WorkflowHelper.getInstance(),
                ContentHelper.getInstance(),
                APILocator.getWorkflowAPI(),
                APILocator.getContentletAPI(),
                ResponseUtil.INSTANCE,
                APILocator.getPermissionAPI(),
                WorkflowImportExportUtil.getInstance(),
                new MultiPartUtils(),
                new WebResource(),
                SystemActionApiFireCommandFactory.getInstance());
    }

    @VisibleForTesting
    WorkflowResource(final WorkflowHelper workflowHelper,
                     final ContentHelper    contentHelper,
                     final WorkflowAPI      workflowAPI,
                     final ContentletAPI    contentletAPI,
                     final ResponseUtil     responseUtil,
                     final PermissionAPI    permissionAPI,
                     final WorkflowImportExportUtil workflowImportExportUtil,
                     final MultiPartUtils   multiPartUtils,
                     final WebResource webResource,
                     final SystemActionApiFireCommandFactory systemActionApiFireCommandProvider) {

        this.workflowHelper           = workflowHelper;
        this.contentHelper            = contentHelper;
        this.webResource              = webResource;
        this.responseUtil             = responseUtil;
        this.workflowAPI              = workflowAPI;
        this.permissionAPI            = permissionAPI;
        this.contentletAPI            = contentletAPI;
        this.multiPartUtils           = multiPartUtils;
        this.workflowImportExportUtil = workflowImportExportUtil;
        this.systemActionApiFireCommandProvider =
                systemActionApiFireCommandProvider;

    }

    /**
     * Returns all schemes non-archived associated to a content type. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param contentTypeId String content type id to get the schemes associated to it, is this is null return
     *                      all the schemes (archived and non-archived).
     * @return Response
     */
    @GET
    @Path("/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findSchemes(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @QueryParam("contentTypeId") final String  contentTypeId,
                                      @DefaultValue("true") @QueryParam("showArchive")  final boolean showArchived) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId);
            final List<WorkflowScheme> schemes = (null != contentTypeId) ?
                    ((showArchived)?
                            this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser()):
                            this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser())
                                    .stream().filter(not(WorkflowScheme::isArchived)).collect(CollectionsUtils.toImmutableList())):
                    this.workflowHelper.findSchemes(showArchived);

            return Response.ok(new ResponseEntityView<>(schemes)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findSchemes exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSchemes.

    /**
     * Returns all actionlets classes
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/actionlets")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionlets(@Context final HttpServletRequest request) {

        this.webResource.init
                (null, true, request, true, null);
        try {

            Logger.debug(this,
                    ()->"Getting the workflow actionlets");

            final List<WorkFlowActionlet> actionlets = this.workflowAPI.findActionlets();
            return Response.ok(new ResponseEntityView<>(actionlets)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findActionlets exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionlets.

    /**
     * Returns all actionlets associated to a workflow action. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String action id to get the actionlet associated to it, is this is null return
     *                      all the WorkflowActionClass
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}/actionlets")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionletsByAction(@Context final HttpServletRequest request,
                                                 @PathParam("actionId") final String  actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {

            Logger.debug(this,
                    ()->"Getting the workflow actionlets for the action: " + actionId);

            final WorkflowAction action = this.workflowAPI.findAction(actionId, initDataObject.getUser());
            DotPreconditions.notNull(action, ()->"Action: " + actionId + ", does not exists", NotFoundInDbException.class);

            final List<WorkflowActionClass> actionClasses = this.workflowAPI.findActionClasses(action);
            return Response.ok(new ResponseEntityView<>(actionClasses)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findActionletsByAction exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionletsByAction.

    /**
     * Returns all schemes for the content type and include schemes non-archive . 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/schemes/schemescontenttypes/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAllSchemesAndSchemesByContentType(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("contentTypeId") final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId
                            + " and including All Schemes");
            final List<WorkflowScheme> schemes = this.workflowHelper.findSchemes();
            final List<WorkflowScheme> contentTypeSchemes = this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser());

            return Response.ok(new ResponseEntityView(
                    new SchemesAndSchemesContentTypeView(schemes, contentTypeSchemes)))
                    .build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findAllSchemesAndSchemesByContentType exception message: " + e
                            .getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);

        }
    } // findAllSchemesAndSchemesByContentType.

    /**
     * Return Steps associated to the scheme, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/steps")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findStepsByScheme(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @PathParam("schemeId") final String schemeId) {

        this.webResource.init
                (null, request, response, true, null);

        try {
            Logger.debug(this, "Getting the workflow steps for the scheme: " + schemeId);
            final List<WorkflowStep> steps = this.workflowHelper.findSteps(schemeId);
            return Response.ok(new ResponseEntityView<>(steps)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findStepsByScheme exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);

        }
    } // findSteps.

    /**
     * Finds the Workflow Actions that are available for a specific Contentlet Inode. Here's an
     * example of how you can use this method:
     * <pre>
     *     GET http://localhost:8080/api/v1/workflow/contentlet/{CONTENTLET-INODE}/actions?renderMode={editing|listing}
     * </pre>
     *
     * @param request    The current instance of the {@link HttpServletRequest}.
     * @param inode      The Inode of the Contentlet.
     * @param renderMode This is a case-insensitive optional parameter. By default, this method will
     *                   run EDITING rendering mode. The available modes are specified via the
     *                   {{@link #validRenderModeSet}} variable.
     *
     * @return Response A {@link Response} object that contains the available actions for the
     * specified Contentlet.
     */
    @GET
    @Path("/contentlet/{inode}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAvailableActions(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               @PathParam("inode")  final String inode,
                                               @QueryParam("renderMode") final String renderMode) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, ()->"Getting the available actions for the contentlet inode: " + inode);

            this.workflowHelper.checkRenderMode (renderMode, initDataObject.getUser(), this.validRenderModeSet);

            final List<WorkflowAction> actions = this.workflowHelper.findAvailableActions(inode, initDataObject.getUser(),
                    LISTING.equalsIgnoreCase(renderMode)
                            ? WorkflowAPI.RenderMode.LISTING
                            : WorkflowAPI.RenderMode.EDITING);
            return Response.ok(new ResponseEntityView<>(actions.stream()
                    .map(this::toWorkflowActionView).collect(Collectors.toList()))).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), String.format("An error occurred when finding available" +
                            " Workflow Actions for Contentlet Inode '%s' in mode '%s': %s", inode, renderMode,
                    ExceptionUtil.getErrorMessage(e)), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findAvailableActions.

    private WorkflowActionView toWorkflowActionView(final WorkflowAction workflowAction) {

        return convertToWorkflowActionView(workflowAction);
    }

    /**
     * Takes the information from a Workflow Action and transforms it into a View object that can
     * display it in JSON notation appropriately. Keep in mind that any new property you add to the
     * Workflow Action class will need to be added here as well.
     *
     * @param workflowAction The {@link WorkflowAction} that will be transformed.
     *
     * @return The {@link WorkflowActionView} that contains the information from the Workflow
     * Action.
     */
    public static WorkflowActionView convertToWorkflowActionView(final WorkflowAction workflowAction) {
        final WorkflowActionView workflowActionView = new WorkflowActionView();
        workflowActionView.setId(workflowAction.getId());
        workflowActionView.setName(workflowAction.getName());
        workflowActionView.setStepId(workflowAction.getSchemeId());
        workflowActionView.setSchemeId(workflowAction.getSchemeId());
        workflowActionView.setCondition(workflowAction.getCondition());
        workflowActionView.setNextStep(workflowAction.getNextStep());
        workflowActionView.setNextAssign(workflowAction.getNextAssign());
        workflowActionView.setIcon(workflowAction.getIcon());
        workflowActionView.setRoleHierarchyForAssign(workflowAction.isRoleHierarchyForAssign());
        workflowActionView.setRequiresCheckout(workflowAction.isRoleHierarchyForAssign());
        workflowActionView.setAssignable(workflowAction.isAssignable());
        workflowActionView.setCommentable(workflowAction.isCommentable());
        workflowActionView.setOrder(workflowAction.getOrder());
        workflowActionView.setSaveActionlet(workflowAction.hasSaveActionlet());
        workflowActionView.setPublishActionlet(workflowAction.hasPublishActionlet());
        workflowActionView.setUnpublishActionlet(workflowAction.hasUnpublishActionlet());
        workflowActionView.setArchiveActionlet(workflowAction.hasArchiveActionlet());
        workflowActionView.setPushPublishActionlet(workflowAction.hasPushPublishActionlet());
        workflowActionView.setUnarchiveActionlet(workflowAction.hasUnarchiveActionlet());
        workflowActionView.setDeleteActionlet(workflowAction.hasDeleteActionlet());
        workflowActionView.setDestroyActionlet(workflowAction.hasDestroyActionlet());
        workflowActionView.setShowOn(workflowAction.getShowOn());
        workflowActionView.setActionInputs(createActionInputViews(workflowAction));
        workflowActionView.setMetadata(workflowAction.getMetadata());
        return workflowActionView;
    }

    private static List<ActionInputView> createActionInputViews (final WorkflowAction workflowAction) {

        final List<ActionInputView> actionInputViews = new ArrayList<>();

        if (workflowAction.isAssignable()) {

            actionInputViews.add(new ActionInputView("assignable", Collections.emptyMap()));
        }

        if (workflowAction.isCommentable()) {

            actionInputViews.add(new ActionInputView("commentable", Collections.emptyMap()));
        }

        if (workflowAction.hasPushPublishActionlet()) {

            actionInputViews.add(new ActionInputView("pushPublish", Collections.emptyMap()));
        }

        // Has a move actionlet but the path is empty
        if (workflowAction.hasMoveActionletActionlet() && !workflowAction.hasMoveActionletHasPathActionlet()) {

            actionInputViews.add(new ActionInputView("moveable", Collections.emptyMap()));
        }

        return actionInputViews;
    }

    /**
     * Get the bulk actions based on the {@link BulkActionForm}
     * @param request HttpServletRequest
     * @param bulkActionForm String
     * @return Response
     */
    @POST
    @Path("/contentlet/actions/bulk")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getBulkActions(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         final BulkActionForm bulkActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            DotPreconditions.notNull(bulkActionForm,"Expected Request body was empty.");
            Logger.debug(this, ()-> "Getting the bulk actions for the contentlets inodes: " + bulkActionForm);
            return Response.ok(new ResponseEntityView
                    (this.workflowHelper.findBulkActions(initDataObject.getUser(), bulkActionForm)))
                    .build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on getBulkActions, bulkActionForm: " + bulkActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // getBulkActions.

    @PUT
    @Path("/contentlet/actions/bulk/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final void fireBulkActions(@Context final HttpServletRequest request,
                                      @Suspended final AsyncResponse asyncResponse,
                                      final FireBulkActionsForm fireBulkActionsForm) {

        final InitDataObject initDataObject = this.webResource.init(null, request, new EmptyHttpResponse(), true, null);
        Logger.debug(this, ()-> "Fire bulk actions: " + fireBulkActionsForm);
        try {
            // check the form
            DotPreconditions.notNull(fireBulkActionsForm,"Expected Request body was empty.");
            ResponseUtil.handleAsyncResponse(() -> {
                try {
                    final BulkActionsResultView view = workflowHelper
                            .fireBulkActions(fireBulkActionsForm, initDataObject.getUser());
                    return Response.ok( new ResponseEntityView(view)).build();
                } catch (Exception e) {
                    asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
                }
                return null;
            }, asyncResponse);

        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to fire bulk actions by : " +fireBulkActionsForm + ", exception message: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    }

    @POST
    @Path("/contentlet/actions/_bulkfire")
    @JSONP
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput fireBulkActions(@Context final HttpServletRequest request,
            final FireBulkActionsForm fireBulkActionsForm)
            throws DotDataException, DotSecurityException {
        final InitDataObject initDataObject = this.webResource
                .init(null, request, new EmptyHttpResponse(), true, null);

        final EventOutput eventOutput = new EventOutput();

        DotConcurrentFactory.getInstance().getSubmitter().submit(()-> {

            final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();


            fireBulkActionsForm.getPopupParamsBean()
                    .getAdditionalParamsMap().put(SUCCESS_ACTION_CALLBACK,
                            (Consumer<Long>) delta -> {
                                eventBuilder.name("success");
                                eventBuilder.data(Map.class,
                                        map("success", delta));
                                eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                                final OutboundEvent event = eventBuilder.build();
                                try {
                                    eventOutput.write(event);
                                } catch (Exception e) {
                                    throw new DotRuntimeException(e);
                                }
                            });

            fireBulkActionsForm.getPopupParamsBean()
                    .getAdditionalParamsMap().put(FAIL_ACTION_CALLBACK,
                            (BiConsumer<String, Exception>) (inode, e) -> {
                                eventBuilder.name("failure");
                                eventBuilder.data(Map.class,
                                        map("failure", inode));
                                eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                                final OutboundEvent event = eventBuilder.build();
                                try {
                                    eventOutput.write(event);
                                } catch (Exception e1) {
                                    throw new DotRuntimeException(e1);
                                }
                            });

            try {
                workflowHelper.fireBulkActionsNoReturn(fireBulkActionsForm, initDataObject.getUser());
                Logger.info(this, "finished");
            } catch (DotSecurityException | DotDataException e) {
                throw new DotRuntimeException(e);
            }
        });

        return eventOutput;
    }

    /**
     * Returns a single action, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @IncludePermissions
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAction(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, ()->"Finding the workflow action " + actionId);
            final WorkflowAction action = this.workflowHelper.findAction(actionId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(this.toWorkflowActionView(action))).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findAction, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // findAction.

    /**
     * Returns a single action condition evaluated, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}/condition")
    @JSONP
    @NoCache
    @IncludePermissions
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response evaluateActionCondition(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, ()->"Finding the workflow action " + actionId);

            final String evaluated = workflowHelper.evaluateActionCondition(actionId, initDataObject.getUser(), request, response);
            return Response.ok(new ResponseEntityView(evaluated)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on evaluateActionCondition, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findAction.

    /**
     * Returns a single action associated to the step, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionByStep(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @PathParam("stepId")   final String stepId,
                                           @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, "Getting the workflow action " + actionId + " for the step: " + stepId);
            final WorkflowAction action = this.workflowHelper.findAction(actionId, stepId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(action)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findAction, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionByStep.

    /**
     * Returns a collection of actions associated to the step, if step does not have any will returns 200 and an empty list.
     * 401 if the user does not have permission.
     * 404 if the stepId does not exists.
     * @param request  HttpServletRequest
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsByStep(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @PathParam("stepId")   final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        final User user = initDataObject.getUser();
        try {
            Logger.debug(this, "Getting the workflow actions for the step: " + stepId);
            final List<WorkflowAction> actions = this.workflowHelper.findActions(stepId, user);
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsByStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionByStep.

    /**
     * Returns a set of actions associated to the schemeId
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsByScheme(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this, "Getting the workflow actions: " + schemeId);
            final List<WorkflowAction> actions = this.workflowHelper.findActionsByScheme(schemeId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionsByScheme.

    /**
     * Returns a set of {@link WorkflowAction} associated to a set to schemes Id and {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @param request  HttpServletRequest
     * @param response {@link HttpServletResponse}
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}
     * @return Response
     */
    @POST
    @Path("/schemes/actions/{systemAction}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsBySchemesAndSystemAction(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam("systemAction") final WorkflowAPI.SystemAction systemAction,
                                                    final WorkflowSchemesForm workflowSchemesForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        DotPreconditions.notNull(workflowSchemesForm,"Expected Request body was empty.");
        try {

            Logger.debug(this, ()->"Getting the actions for the schemes: " + workflowSchemesForm.getSchemes()
                    + " and system action: " + systemAction);

            return Response.ok(new ResponseEntityView<>(
                    this.workflowHelper.findActions(
                            workflowSchemesForm.getSchemes(), systemAction, initDataObject.getUser())))
                    .build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsBySchemesAndSystemAction, schemes: " + workflowSchemesForm.getSchemes() +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSystemActionsByScheme.

    /**
     * Returns a set of {@link SystemActionWorkflowActionMapping} associated to the schemeId
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/system/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findSystemActionsByScheme(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this, "Getting the system actions for the scheme: " + schemeId);
            final List<SystemActionWorkflowActionMapping> systemActions =
                    this.workflowAPI.findSystemActionsByScheme(this.workflowAPI.findScheme(schemeId), initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(systemActions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findSystemActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSystemActionsByScheme.

    /**
     * Returns a set of {@link SystemActionWorkflowActionMapping} associated to the content type
     * @param request  HttpServletRequest
     * @param contentTypeVarOrId String
     * @return Response
     */
    @GET
    @Path("/contenttypes/{contentTypeVarOrId}/system/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findSystemActionsByContentType(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam("contentTypeVarOrId") final String contentTypeVarOrId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this, "Getting the system actions for the content type: " + contentTypeVarOrId);
            final User user = initDataObject.getUser();
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(contentTypeVarOrId);
            final List<SystemActionWorkflowActionMapping> systemActions =
                    this.workflowHelper.findSystemActionsByContentType(contentType, initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(systemActions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findSystemActionsByContentType, content type: " + contentTypeVarOrId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSystemActionsByScheme.

    /**
     * Will retrieve the references as a default {@link WorkflowAction}
     * @param request  HttpServletRequest
     * @param workflowActionId String
     * @return Response
     */
    @GET
    @Path("/system/actions/{workflowActionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getSystemActionsReferredByWorkflowAction(@Context final HttpServletRequest request,
                                                         @Context final HttpServletResponse response,
                                                         @PathParam("workflowActionId") final String workflowActionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this, ()->"Getting the system actions for the workflow action id: " + workflowActionId);
            final User user = initDataObject.getUser();
            final WorkflowAction workflowAction = this.workflowHelper.findAction(workflowActionId, user);
            final List<SystemActionWorkflowActionMapping> systemActions =
                    this.workflowAPI.findSystemActionsByWorkflowAction(workflowAction, user);
            return Response.ok(new ResponseEntityView<>(systemActions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on getSystemActionsReferredByWorkflowAction, workflowActionId: " + workflowActionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // getSystemActionsReferredByWorkflowAction.


    /**
     * Saves an {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}, by default the action is associated to the schema,
     * however if the stepId is set will be automatically associated to the step too.
     * @param request                     HttpServletRequest
     * @param response                    HttpServletResponse
     * @param workflowSystemActionForm    WorkflowActionForm
     * @return Response
     */
    @PUT
    @Path("/system/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveSystemAction(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     final WorkflowSystemActionForm workflowSystemActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            DotPreconditions.notNull(workflowSystemActionForm,"Expected Request body was empty.");
            Logger.debug(this, ()-> "Saving system action: " + workflowSystemActionForm.getSystemAction() +
                                            ", map to action id: " + workflowSystemActionForm.getActionId() +
                                            " and " + (UtilMethods.isSet(workflowSystemActionForm.getSchemeId())?
                                            "scheme: " + workflowSystemActionForm.getSchemeId():
                                            "var: "    + workflowSystemActionForm.getContentTypeVariable()));

            return Response.ok(new ResponseEntityView(
                    this.workflowHelper.mapSystemActionToWorkflowAction(workflowSystemActionForm, initDataObject.getUser())))
                    .build(); // 200
        }  catch (final Exception e) {

            Logger.error(this.getClass(),
                    "Exception on save, workflowActionForm: " + workflowSystemActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveSystemAction


    /**
     * Deletes an {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction}, by default the action is associated to the schema,
     * however if the stepId is set will be automatically associated to the step too.
     * @param request                     HttpServletRequest
     * @param response                    HttpServletResponse
     * @param identifier                  String
     * @return Response
     */
    @DELETE
    @Path("/system/actions/{identifier}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deletesSystemAction(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @PathParam("identifier") final String identifier) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, ()-> "Deleting system action: " + identifier);

            return Response.ok(new ResponseEntityView(
                    this.workflowHelper.deleteSystemAction(identifier, initDataObject.getUser())))
                    .build(); // 200
        }  catch (final Exception e) {

            Logger.error(this.getClass(),
                    "Exception on delete System Action, identifier: " + identifier +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveSystemAction

    /**
     * Saves an action, by default the action is associated to the schema, however if the stepId is set will be automatically associated to the step too.
     * @param request               HttpServletRequest
     * @param workflowActionForm    WorkflowActionForm
     * @return Response
     */
    @POST
    @Path("/actions")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveAction(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        WorkflowAction newAction;

        try {
            DotPreconditions.notNull(workflowActionForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving new workflow action: " + workflowActionForm.getActionName());
            newAction = this.workflowHelper.saveAction(workflowActionForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(newAction)).build(); // 200

        }  catch (final Exception e) {

            Logger.error(this.getClass(),
                    "Exception on save, workflowActionForm: " + workflowActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // save

    /**
     * Updates an existing action
     * @param request HttpServletRequest
     * @param actionId String
     * @param workflowActionForm WorkflowActionStepForm
     * @return Response
     */
    @PUT
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateAction(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("actionId") final String actionId,
                                       final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        try {
            DotPreconditions.notNull(workflowActionForm,"Expected Request body was empty.");
            Logger.debug(this, "Updating action with id: " + actionId);
            final WorkflowAction workflowAction = this.workflowHelper.updateAction(actionId, workflowActionForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(workflowAction)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on updateAction, actionId: " +actionId+", workflowActionForm: " + workflowActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Saves an action into a step
     * @param request HttpServletRequest
     * @param workflowActionStepForm WorkflowActionStepForm
     * @return Response
     */
    @POST
    @Path("/steps/{stepId}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveActionToStep(@Context final HttpServletRequest request,
                                           @Context final HttpServletResponse response,
                                           @PathParam("stepId")   final String stepId,
                                           final WorkflowActionStepForm workflowActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            DotPreconditions.notNull(workflowActionStepForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving a workflow action " + workflowActionStepForm.getActionId()
                    + " in to a step: " + stepId);
            this.workflowHelper.saveActionToStep(new WorkflowActionStepBean.Builder().stepId(stepId)
                    .actionId(workflowActionStepForm.getActionId()).build(), initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on saveActionToStep, stepId: "+stepId+", saveActionToStep: " + workflowActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveActionToStep

    /**
     * Saves an actionlet into an action,
     * if the Parameters (key/value) are not null nor empty, overrides them.
     * @param request HttpServletRequest
     * @param workflowActionletActionForm WorkflowActionletActionForm
     * @return Response
     */
    @POST
    @Path("/actions/{actionId}/actionlets")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveActionletToAction(@Context final HttpServletRequest request,
                                                @PathParam("actionId")   final String actionId,
                                                final WorkflowActionletActionForm workflowActionletActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {

            DotPreconditions.notNull(workflowActionletActionForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving a workflow actionlet " + workflowActionletActionForm.getActionletClass()
                    + " in to a action : " + actionId);
            this.workflowHelper.saveActionletToAction(new WorkflowActionletActionBean.Builder().actionId(actionId)
                            .actionletClass(workflowActionletActionForm.getActionletClass())
                            .order(workflowActionletActionForm.getOrder())
                            .parameters(workflowActionletActionForm.getParameters()).build()
                    , initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on saveActionletToAction, actionId: "+actionId+", saveActionletToAction: " + workflowActionletActionForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // saveActionletToAction



    /**
     * Deletes a step
     * @param request HttpServletRequest
     * @param stepId String
     */
    @DELETE
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final void deleteStep(@Context final HttpServletRequest request,
                                 @Suspended final AsyncResponse asyncResponse,
                                 @PathParam("stepId") final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, new EmptyHttpResponse(), true, null);

        try {
            Logger.debug(this, "Deleting the step: " + stepId);
            ResponseUtil.handleAsyncResponse(this.workflowHelper.deleteStep(stepId, initDataObject.getUser()), asyncResponse);
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    } // deleteStep

    /**
     * Deletes an action associated to the step
     * @param request                   HttpServletRequest
     * @param stepId                   String
     * @return Response
     */
    @DELETE
    @Path("/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteAction(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("actionId") final String actionId,
                                       @PathParam("stepId")   final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, "Deleting the action: " + actionId + " for the step: " + stepId);
            this.workflowHelper.deleteAction(actionId, stepId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteAction, actionId: "+actionId+", stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Deletes an action associated to the scheme and all references into steps
     * @param request                   HttpServletRequest
     * @param actionId                  String
     * @return Response
     */
    @DELETE
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteAction(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, "Deleting the action: " + actionId);
            this.workflowHelper.deleteAction(actionId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Deletes an actionlet associated
     * @param request                   HttpServletRequest
     * @param actionletId                  String
     * @return Response
     */
    @DELETE
    @Path("/actionlets/{actionletId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteActionlet(@Context final HttpServletRequest request,
                                          @PathParam("actionletId") final String actionletId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

        try {

            Logger.debug(this, "Deleting the actionletId: " + actionletId);

            DotPreconditions.notNull(actionletId,
                    ()-> "Not is not allowed for the parameter actionletId on the method deleteActionlet.",
                    IllegalArgumentException.class);

            final WorkflowActionClass actionClass = this.workflowAPI.findActionClass(actionletId);

            DotPreconditions.notNull(actionClass,
                    ()-> "The Actionlet: " + actionletId + ", does not exists",
                    DoesNotExistException.class);

            this.workflowAPI.deleteActionClass(actionClass, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on deleteActionlet, actionletId: " + actionletId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // deleteAction

    /**
     * Change the order of the steps in a scheme
     * @param request                           HttpServletRequest
     * @param stepId                            String stepid to reorder
     * @param order                             int    order for the step
     * @return Response
     */
    @PUT
    @Path("/reorder/step/{stepId}/order/{order}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response reorderStep(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                        @PathParam("stepId")   final String stepId,
                                      @PathParam("order")    final int order) {
        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {

            Logger.debug(this, "Doing reordering of step: " + stepId + ", order: " + order);
            this.workflowHelper.reorderStep(stepId, order, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "WorkflowPortletAccessException on reorderStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // reorderStep


    /**
     * Updates an existing step
     * @param request HttpServletRequest
     * @param stepId String
     * @param stepForm WorkflowStepUpdateForm
     * @return Response
     */
    @PUT
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateStep(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @NotNull @PathParam("stepId") final String stepId,
                                     final WorkflowStepUpdateForm stepForm) {
        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        Logger.debug(this, "updating step for scheme with stepId: " + stepId);
        try {
            DotPreconditions.notNull(stepForm,"Expected Request body was empty.");
            final WorkflowStep step = this.workflowHelper.updateStep(stepId, stepForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(step)).build();
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "WorkflowPortletAccessException on updateStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // updateStep

    /**
     * Creates a new step into a workflow
     * @param request HttpServletRequest
     * @param newStepForm WorkflowStepAddForm
     * @return Response
     */
    @POST
    @Path("/steps")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response addStep(@Context final HttpServletRequest request,
                                  @Context final HttpServletResponse response,
                                  final WorkflowStepAddForm newStepForm) {
        String schemeId = null;
        try {
            DotPreconditions.notNull(newStepForm,"Expected Request body was empty.");
            schemeId = newStepForm.getSchemeId();
            final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
            Logger.debug(this, "updating step for scheme with schemeId: " + schemeId);
            final WorkflowStep step = this.workflowHelper.addStep(newStepForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(step)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on addStep, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }


    /**
     * Retrieves a step given its id.
     * @param request HttpServletRequest
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findStepById(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @NotNull @PathParam("stepId") final String stepId) {
        this.webResource.init(null, request, response, true, null);
        Logger.debug(this, "finding step by id stepId: " + stepId);
        try {
            final WorkflowStep step = this.workflowHelper.findStepById(stepId);
            return Response.ok(new ResponseEntityView(step)).build();
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findStepById, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Fires a workflow action by name and multi part, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param multipart {@link FormDataMultiPart} Multipart form (if an inode is set, this param is not ignored).
     *
     * @return Response
     */
    @PUT
    @Path("/actions/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Fire action by name multipart",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "404", description = "Action not found")})
    public final Response fireActionByNameMultipart(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @QueryParam("inode")            final String inode,
                                              @QueryParam("identifier")       final String identifier,
                                              @QueryParam("indexPolicy")      final String indexPolicy,
                                              @DefaultValue("-1") @QueryParam("language")         final String   language,
                                              final FormDataMultiPart multipart) {

      final InitDataObject initDataObject = new WebResource.InitBuilder()
          .requestAndResponse(request, new MockHttpResponse())
          .requiredAnonAccess(AnonymousAccess.WRITE)
          .init();
        String actionId = null;

        try {

            Logger.debug(this, ()-> "On Fire Action: inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            final FireActionByNameForm fireActionForm = this.processForm (multipart, initDataObject.getUser());
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            actionId = this.workflowHelper.getActionIdOnList
                    (fireActionForm.getActionName(), contentlet, initDataObject.getUser());

            Logger.debug(this, "fire ActionByName Multipart with the actionid: " + actionId);
            return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    }
    /**
     * Fires a workflow action by name, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String} (Optional) to fire an action over the existing language (in combination of identifier).
     * @param fireActionForm {@link FireActionByNameForm} Fire Action by Name Form (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(summary = "Fire action by name",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Action not found")})
    public final Response fireActionByNameSinglePart(@Context final HttpServletRequest request,
                                     @QueryParam("inode")                        final String inode,
                                     @QueryParam("identifier")                   final String identifier,
                                     @QueryParam("indexPolicy")                  final String indexPolicy,
                                     @DefaultValue("-1") @QueryParam("language") final String   language,
                                     final FireActionByNameForm fireActionForm) {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
          .requestAndResponse(request, new MockHttpResponse())
          .requiredAnonAccess(AnonymousAccess.WRITE)
          .init();
        String actionId = null;

        try {

            Logger.debug(this, ()-> "On Fire Action: action name = '" + (null != fireActionForm? fireActionForm.getActionName(): StringPool.BLANK)
                    + "', inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);
            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            actionId = this.workflowHelper.getActionIdOnList
                    (fireActionForm.getActionName(), contentlet, initDataObject.getUser());

            Logger.debug(this, "fire ActionByName with the actionid: " + actionId);

            DotPreconditions.notNull(actionId, Sneaky.sneaked(()-> LanguageUtil.get(initDataObject.getUser().getLocale(),
                    "Unable-to-execute-workflows-InvalidActionName", fireActionForm.getActionName())),
                    DotContentletValidationException.class);

            return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private Response fireAction(final HttpServletRequest request,
                                final FireActionForm fireActionForm,
                                final User user,
                                final Contentlet contentlet,
                                final String actionId,
                                final Optional<SystemActionApiFireCommand> fireCommandOpt) throws DotDataException, DotSecurityException {

        Logger.debug(this, ()-> "Firing workflow action: " + actionId);

        if(null == contentlet || contentlet.getMap().isEmpty()) {

            Logger.debug(this, ()-> "On Fire Action: content is null or empty");
            throw new DoesNotExistException("contentlet-was-not-found");
        }

        final PageMode pageMode = PageMode.get(request);
        final IndexPolicy indexPolicy = contentlet.getIndexPolicy()!=null
                ? contentlet.getIndexPolicy()
                : IndexPolicyProvider.getInstance().forSingleContent();

        final ContentletDependencies.Builder formBuilder = new ContentletDependencies.Builder();
        formBuilder.respectAnonymousPermissions(pageMode.respectAnonPerms).
                workflowActionId(actionId).modUser(user)
                .indexPolicy(indexPolicy);

        if(fireActionForm != null) {

            formBuilder.workflowActionComments(fireActionForm.getComments())
                    .workflowAssignKey(fireActionForm.getAssign())
                    .workflowPublishDate(fireActionForm.getPublishDate())
                    .workflowPublishTime(fireActionForm.getPublishTime())
                    .workflowTimezoneId(fireActionForm.getTimezoneId())
                    .workflowExpireDate(fireActionForm.getExpireDate())
                    .workflowExpireTime(fireActionForm.getExpireTime())
                    .workflowNeverExpire(fireActionForm.getNeverExpire())
                    .workflowFilterKey(fireActionForm.getFilterKey())
                    .workflowWhereToSend(fireActionForm.getWhereToSend())
                    .workflowIWantTo(fireActionForm.getIWantTo())
                    .workflowPathToMove(fireActionForm.getPathToMove());

            this.processPermissions(fireActionForm, formBuilder);
        }

        if (contentlet.getMap().containsKey(Contentlet.RELATIONSHIP_KEY)) {
            formBuilder.relationships((ContentletRelationships) contentlet.getMap().get(Contentlet.RELATIONSHIP_KEY));
        }

        final Optional<List<Category>>categories = MapToContentletPopulator.
                INSTANCE.fetchCategories(contentlet, user, pageMode.respectAnonPerms);

        //Empty collection implies removal, so only when a value is present we must pass the collection
        categories.ifPresent(formBuilder::categories);

        return Response.ok(
                new ResponseEntityView(
                        this.workflowHelper.contentletToMap(
                                fireCommandOpt.isPresent()?
                                        fireCommandOpt.get().fire(contentlet,
                                                this.needSave(fireActionForm), formBuilder.build()):
                                        this.workflowAPI.fireContentWorkflow(contentlet, formBuilder.build()))
                )
        ).build(); // 200
    }

    private void processPermissions(final FireActionForm fireActionForm,
                                    final ContentletDependencies.Builder formBuilder) {

        if (null != fireActionForm.getIndividualPermissions()) {

            final List<Permission> permissions = new ArrayList<>();
            for(final Map.Entry<PermissionAPI.Type, List<String>> entry :
                    fireActionForm.getIndividualPermissions().entrySet()) {

                entry.getValue().forEach(roleId -> permissions.add(
                        new Permission(null, this.mapRoleId(roleId), entry.getKey().getType())));
            }

            formBuilder.permissions(permissions);
        }
    }

    protected String mapRoleId (final String roleIdOrKey) {

        final Role role = Try.of(()-> APILocator.getRoleAPI().loadRoleByKey(roleIdOrKey)).getOrNull();
        return null != role? role.getId(): roleIdOrKey;
    }

    private boolean needSave (final FireActionForm fireActionForm) {

        return null != fireActionForm && UtilMethods.isSet(fireActionForm.getContentletFormData());
    }

    /**
     * Fires a workflow with default action, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String} (Optional) to fire an action over the existing language (in combination of identifier).
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * (if an inode is set, this param is not ignored).
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * @return Response
     */
    @PUT
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Fire default action by name",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Action not found")})
    public final Response fireActionDefaultSinglePart(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @QueryParam("inode")            final String inode,
                                     @QueryParam("identifier")       final String identifier,
                                     @QueryParam("indexPolicy")      final String indexPolicy,
                                     @DefaultValue("-1") @QueryParam("language") final String language,
                                     @PathParam("systemAction") final WorkflowAPI.SystemAction systemAction,
                                     final FireActionForm fireActionForm) {

          final InitDataObject initDataObject = new WebResource.InitBuilder()
          .requestAndResponse(request, response)
          .requiredAnonAccess(AnonymousAccess.WRITE)
          .init();
      
        try {

            Logger.debug(this, ()-> "On Fire Action: systemAction = " + systemAction + ", inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final PageMode mode   = PageMode.get(request);
            final long languageId = LanguageUtil.getLanguageId(language);
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            this.checkContentletState (contentlet, systemAction);

            final Optional<WorkflowAction> workflowActionOpt = // ask to see if there is any default action by content type or scheme
                    this.workflowAPI.findActionMappedBySystemActionContentlet
                            (contentlet, systemAction, initDataObject.getUser());

            if (workflowActionOpt.isPresent()) {

                final WorkflowAction workflowAction = workflowActionOpt.get();
                final String actionId = workflowAction.getId();

                Logger.info(this, "Using the default action: " + workflowAction +
                        ", for the system action: " + systemAction);

                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(workflowAction,
                                this.needSave(fireActionForm), systemAction);

                return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, fireCommandOpt);
            } else {

                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(systemAction);

                if (fireCommandOpt.isPresent()) {

                    return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, null, fireCommandOpt);
                }

                final ContentType contentType = contentlet.getContentType();
                throw new DoesNotExistException("For the contentType: " + (null != contentType?contentType.variable():"unknown") +
                        " systemAction = " + systemAction);
            }
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, systemAction: " + systemAction +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    } // fireAction.

    /**
     * Fires a workflow with default action to perform over a collection of contentlets (Post)
     *
     *
     * The result of the execution is a streaming of the json, it will return a set of maps
     *
     * @param request    {@link HttpServletRequest}
     * @param response   {@link HttpServletResponse}
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * @return Response
     */
    @POST()
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    //@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/octet-stream")
    @Operation(summary = "Fire default action by name on multiple contents",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Action not found")})
    public final Response fireMultipleActionDefault(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam("systemAction") final WorkflowAPI.SystemAction systemAction,
                                                    final FireMultipleActionForm fireActionForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response).requiredAnonAccess(AnonymousAccess.WRITE).init();

        Logger.debug(this, ()-> "On Fire Multiple Actions: systemAction = " + systemAction);

        final PageMode mode   = PageMode.get(request);

        return Response.ok(new MultipleContentletStreamingOutput(systemAction,
                fireActionForm, request, mode, initDataObject))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    } // fireMultipleActionDefault.

    private class MultipleContentletStreamingOutput implements StreamingOutput {

        private final List<Map<String, Object>> contentletsToSaveList;
        private final WorkflowAPI.SystemAction systemAction;
        private final FireMultipleActionForm fireActionForm;
        private final HttpServletRequest request;
        private final PageMode mode;
        private final InitDataObject initDataObject;

        private MultipleContentletStreamingOutput(final WorkflowAPI.SystemAction systemAction,
                                                  final FireMultipleActionForm fireActionForm,
                                                  final HttpServletRequest request,
                                                  final PageMode mode,
                                                  final InitDataObject initDataObject) {

            this.contentletsToSaveList = fireActionForm.getContentletsFormData();
            this.systemAction           = systemAction;
            this.fireActionForm = fireActionForm;
            this.request = request;
            this.mode = mode;
            this.initDataObject = initDataObject;
        }

        @Override
        public void write(final OutputStream output) throws IOException, WebApplicationException {

            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            WorkflowResource.this.saveMultipleContentletsByDefaultAction(this.contentletsToSaveList, this.systemAction, this.fireActionForm,
                    this.request, this.mode, this.initDataObject, output, objectMapper);
        }
    }

    /**
     * This method fires the workflow for multiple contentlets
     * @param contentletsToSaveList
     * @param systemAction
     * @param fireActionForm
     * @param request
     * @param mode
     * @param initDataObject
     * @param outputStream
     * @param objectMapper
     */
    private void saveMultipleContentletsByDefaultAction(final List<Map<String, Object>> contentletsToSaveList,
                                                 final WorkflowAPI.SystemAction systemAction,
                                                 final FireMultipleActionForm fireActionForm,
                                                 final HttpServletRequest request,
                                                 final PageMode mode,
                                                 final InitDataObject initDataObject,
                                                 final OutputStream outputStream,
                                                 final ObjectMapper objectMapper) {

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter(WORKFLOW_SUBMITTER,
                new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(2).maxPoolSize(5).queueCapacity(CONTENTLETS_LIMIT).build());
        final CompletionService<Map<String, Object>> completionService = new ExecutorCompletionService<>(dotSubmitter);
        final List<Future<Map<String, Object>>> futures = new ArrayList<>();
        final HttpServletRequest statelessRequest = RequestUtil.INSTANCE.createStatelessRequest(request);

        for (final Map<String, Object> contentMap : contentletsToSaveList) {

            // this triggers the save
            final Future<Map<String, Object>> future = completionService.submit(() -> {

                HttpServletRequestThreadLocal.INSTANCE.setRequest(statelessRequest);
                final Map<String, Object> resultMap = new HashMap<>();
                final String inode      = (String) contentMap.get("inode");
                final String identifier = (String) contentMap.get("identifier");
                final long languageId   = ConversionUtils.toLong(contentMap.get("languageId"), -1l);
                final User user         = initDataObject.getUser();
                final FireActionForm singleFireActionForm = new FireActionForm.Builder()
                    .contentlet(contentMap).assign(fireActionForm.getAssign())
                    .comments(fireActionForm.getComments()).expireDate(fireActionForm.getExpireDate())
                    .neverExpire(fireActionForm.getNeverExpire()).filterKey(fireActionForm.getFilterKey())
                    .iWantTo(fireActionForm.getIWantTo()).publishDate(fireActionForm.getPublishDate())
                    .publishTime(fireActionForm.getPublishTime()).timezoneId(fireActionForm.getTimezoneId()).whereToSend(fireActionForm.getWhereToSend()).build();
                final IndexPolicy indexPolicy = MapToContentletPopulator.recoverIndexPolicy(
                        singleFireActionForm.getContentletFormData(),
                        IndexPolicyProvider.getInstance().forSingleContent(), request);


                try {

                    fireTransactionalAction(systemAction, singleFireActionForm, request, mode,
                            initDataObject, resultMap, inode, identifier, languageId, user, indexPolicy);
                } catch (Exception e) {

                    final String id = UtilMethods.isSet(identifier) ? identifier :
                            (UtilMethods.isSet(inode) ? inode:
                                    null != singleFireActionForm && null != singleFireActionForm.getContentletFormData() ?
                                            singleFireActionForm.getContentletFormData().toString() :
                                            "unknown contentlet" + System.currentTimeMillis());
                    Logger.error(this, "Error in contentlet: " + id + ", msg: " + e.getMessage()
                            + ", running the action: " + systemAction, e);
                    resultMap.put(id, UtilMethods.isSet(identifier)?
                            ActionFail.newInstanceById(user, identifier, e):ActionFail.newInstance(user, inode, e));
                }

                return resultMap;
            });

            futures.add(future);
        }

        printResponseEntityViewResult(outputStream, objectMapper, completionService, futures);
    }

    ////////////////////////////

    /**
     * Fires a workflow with default action to perform a merge (Patch) between the fields sent in the body form and
     * the existing contentlets.
     *
     * Users may merge just one contentlet by using:
     * - identifier + lang (optional)
     * - inode
     * - a query
     *
     * The result of the execution is a streaming of the json, it will return a set of maps
     *
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String} (Optional) to fire an action over the existing language (in combination of identifier).
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * (if an inode is set, this param is not ignored).
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * @return Response
     */
    @PATCH()
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    //@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Produces("application/octet-stream")
    public final Response fireMergeActionDefault(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @QueryParam("inode")            final String inode,
                                            @QueryParam("identifier")       final String identifier,
                                            @DefaultValue("-1") @QueryParam("language") final String language,
                                            @QueryParam("offset") final int offset,
                                            @PathParam("systemAction") final WorkflowAPI.SystemAction systemAction,
                                            final FireActionForm fireActionForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response).requiredAnonAccess(AnonymousAccess.WRITE).init();

        final String query = null != fireActionForm? fireActionForm.getQuery():StringPool.BLANK;
        Logger.debug(this, ()-> "On Fire Merge Action: systemAction = " + systemAction + ", inode = " + inode +
                ", identifier = " + identifier + ", language = " + language + ", query: " + query);

        final PageMode mode   = PageMode.get(request);
        final long languageId = LanguageUtil.getLanguageId(language);
        final List<SingleContentQuery> contentletsToMergeList = new ArrayList<>();
        if (UtilMethods.isNotSet(query)) { // it is single update

            contentletsToMergeList.add(new SingleContentQuery(identifier, inode, languageId,
                    MapToContentletPopulator.recoverIndexPolicy(fireActionForm.getContentletFormData(),
                            IndexPolicyProvider.getInstance().forSingleContent(), request)));
        } else {

            final List<ContentletSearch> contentletSearches  = this.contentletAPI.searchIndex(query, Config.getIntProperty("", CONTENTLETS_LIMIT),
                    offset, null,
                    initDataObject.getUser(), mode.respectAnonPerms);

            final IndexPolicy indexPolicy = MapToContentletPopulator.recoverIndexPolicy(
                    (Objects.isNull(fireActionForm) || Objects.isNull(fireActionForm.getContentletFormData()))?Map.of():fireActionForm.getContentletFormData(),
                    contentletSearches.size()> 10? IndexPolicy.DEFER: IndexPolicy.WAIT_FOR, request);

            for (final ContentletSearch contentletSearch : contentletSearches) {

                contentletsToMergeList.add(new SingleContentQuery(contentletSearch.getIdentifier(),
                        contentletSearch.getInode(), languageId, indexPolicy));
            }
        }

        return Response.ok(new MergeContentletStreamingOutput(contentletsToMergeList, systemAction,
                fireActionForm, request, mode, initDataObject))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    } // fireMergeActionDefault.


    private class MergeContentletStreamingOutput implements StreamingOutput {

        private final List<SingleContentQuery> contentletsToMergeList;
        private final WorkflowAPI.SystemAction systemAction;
        private final FireActionForm fireActionForm;
        private final HttpServletRequest request;
        private final PageMode mode;
        private final InitDataObject initDataObject;

        private MergeContentletStreamingOutput(final List<SingleContentQuery> contentletsToMergeList,
                                               final WorkflowAPI.SystemAction systemAction,
                                               final FireActionForm fireActionForm,
                                               final HttpServletRequest request,
                                               final PageMode mode,
                                               final InitDataObject initDataObject) {

            this.contentletsToMergeList = contentletsToMergeList;
            this.systemAction           = systemAction;
            this.fireActionForm = fireActionForm;
            this.request = request;
            this.mode = mode;
            this.initDataObject = initDataObject;
        }

        @Override
        public void write(final OutputStream output) throws IOException, WebApplicationException {

            final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
            WorkflowResource.this.mergeContentletsByDefaultAction(this.contentletsToMergeList, this.systemAction, this.fireActionForm,
                    this.request, this.mode, this.initDataObject, output, objectMapper);
        }
    }

    private void mergeContentletsByDefaultAction(final List<SingleContentQuery> contentletsToMergeList,
                                                     final WorkflowAPI.SystemAction systemAction,
                                                     final FireActionForm fireActionForm,
                                                     final HttpServletRequest request,
                                                     final PageMode mode,
                                                     final InitDataObject initDataObject,
                                                     final OutputStream outputStream,
                                                     final ObjectMapper objectMapper) {

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter("workflow_submitter",
                new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(2).maxPoolSize(5).queueCapacity(CONTENTLETS_LIMIT).build());
        final CompletionService<Map<String, Object>> completionService = new ExecutorCompletionService<>(dotSubmitter);
        final List<Future<Map<String, Object>>> futures = new ArrayList<>();
        final HttpServletRequest statelessRequest = RequestUtil.INSTANCE.createStatelessRequest(request);

        for (final SingleContentQuery singleContentQuery : contentletsToMergeList) {

            // this triggers the merges
            final Future<Map<String, Object>> future = completionService.submit(() -> {

                HttpServletRequestThreadLocal.INSTANCE.setRequest(statelessRequest);
                final Map<String, Object> resultMap = new HashMap<>();
                final String inode      = singleContentQuery.getInode();
                final String identifier = singleContentQuery.getIdentifier();
                final long languageId   = singleContentQuery.getLanguage();
                final User user         = initDataObject.getUser();
                final IndexPolicy indexPolicy = singleContentQuery.getIndexPolicy();

                try {

                    fireTransactionalAction(systemAction, fireActionForm, statelessRequest, mode,
                            initDataObject, resultMap, inode, identifier, languageId, user, indexPolicy);
                } catch (Exception e) {

                    final String id = UtilMethods.isSet(identifier)?identifier:inode;
                    Logger.error(this, "Error in contentlet: " + id + ", msg: " + e.getMessage(), e);
                    resultMap.put(id, UtilMethods.isSet(identifier)?
                            ActionFail.newInstanceById(user, identifier, e):ActionFail.newInstance(user, inode, e));
                }

                return resultMap;
            });

            futures.add(future);
        }

        printResponseEntityViewResult(outputStream, objectMapper, completionService, futures);
    }

    @WrapInTransaction
    private void fireTransactionalAction(final SystemAction systemAction,
                                         final FireActionForm fireActionForm,
                                         final HttpServletRequest request,
                                         final PageMode mode, InitDataObject initDataObject,
                                         final Map<String, Object> resultMap,
                                         final String inode, String identifier,
                                         final long languageId,
                                         final User user,
                                         final IndexPolicy indexPolicy) throws DotDataException, DotSecurityException {

        final Contentlet contentlet = this.getContentlet // this do the merge
                (inode, identifier, languageId,
                        () -> WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                        fireActionForm, initDataObject, mode);

        contentlet.setIndexPolicy(indexPolicy);
        this.checkContentletState(contentlet, systemAction);

        String contentletId = null != identifier? identifier: contentlet.getIdentifier();

        final Optional<WorkflowAction> workflowActionOpt = this.workflowAPI.findActionMappedBySystemActionContentlet
                (contentlet, systemAction, user);

        final Response restResponse = this.mergeContentlet(systemAction, fireActionForm, request, user, contentlet, workflowActionOpt);
        final  Map<String, Object> contentletMap = (Map<String, Object>)ResponseEntityView.class.cast(restResponse.getEntity()).getEntity();
        contentletId = !UtilMethods.isSet(contentletId)? (String)contentletMap.get("identifier"):contentletId;
        resultMap.put(contentletId, contentletMap);
    }

    private void printResponseEntityViewResult(final OutputStream outputStream,
                                               final ObjectMapper objectMapper,
                                               final CompletionService<Map<String, Object>> completionService,
                                               final List<Future<Map<String, Object>>> futures) {

        try {

            ResponseUtil.beginWrapResponseEntityView(outputStream, true);
            ResponseUtil.beginWrapProperty(outputStream, "results", false);
            outputStream.write(StringPool.OPEN_BRACKET.getBytes(StandardCharsets.UTF_8));
            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int successCount = 0;
            int failCount    = 0;
            // now recover the N results
            for (int i = 0; i < futures.size(); i++) {

                try {

                    Logger.info(this, "Recovering the result " + (i + 1) + " of " + futures.size());
                    final Map<String, Object> resultMap = completionService.take().get();
                    objectMapper.writeValue(outputStream, resultMap);

                    if (isFail(resultMap)) {
                        failCount++;
                    } else {
                        successCount++;
                    }

                    if (i < futures.size()-1) {
                        outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (InterruptedException | ExecutionException | IOException e) {

                    Logger.error(this, e.getMessage(), e);
                }
            }
            stopWatch.stop();

            outputStream.write(StringPool.CLOSE_BRACKET.getBytes(StandardCharsets.UTF_8));
            outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));

            ResponseUtil.wrapProperty(outputStream, "summary",
                    objectMapper.writeValueAsString(CollectionsUtils.map("time", stopWatch.getTime(),
                            "affected", futures.size(),
                            "successCount", successCount,
                            "failCount", failCount)));
            outputStream.write(StringPool.COMMA.getBytes(StandardCharsets.UTF_8));

            ResponseUtil.endWrapResponseEntityView(outputStream, true);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    private boolean isFail(final Map<String, Object> resultMap) {

        final String id = resultMap.keySet().stream().findFirst().get();
        return resultMap.get(id) instanceof ActionFail;
    }

    private Response mergeContentlet(SystemAction systemAction, FireActionForm fireActionForm, HttpServletRequest request, User user,
                                                Contentlet contentlet, Optional<WorkflowAction> workflowActionOpt) throws DotDataException, DotSecurityException {
        if (workflowActionOpt.isPresent()) {

            final WorkflowAction workflowAction = workflowActionOpt.get();
            final String actionId = workflowAction.getId();

            Logger.info(this, "Using the default action: " + workflowAction +
                    ", for the system action: " + systemAction);

            final Optional<SystemActionApiFireCommand> fireCommandOpt =
                    this.systemActionApiFireCommandProvider.get(workflowAction,
                            this.needSave(fireActionForm), systemAction);

            return this.fireAction(request, fireActionForm, user, contentlet, actionId, fireCommandOpt);
        } else {

            final Optional<SystemActionApiFireCommand> fireCommandOpt =
                    this.systemActionApiFireCommandProvider.get(systemAction);

            if (fireCommandOpt.isPresent()) {

                return this.fireAction(request, fireActionForm, user, contentlet, null, fireCommandOpt);
            }

            final ContentType contentType = contentlet.getContentType();
            throw new DoesNotExistException("For the contentType: " + (null != contentType ? contentType.variable() : "unknown") +
                    " systemAction = " + systemAction);
        }
    }


    /**
     * Check preconditions.
     * If contentlet can not be found, 404
     * if contentlet is not can not be a default action: UNPUBLISH, UNARCHIVE, DELETE, DESTROY
     * @param contentlet
     * @param systemAction
     * @throws NotFoundInDbException
     */
    private void checkContentletState(final Contentlet contentlet, final SystemAction systemAction)
            throws NotFoundInDbException {

        if (null == contentlet) {

            throw new NotFoundInDbException("Not Contentlet Found");
        }

        if (contentlet.isNew()) {

            if (    systemAction == SystemAction.UNPUBLISH ||
                    systemAction == SystemAction.UNARCHIVE ||
                    systemAction == SystemAction.DELETE    ||
                    systemAction == SystemAction.DESTROY) {

                throw new IllegalArgumentException("A new Contentlet can not fire any of these actions: [EDIT, UNPUBLISH, UNARCHIVE, DELETE, DESTROY]");
            }
        }
    }

    /**
     * Exposed under a different path `firemultipart` so Swagger takes it
     */

    @PUT
    @Path("/actions/{actionId}/firemultipart")
    @JSONP
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Fire action by ID multipart",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "404", description = "Action not found")})
    public final Response fireActionMultipartNewPath(@Context               final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam ("actionId")         final String actionId,
            @QueryParam("inode")            final String inode,
            @QueryParam("identifier")       final String identifier,
            @QueryParam("indexPolicy")      final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") final String   language,
            final FormDataMultiPart multipart) {
        return fireActionMultipart(request, response, actionId, inode, identifier, indexPolicy,
                language, multipart);
    }

    /**
     * Fires a workflow action with multi part body
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param actionId   {@link String} (Required) action id to fire
     * (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/{actionId}/fire")
    @JSONP
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response fireActionMultipart(@Context               final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam ("actionId")         final String actionId,
            @QueryParam("inode")            final String inode,
            @QueryParam("identifier")       final String identifier,
            @QueryParam("indexPolicy")      final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") final String   language,
            final FormDataMultiPart multipart) {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .init();

        try {

            Logger.debug(this, ()-> "On Fire Action Multipart: action Id " + actionId + ", inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            final FireActionForm fireActionForm = this.processForm (multipart, initDataObject.getUser());
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    } // fire.

    /**
     * Exposed under a different path `firemultipart` so Swagger takes it
     */
    @PUT
    @Path("/actions/default/firemultipart/{systemAction}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Fire default action by name multipart",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Action not found")})
    public final Response fireActionDefaultMultipartNewPath(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("inode")       final String inode,
            @QueryParam("identifier")  final String identifier,
            @QueryParam("indexPolicy") final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") final String   language,
            @PathParam("systemAction") final WorkflowAPI.SystemAction systemAction,
            final FormDataMultiPart multipart) {
        return fireActionDefaultMultipart(request, response, inode, identifier, indexPolicy, language,
                systemAction, multipart);
    }
    /**
     * Fires a workflow with default action with multi part body
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param systemAction {@link com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction} system action to determine the default action
     * (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/default/fire/{systemAction}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Fire default action",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Action not found")})
    public final Response fireActionDefaultMultipart(
                                              @Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              @QueryParam("inode")       final String inode,
                                              @QueryParam("identifier")  final String identifier,
                                              @QueryParam("indexPolicy") final String indexPolicy,
                                              @DefaultValue("-1") @QueryParam("language") final String   language,
                                              @PathParam("systemAction") final WorkflowAPI.SystemAction systemAction,
                                              final FormDataMultiPart multipart) {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .init();

        try {

            Logger.debug(this, ()-> "On Fire Action Multipart: systemAction = " + systemAction + ", inode = " + inode +
                    ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

            final long languageId = LanguageUtil.getLanguageId(language);
            final PageMode mode = PageMode.get(request);
            final FireActionForm fireActionForm = this.processForm (multipart, initDataObject.getUser());
            //if inode is set we use it to look up a contentlet
            final Contentlet contentlet = this.getContentlet
                    (inode, identifier, languageId,
                            ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                            fireActionForm, initDataObject, mode);

            if (UtilMethods.isSet(indexPolicy)) {
                contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
            }

            this.checkContentletState (contentlet, systemAction);

            final Optional<WorkflowAction> workflowActionOpt =
                    this.workflowAPI.findActionMappedBySystemActionContentlet
                        (contentlet, systemAction, initDataObject.getUser());

            if (workflowActionOpt.isPresent()) {

                final WorkflowAction workflowAction = workflowActionOpt.get();
                final String actionId = workflowAction.getId();
                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(workflowAction,
                                this.needSave(fireActionForm), systemAction);

                return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, fireCommandOpt);
            } else {

                final Optional<SystemActionApiFireCommand> fireCommandOpt =
                        this.systemActionApiFireCommandProvider.get(systemAction);

                if (fireCommandOpt.isPresent()) {

                    return this.fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, null, fireCommandOpt);
                }

                final ContentType contentType = contentlet.getContentType();
                throw new DoesNotExistException("For the contentType: " + (null != contentType?contentType.variable():"unknown") +
                        " systemAction = " + systemAction);
            }
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, systemAction: " + systemAction +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    } // fire.

    /**
     * Fires a workflow action by action id, if the contentlet exists could use inode or identifier and optional language.
     * @param request    {@link HttpServletRequest}
     * @param inode      {@link String} (Optional) to fire an action over the existing inode.
     * @param identifier {@link String} (Optional) to fire an action over the existing identifier (in combination of language).
     * @param language   {@link String}   (Optional) to fire an action over the existing language (in combination of identifier).
     * @param actionId   {@link String} (Required) action id to fire
     * @param fireActionForm {@link FireActionForm} Fire Action Form
     * (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/{actionId}/fire")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Fire action by ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "404", description = "Action not found")})
    public final Response fireActionSinglePart(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam ("actionId")         final String actionId,
            @QueryParam("inode")            final String inode,
            @QueryParam("identifier")       final String identifier,
            @QueryParam("indexPolicy")       final String indexPolicy,
            @DefaultValue("-1") @QueryParam("language") final String   language,
            final FireActionForm fireActionForm) throws DotDataException, DotSecurityException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requestAndResponse(request, response)
                .requiredAnonAccess(AnonymousAccess.WRITE)
                .init();

        Logger.debug(this, ()-> "On Fire Action: action Id " + actionId + ", inode = " + inode +
                ", identifier = " + identifier + ", language = " + language + " indexPolicy = " + indexPolicy);

        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode = PageMode.get(request);
        //if inode is set we use it to look up a contentlet
        final Contentlet contentlet = this.getContentlet
                (inode, identifier, languageId,
                        ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId(),
                        fireActionForm, initDataObject, mode);

        if (UtilMethods.isSet(indexPolicy)) {
            contentlet.setIndexPolicy(IndexPolicy.parseIndexPolicy(indexPolicy));
        }

        return fireAction(request, fireActionForm, initDataObject.getUser(), contentlet, actionId, Optional.empty());
    } // fireAction.

    private LinkedHashSet<String> getBinaryFields(final Map<String, Object> mapContent) {

        List array = (List) mapContent.getOrDefault(BINARY_FIELDS, Collections.EMPTY_LIST);


        final LinkedHashSet<String> hashSet = new LinkedHashSet<>();
        array.forEach(a -> hashSet.add(a.toString()));

        return hashSet;

    }

    private FireActionByNameForm processForm(final FormDataMultiPart multipart, final User user)
            throws IOException, JSONException, DotSecurityException, DotDataException {

        Map<String, Object> contentletMap = Collections.emptyMap();
        final FireActionByNameForm.Builder fireActionFormBuilder = new FireActionByNameForm.Builder();
        final Tuple2<Map<String,Object>, List<File>> multiPartContent =
                this.multiPartUtils.getBodyMapAndBinariesFromMultipart(multipart);

        final LinkedHashSet<String> incomingBinaryFields = this.getBinaryFields(multiPartContent._1);

        if (multiPartContent._1.containsKey(CONTENTLET)) {

            contentletMap = this.convertToContentletMap((JSONObject)multiPartContent._1.get(CONTENTLET));
        }

        this.validateMultiPartContent(contentletMap, incomingBinaryFields);
        this.processFireActionFormValues(fireActionFormBuilder, multiPartContent._1);
        this.processFiles(contentletMap, multiPartContent._2, incomingBinaryFields, user);
        fireActionFormBuilder.contentlet(contentletMap);

        return fireActionFormBuilder.build();
    }

    private Map<String, Object> convertToContentletMap(final JSONObject contentletJson) throws IOException {

        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper().
                readValue(contentletJson.toString(), Map.class);
    }

    private void validateMultiPartContent (final Map<String, Object> contentMap,
                                           final LinkedHashSet<String> binaryFields) {

        for (final String binaryField : binaryFields) {

            if (contentMap.containsKey(binaryField)) {

                throw new BadRequestException("The binary field: " + binaryField
                        + " can not be part of the " + BINARY_FIELDS + " and part of the request body");
            }
        }
    }

    private List<String> getBinaryFields (final List<Field> fields, final Map<String, Object> contentMap,
                                          final int binaryFileSize) {

        // if not any binaryField set, all fields that starts with "binary" will be used.
        final List<String> binaryFields = fields.stream()
                .filter(field -> LegacyFieldTransformer.buildLegacyFieldContent(field).startsWith(PREFIX_BINARY))
                .map(Field::variable)
                .filter(variable -> !contentMap.containsKey(variable)) // if it is not already set, probably to remove
                .collect(Collectors.toList());

        // the binary fields are not set by the user on the body request
        // the size of the binary field should be the same of the
        return binaryFields.size() <= binaryFileSize? binaryFields: binaryFields.subList(0, binaryFields.size());
    }

    private String getContentTypeInode (final Map<String, Object> contentMap, final User user, final List<File> binaryFiles) {

        this.contentHelper.checkOrSetContentType(contentMap, user, binaryFiles);
        return MapToContentletPopulator.INSTANCE.getContentTypeInode(contentMap);
    }

    private void processFiles(final Map<String, Object> contentMap, final List<File> binaryFiles,
                              final LinkedHashSet<String> argBinaryFields, final User user) throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI      = APILocator.getContentTypeAPI(APILocator.systemUser());
        final String         contentTypeInode    = this.getContentTypeInode(contentMap, user, binaryFiles);

        if (!UtilMethods.isSet(contentTypeInode)) {

            throw new BadRequestException("The content type or base type, is not set or is invalid");
        }

        final List<Field>    fields              = contentTypeAPI.find(contentTypeInode).fields();
        final List<String>   binaryFields        = argBinaryFields.size() > 0?
                new ArrayList<>(argBinaryFields) : this.getBinaryFields (fields, contentMap, binaryFiles.size());

        if (UtilMethods.isSet(contentTypeInode) && binaryFields.size() > 0) {

            final Map<String, Field> fieldsMap =
                    fields.stream().collect(Collectors.toMap(Field::variable, field -> field));
            //final int size                     =
            //        Math.min(binaryFiles.size(), binaryFields.size()); // if the user sent more files than fields, we took the min of them.

            for (int i = 0; i < binaryFields.size(); ++i) {

                final String fieldName = binaryFields.get(i);
                if (fieldsMap.containsKey(fieldName)) {

                    final Field field = fieldsMap.get(fieldName);
                    final File binary = i < binaryFiles.size()? binaryFiles.get(i): null;
                    if(binary != null){
                        // if we send null in this map the underlying APIs will understand it as if the binary needs to be removed from the map
                        // Therefore the new version of the contentlet will end up losing pieces of content
                        // if we want to remove the binary the we need to explicitly set something like image=null in the map
                        // Not sending the binary will cause no difference.
                       contentMap.put(field.variable(), binary);
                    }
                }
            }
        }
    }

    private void processFireActionFormValues(final FireActionByNameForm.Builder fireActionFormBuilder,
                                             final Map<String, Object> contentMap) {

        if (contentMap.containsKey(ASSIGN)) {

            fireActionFormBuilder.assign((String)contentMap.get(ASSIGN));
            contentMap.remove(ASSIGN);
        }

        if (contentMap.containsKey(COMMENTS)) {

            fireActionFormBuilder.comments((String)contentMap.get(COMMENTS));
            contentMap.remove(COMMENTS);
        }

        if (contentMap.containsKey(PUBLISH_DATE)) {

            fireActionFormBuilder.publishDate((String)contentMap.get(PUBLISH_DATE));
            contentMap.remove(PUBLISH_DATE);
        }

        if (contentMap.containsKey(PUBLISH_TIME)) {

            fireActionFormBuilder.publishTime((String)contentMap.get(PUBLISH_TIME));
            contentMap.remove(PUBLISH_TIME);
        }

        if (contentMap.containsKey(EXPIRE_DATE)) {

            fireActionFormBuilder.expireDate((String)contentMap.get(EXPIRE_DATE));
            contentMap.remove(EXPIRE_DATE);
        }

        if (contentMap.containsKey(EXPIRE_TIME)) {

            fireActionFormBuilder.expireTime((String)contentMap.get(EXPIRE_TIME));
            contentMap.remove(EXPIRE_TIME);
        }

        if (contentMap.containsKey(NEVER_EXPIRE)) {

            fireActionFormBuilder.neverExpire((String)contentMap.get(NEVER_EXPIRE));
            contentMap.remove(NEVER_EXPIRE);
        }

        if (contentMap.containsKey(Contentlet.WHERE_TO_SEND)) {

            fireActionFormBuilder.whereToSend((String)contentMap.get(Contentlet.WHERE_TO_SEND));
            contentMap.remove(Contentlet.WHERE_TO_SEND);
        }

        if (contentMap.containsKey(ACTION_NAME)) {

            fireActionFormBuilder.actionName((String)contentMap.get(ACTION_NAME));
            contentMap.remove(ACTION_NAME);
        }

        if (contentMap.containsKey(Contentlet.FILTER_KEY)) {
            fireActionFormBuilder.filterKey((String)contentMap.get(Contentlet.FILTER_KEY));
            contentMap.remove(Contentlet.FILTER_KEY);
        }

        if (contentMap.containsKey(Contentlet.I_WANT_TO)) {
            fireActionFormBuilder.filterKey((String)contentMap.get(Contentlet.I_WANT_TO));
            contentMap.remove(Contentlet.I_WANT_TO);
        }

        if (contentMap.containsKey(Contentlet.PATH_TO_MOVE)) {
            fireActionFormBuilder.pathToMove((String)contentMap.get(Contentlet.PATH_TO_MOVE));
            contentMap.remove(Contentlet.PATH_TO_MOVE);
        }
    }

    private Contentlet getContentlet(final String inode,
                                     final String identifier,
                                     final long language,
                                     final Supplier<Long> sessionLanguage,
                                     final FireActionForm fireActionForm,
                                     final InitDataObject initDataObject,
                                     final PageMode pageMode) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        PageMode mode = pageMode;
        final String finalInode      = UtilMethods.isSet(inode)? inode:
                (String)Try.of(()->fireActionForm.getContentletFormData().get("inode")).getOrNull();
        final String finalIdentifier = UtilMethods.isSet(identifier)? identifier:
                (String)Try.of(()->fireActionForm.getContentletFormData().get("identifier")).getOrNull();

        if(UtilMethods.isSet(finalInode)) {

            Logger.debug(this, ()-> "Fire Action, looking for content by inode: " + finalInode);

            final Contentlet currentContentlet = this.contentletAPI.find
                    (finalInode, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.notNull(currentContentlet, ()-> "contentlet-was-not-found", DoesNotExistException.class);

            contentlet = createContentlet(fireActionForm, initDataObject, currentContentlet,mode);
        } else if (UtilMethods.isSet(finalIdentifier)) {

            Logger.debug(this, ()-> "Fire Action, looking for content by identifier: " + finalIdentifier
                    + " and language id: " + language);

            mode = PageMode.EDIT_MODE; // when asking for identifier it is always edit
            final Optional<Contentlet> currentContentlet =  language <= 0?
                    this.workflowHelper.getContentletByIdentifier(finalIdentifier, mode, initDataObject.getUser(), sessionLanguage):
                    this.contentletAPI.findContentletByIdentifierOrFallback
                            (finalIdentifier, mode.showLive, language, initDataObject.getUser(), mode.respectAnonPerms);

            DotPreconditions.isTrue(currentContentlet.isPresent(), ()-> "contentlet-was-not-found", DoesNotExistException.class);

            contentlet = createContentlet(fireActionForm, initDataObject, currentContentlet.get(), mode);
        } else {

            //otherwise the information must be grabbed from the request body.
            Logger.debug(this, ()-> "Fire Action, creating a new contentlet");
            DotPreconditions.notNull(fireActionForm, ()-> "When no inode is sent the info on the Request body becomes mandatory.");
            contentlet = this.populateContentlet(fireActionForm, initDataObject.getUser(), mode);
        }

        return contentlet;
    }



    private Contentlet createContentlet(final FireActionForm fireActionForm,
                                        final InitDataObject initDataObject,
                                        final Contentlet currentContentlet,final PageMode mode) throws DotSecurityException {

        Contentlet contentlet = new Contentlet();
        contentlet.getMap().putAll(currentContentlet.getMap());

        if (null != fireActionForm && null != fireActionForm.getContentletFormData() && null != contentlet) {

            contentlet = this.populateContentlet(fireActionForm, contentlet, initDataObject.getUser(),mode);
        }
        if (contentlet.getInode().isEmpty() && !currentContentlet.getInode().isEmpty()) {
            contentlet.setInode(currentContentlet.getInode());
        }
        return contentlet;
    }

    /**
     * Internal utility to populate a contentlet from a given form object
     * @param fireActionForm FireActionForm
     * @param user User
     * @return Contentlet
     * @throws DotSecurityException
     */
    private Contentlet populateContentlet(final FireActionForm fireActionForm, final User user,final PageMode mode)
            throws DotSecurityException {

        return this.populateContentlet(fireActionForm, new Contentlet(), user,mode);
    } // populateContentlet.

    /**
     * Internal utility to populate a contentlet from a given form object
     * @param fireActionForm {@link FireActionForm}
     * @param contentletInput {@link Contentlet}
     * @param user {@link User}
     * @return Contentlet
     * @throws DotSecurityException
     */
    private Contentlet populateContentlet(final FireActionForm fireActionForm, final Contentlet contentletInput, final User user,final PageMode mode)
            throws DotSecurityException {

        if (contentletInput.isNew()) {
            // checks if has content type assigned, otherwise tries to see
            // if can figure out the content type based on a base type.
            this.contentHelper.checkOrSetContentType(fireActionForm.getContentletFormData(), user);
        }

        final Contentlet contentlet = this.contentHelper.populateContentletFromMap
                (contentletInput, fireActionForm.getContentletFormData());

        final Supplier<String> errorMessageSupplier = () -> {

            String message = "no-permissions-contenttype";

            try {
                message = LanguageUtil.get(user.getLocale(),
                        message, user.getUserId(), contentlet.getContentType().id());
            } catch (LanguageException e) {
                throw new ForbiddenException(message);
            }

            return message;
        };

        if (null == contentlet || null == contentlet.getContentType()) {

            throw new DotContentletValidationException("Workflow-does-not-exists-content-type");
        }

        try {
            if (!this.permissionAPI.doesUserHavePermission(contentlet.getContentType(),
                    PermissionAPI.PERMISSION_READ, user, mode.respectAnonPerms)) {
                throw new DotSecurityException(errorMessageSupplier.get());
            }
        } catch (DotDataException e) {
            throw new DotSecurityException(errorMessageSupplier.get(), e);
        }

        contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE, fireActionForm.getPublishDate());
        contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME, fireActionForm.getPublishTime());
        contentlet.setStringProperty(Contentlet.WORKFLOW_TIMEZONE_ID, fireActionForm.getTimezoneId());
        contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE,  fireActionForm.getExpireDate());
        contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME,  fireActionForm.getExpireTime());
        contentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE, fireActionForm.getNeverExpire());
        contentlet.setStringProperty(Contentlet.WHERE_TO_SEND,   fireActionForm.getWhereToSend());
        contentlet.setStringProperty(Contentlet.FILTER_KEY, fireActionForm.getFilterKey());
        contentlet.setStringProperty(Contentlet.I_WANT_TO, fireActionForm.getFilterKey());
        if (UtilMethods.isSet(fireActionForm.getPathToMove())) {
            contentlet.setStringProperty(Contentlet.PATH_TO_MOVE, fireActionForm.getPathToMove());
        }

        for(Field constant : contentlet.getContentType().fields()) {
          if(constant instanceof ConstantField)
            contentlet.getMap().put(constant.variable(), constant.values());
        }
        
        return contentlet;
    } // populateContentlet.

    /**
     * Change the order of an action associated to the step
     * @param request                           HttpServletRequest
     * @param workflowReorderActionStepForm     WorkflowReorderBean
     * @return Response
     */
    @PUT
    @Path("/reorder/steps/{stepId}/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response reorderAction(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response,
                                        @PathParam("stepId")   final String stepId,
                                        @PathParam("actionId") final String actionId,
                                        final WorkflowReorderWorkflowActionStepForm workflowReorderActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);

        try {
            DotPreconditions.notNull(workflowReorderActionStepForm,"Expected Request body was empty.");
            Logger.debug(this, "Doing reordering of: " + workflowReorderActionStepForm);
            this.workflowHelper.reorderAction(
                    new WorkflowReorderBean.Builder().stepId(stepId).actionId(actionId)
                            .order(workflowReorderActionStepForm.getOrder()).build(),
                    initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on reorderAction, workflowReorderActionStepForm: " + workflowReorderActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // reorderAction

    /**
     * Do an export of the scheme with all dependencies to rebuild it (such as steps and actions)
     * in addition the permission (who can use) will be also returned.
     * @param httpServletRequest HttpServletRequest
     * @param workflowSchemeImportForm WorkflowSchemeImportObjectForm
     * @return Response
     */
    @POST
    @Path("/schemes/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response importScheme(@Context final HttpServletRequest  httpServletRequest,
                                       @Context final HttpServletResponse httpServletResponse,
                                       final WorkflowSchemeImportObjectForm workflowSchemeImportForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, httpServletRequest, httpServletResponse, true, null);
        Response response;

        try {
            DotPreconditions.notNull(workflowSchemeImportForm,"Expected Request body was empty.");
            Logger.debug(this, "Importing the workflow schemes");

            this.workflowAPI.isUserAllowToModifiedWorkflow(initDataObject.getUser());

            final WorkflowSchemeImportExportObject exportObject = new WorkflowSchemeImportExportObject();
            exportObject.setSchemes(workflowSchemeImportForm.getWorkflowImportObject().getSchemes());
            exportObject.setSteps  (workflowSchemeImportForm.getWorkflowImportObject().getSteps());
            exportObject.setActions(workflowSchemeImportForm.getWorkflowImportObject().getActions());
            exportObject.setActionSteps(workflowSchemeImportForm.getWorkflowImportObject().getActionSteps());
            exportObject.setActionClasses(workflowSchemeImportForm.getWorkflowImportObject().getActionClasses());
            exportObject.setActionClassParams(workflowSchemeImportForm.getWorkflowImportObject().getActionClassParams());
            exportObject.setSchemeSystemActionWorkflowActionMappings(workflowSchemeImportForm.getWorkflowImportObject().getSchemeSystemActionWorkflowActionMappings());

            this.workflowHelper.importScheme (
                    exportObject,
                    workflowSchemeImportForm.getPermissions(),
                    initDataObject.getUser());
            response     = Response.ok(new ResponseEntityView("OK")).build(); // 200
        } catch (Exception e){

            Logger.error(this.getClass(),
                    "Exception on importScheme, Error importing schemes", e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    } // importScheme.

    /**
     * Do an export of the scheme with all dependencies to rebuild it (such as steps and actions)
     * in addition the permission (who can use) will be also returned.
     * @param httpServletRequest  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/export")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response exportScheme(@Context final HttpServletRequest  httpServletRequest,
                                       @Context final HttpServletResponse httpServletResponse,
                                       @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, httpServletRequest, httpServletResponse,true, null);
        Response response;
        WorkflowSchemeImportExportObject exportObject;
        List<Permission>                 permissions;
        WorkflowScheme                   scheme;

        try {

            Logger.debug(this, "Exporting the workflow scheme: " + schemeId);
            this.workflowAPI.isUserAllowToModifiedWorkflow(initDataObject.getUser());

            scheme       = this.workflowAPI.findScheme(schemeId);
            exportObject = this.workflowImportExportUtil.buildExportObject(Arrays.asList(scheme));
            permissions  = this.workflowHelper.getActionsPermissions(exportObject.getActions());
            response     = Response.ok(new ResponseEntityView(
                    map("workflowObject", new WorkflowSchemeImportExportObjectView(VERSION, exportObject),
                            "permissions", permissions))).build(); // 200
        } catch (Exception e){
            Logger.error(this.getClass(),
                    "Exception on exportScheme, Error exporting the schemes", e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    } // exportScheme.

    /**
     * Do a deep copy of the scheme including steps, action, permissions and so on.
     * You can include a query string name, to include the scheme name
     * @param httpServletRequest  HttpServletRequest
     * @param schemeId String
     * @param name String
     * @param workflowCopyForm (Optional param. use it to set any specifics on the new scheme)
     * @return Response
     */
    @POST
    @Path("/schemes/{schemeId}/copy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response copyScheme(@Context final HttpServletRequest httpServletRequest,
                                     @Context final HttpServletResponse httpServletResponse,
                                     @PathParam("schemeId") final String schemeId,
                                     @QueryParam("name") final String name,
                                     final WorkflowCopyForm workflowCopyForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, httpServletRequest, httpServletResponse,true, null);
        Response response;

        try {
            final Optional<String> workflowName = (
                    UtilMethods.isSet(name) ? Optional.of(name) :
                            (
                                    UtilMethods.isSet(workflowCopyForm) ? Optional
                                            .of(workflowCopyForm.getName()) :
                                            Optional.empty()
                            )
            );

            Logger.debug(this, "Copying the workflow scheme: " + schemeId);
            response     = Response.ok(new ResponseEntityView(
                    this.workflowAPI.deepCopyWorkflowScheme(
                            this.workflowAPI.findScheme(schemeId),
                            initDataObject.getUser(), workflowName))
            ).build(); // 200
        } catch (Exception e){
            Logger.error(this.getClass(),
                    "Exception on exportScheme, Error exporting the schemes", e);
            return ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    } // exportScheme.

    /**
     * Returns all the possible default actions associated to the content type workflow schemes.
     * 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/defaultactions/contenttype/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAvailableDefaultActionsByContentType(@Context final HttpServletRequest request,
                                                                   @Context final HttpServletResponse response,
            @PathParam("contentTypeId")      final String contentTypeId) {
        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this,
                    () -> "Getting the available workflow schemes default action for the ContentType: "
                            + contentTypeId );
            final List<WorkflowDefaultActionView> actions = this.workflowHelper.findAvailableDefaultActionsByContentType(contentTypeId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on find Available Default Actions exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // findAvailableDefaultActionsByContentType.

    /**
     *
     * Returns all the possible default actions associated to the workflow schemes.
     * 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/defaultactions/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAvailableDefaultActionsBySchemes(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("ids") final String schemeIds) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {

            Logger.debug(this,
                    ()->"Getting the available workflow schemes default action for the schemes: "
                            + schemeIds);
            final List<WorkflowDefaultActionView> actions = this.workflowHelper
                    .findAvailableDefaultActionsBySchemes(schemeIds, initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on find Available Default Actions exception message: " + e
                            .getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findAvailableDefaultActionsBySchemes.

    /**
     * Finds the available actions of the initial/first step(s) of the workflow scheme(s) associated
     * with a content type Id.
     * @param request HttpServletRequest
     * @param contentTypeId String
     * @return Response
     */
    @GET
    @Path("/initialactions/contenttype/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findInitialAvailableActionsByContentType(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("contentTypeId") final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        try {
            Logger.debug(this,
                    ()->"Getting the available actions for the contentlet inode: " + contentTypeId);
            final List<WorkflowDefaultActionView> actions = this.workflowHelper
                    .findInitialAvailableActionsByContentType(contentTypeId,
                            initDataObject.getUser());
            return Response.ok(new ResponseEntityView<>(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findInitialAvailableActionsByContentType, content type id: "
                            + contentTypeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findInitialAvailableActionsByContentType.

    /**
     * Creates a new scheme
     *
     * @param request HttpServletRequest
     * @param workflowSchemeForm WorkflowSchemeForm
     * @return Response
     */
    @POST
    @Path("/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveScheme(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                               final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        try {
            DotPreconditions.notNull(workflowSchemeForm,"Expected Request body was empty.");
            Logger.debug(this, ()->"Saving scheme named: " + workflowSchemeForm.getSchemeName());
            final WorkflowScheme scheme = this.workflowHelper.saveOrUpdate(null, workflowSchemeForm, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(scheme)).build(); // 200
        } catch (Exception e) {
            final String schemeName = workflowSchemeForm == null ? "" : workflowSchemeForm.getSchemeName();
            Logger.error(this.getClass(), "Exception on save, schema named: " + schemeName + ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }


    /**
     * Updates an existing scheme
     * @param request HttpServletRequest
     * @param workflowSchemeForm WorkflowSchemeForm
     * @return Response
     */
    @PUT
    @Path("/schemes/{schemeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateScheme(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                 @PathParam("schemeId") final String schemeId,
                                       final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, request, response, true, null);
        Logger.debug(this, "Updating scheme with id: " + schemeId);
        try {
            DotPreconditions.notNull(workflowSchemeForm,"Expected Request body was empty.");
            final User           user   = initDataObject.getUser();
            final WorkflowScheme scheme = this.workflowHelper.saveOrUpdate(schemeId, workflowSchemeForm, user);
            return Response.ok(new ResponseEntityView(scheme)).build(); // 200
        }  catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to update schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Deletes an existing scheme (the response is async)
     * @param request HttpServletRequest
     * @return Response
     */
    @DELETE
    @Path("/schemes/{schemeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final void deleteScheme(@Context final HttpServletRequest request,
                                   @Suspended final AsyncResponse asyncResponse,
                                   @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init(null, request,new EmptyHttpResponse(), true, null);
        Logger.debug(this, ()-> "Deleting scheme with id: " + schemeId);
        try {

            ResponseUtil.handleAsyncResponse(
                    this.workflowHelper.delete(schemeId, initDataObject.getUser()), asyncResponse);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to delete schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    } // deleteScheme.

    /**
     * Returns the status of a specific piece of Content in the Workflow it is assigned to. In
     * summary:
     * <ul>
     *     <li>The Workflow Scheme that the Contentlet is in.</li>
     *     <li>The Step that the Contentlet is in.</li>
     *     <li>The User assigned to such a Step.</li>
     * </ul>
     * Here's an example of how to use this endpoint:
     * <pre>
     *     http://localhost:8080/api/v1/workflow/status/{contentletInode}
     * </pre>
     *
     * @param request         The current instance of the {@link HttpServletRequest}.
     * @param response        The current instance of the {@link HttpServletResponse}.
     * @param contentletInode The inode of the Contentlet whose status will be checked.
     *
     * @return The status information of the Contentlet in the Workflow it is assigned to.
     *
     * @throws DotDataException          The specified Contentlet Inode was not found.
     * @throws DotSecurityException      The User calling this endpoint does not have required
     *                                   permissions to do so.
     * @throws InvocationTargetException Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     * @throws IllegalAccessException    Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     */
    @GET
    @Path("/status/{contentletInode}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final ResponseContentletWorkflowStatusView getStatusForContentlet(@Context final HttpServletRequest request,
                                                                             @Context final HttpServletResponse response,
                                                                             @PathParam("contentletInode") final String contentletInode)
            throws DotDataException, DotSecurityException, InvocationTargetException, IllegalAccessException {
        Logger.debug(this, String.format("Retrieving Workflow status for Contentlet with Inode " +
                "'%s'", contentletInode));
        final InitDataObject initDataObject = this.webResource.init
                (null, request, response, true, null);
        final User user = initDataObject.getUser();
        WorkflowStep wfStep = null;
        WorkflowScheme scheme = null;
        WorkflowTask wfTask = new WorkflowTask();
        final Contentlet contentlet = this.contentletAPI.find(contentletInode, user, false);
        final Optional<WorkflowStep> stepOpt = this.workflowAPI.findCurrentStep(contentlet);
        if (stepOpt.isPresent()) {
            wfStep = stepOpt.get();
            scheme = this.workflowAPI.findScheme(wfStep.getSchemeId());
        }
        final WorkflowTask originalTask = this.workflowAPI.findTaskByContentlet(contentlet);
        if (null != originalTask) {
            BeanUtils.copyProperties(wfTask, originalTask);
            wfTask = this.workflowHelper.handleWorkflowTaskData(wfTask);
        }
        return new ResponseContentletWorkflowStatusView(new ContentletWorkflowStatusView(scheme,
                wfStep, wfTask));
    }

    /**
     * Returns the status of a specific piece of Content in the Workflow it is assigned to. In
     * summary:
     * <ul>
     *     <li>The Workflow Scheme that the Contentlet is in.</li>
     *     <li>The Step that the Contentlet is in.</li>
     *     <li>The User assigned to such a Step.</li>
     * </ul>
     * Here's an example of how to use this endpoint:
     * <pre>
     *     http://localhost:8080/api/v1/workflow/status/{contentletInode}
     * </pre>
     *
     * @param request         The current instance of the {@link HttpServletRequest}.
     * @param response        The current instance of the {@link HttpServletResponse}.
     * @param contentletIdentifier The inode of the Contentlet whose status will be checked.
     *
     * @return The status information of the Contentlet in the Workflow it is assigned to.
     *
     * @throws DotDataException          The specified Contentlet Inode was not found.
     * @throws DotSecurityException      The User calling this endpoint does not have required
     *                                   permissions to do so.
     * @throws InvocationTargetException Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     * @throws IllegalAccessException    Failed to transform the {@link WorkflowTask} data for this
     *                                   view.
     */
    @GET
    @Path("/tasks/history/comments/{contentletIdentifier}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "getWorkflowTasksHistoryComments", summary = "Find workflow tasks history and comments of content",
            description = "Retrieve the workflow tasks comments of a contentlet by its [id]" +
                    "(https://www.dotcms.com/docs/latest/content-versions#IdentifiersInodes).\n\n" +
                    "Returns an object containing the associated [workflow history or comments]" +
                    "https://www2.dotcms.com/docs/latest/workflow-tasks, [workflow task]",
            tags = {"Workflow"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Action(s) returned successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityWorkflowHistoryCommentsView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request")
            }
    )
    public final ResponseEntityWorkflowHistoryCommentsView getWorkflowTasksHistoryComments(@Context final HttpServletRequest request,
                                                                                           @Context final HttpServletResponse response,
                                                                                           @PathParam("contentletIdentifier") @Parameter(
                                                                                                   required = true,
                                                                                                   description = "Id of content  to inspect for workflow tasks.\n\n",
                                                                                                   schema = @Schema(type = "string")
                                                                                           ) final String contentletIdentifier,
                                                                                           @DefaultValue("-1") @QueryParam("language") @Parameter(
                                                                                                   description = "Language version of target content.",
                                                                                                   schema = @Schema(type = "string")) final String language
    )
            throws DotDataException, DotSecurityException, InvocationTargetException, IllegalAccessException {

        Logger.debug(this, String.format("Retrieving Workflow tasks for Contentlet with identifier " +
                "'%s'", contentletIdentifier));
        final InitDataObject initDataObject = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true).requiredFrontendUser(false).init();

        final User user = initDataObject.getUser();
        final long languageId = LanguageUtil.getLanguageId(language);
        final PageMode mode = PageMode.get(request);

        final Optional<Contentlet> currentContentlet =  languageId <= 0?
                this.workflowHelper.getContentletByIdentifier(contentletIdentifier, mode, initDataObject.getUser(),
                        ()->WebAPILocator.getLanguageWebAPI().getLanguage(request).getId()):
                this.contentletAPI.findContentletByIdentifierOrFallback
                        (contentletIdentifier, mode.showLive, languageId, initDataObject.getUser(), mode.respectAnonPerms);

        if (currentContentlet.isPresent()) {

            final WorkflowTask currentWorkflowTask = this.workflowAPI.findTaskByContentlet(currentContentlet.get());
            final List<WorkflowTimelineItem> workflowComments = this.workflowAPI.getCommentsAndChangeHistory(currentWorkflowTask);
            final List<WorkflowTimelineItemView> workflowTimelineItemViews = workflowComments.stream()
                    .map(this::toWorkflowTimelineItemView)
                    .collect(Collectors.toList());
            return new ResponseEntityWorkflowHistoryCommentsView(workflowTimelineItemViews);
        }

        throw new DoesNotExistException("Contentlet with identifier " + contentletIdentifier + " does not exist.");
    }

    private WorkflowTimelineItemView toWorkflowTimelineItemView(final WorkflowTimelineItem wfTimeLine) {

        final WorkflowStep step = wfTimeLine instanceof WorkflowHistory && UtilMethods.isSet(wfTimeLine.stepId())?
                Try.of(()->this.workflowHelper.findStepById(wfTimeLine.stepId())).getOrNull():null;
        final WorkflowAction action = wfTimeLine instanceof WorkflowHistory &&  UtilMethods.isSet(wfTimeLine.actionId())?
                Try.of(()->this.workflowHelper.findAction(wfTimeLine.actionId(), APILocator.systemUser())).getOrNull():null;

        final Map<String, String> minimalStepViewMap = new HashMap<>();
        if (Objects.nonNull(step)) {

            minimalStepViewMap.put("id",step.getId());
            minimalStepViewMap.put("name",step.getName());
            minimalStepViewMap.put("schemeId",step.getSchemeId());
        }

        final Map<String, String> minimalActionViewMap = new HashMap<>();
        if (Objects.nonNull(action)) {

            minimalActionViewMap.put("id",action.getId());
            minimalActionViewMap.put("name",action.getName());
            minimalActionViewMap.put("schemeId",action.getSchemeId());
        }

        final String postedBy = this.workflowHelper.getPostedBy(wfTimeLine.roleId());
        return new WorkflowTimelineItemView(wfTimeLine.createdDate(), wfTimeLine.roleId(), postedBy,
                wfTimeLine.commentDescription(), wfTimeLine.taskId(), wfTimeLine.type(), minimalStepViewMap, minimalActionViewMap);
    }

} // E:O:F:WorkflowResource.
