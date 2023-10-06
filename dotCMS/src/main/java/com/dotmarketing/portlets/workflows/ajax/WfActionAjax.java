package com.dotmarketing.portlets.workflows.ajax;

import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowReorderBean;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Deprecated(forRemoval = true)
public class WfActionAjax extends WfBaseAction {

    private final WorkflowHelper workflowHelper = WorkflowHelper.getInstance();
	private final UserWebAPI     userWebAPI     = WebAPILocator.getUserWebAPI();

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
	 * @throws IOException
	 */
	public void deleteActionForStep(final HttpServletRequest request,
					   final HttpServletResponse response) throws IOException {

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
					 final HttpServletResponse response) throws ServletException, IOException, DotDataException {
		final String actionId = request.getParameter("actionId");
		final String actionName = request.getParameter("actionName");
        final WorkflowActionForm.Builder builder = new WorkflowActionForm.Builder();
		if (!WorkflowAction.SEPARATOR.equalsIgnoreCase(actionId)) {
			builder.actionName(actionName)
					.actionId(actionId)
					.schemeId(request.getParameter("schemeId"))
					.stepId(request.getParameter("stepId"))
					.actionIcon(request.getParameter("actionIconSelect"))
					.actionAssignable(request.getParameter("actionAssignable") != null)
					.actionCommentable(request.getParameter("actionCommentable") != null)
					.requiresCheckout(false)
					.actionRoleHierarchyForAssign(request.getParameter("actionRoleHierarchyForAssign") != null)
					.actionNextStep(request.getParameter("actionNextStep"))
					.actionNextAssign(request.getParameter("actionAssignToSelect"))
					.actionCondition(request.getParameter("actionCondition"))
					.showOn(WorkflowState.toSet(request.getParameterValues("showOn")));
		} else {
			createSeparatorAction(builder, request);
		}
		final String whoCanUseTmp = UtilMethods.isSet(request.getParameter("whoCanUse"))
				? request.getParameter("whoCanUse")
				: StringPool.BLANK;
		final List<String> whoCanUse = Arrays.asList(whoCanUseTmp.split(StringPool.COMMA));
		builder.whoCanUse(whoCanUse);

        WorkflowAction newAction;
		final User user      = this.userWebAPI.getUser(request);

        try {
            newAction  = this.workflowHelper.saveAction(builder.build(), user);
            response.getWriter().println("SUCCESS:" + newAction.getId());
        } catch (final Exception e) {
			Logger.error(this.getClass(), String.format("An error occurred when saving Workflow " +
					"Action '%s' [ %s ]: %s", actionName, actionId, e.getMessage()), e);
            writeError(response, e.getMessage());
        }
    } // save.

	/**
	 * Creates a separator action for a specific Workflow Step. This separator is simply a special
	 * type of Workflow Action that is only meant to allow Users to create groups of actions.
	 *
	 * @param builder The current instance of the {@link WorkflowActionForm.Builder} class.
	 * @param request The current {@link HttpServletRequest} instance.
	 *
	 * @throws DotDataException An error occurred when persisting the separator.
	 */
	private void createSeparatorAction(final WorkflowActionForm.Builder builder,
									   final HttpServletRequest request) throws DotDataException {
		builder.actionName("-------- SEPARATOR --------")
				.schemeId(request.getParameter("schemeId"))
				.stepId(request.getParameter("stepId"))
				.actionAssignable(false)
				.actionCommentable(false)
				.requiresCheckout(false)
				.actionRoleHierarchyForAssign(false)
				.actionNextStep(WorkflowAction.CURRENT_STEP)
				.actionNextAssign(APILocator.getRoleAPI().loadRoleByKey(Role.CMS_ANONYMOUS_ROLE).getId())
				.actionCondition(WorkflowAction.SEPARATOR)
				.showOn(WorkflowState.toSet(null));
	}

	/**
	 * Security check demanded by Sonar
	 * We register all the allowed methods down here
	 *
	 * @return allowed method names
	 */
	@Override
	protected Set<String> getAllowedCommands() {
		return Set.of( "action", "reorder", "delete", "add", "save", "deleteActionForStep" );
	}

}
