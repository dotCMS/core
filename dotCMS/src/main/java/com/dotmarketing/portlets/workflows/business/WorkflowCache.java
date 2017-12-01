package com.dotmarketing.portlets.workflows.business;

import java.util.List;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;

//This interface should have default package access
public abstract class WorkflowCache implements Cachable{

    public final String defaultKey = "DEFAULT_WORKFLOW_SCHEME";
	protected static String PRIMARY_GROUP = "WorkflowCache";
	protected static String TASK_GROUP = "WorkflowTaskCache";
	protected static String STEP_GROUP = "WorkflowStepCache";
	protected static String ACTION_GROUP = "WorkflowActionCache";
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
	abstract protected List<WorkflowAction> getActions(WorkflowStep step);
	
	abstract public void clearCache();
	abstract protected void remove(Contentlet contentlet);
	public abstract void remove(WorkflowScheme scheme);
	abstract public void remove(WorkflowStep step);
	abstract protected void removeActions(WorkflowStep step);
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
		return new String[]{PRIMARY_GROUP, TASK_GROUP, STEP_GROUP};
	}

	abstract protected void add404Task(Contentlet contentlet) ;
	abstract protected boolean is404(Contentlet contentlet);
}