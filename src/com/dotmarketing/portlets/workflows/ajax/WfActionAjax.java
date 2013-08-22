package com.dotmarketing.portlets.workflows.ajax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class WfActionAjax extends WfBaseAction {
	 public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{};
	public void reorder(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String actionId = request.getParameter("actionId");
		String o = request.getParameter("order");
		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		try {
			int order = Integer.parseInt(o);
			//anyone with permission the workflowscheme portlet can reoirder actions
			WorkflowAction action = wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());

				wapi.reorderAction(action, order);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}

	}


	
	
	
	public void delete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String actionId = request.getParameter("actionId");

		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		try {

			WorkflowAction action = wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());
			WorkflowStep step = wapi.findStep(action.getStepId());

			
			
			
			wapi.deleteAction(action);
			writeSuccess(response, step.getSchemeId() );
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}

	}

	public void save(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		WorkflowAPI wapi = APILocator.getWorkflowAPI();

		String actionName = request.getParameter("actionName");
		String actionId = request.getParameter("actionId");
		String whoCanUseTmp = request.getParameter("whoCanUse");
		List<String> whoCanUse = Arrays.asList(whoCanUseTmp.split(","));
		String actionIcon = request.getParameter("actionIconSelect");
		boolean actionAssignable = (request.getParameter("actionAssignable") != null);
		boolean actionCommentable = (request.getParameter("actionCommentable") != null);
		boolean requiresCheckout = (request.getParameter("actionRequiresCheckout") != null);
		boolean roleHierarchyForAssign = false;
		if(actionAssignable){
			roleHierarchyForAssign = (request.getParameter("actionRoleHierarchyForAssign") != null);
		}
		String actionNextAssign = request.getParameter("actionAssignToSelect");
		String actionNextStep = request.getParameter("actionNextStep");
		if (actionNextAssign != null && actionNextAssign.startsWith("role-")) {
			actionNextAssign = actionNextAssign.replaceAll("role-", "");
		}
		String actionCondition = request.getParameter("actionCondition");
		String stepId = request.getParameter("stepId");
		WorkflowAction newAction = new WorkflowAction();

		boolean isNew = true;
		try {

			WorkflowAction origAction = APILocator.getWorkflowAPI().findAction(actionId, APILocator.getUserAPI().getSystemUser());
			BeanUtils.copyProperties(newAction, origAction);
			if(origAction !=null || !origAction.isNew()){
				isNew=false;
			}
		} catch (Exception e) {
			
			Logger.debug(this.getClass(), "Unable to find action" + actionId);
		}
		newAction.setName(actionName);
		newAction.setAssignable(actionAssignable);
		newAction.setCommentable(actionCommentable);
		newAction.setIcon(actionIcon);
		newAction.setNextStep(actionNextStep);
		newAction.setStepId(stepId);
		newAction.setCondition(actionCondition);
		newAction.setRequiresCheckout(requiresCheckout);
		newAction.setRoleHierarchyForAssign(roleHierarchyForAssign);
		try {
			newAction.setNextAssign(resolveRole(actionNextAssign).getId());
			if(!UtilMethods.isSet(newAction.getNextAssign())){
				newAction.setNextAssign(null);
			}
			List<Permission> permissions = new ArrayList<Permission>();
            for ( String perm : whoCanUse ) {
                if ( !UtilMethods.isSet( perm ) ) {
                    continue;
                }

                Role role = resolveRole( perm );
                Permission p = new Permission( newAction.getId(), role.getId(), PermissionAPI.PERMISSION_USE );

                boolean exists = false;
                for ( Permission curr : permissions ) {
                    exists = exists || curr.getRoleId().equals( p.getRoleId() );
                }

                if ( !exists ) {
                    permissions.add( p );
                }
            }

            wapi.saveAction(newAction, permissions);

			if(isNew){
				WorkflowActionClass wac = new WorkflowActionClass();
				wac.setActionId(newAction.getId());
				wac.setClazz(NotifyAssigneeActionlet.class.getName());
				wac.setName(NotifyAssigneeActionlet.class.newInstance().getName());
				wac.setOrder(0);
				wapi.saveActionClass(wac);
				
			}
			
			
			response.getWriter().println("SUCCESS:" + newAction.getId());
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			writeError(response, e.getMessage());
		}
	}
	
	
	
	
	private Role resolveRole(String id) throws DotDataException{
		Role test = null;
		
		String newid = id.substring(id.indexOf("-") + 1, id.length());
		
		if(id.startsWith("user-")){
			test = APILocator.getRoleAPI().loadRoleByKey(newid);
		}
		else if(id.startsWith("role-")){
			test = APILocator.getRoleAPI().loadRoleById(newid);
		}else{
			test = APILocator.getRoleAPI().loadRoleById(id);
		}
		return test;
		
	}
	
	
}
