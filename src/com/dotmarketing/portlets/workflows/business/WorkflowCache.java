package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;

//This interface should have default package access
public abstract class WorkflowCache implements Cachable{
	protected static String PRIMARY_GROUP = "WorkflowCache";
	protected static String TASK_GROUP = "WorkflowTaskCache";
	protected static String STEP_GROUP = "WorkflowStepCache";
	abstract protected WorkflowScheme add(WorkflowScheme scheme);

	abstract protected WorkflowScheme getScheme(String key);
	abstract protected WorkflowStep getStep(String key);
	abstract protected WorkflowStep getStep(Contentlet con);
	abstract protected WorkflowTask getTask(Contentlet key);
	abstract protected WorkflowStep addStep(Contentlet contentlet, WorkflowStep step);
	abstract protected WorkflowStep addStep(WorkflowStep step) ;
	abstract protected WorkflowTask addTask(WorkflowTask step) ;
	abstract protected WorkflowTask addTask(Contentlet contentlet, WorkflowTask task);
	abstract protected WorkflowStep add(WorkflowStep step);
	abstract public void clearCache();
	abstract protected void remove(Contentlet contentlet);
	abstract protected void remove(WorkflowScheme scheme);
	abstract protected void remove(WorkflowStep step);
	abstract protected void remove(WorkflowTask task) ;
	abstract protected WorkflowScheme getSchemeByStruct(String key) ;
	abstract protected WorkflowScheme addForStructure(String struct, WorkflowScheme scheme) ;
	abstract protected void removeStructure(String struct);
	protected void flushSteps(){
		CacheLocator.getCacheAdministrator().flushGroup(STEP_GROUP);
	}
	protected void flushTasks(){
		CacheLocator.getCacheAdministrator().flushGroup(TASK_GROUP);
	}
	abstract protected void clearStepsCache() ;
	abstract protected WorkflowScheme getDefaultScheme();
	abstract protected WorkflowScheme addDefaultScheme(WorkflowScheme scheme);

	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	public String[] getGroups() {
		return new String[]{PRIMARY_GROUP, TASK_GROUP, STEP_GROUP};
	}
	
}