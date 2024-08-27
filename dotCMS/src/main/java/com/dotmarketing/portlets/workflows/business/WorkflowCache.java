package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.*;

import java.util.List;
import java.util.Map;

//This interface should have default package access
public abstract class WorkflowCache implements Cachable {

    public final String defaultKey 				= "DEFAULT_WORKFLOW_SCHEME";
	protected static String PRIMARY_GROUP 		= "WorkflowCache";
	protected static String TASK_GROUP 			= "WorkflowTaskCache";
	protected static String STEP_GROUP 			= "WorkflowStepCache";
	protected static String ACTION_GROUP 		= "WorkflowActionCache";
	protected static String SYSTEM_ACTION_GROUP 		= "SystemActionCache";
	protected static String ACTION_CLASS_GROUP 	= "WorkflowActionClassCache";
	abstract protected WorkflowScheme add(WorkflowScheme scheme);

	public abstract WorkflowScheme getScheme(String key);
	abstract protected WorkflowStep getStep(String key);
	abstract protected List<WorkflowStep> getSteps(Contentlet con);
	abstract protected WorkflowTask getTask(Contentlet key);
	abstract protected List<WorkflowStep> addSteps(Contentlet contentlet, List<WorkflowStep> steps);
	abstract protected WorkflowStep addStep(WorkflowStep step) ;
	abstract protected WorkflowTask addTask(WorkflowTask step) ;
	abstract protected WorkflowTask addTask(Contentlet contentlet, WorkflowTask task);
	abstract protected WorkflowStep add(WorkflowStep step);
	abstract protected List<WorkflowAction> addActions(WorkflowStep step, List<WorkflowAction> actions);
	abstract protected List<WorkflowAction> addActions(WorkflowScheme scheme, List<WorkflowAction> actions);
	abstract protected List<WorkflowActionClass> addActionClasses(WorkflowAction action, List<WorkflowActionClass> actionClasses);
	abstract protected List<WorkflowAction> getActions(WorkflowStep step);

	abstract protected List<WorkflowActionClass> getActionClasses(final WorkflowAction action);
	abstract protected List<WorkflowAction> getActions(WorkflowScheme scheme);
	
	abstract public void clearCache();
	abstract protected void remove(Contentlet contentlet);
	public abstract void remove(WorkflowScheme scheme);
	abstract public void remove(WorkflowStep step);
	abstract public void remove(WorkflowAction action);
	abstract protected void removeActions(WorkflowStep step);
	abstract protected void removeActions(WorkflowScheme scheme);
	abstract protected void remove(WorkflowTask task) ;
	abstract protected List<WorkflowScheme> getSchemesByStruct(String key) ;
	abstract protected List<WorkflowScheme> addForStructure(String struct, List<WorkflowScheme> scheme) ;
	abstract protected void removeStructure(String struct);
	protected void flushSteps(){
		CacheLocator.getCacheAdministrator().flushGroup(STEP_GROUP);
	}
	protected void flushTasks(){
		CacheLocator.getCacheAdministrator().flushGroup(TASK_GROUP);
	}
	abstract protected void clearStepsCache() ;
	public abstract WorkflowScheme getDefaultScheme();
	abstract protected WorkflowScheme addDefaultScheme(WorkflowScheme scheme);

	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	public String[] getGroups() {
		return new String[]{PRIMARY_GROUP, TASK_GROUP, STEP_GROUP, ACTION_GROUP, SYSTEM_ACTION_GROUP, ACTION_CLASS_GROUP};
	}

	abstract protected void add404Task(Contentlet contentlet) ;
	abstract protected boolean is404(Contentlet contentlet);

	/**
	 * Removes system actions referrer by workflow action id
	 * @param workflowActionId {@link String}
	 */
	public abstract void removeSystemActionsByWorkflowAction(String workflowActionId);

	/**
	 * Removes system actions referrer by workflow action id
	 * @param schemeId {@link String}
	 */
	public abstract void removeSystemActionsByScheme(String schemeId);

	/**
	 * Removes the system actions referrer by content type variable
	 * @param variable {@link String}
	 */
	public abstract void removeSystemActionsByContentType(String variable);

	/**
	 * Adds the list of system actions by system action name
	 * @param systemActionName {@link String}
 	 * @param schemeIdList     {@link List}
	 * @param results          {@link List}
	 */
	public abstract void addSystemActionsBySystemActionNameAndSchemeIds(String systemActionName, List<String> schemeIdList, List<Map<String, Object>> results);

	/**
	 * Finds the system mapping rows by systemActionName and scheme id list.
	 * @param systemActionName {@link String}
	 * @param schemeIdList     {@link List}
	 * @return List
	 */
	public abstract List<Map<String, Object>> findSystemActionsBySchemes(String systemActionName, List<String> schemeIdList);

	/**
	 * Finds the system mapping by systemActionName and content type variable
	 * @param systemActionName {@link String}
	 * @param variable        {@link String}
	 * @return Map
	 */
	public abstract Map<String, Object> findSystemActionByContentType(String systemActionName, String variable);

	/**
	 * Adds the system mapping row by systemActionName and content type variable
	 * @param systemActionName {@link String}
	 * @param variable         {@link String}
	 * @param mappingRow       {@link Map}
	 */
	public abstract void addSystemActionBySystemActionNameAndContentTypeVariable(String systemActionName, String variable, Map<String, Object> mappingRow);

	/**
	 * Finds the system mapping rows by workflow action id
	 * @param workflowActionId  {@link String}
	 * @return List
	 */
	public abstract List<Map<String, Object>> findSystemActionsByWorkflowAction(String workflowActionId);

	/**
	 * Adds the system mapping rows by workflow action id
	 * @param workflowActionId {@link String}
	 * @param results {@link List}
	 */
	public abstract void addSystemActionsByWorkflowAction(String workflowActionId, List<Map<String, Object>> results);

	/**
	 * Finds the system action rows by scheme id.
	 * @param schemeId {@link String}
	 * @return List
	 */
	public abstract List<Map<String, Object>> findSystemActionsByScheme(String schemeId);

	/**
	 * Adds the system mapping rows by scheme id
	 * @param schemeId {@link String}
	 * @param results  {@link List}
	 */
	public abstract void addSystemActionsByScheme(String schemeId, List<Map<String, Object>> results);

	/**
	 * Finds the system action rows by content type variable
	 * @param variable {@link String}
	 * @return List
	 */
	public abstract List<Map<String, Object>> findSystemActionsByContentType(String variable);

	/**
	 * Adds the system mapping rows by content type variable
	 * @param variable {@link String}
	 * @param results  {@link List}
	 */
	public abstract void addSystemActionsByContentType(String variable, List<Map<String, Object>> results);

	/**
	 * Remove all related caches to the mapping
	 * @param mapping SystemActionWorkflowActionMapping
	 */
	public abstract void removeSystemActionWorkflowActionMapping(SystemActionWorkflowActionMapping mapping);
}
