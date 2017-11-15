package com.dotcms.rest.api.v1.workflow;

import com.dotcms.business.WrapInTransaction;
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
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowActionStepForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.Beta;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

import static com.dotcms.util.CollectionsUtils.map;

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
     * Returns a single action associated to the step, 404 if does not exists. 401 if the user does not have permission.
     * @param request  HttpServletRequest
     * @param actionId String
     * @param stepId String
     * @return Response
     */
    @GET
    @Path("/action/{actionId}/step/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionByStep(@Context final HttpServletRequest request,
                                              @PathParam("actionId") final String actionId,
                                              @PathParam("stepId")   final String stepId) {

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
    @Path("/actions/step/{stepId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsByStep(@Context final HttpServletRequest request,
                                           @PathParam("stepId")   final String stepId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        List<WorkflowAction> actions;
        final Locale locale = LocaleUtil.getLocale(request);
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
                    "DoesNotExistException on findActionsByStep, stepId: " + stepId +
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
    @Path("/actions/scheme/{schemeId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsByScheme(@Context final HttpServletRequest request,
                                              @PathParam("schemeId") String schemeId) {

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
    @Path("/action")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @WrapInTransaction
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
    @Path("/action/step")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @WrapInTransaction
    public final Response saveActionToStep(@Context final HttpServletRequest request,
                               final WorkflowActionStepForm workflowActionStepForm) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;

        try {

            Logger.debug(this, "Saving a workflow action " + workflowActionStepForm.getActionId()
                    + " in to a step: " + workflowActionStepForm.getStepId());
            this.workflowHelper.saveActionToStep(workflowActionStepForm, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView("ok")).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(),
                    "Exception on saveActionToStep, workflowActionForm: " + workflowActionStepForm +
                            ", exception message: " + e.getMessage(), e);
            response = (e.getCause() instanceof SecurityException)?
                    ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED) :
                    ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // saveActionToStep


} // E:O:F:WorkflowResource.
