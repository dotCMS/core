package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

//This interface should have default package access
public class WorkflowCacheImpl extends WorkflowCache {
	final String defaultKey = "DEFAULT_WORKFLOW_SCHEME";
	private DotCacheAdministrator cache;

	public WorkflowCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
	}

	protected WorkflowScheme addDefaultScheme(WorkflowScheme scheme) {

		cache.put(defaultKey, scheme, getPrimaryGroup());
		return scheme;
	}

	protected WorkflowScheme addForStructure(String struct, WorkflowScheme scheme) {
		String key = struct;

		// Add the key to the cache
		cache.put(key, scheme.getId(), getPrimaryGroup());
		cache.put(scheme.getId(), scheme, getPrimaryGroup());

		return scheme;
		
	}
	
	protected WorkflowScheme add(WorkflowScheme scheme) {

		String key = scheme.getId();

		// Add the key to the cache
		cache.put(key, scheme, getPrimaryGroup());

			return scheme;
		
	}

	protected WorkflowScheme getScheme(String key) {

		WorkflowScheme scheme = null;
		try {
			scheme = (WorkflowScheme) cache.get(key, getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return scheme;
	}

	
	protected WorkflowScheme getSchemeByStruct(String structId) {


		try {
			return  (WorkflowScheme) cache.get((String) cache.get(structId, getPrimaryGroup()), getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return null;
	}
	
	protected void removeStructure(String struct) {
		if (struct != null ) {
			cache.remove(struct, getPrimaryGroup());
		}
	}
	
	
	protected void remove(WorkflowScheme scheme) {
		if (scheme != null && UtilMethods.isSet(scheme)) {
			cache.remove(scheme.getId(), getPrimaryGroup());
		}
	}

	protected void remove(WorkflowStep step) {
		cache.remove(step.getId(), STEP_GROUP);
	}
	
	protected void remove(WorkflowTask task) {
		cache.remove(task.getWebasset(), TASK_GROUP);
		cache.remove(task.getWebasset(), STEP_GROUP);
		cache.remove(task.getId(), TASK_GROUP);
	}
	
	protected void remove(Contentlet contentlet) {
		if (contentlet != null ) {
			cache.remove(contentlet.getIdentifier(), STEP_GROUP);
			cache.remove(contentlet.getIdentifier(), TASK_GROUP);
		}
	}
	protected WorkflowScheme getDefaultScheme() {
		return getScheme(defaultKey);
	}

	public void clearCache() {
		// clear the cache
		for(String x : getGroups()){
			cache.flushGroup(x);
		}
	}
	@Override
	protected void clearStepsCache() {
		// clear the cache
		cache.flushGroup(STEP_GROUP);
		
	}
	protected WorkflowStep getStep(String key) {
		WorkflowStep step = null;
		try {
			step = (WorkflowStep) cache.get(key, STEP_GROUP);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return step;
	}

	protected WorkflowStep add(WorkflowStep step) {
		String key = step.getId();

		// Add the key to the cache
		cache.put(key, step, STEP_GROUP);

			return step;
		
	}

	 protected WorkflowTask getTask(Contentlet contentlet){
		
			WorkflowTask task = null;
			try {
				String taskId = (String) cache.get(contentlet.getIdentifier(), TASK_GROUP);
				task = (WorkflowTask) cache.get(taskId, TASK_GROUP);
			} catch (Exception e) {
				Logger.debug(this.getClass(), e.getMessage());
			}
			return task;
	 }
	 protected WorkflowStep getStep(Contentlet contentlet){
			
		 WorkflowStep step = null;
			try {
				String stepId = (String) cache.get(contentlet.getIdentifier(), STEP_GROUP);
				step = (WorkflowStep) cache.get(stepId, STEP_GROUP);
			} catch (Exception e) {
				Logger.debug(this.getClass(), e.getMessage());
			}
			return step;
	 }
	 
	protected WorkflowStep addStep(WorkflowStep step) {
		 if(step ==null || !UtilMethods.isSet(step.getId()) ){
			 return null;
		 }

		cache.put(step.getId(), step , STEP_GROUP);		

		return step;
	}
	 
	protected WorkflowTask addTask(WorkflowTask task) {
		 if(task ==null || !UtilMethods.isSet(task.getId()) ){
			 return null;
		 }

		cache.put(task.getId(), task , TASK_GROUP);		

		return task;
	}
	
	
	
	protected WorkflowStep addStep(Contentlet contentlet, WorkflowStep step) {
		 if(contentlet ==null || !UtilMethods.isSet(contentlet.getIdentifier())
				 || step ==null || !UtilMethods.isSet(step.getId()) ){
			 return null;
		 }


		// Add the Step id as a string to the cache
		cache.put(contentlet.getIdentifier(), step.getId(), STEP_GROUP);
		return addStep(step);	


	}
	 
	 
	 
	protected WorkflowTask addTask(Contentlet contentlet, WorkflowTask task) {
		 if(contentlet ==null || !UtilMethods.isSet(contentlet.getIdentifier())
				 || task ==null || !UtilMethods.isSet(task.getId()) ){
			 return null;
		 }


		// Add the key to the cache
		cache.put(contentlet.getIdentifier(), task.getId(), TASK_GROUP);
		return addTask(task);
	}
}