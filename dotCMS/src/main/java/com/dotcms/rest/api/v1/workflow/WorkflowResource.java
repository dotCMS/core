package com.dotcms.rest.api.v1.workflow;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.container.AsyncResponse;
import com.dotcms.repackage.javax.ws.rs.container.Suspended;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.IncludePermissions;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.workflow.form.*;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.dotcms.rest.ResponseEntityView.OK;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotcms.util.DotLambdas.not;

@SuppressWarnings("serial")
@Path("/v1/workflow")
public class WorkflowResource {

    public  final static String VERSION = "1.0";
    private static final String LISTING = "listing";
    private static final String EDITING = "editing";

    private final WorkflowHelper   workflowHelper;
    private final ContentHelper    contentHelper;
    private final WebResource      webResource;
    private final WorkflowAPI      workflowAPI;
    private final ResponseUtil     responseUtil;
    private final ContentletAPI    contentletAPI;
    private final PermissionAPI    permissionAPI;
    private final WorkflowImportExportUtil workflowImportExportUtil;
    private final Set<String> validRenderModeSet = ImmutableSet.of(LISTING, EDITING);


    /**
     * Default constructor.
     */
    public WorkflowResource() {
        this(WorkflowHelper.getInstance(),
                ContentHelper.getInstance(),
                APILocator.getWorkflowAPI(),
                APILocator.getContentletAPI(),
                ResponseUtil.INSTANCE,
                APILocator.getPermissionAPI(),
                WorkflowImportExportUtil.getInstance(),
                new WebResource());
    }

    @VisibleForTesting
    WorkflowResource(final WorkflowHelper workflowHelper,
                               final ContentHelper    contentHelper,
                               final WorkflowAPI      workflowAPI,
                               final ContentletAPI    contentletAPI,
                               final ResponseUtil     responseUtil,
                               final PermissionAPI    permissionAPI,
                               final WorkflowImportExportUtil workflowImportExportUtil,
                               final WebResource webResource) {

        this.workflowHelper           = workflowHelper;
        this.contentHelper            = contentHelper;
        this.webResource              = webResource;
        this.responseUtil             = responseUtil;
        this.workflowAPI              = workflowAPI;
        this.permissionAPI            = permissionAPI;
        this.contentletAPI            = contentletAPI;
        this.workflowImportExportUtil = workflowImportExportUtil;

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
                                      @QueryParam("contentTypeId") final String  contentTypeId,
                                      @DefaultValue("true") @QueryParam("showArchive")  final boolean showArchived) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId);
            final List<WorkflowScheme> schemes = (null != contentTypeId) ?
                    ((showArchived)?
                            this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser()):
                            this.workflowHelper.findSchemesByContentType(contentTypeId, initDataObject.getUser())
                                    .stream().filter(not(WorkflowScheme::isArchived)).collect(CollectionsUtils.toImmutableList())):
                    this.workflowHelper.findSchemes(showArchived);

            return Response.ok(new ResponseEntityView(schemes)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findSchemes exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findSchemes.

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
            @PathParam("contentTypeId") final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

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
                                            @PathParam("schemeId") final String schemeId) {

        this.webResource.init
                (null, true, request, true, null);

        try {
            Logger.debug(this, "Getting the workflow steps for the scheme: " + schemeId);
            final List<WorkflowStep> steps = this.workflowHelper.findSteps(schemeId);
            return Response.ok(new ResponseEntityView(steps)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception on findStepsByScheme exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);

        }
    } // findSteps.

    /**
     * Finds the available actions for an inode
     *
     * @param request HttpServletRequest
     * @param inode String
     * @param renderMode String, this is an uncase sensitive query string (?renderMode=) optional parameter.
     *                   By default the findAvailableAction will run on WorkflowAPI.RenderMode.EDITING, if you want to run for instance on WorkflowAPI.RenderMode.LISTING
     *                   you can send the renderMode parameter as ?renderMode=listing
     *                   This will be used to filter the action based on the show on configuration for each action.
     * @return Response
     */
    @GET
    @Path("/contentlet/{inode}/actions")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAvailableActions(@Context final HttpServletRequest request,
                                               @PathParam("inode")  final String inode,
                                               @QueryParam("renderMode") final String renderMode) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this, ()->"Getting the available actions for the contentlet inode: " + inode);

            this.workflowHelper.checkRenderMode (renderMode, initDataObject.getUser(), this.validRenderModeSet);

            final List<WorkflowAction> actions = this.workflowHelper.findAvailableActions(inode, initDataObject.getUser(),
                    LISTING.equalsIgnoreCase(renderMode)?WorkflowAPI.RenderMode.LISTING:WorkflowAPI.RenderMode.EDITING);
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findAvailableActions, contentlet inode: " + inode +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findAvailableActions.



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
                                         final BulkActionForm bulkActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
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

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
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
                                     @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this, ()->"Finding the workflow action " + actionId);
            final WorkflowAction action = this.workflowHelper.findAction(actionId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(action)).build(); // 200
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
                (null, true, request, true, null);
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
                                           @PathParam("stepId")   final String stepId,
                                           @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
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
                                            @PathParam("stepId")   final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        final User user = initDataObject.getUser();
        try {
            Logger.debug(this, "Getting the workflow actions for the step: " + stepId);
            final List<WorkflowAction> actions = this.workflowHelper.findActions(stepId, user);
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
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
                                              @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this, "Getting the workflow actions: " + schemeId);
            final List<WorkflowAction> actions = this.workflowHelper.findActionsByScheme(schemeId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on findActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    } // findActionsByScheme.

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
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveAction(@Context final HttpServletRequest request,
                               final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
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
                                       @PathParam("actionId") final String actionId,
                                       final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
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
                                           @PathParam("stepId")   final String stepId,
                                           final WorkflowActionStepForm workflowActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            DotPreconditions.notNull(workflowActionStepForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving a workflow action " + workflowActionStepForm.getActionId()
                    + " in to a step: " + stepId);
            this.workflowHelper.saveActionToStep(new WorkflowActionStepBean.Builder().stepId(stepId)
                    .actionId(workflowActionStepForm.getActionId()).build(), initDataObject.getUser());
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Exception on updateAction, stepId: "+stepId+", saveActionToStep: " + workflowActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }

    } // saveAction

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
                (null, true, request, true, null);

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
                                       @PathParam("actionId") final String actionId,
                                       @PathParam("stepId")   final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

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
                                       @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

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
                                        @PathParam("stepId")   final String stepId, 
                                        @PathParam("order")    final int order) {
        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

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
                                     @NotNull @PathParam("stepId") final String stepId,
                                     final WorkflowStepUpdateForm stepForm) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
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
                                  final WorkflowStepAddForm newStepForm) {
        String schemeId = null;
        try {
            DotPreconditions.notNull(newStepForm,"Expected Request body was empty.");
            schemeId = newStepForm.getSchemeId();
            final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
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
                                       @NotNull @PathParam("stepId") final String stepId) {
        this.webResource.init(null, true, request, true, null);
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
     * Fires a workflow action
     * @param request HttpServletRequest
     * @param inode String
     * @param actionId String
     * @param fireActionForm FireActionForm This param is mandatory only is the inode isn't sent
     * (if an inode is set, this param is not ignored).
     * @return Response
     */
    @PUT
    @Path("/actions/{actionId}/fire")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response fireAction(@Context final HttpServletRequest request,
                               @QueryParam("inode")            final String inode,
                               @NotNull @PathParam("actionId") final String actionId,
                               final FireActionForm fireActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

        try {

            Contentlet contentlet = null;
            //if inode is set we use it to look up a contentlet
            if(UtilMethods.isSet(inode)) {

                final Contentlet currentContentlet = this.contentletAPI.find
                        (inode, initDataObject.getUser(), false);

                DotPreconditions.notNull(currentContentlet,
                        ()-> "contentlet-was-not-found",
                        DoesNotExistException.class);

                contentlet = new Contentlet();
                contentlet.getMap().putAll(currentContentlet.getMap());

                if (null != fireActionForm && null != contentlet) {

                    contentlet = this.populateContentlet(fireActionForm, contentlet, initDataObject.getUser());
                }
            } else {
                //otherwise the information must be grabbed from the request body.
                DotPreconditions.notNull(fireActionForm, ()-> "When no inode is sent the info on the Request body becomes mandatory.");
                contentlet = this.populateContentlet(fireActionForm, initDataObject.getUser());
            }

            Logger.debug(this, ()-> "Firing workflow action: " + actionId + ", inode: " + inode);

            if(null == contentlet || contentlet.getMap().isEmpty()){
                throw new DoesNotExistException("contentlet-was-not-found");
            }

            final ContentletDependencies.Builder formBuilder = new ContentletDependencies.Builder();
            formBuilder.respectAnonymousPermissions(PageMode.get(request).respectAnonPerms).
                    workflowActionId(actionId).modUser(initDataObject.getUser())
                    .indexPolicy(IndexPolicyProvider.getInstance().forSingleContent());

            if(fireActionForm != null) {
                formBuilder.workflowActionComments(fireActionForm.getComments())
                        .workflowAssignKey(fireActionForm.getAssign());
            }

            return Response.ok(
                    new ResponseEntityView(
                       this.workflowAPI.fireContentWorkflow(contentlet, formBuilder.build())
                    )
            ).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on firing, workflow action: " + actionId +
                            ", inode: " + inode, e);

            return ResponseUtil.mapExceptionResponse(e);
        }
    } // fire.

    /**
     * Internal utility to populate a contentlet from a given form object
     * @param fireActionForm FireActionForm
     * @param user User
     * @return Contentlet
     * @throws DotSecurityException
     */
    private Contentlet populateContentlet(final FireActionForm fireActionForm, final User user)
            throws DotSecurityException {

        return this.populateContentlet(fireActionForm, new Contentlet(), user);
    } // populateContentlet.

    /**
     * Internal utility to populate a contentlet from a given form object
     * @param fireActionForm {@link FireActionForm}
     * @param contentletInput {@link Contentlet}
     * @param user {@link User}
     * @return Contentlet
     * @throws DotSecurityException
     */
    private Contentlet populateContentlet(final FireActionForm fireActionForm, final Contentlet contentletInput, final User user)
            throws DotSecurityException {

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

        try {
            if (!this.permissionAPI.doesUserHavePermission(contentlet.getContentType(),
                    PermissionAPI.PERMISSION_READ, user, false)) {
                throw new DotSecurityException(errorMessageSupplier.get());
            }
        } catch (DotDataException e) {
            throw new DotSecurityException(errorMessageSupplier.get(), e);
        }

        contentlet.setStringProperty("wfPublishDate", fireActionForm.getPublishDate());
        contentlet.setStringProperty("wfPublishTime", fireActionForm.getPublishTime());
        contentlet.setStringProperty("wfExpireDate", fireActionForm.getExpireDate());
        contentlet.setStringProperty("wfExpireTime", fireActionForm.getExpireTime());
        contentlet.setStringProperty("wfNeverExpire", fireActionForm.getNeverExpire());
        contentlet.setStringProperty("whereToSend", fireActionForm.getWhereToSend());
        contentlet.setStringProperty("forcePush", fireActionForm.getForcePush());

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
                                        @PathParam("stepId")   final String stepId,
                                        @PathParam("actionId") final String actionId,
                                        final WorkflowReorderWorkflowActionStepForm workflowReorderActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);

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
     * @param request HttpServletRequest
     * @param workflowSchemeImportForm WorkflowSchemeImportObjectForm
     * @return Response
     */
    @POST
    @Path("/schemes/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response importScheme(@Context final HttpServletRequest request,
                                       final WorkflowSchemeImportObjectForm workflowSchemeImportForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
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
     * @param request  HttpServletRequest
     * @param schemeId String
     * @return Response
     */
    @GET
    @Path("/schemes/{schemeId}/export")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response exportScheme(@Context final HttpServletRequest request,
                                              @PathParam("schemeId") final String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
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
     * @param request  HttpServletRequest
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
    public final Response copyScheme(@Context final HttpServletRequest request,
                               @PathParam("schemeId") final String schemeId,
                               @QueryParam("name") final String name,
                               final WorkflowCopyForm workflowCopyForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
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
            @PathParam("contentTypeId")      final String contentTypeId) {
        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this,
                    () -> "Getting the available workflow schemes default action for the ContentType: "
                            + contentTypeId );
            final List<WorkflowDefaultActionView> actions = this.workflowHelper.findAvailableDefaultActionsByContentType(contentTypeId, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
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
            @QueryParam("ids") final String schemeIds) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {

            Logger.debug(this,
                    "Getting the available workflow schemes default action for the schemes: "
                            + schemeIds);
            final List<WorkflowDefaultActionView> actions = this.workflowHelper
                    .findAvailableDefaultActionsBySchemes(schemeIds, initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
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
            @PathParam("contentTypeId") final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        try {
            Logger.debug(this,
                    "Getting the available actions for the contentlet inode: " + contentTypeId);
            final List<WorkflowDefaultActionView> actions = this.workflowHelper
                    .findInitialAvailableActionsByContentType(contentTypeId,
                            initDataObject.getUser());
            return Response.ok(new ResponseEntityView(actions)).build(); // 200
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
                               final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        try {
            DotPreconditions.notNull(workflowSchemeForm,"Expected Request body was empty.");
            Logger.debug(this, "Saving scheme named: " + workflowSchemeForm.getSchemeName());
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
                                 @PathParam("schemeId") final String schemeId,
                                 final WorkflowSchemeForm workflowSchemeForm) {
        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
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

        final InitDataObject initDataObject = this.webResource.init(null, true, request, true, null);
        Logger.debug(this, ()-> "Deleting scheme with id: " + schemeId);
        try {

            ResponseUtil.handleAsyncResponse(
                    this.workflowHelper.delete(schemeId, initDataObject.getUser()), asyncResponse);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception attempting to delete schema identified by : " +schemeId + ", exception message: " + e.getMessage(), e);
            asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
        }
    } // deleteScheme.

} // E:O:F:WorkflowResource.