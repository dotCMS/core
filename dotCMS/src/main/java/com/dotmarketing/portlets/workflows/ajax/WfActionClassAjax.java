package com.dotmarketing.portlets.workflows.ajax;

import com.dotcms.util.AnnotationUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.util.ActionletUtil;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Deprecated
public class WfActionClassAjax extends WfBaseAction {

	private final UserWebAPI  userWebAPI     = WebAPILocator.getUserWebAPI();
	private final WorkflowAPI workflowAPI    = APILocator.getWorkflowAPI();

	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};

	public void reorder(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final String actionClassId = request.getParameter("actionClassId");
		final String o = request.getParameter("order");

		try {
			final User user  = this.userWebAPI.getUser(request);
			final int  order = Integer.parseInt(o);
			final WorkflowActionClass actionClass = this.workflowAPI.findActionClass(actionClassId);

			if(actionClass.getOrder() != order) { // Reorder ONLY when position changed
				this.workflowAPI.reorderActionClass(actionClass, order, user);
			}
		} catch (Exception e) {

			// dojo sends this Ajax method "reorder Actions" calls, which fail.  Not sure why
			//Logger.error(this.getClass(), e.getMessage(), e);
			//writeError(response, e.getMessage());
		}
	}

	public void delete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		final String actionClassId = request.getParameter("actionClassId");

		try {

			final User   user     				  = this.userWebAPI.getUser(request);
			final WorkflowActionClass actionClass = this.workflowAPI.findActionClass(actionClassId);
			this.workflowAPI.deleteActionClass(actionClass, user);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	}

	public void add(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException {

		final User   user     						  = this.userWebAPI.getUser(request);
		final String actionId						  = request.getParameter("actionId");
		final String actionName   					  = request.getParameter("actionletName");
		final String clazz 							  = request.getParameter("actionletClass");
		final WorkflowActionClass workflowActionClass = new WorkflowActionClass();

		try {
			// We don't need to get "complete" action object from the database
			// to retrieve all action classes from him. So, we can create simple action object
			// with the "action id" contain in actionClass parameter.
			final WorkflowAction action = new WorkflowAction();
			action.setId(actionId);

			final List<WorkflowActionClass> classes = this.workflowAPI.findActionClasses(action);
			if (classes != null) {
				workflowActionClass.setOrder(classes.size());
			}
			workflowActionClass.setClazz(clazz);
			workflowActionClass.setName(actionName);
			workflowActionClass.setActionId(actionId);
			this.workflowAPI.saveActionClass(workflowActionClass, user);

			final boolean isOnlyBatch = ActionletUtil.isOnlyBatch(clazz);
			response.setContentType("text/plain");
			response.getWriter().println(String.format("%s:%s:%s",workflowActionClass.getId(),
					workflowActionClass.getName(), Boolean.toString(isOnlyBatch)));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	}

	public void save(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			final User user      										  = this.userWebAPI.getUser(request);
			final String actionClassId									  = request.getParameter("actionClassId");
			final WorkflowActionClass workflowActionClass  				  = this.workflowAPI.findActionClass(actionClassId);
			final WorkFlowActionlet actionlet 							  = this.workflowAPI.findActionlet(workflowActionClass.getClazz());
			final List<WorkflowActionletParameter> params				  = actionlet.getParameters();
			final Map<String, WorkflowActionClassParameter> enteredParams = this.workflowAPI.findParamsForActionClass(workflowActionClass);
			final List<WorkflowActionClassParameter> newParams 			  = new ArrayList<>();
			String userIds = null;

			for (final WorkflowActionletParameter expectedParam : params) {

				WorkflowActionClassParameter enteredParam = enteredParams.get(expectedParam.getKey());
				if (enteredParam == null) {
					enteredParam = new WorkflowActionClassParameter();
				}
				enteredParam.setActionClassId(workflowActionClass.getId());
				enteredParam.setKey(expectedParam.getKey());
				enteredParam.setValue(request.getParameter("acp-" + expectedParam.getKey()));
				newParams.add(enteredParam);
				userIds = enteredParam.getValue();
				//Validate userIds or emails
				final String errors = expectedParam.hasError(userIds);
				if(errors != null){
					writeError(response, errors);
					return;
				}
			}

			this.workflowAPI.saveWorkflowActionClassParameters(newParams, user);
			response.getWriter().println(workflowActionClass.getId() + ":" + workflowActionClass.getName());
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	}

	/**
	 * Security check demanded by Sonar
	 * We register all the allowed methods down here
	 *
	 * @return allowed method names
	 */
	@Override
	protected Set<String> getAllowedCommands() {
		return Set.of("save","add","delete","reorder","action");
	}
}

