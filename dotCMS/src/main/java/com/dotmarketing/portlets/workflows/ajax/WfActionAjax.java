package com.dotmarketing.portlets.workflows.ajax;

import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowReorderBean;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowStatus;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class WfActionAjax extends WfBaseAction {

    private final WorkflowHelper workflowHelper = WorkflowHelper.getInstance();
    private final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
	private final UserWebAPI userWebAPI     = WebAPILocator.getUserWebAPI();

    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};

    public void reorder(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {

		final String actionId   = request.getParameter("actionId");
		final String stepId     = request.getParameter("stepId");
		final String orderParam = request.getParameter("order");

		try {

			this.workflowHelper.reorderAction(new WorkflowReorderBean.Builder()
						.actionId(actionId).stepId(stepId)
						.order(Integer.parseInt(orderParam)).build(),
											  this.userWebAPI.getUser(request));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	} // reorder.

	/**
	 * Deletes just the action associated to the step, but the action still alive as part of the scheme.
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void deleteActionForStep(final HttpServletRequest request,
					   final HttpServletResponse response) throws ServletException, IOException {

		final String actionId = request.getParameter("actionId");
		final String stepId   = request.getParameter("stepId");
		WorkflowStep workflowStep = null;

		try {

			Logger.debug(this, "Deleting the action: " + actionId +
							", for the step: " + stepId);
			final User user = this.userWebAPI.getUser(request);
			workflowStep    = this.workflowHelper.deleteAction
					(actionId, stepId, user);
			writeSuccess(response, workflowStep.getSchemeId() );
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	} // delete.

	/**
	 * This method deletes the action associated to the scheme and all references to the steps.
	 * @param request  HttpServletRequest
	 * @param response HttpServletResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	public void delete(final HttpServletRequest request,
                       final HttpServletResponse response) throws ServletException, IOException {

		final String actionId = request.getParameter("actionId");

		try {

			Logger.debug(this, "Deleting the action: " + actionId);
			this.workflowHelper.deleteAction(actionId, this.userWebAPI.getUser(request));


			writeSuccess(response, StringPool.BLANK);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	} // delete.


	public void save(final HttpServletRequest request,
					 final HttpServletResponse response) throws ServletException, IOException {

        final WorkflowActionForm.Builder builder = new WorkflowActionForm.Builder();

        builder.actionName(request.getParameter("actionName"))
                .actionId  (request.getParameter("actionId"))
                .schemeId  (request.getParameter("schemeId"))
				.stepId    (request.getParameter("stepId"))
                .actionIcon(request.getParameter("actionIconSelect"))
                .actionAssignable (request.getParameter("actionAssignable") != null)
                .actionCommentable(request.getParameter("actionCommentable") != null)
				.requiresCheckout(request.getParameter("actionRequiresCheckout") != null)
                .requiresCheckoutOption (request.getParameter("actionRequiresCheckoutOption"))
                .actionRoleHierarchyForAssign(request.getParameter("actionRoleHierarchyForAssign") != null)
                .actionNextStep(request.getParameter  ("actionNextStep"))
                .actionNextAssign(request.getParameter("actionAssignToSelect"))
                .actionCondition(request.getParameter ("actionCondition"))
				.showOn(WorkflowStatus.toSet(request.getParameter ("showOn")));

		final String whoCanUseTmp       = request.getParameter("whoCanUse");
		final List<String> whoCanUse    = Arrays.asList(whoCanUseTmp.split(","));
		builder.whoCanUse(whoCanUse);

        WorkflowAction newAction        = null;
		final User user      = this.userWebAPI.getUser(request);

        try {

            newAction  = this.workflowHelper.save(builder.build(), user);
            response.getWriter().println("SUCCESS:" + newAction.getId());
        } catch (Exception e) {

            Logger.error(this.getClass(), e.getMessage(), e);
            writeError(response, e.getMessage());
        }
    } // save.
}
