package com.dotcms.rest.api.v1.workflow;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.workflow.form.*;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.Beta;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("serial")
@Beta /* Non Official released */
@Path("/v1/workflow")
public class WorkflowResource {

    private final WorkflowHelper workflowHelper;
    private final WebResource webResource;
    private final WorkflowAPI workflowAPI;
    private final ResponseUtil responseUtil;


    /**
     * Default constructor.
     */
    public WorkflowResource() {
        this(WorkflowHelper.getInstance(),
                APILocator.getWorkflowAPI(),
                ResponseUtil.INSTANCE,
                new WebResource());
    }

    @VisibleForTesting
    protected WorkflowResource(final WorkflowHelper workflowHelper,
                               final WorkflowAPI workflowAPI,
                               final ResponseUtil responseUtil,
                               final WebResource webResource) {

        this.workflowHelper = workflowHelper;
        this.webResource    = webResource;
        this.responseUtil   = responseUtil;
        this.workflowAPI    = workflowAPI;
    }

    /**
     * Returns all schemes non-archived. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/schemes")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findSchemes(@Context final HttpServletRequest request) {

        this.webResource.init
                (null, true, request, true, null);
        Response response;
        List<WorkflowScheme> schemes;

        try {

            Logger.debug(this, "Getting the workflow schemes");
            schemes   = this.workflowHelper.findSchemes();

            response  =
                    Response.ok(new ResponseEntityView(schemes)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findSchemes exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // findSchemes.

    /**
     * Returns all schemes for the content type. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @return Response
     */
    @GET
    @Path("/schemes/contenttypes/{contentTypeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findSchemesByContentType(@Context                         final HttpServletRequest request,
                                                   @PathParam("contentTypeId")      final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        List<WorkflowScheme> schemes;

        try {

            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId);
            schemes   = this.workflowHelper.findSchemesByContentType
                    (contentTypeId, initDataObject.getUser());
            response  =
                    Response.ok(new ResponseEntityView(schemes)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findSchemesByContentType exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // findSchemesByContentType.

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
    public final Response findAllSchemesAndSchemesByContentType(@Context            final HttpServletRequest request,
                                                   @PathParam("contentTypeId")      final String contentTypeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        List<WorkflowScheme> schemes;
        List<WorkflowScheme> contentTypeSchemes;

        try {

            Logger.debug(this,
                    "Getting the workflow schemes for the contentTypeId: " + contentTypeId
                            + " and including All Schemes");
            schemes   = this.workflowHelper.findSchemes();
            contentTypeSchemes = this.workflowHelper.findSchemesByContentType
                    (contentTypeId, initDataObject.getUser());

            response  =
                    Response.ok(new ResponseEntityView(new SchemesAndSchemesContentTypeView
                            (schemes, contentTypeSchemes))).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findSchemes exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // findAllSchemesAndSchemesByContentType.

    /**
     * Returns a single action associated to the step, 404 if does not exists. 401 if the user does not have permission.
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
        Response response;
        List<WorkflowStep> steps;

        try {

            Logger.debug(this, "Getting the workflow steps for the scheme: " + schemeId);
            steps     = this.workflowHelper.findSteps(schemeId);

            response  =
                    Response.ok(new ResponseEntityView(steps)).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on findStepsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findStepsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // findSteps.

    /**
     * Returns a single action associated to the step, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @return Response
     */
    @GET
    @Path("/actions/{actionId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findAction(@Context final HttpServletRequest request,
                                     @PathParam("actionId") final String actionId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        WorkflowAction action;
        final Locale locale = LocaleUtil.getLocale(request);
        final User user   = initDataObject.getUser();

        try {

            Logger.debug(this, "Finding the workflow action " + actionId);
            action    = this.workflowAPI.findAction(actionId, user);

            response  = (null == action)?
                    this.responseUtil.getErrorResponse(request, Response.Status.NOT_FOUND, locale, user.getUserId(), "Workflow-does-not-exists-action"):
                    Response.ok(new ResponseEntityView(action)).build(); // 200
        } catch (DotSecurityException e) {

            Logger.error(this.getClass(),
                    "DotSecurityException on findActionByStep, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findActionByStep, actionId: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
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
        Response response;
        WorkflowAction action;
        final Locale locale = LocaleUtil.getLocale(request);
        final User   user   = initDataObject.getUser();

        try {

            Logger.debug(this, "Getting the workflow action " + actionId + " for the step: " + stepId);
            action    = this.workflowAPI.findAction(actionId, stepId, user);

            response  = (null == action)?
                    this.responseUtil.getErrorResponse(request, Response.Status.NOT_FOUND, locale, user.getUserId(), "Workflow-does-not-exists-action"):
                    Response.ok(new ResponseEntityView(action)).build(); // 200
        } catch (DotSecurityException e) {

            Logger.error(this.getClass(),
                    "DotSecurityException on findActionByStep, actionId: " + actionId +
                            ", stepId: " + stepId + ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findActionByStep, actionId: " + actionId +
                            ", stepId: " + stepId + ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
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
        Response response;
        List<WorkflowAction> actions;
        final User   user   = initDataObject.getUser();

        try {

            Logger.debug(this, "Getting the workflow actions for the step: " + stepId);
            actions   = this.workflowHelper.findActions(stepId, user);

            response  =
                    Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on findActionsByStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findActionsByStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
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
        Response response;
        List<WorkflowAction> actions;

        try {

            Logger.debug(this, "Getting the workflow actions: " + schemeId);
            actions   = this.workflowHelper.findActionsByScheme(schemeId, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on findActionsByScheme, schemeId: " + schemeId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
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
    public final Response save(@Context final HttpServletRequest request,
                               final WorkflowActionForm workflowActionForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        WorkflowAction newAction;

        try {

            Logger.debug(this, "Saving new workflow action: " + workflowActionForm.getActionName());
            newAction = this.workflowHelper.save(workflowActionForm, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(newAction)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on save, workflowActionForm: " + workflowActionForm +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // save

    /**
     * Saves an action into a step
     * @param request                   HttpServletRequest
     * @param workflowActionStepForm    WorkflowActionStepForm
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
        Response response;

        try {

            Logger.debug(this, "Saving a workflow action " + workflowActionStepForm.getActionId()
                    + " in to a step: " + stepId);
            this.workflowHelper.saveActionToStep(new WorkflowActionStepBean.Builder().stepId(stepId)
                    .actionId(workflowActionStepForm.getActionId()).build(), initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView("ok")).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on saveAction, workflowActionForm: " + workflowActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // saveAction


    /**
     * Deletes a step
     * @param request                   HttpServletRequest
     * @param stepId                   String
     * @return Response
     */
    @DELETE
    @Path("/steps/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteStep(@Context final HttpServletRequest request,
                                     @PathParam("stepId") final String stepId) {

        this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Deleting the step: " + stepId);
            this.workflowHelper.deleteStep(stepId);
            response  = Response.ok(new ResponseEntityView("ok")).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on deleteStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on deleteStep, stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
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
        Response response;

        try {

            Logger.debug(this, "Deleting the action: " + actionId + " for the step: " + stepId);
            this.workflowHelper.deleteAction(actionId, stepId, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView("ok")).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on deleteAction, action: " + actionId
                            + ", stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on deleteAction, action: " + actionId
                            + " stepId: " + stepId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
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
        Response response;

        try {

            Logger.debug(this, "Deleting the action: " + actionId);
            this.workflowHelper.deleteAction(actionId, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView("ok")).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on deleteAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on deleteAction, action: " + actionId +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // deleteAction

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
        Response response;

        try {

            Logger.debug(this, "Doing reordering of: " + workflowReorderActionStepForm);
            this.workflowHelper.reorderAction(
                    new WorkflowReorderBean.Builder().stepId(stepId).actionId(actionId)
                            .order(workflowReorderActionStepForm.getOrder()).build(),
                    initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView("Ok")).build(); // 200
        } catch (DoesNotExistException e) {

            Logger.error(this.getClass(),
                    "DoesNotExistException on reorderAction, workflowReorderActionStepForm: " + workflowReorderActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on reorderAction, workflowReorderActionStepForm: " + workflowReorderActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // reorderAction
} // E:O:F:WorkflowResource.
