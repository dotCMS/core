package com.dotmarketing.portlets.workflows.ajax;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class WfActionAjax extends WfBaseAction {

    private final WorkflowHelper workflowHelper = WorkflowHelper.getInstance();
    private final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};

    public void reorder(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {

		final  String actionId = request.getParameter("actionId");
		final String orderParam = request.getParameter("order");

		try {
			final int order = Integer.parseInt(orderParam);
			//anyone with permission the workflowscheme portlet can reorder actions
			final WorkflowAction action =
                    this.workflowAPI.findAction(actionId, APILocator.getUserAPI().getSystemUser());

            this.workflowAPI.reorderAction(action, order);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	} // reorder.

	
	public void delete(final HttpServletRequest request,
                       final HttpServletResponse response) throws ServletException, IOException {

		final String actionId = request.getParameter("actionId");

		try {

			final WorkflowAction action =
                    this.workflowAPI.findAction(actionId, APILocator.getUserAPI().getSystemUser());
            final WorkflowStep step =
                    this.workflowAPI.findStep(action.getStepId());

            this.workflowAPI.deleteAction(action);
			writeSuccess(response, step.getSchemeId() );
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	} // delete.

    @WrapInTransaction
	public void save(final HttpServletRequest request,
					 final HttpServletResponse response) throws ServletException, IOException {

        final WorkflowActionForm.Builder builder = new WorkflowActionForm.Builder();

        builder.actionName(request.getParameter("actionName"))
                .actionId  (request.getParameter("actionId"))
                .schemeId  (request.getParameter("schemeId"))
                .actionIcon(request.getParameter("actionIconSelect"))
                .actionAssignable (request.getParameter("actionAssignable") != null)
                .actionCommentable(request.getParameter("actionCommentable") != null)
                .requiresCheckout (request.getParameter("actionRequiresCheckout") != null)
                .actionRoleHierarchyForAssign(request.getParameter("actionRoleHierarchyForAssign") != null)
                .actionNextStep(request.getParameter  ("actionNextStep"))
                .actionNextAssign(request.getParameter("actionAssignToSelect"))
                .actionCondition(request.getParameter ("actionCondition"));

		final String whoCanUseTmp       = request.getParameter("whoCanUse");
		final List<String> whoCanUse    = Arrays.asList(whoCanUseTmp.split(","));
		builder.whoCanUse(whoCanUse);

        WorkflowAction newAction        = null;

        try {

            newAction  = this.workflowHelper.save(builder.build());
            response.getWriter().println("SUCCESS:" + newAction.getId());
        } catch (Exception e) {

            Logger.error(this.getClass(), e.getMessage(), e);
            writeError(response, e.getMessage());
        }
    } // save.
}
