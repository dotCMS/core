package com.dotmarketing.portlets.workflows.ajax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.MultiEmailParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;

public class WfActionClassAjax extends WfBaseAction {
	 public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};
	public void reorder(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String actionClassId = request.getParameter("actionClassId");
		String o = request.getParameter("order");
		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		try {
			int order = Integer.parseInt(o);
			WorkflowActionClass actionClass = wapi.findActionClass(actionClassId);
			wapi.reorderActionClass(actionClass, order);
		} catch (Exception e) {
			
			// dojo sends this Ajax method "reorder Actions" calls, which fail.  Not sure why 
			//Logger.error(this.getClass(), e.getMessage(), e);
			//writeError(response, e.getMessage());
		}

	}

	public void delete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String actionClassId = request.getParameter("actionClassId");

		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		try {

			WorkflowActionClass actionClass = wapi.findActionClass(actionClassId);
			wapi.deleteActionClass(actionClass);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}

	}

	public void add(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		String actionId = request.getParameter("actionId");
		String actionName = request.getParameter("actionletName");
		String clazz = request.getParameter("actionletClass");
		WorkflowActionClass wac = new WorkflowActionClass();

		try {
			WorkflowAction action = wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());
			List<WorkflowActionClass> classes = wapi.findActionClasses(action);
			if (classes != null) {
				wac.setOrder(classes.size());
			}
			wac.setClazz(clazz);
			wac.setName(actionName);
			wac.setActionId(actionId);
			wapi.saveActionClass(wac);

			response.getWriter().println(wac.getId() + ":" + wac.getName());
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	}

	public void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		try {
			String actionClassId = request.getParameter("actionClassId");
			WorkflowActionClass wac = wapi.findActionClass(actionClassId);
			WorkFlowActionlet actionlet = wapi.findActionlet(wac.getClazz());
			List<WorkflowActionletParameter> params = actionlet.getParameters();
			Map<String, WorkflowActionClassParameter> enteredParams = wapi.findParamsForActionClass(wac);
			List<WorkflowActionClassParameter> newParams = new ArrayList<WorkflowActionClassParameter>();			
			String userIds = null;
			for (WorkflowActionletParameter expectedParam : params) {				
				WorkflowActionClassParameter enteredParam = enteredParams.get(expectedParam.getKey());
				if (enteredParam == null) {
					enteredParam = new WorkflowActionClassParameter();
				}

				enteredParam.setActionClassId(wac.getId());

				enteredParam.setKey(expectedParam.getKey());
				enteredParam.setValue(request.getParameter("acp-" + expectedParam.getKey()));
				newParams.add(enteredParam);
				if(enteredParam.getKey().equalsIgnoreCase("approvers")){
					MultiEmailParameter emailParameter = new MultiEmailParameter(expectedParam.getKey(), expectedParam.getDisplayName(), expectedParam.getDefaultValue(), expectedParam.isRequired());
					userIds = enteredParam.getValue();
					//Validate userIds or emails
					String errors = emailParameter.hasError(userIds);
					if(errors.length() > 0){
						writeError(response, errors);
						return;
					}
				}
			}			
			wapi.saveWorkflowActionClassParameters(newParams);
			response.getWriter().println(wac.getId() + ":" + wac.getName());
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	}
}


