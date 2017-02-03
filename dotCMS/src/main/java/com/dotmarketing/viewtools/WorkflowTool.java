package com.dotmarketing.viewtools;

import java.util.List;
import java.util.Map;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.liferay.portal.model.User;


/**
 * This class is a thin wrapper - mostly read only, for the WorkflowAPI
 * @author will
 *
 */
public class WorkflowTool implements ViewTool {

	WorkflowAPI wapi = APILocator.getWorkflowAPI();

	public void init(Object obj) {

	}

	
	
	
	
	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException {
		return wapi.findTaskByContentlet(contentlet);
	}

	public WorkflowStep findStepByContentlet(Contentlet contentlet) throws DotDataException {
		return wapi.findStepByContentlet(contentlet);
	}

	public WorkflowTask findTaskById(String id) throws DotDataException {
		return wapi.findTaskById(id);
	}

	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		return wapi.findWorkFlowComments(task);
	}

	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {

		return wapi.findWorkflowHistory(task);
	}

	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException {

		return wapi.findSchemes(showArchived);
	}

	public WorkflowScheme findDefaultScheme() throws DotDataException {
		return wapi.findDefaultScheme();
	}

	public WorkflowScheme findScheme(String id) throws DotDataException {
		return wapi.findScheme(id);
	}

	public WorkflowScheme findSchemeForStruct(Structure struct) throws DotDataException {
		return wapi.findSchemeForStruct(struct);
	}

	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
		return wapi.findSteps(scheme);
	}

	public WorkflowAction findAction(String id, User user) throws DotDataException, DotSecurityException {
		return wapi.findAction(id, user);
	}

	public List<WorkflowAction> findAvailableActions(Contentlet contentlet, User user) throws DotDataException, DotSecurityException {
		return wapi.findAvailableActions(contentlet, user);
	}

	public List<WorkflowAction> findActions(WorkflowStep step, User user) throws DotDataException, DotSecurityException {
		return wapi.findActions(step, user);
	}

	public WorkflowStep findStep(String id) throws DotDataException {
		return wapi.findStep(id);
	}

	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException {
		return wapi.findActionClasses(action);
	}

	public WorkflowActionClass findActionClass(String id) throws DotDataException {
		return wapi.findActionClass(id);
	}

	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws DotDataException {
		return wapi.findParamsForActionClass(actionClass);
	}

	public List<WorkFlowActionlet> findActionlets() throws DotDataException {
		return wapi.findActionlets();
	}

	public WorkFlowActionlet findActionlet(String clazz) throws DotDataException {
		return wapi.findActionlet(clazz);
	}

	public WorkflowProcessor fireWorkflowPreCheckin(Contentlet contentlet, User user) throws DotDataException, DotWorkflowException,
			DotContentletValidationException {
		return wapi.fireWorkflowPreCheckin(contentlet, user);
	}

	public void fireWorkflowPostCheckin(WorkflowProcessor wflow) throws DotDataException, DotWorkflowException {
		wapi.fireWorkflowPostCheckin(wflow);
	}

	public WorkflowProcessor fireWorkflowNoCheckin(Contentlet contentlet, User user) throws DotDataException, DotWorkflowException,
			DotContentletValidationException {
		return wapi.fireWorkflowNoCheckin(contentlet,user);
	}

}